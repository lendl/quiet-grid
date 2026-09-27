package com.quietgrid.app.core.themes

import com.quietgrid.app.R

fun themeLabelRes(themeId: String): Int = when (themeId) {
    "animals" -> R.string.theme_animals
    "food" -> R.string.theme_food
    "nature" -> R.string.theme_nature
    "weather" -> R.string.theme_weather
    "sports" -> R.string.theme_sports
    "clothing" -> R.string.theme_clothing
    "transport" -> R.string.theme_transport
    "home" -> R.string.theme_home
    "professions" -> R.string.theme_professions
    "emotions" -> R.string.theme_emotions
    "space" -> R.string.theme_space
    "art" -> R.string.theme_art
    "bodyparts" -> R.string.theme_bodyparts
    "school" -> R.string.theme_school
    "music" -> R.string.theme_music
    "technology" -> R.string.theme_technology
    "geography" -> R.string.theme_geography
    "fantasy" -> R.string.theme_fantasy
    else -> R.string.theme_fallback
}

private val THEME_ICONS: Map<String, String> = mapOf(
    "animals" to "🐾",
    "food" to "🍎",
    "nature" to "🌿",
    "weather" to "⛅",
    "sports" to "⚽",
    "clothing" to "👕",
    "transport" to "🚗",
    "home" to "🏠",
    "professions" to "💼",
    "emotions" to "🙂",
    "space" to "🚀",
    "art" to "🎨",
    "bodyparts" to "🧍",
    "school" to "🏫",
    "music" to "🎵",
    "technology" to "💻",
    "geography" to "🗺️",
    "fantasy" to "🐉",
)

fun themeIcon(themeId: String): String? = THEME_ICONS[themeId]
