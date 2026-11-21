package com.quietgrid.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.formatElapsed
import com.quietgrid.app.core.tips.QuietTip
import com.quietgrid.app.games.GameRegistry
import com.quietgrid.app.games.gameDifficultyLabelRes
import com.quietgrid.app.session.LossReason
import com.quietgrid.app.ui.components.OutlinedGlowButton
import com.quietgrid.app.ui.components.rememberHapticController
import com.quietgrid.app.ui.components.rememberReduceMotion

@Composable
fun LossScreen(
    gameId: GameId,
    difficulty: Difficulty,
    elapsedSeconds: Int,
    reason: LossReason,
    score: Int,
    isMixActive: Boolean,
    onAddToMix: () -> Unit,
    onRetry: () -> Unit,
    onOtherDifficulty: () -> Unit,
    onTryAnotherGame: () -> Unit,
    onWalkThroughSolve: () -> Unit,
    dailyDate: String? = null,
    onShareDaily: () -> Unit = {},
    onBackToDaily: () -> Unit = {},
    hasNextDaily: Boolean = false,
    onPlayNextDaily: () -> Unit = {},
    onBackToJourney: (() -> Unit)? = null,
    onOpenGamePage: (() -> Unit)? = null,
    tip: QuietTip? = null,
    tipActions: QuietTipActions = QuietTipActions(),
    onPrimaryTapped: (early: Boolean) -> Unit = {},
    newBadgeId: String? = null,
    onOpenMilestones: () -> Unit = {},
) {
    val haptics = rememberHapticController()
    LaunchedEffect(Unit) { if (reason != LossReason.ABANDONED) haptics.incorrectFeedback() }
    val lossCopy = GameRegistry.of(gameId).lossCopy(reason)
    val eyebrowRes = lossCopy.eyebrowRes
    val titleRes = lossCopy.titleRes
    val bodyRes = lossCopy.bodyRes
    val difficultyLabelRes = gameDifficultyLabelRes(gameId, difficulty)
    val abandoned = reason == LossReason.ABANDONED
    val errorColor = MaterialTheme.colorScheme.error

    val highlight = remember { CompletionExtras.consume() }
    val revealWord = (highlight as? CompletionHighlight.RevealWord)?.word?.takeIf { it.isNotEmpty() }
    val revealWordList = (highlight as? CompletionHighlight.RevealWordList)?.words?.takeIf { it.isNotEmpty() }
    val bestTile = (highlight as? CompletionHighlight.BestTile)?.tile
    val analyzerSnapshot = rememberSaveable { mutableStateOf(AnalyzerHandoff.consume()) }.value

    val entrance = rememberResultEntrance()
    val reduceMotion = rememberReduceMotion()

    val links = lossResultLinks(
        dailyDate = dailyDate,
        hasNextDaily = hasNextDaily,
        isMixActive = isMixActive,
        onShareDaily = onShareDaily,
        onBackToDaily = onBackToDaily,
        onOtherDifficulty = onOtherDifficulty,
        onTryAnotherGame = onTryAnotherGame,
        onBackToJourney = onBackToJourney,
    )
    val primaryAction: @Composable () -> Unit = {
        if (dailyDate != null) {
            DailyResultPrimaryButton(hasNextDaily, onPlayNextDaily, onShareDaily)
        } else {
            OutlinedGlowButton(
                onClick = {
                    if (!reduceMotion) onPrimaryTapped(!entrance.finished())
                    onRetry()
                },
            ) {
                Text(
                    stringResource(mixAwarePrimaryLabel(isMixActive, R.string.loss_try_again)),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
    val walkThrough: (() -> Unit)? = analyzerSnapshot?.let { snapshot ->
        {
            AnalyzerHandoff.set(snapshot)
            onWalkThroughSolve()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sideways = isSidewaysPhoneResult(maxWidth, maxHeight)
        val medallionSize = if (sideways) 72.dp else 96.dp
        val lossFigure: @Composable (Boolean) -> Unit = { shared ->
            ResultMedallion(gameId, errorColor, medallionSize, shared = shared) {
                LossGlyph(abandoned, errorColor, Modifier.size(medallionSize * GLYPH_SIZE_FRACTION))
            }
        }
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .badgesFadeIn(entrance),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier
                        .border(1.dp, errorColor, CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(Modifier.size(10.dp).background(errorColor, CircleShape))
                    Text(stringResource(eyebrowRes), style = MaterialTheme.typography.labelMedium, color = errorColor)
                }
                if (tip != null) {
                    QuietTipButton(
                        tip = tip,
                        actions = tipActions,
                        modifier = Modifier.offset(x = 12.dp),
                        ringWhen = { entrance.badgesAlpha() >= 1f },
                    )
                }
                ResultOverflowMenu(
                    gameId = gameId,
                    difficulty = difficulty,
                    onAddToMix = onAddToMix,
                    modifier = Modifier.offset(x = 12.dp),
                    onOpenGamePage = onOpenGamePage,
                )
            }

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (sideways) {
                        Row(
                            Modifier.widthIn(max = 560.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            Box(Modifier.heroSlot(entrance)) { lossFigure(true) }
                            Column {
                                Text(stringResource(titleRes), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.riseIn(entrance, 0))
                                Text(
                                    stringResource(bodyRes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp).riseIn(entrance, 1),
                                )
                                Column(Modifier.riseIn(entrance, 2)) {
                                    LossReveals(revealWord, revealWordList, centered = false)
                                }
                            }
                        }

                        Row(
                            Modifier.padding(top = 22.dp).riseIn(entrance, 3),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            LossMetaItems(gameId, difficultyLabelRes, elapsedSeconds, score, bestTile)
                        }
                        if (newBadgeId != null) {
                            NewBadgeLine(newBadgeId, onOpenMilestones, Modifier.padding(top = 10.dp).badgesFadeIn(entrance))
                        }

                        CompactResultActions(links, Modifier.padding(top = 22.dp).riseIn(entrance, 4), primaryAction)
                        if (walkThrough != null) {
                            TextButton(onClick = walkThrough, modifier = Modifier.padding(top = 4.dp).riseIn(entrance, 5)) {
                                Text(stringResource(R.string.walk_through_solve))
                            }
                        }
                    } else {
                        Box(Modifier.heroSlot(entrance)) { lossFigure(true) }

                        Text(
                            stringResource(titleRes),
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 12.dp).riseIn(entrance, 0),
                        )
                        Text(
                            stringResource(bodyRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp).riseIn(entrance, 1),
                        )
                        Column(Modifier.riseIn(entrance, 2), horizontalAlignment = Alignment.CenterHorizontally) {
                            LossReveals(revealWord, revealWordList, centered = true)
                        }

                        Row(
                            Modifier.padding(top = 20.dp).riseIn(entrance, 3),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            LossMetaItems(gameId, difficultyLabelRes, elapsedSeconds, score, bestTile)
                        }
                        if (newBadgeId != null) {
                            NewBadgeLine(newBadgeId, onOpenMilestones, Modifier.padding(top = 14.dp).badgesFadeIn(entrance))
                        }

                        Box(Modifier.padding(top = 24.dp).riseIn(entrance, 4)) { primaryAction() }

                        if (walkThrough != null) {
                            TextButton(onClick = walkThrough, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).riseIn(entrance, 5)) {
                                Text(stringResource(R.string.walk_through_solve))
                            }
                        }

                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp).riseIn(entrance, 6),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ResultLinkGroup(links)
                        }
                    }
                }
            }
        }
        ResultHeroFlight(entrance) { lossFigure(false) }
    }
}

private const val GLYPH_SIZE_FRACTION = 0.42f

internal fun lossResultLinks(
    dailyDate: String?,
    hasNextDaily: Boolean,
    isMixActive: Boolean,
    onShareDaily: () -> Unit,
    onBackToDaily: () -> Unit,
    onOtherDifficulty: () -> Unit,
    onTryAnotherGame: () -> Unit,
    onBackToJourney: (() -> Unit)?,
): List<ResultLink> = when {
    dailyDate != null -> dailyResultLinks(hasNextDaily, onShareDaily, onBackToDaily, onTryAnotherGame)
    onBackToJourney != null -> listOf(
        ResultLink(R.string.journey_back_to_map, onBackToJourney),
        ResultLink(R.string.loss_try_another_game, onTryAnotherGame),
    )
    else -> buildList {
        if (!isMixActive) add(ResultLink(R.string.loss_other_difficulty, onOtherDifficulty))
        add(ResultLink(R.string.loss_try_another_game, onTryAnotherGame))
    }
}

@Composable
private fun LossReveals(revealWord: String?, revealWordList: List<String>?, centered: Boolean) {
    if (revealWord != null) {
        RevealedAnswer(R.string.wordguess_reveal_word_label, revealWord.uppercase(), centered)
    }
    if (revealWordList != null) {
        RevealedAnswer(R.string.themeclear_reveal_words_label, revealWordList.joinToString(" · "), centered)
    }
}

@Composable
private fun RevealedAnswer(labelRes: Int, answer: String, centered: Boolean) {
    Column(
        Modifier.padding(top = if (centered) 10.dp else 6.dp),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Text(
            stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            answer,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun LossMetaItems(gameId: GameId, difficultyLabelRes: Int, elapsedSeconds: Int, score: Int, bestTile: Int?) {
    MetaItem(stringResource(R.string.loss_difficulty), stringResource(difficultyLabelRes))
    ResultMetaDivider()
    MetaItem(stringResource(R.string.loss_elapsed_time), formatElapsed(elapsedSeconds))
    if (GameRegistry.of(gameId).capabilities.scoreOnLossTracked) {
        ResultMetaDivider()
        MetaItem(stringResource(R.string.completion_score), score.toString())
    }
    if (bestTile != null) {
        ResultMetaDivider()
        MetaItem(stringResource(R.string.completion_best_tile), bestTile.toString())
    }
}

@Composable
private fun MetaItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
    }
}
