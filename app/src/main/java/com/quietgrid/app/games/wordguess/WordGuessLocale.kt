package com.quietgrid.app.games.wordguess

import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

val WORDGUESS_SUPPORTED_LOCALES = setOf("en", "de", "es", "fr", "nl")

fun currentWordGuessLocale(): String {
    val applied = AppCompatDelegate.getApplicationLocales().get(0)?.language
    val candidate = applied ?: Locale.getDefault().language
    return if (candidate in WORDGUESS_SUPPORTED_LOCALES) candidate else "en"
}
