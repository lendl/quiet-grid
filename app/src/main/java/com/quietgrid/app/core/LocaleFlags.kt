package com.quietgrid.app.core

fun localeFlagEmoji(languageTag: String): String = when (languageTag) {
    "en" -> "🇬🇧"
    "nl" -> "🇳🇱"
    "de" -> "🇩🇪"
    "fr" -> "🇫🇷"
    "es" -> "🇪🇸"
    else -> "🌐"
}
