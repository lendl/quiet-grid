package com.quietgrid.app.games.killersudoku

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.quietgrid.app.core.GameId
import com.quietgrid.app.games.GameCapabilities
import com.quietgrid.app.games.GameDefinition
import com.quietgrid.app.games.GameFinish
import com.quietgrid.app.games.PlayRequest
import com.quietgrid.app.ui.components.GlyphPaint
import com.quietgrid.app.ui.components.GlyphRect
import com.quietgrid.app.ui.components.GlyphShape
import com.quietgrid.app.ui.components.GlyphText

object KillerSudokuDefinition : GameDefinition {
    override val id = GameId.KILLERSUDOKU
    override val capabilities = GameCapabilities(
        hints = true,
        pinchZoom = true,
        mistakeCheck = true,
        flawlessEligible = true,
        accuracyTracked = true,
    )
    override val quickStart = KillerSudokuQuickStart
    override val iconHue = Color(0xFF5F9334)
    override val iconGlyph: List<GlyphShape> = listOf(
        GlyphRect(1.5f, 1.5f, 13.5f, 6f, 1.5f, GlyphPaint.ACCENT),
        GlyphText(8.25f, 4.7f, "12", 4.4f, GlyphPaint.CUTOUT),
        GlyphRect(16.5f, 1.5f, 6f, 13.5f, 1.5f, GlyphPaint.NEUTRAL),
        GlyphText(19.5f, 8.2f, "9", 4.6f, GlyphPaint.MARK),
        GlyphRect(1.5f, 9f, 6f, 13.5f, 1.5f, GlyphPaint.NEUTRAL),
        GlyphText(4.5f, 15.7f, "8", 4.6f, GlyphPaint.MARK),
        GlyphRect(9f, 9f, 6f, 6f, 1.5f, GlyphPaint.EMPTY),
        GlyphRect(9f, 16.5f, 13.5f, 6f, 1.5f, GlyphPaint.ACCENT),
        GlyphText(15.75f, 19.7f, "3", 4.6f, GlyphPaint.CUTOUT),
    )

    @Composable
    override fun PlayScreen(request: PlayRequest) {
        KillerSudokuPlayScreen(
            difficulty = request.difficulty,
            resume = request.resume,
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
