package com.quietgrid.app.games.wordswap

import java.util.Locale

val WORDSWAP_SUPPORTED_LOCALES = setOf("en", "de", "es", "fr", "it", "nl", "pl", "pt")

fun currentWordSwapLocale(puzzleLanguageOverride: String): String {
    val candidate = puzzleLanguageOverride.ifEmpty { Locale.getDefault().language }
    return if (candidate in WORDSWAP_SUPPORTED_LOCALES) candidate else "en"
}

fun wordSwapLocaleOfId(id: String): String =
    id.split('-').getOrNull(1)?.takeIf { it in WORDSWAP_SUPPORTED_LOCALES } ?: "en"

fun wordSwapBankAsset(locale: String): String =
    if (locale == "en") "wordswap_puzzles.json" else "wordswap_puzzles_$locale.json"
