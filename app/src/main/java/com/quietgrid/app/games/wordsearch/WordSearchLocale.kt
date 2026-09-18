package com.quietgrid.app.games.wordsearch

import java.util.Locale

val WORDSEARCH_SUPPORTED_LOCALES = setOf("en", "nl", "de", "es", "fr", "pl", "pt")

fun currentWordSearchLocale(puzzleLanguageOverride: String): String {
    val candidate = puzzleLanguageOverride.ifEmpty { Locale.getDefault().language }
    return if (candidate in WORDSEARCH_SUPPORTED_LOCALES) candidate else "en"
}
