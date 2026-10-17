package com.quietgrid.app.games.guessbynumbers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.games.wordguess.WordGuessDock
import com.quietgrid.app.games.wordguess.WordGuessKeyboard
import com.quietgrid.app.games.wordguess.wordGuessDockHeight
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.FeedbackText
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController
import com.quietgrid.app.ui.play.GamePlayScaffold
import com.quietgrid.app.ui.play.InfoItem
import com.quietgrid.app.ui.play.PlayTimer
import com.quietgrid.app.ui.play.puzzleLanguageInfo
import kotlinx.coroutines.delay

@Composable
fun GuessByNumbersPlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    onBack: () -> Unit,
    onFinished: (GuessByNumbersResult) -> Unit,
) {
    val viewModel = hiltViewModel<GuessByNumbersPlayViewModel, GuessByNumbersPlayViewModel.Factory>(
        creationCallback = { factory -> factory.create(difficulty, resume) },
    )
    CollectPuzzleResult(viewModel.result, onFinished)

    var currentInput by remember { mutableStateOf("") }
    var invalidFlash by remember { mutableStateOf(false) }

    val session = viewModel.session

    val haptics = rememberHapticController()
    LaunchedEffect(viewModel.wrongGuessTrigger) {
        if (viewModel.wrongGuessTrigger > 0) haptics.incorrectFeedback()
    }

    LaunchedEffect(invalidFlash) {
        if (invalidFlash) {
            delay(500)
            invalidFlash = false
        }
    }

    LaunchedEffect(session?.guesses?.size) {
        currentInput = ""
    }

    GamePlayScaffold(
        gameId = GameId.GUESSBYNUMBERS,
        timer = PlayTimer.Elapsed(viewModel.elapsedSeconds.toInt()),
        onBack = onBack,
        onEndPuzzle = { viewModel.endPuzzle() },
        difficulty = session?.let { Difficulty.fromKey(it.difficulty) },
        info = if (session == null) {
            emptyList()
        } else {
            listOfNotNull(
                InfoItem.Label(
                    stringResource(
                        R.string.wordguess_guess_count,
                        (session.guesses.size + 1).coerceAtMost(GUESS_BY_NUMBERS_MAX_GUESSES),
                        GUESS_BY_NUMBERS_MAX_GUESSES,
                    ),
                ),
                puzzleLanguageInfo(session.locale),
            )
        },
        boardAspectRatio = GUESS_BY_NUMBERS_BOARD_ASPECT,
        wideDock = true,
        phonePortraitOnly = true,
        dockHeight = ::wordGuessDockHeight,
        dock = {
            if (session != null) {
                WordGuessDock(
                    revealedWord = session.targetWord.takeIf { session.status == GuessByNumbersStatus.LOST },
                    invalidFlash = invalidFlash,
                    invalidMessageRes = R.string.guessbynumbers_invalid_word_message,
                    keyboard = if (session.status == GuessByNumbersStatus.PLAYING) {
                        {
                            WordGuessKeyboard(
                                keyboardState = emptyMap(),
                                onLetter = { ch -> if (currentInput.length < session.wordLength) currentInput += ch },
                                onBackspace = { currentInput = currentInput.dropLast(1) },
                                onEnter = {
                                    if (currentInput.length == session.wordLength) {
                                        viewModel.onSubmitGuess(currentInput) { invalidFlash = true; haptics.incorrectFeedback() }
                                    }
                                },
                                locale = session.locale,
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        },
        board = {
            PuzzleBoardContainer(
                visible = session != null,
                playFresh = !resume,
                zoomable = false,
                showFrame = false,
                flashTrigger = viewModel.wrongGuessTrigger,
            ) {
                if (session != null) {
                    GuessByNumbersGrid(
                        wordLength = session.wordLength,
                        maxGuesses = GUESS_BY_NUMBERS_MAX_GUESSES,
                        guesses = session.guesses,
                        currentInput = currentInput,
                    )
                }
            }
        },
    )
}

private const val GUESS_BY_NUMBERS_BOARD_ASPECT = 0.7f

@Composable
internal fun GuessByNumbersMessageArea(revealWord: String?, invalidFlash: Boolean, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        if (revealWord != null) {
            Text(
                "${stringResource(R.string.wordguess_reveal_word_label)}: ${revealWord.uppercase()}",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        FeedbackText(
            text = stringResource(R.string.guessbynumbers_invalid_word_message),
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.error),
            isCorrect = false,
            isIncorrect = invalidFlash,
            modifier = if (invalidFlash) Modifier else Modifier.alpha(0f).clearAndSetSemantics {},
        )
    }
}
