package com.quietgrid.app.games.nonogram

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.quietgrid.app.core.GameId
import com.quietgrid.app.games.ChallengerSpec
import com.quietgrid.app.games.GameCapabilities
import com.quietgrid.app.games.GameDefinition
import com.quietgrid.app.games.GameFinish
import com.quietgrid.app.games.PlayRequest
import com.quietgrid.app.ui.components.GlyphPaint
import com.quietgrid.app.ui.components.GlyphRect
import com.quietgrid.app.ui.components.GlyphShape
import com.quietgrid.app.ui.screens.CompletionExtras
import com.quietgrid.app.ui.screens.CompletionHighlight

object NonogramDefinition : GameDefinition {
    override val id = GameId.NONOGRAM
    override val capabilities = GameCapabilities(
        hints = true,
    )
    override val challenger = ChallengerSpec(
        config = NONOGRAM_CHALLENGER_CONFIG,
        playScreen = { onFinished -> NonogramChallengerPlayScreen(onFinished = onFinished) },
    )
    override val quickStart = NonogramQuickStart
    override val iconHue = Color(0xFF4A8A47)
    override val iconGlyph: List<GlyphShape> = listOf(
        GlyphRect(9.8f, 1.5f, 1.4f, 1.8f, 0.7f, GlyphPaint.SMALL),
        GlyphRect(9.8f, 4.6f, 1.4f, 1.8f, 0.7f, GlyphPaint.SMALL),
        GlyphRect(15.2f, 1.3f, 1.4f, 5.5f, 0.7f, GlyphPaint.SMALL),
        GlyphRect(20.6f, 4.8f, 1.4f, 2f, 0.7f, GlyphPaint.SMALL),
        GlyphRect(2.8f, 9.8f, 4f, 1.4f, 0.7f, GlyphPaint.SMALL),
        GlyphRect(4.8f, 15.2f, 2f, 1.4f, 0.7f, GlyphPaint.SMALL),
        GlyphRect(1.3f, 20.6f, 5.5f, 1.4f, 0.7f, GlyphPaint.SMALL),
        GlyphRect(8.3f, 8.3f, 4.4f, 4.4f, 1f, GlyphPaint.ACCENT),
        GlyphRect(13.7f, 8.3f, 4.4f, 4.4f, 1f, GlyphPaint.ACCENT),
        GlyphRect(19.1f, 8.3f, 4.4f, 4.4f, 1f, GlyphPaint.EMPTY),
        GlyphRect(8.3f, 13.7f, 4.4f, 4.4f, 1f, GlyphPaint.EMPTY),
        GlyphRect(13.7f, 13.7f, 4.4f, 4.4f, 1f, GlyphPaint.ACCENT),
        GlyphRect(19.1f, 13.7f, 4.4f, 4.4f, 1f, GlyphPaint.EMPTY),
        GlyphRect(8.3f, 19.1f, 4.4f, 4.4f, 1f, GlyphPaint.ACCENT),
        GlyphRect(13.7f, 19.1f, 4.4f, 4.4f, 1f, GlyphPaint.ACCENT),
        GlyphRect(19.1f, 19.1f, 4.4f, 4.4f, 1f, GlyphPaint.ACCENT),
    )

    @Composable
    override fun PlayScreen(request: PlayRequest) {
        NonogramPlayScreen(
            difficulty = request.difficulty,
            resume = request.resume,
            journey = request.journey,
            onRestarted = request.onRestarted,
            onBack = request.onBack,
            onFinished = { result ->
                if (result.solved && request.journey == null) CompletionExtras.set(CompletionHighlight.Picture(result.solution))
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
