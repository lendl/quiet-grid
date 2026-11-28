package com.quietgrid.app.core.tips

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.GamePlayPreferences
import com.quietgrid.app.data.GamePreferenceOverrides
import com.quietgrid.app.data.GameToggle
import com.quietgrid.app.data.PlayRecord
import com.quietgrid.app.data.ScreenRotation
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

enum class TipKind { SETTING, DISCOVERY }

sealed interface QuietTip {
    val key: String
    val kind: TipKind

    data class LockRotation(val gameId: GameId, val target: ScreenRotation) : QuietTip {
        override val key: String get() = rotationTipKey(gameId)
        override val kind: TipKind get() = TipKind.SETTING
    }

    data class KeepAwake(val gameId: GameId) : QuietTip {
        override val key: String get() = keepAwakeTipKey(gameId)
        override val kind: TipKind get() = TipKind.SETTING
    }

    data object ReduceMotion : QuietTip {
        override val key: String get() = REDUCE_MOTION_TIP_KEY
        override val kind: TipKind get() = TipKind.SETTING
    }

    data class MakeMix(val games: List<Pair<GameId, Difficulty>>) : QuietTip {
        override val key: String get() = MIX_TIP_KEY
        override val kind: TipKind get() = TipKind.DISCOVERY
    }

    data object TryDaily : QuietTip {
        override val key: String get() = DAILY_TIP_KEY
        override val kind: TipKind get() = TipKind.DISCOVERY
    }
}

fun rotationTipKey(gameId: GameId): String = "rotation:${gameId.key}"

fun keepAwakeTipKey(gameId: GameId): String = "keep_awake:${gameId.key}"

const val REDUCE_MOTION_TIP_KEY = "reduce_motion"
const val MIX_TIP_KEY = "mix"
const val DAILY_TIP_KEY = "daily"

data class QuietTipState(
    val lastShownDay: LocalDate? = null,
    val resultsSinceLast: Int = 0,
    val shownCounts: Map<String, Int> = emptyMap(),
    val closedKeys: Set<String> = emptySet(),
    val openedKeys: Set<String> = emptySet(),
    val snoozedUntil: Map<String, LocalDate> = emptyMap(),
    val rotationBounces: Map<GameId, Int> = emptyMap(),
    val rotationTargets: Map<GameId, ScreenRotation> = emptyMap(),
    val screenTimeouts: Map<GameId, Int> = emptyMap(),
    val earlyExitsInRow: Int = 0,
    val dailyTabOpened: Boolean = false,
)

data class TipInputs(
    val gameId: GameId,
    val solved: Boolean,
    val today: LocalDate,
    val history: List<PlayRecord>,
    val state: QuietTipState,
    val beQuiet: Boolean,
    val suggestionsEnabled: Boolean,
    val overrides: GamePreferenceOverrides,
    val effective: GamePlayPreferences,
    val appReduceMotion: Boolean,
    val hasAnyMix: Boolean,
    val gameKeepsScreenOn: Boolean = false,
    val zone: TimeZone = TimeZone.currentSystemDefault(),
)

const val MIN_RESULTS_BETWEEN_TIPS = 3
const val MAX_TIMES_SHOWN = 2
const val ROTATION_BOUNCES_NEEDED = 2
const val SCREEN_TIMEOUTS_NEEDED = 2
const val EARLY_EXITS_NEEDED = 5
const val MIX_GAMES_NEEDED = 4
const val MIX_WINDOW_DAYS = 7L
const val DAILY_PLAY_DAYS_NEEDED = 5
const val DAILY_WINDOW_DAYS = 14L
const val SNOOZE_DAYS = 7L

fun pickQuietTip(inputs: TipInputs): QuietTip? {
    if (inputs.beQuiet || !inputs.suggestionsEnabled) return null
    val state = inputs.state
    val lastShown = state.lastShownDay
    if (lastShown != null && lastShown >= inputs.today) return null
    if (state.resultsSinceLast < MIN_RESULTS_BETWEEN_TIPS) return null
    return candidates(inputs).firstOrNull { state.allows(it.key, inputs.today) }
}

private fun QuietTipState.allows(key: String, today: LocalDate): Boolean {
    if (key in closedKeys || key in openedKeys) return false
    if ((shownCounts[key] ?: 0) >= MAX_TIMES_SHOWN) return false
    val snoozed = snoozedUntil[key]
    return snoozed == null || today >= snoozed
}

private fun candidates(inputs: TipInputs): Sequence<QuietTip> = sequence {
    rotationTip(inputs)?.let { yield(it) }
    keepAwakeTip(inputs)?.let { yield(it) }
    reduceMotionTip(inputs)?.let { yield(it) }
    if (inputs.solved) {
        mixTip(inputs)?.let { yield(it) }
        dailyTip(inputs)?.let { yield(it) }
    }
}

private fun rotationTip(inputs: TipInputs): QuietTip? {
    val state = inputs.state
    if ((state.rotationBounces[inputs.gameId] ?: 0) < ROTATION_BOUNCES_NEEDED) return null
    if (inputs.overrides.rotation != null || inputs.effective.rotation != ScreenRotation.AUTO) return null
    val target = state.rotationTargets[inputs.gameId]?.takeIf { it != ScreenRotation.AUTO } ?: ScreenRotation.PORTRAIT
    return QuietTip.LockRotation(inputs.gameId, target)
}

private fun keepAwakeTip(inputs: TipInputs): QuietTip? {
    if (inputs.gameKeepsScreenOn) return null
    if ((inputs.state.screenTimeouts[inputs.gameId] ?: 0) < SCREEN_TIMEOUTS_NEEDED) return null
    if (GameToggle.KEEP_SCREEN_ON in inputs.overrides.toggles || inputs.effective.keepScreenOn) return null
    return QuietTip.KeepAwake(inputs.gameId)
}

private fun reduceMotionTip(inputs: TipInputs): QuietTip? {
    if (inputs.state.earlyExitsInRow < EARLY_EXITS_NEEDED) return null
    if (inputs.appReduceMotion || GameToggle.REDUCE_MOTION in inputs.overrides.toggles) return null
    return QuietTip.ReduceMotion
}

private fun mixTip(inputs: TipInputs): QuietTip? {
    if (inputs.hasAnyMix) return null
    val games = recentFavoriteGames(inputs.history, inputs.today, inputs.zone)
    if (games.size < MIX_GAMES_NEEDED) return null
    return QuietTip.MakeMix(games)
}

private fun dailyTip(inputs: TipInputs): QuietTip? {
    if (inputs.state.dailyTabOpened) return null
    if (inputs.history.any { it.dailyDate != null }) return null
    val firstDay = inputs.today.minus(DAILY_WINDOW_DAYS - 1, DateTimeUnit.DAY)
    val playDays = inputs.history
        .map { it.localDate(inputs.zone) }
        .filter { it >= firstDay && it <= inputs.today }
        .toSet()
    if (playDays.size < DAILY_PLAY_DAYS_NEEDED) return null
    return QuietTip.TryDaily
}

fun recentFavoriteGames(history: List<PlayRecord>, today: LocalDate, zone: TimeZone): List<Pair<GameId, Difficulty>> {
    val firstDay = today.minus(MIX_WINDOW_DAYS - 1, DateTimeUnit.DAY)
    val recent = history.filter { record ->
        val day = record.localDate(zone)
        record.solved && !record.isChallenger && record.dailyDate == null && record.journeyStop == null &&
            day >= firstDay && day <= today
    }
    return recent
        .mapNotNull { record -> gameIdOf(record.gameId)?.let { it to record } }
        .groupBy({ it.first }, { it.second })
        .entries
        .sortedWith(compareByDescending<Map.Entry<GameId, List<PlayRecord>>> { it.value.size }.thenByDescending { entry -> entry.value.maxOf { it.timestampMillis } })
        .take(MIX_GAMES_NEEDED)
        .mapNotNull { (gameId, records) -> usualDifficulty(records)?.let { gameId to it } }
}

private fun usualDifficulty(records: List<PlayRecord>): Difficulty? =
    records
        .mapNotNull { record -> difficultyOf(record.difficulty)?.let { it to record.timestampMillis } }
        .groupBy({ it.first }, { it.second })
        .maxWithOrNull(compareBy<Map.Entry<Difficulty, List<Long>>> { it.value.size }.thenBy { it.value.max() })
        ?.key

private fun gameIdOf(key: String): GameId? = GameId.entries.firstOrNull { it.key == key }

private fun difficultyOf(key: String): Difficulty? = Difficulty.entries.firstOrNull { it.key == key }

private fun PlayRecord.localDate(zone: TimeZone): LocalDate = Instant.fromEpochMilliseconds(timestampMillis).toLocalDateTime(zone).date
