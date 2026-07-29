package com.quietgrid.engine.wordsearch

import com.quietgrid.engine.core.Difficulty
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

data class WordSearchQualityMetrics(val overlapRatio: Double, val directionEntropy: Double, val score: Double)

private fun clamp01(value: Double): Double = max(0.0, min(1.0, value))
private fun log2(value: Double): Double = ln(value) / ln(2.0)

private fun calculateDirectionEntropy(placements: List<WordPlacement>): Double {
    if (placements.isEmpty()) return 0.0
    val counts = placements.groupingBy { it.direction }.eachCount()
    val total = placements.size
    var entropy = 0.0
    counts.values.forEach { count ->
        val probability = count.toDouble() / total
        entropy += -(probability * log2(probability))
    }
    val maxEntropy = log2(max(2, counts.size).toDouble())
    return if (maxEntropy <= 0.0) 0.0 else clamp01(entropy / maxEntropy)
}

fun buildQualityMetrics(placements: List<WordPlacement>): WordSearchQualityMetrics {
    val totalWordLetters = placements.sumOf { it.word.length }
    val occupied = mutableSetOf<Int>()
    placements.forEach { placement -> placement.positions.forEach { occupied.add(toGridKey(it)) } }
    val overlapRatio = if (totalWordLetters == 0) 0.0 else clamp01((totalWordLetters - occupied.size).toDouble() / totalWordLetters)
    val directionEntropy = calculateDirectionEntropy(placements)
    val score = clamp01(overlapRatio * 0.5 + directionEntropy * 0.5)
    return WordSearchQualityMetrics(overlapRatio, directionEntropy, score)
}

fun passesQualityThreshold(difficulty: Difficulty, quality: WordSearchQualityMetrics): Boolean {
    val threshold = WORD_SEARCH_QUALITY_THRESHOLDS.getValue(difficulty)
    return quality.score >= threshold.minScore && quality.overlapRatio >= threshold.minOverlapRatio && quality.directionEntropy >= threshold.minDirectionEntropy
}

fun buildDifficultyRatedScore(difficulty: Difficulty, qualityScore: Double): Double {
    val threshold = WORD_SEARCH_QUALITY_THRESHOLDS.getValue(difficulty)
    val normalized = clamp01((qualityScore - threshold.minScore) / max(0.001, 1 - threshold.minScore))
    val tierBase = mapOf(Difficulty.EASY to 0, Difficulty.MEDIUM to 25, Difficulty.HARD to 50, Difficulty.EXPERT to 75)
    return Math.round((tierBase.getValue(difficulty) + normalized * 24.9) * 10) / 10.0
}
