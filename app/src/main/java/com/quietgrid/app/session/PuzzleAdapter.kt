package com.quietgrid.app.session

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId

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

    /** Return null if the payload doesn't decode or isn't in a restorable state; caller falls back to [freshSession]. */
    fun restoreSession(payload: String, elapsedSeconds: Double): TSession?

    fun difficultyOf(session: TSession): Difficulty

    fun hasMeaningfulProgress(session: TSession): Boolean

    fun encode(session: TSession): String

    fun scoreOnWin(session: TSession, difficulty: Difficulty, elapsedSeconds: Int): Int

    fun buildResult(session: TSession?, outcome: PuzzleOutcome): TResult
}
