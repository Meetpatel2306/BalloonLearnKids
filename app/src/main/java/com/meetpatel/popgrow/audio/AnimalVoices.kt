package com.meetpatel.popgrow.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * Plays real animal recordings — if any have been added to the project.
 *
 * Drop sound files into `app/src/main/res/raw/` named `animal_cow.ogg`,
 * `animal_dog.ogg` and so on (`.ogg`, `.mp3` and `.wav` all work), and each
 * animal will call out in its own real voice when a child finds it. Anything
 * missing simply falls back to the spoken name and sound ("The cow says moo"),
 * so the game is complete either way and nothing is ever downloaded at runtime.
 *
 * Only use recordings you are allowed to ship — public-domain or CC0 clips, or
 * ones you record yourself. See ANIMAL_SOUNDS.md in the project root.
 */
class AnimalVoices(context: Context) {

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
    private var released = false

    init {
        pool.setOnLoadCompleteListener { _, id, status ->
            if (status == 0) synchronized(ready) { ready.add(id) }
        }
        // Look for a raw resource per animal; absent ones are simply skipped.
        val res = context.resources
        NAMES.forEach { name ->
            val resId = runCatching {
                res.getIdentifier("animal_${name.lowercase()}", "raw", context.packageName)
            }.getOrDefault(0)
            if (resId != 0) {
                runCatching { ids[name] = pool.load(context, resId, 1) }
            }
        }
    }

    /** True if a real recording for [name] existed and started playing. */
    fun play(name: String, volume: Float = 0.9f): Boolean {
        if (released) return false
        val id = ids[name] ?: return false
        synchronized(ready) { if (id !in ready) return false }
        val v = volume.coerceIn(0f, 1f)
        return pool.play(id, v, v, 1, 0, 1f) != 0
    }

    /** Whether any recordings at all were found — handy for a settings note. */
    fun hasRecordings(): Boolean = ids.isNotEmpty()

    fun release() {
        if (released) return
        released = true
        pool.release()
    }

    private companion object {
        val NAMES = listOf(
            "Cat", "Dog", "Cow", "Pig", "Duck", "Rabbit", "Bear",
            "Lion", "Monkey", "Elephant", "Frog", "Fish", "Bird", "Horse",
        )
    }
}
