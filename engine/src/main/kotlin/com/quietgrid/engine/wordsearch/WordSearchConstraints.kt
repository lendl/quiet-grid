package com.quietgrid.engine.wordsearch

import com.quietgrid.engine.core.Difficulty

data class WordSearchSizeRange(val rowsMin: Int, val rowsMax: Int, val colsMin: Int, val colsMax: Int)
data class WordSearchDifficultyConfig(val sizeRange: WordSearchSizeRange, val allowedDirections: List<WordSearchDirection>, val overlapFrequency: Double)

val WORD_SEARCH_DIFFICULTY_CONFIG: Map<Difficulty, WordSearchDifficultyConfig> = mapOf(
    Difficulty.EASY to WordSearchDifficultyConfig(
        WordSearchSizeRange(8, 10, 8, 10), listOf(WordSearchDirection.RIGHT, WordSearchDirection.DOWN), 0.15,
    ),
    Difficulty.MEDIUM to WordSearchDifficultyConfig(
        WordSearchSizeRange(10, 12, 10, 12),
        listOf(WordSearchDirection.RIGHT, WordSearchDirection.LEFT, WordSearchDirection.DOWN, WordSearchDirection.UP, WordSearchDirection.DOWN_RIGHT, WordSearchDirection.UP_RIGHT),
        0.28,
    ),
    Difficulty.HARD to WordSearchDifficultyConfig(
        WordSearchSizeRange(12, 14, 12, 14),
        listOf(WordSearchDirection.RIGHT, WordSearchDirection.LEFT, WordSearchDirection.DOWN, WordSearchDirection.UP, WordSearchDirection.DOWN_RIGHT, WordSearchDirection.DOWN_LEFT, WordSearchDirection.UP_RIGHT, WordSearchDirection.UP_LEFT),
        0.40,
    ),
    Difficulty.EXPERT to WordSearchDifficultyConfig(
        WordSearchSizeRange(14, 16, 14, 16),
        listOf(WordSearchDirection.RIGHT, WordSearchDirection.LEFT, WordSearchDirection.DOWN, WordSearchDirection.UP, WordSearchDirection.DOWN_RIGHT, WordSearchDirection.DOWN_LEFT, WordSearchDirection.UP_RIGHT, WordSearchDirection.UP_LEFT),
        0.55,
    ),
)

data class WordSearchQualityThreshold(val minScore: Double, val minOverlapRatio: Double, val minDirectionEntropy: Double)

val WORD_SEARCH_QUALITY_THRESHOLDS: Map<Difficulty, WordSearchQualityThreshold> = mapOf(
    Difficulty.EASY to WordSearchQualityThreshold(0.20, 0.02, 0.0),
    Difficulty.MEDIUM to WordSearchQualityThreshold(0.25, 0.04, 0.2),
    Difficulty.HARD to WordSearchQualityThreshold(0.30, 0.06, 0.3),
    Difficulty.EXPERT to WordSearchQualityThreshold(0.35, 0.08, 0.35),
)

/** rows x cols cross-product for [difficulty]'s size range -- ranges intentionally overlap at difficulty boundaries. */
fun wordSearchAllowedSizes(difficulty: Difficulty): List<Pair<Int, Int>> {
    val range = WORD_SEARCH_DIFFICULTY_CONFIG.getValue(difficulty).sizeRange
    return (range.rowsMin..range.rowsMax).flatMap { rows -> (range.colsMin..range.colsMax).map { cols -> rows to cols } }
}
