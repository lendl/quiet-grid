package com.quietgrid.app.games.wordguess

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.data.AppContainer
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.ElapsedTimerText
import com.quietgrid.app.ui.components.EndPuzzleDialog
import com.quietgrid.app.ui.components.FeedbackText
import com.quietgrid.app.ui.components.GameBackButton
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberPuzzleViewModel
import com.quietgrid.engine.wordguess.foldWordGuessKeyboardState
import kotlinx.coroutines.delay

@Composable
fun WordGuessPlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    onBack: () -> Unit,
    onFinished: (WordGuessResult) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val viewModel = rememberPuzzleViewModel {
        WordGuessPlayViewModel(
            appContext = context,
            sessionRepository = AppContainer.sessionRepository,
            statsRepository = AppContainer.statsRepository,
            requestedDifficulty = difficulty,
            resume = resume,
        )
    }

    CollectPuzzleResult(viewModel.result, onFinished)

    var showEndDialog by remember { mutableStateOf(false) }
    var currentInput by remember { mutableStateOf("") }
    var invalidFlash by remember { mutableStateOf(false) }
    var extraKeys by remember { mutableStateOf<List<Char>>(emptyList()) }

    val session = viewModel.session

    LaunchedEffect(session?.locale) {
        val locale = session?.locale ?: return@LaunchedEffect
        val dictionary = WordGuessPuzzleBank.loadDictionary(context, locale)
        extraKeys = dictionary.flatMap { it.toList() }.filter { it !in 'a'..'z' }.distinct().sorted()
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

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GameBackButton(onBack)
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (session != null) {
                    Column {
                        Text(stringResource(R.string.wordguess_guesses_label), style = MaterialTheme.typography.labelSmall)
                        Text("${session.guesses.size} / $WORD_GUESS_MAX_GUESSES", style = MaterialTheme.typography.titleMedium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.wordguess_difficulty_label), style = MaterialTheme.typography.labelSmall)
                        Text(stringResource(wordGuessDifficultyLabelRes(Difficulty.fromKey(session.difficulty))), style = MaterialTheme.typography.titleMedium)
                    }
                }
                TextButton(onClick = { showEndDialog = true }) {
                    Text(stringResource(R.string.common_end_puzzle))
                }
            }
        }

        ElapsedTimerText(
            viewModel.elapsedSeconds.toInt(),
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        PuzzleBoardContainer(visible = session != null, playFresh = !resume, zoomable = false) {
            if (session != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WordGuessGrid(
                        wordLength = session.wordLength,
                        maxGuesses = WORD_GUESS_MAX_GUESSES,
                        guesses = session.guesses,
                        currentInput = currentInput,
                    )
                    if (session.status == WordGuessStatus.LOST) {
                        Text(
                            "${stringResource(R.string.wordguess_reveal_word_label)}: ${session.targetWord.uppercase()}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    if (invalidFlash) {
                        FeedbackText(
                            text = stringResource(R.string.wordguess_invalid_word_message),
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.error),
                            isCorrect = false,
                            isIncorrect = invalidFlash,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }

        if (session != null && session.status == WordGuessStatus.PLAYING) {
            val keyboardState = foldWordGuessKeyboardState(session.guesses.map { it.guess to it.feedback })
            WordGuessKeyboard(
                extraKeys = extraKeys,
                keyboardState = keyboardState,
                onLetter = { ch -> if (currentInput.length < session.wordLength) currentInput += ch },
                onBackspace = { currentInput = currentInput.dropLast(1) },
                onEnter = {
                    if (currentInput.length == session.wordLength) {
                        viewModel.onSubmitGuess(currentInput) { invalidFlash = true }
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
            viewModel.endPuzzle()
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
