package com.meetpatel.popgrow.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Speaks short words aloud — a colour, a number, a letter — using the phone's
 * built-in text-to-speech engine. That engine lives on the device, so this adds
 * no audio files to the APK and needs no internet: the app stays tiny and fully
 * offline. If no engine is available it simply stays silent and the game plays on.
 */
class Speaker(context: Context) {

    private var tts: TextToSpeech? = null
    @Volatile private var ready = false

    /** Callbacks waiting on a particular utterance to finish. */
    private val waiting = ConcurrentHashMap<String, () -> Unit>()
    private val nextId = AtomicLong(0)

    init {
        tts = runCatching {
            TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    runCatching { tts?.language = Locale.US }
                    // Well below normal talking speed: a two-year-old needs time
                    // to hear each word. The slightly raised pitch keeps it warm
                    // rather than robotic.
                    runCatching { tts?.setSpeechRate(0.68f) }
                    runCatching { tts?.setPitch(1.12f) }
                    runCatching {
                        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) = Unit
                            override fun onDone(utteranceId: String?) = finish(utteranceId)
                            @Deprecated("Required by the base class")
                            override fun onError(utteranceId: String?) = finish(utteranceId)
                            override fun onError(utteranceId: String?, errorCode: Int) = finish(utteranceId)
                        })
                    }
                    ready = true
                }
            }
        }.getOrNull()
    }

    private fun finish(utteranceId: String?) {
        utteranceId?.let { waiting.remove(it)?.invoke() }
    }

    /**
     * Speaks [text]. Returns true if speech actually started, so a caller that
     * wants to wait knows whether [onDone] will ever arrive. [onDone] fires when
     * the words have finished — used to line up the animal's name, then its
     * sound, then the next animal.
     */
    fun say(text: String, flush: Boolean = true, onDone: (() -> Unit)? = null): Boolean {
        if (!ready || text.isBlank()) return false
        val mode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val id = "u${nextId.incrementAndGet()}"
        if (onDone != null) waiting[id] = onDone
        val result = runCatching { tts?.speak(text, mode, null, id) }.getOrNull()
        if (result != TextToSpeech.SUCCESS) {
            waiting.remove(id)
            return false
        }
        return true
    }

    fun shutdown() {
        waiting.clear()
        runCatching { tts?.shutdown() }
        tts = null
        ready = false
    }
}
