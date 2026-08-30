package com.quietgrid.app.games.wordguess

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.formatElapsed
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.EndPuzzleDialog
import com.quietgrid.app.ui.components.EndPuzzleIconButton
import com.quietgrid.app.ui.components.FeedbackText
import com.quietgrid.app.ui.components.GameBackButton
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController
import com.quietgrid.engine.wordguess.foldWordGuessKeyboardState
import kotlinx.coroutines.delay

@Composable
fun WordGuessChallengerPlayScreen(
    onFinished: (WordGuessChallengerResult) -> Unit,
) {
    val viewModel = hiltViewModel<WordGuessChallengerViewModel>()

    CollectPuzzleResult(viewModel.result, onFinished)

    BackHandler { viewModel.endRun() }

    var showEndDialog by remember { mutableStateOf(false) }
    var currentInput by remember { mutableStateOf("") }
    var invalidFlash by remember { mutableStateOf(false) }
    val session = viewModel.session

    val haptics = rememberHapticController()
    LaunchedEffect(viewModel.wrongGuessTrigger) {
        if (viewModel.wrongGuessTrigger > 0) haptics.incorrectFeedback()
    }
    LaunchedEffect(viewModel.puzzleWonTrigger) {
        if (viewModel.puzzleWonTrigger > 0) haptics.correctFeedback()
    }
    LaunchedEffect(viewModel.puzzleLostTrigger) {
        if (viewModel.puzzleLostTrigger > 0) haptics.incorrectFeedback()
    }

    LaunchedEffect(invalidFlash) {
        if (invalidFlash) {
            delay(500)
            invalidFlash = false
        }
    }

    LaunchedEffect(session?.puzzleSession?.puzzleId, session?.puzzleSession?.guesses?.size) {
        currentInput = ""
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GameBackButton(onBack = viewModel::endRun)
            Text(
                stringResource(R.string.wordguess_challenger_label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 12.dp).weight(1f),
            )
            if (session != null) {
                Row(Modifier.padding(end = 8.dp)) {
                    repeat(WORDGUESS_CHALLENGER_STARTING_LIVES) { index ->
                        ChallengerHeartIcon(
                            filled = index < session.livesRemaining,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                }
            }
            EndPuzzleIconButton(onClick = { showEndDialog = true })
        }

        if (session != null) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${stringResource(R.string.wordguess_challenger_tier_label)}: ${stringResource(wordGuessDifficultyLabelRes(session.tier))}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "${stringResource(R.string.wordguess_challenger_solved_label)}: ${session.puzzlesSolved}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                val secondsInt = maxOf(0, session.secondsRemaining.toInt())
                val lowTime = session.secondsRemaining <= 10
                val timePulse = remember { Animatable(1f) }
                LaunchedEffect(secondsInt) {
                    if (lowTime && secondsInt > 0) {
                        timePulse.snapTo(1f)
                        timePulse.animateTo(
                            targetValue = 1f,
                            animationSpec = keyframes {
                                durationMillis = 300
                                1f at 0
                                1.35f at 120
                                1f at 300
                            },
                        )
                    }
                }
                Text(
                    formatElapsed(secondsInt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (lowTime) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.graphicsLayer(scaleX = timePulse.value, scaleY = timePulse.value),
                )
            }
        }

        PuzzleBoardContainer(visible = session != null, playFresh = true, zoomable = false) {
            session?.let { current ->
                Box(Modifier.fillMaxSize()) {
                    WordGuessGrid(
                        wordLength = current.puzzleSession.wordLength,
                        maxGuesses = WORD_GUESS_MAX_GUESSES,
                        guesses = current.puzzleSession.guesses,
                        currentInput = currentInput,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                    ) {
                        if (current.puzzleSession.status == WordGuessStatus.LOST) {
                            Text(
                                "${stringResource(R.string.wordguess_reveal_word_label)}: ${current.puzzleSession.targetWord.uppercase()}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        if (invalidFlash) {
                            FeedbackText(
                                text = stringResource(R.string.wordguess_invalid_word_message),
                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.error),
                                isCorrect = false,
                                isIncorrect = invalidFlash,
                            )
                        }
                    }
                }
            }
        }

        if (session != null && session.puzzleSession.status == WordGuessStatus.PLAYING) {
            val keyboardState = foldWordGuessKeyboardState(session.puzzleSession.guesses.map { it.guess to it.feedback })
            WordGuessKeyboard(
                keyboardState = keyboardState,
                onLetter = { ch -> if (currentInput.length < session.puzzleSession.wordLength) currentInput += ch },
                onBackspace = { currentInput = currentInput.dropLast(1) },
                onEnter = {
                    if (currentInput.length == session.puzzleSession.wordLength) {
                        viewModel.onSubmitGuess(currentInput) { invalidFlash = true; haptics.incorrectFeedback() }
                    }
                },
            )
        }
    }

    EndPuzzleDialog(
        visible = showEndDialog,
        onDismiss = { showEndDialog = false },
        onConfirm = {
            showEndDialog = false
            viewModel.endRun()
        },
    )
}

@Composable
private fun ChallengerHeartIcon(filled: Boolean, modifier: Modifier = Modifier) {
    Icon(
        if (filled) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
        modifier = modifier,
    )
}
