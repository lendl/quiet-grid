package com.quietgrid.app.nav

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.quietgrid.app.core.GameId
import com.quietgrid.app.games.GameRegistry
import com.quietgrid.app.games.blockfill.BlockFillEndlessPlayScreen
import com.quietgrid.app.games.blockfill.BlockFillEndlessResult
import com.quietgrid.app.games.blockfill.BlockFillEndlessResultScreen
import com.quietgrid.app.games.minesweeper.MinesweeperEndlessPlayScreen
import com.quietgrid.app.games.minesweeper.MinesweeperEndlessResult
import com.quietgrid.app.games.minesweeper.MinesweeperEndlessResultScreen
import com.quietgrid.app.ui.components.ChallengerResultScreen
import com.quietgrid.app.ui.play.ProvideGamePlayPreferences
import com.quietgrid.app.ui.screens.ChallengerExtras
import com.quietgrid.app.ui.screens.ChallengerRunDetails

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.challengerGraph(navigator: AppNavigator, sharedScope: SharedTransitionScope) {
    composable<ChallengerRoute> { entry ->
        val gameId = entry.toRoute<ChallengerRoute>().gameId
        val playPreferences = navigator.playPreferencesFor(gameId)
        if (playPreferences.keepScreenOn) Box(Modifier.keepScreenOn())
        ProvideGamePlayPreferences(playPreferences) {
            GameRegistry.of(gameId).challenger?.playScreen?.invoke { result ->
                ChallengerExtras.set(ChallengerRunDetails(result.puzzleHistory, result.solvesInTier))
                navigator.navigateFromTabs(
                    ChallengerResultRoute(
                        gameId = gameId,
                        puzzlesSolved = result.puzzlesSolved,
                        tier = result.tierReached,
                        score = result.score,
                        isNewHighScore = result.isNewHighScore,
                        reason = result.reason,
                        previousBest = result.previousBest,
                        fastestSolveSeconds = result.fastestSolveSeconds,
                    ),
                )
            }
        }
    }

    composable<ChallengerResultRoute> { entry ->
        val route = entry.toRoute<ChallengerResultRoute>()
        CompositionLocalProvider(
            LocalSharedTransitionScope provides sharedScope,
            LocalAnimatedVisibilityScope provides this,
        ) {
            ProvideGamePlayPreferences(navigator.playPreferencesFor(route.gameId)) {
                GameRegistry.of(route.gameId).challenger?.let { spec ->
                    ChallengerResultScreen(
                        gameId = route.gameId,
                        spec = spec,
                        puzzlesSolved = route.puzzlesSolved,
                        tierReached = route.tier,
                        score = route.score,
                        isNewHighScore = route.isNewHighScore,
                        reason = route.reason,
                        previousBest = route.previousBest,
                        fastestSolveSeconds = route.fastestSolveSeconds,
                        isMixActive = navigator.activeMix != null,
                        onPlayAgain = { navigator.playAgainOrDrawMix { navigator.navigateFromTabs(ChallengerRoute(route.gameId)) } },
                        onBackToPuzzles = { navigator.navigateFromTabs(PickerRoute(route.gameId)) },
                        onTryAnotherGame = { navigator.endMixAndGoToGames() },
                    )
                }
            }
        }
    }
}

fun NavGraphBuilder.endlessGraph(navigator: AppNavigator) {
    composable<EndlessRoute> { entry ->
        val route = entry.toRoute<EndlessRoute>()
        val gameId = if (route.gameId == GameId.MINESWEEPER) GameId.MINESWEEPER else GameId.BLOCKFILL
        val playPreferences = navigator.playPreferencesFor(gameId)
        if (playPreferences.keepScreenOn) Box(Modifier.keepScreenOn())
        ProvideGamePlayPreferences(playPreferences) {
            if (gameId == GameId.MINESWEEPER) {
                MinesweeperEndlessPlayScreen(
                    resume = route.resume,
                    onBack = { navigator.back() },
                    onFinished = { result ->
                        navigator.navigateFromTabs(
                            MinesweeperEndlessResultRoute(result.score, result.ring, result.elapsedSeconds, result.isNewBest, result.previousBest, result.reason),
                        )
                    },
                )
            } else {
                BlockFillEndlessPlayScreen(
                    resume = route.resume,
                    onBack = { navigator.back() },
                    onFinished = { result ->
                        navigator.navigateFromTabs(
                            BlockFillEndlessResultRoute(
                                result.score,
                                result.levelReached,
                                result.linesCleared,
                                result.moves,
                                result.isNewBest,
                                result.previousBest,
                                result.reason,
                            ),
                        )
                    },
                )
            }
        }
    }

    composable<MinesweeperEndlessResultRoute> { entry ->
        val route = entry.toRoute<MinesweeperEndlessResultRoute>()
        ProvideGamePlayPreferences(navigator.playPreferencesFor(GameId.MINESWEEPER)) {
            MinesweeperEndlessResultScreen(
                result = MinesweeperEndlessResult(route.score, route.ring, route.elapsedSeconds, route.isNewBest, route.previousBest, route.reason),
                onPlayAgain = { navigator.navigateFromTabs(EndlessRoute(GameId.MINESWEEPER, resume = false)) },
                onBack = { navigator.navigateFromTabs(PickerRoute(GameId.MINESWEEPER)) },
                onTryAnotherGame = { navigator.endMixAndGoToGames() },
            )
        }
    }

    composable<BlockFillEndlessResultRoute> { entry ->
        val route = entry.toRoute<BlockFillEndlessResultRoute>()
        ProvideGamePlayPreferences(navigator.playPreferencesFor(GameId.BLOCKFILL)) {
            BlockFillEndlessResultScreen(
                result = BlockFillEndlessResult(
                    score = route.score,
                    levelReached = route.levelReached,
                    linesCleared = route.linesCleared,
                    moves = route.moves,
                    isNewBest = route.isNewBest,
                    previousBest = route.previousBest,
                    reason = route.reason,
                ),
                onPlayAgain = { navigator.navigateFromTabs(EndlessRoute(GameId.BLOCKFILL, resume = false)) },
                onBack = { navigator.navigateFromTabs(PickerRoute(GameId.BLOCKFILL)) },
                onTryAnotherGame = { navigator.endMixAndGoToGames() },
            )
        }
    }
}
