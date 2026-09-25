package com.quietgrid.app.core.daily

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.ActiveSessionEnvelope
import com.quietgrid.app.data.PlayRecord
import java.time.LocalDate

sealed interface DailyTierStatus {
    data object Unplayed : DailyTierStatus
    data object InProgress : DailyTierStatus
    data class Solved(val elapsedSeconds: Int) : DailyTierStatus
    data object Lost : DailyTierStatus
}

data class DailyTierUi(val difficulty: Difficulty, val status: DailyTierStatus)

data class DailyGameUi(val gameId: GameId, val streak: Int, val tiers: List<DailyTierUi>)

fun dailyTierStatus(
    records: List<PlayRecord>,
    envelope: ActiveSessionEnvelope?,
    gameId: GameId,
    difficulty: Difficulty,
    date: LocalDate,
): DailyTierStatus {
    val dateKey = date.toString()
    val record = records
        .filter { it.gameId == gameId.key && it.dailyDate == dateKey && it.difficulty == difficulty.key }
        .maxByOrNull { it.timestampMillis }
    if (record != null) return if (record.solved) DailyTierStatus.Solved(record.elapsedSeconds) else DailyTierStatus.Lost
    val inProgress = envelope != null &&
        envelope.gameId == gameId.key &&
        envelope.dailyDate == dateKey &&
        envelope.dailyTier == difficulty.key
    return if (inProgress) DailyTierStatus.InProgress else DailyTierStatus.Unplayed
}

fun dailyStreak(records: List<PlayRecord>, gameIds: Set<GameId>, today: LocalDate): Int {
    val keys = gameIds.mapTo(HashSet()) { it.key }
    val solvedDates = records
        .filter { it.solved && it.gameId in keys }
        .mapNotNull { record -> record.dailyDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } }
        .toSet()
    var day = if (today in solvedDates) today else today.minusDays(1)
    var count = 0
    while (day in solvedDates) {
        count++
        day = day.minusDays(1)
    }
    return count
}

fun buildDailyGames(
    subscribed: Map<GameId, Set<Difficulty>>,
    eligible: List<GameId>,
    poolSizes: Map<GameId, Map<Difficulty, Int>>,
    records: List<PlayRecord>,
    envelope: ActiveSessionEnvelope?,
    today: LocalDate,
): List<DailyGameUi> = GameCatalog.games
    .map { it.id }
    .filter { it in subscribed && it in eligible }
    .mapNotNull { gameId ->
        val subscribedTiers = subscribed[gameId].orEmpty()
        val tiers = availableDailyTiers(poolSizes[gameId].orEmpty()).filter { it in subscribedTiers }
        if (tiers.isEmpty()) return@mapNotNull null
        DailyGameUi(
            gameId = gameId,
            streak = dailyStreak(records, setOf(gameId), today),
            tiers = tiers.map { DailyTierUi(it, dailyTierStatus(records, envelope, gameId, it, today)) },
        )
    }
