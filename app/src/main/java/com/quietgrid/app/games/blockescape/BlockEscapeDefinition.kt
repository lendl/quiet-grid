package com.quietgrid.app.games.blockescape

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

object BlockEscapeDefinition : GameDefinition {
    override val id = GameId.BLOCKESCAPE
    override val capabilities = GameCapabilities(
        hints = true,
    )
    override val challenger = ChallengerSpec(
        config = BLOCKESCAPE_CHALLENGER_CONFIG,
        playScreen = { onFinished -> BlockEscapeChallengerPlayScreen(onFinished = onFinished) },
    )
    override val quickStart = BlockEscapeQuickStart
    override val iconHue = Color(0xFFB8664E)
    override val iconGlyph: List<GlyphShape> = listOf(
        GlyphRect(2f, 2f, 13f, 5.5f, 2f, GlyphPaint.NEUTRAL),
        GlyphRect(16.5f, 2f, 5.5f, 13f, 2f, GlyphPaint.NEUTRAL),
        GlyphRect(2f, 9.25f, 11f, 5.5f, 2f, GlyphPaint.ACCENT),
        GlyphRect(9f, 16.5f, 13f, 5.5f, 2f, GlyphPaint.NEUTRAL),
    )

    @Composable
    override fun PlayScreen(request: PlayRequest) {
        BlockEscapePlayScreen(
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
