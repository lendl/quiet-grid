package com.quietgrid.cli.wordsearch

import com.quietgrid.cli.themes.loadSharedThemes
import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.themeclear.ThemeClearTheme
import com.quietgrid.engine.wordsearch.*

private const val MAX_HIDDEN_WORD_ATTEMPTS = 5

private fun normalizeWordToken(value: String): String = value.uppercase().replace(Regex("[^A-Z]"), "")

private fun pickLanguage(preferredLanguages: List<String>?): String {
    val themes = loadSharedThemes()
    if (!preferredLanguages.isNullOrEmpty()) {
        val supported = preferredLanguages.filter { themes.containsKey(it) }
        if (supported.isNotEmpty()) return supported.random()
    }
    return themes.keys.random()
}

private fun pickTheme(language: String, themeId: String?): ThemeClearTheme {
    val themes = loadSharedThemes()
    val languageThemes = themes[language] ?: themes.getValue("en")
    if (themeId == null) return languageThemes.random()
    return languageThemes.firstOrNull { it.themeId == themeId } ?: error("Unknown wordsearch theme '$themeId' for language '$language'")
}

private fun buildDiversitySignature(placements: List<WordPlacement>, hiddenPositions: List<WSCellRef>): String {
    val wordAnchors = placements.map { "${it.word}:${it.start.row},${it.start.col},${it.direction}" }.sorted().joinToString("|")
    val hiddenAnchor = hiddenPositions.map { "${it.row},${it.col}" }.sorted().joinToString(",")
    return "$wordAnchors::$hiddenAnchor"
}

fun generateWordSearchPuzzle(
    rows: Int,
    cols: Int,
    difficulty: Difficulty,
    preferredLanguages: List<String>? = null,
    themeId: String? = null,
): WordSearchPuzzleEntry? {
    val config = WORD_SEARCH_DIFFICULTY_CONFIG.getValue(difficulty)
    val language = pickLanguage(preferredLanguages)
    val theme = pickTheme(language, themeId)

    val maxFit = maxOf(rows, cols)
    val normalizedThemeWords = theme.words.map { normalizeWordToken(it) }.distinct()
    val hiddenWordPool = buildHiddenWordPool(normalizedThemeWords)

    var hiddenWord: String? = null
    var reservedHiddenWord: ReservedHiddenWord? = null
    var placementResult: PlacementResult? = null

    for (attempt in 0 until MAX_HIDDEN_WORD_ATTEMPTS) {
        val candidateHiddenWord = pickHiddenWord(hiddenWordPool, rows, cols) ?: return null
        val candidateReserved = reserveHiddenWordCells(candidateHiddenWord, rows, cols)
        val candidateReservedCells = candidateReserved.positions.map { toGridKey(it) }.toSet()

        val candidateWordPool = normalizedThemeWords.filter { it != candidateHiddenWord && it.length in 3..maxFit }
        if (candidateWordPool.isEmpty()) return null

        val candidateResult = buildFullCoverageGrid(rows, cols, candidateWordPool, candidateReservedCells, config.allowedDirections, config.overlapFrequency)
        if (candidateResult != null) {
            hiddenWord = candidateHiddenWord
            reservedHiddenWord = candidateReserved
            placementResult = candidateResult
            break
        }
    }

    val result = placementResult ?: return null
    val hidden = hiddenWord ?: return null
    val reserved = reservedHiddenWord ?: return null
    val (grid, placements) = result

    if (hasCoverageViolation(placements)) return null

    val quality = buildQualityMetrics(placements)
    if (!passesQualityThreshold(difficulty, quality)) return null

    reserved.positions.forEachIndexed { index, cell -> grid[cell.row][cell.col] = hidden[index].toString() }
    val hasGap = grid.any { row -> row.any { it == "" || it == "#" } }
    if (hasGap) return null

    val placedWords = placements.map { it.word }.toSet()
    val alternativeHiddenWords = hiddenWordPool.filter { it.length == hidden.length && it != hidden && it !in placedWords }.shuffled()
    val finalHiddenWord = fillGhostFreeHiddenWord(grid, placements, reserved.positions, listOf(hidden) + alternativeHiddenWords) ?: return null

    val diversitySignature = buildDiversitySignature(placements, reserved.positions)

    return WordSearchPuzzleEntry(
        id = "ws-${"$language-${theme.themeId}-$difficulty-$diversitySignature".hashCode()}",
        difficulty = difficulty.key,
        rows = rows,
        cols = cols,
        themeId = theme.themeId,
        grid = grid.map { it.toList() },
        words = placements.map { WSWordEntry(it.id, it.word, it.positions) },
        hiddenWord = WSHiddenWord(finalHiddenWord, theme.themeId, reserved.positions),
        locale = language,
    )
}
