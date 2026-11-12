package com.quietgrid.app.games.chimptest

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
import com.quietgrid.app.ui.components.GlyphRect
import com.quietgrid.app.ui.components.GlyphShape
import com.quietgrid.app.ui.components.GlyphText

object ChimpTestDefinition : GameDefinition {
    override val id = GameId.CHIMPTEST
    override val capabilities = GameCapabilities()
    override val challenger = ChallengerSpec(
        config = CHIMPTEST_CHALLENGER_CONFIG,
        descriptionRes = R.string.chimp_challenger_description,
        endCopy = mapOf(
            "lives_exhausted" to ChallengerEndCopy(R.string.challenger_result_lives_exhausted_title, R.string.chimp_challenger_result_lives_exhausted_body),
        ),
        playScreen = { onFinished -> ChimpTestChallengerPlayScreen(onFinished = onFinished) },
    )
    override val abandonedCopy = LossCopy(R.string.loss_abandoned_eyebrow, R.string.chimp_loss_abandoned_title, R.string.chimp_loss_abandoned_body)
    override val ruleFailureCopy = LossCopy(R.string.chimp_loss_rule_failure_eyebrow, R.string.chimp_loss_rule_failure_title, R.string.chimp_loss_rule_failure_body)
    override val quickStart = ChimpTestQuickStart
    override val iconHue = Color(0xFFB24F7C)
    override val iconGlyph: List<GlyphShape> = listOf(
        GlyphRect(2f, 2.5f, 7.5f, 7.5f, 1.8f, GlyphPaint.ACCENT),
        GlyphText(5.75f, 6.3f, "1", 5.2f, GlyphPaint.CUTOUT),
        GlyphRect(14.5f, 4.5f, 7.5f, 7.5f, 1.8f, GlyphPaint.NEUTRAL),
        GlyphRect(8.5f, 14.5f, 7.5f, 7.5f, 1.8f, GlyphPaint.NEUTRAL),
    )

    @Composable
    override fun PlayScreen(request: PlayRequest) {
        ChimpTestPlayScreen(
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
