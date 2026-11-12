package com.quietgrid.app.games.sudoku

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.quietgrid.app.R
import com.quietgrid.app.core.GameId
import com.quietgrid.app.games.ChallengerEndCopy
import com.quietgrid.app.games.ChallengerSpec
import com.quietgrid.app.games.GameCapabilities
import com.quietgrid.app.games.GameDefinition
import com.quietgrid.app.games.GameFinish
import com.quietgrid.app.games.LossCopy
import com.quietgrid.app.games.PlayRequest
import com.quietgrid.app.ui.components.GlyphPaint
import com.quietgrid.app.ui.components.GlyphRect
import com.quietgrid.app.ui.components.GlyphShape
import com.quietgrid.app.ui.components.GlyphText

object SudokuDefinition : GameDefinition {
    override val id = GameId.SUDOKU
    override val capabilities = GameCapabilities(
        hints = true,
        pinchZoom = true,
        mistakeCheck = true,
        flawlessEligible = true,
        accuracyTracked = true,
    )
    override val challenger = ChallengerSpec(
        config = SUDOKU_CHALLENGER_CONFIG,
        endCopy = mapOf(
            "lives_exhausted" to ChallengerEndCopy(R.string.challenger_result_lives_exhausted_title, R.string.sudoku_challenger_result_lives_exhausted_body),
        ),
        playScreen = { onFinished -> SudokuChallengerPlayScreen(onFinished = onFinished) },
    )
    override val abandonedCopy = LossCopy(R.string.loss_abandoned_eyebrow, R.string.sudoku_loss_abandoned_title, R.string.sudoku_loss_abandoned_body)
    override val quickStart = SudokuQuickStart
    override val iconHue = Color(0xFF8550C4)
    override val iconGlyph: List<GlyphShape> = listOf(
        GlyphRect(1.5f, 1.5f, 6f, 6f, 1.5f, GlyphPaint.EMPTY),
        GlyphText(4.5f, 4.7f, "5", 4.6f, GlyphPaint.MARK),
        GlyphRect(9f, 1.5f, 6f, 6f, 1.5f, GlyphPaint.EMPTY),
        GlyphRect(16.5f, 1.5f, 6f, 6f, 1.5f, GlyphPaint.EMPTY),
        GlyphRect(1.5f, 9f, 6f, 6f, 1.5f, GlyphPaint.EMPTY),
        GlyphRect(9f, 9f, 6f, 6f, 1.5f, GlyphPaint.ACCENT),
        GlyphText(12f, 12.2f, "7", 4.6f, GlyphPaint.CUTOUT),
        GlyphRect(16.5f, 9f, 6f, 6f, 1.5f, GlyphPaint.EMPTY),
        GlyphText(19.5f, 12.2f, "3", 4.6f, GlyphPaint.MARK),
        GlyphRect(1.5f, 16.5f, 6f, 6f, 1.5f, GlyphPaint.EMPTY),
        GlyphText(4.5f, 19.7f, "8", 4.6f, GlyphPaint.MARK),
        GlyphRect(9f, 16.5f, 6f, 6f, 1.5f, GlyphPaint.EMPTY),
        GlyphRect(16.5f, 16.5f, 6f, 6f, 1.5f, GlyphPaint.EMPTY),
    )

    @Composable
    override fun PlayScreen(request: PlayRequest) {
        SudokuPlayScreen(
            difficulty = request.difficulty,
            resume = request.resume,
            dailyDate = request.dailyDate,
            journey = request.journey,
            onRestarted = request.onRestarted,
            onBack = request.onBack,
            onFinished = { result ->
                request.onFinished(
                    GameFinish(
                        difficulty = result.difficulty,
                        solved = result.solved,
                        score = result.score,
                        accuracyPct = result.accuracyPct,
                        elapsedSeconds = result.elapsedSeconds,
                        isFirstSolve = result.isFirstSolve,
                        isNewHighScore = result.isNewHighScore,
                        lossReason = result.lossReason,
                        journeySignals = result.journeySignals,
                    ),
                )
            },
        )
    }
}
