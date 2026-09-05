package com.quietgrid.app.games.wordsearch

import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.QuickStartContent
import com.quietgrid.app.ui.components.QuickStartExample

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

val WordSearchQuickStart = QuickStartContent(
    goalRes = R.string.wordsearch_quickstart_goal,
    bulletRes = listOf(R.string.wordsearch_quickstart_bullet_1, R.string.wordsearch_quickstart_bullet_2),
    examples = listOf(
        QuickStartExample(R.string.wordsearch_quickstart_example_1_word, R.string.wordsearch_quickstart_example_1_hint),
    ),
    hookRes = R.string.wordsearch_quickstart_hook,
)

fun wordSearchThemeLabel(clue: String): String =
    clue.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

private val WORDSEARCH_THEME_ICONS: Map<String, String> = mapOf(
    "animals" to "🐾", "dieren" to "🐾", "tiere" to "🐾", "animaux" to "🐾", "animales" to "🐾", "zwierzeta" to "🐾",
    "food" to "🍎", "eten" to "🍎", "essen" to "🍎", "nourriture" to "🍎", "comida" to "🍎", "jedzenie" to "🍎",
    "nature" to "🌿", "natuur" to "🌿", "natur" to "🌿", "naturaleza" to "🌿", "przyroda" to "🌿",
    "weather" to "⛅",
    "sports" to "⚽", "sporten" to "⚽", "sport" to "⚽", "deportes" to "⚽",
    "clothing" to "👕", "kleding" to "👕", "kleidung" to "👕", "vetements" to "👕", "ropa" to "👕", "ubrania" to "👕",
    "transport" to "🚗", "vervoer" to "🚗", "transporte" to "🚗",
    "home" to "🏠", "thuis" to "🏠", "zuhause" to "🏠", "maison" to "🏠", "hogar" to "🏠", "dom" to "🏠",
    "professions" to "💼", "beroepen" to "💼", "profesiones" to "💼", "metiers" to "💼", "zawody" to "💼",
    "emotions" to "🙂", "emotionen" to "🙂", "emociones" to "🙂", "emocje" to "🙂",
    "eigenschappen" to "✨", "eigenschaften" to "✨", "qualites" to "✨", "cualidades" to "✨", "cechy" to "✨",
    "space" to "🚀", "ruimte" to "🚀", "weltraum" to "🚀", "espace" to "🚀", "espacio" to "🚀", "kosmos" to "🚀",
    "art" to "🎨",
    "bodyparts" to "🧍", "lichaamsdelen" to "🧍", "koerper" to "🧍", "corps" to "🧍", "cuerpo" to "🧍", "cialo" to "🧍",
    "school" to "🏫", "escuela" to "🏫", "ecole" to "🏫", "schule" to "🏫", "szkola" to "🏫",
    "music" to "🎵", "muziek" to "🎵", "musik" to "🎵", "musique" to "🎵", "musica" to "🎵", "muzyka" to "🎵",
    "technology" to "💻", "elektronik" to "🔌", "electronique" to "🔌", "electronica" to "🔌", "elektronika" to "🔌",
    "geography" to "🗺️",
    "fantasy" to "🐉",
    "familie" to "👪", "family" to "👪", "famille" to "👪", "familia" to "👪", "rodzina" to "👪",
    "kleuren" to "🌈", "farben" to "🌈", "couleurs" to "🌈", "colores" to "🌈", "kolory" to "🌈",
    "hobbys" to "🧩", "loisirs" to "🧩", "pasatiempos" to "🧩", "hobby" to "🧩",
    "werkzeuge" to "🔧", "outils" to "🔧", "herramientas" to "🔧", "narzedzia" to "🔧",
)

fun wordSearchThemeIcon(themeId: String): String? = WORDSEARCH_THEME_ICONS[themeId.lowercase()]
