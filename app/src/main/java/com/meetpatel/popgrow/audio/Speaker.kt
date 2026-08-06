package com.meetpatel.popgrow.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Speaks short words aloud — a colour, a number, a letter — using the phone's
 * built-in text-to-speech engine. That engine lives on the device, so this adds
 * no audio files to the APK and needs no internet: the app stays tiny and fully
 * offline. If no engine is available it simply stays silent and the game plays on.
 */
class Speaker(context: Context) {

    private var tts: TextToSpeech? = null
    @Volatile private var ready = false

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
                    ready = true
                }
            }
        }.getOrNull()
    }

    fun say(text: String, flush: Boolean = true) {
        if (!ready || text.isBlank()) return
        val mode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        runCatching { tts?.speak(text, mode, null, text) }
    }

    fun shutdown() {
        runCatching { tts?.shutdown() }
        tts = null
        ready = false
    }
}
