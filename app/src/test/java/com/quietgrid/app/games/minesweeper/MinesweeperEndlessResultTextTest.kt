package com.quietgrid.app.games.minesweeper

import com.quietgrid.app.R
import org.junit.Assert.assertEquals
import org.junit.Test

class MinesweeperEndlessResultTextTest {

    private fun result(score: Int, isNewBest: Boolean, previousBest: Int, reason: String) =
        MinesweeperEndlessResult(score, ring = 2, elapsedSeconds = 60, isNewBest = isNewBest, previousBest = previousBest, reason = reason)

    @Test
    fun `run time formats minutes and hours`() {
        assertEquals("0:00", formatEndlessRunTime(0))
        assertEquals("12:34", formatEndlessRunTime(754))
        assertEquals("1:02:05", formatEndlessRunTime(3725))
    }

    @Test
    fun `title follows the reason`() {
        assertEquals(
            R.string.minesweeper_endless_result_out_of_hearts_title,
            minesweeperEndlessResultTitleRes(result(3, false, 9, MINESWEEPER_ENDLESS_REASON_OUT_OF_HEARTS)),
        )
        assertEquals(
            R.string.minesweeper_endless_result_ended_title,
            minesweeperEndlessResultTitleRes(result(3, false, 9, MINESWEEPER_ENDLESS_REASON_ENDED)),
        )
    }

    @Test
    fun `body celebrates a best before naming the reason`() {
        assertEquals(
            R.string.minesweeper_endless_result_new_best_body,
            minesweeperEndlessResultBodyRes(result(12, true, 9, MINESWEEPER_ENDLESS_REASON_OUT_OF_HEARTS)),
        )
        assertEquals(
            R.string.minesweeper_endless_result_first_run_body,
            minesweeperEndlessResultBodyRes(result(4, true, 0, MINESWEEPER_ENDLESS_REASON_ENDED)),
        )
        assertEquals(
            R.string.minesweeper_endless_result_out_of_hearts_body,
            minesweeperEndlessResultBodyRes(result(3, false, 9, MINESWEEPER_ENDLESS_REASON_OUT_OF_HEARTS)),
        )
        assertEquals(
            R.string.minesweeper_endless_result_ended_body,
            minesweeperEndlessResultBodyRes(result(3, false, 9, MINESWEEPER_ENDLESS_REASON_ENDED)),
        )
    }
}
