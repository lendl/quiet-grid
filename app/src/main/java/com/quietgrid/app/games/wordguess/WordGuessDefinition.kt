package com.quietgrid.app.games.wordguess

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
import com.quietgrid.app.ui.screens.CompletionExtras
import com.quietgrid.app.ui.screens.CompletionHighlight

object WordGuessDefinition : GameDefinition {
    override val id = GameId.WORDGUESS
    override val capabilities = GameCapabilities(
        phonePortraitOnly = true,
    )
    override val challenger = ChallengerSpec(
        config = WORDGUESS_CHALLENGER_CONFIG,
        descriptionRes = R.string.wordguess_challenger_description,
        puzzlesSolvedLabelRes = R.string.wordguess_challenger_result_puzzles_solved,
        endCopy = mapOf(
            "lives_exhausted" to ChallengerEndCopy(R.string.challenger_result_lives_exhausted_title, R.string.wordguess_challenger_result_lives_exhausted_body),
        ),
        playScreen = { onFinished -> WordGuessChallengerPlayScreen(onFinished = onFinished) },
    )
    override val abandonedCopy = LossCopy(R.string.wordguess_loss_rule_failure_eyebrow, R.string.wordguess_loss_rule_failure_title, R.string.wordguess_loss_rule_failure_body)
    override val quickStart = WordGuessQuickStart
    override val iconHue = Color(0xFF9C6B3A)
    override val iconGlyph: List<GlyphShape> = listOf(
        GlyphRect(1.5f, 1.5f, 6f, 6f, 1.5f, GlyphPaint.NEUTRAL),
        GlyphText(4.5f, 4.7f, "C", 5.2f, GlyphPaint.MARK),
        GlyphRect(9f, 1.5f, 6f, 6f, 1.5f, GlyphPaint.ACCENT),
        GlyphText(12f, 4.7f, "A", 5.2f, GlyphPaint.CUTOUT),
        GlyphRect(16.5f, 1.5f, 6f, 6f, 1.5f, GlyphPaint.EMPTY),
        GlyphText(19.5f, 4.7f, "T", 5.2f, GlyphPaint.MARK),
        GlyphRect(1.5f, 9f, 6f, 6f, 1.5f, GlyphPaint.ACCENT),
        GlyphText(4.5f, 12.2f, "C", 5.2f, GlyphPaint.CUTOUT),
        GlyphRect(9f, 9f, 6f, 6f, 1.5f, GlyphPaint.ACCENT),
        GlyphText(12f, 12.2f, "A", 5.2f, GlyphPaint.CUTOUT),
        GlyphRect(16.5f, 9f, 6f, 6f, 1.5f, GlyphPaint.EMPTY),
        GlyphText(19.5f, 12.2f, "R", 5.2f, GlyphPaint.MARK),
        GlyphRect(1.5f, 16.5f, 6f, 6f, 1.5f, GlyphPaint.EMPTY),
        GlyphRect(9f, 16.5f, 6f, 6f, 1.5f, GlyphPaint.EMPTY),
        GlyphRect(16.5f, 16.5f, 6f, 6f, 1.5f, GlyphPaint.EMPTY),
    )

    @Composable
    override fun PlayScreen(request: PlayRequest) {
        WordGuessPlayScreen(
            difficulty = request.difficulty,
            resume = request.resume,
            dailyDate = request.dailyDate,
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
