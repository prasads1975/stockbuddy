package com.gigakin.stockbuddy.data.prefs

import android.content.Context
import androidx.core.content.edit
import com.gigakin.stockbuddy.util.ArticleIdMode

/** Simple scalar app config (Article ID mode) — not worth a single-row DB table for this. */
class AppPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("stockbuddy_prefs", Context.MODE_PRIVATE)

    var articleIdMode: ArticleIdMode
        get() = ArticleIdMode.valueOf(
            prefs.getString(KEY_ARTICLE_ID_MODE, ArticleIdMode.OPTIONAL.name) ?: ArticleIdMode.OPTIONAL.name
        )
        set(value) = prefs.edit { putString(KEY_ARTICLE_ID_MODE, value.name) }

    var fieldConfigCompleted: Boolean
        get() = prefs.getBoolean(KEY_FIELD_CONFIG_DONE, false)
        set(value) = prefs.edit { putBoolean(KEY_FIELD_CONFIG_DONE, value) }

    companion object {
        private const val KEY_ARTICLE_ID_MODE = "article_id_mode"
        private const val KEY_FIELD_CONFIG_DONE = "field_config_completed"
    }
}
