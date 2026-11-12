package com.quietgrid.app.games.circuit

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
import com.quietgrid.app.ui.components.GlyphRect
import com.quietgrid.app.ui.components.GlyphShape

object CircuitDefinition : GameDefinition {
    override val id = GameId.CIRCUIT
    override val capabilities = GameCapabilities(
        hints = true,
        pinchZoom = true,
    )
    override val quickStart = CircuitQuickStart
    override val iconHue = Color(0xFFB0782A)
    override val iconGlyph: List<GlyphShape> = listOf(
        GlyphRect(1.5f, 1.5f, 9.5f, 9.5f, 2.5f, GlyphPaint.NEUTRAL),
        GlyphRect(13f, 1.5f, 9.5f, 9.5f, 2.5f, GlyphPaint.NEUTRAL),
        GlyphRect(1.5f, 13f, 9.5f, 9.5f, 2.5f, GlyphPaint.NEUTRAL),
        GlyphRect(13f, 13f, 9.5f, 9.5f, 2.5f, GlyphPaint.EMPTY),
        GlyphPath("M6.25 17.75 V6.25 H17.75 V12.5", GlyphPaint.LINE_ACCENT, strokeWidth = 2.6f),
        GlyphOval(6.25f, 17.75f, 2.4f, 2.4f, GlyphPaint.ACCENT),
        GlyphOval(17.75f, 12.5f, 2.4f, 2.4f, GlyphPaint.ACCENT),
    )

    @Composable
    override fun PlayScreen(request: PlayRequest) {
        CircuitPlayScreen(
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
