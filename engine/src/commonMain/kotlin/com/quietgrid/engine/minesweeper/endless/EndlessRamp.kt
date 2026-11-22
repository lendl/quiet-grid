package com.quietgrid.engine.minesweeper.endless

import kotlin.math.min

const val ENDLESS_START_MINES = 9
const val ENDLESS_MINES_CAP = 13
const val ENDLESS_LEVELS_PER_MINE = 10
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
    val cluePairSteps: IntRange,
    val bottleneckSteps: IntRange,
)

fun endlessTargetsFor(level: Int): EndlessTargets {
    val mines = min(ENDLESS_MINES_CAP, ENDLESS_START_MINES + (level - 1) / ENDLESS_LEVELS_PER_MINE)
    val cluePairStep = steppedLevel(level, ENDLESS_CLUE_PAIR_FROM_LEVEL, ENDLESS_CLUE_PAIR_EVERY, ENDLESS_CLUE_PAIR_CAP)
    val bottleneckStep = steppedLevel(level, ENDLESS_BOTTLENECK_FROM_LEVEL, ENDLESS_BOTTLENECK_EVERY, ENDLESS_BOTTLENECK_CAP)
    return EndlessTargets(
        level = level,
        density = mines / ENDLESS_SECTION_CELLS.toDouble(),
        mines = mines,
        cluePairSteps = 0..cluePairStep + ENDLESS_CLUE_PAIR_MARGIN,
        bottleneckSteps = 0..bottleneckStep + ENDLESS_BOTTLENECK_MARGIN,
    )
}

private fun steppedLevel(level: Int, fromLevel: Int, every: Int, cap: Int): Int =
    if (level < fromLevel) 0 else min(cap, 1 + (level - fromLevel) / every)
