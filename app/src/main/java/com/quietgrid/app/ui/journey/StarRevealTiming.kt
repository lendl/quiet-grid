package com.quietgrid.app.ui.journey

import kotlin.math.min
import kotlin.math.pow

const val GENTLE_POP_START_MS = 350
const val GENTLE_POP_STAGGER_MS = 380
const val GENTLE_POP_DURATION_MS = 460
const val GENTLE_POP_OVERSHOOT = 1.6f

val STAR_REVEAL_SLOTS = listOf(1, 0, 2)

data class StarFrame(val scale: Float, val alpha: Float)

private fun revealStartMs(revealIndex: Int): Long = (GENTLE_POP_START_MS + revealIndex * GENTLE_POP_STAGGER_MS).toLong()

private fun easeOutBack(x: Float): Float {
    val c = GENTLE_POP_OVERSHOOT
    return 1f + (c + 1f) * (x - 1f).pow(3) + c * (x - 1f).pow(2)
}

fun gentlePopFrame(elapsedMs: Long, revealIndex: Int): StarFrame {
    val progress = ((elapsedMs - revealStartMs(revealIndex)).toFloat() / GENTLE_POP_DURATION_MS).coerceIn(0f, 1f)
    if (progress == 0f) return StarFrame(0f, 0f)
    return StarFrame(easeOutBack(progress), min(1f, progress * 3f))
}

fun gentlePopTotalMs(earned: Int): Long = if (earned <= 0) 0L else revealStartMs(earned - 1) + GENTLE_POP_DURATION_MS

fun goalBrightenMs(goals: List<JourneyGoal>): List<Long> {
    val earned = goals.count { it.met }
    val lastStart = if (earned == 0) 0L else revealStartMs(earned - 1)
    var metSoFar = 0
    return goals.map { goal -> if (goal.met) revealStartMs(metSoFar++) else lastStart }
}
