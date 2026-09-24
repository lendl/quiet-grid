package com.quietgrid.app.core.daily

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.ActiveSessionEnvelope
import com.quietgrid.app.data.PlayRecord
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

fun nextReminderDelay(now: ZonedDateTime, time: LocalTime): Duration {
    val todayAt = now.toLocalDate().atTime(time).atZone(now.zone)
    val target = if (todayAt.isAfter(now)) todayAt else now.toLocalDate().plusDays(1).atTime(time).atZone(now.zone)
    return Duration.between(now, target)
}

fun gamesNeedingReminder(
    subscribed: Set<GameId>,
    eligible: List<GameId>,
    poolSizes: Map<GameId, Map<Difficulty, Int>>,
    records: List<PlayRecord>,
    envelope: ActiveSessionEnvelope?,
    today: LocalDate,
): List<GameId> = buildDailyGames(subscribed, eligible, poolSizes, records, envelope, today)
    .filter { game -> game.tiers.any { it.status == DailyTierStatus.Unplayed || it.status == DailyTierStatus.InProgress } }
    .map { it.gameId }
