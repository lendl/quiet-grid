package com.quietgrid.app.games.chimptest

import com.quietgrid.app.core.Difficulty
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

private const val MIN_SCORE = 1000

private data class ChimpTestDifficultyConfig(val gridSize: Int, val startCount: Int, val maxCount: Int)

private val DIFFICULTY_CONFIG = mapOf(
    Difficulty.EASY to ChimpTestDifficultyConfig(gridSize = 4, startCount = 3, maxCount = 7),
    Difficulty.MEDIUM to ChimpTestDifficultyConfig(gridSize = 5, startCount = 4, maxCount = 9),
    Difficulty.HARD to ChimpTestDifficultyConfig(gridSize = 6, startCount = 5, maxCount = 11),
    Difficulty.EXPERT to ChimpTestDifficultyConfig(gridSize = 7, startCount = 6, maxCount = 13),
)

fun generateChimpTestCells(count: Int, gridSize: Int): List<ChimpTestCell> {
    val positions = mutableListOf<Pair<Int, Int>>()
    for (r in 0 until gridSize) {
        for (c in 0 until gridSize) {
            positions.add(r to c)
        }
    }
    positions.shuffle(Random)
    return positions.take(count).mapIndexed { index, (row, col) ->
        ChimpTestCell(number = index + 1, row = row, col = col, hidden = false)
    }
}

fun createChimpTestSession(difficulty: Difficulty): ChimpTestSession {
    val config = DIFFICULTY_CONFIG.getValue(difficulty)
    val puzzle = ChimpTestPuzzle(
        id = "${difficulty.key}-${System.currentTimeMillis()}",
        difficulty = difficulty.key,
        gridSize = config.gridSize,
        startCount = config.startCount,
        maxCount = config.maxCount,
    )
    return ChimpTestSession(
        puzzle = puzzle,
        currentCount = config.startCount,
        cells = generateChimpTestCells(config.startCount, config.gridSize),
        nextExpected = 1,
        revealAll = false,
        wrongTapCell = null,
        roundTimes = emptyList(),
        roundStartElapsed = 0.0,
        status = ChimpTestStatus.PLAYING,
    )
}

fun runChimpTestAction(session: ChimpTestSession, row: Int, col: Int, elapsedSeconds: Double): ChimpTestActionResult {
    if (session.status != ChimpTestStatus.PLAYING || session.revealAll) {
        return ChimpTestActionResult(changed = false, session = session, effects = emptyList())
    }

    // Allow tapping hidden cells -- after the first tap all remaining cells lose their numbers
    // but must still be tappable. Cells with number < nextExpected are already correctly tapped.
    val tappedCell = session.cells.firstOrNull {
        it.number >= session.nextExpected && it.row == row && it.col == col
    }

    if (tappedCell == null || tappedCell.number != session.nextExpected) {
        return ChimpTestActionResult(
            changed = true,
            session = session.copy(revealAll = true, wrongTapCell = tappedCell?.number),
            effects = listOf(ChimpTestEffect.WrongTap),
        )
    }

    // On the first tap, hide ALL remaining numbers (classic Chimp Test: memorise the rest).
    val isFirstTap = session.nextExpected == 1
    val newCells = session.cells.map { cell ->
        if (isFirstTap || cell.number == session.nextExpected) cell.copy(hidden = true) else cell
    }

    val newNextExpected = session.nextExpected + 1

    if (newNextExpected > session.currentCount) {
        val roundTime = elapsedSeconds - session.roundStartElapsed
        val newRoundTimes = session.roundTimes + roundTime

        if (session.currentCount >= session.puzzle.maxCount) {
            return ChimpTestActionResult(
                changed = true,
                session = session.copy(cells = newCells, status = ChimpTestStatus.WON, roundTimes = newRoundTimes),
                effects = emptyList(),
            )
        }

        val nextCount = session.currentCount + 1
        return ChimpTestActionResult(
            changed = true,
            session = session.copy(
                currentCount = nextCount,
                cells = generateChimpTestCells(nextCount, session.puzzle.gridSize),
                nextExpected = 1,
                roundTimes = newRoundTimes,
                roundStartElapsed = elapsedSeconds,
            ),
            effects = emptyList(),
        )
    }

    return ChimpTestActionResult(
        changed = true,
        session = session.copy(cells = newCells, nextExpected = newNextExpected),
        effects = emptyList(),
    )
}

fun chimpTestScore(session: ChimpTestSession): Int {
    if (session.status != ChimpTestStatus.WON) return 0
    val totalRoundTime = session.roundTimes.sum()
    return max(MIN_SCORE, 50_000 - (totalRoundTime * 500).roundToInt())
}
