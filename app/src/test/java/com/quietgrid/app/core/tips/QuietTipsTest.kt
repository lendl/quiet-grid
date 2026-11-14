package com.quietgrid.app.core.tips

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.GamePlayPreferences
import com.quietgrid.app.data.GamePreferenceOverrides
import com.quietgrid.app.data.GameToggle
import com.quietgrid.app.data.PlayRecord
import com.quietgrid.app.data.ScreenRotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class QuietTipsTest {

    private val today = LocalDate.of(2026, 11, 14)
    private val ready = QuietTipState(resultsSinceLast = MIN_RESULTS_BETWEEN_TIPS)

    private fun inputs(
        state: QuietTipState = ready,
        solved: Boolean = true,
        history: List<PlayRecord> = emptyList(),
        beQuiet: Boolean = false,
        suggestionsEnabled: Boolean = true,
        overrides: GamePreferenceOverrides = GamePreferenceOverrides(),
        effective: GamePlayPreferences = GamePlayPreferences(),
        appReduceMotion: Boolean = false,
        hasAnyMix: Boolean = false,
        gameKeepsScreenOn: Boolean = false,
    ) = TipInputs(
        gameId = GameId.SUDOKU,
        solved = solved,
        today = today,
        history = history,
        state = state,
        beQuiet = beQuiet,
        suggestionsEnabled = suggestionsEnabled,
        overrides = overrides,
        effective = effective,
        appReduceMotion = appReduceMotion,
        hasAnyMix = hasAnyMix,
        gameKeepsScreenOn = gameKeepsScreenOn,
        zone = ZoneOffset.UTC,
    )

    private fun record(
        gameId: GameId,
        daysAgo: Long,
        difficulty: Difficulty = Difficulty.EASY,
        solved: Boolean = true,
        isChallenger: Boolean = false,
        dailyDate: String? = null,
        journeyStop: String? = null,
        minute: Int = 0,
    ) = PlayRecord(
        gameId = gameId.key,
        difficulty = difficulty.key,
        solved = solved,
        score = 100,
        elapsedSeconds = 60,
        timestampMillis = today.minusDays(daysAgo).atTime(12, minute).toInstant(ZoneOffset.UTC).toEpochMilli(),
        isChallenger = isChallenger,
        dailyDate = dailyDate,
        journeyStop = journeyStop,
    )

    private val rotationState = ready.copy(
        rotationBounces = mapOf(GameId.SUDOKU to ROTATION_BOUNCES_NEEDED),
        rotationTargets = mapOf(GameId.SUDOKU to ScreenRotation.PORTRAIT),
    )
    private val keepAwakeState = ready.copy(screenTimeouts = mapOf(GameId.SUDOKU to SCREEN_TIMEOUTS_NEEDED))
    private val reduceMotionState = ready.copy(earlyExitsInRow = EARLY_EXITS_NEEDED)
    private val fourGames = listOf(GameId.SUDOKU, GameId.TAKUZU, GameId.NONOGRAM, GameId.HASHI).map { record(it, daysAgo = 1) }
    private val fivePlayDays = (0L until 5L).map { record(GameId.SUDOKU, daysAgo = it * 2) }

    @Test
    fun `rotation tip fires after two bounces with the orientation they returned to`() {
        val landscape = rotationState.copy(rotationTargets = mapOf(GameId.SUDOKU to ScreenRotation.LANDSCAPE))

        assertEquals(QuietTip.LockRotation(GameId.SUDOKU, ScreenRotation.PORTRAIT), pickQuietTip(inputs(rotationState)))
        assertEquals(QuietTip.LockRotation(GameId.SUDOKU, ScreenRotation.LANDSCAPE), pickQuietTip(inputs(landscape)))
    }

    @Test
    fun `rotation tip needs two bounces in this game`() {
        val oneBounce = rotationState.copy(rotationBounces = mapOf(GameId.SUDOKU to 1, GameId.TAKUZU to 5))

        assertNull(pickQuietTip(inputs(oneBounce)))
    }

    @Test
    fun `rotation tip can show after a loss`() {
        assertEquals(QuietTip.LockRotation(GameId.SUDOKU, ScreenRotation.PORTRAIT), pickQuietTip(inputs(rotationState, solved = false)))
    }

    @Test
    fun `rotation tip stays quiet when rotation is already set`() {
        val perGame = GamePreferenceOverrides(rotation = ScreenRotation.LANDSCAPE)
        val appWide = GamePlayPreferences(rotation = ScreenRotation.PORTRAIT)

        assertNull(pickQuietTip(inputs(rotationState, overrides = perGame, effective = GamePlayPreferences(rotation = ScreenRotation.LANDSCAPE))))
        assertNull(pickQuietTip(inputs(rotationState, effective = appWide)))
    }

    @Test
    fun `keep awake tip fires after two screen timeouts`() {
        assertEquals(QuietTip.KeepAwake(GameId.SUDOKU), pickQuietTip(inputs(keepAwakeState)))
    }

    @Test
    fun `keep awake tip stays quiet when the screen already stays on`() {
        val perGame = GamePreferenceOverrides(toggles = mapOf(GameToggle.KEEP_SCREEN_ON to false))

        assertNull(pickQuietTip(inputs(keepAwakeState, overrides = perGame)))
        assertNull(pickQuietTip(inputs(keepAwakeState, effective = GamePlayPreferences(keepScreenOn = true))))
        assertNull(pickQuietTip(inputs(keepAwakeState, gameKeepsScreenOn = true)))
        assertNull(pickQuietTip(inputs(keepAwakeState.copy(screenTimeouts = mapOf(GameId.SUDOKU to 1)))))
    }

    @Test
    fun `reduce motion tip fires after five early exits in a row`() {
        assertEquals(QuietTip.ReduceMotion, pickQuietTip(inputs(reduceMotionState)))
        assertEquals(QuietTip.ReduceMotion, pickQuietTip(inputs(reduceMotionState, solved = false)))
        assertNull(pickQuietTip(inputs(reduceMotionState.copy(earlyExitsInRow = EARLY_EXITS_NEEDED - 1))))
    }

    @Test
    fun `reduce motion tip stays quiet when reduce motion is already set`() {
        val perGame = GamePreferenceOverrides(toggles = mapOf(GameToggle.REDUCE_MOTION to false))

        assertNull(pickQuietTip(inputs(reduceMotionState, appReduceMotion = true)))
        assertNull(pickQuietTip(inputs(reduceMotionState, overrides = perGame)))
    }

    @Test
    fun `mix tip fires after four different games solved this week`() {
        val tip = pickQuietTip(inputs(history = fourGames))

        assertEquals(QuietTip.MakeMix::class, tip!!::class)
        assertEquals(setOf(GameId.SUDOKU, GameId.TAKUZU, GameId.NONOGRAM, GameId.HASHI), (tip as QuietTip.MakeMix).games.map { it.first }.toSet())
    }

    @Test
    fun `mix tip takes the four most solved games at their usual difficulty`() {
        val history = listOf(
            record(GameId.SUDOKU, 1, Difficulty.HARD),
            record(GameId.SUDOKU, 2, Difficulty.HARD),
            record(GameId.SUDOKU, 3, Difficulty.EASY),
            record(GameId.TAKUZU, 1, Difficulty.MEDIUM),
            record(GameId.TAKUZU, 2, Difficulty.MEDIUM),
            record(GameId.NONOGRAM, 1, Difficulty.EXPERT),
            record(GameId.NONOGRAM, 2, Difficulty.EXPERT),
            record(GameId.HASHI, 1, Difficulty.EASY),
            record(GameId.HASHI, 2, Difficulty.EASY),
            record(GameId.MINESWEEPER, 4, Difficulty.EASY),
        )

        val tip = pickQuietTip(inputs(history = history)) as QuietTip.MakeMix

        assertEquals(
            listOf(
                GameId.SUDOKU to Difficulty.HARD,
                GameId.HASHI to Difficulty.EASY,
                GameId.NONOGRAM to Difficulty.EXPERT,
                GameId.TAKUZU to Difficulty.MEDIUM,
            ).toSet(),
            tip.games.toSet(),
        )
        assertEquals(GameId.SUDOKU to Difficulty.HARD, tip.games.first())
        assertEquals(4, tip.games.size)
    }

    @Test
    fun `mix tip ignores challenger, daily, journey, unsolved and old records`() {
        val history = listOf(
            record(GameId.SUDOKU, 1),
            record(GameId.TAKUZU, 1),
            record(GameId.NONOGRAM, 1),
            record(GameId.HASHI, 1, isChallenger = true),
            record(GameId.HASHI, 1, dailyDate = today.toString()),
            record(GameId.HASHI, 1, journeyStop = "c1s1"),
            record(GameId.HASHI, 1, solved = false),
            record(GameId.HASHI, MIX_WINDOW_DAYS),
        )

        assertNull(pickQuietTip(inputs(history = history)))
    }

    @Test
    fun `mix tip stays quiet when a mix exists or after a loss`() {
        assertNull(pickQuietTip(inputs(history = fourGames, hasAnyMix = true)))
        assertNull(pickQuietTip(inputs(history = fourGames, solved = false)))
    }

    @Test
    fun `daily tip fires after play on five days in two weeks`() {
        assertEquals(QuietTip.TryDaily, pickQuietTip(inputs(history = fivePlayDays)))
    }

    @Test
    fun `daily tip needs five different days inside the window`() {
        val sameDay = (0 until 10).map { record(GameId.SUDOKU, daysAgo = 0, minute = it) }
        val tooOld = (0L until 4L).map { record(GameId.SUDOKU, daysAgo = it) } + record(GameId.SUDOKU, DAILY_WINDOW_DAYS)

        assertNull(pickQuietTip(inputs(history = sameDay)))
        assertNull(pickQuietTip(inputs(history = tooOld)))
    }

    @Test
    fun `daily tip stays quiet once the daily is known`() {
        val playedDaily = fivePlayDays + record(GameId.TAKUZU, 20, dailyDate = "2026-10-25")

        assertNull(pickQuietTip(inputs(history = playedDaily)))
        assertNull(pickQuietTip(inputs(ready.copy(dailyTabOpened = true), history = fivePlayDays)))
        assertNull(pickQuietTip(inputs(history = fivePlayDays, solved = false)))
    }

    @Test
    fun `be quiet and suggestions off silence every tip`() {
        assertNull(pickQuietTip(inputs(rotationState, beQuiet = true)))
        assertNull(pickQuietTip(inputs(rotationState, suggestionsEnabled = false)))
    }

    @Test
    fun `at most one tip a day`() {
        assertNull(pickQuietTip(inputs(rotationState.copy(lastShownDay = today))))
        assertEquals(QuietTip.LockRotation(GameId.SUDOKU, ScreenRotation.PORTRAIT), pickQuietTip(inputs(rotationState.copy(lastShownDay = today.minusDays(1)))))
    }

    @Test
    fun `three finished puzzles between tips`() {
        assertNull(pickQuietTip(inputs(rotationState.copy(resultsSinceLast = MIN_RESULTS_BETWEEN_TIPS - 1))))
    }

    @Test
    fun `a tip shows at most twice`() {
        val key = rotationTipKey(GameId.SUDOKU)

        assertNull(pickQuietTip(inputs(rotationState.copy(shownCounts = mapOf(key to MAX_TIMES_SHOWN)))))
        assertEquals(QuietTip.LockRotation(GameId.SUDOKU, ScreenRotation.PORTRAIT), pickQuietTip(inputs(rotationState.copy(shownCounts = mapOf(key to 1)))))
    }

    @Test
    fun `an opened tip does not come back`() {
        assertNull(pickQuietTip(inputs(rotationState.copy(openedKeys = setOf(rotationTipKey(GameId.SUDOKU))))))
    }

    @Test
    fun `a closed tip never comes back`() {
        assertNull(pickQuietTip(inputs(rotationState.copy(closedKeys = setOf(rotationTipKey(GameId.SUDOKU))))))
    }

    @Test
    fun `a snoozed tip waits until its date`() {
        val key = rotationTipKey(GameId.SUDOKU)

        assertNull(pickQuietTip(inputs(rotationState.copy(snoozedUntil = mapOf(key to today.plusDays(1))))))
        assertEquals(
            QuietTip.LockRotation(GameId.SUDOKU, ScreenRotation.PORTRAIT),
            pickQuietTip(inputs(rotationState.copy(snoozedUntil = mapOf(key to today)))),
        )
    }

    @Test
    fun `setting tips win over discovery tips in a fixed order`() {
        val everything = ready.copy(
            rotationBounces = rotationState.rotationBounces,
            rotationTargets = rotationState.rotationTargets,
            screenTimeouts = keepAwakeState.screenTimeouts,
            earlyExitsInRow = EARLY_EXITS_NEEDED,
        )
        val history = fourGames + fivePlayDays

        assertEquals(QuietTip.LockRotation(GameId.SUDOKU, ScreenRotation.PORTRAIT), pickQuietTip(inputs(everything, history = history)))

        val noRotation = everything.copy(closedKeys = setOf(rotationTipKey(GameId.SUDOKU)))
        assertEquals(QuietTip.KeepAwake(GameId.SUDOKU), pickQuietTip(inputs(noRotation, history = history)))

        val noKeepAwake = noRotation.copy(closedKeys = noRotation.closedKeys + keepAwakeTipKey(GameId.SUDOKU))
        assertEquals(QuietTip.ReduceMotion, pickQuietTip(inputs(noKeepAwake, history = history)))

        val noReduceMotion = noKeepAwake.copy(closedKeys = noKeepAwake.closedKeys + REDUCE_MOTION_TIP_KEY)
        assertEquals(QuietTip.MakeMix::class, pickQuietTip(inputs(noReduceMotion, history = history))!!::class)

        val noMix = noReduceMotion.copy(closedKeys = noReduceMotion.closedKeys + MIX_TIP_KEY)
        assertEquals(QuietTip.TryDaily, pickQuietTip(inputs(noMix, history = history)))
    }
}
