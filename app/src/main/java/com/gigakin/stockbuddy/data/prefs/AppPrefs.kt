package com.gigakin.stockbuddy.data.prefs

import android.content.Context
import androidx.core.content.edit

/** Simple scalar app config — for field configuration state. */
class AppPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("stockbuddy_prefs", Context.MODE_PRIVATE)

    var fieldConfigCompleted: Boolean
        get() = prefs.getBoolean(KEY_FIELD_CONFIG_DONE, false)
        set(value) = prefs.edit { putBoolean(KEY_FIELD_CONFIG_DONE, value) }

    companion object {
        private const val KEY_FIELD_CONFIG_DONE = "field_config_completed"
    }
}
