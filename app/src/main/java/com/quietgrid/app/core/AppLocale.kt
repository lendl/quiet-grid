package com.quietgrid.app.core

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import java.util.Locale

private const val PREFS_NAME = "app_locale"
private const val KEY_LANGUAGE_TAG = "language_tag"

object AppLocale {
    fun currentTag(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = context.getSystemService(LocaleManager::class.java)?.applicationLocales
            if (locales != null && !locales.isEmpty) return locales[0]?.language ?: ""
        }
        return prefs(context).getString(KEY_LANGUAGE_TAG, "") ?: ""
    }

    fun setLanguage(context: Context, tag: String) {
        prefs(context).edit().putString(KEY_LANGUAGE_TAG, tag).apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)?.applicationLocales =
                if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        }
    }

    fun wrap(context: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return context
        val tag = prefs(context).getString(KEY_LANGUAGE_TAG, "") ?: ""
        if (tag.isEmpty()) return context
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
