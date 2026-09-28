package com.quietgrid.engine.themeclear

import kotlinx.serialization.Serializable

@Serializable
data class ThemeClearBankEntry(
    val difficulty: String,
    val themeId: String,
    val grid: List<String>,
    val words: List<String>,
    val locale: String = "en",
) {
    val id: String
        get() = themeClearPuzzleId(difficulty, themeId, grid)
}

fun themeClearPuzzleId(difficulty: String, themeId: String, grid: List<String>): String =
    "tc-$difficulty-$themeId-${grid.joinToString("").lowercase()}"

fun ThemeClearBankEntry.toPuzzleEntry(): ThemeClearPuzzleEntry = ThemeClearPuzzleEntry(
    id = id,
    difficulty = difficulty,
    themeId = themeId,
    rows = grid.size,
    cols = grid.firstOrNull()?.length ?: 0,
    grid = grid,
    words = words,
    locale = locale,
)

fun ThemeClearPuzzleEntry.toBankEntry(): ThemeClearBankEntry {
    val bankEntry = ThemeClearBankEntry(difficulty, themeId, grid, words, locale)
    require(bankEntry.toPuzzleEntry() == this) { "Theme clear puzzle $id cannot be stored as a bank entry without losing data" }
    return bankEntry
}
