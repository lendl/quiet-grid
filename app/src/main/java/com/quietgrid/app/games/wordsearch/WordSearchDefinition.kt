package com.quietgrid.app.games.wordsearch

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.quietgrid.app.R
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.themes.themeIcon
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

object WordSearchDefinition : GameDefinition {
    override val id = GameId.WORDSEARCH
    override val capabilities = GameCapabilities(
        hints = true,
        pinchZoom = true,
        accuracyTracked = true,
        themed = true,
    )
    override val abandonedCopy = LossCopy(R.string.loss_abandoned_eyebrow, R.string.wordsearch_loss_abandoned_title, R.string.wordsearch_loss_abandoned_body)
    override val quickStart = WordSearchQuickStart
    override val iconHue = Color(0xFFC2475A)
    override val iconGlyph: List<GlyphShape> = listOf(
        GlyphText(4.5f, 4.7f, "R", 5.2f, GlyphPaint.MARK),
        GlyphText(12f, 4.7f, "K", 5.2f, GlyphPaint.MARK),
        GlyphText(19.5f, 4.7f, "S", 5.2f, GlyphPaint.MARK),
        GlyphRect(1.5f, 8.4f, 21f, 7.2f, 3.6f, GlyphPaint.ACCENT),
        GlyphText(4.5f, 12.2f, "C", 5.2f, GlyphPaint.CUTOUT),
        GlyphText(12f, 12.2f, "A", 5.2f, GlyphPaint.CUTOUT),
        GlyphText(19.5f, 12.2f, "T", 5.2f, GlyphPaint.CUTOUT),
        GlyphText(4.5f, 19.7f, "N", 5.2f, GlyphPaint.MARK),
        GlyphText(12f, 19.7f, "E", 5.2f, GlyphPaint.MARK),
        GlyphText(19.5f, 19.7f, "P", 5.2f, GlyphPaint.MARK),
    )

    @Composable
    override fun PlayScreen(request: PlayRequest) {
        WordSearchPlayScreen(
            difficulty = request.difficulty,
            resume = request.resume,
            dailyDate = request.dailyDate,
            onBack = request.onBack,
            onFinished = { result ->
                if (result.solved) themeIcon(result.themeId)?.let { CompletionExtras.set(CompletionHighlight.ThemeIcon(it)) }
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
