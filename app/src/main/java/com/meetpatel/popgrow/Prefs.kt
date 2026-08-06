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

    /**
     * Whether the pointing-hand hint has already been shown for a mode. It
     * appears only the first few taps of a child's first ever visit to that
     * mode, then never again — a flag, not a history of anything personal.
     */
    fun tutorialSeen(mode: String): Boolean = sp.getBoolean("$KEY_TUTORIAL$mode", false)

    fun markTutorialSeen(mode: String) = sp.edit { putBoolean("$KEY_TUTORIAL$mode", true) }

    private companion object {
        const val KEY_SOUND = "sound"
        const val KEY_HAPTICS = "haptics"
        const val KEY_TUTORIAL = "tutorial_"
    }
}
