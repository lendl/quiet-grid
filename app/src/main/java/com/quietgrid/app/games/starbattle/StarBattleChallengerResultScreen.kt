package com.quietgrid.app.games.starbattle

import androidx.compose.runtime.Composable
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.ChallengerResultScreen
import com.quietgrid.app.ui.components.ChallengerResultStrings

@Composable
fun StarBattleChallengerResultScreen(
    puzzlesSolved: Int,
    tierReached: Difficulty,
    score: Int,
    isNewHighScore: Boolean,
    reason: String,
    previousBest: Int,
    fastestSolveSeconds: Double?,
    isMixActive: Boolean,
    onPlayAgain: () -> Unit,
    onBackToPuzzles: () -> Unit,
    onTryAnotherGame: () -> Unit,
) {
    val titleRes: Int
    val bodyRes: Int
    when (reason) {
        "hearts_exhausted" -> {
            titleRes = R.string.starbattle_challenger_result_hearts_exhausted_title
            bodyRes = R.string.starbattle_challenger_result_hearts_exhausted_body
        }
        "abandoned" -> {
            titleRes = R.string.starbattle_challenger_result_abandoned_title
            bodyRes = R.string.starbattle_challenger_result_abandoned_body
        }
        else -> {
            titleRes = R.string.starbattle_challenger_result_time_up_title
            bodyRes = R.string.starbattle_challenger_result_time_up_body
        }
    }

    ChallengerResultScreen(
        strings = ChallengerResultStrings(
            titleRes = titleRes,
            bodyRes = bodyRes,
            puzzlesSolvedLabelRes = R.string.starbattle_challenger_result_puzzles_solved,
            scoreLabelRes = R.string.starbattle_challenger_result_score,
            fastestSolveLabelRes = R.string.starbattle_challenger_result_fastest_solve,
            newHighScoreBadgeRes = R.string.starbattle_challenger_result_new_high_score,
            offBestRes = R.string.starbattle_challenger_result_off_best,
            playAgainRes = R.string.starbattle_challenger_play_again,
            backToPuzzlesRes = R.string.starbattle_challenger_back_to_puzzles,
            tryAnotherGameRes = R.string.starbattle_challenger_try_another_game,
        ),
        difficultyLabelRes = ::starBattleDifficultyLabelRes,
        puzzlesSolved = puzzlesSolved,
        tierReached = tierReached,
        score = score,
        isNewHighScore = isNewHighScore,
        previousBest = previousBest,
        fastestSolveSeconds = fastestSolveSeconds,
        reason = reason,
        startingLives = STARBATTLE_STARTING_LIVES,
        solvesPerTier = STARBATTLE_CHALLENGER_SOLVES_PER_TIER,
        isMixActive = isMixActive,
        onPlayAgain = onPlayAgain,
        onBackToPuzzles = onBackToPuzzles,
        onTryAnotherGame = onTryAnotherGame,
    )
}
