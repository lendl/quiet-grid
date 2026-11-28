package com.quietgrid.app.core.daily

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.ActiveSessionEnvelope
import com.quietgrid.app.data.PlayRecord
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Instant

fun nextReminderDelay(now: Instant, zone: TimeZone, time: LocalTime): Duration {
    val todayDate = now.toLocalDateTime(zone).date
    val todayAt = LocalDateTime(todayDate, time).toInstant(zone)
    val target = if (todayAt > now) todayAt else LocalDateTime(todayDate.plus(1, DateTimeUnit.DAY), time).toInstant(zone)
    return target - now
}

fun gamesNeedingReminder(
    subscribed: Map<GameId, Set<Difficulty>>,
    eligible: List<GameId>,
    poolSizes: Map<GameId, Map<Difficulty, Int>>,
    records: List<PlayRecord>,
    envelope: ActiveSessionEnvelope?,
    today: LocalDate,
): List<GameId> = buildDailyGames(subscribed, eligible, poolSizes, records, envelope, today)
    .filter { game -> game.tiers.any { it.status == DailyTierStatus.Unplayed || it.status == DailyTierStatus.InProgress } }
    .map { it.gameId }
