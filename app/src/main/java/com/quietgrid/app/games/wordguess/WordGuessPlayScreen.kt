package com.quietgrid.app.games.wordguess

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController
import com.quietgrid.app.ui.play.GamePlayScaffold
import com.quietgrid.app.ui.play.InfoItem
import com.quietgrid.app.ui.play.PlayTimer
import com.quietgrid.app.ui.play.puzzleLanguageInfo
import com.quietgrid.engine.wordguess.foldWordGuessKeyboardState
import java.time.LocalDate
import kotlinx.coroutines.delay

@Composable
fun WordGuessPlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    dailyDate: LocalDate? = null,
    onBack: () -> Unit,
    onFinished: (WordGuessResult) -> Unit,
) {
    val viewModel = hiltViewModel<WordGuessPlayViewModel, WordGuessPlayViewModel.Factory>(
        creationCallback = { factory -> factory.create(difficulty, resume, dailyDate) },
    )
    CollectPuzzleResult(viewModel.result, onFinished)
    val finishing by viewModel.finishing.collectAsState()

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
        gameId = GameId.WORDGUESS,
        timer = PlayTimer.Elapsed(viewModel.elapsedSeconds.toInt()),
        onBack = onBack,
        onEndPuzzle = { viewModel.endPuzzle() },
        finishing = finishing,
        foldRows = WORD_GUESS_MAX_GUESSES,
        foldCols = session?.wordLength,
        difficulty = session?.let { Difficulty.fromKey(it.difficulty) },
        info = if (session == null) {
            emptyList()
        } else {
            listOfNotNull(
                InfoItem.Label(
                    stringResource(R.string.wordguess_guess_count, (session.guesses.size + 1).coerceAtMost(WORD_GUESS_MAX_GUESSES), WORD_GUESS_MAX_GUESSES),
                ),
                puzzleLanguageInfo(session.locale),
            )
        },
        boardAspectRatio = (session?.wordLength ?: 5).toFloat() / WORD_GUESS_MAX_GUESSES,
        wideDock = true,
        phonePortraitOnly = true,
        dockHeight = ::wordGuessDockHeight,
        dock = {
            if (session != null) {
                WordGuessDock(
                    revealedWord = if (session.status == WordGuessStatus.LOST) session.targetWord else null,
                    invalidFlash = invalidFlash,
                    keyboard = if (session.status == WordGuessStatus.PLAYING) {
                        {
                            WordGuessKeyboard(
                                keyboardState = foldWordGuessKeyboardState(session.guesses.map { it.guess to it.feedback }),
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
                    WordGuessGrid(
                        wordLength = session.wordLength,
                        maxGuesses = WORD_GUESS_MAX_GUESSES,
                        guesses = session.guesses,
                        currentInput = currentInput,
                    )
                }
            }
        },
    )
}

fun wordGuessDifficultyLabelRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.wordguess_difficulty_easy
    Difficulty.MEDIUM -> R.string.wordguess_difficulty_medium
    Difficulty.HARD -> R.string.wordguess_difficulty_hard
    Difficulty.EXPERT -> R.string.wordguess_difficulty_expert
}

fun wordGuessDifficultyDescriptionRes(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> R.string.wordguess_difficulty_desc_easy
    Difficulty.MEDIUM -> R.string.wordguess_difficulty_desc_medium
    Difficulty.HARD -> R.string.wordguess_difficulty_desc_hard
    Difficulty.EXPERT -> R.string.wordguess_difficulty_desc_expert
}
