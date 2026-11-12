package com.quietgrid.app.games.blockfill

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

object BlockFillDefinition : GameDefinition {
    override val id = GameId.BLOCKFILL
    override val capabilities = GameCapabilities()
    override val abandonedCopy = LossCopy(R.string.blockfill_loss_rule_failure_eyebrow, R.string.blockfill_loss_rule_failure_title, R.string.blockfill_loss_rule_failure_body)
    override val quickStart = BlockFillQuickStart
    override val iconHue = Color(0xFF2E8C80)
    override val iconGlyph: List<GlyphShape> = listOf(
        GlyphRect(1.5f, 1.5f, 6f, 6f, 1.5f, GlyphPaint.EMPTY),
        GlyphRect(9f, 1.5f, 6f, 6f, 1.5f, GlyphPaint.EMPTY),
        GlyphRect(16.5f, 1.5f, 6f, 6f, 1.5f, GlyphPaint.ACCENT),
        GlyphRect(1.5f, 9f, 6f, 6f, 1.5f, GlyphPaint.NEUTRAL),
        GlyphRect(9f, 9f, 6f, 6f, 1.5f, GlyphPaint.ACCENT),
        GlyphRect(16.5f, 9f, 6f, 6f, 1.5f, GlyphPaint.ACCENT),
        GlyphRect(1.5f, 16.5f, 6f, 6f, 1.5f, GlyphPaint.NEUTRAL),
        GlyphRect(9f, 16.5f, 6f, 6f, 1.5f, GlyphPaint.NEUTRAL),
        GlyphRect(16.5f, 16.5f, 6f, 6f, 1.5f, GlyphPaint.NEUTRAL),
    )

    @Composable
    override fun PlayScreen(request: PlayRequest) {
        BlockFillPlayScreen(
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
