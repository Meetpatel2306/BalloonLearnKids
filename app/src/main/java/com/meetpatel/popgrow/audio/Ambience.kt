package com.meetpatel.popgrow.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Random
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * The living soundscape behind the game: a soft wind that breathes louder and
 * softer, with the occasional bird or cricket. Like every other sound in the
 * app it is synthesised on the device at first launch — nothing is downloaded
 * and nothing ships in the APK, so the game stays tiny and fully offline.
 *
 * Playback is deliberately gentle and low so it never competes with the musical
 * pops a child is making; it sits under everything as atmosphere.
 */
class Ambience(context: Context) {

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .build()

    private val ready = HashSet<Int>()
    private val rnd = Random()

    private var windId = 0
    private val dayIds = mutableListOf<Int>()
    private val nightIds = mutableListOf<Int>()

    /** 0 = day (birds), 1 = night (crickets, frogs, owls). Set by the UI from
     * the sky's current mood so the soundscape matches what the child sees. */
    @Volatile private var night = false

    fun setNight(value: Boolean) {
        night = value
    }

    private var windStream = 0
    private var scope: CoroutineScope? = null

    @Volatile private var wanted = false
    @Volatile private var released = false

    init {
        pool.setOnLoadCompleteListener { _, id, status ->
            if (status == 0) synchronized(ready) { ready.add(id) }
        }
        val dir = File(context.cacheDir, "ambience").apply { mkdirs() }
        windId = load(dir, "wind") { writeWind(it) }
        dayIds += load(dir, "bird1") { writeBird1(it) }
        dayIds += load(dir, "bird2") { writeBird2(it) }
        dayIds += load(dir, "duck") { writeDuck(it) }
        nightIds += load(dir, "cricket") { writeCricket(it) }
        nightIds += load(dir, "frog") { writeFrog(it) }
        nightIds += load(dir, "owl") { writeOwl(it) }
    }

    private fun load(dir: File, name: String, synth: (File) -> Unit): Int {
        val f = File(dir, "${name}_v$CACHE_VERSION.wav")
        if (!f.exists() || f.length() < MIN_VALID_BYTES) runCatching { synth(f) }
        return if (f.exists()) pool.load(f.absolutePath, 1) else 0
    }

    private fun isReady(id: Int): Boolean = synchronized(ready) { id in ready }

    // --------------------------------------------------------------- lifecycle

    /** Begin the soundscape and remember that it is wanted (see [resume]). */
    fun start() {
        wanted = true
        begin()
    }

    /** Leave the game: silence and forget it was wanted. */
    fun stop() {
        wanted = false
        end()
    }

    /** App went to the background: silence but remember to bring it back. */
    fun pause() = end()

    /** App returned to the foreground: restart only if a game still wants it. */
    fun resume() { if (wanted) begin() }

    fun release() {
        if (released) return
        released = true
        wanted = false
        end()
        pool.release()
    }

    private fun begin() {
        if (released || scope != null) return
        val s = CoroutineScope(Dispatchers.Default + SupervisorJob())
        scope = s
        s.launch { windLoop() }
        s.launch { critterLoop() }
    }

    private fun end() {
        scope?.cancel()
        scope = null
        if (windStream != 0) {
            runCatching { pool.stop(windStream) }
            windStream = 0
        }
    }

    // ----------------------------------------------------------------- driving

    private suspend fun windLoop() {
        var tries = 0
        while (windId == 0 || !isReady(windId)) {
            if (tries++ > 200) return
            delay(50)
        }
        windStream = pool.play(windId, 0f, 0f, 1, -1, 1f)
        var vol = 0.06f
        while (coroutineContext.isActive) {
            // Wander towards a new random loudness, so the wind gusts and lulls.
            val target = 0.03f + rnd.nextFloat() * 0.17f
            repeat(24) {
                if (!coroutineContext.isActive) return
                vol += (target - vol) * 0.15f
                runCatching { pool.setVolume(windStream, vol, vol) }
                delay(120)
            }
        }
    }

    private suspend fun critterLoop() {
        while (coroutineContext.isActive) {
            delay((4000 + rnd.nextInt(9000)).toLong())
            if (!coroutineContext.isActive) return
            // Birds while the sky is bright; crickets, frogs and owls at night.
            val ids = if (night) nightIds else dayIds
            val choices = ids.filter { isReady(it) }
            if (choices.isEmpty()) continue
            val id = choices[rnd.nextInt(choices.size)]
            val v = 0.07f + rnd.nextFloat() * 0.13f
            runCatching { pool.play(id, v, v, 1, 0, 0.9f + rnd.nextFloat() * 0.25f) }
        }
    }

    // --------------------------------------------------------------- synthesis

    /** A 5-second seamless gust: low-passed noise under a raised-cosine window,
     * so it fades to silence at both ends and loops without a click. */
    private fun writeWind(file: File) {
        val frames = (SAMPLE_RATE * 5f).toInt()
        val buf = FloatArray(frames)
        var lp = 0f
        var peak = 0.0001f
        for (i in 0 until frames) {
            val white = rnd.nextFloat() * 2f - 1f
            lp += 0.06f * (white - lp)
            buf[i] = lp
            if (kotlin.math.abs(lp) > peak) peak = kotlin.math.abs(lp)
        }
        val norm = 0.9f / peak
        writePcm(file, ShortArray(frames) { i ->
            val window = (0.5f - 0.5f * cos(TWO_PI.toFloat() * i / frames))
            (buf[i] * norm * window * Short.MAX_VALUE).toInt().toShort()
        })
    }

    private fun writeBird1(file: File) = writePcm(file, chirps(0.6f) { out ->
        // Three quick rising tweets.
        for (k in 0 until 3) tone(out, 0.14f * k, 0.07f, 1900f, 3400f, 0.5f)
    })

    private fun writeBird2(file: File) = writePcm(file, chirps(0.45f) { out ->
        // A two-note whistle with a little vibrato.
        tone(out, 0f, 0.16f, 2500f, 2400f, 0.45f, vibHz = 22f, vibDepth = 60f)
        tone(out, 0.2f, 0.16f, 2050f, 1950f, 0.45f, vibHz = 22f, vibDepth = 60f)
    })

    private fun writeCricket(file: File) = writePcm(file, chirps(0.6f) { out ->
        // A soft amplitude-modulated buzz.
        val n = out.size
        var phase = 0f
        for (i in 0 until n) {
            val t = i / SAMPLE_RATE.toFloat()
            phase += (TWO_PI * 2600f / SAMPLE_RATE).toFloat()
            val am = 0.5f + 0.5f * sin(TWO_PI.toFloat() * 55f * t)
            val env = sin(PI.toFloat() * (i / n.toFloat()))
            out[i] += sin(phase) * am * env * 0.28f
        }
    })

    private fun writeFrog(file: File) = writePcm(file, chirps(0.55f) { out ->
        // Two low, pulsed croaks with a bit of a growl (2nd harmonic).
        for (k in 0 until 2) {
            val start = (SAMPLE_RATE * 0.28f * k).toInt()
            val n = (SAMPLE_RATE * 0.18f).toInt()
            var phase = 0f
            for (j in 0 until n) {
                val i = start + j
                if (i >= out.size) break
                phase += (TWO_PI * 180f / SAMPLE_RATE).toFloat()
                val am = 0.5f + 0.5f * sin(TWO_PI.toFloat() * 32f * j / SAMPLE_RATE)
                val env = sin(PI.toFloat() * (j / n.toFloat()))
                out[i] += (sin(phase) + 0.3f * sin(2f * phase)) * am * env * 0.4f
            }
        }
    })

    private fun writeDuck(file: File) = writePcm(file, chirps(0.5f) { out ->
        // Two nasal quacks — bright harmonics with a fast decay.
        for (k in 0 until 2) {
            val start = (SAMPLE_RATE * 0.22f * k).toInt()
            val n = (SAMPLE_RATE * 0.13f).toInt()
            var phase = 0f
            for (j in 0 until n) {
                val i = start + j
                if (i >= out.size) break
                phase += (TWO_PI * 560f / SAMPLE_RATE).toFloat()
                val harm = sin(phase) + 0.5f * sin(2f * phase) + 0.3f * sin(3f * phase)
                val env = exp(-9f * (j / SAMPLE_RATE.toFloat()))
                out[i] += harm * env * 0.3f
            }
        }
    })

    private fun writeOwl(file: File) = writePcm(file, chirps(0.7f) { out ->
        // Two soft, breathy hoots.
        tone(out, 0f, 0.22f, 360f, 345f, 0.4f, vibHz = 8f, vibDepth = 14f)
        tone(out, 0.32f, 0.28f, 330f, 300f, 0.4f, vibHz = 8f, vibDepth = 14f)
    })

    /** Build a float buffer of [seconds], hand it to [fill], return 16-bit PCM. */
    private inline fun chirps(seconds: Float, fill: (FloatArray) -> Unit): ShortArray {
        val out = FloatArray((SAMPLE_RATE * seconds).toInt())
        fill(out)
        return ShortArray(out.size) { i ->
            (out[i].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
    }

    /** Add a smoothly enveloped tone that glides from [fStart] to [fEnd]. */
    private fun tone(
        out: FloatArray, startSec: Float, durSec: Float,
        fStart: Float, fEnd: Float, amp: Float,
        vibHz: Float = 0f, vibDepth: Float = 0f,
    ) {
        val start = (SAMPLE_RATE * startSec).toInt()
        val n = (SAMPLE_RATE * durSec).toInt()
        var phase = 0f
        for (j in 0 until n) {
            val i = start + j
            if (i >= out.size) break
            val fr = j / n.toFloat()
            var f = fStart + (fEnd - fStart) * fr
            if (vibHz > 0f) f += vibDepth * sin(TWO_PI.toFloat() * vibHz * j / SAMPLE_RATE)
            phase += (TWO_PI.toFloat() * f / SAMPLE_RATE)
            val env = sin(PI.toFloat() * fr)
            out[i] += sin(phase) * env * amp
        }
    }

    private fun writePcm(file: File, samples: ShortArray) {
        val data = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { data.putShort(it) }
        val bytes = data.array()
        FileOutputStream(file).use { out ->
            out.write(wavHeader(bytes.size))
            out.write(bytes)
        }
    }

    private fun wavHeader(dataSize: Int): ByteArray {
        val byteRate = SAMPLE_RATE * 2
        return ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + dataSize); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1.toShort()); putShort(1.toShort())
            putInt(SAMPLE_RATE); putInt(byteRate); putShort(2.toShort()); putShort(16.toShort())
            put("data".toByteArray()); putInt(dataSize)
        }.array()
    }

    private companion object {
        const val SAMPLE_RATE = 22050
        const val MIN_VALID_BYTES = 1024L
        const val CACHE_VERSION = 2
        const val TWO_PI = 2.0 * PI
    }
}
