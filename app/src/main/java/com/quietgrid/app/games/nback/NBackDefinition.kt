package com.quietgrid.app.games.nback

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
import com.quietgrid.app.ui.screens.CompletionExtras
import com.quietgrid.app.ui.screens.CompletionHighlight

object NBackDefinition : GameDefinition {
    override val id = GameId.NBACK
    override val capabilities = GameCapabilities(
        accuracyTracked = true,
        alwaysKeepScreenOn = true,
    )
    override val quickStart = NBackQuickStart
    override val iconHue = Color(0xFF9A4FB0)
    override val iconGlyph: List<GlyphShape> = listOf(
        GlyphRect(1.5f, 1.5f, 6f, 6f, 1.5f, GlyphPaint.EMPTY),
        GlyphRect(9f, 1.5f, 6f, 6f, 1.5f, GlyphPaint.EMPTY),
        GlyphRect(16.5f, 1.5f, 6f, 6f, 1.5f, GlyphPaint.ACCENT),
        GlyphRect(1.5f, 9f, 6f, 6f, 1.5f, GlyphPaint.EMPTY),
        GlyphRect(9f, 9f, 6f, 6f, 1.5f, GlyphPaint.EMPTY),
        GlyphRect(16.5f, 9f, 6f, 6f, 1.5f, GlyphPaint.EMPTY),
        GlyphRect(1.5f, 16.5f, 6f, 6f, 1.5f, GlyphPaint.NEUTRAL),
        GlyphRect(9f, 16.5f, 6f, 6f, 1.5f, GlyphPaint.EMPTY),
        GlyphRect(16.5f, 16.5f, 6f, 6f, 1.5f, GlyphPaint.EMPTY),
    )

    @Composable
    override fun PlayScreen(request: PlayRequest) {
        NBackPlayScreen(
            difficulty = request.difficulty,
            resume = request.resume,
            onBack = request.onBack,
            onFinished = { result ->
                if (result.solved) {
                    CompletionExtras.set(
                        CompletionHighlight.NBackBreakdown(
                            hits = result.hits,
                            misses = result.misses,
                            falsePositives = result.falsePositives,
                            avgReactionTimeMs = result.averageReactionTimeMs,
                        ),
                    )
                }
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
