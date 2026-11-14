package com.quietgrid.app.nav

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.journey.JourneyRequest
import com.quietgrid.app.data.ActiveSessionSummary
import com.quietgrid.app.data.SESSION_MODE_ENDLESS
import com.quietgrid.app.session.LossReason
import java.time.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data object TabsRoute

@Serializable
data class PickerRoute(val gameId: GameId)

@Serializable
data class RulesRoute(val gameId: GameId)

@Serializable
data class PlayRoute(
    val gameId: GameId,
    val difficulty: Difficulty,
    val resume: Boolean,
    val daily: String? = null,
    val journeyStop: String? = null,
    val journeyPuzzle: String? = null,
    val transform: Int = 0,
) {
    val dailyDate: LocalDate?
        get() = daily?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    val journey: JourneyRequest?
        get() = journeyStop?.let { JourneyRequest(it, journeyPuzzle ?: it, transform) }

    companion object {
        fun of(gameId: GameId, difficulty: Difficulty, resume: Boolean, daily: LocalDate? = null, journey: JourneyRequest? = null) = PlayRoute(
            gameId = gameId,
            difficulty = difficulty,
            resume = resume,
            daily = daily?.toString(),
            journeyStop = journey?.stopId,
            journeyPuzzle = journey?.puzzleId,
            transform = journey?.transform ?: 0,
        )
    }
}

@Serializable
data class JourneyRoute(val gameId: GameId)

@Serializable
data class JourneyMapRoute(val gameId: GameId, val chapter: Int)

@Serializable
data class JourneyResultRoute(val gameId: GameId, val stopId: String, val elapsedSeconds: Int, val hintsUsed: Int, val mistakes: Int)

@Serializable
data class ChallengerRoute(val gameId: GameId)

@Serializable
data class ChallengerResultRoute(
    val gameId: GameId,
    val puzzlesSolved: Int,
    val tier: Difficulty,
    val score: Int,
    val isNewHighScore: Boolean,
    val reason: String,
    val previousBest: Int,
    val fastestSolveSeconds: Double?,
)

@Serializable
data class CompletionRoute(
    val gameId: GameId,
    val difficulty: Difficulty,
    val score: Int,
    val accuracyPct: Int,
    val elapsedSeconds: Int,
    val isFirstSolve: Boolean,
    val isNewHighScore: Boolean,
    val daily: String? = null,
)

@Serializable
data class LossRoute(
    val gameId: GameId,
    val difficulty: Difficulty,
    val elapsedSeconds: Int,
    val reason: LossReason,
    val score: Int,
    val daily: String? = null,
    val journeyStop: String? = null,
)

@Serializable
data class AnalyzerRoute(val gameId: GameId)

@Serializable
data class EndlessRoute(val gameId: GameId, val resume: Boolean)

@Serializable
data class BlockFillEndlessResultRoute(
    val score: Int,
    val levelReached: Int,
    val linesCleared: Int,
    val moves: Int,
    val isNewBest: Boolean,
    val previousBest: Int,
    val reason: String,
)

@Serializable
data class MinesweeperEndlessResultRoute(
    val score: Int,
    val ring: Int,
    val elapsedSeconds: Int,
    val isNewBest: Boolean,
    val previousBest: Int,
    val reason: String,
)

@Serializable
data class MixEditorRoute(val mixId: String)

@Serializable
data class AddToMixRoute(val gameId: GameId, val difficulty: Difficulty)

@Serializable
data class AddToNewMixRoute(val gameId: GameId, val difficulty: Difficulty)

@Serializable
data class SupportInfoRoute(val key: String)

@Serializable
data object SettingsRoute

@Serializable
data object MilestonesRoute

@Serializable
data object SupportRoute

@Serializable
data object TrustRoute

@Serializable
data object AboutRoute

fun resumeRoute(summary: ActiveSessionSummary): Any? {
    val gameId = GameId.entries.firstOrNull { it.key == summary.gameId } ?: return null
    if (summary.mode == SESSION_MODE_ENDLESS) return EndlessRoute(gameId, resume = true)
    val daily = summary.dailyDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val journey = summary.journey ?: return PlayRoute.of(gameId, Difficulty.EASY, resume = true, daily = daily)
    val difficulty = Difficulty.entries.firstOrNull { it.key == summary.difficulty } ?: Difficulty.EASY
    return PlayRoute.of(gameId, difficulty, resume = true, daily = daily, journey = journey)
}
