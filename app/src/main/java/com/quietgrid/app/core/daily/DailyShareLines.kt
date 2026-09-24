package com.quietgrid.app.core.daily

import com.quietgrid.engine.wordguess.LetterState

private const val SQUARE_CORRECT = "\uD83D\uDFE9"
private const val SQUARE_PRESENT = "\uD83D\uDFE8"
private const val SQUARE_ABSENT = "\u2B1B"

fun joinDailyShareLines(header: String, resultLine: String, detail: String?, streakLine: String?): String =
    listOfNotNull(header, resultLine, detail, streakLine).joinToString("\n")

fun wordGuessShareGrid(rows: List<List<LetterState>>): String = rows.joinToString("\n") { row ->
    row.joinToString("") { state ->
        when (state) {
            LetterState.CORRECT -> SQUARE_CORRECT
            LetterState.PRESENT -> SQUARE_PRESENT
            LetterState.ABSENT -> SQUARE_ABSENT
        }
    }
}
