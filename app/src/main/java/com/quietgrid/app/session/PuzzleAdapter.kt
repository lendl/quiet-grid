package com.quietgrid.app.session

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import java.time.LocalDate

data class PuzzleOutcome(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val elapsedSeconds: Int,
    val lossReason: String?,
    val isFirstSolve: Boolean,
    val isNewHighScore: Boolean,
)

interface PuzzleAdapter<TSession, TResult> {
    val gameId: GameId

    suspend fun freshSession(difficulty: Difficulty): TSession?

    fun restoreSession(payload: String, elapsedSeconds: Double): TSession?

    fun difficultyOf(session: TSession): Difficulty

    fun hasMeaningfulProgress(session: TSession): Boolean

    fun encode(session: TSession): String

    fun scoreOnWin(session: TSession, difficulty: Difficulty, elapsedSeconds: Int): Int

    fun scoreOnLoss(session: TSession, difficulty: Difficulty, elapsedSeconds: Int): Int = 0

    fun puzzleIdOf(session: TSession): String? = null

    fun buildResult(session: TSession?, outcome: PuzzleOutcome): TResult

    suspend fun dailySession(difficulty: Difficulty, date: LocalDate): TSession? = null

    fun dailyShareDetail(session: TSession): String? = null
}
