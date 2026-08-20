package com.quietgrid.app.games.wordsearch

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty

fun wordSearchDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.wordsearch_difficulty_easy
    Difficulty.MEDIUM -> R.string.wordsearch_difficulty_medium
    Difficulty.HARD -> R.string.wordsearch_difficulty_hard
    Difficulty.EXPERT -> R.string.wordsearch_difficulty_expert
}

fun wordSearchDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.wordsearch_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.wordsearch_difficulty_desc_medium
    Difficulty.HARD -> R.string.wordsearch_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.wordsearch_difficulty_desc_expert
}

fun wordSearchThemeLabel(clue: String): String =
    clue.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

private val WORDSEARCH_THEME_ICONS: Map<String, String> = mapOf(
    "animals" to "🐾", "dieren" to "🐾", "tiere" to "🐾", "animaux" to "🐾", "animales" to "🐾",
    "food" to "🍎", "eten" to "🍎", "essen" to "🍎", "nourriture" to "🍎", "comida" to "🍎",
    "nature" to "🌿", "natuur" to "🌿", "natur" to "🌿", "naturaleza" to "🌿",
    "weather" to "⛅",
    "sports" to "⚽", "sporten" to "⚽", "sport" to "⚽", "deportes" to "⚽",
    "clothing" to "👕", "kleding" to "👕", "kleidung" to "👕", "vetements" to "👕", "ropa" to "👕",
    "transport" to "🚗", "vervoer" to "🚗", "transporte" to "🚗",
    "home" to "🏠", "thuis" to "🏠", "zuhause" to "🏠", "maison" to "🏠", "hogar" to "🏠",
    "professions" to "💼", "beroepen" to "💼", "profesiones" to "💼", "metiers" to "💼",
    "emotions" to "🙂", "emotionen" to "🙂", "emociones" to "🙂",
    "eigenschappen" to "✨", "eigenschaften" to "✨", "qualites" to "✨", "cualidades" to "✨",
    "space" to "🚀", "ruimte" to "🚀", "weltraum" to "🚀", "espace" to "🚀", "espacio" to "🚀",
    "art" to "🎨",
    "bodyparts" to "🧍", "lichaamsdelen" to "🧍", "koerper" to "🧍", "corps" to "🧍", "cuerpo" to "🧍",
    "school" to "🏫", "escuela" to "🏫", "ecole" to "🏫", "schule" to "🏫",
    "music" to "🎵", "muziek" to "🎵", "musik" to "🎵", "musique" to "🎵", "musica" to "🎵",
    "technology" to "💻", "elektronik" to "🔌", "electronique" to "🔌", "electronica" to "🔌",
    "geography" to "🗺️",
    "fantasy" to "🐉",
    "familie" to "👪", "family" to "👪", "famille" to "👪", "familia" to "👪",
    "kleuren" to "🌈", "farben" to "🌈", "couleurs" to "🌈", "colores" to "🌈",
    "hobbys" to "🧩", "loisirs" to "🧩", "pasatiempos" to "🧩",
    "werkzeuge" to "🔧", "outils" to "🔧", "herramientas" to "🔧",
)

fun wordSearchThemeIcon(themeId: String): String? = WORDSEARCH_THEME_ICONS[themeId.lowercase()]
