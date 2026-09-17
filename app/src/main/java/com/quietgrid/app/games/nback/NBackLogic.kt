package com.quietgrid.app.games.nback

import com.quietgrid.app.core.Difficulty
import kotlin.math.roundToInt
import kotlin.random.Random

const val NBACK_TOTAL_TRIALS = 32
const val NBACK_GRID_SIZE = 9
const val NBACK_STIMULUS_ON_MS = 500L
const val NBACK_TAP_FEEDBACK_MS = 200L
const val NBACK_START_DELAY_MS = 1500L
private const val NBACK_TARGET_RATE = 0.28
private const val NBACK_MAX_REACTION_TIME_MS = 2500.0
private const val NBACK_MAX_REACTION_TIME_BONUS = 1000

private val NBACK_TIER_CONFIG = mapOf(
    Difficulty.EASY to NBackDifficultyConfig(n = 1, intervalMs = 2500L, modality = NBackModality.SPATIAL),
    Difficulty.MEDIUM to NBackDifficultyConfig(n = 2, intervalMs = 2000L, modality = NBackModality.SPATIAL),
    Difficulty.HARD to NBackDifficultyConfig(n = 3, intervalMs = 1500L, modality = NBackModality.SPATIAL),
    Difficulty.EXPERT to NBackDifficultyConfig(n = 4, intervalMs = 1250L, modality = NBackModality.SPATIAL),
)

fun generateNBackTrials(n: Int, totalTrials: Int = NBACK_TOTAL_TRIALS, random: Random = Random.Default): List<NBackTrial> {
    val positions = MutableList(totalTrials) { -1 }
    for (index in 0 until n) {
        positions[index] = random.nextInt(NBACK_GRID_SIZE)
    }
    val eligibleIndices = (n until totalTrials).toList()
    val targetCount = (eligibleIndices.size * NBACK_TARGET_RATE).roundToInt()
    val targetIndices = eligibleIndices.shuffled(random).take(targetCount).toSet()
    for (index in n until totalTrials) {
        positions[index] = if (index in targetIndices) {
            positions[index - n]
        } else {
            var candidate = random.nextInt(NBACK_GRID_SIZE)
            while (candidate == positions[index - n]) {
                candidate = random.nextInt(NBACK_GRID_SIZE)
            }
            candidate
        }
    }
    return positions.mapIndexed { index, position ->
        NBackTrial(position = position, isTargetMatch = index >= n && position == positions[index - n])
    }
}

fun createNBackSession(difficulty: Difficulty): NBackSession {
    val config = NBACK_TIER_CONFIG.getValue(difficulty)
    return NBackSession(
        difficulty = difficulty,
        config = config,
        trials = generateNBackTrials(config.n),
        currentIndex = -1,
    )
}

fun computeNBackScore(trials: List<NBackTrial>): Int {
    val totalTargets = trials.count { it.isTargetMatch }
    if (totalTargets == 0) return 0
    val hits = nbackHits(trials)
    val falsePositives = nbackFalsePositives(trials)
    val accuracyScore = (maxOf(0, hits - falsePositives).toDouble() / totalTargets * 10000).roundToInt()
    val hitReactionTimes = trials.filter { it.isTargetMatch && it.responded }.mapNotNull { it.reactionTimeMs }
    val reactionTimeBonus = if (hitReactionTimes.isEmpty()) {
        0
    } else {
        val avgReactionTimeMs = hitReactionTimes.average().coerceIn(0.0, NBACK_MAX_REACTION_TIME_MS)
        ((NBACK_MAX_REACTION_TIME_MS - avgReactionTimeMs) / NBACK_MAX_REACTION_TIME_MS * NBACK_MAX_REACTION_TIME_BONUS).roundToInt()
    }
    return accuracyScore + reactionTimeBonus
}

fun nbackAccuracyPct(trials: List<NBackTrial>): Int {
    if (trials.isEmpty()) return 100
    val correct = trials.count { it.isTargetMatch == it.responded }
    return (correct.toDouble() / trials.size * 100).roundToInt()
}

fun nbackHits(trials: List<NBackTrial>): Int = trials.count { it.isTargetMatch && it.responded }
fun nbackMisses(trials: List<NBackTrial>): Int = trials.count { it.isTargetMatch && !it.responded }
fun nbackFalsePositives(trials: List<NBackTrial>): Int = trials.count { !it.isTargetMatch && it.responded }

fun nbackAverageReactionTimeMs(trials: List<NBackTrial>): Int {
    val times = trials.filter { it.isTargetMatch && it.responded }.mapNotNull { it.reactionTimeMs }
    return if (times.isEmpty()) 0 else times.average().roundToInt()
}

fun nbackHasMeaningfulProgress(session: NBackSession): Boolean = false
