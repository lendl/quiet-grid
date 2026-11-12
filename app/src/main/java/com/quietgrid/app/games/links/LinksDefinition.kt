package com.quietgrid.app.games.links

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.quietgrid.app.core.GameId
import com.quietgrid.app.games.GameCapabilities
import com.quietgrid.app.games.GameDefinition
import com.quietgrid.app.games.GameFinish
import com.quietgrid.app.games.PlayRequest
import com.quietgrid.app.ui.components.GlyphOval
import com.quietgrid.app.ui.components.GlyphPaint
import com.quietgrid.app.ui.components.GlyphPath
import com.quietgrid.app.ui.components.GlyphShape

object LinksDefinition : GameDefinition {
    override val id = GameId.LINKS
    override val capabilities = GameCapabilities(
        hints = true,
        pinchZoom = true,
        flawlessEligible = true,
        accuracyTracked = true,
    )
    override val quickStart = LinksQuickStart
    override val iconHue = Color(0xFF2F7FB8)
    override val iconGlyph: List<GlyphShape> = listOf(
        GlyphPath("M5 5 H12 V12 H19 V19", GlyphPaint.LINE_ACCENT, strokeWidth = 3.2f),
        GlyphOval(5f, 5f, 3.5f, 3.5f, GlyphPaint.ACCENT),
        GlyphOval(19f, 19f, 3.5f, 3.5f, GlyphPaint.ACCENT),
        GlyphOval(19f, 5f, 3.5f, 3.5f, GlyphPaint.NEUTRAL),
        GlyphOval(5f, 19f, 3.5f, 3.5f, GlyphPaint.NEUTRAL),
    )

    @Composable
    override fun PlayScreen(request: PlayRequest) {
        LinksPlayScreen(
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
