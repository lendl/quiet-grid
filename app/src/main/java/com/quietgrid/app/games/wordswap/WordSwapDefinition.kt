package com.quietgrid.app.games.wordswap

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.quietgrid.app.core.GameId
import com.quietgrid.app.games.GameCapabilities
import com.quietgrid.app.games.GameDefinition
import com.quietgrid.app.games.GameFinish
import com.quietgrid.app.games.PlayRequest
import com.quietgrid.app.ui.components.GlyphPaint
import com.quietgrid.app.ui.components.GlyphPath
import com.quietgrid.app.ui.components.GlyphRect
import com.quietgrid.app.ui.components.GlyphShape
import com.quietgrid.app.ui.components.GlyphText

object WordSwapDefinition : GameDefinition {
    override val id = GameId.WORDSWAP
    override val capabilities = GameCapabilities(
        hints = true,
        accuracyTracked = true,
    )
    override val quickStart = WordSwapQuickStart
    override val iconHue = Color(0xFF8A6BBE)
    override val iconGlyph: List<GlyphShape> = listOf(
        GlyphRect(1.5f, 1.5f, 6f, 6f, 1.5f, GlyphPaint.ACCENT),
        GlyphText(4.5f, 4.7f, "S", 4.4f, GlyphPaint.CUTOUT),
        GlyphRect(9f, 1.5f, 6f, 6f, 1.5f, GlyphPaint.NEUTRAL),
        GlyphText(12f, 4.7f, "W", 4.4f, GlyphPaint.MARK),
        GlyphRect(16.5f, 1.5f, 6f, 6f, 1.5f, GlyphPaint.NEUTRAL),
        GlyphText(19.5f, 4.7f, "A", 4.4f, GlyphPaint.MARK),
        GlyphRect(1.5f, 9f, 6f, 6f, 1.5f, GlyphPaint.NEUTRAL),
        GlyphRect(16.5f, 9f, 6f, 6f, 1.5f, GlyphPaint.NEUTRAL),
        GlyphText(19.5f, 12.2f, "P", 4.4f, GlyphPaint.MARK),
        GlyphRect(1.5f, 16.5f, 6f, 6f, 1.5f, GlyphPaint.NEUTRAL),
        GlyphRect(9f, 16.5f, 6f, 6f, 1.5f, GlyphPaint.NEUTRAL),
        GlyphRect(16.5f, 16.5f, 6f, 6f, 1.5f, GlyphPaint.ACCENT),
        GlyphPath("M8.5 9.5 Q12 12 15.5 14.5", GlyphPaint.LINE_ACCENT, strokeWidth = 1.6f),
    )

    @Composable
    override fun PlayScreen(request: PlayRequest) {
        WordSwapPlayScreen(
            difficulty = request.difficulty,
            resume = request.resume,
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
                    ),
                )
            },
        )
    }
}
