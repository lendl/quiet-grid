package com.quietgrid.app.games.arrowescape

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
import com.quietgrid.app.ui.components.GlyphShape

object ArrowEscapeDefinition : GameDefinition {
    override val id = GameId.ARROWESCAPE
    override val capabilities = GameCapabilities(
        hints = true,
        pinchZoom = true,
    )
    override val abandonedCopy = LossCopy(R.string.arrowescape_loss_rule_failure_eyebrow, R.string.arrowescape_loss_rule_failure_title, R.string.arrowescape_loss_rule_failure_body)
    override val quickStart = ArrowEscapeQuickStart
    override val iconHue = Color(0xFF4F6E9C)
    override val iconGlyph: List<GlyphShape> = listOf(
        GlyphPath("M4 3.5 V11 M1.5 8.5 L4 11 L6.5 8.5", GlyphPaint.LINE, strokeWidth = 2.2f),
        GlyphPath("M3 18 H13 M10 15 L13 18 L10 21", GlyphPaint.LINE, strokeWidth = 2.2f),
        GlyphPath("M19 21 V4 M16 7 L19 4 L22 7", GlyphPaint.LINE_ACCENT, strokeWidth = 2.2f),
    )

    @Composable
    override fun PlayScreen(request: PlayRequest) {
        ArrowEscapePlayScreen(
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
                        accuracyPct = 100,
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
