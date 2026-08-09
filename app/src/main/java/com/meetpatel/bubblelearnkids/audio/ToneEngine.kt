package com.meetpatel.bubblelearnkids.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Every sound in the game is synthesised at first launch, so the APK ships with
 * zero audio assets.
 *
 * The scale is C major pentatonic: it has no semitone clashes, which means a
 * toddler mashing bubbles at random can never produce a dissonant interval.
 * Whatever they tap sounds deliberate. That is the whole trick behind the game.
 */
class ToneEngine(context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(12)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundIds = IntArray(SCALE.size)
    private val ready = HashSet<Int>()
    private var released = false
    private var popId = 0
    private var boingId = 0
    private var fanfareId = 0
    private val rnd = java.util.Random()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) synchronized(ready) { ready.add(sampleId) }
        }
        val dir = File(context.cacheDir, "tones").apply { mkdirs() }
        SCALE.forEachIndexed { i, freq ->
            val f = File(dir, "note_${i}_v$CACHE_VERSION.wav")
            if (!f.exists() || f.length() < MIN_VALID_BYTES) {
                runCatching { writeToneWav(f, freq) }
            }
            soundIds[i] = if (f.exists()) soundPool.load(f.absolutePath, 1) else 0
        }
        // A short percussive "pop" click, layered under every note on a tap.
        val popFile = File(dir, "pop_v$CACHE_VERSION.wav")
        if (!popFile.exists() || popFile.length() < MIN_VALID_BYTES) {
            runCatching { writePopWav(popFile) }
        }
        popId = if (popFile.exists()) soundPool.load(popFile.absolutePath, 1) else 0

        // A springy "boing" for balloons and a little fanfare for levelling up.
        val boingFile = File(dir, "boing_v$CACHE_VERSION.wav")
        if (!boingFile.exists() || boingFile.length() < MIN_VALID_BYTES) {
            runCatching { writeBoingWav(boingFile) }
        }
        boingId = if (boingFile.exists()) soundPool.load(boingFile.absolutePath, 1) else 0

        val fanfareFile = File(dir, "fanfare_v$CACHE_VERSION.wav")
        if (!fanfareFile.exists() || fanfareFile.length() < MIN_VALID_BYTES) {
            runCatching { writeFanfareWav(fanfareFile) }
        }
        fanfareId = if (fanfareFile.exists()) soundPool.load(fanfareFile.absolutePath, 1) else 0
    }

    /** The bouncy squeak of a balloon being tapped. */
    fun playBoing(volume: Float = 0.7f) = playCached(boingId, volume, 0.92f + rnd.nextFloat() * 0.18f)

    /** A short rising fanfare when the child reaches a new level. */
    fun playFanfare(volume: Float = 0.8f) = playCached(fanfareId, volume, 1f)

    private fun playCached(id: Int, volume: Float, rate: Float) {
        if (released || id == 0) return
        synchronized(ready) { if (id !in ready) return }
        val v = volume.coerceIn(0f, 1f)
        soundPool.play(id, v, v, 1, 0, rate)
    }

    /** The little click of a bubble bursting. A touch of pitch jitter keeps a
     * fast run of pops from sounding like a machine gun. */
    fun playPop(volume: Float = 0.6f) {
        if (released || popId == 0) return
        synchronized(ready) { if (popId !in ready) return }
        val v = volume.coerceIn(0f, 1f)
        soundPool.play(popId, v, v, 1, 0, 0.94f + rnd.nextFloat() * 0.22f)
    }

    /** Plays one note of the scale. Silently no-ops until the sample is decoded. */
    fun playNote(index: Int, volume: Float = 1f) {
        if (released) return
        val id = soundIds[index.coerceIn(0, SCALE.lastIndex)]
        if (id == 0) return
        synchronized(ready) { if (id !in ready) return }
        val v = volume.coerceIn(0f, 1f)
        soundPool.play(id, v, v, 1, 0, 1f)
    }

    /** A little three-note sparkle, used when a butterfly arrives. */
    fun playChord(vararg indices: Int, volume: Float = 0.55f) {
        indices.forEach { playNote(it, volume) }
    }

    /** A bright top-of-the-scale twinkle for the rare rainbow bubble — clearly
     * different from the low arrival chord, so the celebration feels special. */
    fun playSparkle(volume: Float = 0.55f) {
        playChord(5, 7, 9, volume = volume)
    }

    fun release() {
        if (released) return
        released = true
        soundPool.release()
    }

    // ---------------------------------------------------------------- synthesis

    private fun writeToneWav(file: File, freq: Float) {
        val frames = (SAMPLE_RATE * DURATION_SECONDS).toInt()
        val pcm = ByteBuffer.allocate(frames * 2).order(ByteOrder.LITTLE_ENDIAN)
        val attackFrames = SAMPLE_RATE * ATTACK_SECONDS

        for (i in 0 until frames) {
            val t = i / SAMPLE_RATE.toFloat()
            // Soft attack so the note never clicks, then an exponential decay that
            // gives it a wooden, music-box character rather than a raw beep.
            val attack = if (i < attackFrames) i / attackFrames else 1f
            val envelope = attack * exp(-DECAY * t)
            val w = sin(TWO_PI * freq * t) +
                0.32 * sin(2 * TWO_PI * freq * t) +
                0.11 * sin(3 * TWO_PI * freq * t)
            val sample = (w / 1.43).toFloat() * envelope * 0.82f
            pcm.putShort((sample.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort())
        }

        val data = pcm.array()
        FileOutputStream(file).use { out ->
            out.write(wavHeader(data.size))
            out.write(data)
        }
    }

    private fun writePopWav(file: File) {
        val frames = (SAMPLE_RATE * 0.09f).toInt()
        val pcm = ByteBuffer.allocate(frames * 2).order(ByteOrder.LITTLE_ENDIAN)
        var phase = 0f
        for (i in 0 until frames) {
            val t = i / SAMPLE_RATE.toFloat()
            // A quick downward pitch chirp plus a tiny noise transient gives the
            // satisfying "plop" of a bubble bursting.
            val freq = 180f + 620f * exp(-9f * t)
            phase += (TWO_PI * freq / SAMPLE_RATE).toFloat()
            val env = exp(-32f * t)
            val noise = (rnd.nextFloat() * 2f - 1f) * exp(-90f * t) * 0.4f
            val sample = (sin(phase) * 0.7f + noise) * env * 0.7f
            pcm.putShort((sample.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort())
        }
        val data = pcm.array()
        FileOutputStream(file).use { out ->
            out.write(wavHeader(data.size))
            out.write(data)
        }
    }

    private fun writeBoingWav(file: File) {
        val frames = (SAMPLE_RATE * 0.22f).toInt()
        val out = FloatArray(frames)
        var phase = 0f
        for (i in 0 until frames) {
            val fr = i / frames.toFloat()
            // Pitch springs up then back down — the classic cartoon "boing".
            val freq = 300f + 700f * sin(PI.toFloat() * fr)
            phase += (TWO_PI * freq / SAMPLE_RATE).toFloat()
            val env = sin(PI.toFloat() * fr)
            out[i] = (sin(phase) + 0.3f * sin(2f * phase)).toFloat() * env * 0.6f
        }
        writeFloatPcm(file, out)
    }

    private fun writeFanfareWav(file: File) {
        val out = FloatArray((SAMPLE_RATE * 0.62f).toInt())
        val steps = intArrayOf(0, 2, 4, 7, 9)   // an ascending pentatonic run
        steps.forEachIndexed { k, idx ->
            val start = (SAMPLE_RATE * 0.1f * k).toInt()
            val freq = SCALE[idx]
            val n = (SAMPLE_RATE * 0.32f).toInt()
            for (j in 0 until n) {
                val i = start + j
                if (i >= out.size) break
                val t = j / SAMPLE_RATE.toFloat()
                val env = exp(-5f * t)
                val w = sin(TWO_PI * freq * t) + 0.3 * sin(2 * TWO_PI * freq * t)
                out[i] += (w / 1.3).toFloat() * env * 0.4f
            }
        }
        writeFloatPcm(file, out)
    }

    private fun writeFloatPcm(file: File, buf: FloatArray) {
        val pcm = ByteBuffer.allocate(buf.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        buf.forEach { pcm.putShort((it.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()) }
        val data = pcm.array()
        FileOutputStream(file).use { out ->
            out.write(wavHeader(data.size))
            out.write(data)
        }
    }

    private fun wavHeader(dataSize: Int): ByteArray {
        val byteRate = SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8
        return ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(36 + dataSize)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)                                   // PCM chunk size
            putShort(1.toShort())                        // format = PCM
            putShort(CHANNELS.toShort())
            putInt(SAMPLE_RATE)
            putInt(byteRate)
            putShort((CHANNELS * BITS_PER_SAMPLE / 8).toShort())
            putShort(BITS_PER_SAMPLE.toShort())
            put("data".toByteArray())
            putInt(dataSize)
        }.array()
    }

    companion object {
        /** C major pentatonic across two octaves: C D E G A C D E G A. */
        val SCALE = floatArrayOf(
            261.63f, 293.66f, 329.63f, 392.00f, 440.00f,
            523.25f, 587.33f, 659.25f, 783.99f, 880.00f
        )

        private const val SAMPLE_RATE = 44100
        private const val CHANNELS = 1
        private const val BITS_PER_SAMPLE = 16
        private const val DURATION_SECONDS = 0.85f
        private const val ATTACK_SECONDS = 0.006f
        private const val DECAY = 4.2f
        private const val TWO_PI = 2.0 * PI
        private const val MIN_VALID_BYTES = 1024L

        /** Bump to regenerate cached samples after changing the synthesis. */
        private const val CACHE_VERSION = 2
    }
}
