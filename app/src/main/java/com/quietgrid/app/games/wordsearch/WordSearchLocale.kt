package com.quietgrid.app.games.wordsearch

import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

val WORDSEARCH_SUPPORTED_LOCALES = setOf("en", "nl", "de")

fun currentWordSearchLocale(): String {
    val applied = AppCompatDelegate.getApplicationLocales().get(0)?.language
    val candidate = applied ?: Locale.getDefault().language
    return if (candidate in WORDSEARCH_SUPPORTED_LOCALES) candidate else "en"
}
