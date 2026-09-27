package com.quietgrid.engine.themeclear

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.text.Normalizer

const val THEMECLEAR_MIN_WORD_LENGTH = 3

@Serializable
data class ThemeClearTheme(val themeId: String, val words: List<String>)

@Serializable
data class ThemeClearMetrics(
    val finishCount: Int,
    val trapWords: Int,
    val spellableWords: Int,
    val blindSuccessRate: Double = 0.0,
) {
    val trapRate: Double
        get() = if (spellableWords == 0) 0.0 else trapWords.toDouble() / spellableWords
}

@Serializable
data class ThemeClearPuzzleEntry(
    val id: String,
    val difficulty: String,
    val themeId: String,
    val rows: Int,
    val cols: Int,
    val grid: List<String>,
    val words: List<String>,
    val metrics: ThemeClearMetrics,
    val locale: String = "en",
)

private val combiningMarks = Regex("\\p{Mn}+")

fun normalizeThemeClearWord(value: String): String =
    Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        .replace(combiningMarks, "")
        .uppercase()
        .filter { it in 'A'..'Z' }

private val themeClearJson = Json { ignoreUnknownKeys = true }

fun parseThemeClearThemes(text: String): Map<String, List<ThemeClearTheme>> =
    themeClearJson.decodeFromString(MapSerializer(String.serializer(), ListSerializer(ThemeClearTheme.serializer())), text)
