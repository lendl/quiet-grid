package com.quietgrid.app.games.wordguess

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.GameId
import com.quietgrid.app.session.ChallengerResult
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController
import com.quietgrid.app.ui.play.GamePlayScaffold
import com.quietgrid.app.ui.play.InfoItem
import com.quietgrid.app.ui.play.PlayTimer
import com.quietgrid.app.ui.play.heartsInfo
import com.quietgrid.engine.wordguess.foldWordGuessKeyboardState
import kotlinx.coroutines.delay

@Composable
fun WordGuessChallengerPlayScreen(
    onFinished: (ChallengerResult) -> Unit,
) {
    val viewModel = hiltViewModel<WordGuessChallengerViewModel>()

    CollectPuzzleResult(viewModel.result, onFinished)

    BackHandler { viewModel.endRun() }

    var currentInput by remember { mutableStateOf("") }
    var invalidFlash by remember { mutableStateOf(false) }
    val session = viewModel.session

    val haptics = rememberHapticController()
    var flashTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(viewModel.wrongGuessTrigger) {
        if (viewModel.wrongGuessTrigger > 0) {
            haptics.incorrectFeedback()
            flashTrigger++
        }
    }
    LaunchedEffect(viewModel.puzzleWonTrigger) {
        if (viewModel.puzzleWonTrigger > 0) haptics.correctFeedback()
    }
    LaunchedEffect(viewModel.puzzleLostTrigger) {
        if (viewModel.puzzleLostTrigger > 0) {
            haptics.incorrectFeedback()
            flashTrigger++
        }
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

    val info = if (session == null) {
        emptyList()
    } else {
        listOf(
            heartsInfo(session.livesRemaining, WORDGUESS_CHALLENGER_STARTING_LIVES, viewModel.puzzleLostTrigger),
            InfoItem.Label("${stringResource(R.string.wordguess_challenger_tier_label)}: ${stringResource(wordGuessDifficultyLabelRes(session.tier))}"),
            InfoItem.Label("${stringResource(R.string.wordguess_challenger_solved_label)}: ${session.puzzlesSolved}"),
        )
    }

    GamePlayScaffold(
        gameId = GameId.WORDGUESS,
        timer = session?.let { PlayTimer.Countdown(it.secondsRemaining.toInt()) },
        onBack = viewModel::endRun,
        onEndPuzzle = viewModel::endRun,
        info = info,
        boardAspectRatio = (session?.puzzleSession?.wordLength ?: 5).toFloat() / WORD_GUESS_MAX_GUESSES,
        wideDock = true,
        phonePortraitOnly = true,
        dockHeight = ::wordGuessDockHeight,
        dock = {
            if (session != null) {
                val puzzle = session.puzzleSession
                WordGuessDock(
                    revealedWord = if (puzzle.status == WordGuessStatus.LOST) puzzle.targetWord else null,
                    invalidFlash = invalidFlash,
                    keyboard = if (puzzle.status == WordGuessStatus.PLAYING) {
                        {
                            WordGuessKeyboard(
                                keyboardState = foldWordGuessKeyboardState(puzzle.guesses.map { it.guess to it.feedback }),
                                onLetter = { ch -> if (currentInput.length < puzzle.wordLength) currentInput += ch },
                                onBackspace = { currentInput = currentInput.dropLast(1) },
                                onEnter = {
                                    if (currentInput.length == puzzle.wordLength) {
                                        viewModel.onSubmitGuess(currentInput) { invalidFlash = true; haptics.incorrectFeedback() }
                                    }
                                },
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
                playFresh = true,
                zoomable = false,
                showFrame = false,
                flashTrigger = flashTrigger,
            ) {
                session?.let { current ->
                    WordGuessGrid(
                        wordLength = current.puzzleSession.wordLength,
                        maxGuesses = WORD_GUESS_MAX_GUESSES,
                        guesses = current.puzzleSession.guesses,
                        currentInput = currentInput,
                    )
                }
            }
        },
    )
}
