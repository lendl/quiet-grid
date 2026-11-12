package com.quietgrid.app.games.guessbynumbers

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.quietgrid.app.R
import com.quietgrid.app.core.GameId
import com.quietgrid.app.games.GameCapabilities
import com.quietgrid.app.games.GameDefinition
import com.quietgrid.app.games.GameFinish
import com.quietgrid.app.games.LossCopy
import com.quietgrid.app.games.PlayRequest
import com.quietgrid.app.ui.components.GlyphOval
import com.quietgrid.app.ui.components.GlyphPaint
import com.quietgrid.app.ui.components.GlyphRect
import com.quietgrid.app.ui.components.GlyphShape
import com.quietgrid.app.ui.components.GlyphText
import com.quietgrid.app.ui.screens.CompletionExtras
import com.quietgrid.app.ui.screens.CompletionHighlight

object GuessByNumbersDefinition : GameDefinition {
    override val id = GameId.GUESSBYNUMBERS
    override val capabilities = GameCapabilities(
        phonePortraitOnly = true,
    )
    override val abandonedCopy = LossCopy(R.string.guessbynumbers_loss_rule_failure_eyebrow, R.string.guessbynumbers_loss_rule_failure_title, R.string.guessbynumbers_loss_rule_failure_body)
    override val quickStart = GuessByNumbersQuickStart
    override val iconHue = Color(0xFF3B72B8)
    override val iconGlyph: List<GlyphShape> = listOf(
        GlyphRect(1.5f, 3f, 4.5f, 5.5f, 1.2f, GlyphPaint.NEUTRAL),
        GlyphRect(7f, 3f, 4.5f, 5.5f, 1.2f, GlyphPaint.NEUTRAL),
        GlyphRect(12.5f, 3f, 4.5f, 5.5f, 1.2f, GlyphPaint.NEUTRAL),
        GlyphRect(18f, 3f, 4.5f, 5.5f, 1.2f, GlyphPaint.NEUTRAL),
        GlyphOval(7.5f, 16.5f, 4.5f, 4.5f, GlyphPaint.ACCENT),
        GlyphText(7.5f, 16.7f, "2", 5.5f, GlyphPaint.CUTOUT),
        GlyphOval(16.5f, 16.5f, 4.5f, 4.5f, GlyphPaint.NEUTRAL),
        GlyphText(16.5f, 16.7f, "1", 5.5f, GlyphPaint.MARK),
    )

    @Composable
    override fun PlayScreen(request: PlayRequest) {
        GuessByNumbersPlayScreen(
            difficulty = request.difficulty,
            resume = request.resume,
            onBack = request.onBack,
            onFinished = { result ->
                if (!result.solved && result.targetWord.isNotEmpty()) CompletionExtras.set(CompletionHighlight.RevealWord(result.targetWord))
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
