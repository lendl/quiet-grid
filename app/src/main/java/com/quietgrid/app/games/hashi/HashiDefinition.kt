package com.quietgrid.app.games.hashi

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

object HashiDefinition : GameDefinition {
    override val id = GameId.HASHI
    override val capabilities = GameCapabilities(
        hints = true,
        pinchZoom = true,
    )
    override val quickStart = HashiQuickStart
    override val iconHue = Color(0xFF3E7C66)
    override val iconGlyph: List<GlyphShape> = listOf(
        GlyphPath("M8.5 3.9 H15.5 M8.5 6.6 H15.5", GlyphPaint.LINE, strokeWidth = 1.5f),
        GlyphPath("M5.25 9 V15", GlyphPaint.LINE, strokeWidth = 1.6f),
        GlyphOval(5.25f, 5.25f, 3.75f, 3.75f, GlyphPaint.ACCENT),
        GlyphOval(18.75f, 5.25f, 3.75f, 3.75f, GlyphPaint.NEUTRAL),
        GlyphOval(5.25f, 18.75f, 3.75f, 3.75f, GlyphPaint.NEUTRAL),
    )

    @Composable
    override fun PlayScreen(request: PlayRequest) {
        HashiPlayScreen(
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
