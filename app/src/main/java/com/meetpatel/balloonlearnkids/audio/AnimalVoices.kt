package com.meetpatel.balloonlearnkids.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * The animal calls — a roar, a moo, a bark.
 *
 * These are synthesised with a **source–filter voice model**, the same idea
 * behind speech synthesis: a rough buzzing source (a sawtooth at the animal's
 * pitch, plus breath noise) is pushed through two or three resonant *formant*
 * filters. The formants are what make a sound read as a throat and mouth
 * rather than a beep — a lion's low, wide-open formants growl; a bird's high,
 * narrow ones chirp. Growl is added as amplitude roughness and pitch jitter.
 *
 * They are cartoon voices, not field recordings, but they are recognisably the
 * animal. Everything is generated on the device at first launch, so the app
 * stays fully offline and adds no audio files to the download.
 *
 * If you prefer real recordings, drop them into `app/src/main/res/raw/` as
 * `animal_lion.ogg` and so on — those always win. See ANIMAL_SOUNDS.md.
 */
class AnimalVoices(context: Context) {

    /**
     * One animal's voice.
     *
     * [f0] is the pitch over time and [formants] the resonances over time, both
     * given as breakpoints that are glided between. [noise] mixes in breath,
     * [rough]/[roughHz] add the growl, [jitter] roughens the pitch, and [env]
     * shapes the loudness. [repeats] makes barks and chirps repeat.
     */
    private class Voice(
        val dur: Float,
        val f0: FloatArray,
        val formants: Array<FloatArray>,
        val bandwidths: FloatArray,
        val noise: Float = 0.1f,
        val rough: Float = 0f,
        val roughHz: Float = 25f,
        val jitter: Float = 0.01f,
        val env: FloatArray = floatArrayOf(0f, 1f, 1f, 0f),
        val repeats: Int = 1,
        val gap: Float = 0.08f,
        val gain: Float = 0.8f,
    )

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids = HashMap<String, Int>()
    private val ready = HashSet<Int>()
    /** How long each call actually lasts, already capped at [MAX_MS]. */
    private val lengths = HashMap<String, Int>()
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val rnd = Random()
    private var released = false

    init {
        pool.setOnLoadCompleteListener { _, id, status ->
            if (status == 0) synchronized(ready) { ready.add(id) }
        }
        val res = context.resources
        val dir = File(context.cacheDir, "animals").apply { mkdirs() }

        VOICES.forEach { (name, voice) ->
            // A real recording, if one has been added, always wins.
            val rawId = runCatching {
                res.getIdentifier("animal_${name.lowercase()}", "raw", context.packageName)
            }.getOrDefault(0)
            if (rawId != 0) {
                runCatching { ids[name] = pool.load(context, rawId, 1) }
                // Read the clip's real length so we know when it has finished —
                // and never let one run longer than two seconds.
                // MediaMetadataRetriever is only AutoCloseable from API 29, so
                // it is released by hand to keep working back to Android 7.
                val ms = runCatching {
                    val mmr = android.media.MediaMetadataRetriever()
                    try {
                        context.resources.openRawResourceFd(rawId).use { fd ->
                            mmr.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                            mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                                ?.toIntOrNull() ?: MAX_MS
                        }
                    } finally {
                        runCatching { mmr.release() }
                    }
                }.getOrDefault(MAX_MS)
                lengths[name] = ms.coerceIn(200, MAX_MS)
                return@forEach
            }
            val f = File(dir, "${name.lowercase()}_v$CACHE_VERSION.wav")
            if (!f.exists() || f.length() < MIN_VALID_BYTES) runCatching { render(f, voice) }
            if (f.exists()) {
                ids[name] = pool.load(f.absolutePath, 1)
                val synthMs = ((voice.dur + voice.gap) * voice.repeats * 1000f).toInt()
                lengths[name] = synthMs.coerceIn(200, MAX_MS)
            }
        }
    }

    /** True if the call started playing. It is cut off after [MAX_MS] so no
     * clip ever drags on, however long the original recording was. */
    fun play(name: String, volume: Float = 0.95f): Boolean {
        if (released) return false
        val id = ids[name] ?: return false
        synchronized(ready) { if (id !in ready) return false }
        val v = volume.coerceIn(0f, 1f)
        // Recordings play as they are; synthesised calls get a little pitch
        // variation so repeat plays never sound mechanical.
        val rate = if (lengths.containsKey(name)) 1f else 0.96f + rnd.nextFloat() * 0.08f
        val stream = pool.play(id, v, v, 1, 0, rate)
        if (stream == 0) return false
        val cap = (lengths[name] ?: MAX_MS).coerceAtMost(MAX_MS)
        handler.postDelayed({ runCatching { pool.stop(stream) } }, cap.toLong())
        return true
    }

    /** How long the call runs, so the name can be spoken once it has finished. */
    fun durationMs(name: String): Long =
        (lengths[name] ?: MAX_MS).coerceAtMost(MAX_MS).toLong()

    fun release() {
        if (released) return
        released = true
        handler.removeCallbacksAndMessages(null)
        pool.release()
    }

    // --------------------------------------------------------------- synthesis

    /** Read a breakpoint curve at position [t] (0..1), gliding between points. */
    private fun curve(points: FloatArray, t: Float): Float {
        if (points.size == 1) return points[0]
        val x = (t.coerceIn(0f, 1f)) * (points.size - 1)
        val i = floor(x).toInt().coerceAtMost(points.size - 2)
        val f = x - i
        return points[i] + (points[i + 1] - points[i]) * f
    }

    /** A resonant band-pass, recomputed as the formant moves. */
    private class Biquad {
        var x1 = 0f; var x2 = 0f; var y1 = 0f; var y2 = 0f
        fun process(input: Float, freq: Float, bw: Float, sampleRate: Int): Float {
            val w0 = 2.0 * PI * freq / sampleRate
            val q = (freq / bw).coerceIn(0.5f, 20f)
            val alpha = (sin(w0) / (2 * q)).toFloat()
            val cosW = cos(w0).toFloat()
            val a0 = 1f + alpha
            val b0 = alpha / a0
            val b2 = -alpha / a0
            val a1 = (-2f * cosW) / a0
            val a2 = (1f - alpha) / a0
            val out = b0 * input + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = input
            y2 = y1; y1 = out
            return out
        }
    }

    private fun render(file: File, v: Voice) {
        val one = (SAMPLE_RATE * v.dur).toInt()
        val stride = one + (SAMPLE_RATE * v.gap).toInt()
        val total = stride * v.repeats
        val out = FloatArray(total)

        repeat(v.repeats) { r ->
            val offset = r * stride
            val filters = Array(v.formants.size) { Biquad() }
            var phase = 0f
            var peak = 0.0001f
            val buf = FloatArray(one)

            for (i in 0 until one) {
                val t = i / one.toFloat()
                val secs = i / SAMPLE_RATE.toFloat()

                // Source: a sawtooth (rich in harmonics, so the formants have
                // something to shape) plus breath noise, with pitch jitter.
                val pitch = curve(v.f0, t) * (1f + v.jitter * (rnd.nextFloat() - 0.5f))
                phase += pitch / SAMPLE_RATE
                if (phase >= 1f) phase -= 1f
                val saw = 2f * (phase - floor(phase + 0.5f))
                val breath = (rnd.nextFloat() * 2f - 1f) * v.noise
                val source = saw * (1f - v.noise) + breath

                // Filter: the formants that give the call its animal character.
                var shaped = 0f
                for (k in v.formants.indices) {
                    val fq = curve(v.formants[k], t).coerceIn(80f, SAMPLE_RATE / 2.2f)
                    val weight = 1f / (k + 1f)
                    shaped += filters[k].process(source, fq, v.bandwidths[k], SAMPLE_RATE) * weight
                }

                // Growl: amplitude roughness, plus the loudness shape.
                val growl = if (v.rough > 0f) {
                    1f - v.rough * 0.5f * (1f - sin(2f * PI.toFloat() * v.roughHz * secs))
                } else 1f
                buf[i] = shaped * curve(v.env, t) * growl
                val a = kotlin.math.abs(buf[i])
                if (a > peak) peak = a
            }

            // Normalise each call so quiet formant settings still come through.
            val norm = v.gain / peak
            for (i in 0 until one) {
                val j = offset + i
                if (j < out.size) out[j] += buf[i] * norm
            }
        }

        val pcm = ByteBuffer.allocate(out.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        out.forEach { pcm.putShort((it.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()) }
        val data = pcm.array()
        FileOutputStream(file).use { o ->
            o.write(header(data.size))
            o.write(data)
        }
    }

    private fun header(dataSize: Int): ByteArray =
        ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + dataSize); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1)
            putInt(SAMPLE_RATE); putInt(SAMPLE_RATE * 2); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(dataSize)
        }.array()

    private companion object {
        const val SAMPLE_RATE = 22050
        const val MIN_VALID_BYTES = 1024L

        /** No animal call ever plays for longer than this. */
        const val MAX_MS = 2000

        /** Bump to regenerate the cached calls after changing the voices. */
        const val CACHE_VERSION = 3

        /**
         * Each animal's voice. Low, wide formants growl and rumble; high,
         * narrow ones chirp and squeak; heavy noise gives breath and rasp.
         */
        val VOICES: Map<String, Voice> = mapOf(
            // Deep, rasping, and it swells then falls away.
            "Lion" to Voice(
                dur = 1.25f,
                f0 = floatArrayOf(105f, 88f, 72f, 62f),
                formants = arrayOf(
                    floatArrayOf(320f, 260f, 210f),
                    floatArrayOf(950f, 780f, 620f),
                    floatArrayOf(2100f, 1900f, 1700f),
                ),
                bandwidths = floatArrayOf(110f, 200f, 320f),
                noise = 0.34f, rough = 0.55f, roughHz = 28f, jitter = 0.05f,
                env = floatArrayOf(0f, 0.55f, 1f, 0.95f, 0.5f, 0f),
            ),
            // Lower and slower still, all rumble.
            "Bear" to Voice(
                dur = 0.95f,
                f0 = floatArrayOf(88f, 74f, 66f),
                formants = arrayOf(
                    floatArrayOf(260f, 220f),
                    floatArrayOf(720f, 640f),
                ),
                bandwidths = floatArrayOf(120f, 240f),
                noise = 0.42f, rough = 0.62f, roughHz = 22f, jitter = 0.06f,
                env = floatArrayOf(0f, 0.8f, 1f, 0.7f, 0f),
            ),
            // Two short, broadband barks.
            "Dog" to Voice(
                dur = 0.20f,
                f0 = floatArrayOf(300f, 210f, 170f),
                formants = arrayOf(
                    floatArrayOf(520f, 460f),
                    floatArrayOf(1500f, 1250f),
                    floatArrayOf(2600f, 2400f),
                ),
                bandwidths = floatArrayOf(150f, 350f, 500f),
                noise = 0.38f, rough = 0.2f, roughHz = 45f, jitter = 0.03f,
                env = floatArrayOf(0f, 1f, 0.5f, 0.12f, 0f),
                repeats = 2, gap = 0.13f,
            ),
            // The classic "ee-ow": the second formant falls away.
            "Cat" to Voice(
                dur = 0.62f,
                f0 = floatArrayOf(640f, 720f, 560f, 470f),
                formants = arrayOf(
                    floatArrayOf(780f, 900f, 620f, 520f),
                    floatArrayOf(2200f, 1900f, 1100f, 900f),
                ),
                bandwidths = floatArrayOf(140f, 260f),
                noise = 0.06f, rough = 0.12f, roughHz = 16f, jitter = 0.02f,
                env = floatArrayOf(0f, 0.9f, 1f, 0.8f, 0f),
            ),
            // Long, low and open-mouthed.
            "Cow" to Voice(
                dur = 1.05f,
                f0 = floatArrayOf(150f, 138f, 120f, 108f),
                formants = arrayOf(
                    floatArrayOf(420f, 380f, 310f),
                    floatArrayOf(950f, 880f, 800f),
                ),
                bandwidths = floatArrayOf(110f, 220f),
                noise = 0.12f, rough = 0.18f, roughHz = 13f, jitter = 0.02f,
                env = floatArrayOf(0f, 0.7f, 1f, 0.9f, 0.4f, 0f),
            ),
            // Snuffly, noisy little grunts.
            "Pig" to Voice(
                dur = 0.16f,
                f0 = floatArrayOf(330f, 240f, 190f),
                formants = arrayOf(
                    floatArrayOf(620f, 520f),
                    floatArrayOf(1300f, 1100f),
                ),
                bandwidths = floatArrayOf(180f, 380f),
                noise = 0.5f, rough = 0.45f, roughHz = 55f, jitter = 0.05f,
                env = floatArrayOf(0f, 1f, 0.6f, 0f),
                repeats = 3, gap = 0.09f,
            ),
            // Nasal, buzzy quacks.
            "Duck" to Voice(
                dur = 0.15f,
                f0 = floatArrayOf(500f, 420f, 360f),
                formants = arrayOf(
                    floatArrayOf(950f, 850f),
                    floatArrayOf(2300f, 2000f),
                ),
                bandwidths = floatArrayOf(220f, 420f),
                noise = 0.24f, rough = 0.4f, roughHz = 70f, jitter = 0.03f,
                env = floatArrayOf(0f, 1f, 0.55f, 0f),
                repeats = 2, gap = 0.11f,
            ),
            // A tiny high squeak.
            "Rabbit" to Voice(
                dur = 0.13f,
                f0 = floatArrayOf(1050f, 1450f, 1250f),
                formants = arrayOf(
                    floatArrayOf(1600f, 2000f),
                    floatArrayOf(3200f, 3400f),
                ),
                bandwidths = floatArrayOf(260f, 500f),
                noise = 0.08f, jitter = 0.02f,
                env = floatArrayOf(0f, 1f, 0.4f, 0f),
                repeats = 2, gap = 0.10f,
            ),
            // Bright rising hoots.
            "Monkey" to Voice(
                dur = 0.16f,
                f0 = floatArrayOf(560f, 900f, 820f),
                formants = arrayOf(
                    floatArrayOf(520f, 700f),
                    floatArrayOf(1150f, 1400f),
                ),
                bandwidths = floatArrayOf(150f, 300f),
                noise = 0.10f, jitter = 0.02f,
                env = floatArrayOf(0f, 1f, 0.7f, 0f),
                repeats = 3, gap = 0.09f,
            ),
            // A rising brassy trumpet.
            "Elephant" to Voice(
                dur = 0.95f,
                f0 = floatArrayOf(230f, 380f, 560f, 620f),
                formants = arrayOf(
                    floatArrayOf(600f, 900f, 1250f),
                    floatArrayOf(1800f, 2100f, 2400f),
                ),
                bandwidths = floatArrayOf(130f, 300f),
                noise = 0.18f, rough = 0.22f, roughHz = 18f, jitter = 0.03f,
                env = floatArrayOf(0f, 0.7f, 1f, 0.95f, 0.3f, 0f),
            ),
            // Two rattling croaks.
            "Frog" to Voice(
                dur = 0.22f,
                f0 = floatArrayOf(210f, 185f, 170f),
                formants = arrayOf(
                    floatArrayOf(430f, 380f),
                    floatArrayOf(1050f, 950f),
                ),
                bandwidths = floatArrayOf(160f, 300f),
                noise = 0.22f, rough = 0.75f, roughHz = 42f, jitter = 0.04f,
                env = floatArrayOf(0f, 1f, 0.8f, 0f),
                repeats = 2, gap = 0.14f,
            ),
            // Rising bubbles.
            "Fish" to Voice(
                dur = 0.09f,
                f0 = floatArrayOf(320f, 720f),
                formants = arrayOf(
                    floatArrayOf(700f, 1300f),
                ),
                bandwidths = floatArrayOf(200f),
                noise = 0.10f, jitter = 0.01f,
                env = floatArrayOf(0f, 1f, 0.3f, 0f),
                repeats = 3, gap = 0.07f,
            ),
            // Quick high chirps.
            "Bird" to Voice(
                dur = 0.10f,
                f0 = floatArrayOf(2100f, 3300f, 2700f),
                formants = arrayOf(
                    floatArrayOf(3000f, 3600f),
                ),
                bandwidths = floatArrayOf(600f),
                noise = 0.03f, jitter = 0.01f,
                env = floatArrayOf(0f, 1f, 0.5f, 0f),
                repeats = 3, gap = 0.08f,
            ),
            // A falling whinny with a fast flutter.
            "Horse" to Voice(
                dur = 0.85f,
                f0 = floatArrayOf(720f, 560f, 380f, 300f),
                formants = arrayOf(
                    floatArrayOf(850f, 700f, 600f),
                    floatArrayOf(1700f, 1500f, 1300f),
                ),
                bandwidths = floatArrayOf(150f, 320f),
                noise = 0.28f, rough = 0.55f, roughHz = 24f, jitter = 0.04f,
                env = floatArrayOf(0f, 1f, 0.9f, 0.6f, 0f),
            ),
        )
    }
}
