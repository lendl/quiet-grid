package com.quietgrid.app.nav

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.journey.JourneyRequest
import com.quietgrid.app.core.journey.JourneySignals
import com.quietgrid.app.data.ActiveSessionSummary
import com.quietgrid.app.data.SESSION_MODE_ENDLESS
import com.quietgrid.app.games.blockfill.BlockFillEndlessResult
import com.quietgrid.app.games.minesweeper.MinesweeperEndlessResult
import java.time.LocalDate

object Routes {
    const val TABS = "tabs"
    const val PICKER = "picker/{gameId}"
    const val RULES = "rules/{gameId}"
    const val PLAY = "play/{gameId}/{difficulty}/{resume}?daily={daily}&journey={journey}&journeyPuzzle={journeyPuzzle}&transform={transform}"
    const val JOURNEY = "journey/{gameId}"
    const val JOURNEY_MAP = "journeyMap/{gameId}/{chapter}"
    const val CHALLENGER = "challenger/{gameId}"
    const val CHALLENGER_RESULT = "challengerResult/{gameId}/{puzzlesSolved}/{tier}/{score}/{isNewHighScore}/{reason}/{previousBest}/{fastestSolveSeconds}"
    const val COMPLETION = "completion/{gameId}/{difficulty}/{score}/{accuracyPct}/{elapsedSeconds}/{isFirstSolve}/{isNewHighScore}/{bestTile}?daily={daily}"
    const val LOSS = "loss/{gameId}/{difficulty}/{elapsedSeconds}/{reason}/{score}/{bestTile}?daily={daily}&journey={journey}"
    const val JOURNEY_RESULT = "journeyResult/{gameId}/{stopId}/{elapsedSeconds}/{hintsUsed}/{mistakes}"
    const val ANALYZER = "analyzer/{gameId}"
    const val SUPPORT_INFO = "supportInfo/{key}"
    const val SETTINGS = "settings"
    const val SUPPORT = "support"
    const val TRUST = "trust"
    const val ABOUT = "about"
    const val MIX_EDITOR = "mixEditor/{mixId}"
    const val ADD_TO_MIX = "addToMix/{gameId}/{difficulty}"
    const val ADD_TO_NEW_MIX = "addToNewMix/{gameId}/{difficulty}"
    const val ENDLESS = "endless/{gameId}/{resume}"
    const val ENDLESS_RESULT = "endlessResult/{score}/{level}/{lines}/{moves}/{isNewBest}/{previousBest}/{reason}"
    const val MINESWEEPER_ENDLESS_RESULT = "minesweeperEndlessResult/{score}/{ring}/{elapsedSeconds}/{isNewBest}/{previousBest}/{reason}"

    fun picker(gameId: GameId) = "picker/${gameId.key}"

    fun rules(gameId: GameId) = "rules/${gameId.key}"
    fun play(gameId: GameId, difficulty: Difficulty, resume: Boolean, daily: LocalDate? = null, journey: JourneyRequest? = null): String {
        val query = listOfNotNull(
            daily?.let { "daily=$it" },
            journey?.let { "journey=${it.stopId}&journeyPuzzle=${it.puzzleId}&transform=${it.transform}" },
        )
        return "play/${gameId.key}/${difficulty.key}/$resume" + if (query.isEmpty()) "" else query.joinToString("&", prefix = "?")
    }

    fun journey(gameId: GameId) = "journey/${gameId.key}"

    fun journeyMap(gameId: GameId, chapter: Int) = "journeyMap/${gameId.key}/$chapter"

    fun resume(summary: ActiveSessionSummary): String? {
        val gameId = GameId.entries.firstOrNull { it.key == summary.gameId } ?: return null
        if (summary.mode == SESSION_MODE_ENDLESS) return endless(gameId, resume = true)
        val daily = summary.dailyDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val journey = summary.journey ?: return play(gameId, Difficulty.EASY, resume = true, daily = daily)
        val difficulty = Difficulty.entries.firstOrNull { it.key == summary.difficulty } ?: Difficulty.EASY
        return play(gameId, difficulty, resume = true, daily = daily, journey = journey)
    }

    fun challenger(gameId: GameId) = "challenger/${gameId.key}"

    fun endless(gameId: GameId, resume: Boolean) = "endless/${gameId.key}/$resume"

    fun endlessResult(result: BlockFillEndlessResult) =
        "endlessResult/${result.score}/${result.levelReached}/${result.linesCleared}/${result.moves}/" +
            "${result.isNewBest}/${result.previousBest}/${result.reason}"

    fun minesweeperEndlessResult(result: MinesweeperEndlessResult) =
        "minesweeperEndlessResult/${result.score}/${result.ring}/${result.elapsedSeconds}/" +
            "${result.isNewBest}/${result.previousBest}/${result.reason}"

    fun challengerResult(
        gameId: GameId,
        puzzlesSolved: Int,
        tier: Difficulty,
        score: Int,
        isNewHighScore: Boolean,
        reason: String,
        previousBest: Int,
        fastestSolveSeconds: Double?,
    ) = "challengerResult/${gameId.key}/$puzzlesSolved/${tier.key}/$score/$isNewHighScore/$reason/$previousBest/${fastestSolveSeconds?.toFloat() ?: -1f}"

    fun completion(
        gameId: GameId,
        difficulty: Difficulty,
        score: Int,
        accuracyPct: Int,
        elapsedSeconds: Int,
        isFirstSolve: Boolean,
        isNewHighScore: Boolean,
        bestTile: Int,
        daily: String? = null,
    ) = "completion/${gameId.key}/${difficulty.key}/$score/$accuracyPct/$elapsedSeconds/$isFirstSolve/$isNewHighScore/$bestTile" +
        (daily?.let { "?daily=$it" } ?: "")

    fun loss(
        gameId: GameId,
        difficulty: Difficulty,
        elapsedSeconds: Int,
        reason: String,
        score: Int,
        bestTile: Int,
        daily: String? = null,
        journeyStop: String? = null,
    ): String {
        val query = listOfNotNull(daily?.let { "daily=$it" }, journeyStop?.let { "journey=$it" })
        return "loss/${gameId.key}/${difficulty.key}/$elapsedSeconds/$reason/$score/$bestTile" +
            if (query.isEmpty()) "" else query.joinToString("&", prefix = "?")
    }

    fun journeyResult(gameId: GameId, stopId: String, elapsedSeconds: Int, signals: JourneySignals) =
        "journeyResult/${gameId.key}/$stopId/$elapsedSeconds/${signals.hintsUsed}/${signals.mistakes}"

    fun analyzer(gameId: GameId) = "analyzer/${gameId.key}"

    fun supportInfo(key: String) = "supportInfo/$key"

    fun mixEditor(mixId: String) = "mixEditor/$mixId"

    fun addToMix(gameId: GameId, difficulty: Difficulty) = "addToMix/${gameId.key}/${difficulty.key}"

    fun addToNewMix(gameId: GameId, difficulty: Difficulty) = "addToNewMix/${gameId.key}/${difficulty.key}"
}
