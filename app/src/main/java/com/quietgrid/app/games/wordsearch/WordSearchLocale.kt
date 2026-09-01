package com.quietgrid.app.games.wordsearch

import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

val WORDSEARCH_SUPPORTED_LOCALES = setOf("en", "nl", "de", "es", "fr")

fun currentWordSearchLocale(puzzleLanguageOverride: String): String {
    val candidate = puzzleLanguageOverride.ifEmpty {
        AppCompatDelegate.getApplicationLocales().get(0)?.language ?: Locale.getDefault().language
    }
    return if (candidate in WORDSEARCH_SUPPORTED_LOCALES) candidate else "en"
}
