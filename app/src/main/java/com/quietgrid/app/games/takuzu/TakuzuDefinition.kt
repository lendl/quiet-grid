package com.quietgrid.app.games.takuzu

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
import com.quietgrid.app.ui.components.GlyphText
import com.quietgrid.app.ui.screens.AnalyzerHandoff

object TakuzuDefinition : GameDefinition {
    override val id = GameId.TAKUZU
    override val capabilities = GameCapabilities(
        hints = true,
        pinchZoom = true,
        mistakeCheck = true,
        flawlessEligible = true,
        accuracyTracked = true,
    )
    override val challenger = ChallengerSpec(
        config = TAKUZU_CHALLENGER_CONFIG,
        playScreen = { onFinished -> TakuzuChallengerPlayScreen(onFinished = onFinished) },
    )
    override val analyzerScreen: @Composable (String?) -> Unit = { snapshot -> TakuzuAnalyzerScreen(snapshot = snapshot) }
    override val quickStart = TakuzuQuickStart
    override val iconHue = Color(0xFF2A85A6)
    override val iconGlyph: List<GlyphShape> = listOf(
        GlyphRect(1.5f, 1.5f, 9.5f, 9.5f, 2f, GlyphPaint.ACCENT),
        GlyphText(6.25f, 6.4f, "1", 6.5f, GlyphPaint.CUTOUT),
        GlyphRect(13f, 1.5f, 9.5f, 9.5f, 2f, GlyphPaint.EMPTY),
        GlyphText(17.75f, 6.4f, "0", 6.5f, GlyphPaint.MARK),
        GlyphRect(1.5f, 13f, 9.5f, 9.5f, 2f, GlyphPaint.EMPTY),
        GlyphText(6.25f, 17.9f, "0", 6.5f, GlyphPaint.MARK),
        GlyphRect(13f, 13f, 9.5f, 9.5f, 2f, GlyphPaint.EMPTY),
        GlyphText(17.75f, 17.9f, "1", 6.5f, GlyphPaint.MARK),
    )

    @Composable
    override fun PlayScreen(request: PlayRequest) {
        TakuzuPlayScreen(
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
                        accuracyPct = result.accuracyPct,
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
