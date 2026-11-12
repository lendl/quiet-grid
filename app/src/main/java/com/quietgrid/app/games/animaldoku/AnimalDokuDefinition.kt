package com.quietgrid.app.games.animaldoku

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
import com.quietgrid.app.ui.components.GlyphOval
import com.quietgrid.app.ui.components.GlyphPaint
import com.quietgrid.app.ui.components.GlyphRect
import com.quietgrid.app.ui.components.GlyphShape
import com.quietgrid.app.ui.screens.AnalyzerHandoff

object AnimalDokuDefinition : GameDefinition {
    override val id = GameId.ANIMALDOKU
    override val capabilities = GameCapabilities(
        hints = true,
    )
    override val challenger = ChallengerSpec(
        config = ANIMALDOKU_CHALLENGER_CONFIG,
        endCopy = mapOf(
            "lives_exhausted" to ChallengerEndCopy(R.string.challenger_result_lives_exhausted_title, R.string.animaldoku_challenger_result_lives_exhausted_body),
        ),
        playScreen = { onFinished -> AnimalDokuChallengerPlayScreen(onFinished = onFinished) },
    )
    override val abandonedCopy = LossCopy(R.string.animaldoku_loss_rule_failure_eyebrow, R.string.animaldoku_loss_rule_failure_title, R.string.animaldoku_loss_rule_failure_body)
    override val quickStart = AnimalDokuQuickStart
    override val iconHue = Color(0xFFC25E3C)
    override val iconGlyph: List<GlyphShape> = listOf(
        GlyphRect(1.5f, 1.5f, 9.5f, 9.5f, 2f, GlyphPaint.ACCENT),
        GlyphOval(6.25f, 7.6f, 2.1f, 1.7f, GlyphPaint.CUTOUT),
        GlyphOval(3.9f, 4.6f, 1f, 1f, GlyphPaint.CUTOUT),
        GlyphOval(6.25f, 3.8f, 1f, 1f, GlyphPaint.CUTOUT),
        GlyphOval(8.6f, 4.6f, 1f, 1f, GlyphPaint.CUTOUT),
        GlyphRect(13f, 1.5f, 9.5f, 9.5f, 2f, GlyphPaint.NEUTRAL),
        GlyphRect(1.5f, 13f, 9.5f, 9.5f, 2f, GlyphPaint.NEUTRAL),
        GlyphRect(13f, 13f, 9.5f, 9.5f, 2f, GlyphPaint.EMPTY),
    )

    @Composable
    override fun PlayScreen(request: PlayRequest) {
        AnimalDokuPlayScreen(
            difficulty = request.difficulty,
            resume = request.resume,
            dailyDate = request.dailyDate,
            journey = request.journey,
            onRestarted = request.onRestarted,
            onBack = request.onBack,
            onFinished = { result ->
                if (!result.solved) AnalyzerHandoff.set(result.analyzerSnapshot)
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
