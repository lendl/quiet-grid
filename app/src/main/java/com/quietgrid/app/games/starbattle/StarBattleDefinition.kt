package com.quietgrid.app.games.starbattle

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.quietgrid.app.R
import com.quietgrid.app.core.GameId
import com.quietgrid.app.games.ChallengerEndCopy
import com.quietgrid.app.games.ChallengerSpec
import com.quietgrid.app.games.GameCapabilities
import com.quietgrid.app.games.GameDefinition
import com.quietgrid.app.games.GameFinish
import com.quietgrid.app.games.LossCopy
import com.quietgrid.app.games.PlayRequest
import com.quietgrid.app.ui.components.GlyphPaint
import com.quietgrid.app.ui.components.GlyphPath
import com.quietgrid.app.ui.components.GlyphRect
import com.quietgrid.app.ui.components.GlyphShape

object StarBattleDefinition : GameDefinition {
    override val id = GameId.STARBATTLE
    override val capabilities = GameCapabilities()
    override val challenger = ChallengerSpec(
        config = STARBATTLE_CHALLENGER_CONFIG,
        endCopy = mapOf(
            "hearts_exhausted" to ChallengerEndCopy(R.string.starbattle_challenger_result_hearts_exhausted_title, R.string.starbattle_challenger_result_hearts_exhausted_body),
        ),
        playScreen = { onFinished -> StarBattleChallengerPlayScreen(onFinished = onFinished) },
    )
    override val abandonedCopy = LossCopy(R.string.starbattle_loss_rule_failure_eyebrow, R.string.starbattle_loss_rule_failure_title, R.string.starbattle_loss_rule_failure_body)
    override val quickStart = StarBattleQuickStart
    override val iconHue = Color(0xFF5653C2)
    override val iconGlyph: List<GlyphShape> = listOf(
        GlyphRect(1.5f, 1.5f, 6f, 6f, 1.5f, GlyphPaint.ACCENT),
        GlyphPath("M4.5 2.2 L5.12 3.95 L6.97 4 L5.5 5.12 L6.03 6.9 L4.5 5.85 L2.97 6.9 L3.5 5.12 L2.03 4 L3.88 3.95 Z", GlyphPaint.CUTOUT),
        GlyphRect(9f, 1.5f, 6f, 6f, 1.5f, GlyphPaint.NEUTRAL),
        GlyphRect(16.5f, 1.5f, 6f, 6f, 1.5f, GlyphPaint.NEUTRAL),
        GlyphRect(1.5f, 9f, 6f, 6f, 1.5f, GlyphPaint.NEUTRAL),
        GlyphRect(9f, 9f, 6f, 6f, 1.5f, GlyphPaint.NEUTRAL),
        GlyphRect(16.5f, 9f, 6f, 6f, 1.5f, GlyphPaint.NEUTRAL),
        GlyphRect(1.5f, 16.5f, 6f, 6f, 1.5f, GlyphPaint.NEUTRAL),
        GlyphRect(9f, 16.5f, 6f, 6f, 1.5f, GlyphPaint.ACCENT),
        GlyphPath("M12 17.2 L12.62 18.95 L14.47 19 L13 20.12 L13.53 21.9 L12 20.85 L10.47 21.9 L11 20.12 L9.53 19 L11.38 18.95 Z", GlyphPaint.CUTOUT),
        GlyphRect(16.5f, 16.5f, 6f, 6f, 1.5f, GlyphPaint.NEUTRAL),
    )

    @Composable
    override fun PlayScreen(request: PlayRequest) {
        StarBattlePlayScreen(
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
