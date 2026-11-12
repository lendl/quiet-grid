package com.quietgrid.app.games.sequencememory

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

object SequenceMemoryDefinition : GameDefinition {
    override val id = GameId.SEQUENCEMEMORY
    override val capabilities = GameCapabilities()
    override val abandonedCopy = LossCopy(R.string.sequencememory_loss_rule_failure_eyebrow, R.string.sequencememory_loss_rule_failure_title, R.string.sequencememory_loss_rule_failure_body)
    override val quickStart = SequenceMemoryQuickStart
    override val iconHue = Color(0xFFCC4F8F)
    override val iconGlyph: List<GlyphShape> = listOf(
        GlyphRect(1.5f, 1.5f, 9.5f, 9.5f, 4f, GlyphPaint.NEUTRAL),
        GlyphRect(13f, 1.5f, 9.5f, 9.5f, 4f, GlyphPaint.ACCENT),
        GlyphRect(1.5f, 13f, 9.5f, 9.5f, 4f, GlyphPaint.EMPTY),
        GlyphRect(13f, 13f, 9.5f, 9.5f, 4f, GlyphPaint.NEUTRAL),
    )

    @Composable
    override fun PlayScreen(request: PlayRequest) {
        SequenceMemoryPlayScreen(
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
