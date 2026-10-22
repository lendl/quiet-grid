package com.quietgrid.engine.minesweeper.endless

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

const val ENDLESS_DENSITY_START = 0.06
const val ENDLESS_DENSITY_STEP = 0.0025
const val ENDLESS_DENSITY_CAP = 0.16
const val ENDLESS_CASCADE_FROM_LEVEL = 20
const val ENDLESS_CASCADE_START_LIMIT = 40
const val ENDLESS_CASCADE_MIN_LIMIT = 25
const val ENDLESS_CLUE_PAIR_FROM_LEVEL = 30
const val ENDLESS_CLUE_PAIR_EVERY = 4
const val ENDLESS_CLUE_PAIR_CAP = 6
const val ENDLESS_CLUE_PAIR_MARGIN = 2
const val ENDLESS_BOTTLENECK_FROM_LEVEL = 40
const val ENDLESS_BOTTLENECK_EVERY = 5
const val ENDLESS_BOTTLENECK_CAP = 6
const val ENDLESS_BOTTLENECK_MARGIN = 3

data class EndlessTargets(
    val level: Int,
    val density: Double,
    val mines: Int,
    val largestCascadeLimit: Int?,
    val cluePairSteps: IntRange,
    val bottleneckSteps: IntRange,
)

fun endlessTargetsFor(level: Int): EndlessTargets {
    val density = min(ENDLESS_DENSITY_CAP, ENDLESS_DENSITY_START + ENDLESS_DENSITY_STEP * (level - 1))
    val cascadeLimit = if (level < ENDLESS_CASCADE_FROM_LEVEL) {
        null
    } else {
        max(ENDLESS_CASCADE_MIN_LIMIT, ENDLESS_CASCADE_START_LIMIT - (level - ENDLESS_CASCADE_FROM_LEVEL))
    }
    val cluePairStep = steppedLevel(level, ENDLESS_CLUE_PAIR_FROM_LEVEL, ENDLESS_CLUE_PAIR_EVERY, ENDLESS_CLUE_PAIR_CAP)
    val bottleneckStep = steppedLevel(level, ENDLESS_BOTTLENECK_FROM_LEVEL, ENDLESS_BOTTLENECK_EVERY, ENDLESS_BOTTLENECK_CAP)
    return EndlessTargets(
        level = level,
        density = density,
        mines = (ENDLESS_SECTION_CELLS * density).roundToInt(),
        largestCascadeLimit = cascadeLimit,
        cluePairSteps = 0..cluePairStep + ENDLESS_CLUE_PAIR_MARGIN,
        bottleneckSteps = 0..bottleneckStep + ENDLESS_BOTTLENECK_MARGIN,
    )
}

private fun steppedLevel(level: Int, fromLevel: Int, every: Int, cap: Int): Int =
    if (level < fromLevel) 0 else min(cap, 1 + (level - fromLevel) / every)
