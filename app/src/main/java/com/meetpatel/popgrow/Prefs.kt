package com.meetpatel.popgrow

import android.content.Context
import androidx.core.content.edit

/**
 * The only thing the app persists: two booleans. No identifiers, no counters,
 * no timestamps, nothing that could describe a person.
 */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("pop_and_grow", Context.MODE_PRIVATE)

    var soundEnabled: Boolean
        get() = sp.getBoolean(KEY_SOUND, true)
        set(value) = sp.edit { putBoolean(KEY_SOUND, value) }

    var hapticsEnabled: Boolean
        get() = sp.getBoolean(KEY_HAPTICS, true)
        set(value) = sp.edit { putBoolean(KEY_HAPTICS, value) }

    /** The background soundscape (wind, birds, crickets), separate from the
     * musical pops so a grown-up can quieten one without losing the other. */
    var musicEnabled: Boolean
        get() = sp.getBoolean(KEY_MUSIC, true)
        set(value) = sp.edit { putBoolean(KEY_MUSIC, value) }

    /** Bolder outlines and a calmer background — helps children with low vision
     * (and anyone playing in bright sunlight) pick the balloons out. */
    var highContrast: Boolean
        get() = sp.getBoolean(KEY_CONTRAST, false)
        set(value) = sp.edit { putBoolean(KEY_CONTRAST, value) }

    /** How fast the balloons rise, 0.5 (gentle) .. 1.5 (lively). */
    var speed: Float
        get() = sp.getFloat(KEY_SPEED, 1f).coerceIn(SPEED_MIN, SPEED_MAX)
        set(value) = sp.edit { putFloat(KEY_SPEED, value.coerceIn(SPEED_MIN, SPEED_MAX)) }

    /** How big the balloons are, 1.0 (normal) .. 1.5 (extra large). It only ever
     * grows them, so the minimum touch target can never be made unsafe. */
    var size: Float
        get() = sp.getFloat(KEY_SIZE, 1f).coerceIn(SIZE_MIN, SIZE_MAX)
        set(value) = sp.edit { putFloat(KEY_SIZE, value.coerceIn(SIZE_MIN, SIZE_MAX)) }

    /**
     * Whether the pointing-hand hint has already been shown for a mode. It
     * appears only the first few taps of a child's first ever visit to that
     * mode, then never again — a flag, not a history of anything personal.
     */
    fun tutorialSeen(mode: String): Boolean = sp.getBoolean("$KEY_TUTORIAL$mode", false)

    fun markTutorialSeen(mode: String) = sp.edit { putBoolean("$KEY_TUTORIAL$mode", true) }

    /** Show the first-run pointing hand again in every mode. */
    fun resetTutorials() = sp.edit {
        sp.all.keys.filter { it.startsWith(KEY_TUTORIAL) }.forEach { remove(it) }
    }

    companion object {
        const val SPEED_MIN = 0.5f
        const val SPEED_MAX = 1.5f
        const val SIZE_MIN = 1.0f
        const val SIZE_MAX = 1.5f

        private const val KEY_SOUND = "sound"
        private const val KEY_HAPTICS = "haptics"
        private const val KEY_MUSIC = "music"
        private const val KEY_CONTRAST = "contrast"
        private const val KEY_SPEED = "speed"
        private const val KEY_SIZE = "size"
        private const val KEY_TUTORIAL = "tutorial_"
    }
}
