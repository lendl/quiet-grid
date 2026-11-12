package com.quietgrid.app.games.minesweeper

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.quietgrid.app.R
import com.quietgrid.app.core.GameId
import com.quietgrid.app.games.GameCapabilities
import com.quietgrid.app.games.GameDefinition
import com.quietgrid.app.games.GameFinish
import com.quietgrid.app.games.LossCopy
import com.quietgrid.app.games.PlayRequest
import com.quietgrid.app.ui.components.GlyphPaint
import com.quietgrid.app.ui.components.GlyphPath
import com.quietgrid.app.ui.components.GlyphRect
import com.quietgrid.app.ui.components.GlyphShape
import com.quietgrid.app.ui.components.GlyphText

object MinesweeperDefinition : GameDefinition {
    override val id = GameId.MINESWEEPER
    override val capabilities = GameCapabilities(
        hints = true,
        pinchZoom = true,
    )
    override val abandonedCopy = LossCopy(R.string.minesweeper_loss_rule_failure_eyebrow, R.string.minesweeper_loss_rule_failure_title, R.string.minesweeper_loss_rule_failure_body)
    override val quickStart = MinesweeperQuickStart
    override val iconHue = Color(0xFF697789)
    override val iconGlyph: List<GlyphShape> = listOf(
        GlyphRect(1.5f, 1.5f, 6f, 6f, 1.5f, GlyphPaint.EMPTY),
        GlyphRect(9f, 1.5f, 6f, 6f, 1.5f, GlyphPaint.EMPTY),
        GlyphRect(16.5f, 1.5f, 6f, 6f, 1.5f, GlyphPaint.NEUTRAL),
        GlyphRect(1.5f, 9f, 6f, 6f, 1.5f, GlyphPaint.EMPTY),
        GlyphText(4.5f, 12.2f, "1", 4.6f, GlyphPaint.MARK),
        GlyphRect(9f, 9f, 6f, 6f, 1.5f, GlyphPaint.EMPTY),
        GlyphText(12f, 12.2f, "2", 4.6f, GlyphPaint.MARK),
        GlyphRect(16.5f, 9f, 6f, 6f, 1.5f, GlyphPaint.NEUTRAL),
        GlyphRect(1.5f, 16.5f, 6f, 6f, 1.5f, GlyphPaint.NEUTRAL),
        GlyphRect(9f, 16.5f, 6f, 6f, 1.5f, GlyphPaint.NEUTRAL),
        GlyphRect(16.5f, 16.5f, 6f, 6f, 1.5f, GlyphPaint.ACCENT),
        GlyphRect(18.1f, 17.7f, 0.9f, 3.9f, 0f, GlyphPaint.CUTOUT),
        GlyphPath("M19 17.7 L21.6 18.9 L19 20.1 Z", GlyphPaint.CUTOUT),
    )

    @Composable
    override fun PlayScreen(request: PlayRequest) {
        MinesweeperPlayScreen(
            difficulty = request.difficulty,
            resume = request.resume,
            onBack = request.onBack,
            onFinished = { result ->
                request.onFinished(
                    GameFinish(
                        difficulty = result.difficulty,
                        solved = result.solved,
                        score = result.score,
                        accuracyPct = 100,
                        elapsedSeconds = result.elapsedSeconds,
                        isFirstSolve = result.isFirstSolve,
                        isNewHighScore = result.isNewHighScore,
                        lossReason = result.lossReason,
                    ),
                )
            },
        )
    }
}
