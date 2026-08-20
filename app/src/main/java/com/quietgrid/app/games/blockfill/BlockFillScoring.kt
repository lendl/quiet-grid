package com.quietgrid.app.games.blockfill

const val BLOCKFILL_POINTS_PER_LINE_CLEARED = 80
const val BLOCKFILL_MULTI_CLEAR_BONUS_PER_EXTRA_LINE = 40
const val BLOCKFILL_COMBO_BONUS_PER_STREAK = 20
const val BLOCKFILL_FULL_BOARD_CLEAR_BONUS = 300

fun scorePlacement(linesCleared: Int, comboStreakBeforeThisMove: Int, boardEmptiedAfter: Boolean): Int {
    if (linesCleared == 0) return 0

    var points = linesCleared * BLOCKFILL_POINTS_PER_LINE_CLEARED
    points += maxOf(0, linesCleared - 1) * BLOCKFILL_MULTI_CLEAR_BONUS_PER_EXTRA_LINE
    points += comboStreakBeforeThisMove * BLOCKFILL_COMBO_BONUS_PER_STREAK
    if (boardEmptiedAfter) points += BLOCKFILL_FULL_BOARD_CLEAR_BONUS

    return points
}
