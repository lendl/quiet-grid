package com.quietgrid.app.games.wordguess

import java.util.Locale

val WORDGUESS_SUPPORTED_LOCALES = setOf("en", "de", "es", "fr", "nl", "pl", "pt")

fun currentWordGuessLocale(puzzleLanguageOverride: String): String {
    val candidate = puzzleLanguageOverride.ifEmpty { Locale.getDefault().language }
    return if (candidate in WORDGUESS_SUPPORTED_LOCALES) candidate else "en"
}
