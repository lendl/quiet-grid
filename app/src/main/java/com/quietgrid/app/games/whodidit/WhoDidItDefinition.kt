package com.quietgrid.app.games.whodidit

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.games.GameCapabilities
import com.quietgrid.app.games.GameDefinition
import com.quietgrid.app.games.GameFinish
import com.quietgrid.app.games.LossCopy
import com.quietgrid.app.games.PlayRequest
import com.quietgrid.app.ui.components.GlyphOval
import com.quietgrid.app.ui.components.GlyphPaint
import com.quietgrid.app.ui.components.GlyphPath
import com.quietgrid.app.ui.components.GlyphShape
import com.quietgrid.app.ui.screens.CompletionExtras
import com.quietgrid.app.ui.screens.CompletionHighlight

object WhoDidItDefinition : GameDefinition {
    override val id = GameId.WHODIDIT
    override val capabilities = GameCapabilities(
        hints = true,
        pinchZoom = true,
    )
    override val abandonedCopy = LossCopy(R.string.whodidit_loss_abandoned_eyebrow, R.string.whodidit_loss_abandoned_title, R.string.whodidit_loss_abandoned_body)
    override val quickStart = WhoDidItQuickStart
    override val iconHue = Color(0xFF8A3B4E)
    override val iconGlyph: List<GlyphShape> = listOf(
        GlyphOval(10f, 10f, 6.5f, 6.5f, GlyphPaint.EMPTY),
        GlyphOval(10f, 8.2f, 1.9f, 1.9f, GlyphPaint.ACCENT),
        GlyphPath("M6.4 13.6 C6.4 11.2 8 10.6 10 10.6 C12 10.6 13.6 11.2 13.6 13.6 Z", GlyphPaint.ACCENT),
        GlyphPath("M3.5 10 A6.5 6.5 0 1 0 16.5 10 A6.5 6.5 0 1 0 3.5 10", GlyphPaint.LINE_ACCENT, strokeWidth = 2.4f),
        GlyphPath("M15 15 L21.5 21.5", GlyphPaint.LINE_ACCENT, strokeWidth = 3.4f),
    )

    override fun difficultyLabelRes(difficulty: Difficulty): Int = whoDidItDifficultyLabelRes(difficulty)

    @Composable
    override fun PlayScreen(request: PlayRequest) {
        val resources = LocalContext.current.resources
        WhoDidItPlayScreen(
            difficulty = request.difficulty,
            resume = request.resume,
            dailyDate = request.dailyDate,
            onBack = request.onBack,
            onFinished = { result ->
                val revealPuzzle = result.reveal
                if (result.solved && revealPuzzle != null) CompletionExtras.set(CompletionHighlight.CaseSolved(WhoDidItText(resources, revealPuzzle).reveal()))
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
