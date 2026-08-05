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

    private companion object {
        const val KEY_SOUND = "sound"
        const val KEY_HAPTICS = "haptics"
    }
}
