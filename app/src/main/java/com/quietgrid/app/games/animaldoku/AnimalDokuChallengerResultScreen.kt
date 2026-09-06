package com.quietgrid.app.games.animaldoku

import androidx.compose.runtime.Composable
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.ChallengerResultScreen
import com.quietgrid.app.ui.components.ChallengerResultStrings

@Composable
fun AnimalDokuChallengerResultScreen(
    puzzlesSolved: Int,
    tierReached: Difficulty,
    score: Int,
    isNewHighScore: Boolean,
    reason: String,
    previousBest: Int,
    fastestSolveSeconds: Double?,
    onPlayAgain: () -> Unit,
    onBackToPuzzles: () -> Unit,
    onTryAnotherGame: () -> Unit,
) {
    val titleRes: Int
    val bodyRes: Int
    when (reason) {
        "lives_exhausted" -> {
            titleRes = R.string.animaldoku_challenger_result_lives_exhausted_title
            bodyRes = R.string.animaldoku_challenger_result_lives_exhausted_body
        }
        "abandoned" -> {
            titleRes = R.string.animaldoku_challenger_result_abandoned_title
            bodyRes = R.string.animaldoku_challenger_result_abandoned_body
        }
        else -> {
            titleRes = R.string.animaldoku_challenger_result_time_up_title
            bodyRes = R.string.animaldoku_challenger_result_time_up_body
        }
    }

    ChallengerResultScreen(
        strings = ChallengerResultStrings(
            titleRes = titleRes,
            bodyRes = bodyRes,
            puzzlesSolvedLabelRes = R.string.animaldoku_challenger_result_puzzles_solved,
            scoreLabelRes = R.string.animaldoku_challenger_result_score,
            fastestSolveLabelRes = R.string.animaldoku_challenger_result_fastest_solve,
            newHighScoreBadgeRes = R.string.animaldoku_challenger_result_new_high_score,
            offBestRes = R.string.animaldoku_challenger_result_off_best,
            playAgainRes = R.string.animaldoku_challenger_play_again,
            backToPuzzlesRes = R.string.animaldoku_challenger_back_to_puzzles,
            tryAnotherGameRes = R.string.animaldoku_challenger_try_another_game,
        ),
        difficultyLabelRes = ::animalDokuDifficultyLabelRes,
        puzzlesSolved = puzzlesSolved,
        tierReached = tierReached,
        score = score,
        isNewHighScore = isNewHighScore,
        previousBest = previousBest,
        fastestSolveSeconds = fastestSolveSeconds,
        reason = reason,
        startingLives = ANIMALDOKU_STARTING_LIVES,
        solvesPerTier = ANIMALDOKU_CHALLENGER_SOLVES_PER_TIER,
        onPlayAgain = onPlayAgain,
        onBackToPuzzles = onBackToPuzzles,
        onTryAnotherGame = onTryAnotherGame,
    )
}
