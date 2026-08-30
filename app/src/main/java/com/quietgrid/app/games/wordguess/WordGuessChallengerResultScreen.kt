package com.quietgrid.app.games.wordguess

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty

@Composable
fun WordGuessChallengerResultScreen(
    puzzlesSolved: Int,
    tierReached: Difficulty,
    score: Int,
    isNewHighScore: Boolean,
    reason: String,
    onPlayAgain: () -> Unit,
    onBackToPuzzles: () -> Unit,
    onTryAnotherGame: () -> Unit,
) {
    val titleRes: Int
    val bodyRes: Int
    when (reason) {
        "lives_exhausted" -> {
            titleRes = R.string.wordguess_challenger_result_lives_exhausted_title
            bodyRes = R.string.wordguess_challenger_result_lives_exhausted_body
        }
        "abandoned" -> {
            titleRes = R.string.wordguess_challenger_result_abandoned_title
            bodyRes = R.string.wordguess_challenger_result_abandoned_body
        }
        else -> {
            titleRes = R.string.wordguess_challenger_result_time_up_title
            bodyRes = R.string.wordguess_challenger_result_time_up_body
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(titleRes), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Text(
                    stringResource(bodyRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )

                if (isNewHighScore) {
                    Text(
                        stringResource(R.string.wordguess_challenger_result_new_high_score),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }

                Row(
                    Modifier.padding(top = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    ResultMetaItem(stringResource(R.string.wordguess_challenger_result_puzzles_solved), puzzlesSolved.toString())
                    ResultMetaItem(stringResource(R.string.wordguess_challenger_result_tier_reached), stringResource(wordGuessDifficultyLabelRes(tierReached)))
                    ResultMetaItem(stringResource(R.string.wordguess_challenger_result_score), score.toString())
                }

                Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                    Text(stringResource(R.string.wordguess_challenger_play_again))
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(onClick = onBackToPuzzles) {
                        Text(stringResource(R.string.wordguess_challenger_back_to_puzzles), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onTryAnotherGame) {
                        Text(stringResource(R.string.wordguess_challenger_try_another_game), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultMetaItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
    }
}
