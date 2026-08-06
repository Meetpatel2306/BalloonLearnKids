package com.meetpatel.popgrow.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Random
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * A cartoon call for every animal in the Animals game — a moo, a woof, a meow.
 *
 * Like every other sound in the app these are synthesised on the device at
 * first launch: a pitch glide, a few harmonics, some breathy noise and a
 * wobble, tuned per animal. That keeps the app fully offline and adds no audio
 * files to the download, while still giving each animal its own clear voice.
 */
class AnimalVoices(context: Context) {

    /** One burst of sound inside a call: a pitch glide with a tone colour. */
    private class Seg(
        val start: Float,
        val dur: Float,
        val f0: Float,
        val f1: Float,
        val amp: Float = 0.5f,
        val noise: Float = 0f,
        val wobble: Float = 0f,
        val harmonics: Int = 3,
        val decay: Float = 2.5f,
    )

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids = HashMap<String, Int>()
    private val ready = HashSet<Int>()
    private val rnd = Random()
    private var released = false

    init {
        pool.setOnLoadCompleteListener { _, id, status ->
            if (status == 0) synchronized(ready) { ready.add(id) }
        }
        val dir = File(context.cacheDir, "animals").apply { mkdirs() }
        CALLS.forEach { (name, segs) ->
            val f = File(dir, "${name.lowercase()}_v$CACHE_VERSION.wav")
            if (!f.exists() || f.length() < MIN_VALID_BYTES) runCatching { write(f, segs) }
            if (f.exists()) ids[name] = pool.load(f.absolutePath, 1)
        }
    }

    /** Play an animal's call by name, e.g. "Cow". Silent if it isn't ready yet. */
    fun play(name: String, volume: Float = 0.85f) {
        if (released) return
        val id = ids[name] ?: return
        synchronized(ready) { if (id !in ready) return }
        val v = volume.coerceIn(0f, 1f)
        pool.play(id, v, v, 1, 0, 0.97f + rnd.nextFloat() * 0.06f)
    }

    fun release() {
        if (released) return
        released = true
        pool.release()
    }

    // --------------------------------------------------------------- synthesis

    private fun write(file: File, segs: List<Seg>) {
        val total = segs.maxOf { it.start + it.dur } + 0.05f
        val out = FloatArray((SAMPLE_RATE * total).toInt())
        for (s in segs) {
            val from = (SAMPLE_RATE * s.start).toInt()
            val n = (SAMPLE_RATE * s.dur).toInt()
            var phase = 0f
            for (j in 0 until n) {
                val i = from + j
                if (i >= out.size) break
                val fr = j / n.toFloat()
                val t = j / SAMPLE_RATE.toFloat()
                var f = s.f0 + (s.f1 - s.f0) * fr
                if (s.wobble > 0f) f *= 1f + 0.06f * sin(TWO_PI.toFloat() * s.wobble * t)
                phase += TWO_PI.toFloat() * f / SAMPLE_RATE
                // A few harmonics give the call a body; noise adds breath.
                var w = 0f
                for (h in 1..s.harmonics) w += sin(phase * h) / h
                if (s.noise > 0f) w += (rnd.nextFloat() * 2f - 1f) * s.noise
                // Soft in, natural decay out, so nothing clicks.
                val attack = (fr / 0.12f).coerceAtMost(1f)
                val env = attack * exp(-s.decay * t) * sin(PI.toFloat() * fr).coerceAtLeast(0.15f)
                out[i] += w * env * s.amp
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
        const val CACHE_VERSION = 1
        const val TWO_PI = 2.0 * PI

        /** Each animal's voice, tuned by ear: pitch, glide, breath and wobble. */
        val CALLS: Map<String, List<Seg>> = mapOf(
            // A rising-then-falling meow.
            "Cat" to listOf(Seg(0f, 0.55f, 620f, 480f, 0.45f, 0.02f, 7f, 5, 1.6f)),
            // Two short, gruff barks.
            "Dog" to listOf(
                Seg(0f, 0.20f, 260f, 150f, 0.55f, 0.14f, 0f, 6, 7f),
                Seg(0.26f, 0.20f, 250f, 140f, 0.5f, 0.14f, 0f, 6, 7f),
            ),
            // A long, low moo.
            "Cow" to listOf(Seg(0f, 0.85f, 175f, 145f, 0.5f, 0.03f, 4f, 7, 1.1f)),
            // Snuffly oinks.
            "Pig" to listOf(
                Seg(0f, 0.16f, 380f, 240f, 0.42f, 0.22f, 0f, 4, 8f),
                Seg(0.20f, 0.16f, 360f, 230f, 0.42f, 0.22f, 0f, 4, 8f),
                Seg(0.40f, 0.18f, 340f, 210f, 0.4f, 0.22f, 0f, 4, 7f),
            ),
            // Nasal quacks.
            "Duck" to listOf(
                Seg(0f, 0.14f, 620f, 520f, 0.42f, 0.06f, 0f, 6, 9f),
                Seg(0.20f, 0.14f, 600f, 500f, 0.4f, 0.06f, 0f, 6, 9f),
            ),
            // A tiny high squeak.
            "Rabbit" to listOf(Seg(0f, 0.18f, 900f, 1250f, 0.32f, 0.02f, 12f, 2, 5f)),
            // A rumbling growl.
            "Bear" to listOf(Seg(0f, 0.75f, 95f, 72f, 0.55f, 0.18f, 5f, 8, 1.4f)),
            // A big roar that swells.
            "Lion" to listOf(Seg(0f, 0.95f, 130f, 85f, 0.6f, 0.24f, 6f, 8, 0.9f)),
            // Cheeky ooh-ooh calls.
            "Monkey" to listOf(
                Seg(0f, 0.16f, 700f, 980f, 0.36f, 0.03f, 9f, 3, 6f),
                Seg(0.22f, 0.16f, 760f, 1080f, 0.36f, 0.03f, 9f, 3, 6f),
                Seg(0.44f, 0.18f, 820f, 1150f, 0.34f, 0.03f, 9f, 3, 6f),
            ),
            // A rising trumpet.
            "Elephant" to listOf(Seg(0f, 0.75f, 260f, 620f, 0.5f, 0.05f, 5f, 6, 1.5f)),
            // Two croaks.
            "Frog" to listOf(
                Seg(0f, 0.18f, 190f, 175f, 0.5f, 0.10f, 30f, 5, 3.5f),
                Seg(0.26f, 0.18f, 185f, 170f, 0.5f, 0.10f, 30f, 5, 3.5f),
            ),
            // Little underwater bubbles.
            "Fish" to listOf(
                Seg(0f, 0.09f, 420f, 900f, 0.30f, 0.02f, 0f, 2, 12f),
                Seg(0.13f, 0.09f, 480f, 1000f, 0.28f, 0.02f, 0f, 2, 12f),
                Seg(0.26f, 0.09f, 530f, 1120f, 0.26f, 0.02f, 0f, 2, 12f),
            ),
            // Bright tweets.
            "Bird" to listOf(
                Seg(0f, 0.10f, 1900f, 3200f, 0.32f, 0f, 0f, 2, 9f),
                Seg(0.15f, 0.10f, 2000f, 3300f, 0.30f, 0f, 0f, 2, 9f),
                Seg(0.30f, 0.12f, 2100f, 3000f, 0.28f, 0f, 0f, 2, 8f),
            ),
            // A whinny: falling pitch with a fast flutter.
            "Horse" to listOf(Seg(0f, 0.70f, 640f, 300f, 0.45f, 0.10f, 26f, 5, 2.2f)),
        )
    }
}
