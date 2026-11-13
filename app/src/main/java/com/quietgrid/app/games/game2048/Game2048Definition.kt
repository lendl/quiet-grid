package com.quietgrid.app.games.game2048

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

object Game2048Definition : GameDefinition {
    override val id = GameId.GAME_2048
    override val capabilities = GameCapabilities(
        scoreOnLossTracked = true,
    )
    override val abandonedCopy = LossCopy(R.string.game2048_loss_rule_failure_eyebrow, R.string.game2048_loss_rule_failure_title, R.string.game2048_loss_rule_failure_body)
    override val quickStart = Game2048QuickStart
    override val iconHue = Color(0xFFB9822B)
    override val iconGlyph: List<GlyphShape> = listOf(
        GlyphRect(1.5f, 1.5f, 9.5f, 9.5f, 2f, GlyphPaint.NEUTRAL),
        GlyphText(6.25f, 6.4f, "2", 6f, GlyphPaint.MARK),
        GlyphRect(13f, 1.5f, 9.5f, 9.5f, 2f, GlyphPaint.NEUTRAL),
        GlyphText(17.75f, 6.4f, "2", 6f, GlyphPaint.MARK),
        GlyphRect(1.5f, 13f, 21f, 9.5f, 2f, GlyphPaint.ACCENT),
        GlyphText(12f, 17.9f, "4", 6.5f, GlyphPaint.CUTOUT),
    )

    @Composable
    override fun PlayScreen(request: PlayRequest) {
        Game2048PlayScreen(
            difficulty = request.difficulty,
            resume = request.resume,
            onBack = request.onBack,
            onFinished = { result ->
                CompletionExtras.set(CompletionHighlight.BestTile(result.bestTile))
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
