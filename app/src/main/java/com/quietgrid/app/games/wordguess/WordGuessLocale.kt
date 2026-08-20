package com.quietgrid.app.games.wordguess

import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

val WORDGUESS_SUPPORTED_LOCALES = setOf("en", "de", "es", "fr", "nl")

fun currentWordGuessLocale(puzzleLanguageOverride: String): String {
    val candidate = puzzleLanguageOverride.ifEmpty {
        AppCompatDelegate.getApplicationLocales().get(0)?.language ?: Locale.getDefault().language
    }
    return if (candidate in WORDGUESS_SUPPORTED_LOCALES) candidate else "en"
}
