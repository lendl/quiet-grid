package com.quietgrid.app.games.themeclear

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
import com.quietgrid.app.ui.components.GlyphRect
import com.quietgrid.app.ui.components.GlyphShape
import com.quietgrid.app.ui.components.GlyphText
import com.quietgrid.app.ui.screens.CompletionExtras
import com.quietgrid.app.ui.screens.CompletionHighlight

object ThemeClearDefinition : GameDefinition {
    override val id = GameId.THEMECLEAR
    override val capabilities = GameCapabilities(
        hints = true,
        pinchZoom = true,
        themed = true,
    )
    override val abandonedCopy = LossCopy(R.string.loss_abandoned_eyebrow, R.string.themeclear_loss_abandoned_title, R.string.loss_abandoned_body)
    override val quickStart = ThemeClearQuickStart
    override val iconHue = Color(0xFF7D8B2A)
    override val iconGlyph: List<GlyphShape> = listOf(
        GlyphRect(1.5f, 1.5f, 6f, 6f, 1.5f, GlyphPaint.NEUTRAL),
        GlyphText(4.5f, 4.7f, "R", 4.4f, GlyphPaint.MARK),
        GlyphRect(9f, 1.5f, 6f, 6f, 1.5f, GlyphPaint.ACCENT),
        GlyphText(12f, 4.7f, "S", 4.4f, GlyphPaint.CUTOUT),
        GlyphRect(16.5f, 1.5f, 6f, 6f, 1.5f, GlyphPaint.NEUTRAL),
        GlyphText(19.5f, 4.7f, "E", 4.4f, GlyphPaint.MARK),
        GlyphRect(1.5f, 9f, 6f, 6f, 1.5f, GlyphPaint.ACCENT),
        GlyphText(4.5f, 12.2f, "U", 4.4f, GlyphPaint.CUTOUT),
        GlyphRect(9f, 9f, 6f, 6f, 1.5f, GlyphPaint.NEUTRAL),
        GlyphText(12f, 12.2f, "A", 4.4f, GlyphPaint.MARK),
        GlyphRect(16.5f, 9f, 6f, 6f, 1.5f, GlyphPaint.ACCENT),
        GlyphText(19.5f, 12.2f, "N", 4.4f, GlyphPaint.CUTOUT),
        GlyphRect(1.5f, 16.5f, 21f, 6f, 3f, GlyphPaint.ACCENT),
        GlyphText(12f, 19.7f, "SUN", 4.4f, GlyphPaint.CUTOUT, letterSpacing = 1f),
    )

    @Composable
    override fun PlayScreen(request: PlayRequest) {
        ThemeClearPlayScreen(
            difficulty = request.difficulty,
            resume = request.resume,
            onBack = request.onBack,
            onFinished = { result ->
                if (!result.solved && result.intendedWords.isNotEmpty()) CompletionExtras.set(CompletionHighlight.RevealWordList(result.intendedWords))
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
