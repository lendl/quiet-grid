package com.quietgrid.app.games

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.standardDifficultyLabelRes
import com.quietgrid.app.session.ChallengerConfig
import com.quietgrid.app.session.ChallengerResult
import com.quietgrid.app.session.LossReason
import com.quietgrid.app.ui.components.GlyphShape
import com.quietgrid.app.ui.components.QuickStartContent

data class GameCapabilities(
    val hints: Boolean = false,
    val pinchZoom: Boolean = false,
    val mistakeCheck: Boolean = false,
    val phonePortraitOnly: Boolean = false,
    val flawlessEligible: Boolean = false,
    val accuracyTracked: Boolean = false,
    val scoreOnLossTracked: Boolean = false,
    val themed: Boolean = false,
    val alwaysKeepScreenOn: Boolean = false,
)

data class LossCopy(@StringRes val eyebrowRes: Int, @StringRes val titleRes: Int, @StringRes val bodyRes: Int)

private val SHARED_ABANDONED_COPY = LossCopy(
    R.string.loss_abandoned_eyebrow,
    R.string.loss_abandoned_title,
    R.string.loss_abandoned_body,
)

data class ChallengerEndCopy(@StringRes val titleRes: Int, @StringRes val bodyRes: Int)

class ChallengerSpec(
    val config: ChallengerConfig,
    @StringRes val descriptionRes: Int = R.string.challenger_description,
    @StringRes val puzzlesSolvedLabelRes: Int = R.string.challenger_result_puzzles_solved,
    val endCopy: Map<String, ChallengerEndCopy> = emptyMap(),
    val playScreen: @Composable (onFinished: (ChallengerResult) -> Unit) -> Unit,
)

interface GameDefinition {
    val id: GameId
    val capabilities: GameCapabilities
    val quickStart: QuickStartContent
    val iconHue: Color
    val iconGlyph: List<GlyphShape>
    val challenger: ChallengerSpec?
        get() = null
    val abandonedCopy: LossCopy
        get() = SHARED_ABANDONED_COPY
    val ruleFailureCopy: LossCopy?
        get() = null
    val analyzerScreen: (@Composable (snapshot: String?) -> Unit)?
        get() = null

    fun lossCopy(reason: LossReason): LossCopy =
        if (reason == LossReason.ABANDONED) abandonedCopy else ruleFailureCopy ?: abandonedCopy

    @StringRes
    fun difficultyLabelRes(difficulty: Difficulty): Int = standardDifficultyLabelRes(difficulty)

    fun journeyLocale(puzzleLanguage: String): String = ""

    fun journeyLocaleOfStop(stopId: String): String? = null

    @Composable
    fun PlayScreen(request: PlayRequest)
}
