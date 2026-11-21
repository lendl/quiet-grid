package com.quietgrid.app.storeart

import android.app.Application
import android.content.pm.ActivityInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.quietgrid.app.LaunchRequest
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.badges.BadgeFacts
import com.quietgrid.app.core.badges.FinishFacts
import com.quietgrid.app.core.journey.JourneySignals
import com.quietgrid.app.core.journey.loadJourneyFile
import com.quietgrid.app.data.ActiveSessionEnvelope
import com.quietgrid.app.data.BadgeRepository
import com.quietgrid.app.data.DailyRepository
import com.quietgrid.app.data.JourneyProgressStore
import com.quietgrid.app.data.PlayHistoryRepository
import com.quietgrid.app.data.PlayRecord
import com.quietgrid.app.data.SessionRepository
import com.quietgrid.app.data.SettingsRepository
import com.quietgrid.app.data.journeyTrack
import com.quietgrid.app.games.starbattle.STARBATTLE_STARTING_LIVES
import com.quietgrid.app.games.starbattle.StarBattleCellState
import com.quietgrid.app.games.starbattle.StarBattlePersistedSession
import com.quietgrid.app.games.starbattle.StarBattlePuzzleBank
import com.quietgrid.app.games.starbattle.StarBattleStatus
import com.quietgrid.app.nav.AppNavHost
import com.quietgrid.app.ui.components.LocalHapticsEnabled
import com.quietgrid.app.ui.theme.QuietGridTheme
import com.quietgrid.app.ui.theme.ResolvedTheme
import com.quietgrid.engine.starbattle.StarBattlePuzzleEntry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@HiltAndroidTest
@Config(application = HiltTestApplication::class)
class TabletStoreArtCaptureTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createEmptyComposeRule()

    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var dailyRepository: DailyRepository

    @Inject lateinit var historyRepository: PlayHistoryRepository

    @Inject lateinit var sessionRepository: SessionRepository

    @Inject lateinit var journeyStore: JourneyProgressStore

    @Inject lateinit var badgeRepository: BadgeRepository

    private val app get() = ApplicationProvider.getApplicationContext<Application>()

    private enum class Tablet(val folder: String, val qualifiers: String) {
        SEVEN_INCH("tablet-7", "w600dp-h960dp-large-port-xhdpi"),
        TEN_INCH("tablet-10", "w1280dp-h800dp-xlarge-land-xhdpi"),
    }

    private class Shot(
        val name: String,
        val theme: ResolvedTheme,
        val launch: LaunchRequest? = null,
        val waitForText: String? = null,
        val prepare: suspend TabletStoreArtCaptureTest.() -> Unit = {},
        val steps: TabletStoreArtCaptureTest.() -> Unit = {},
    )

    private val shots = listOf(
        Shot("01-daily", ResolvedTheme.DARK, launch = LaunchRequest.DailyTab, waitForText = "Sudoku"),
        Shot("02-games", ResolvedTheme.DARK, waitForText = "Continue"),
        Shot("03-sudoku-play", ResolvedTheme.DARK, launch = LaunchRequest.StartGame(GameId.SUDOKU, Difficulty.MEDIUM), waitForText = "Undo") {
            selectEmptySudokuCell()
        },
        Shot("04-game-page", ResolvedTheme.DARK, waitForText = "Sudoku") { clickText("Sudoku") },
        Shot("05-wordsearch", ResolvedTheme.LIGHT, launch = LaunchRequest.StartGame(GameId.WORDSEARCH, Difficulty.EASY)) {
            settleUntil { letterNodes().size > 9 }
            findWordSearchWords(WORDS_TO_FIND)
        },
        Shot("06-starbattle", ResolvedTheme.SILK, launch = LaunchRequest.Resume, prepare = { seedStarBattleSession() }),
        Shot("07-stats", ResolvedTheme.DARK, waitForText = "Stats") { clickText("Stats") },
    )

    @Before
    fun setUp() {
        assumeTrue(System.getProperty("roborazzi.test.record") == "true")
        hiltRule.inject()
        shadowOf(app.packageManager).addOrUpdateActivity(
            ActivityInfo().apply {
                name = StoreArtHostActivity::class.java.name
                packageName = app.packageName
            },
        )
        runWhilePumpingMainLooper { seed() }
    }

    private fun runWhilePumpingMainLooper(block: suspend () -> Unit) {
        val work = CoroutineScope(Dispatchers.Default).async { block() }
        while (!work.isCompleted) {
            ShadowLooper.idleMainLooper()
            Thread.sleep(REAL_STEP_MS)
        }
        runBlocking { work.await() }
    }

    @Test
    fun captureTabletStoreScreens() {
        Tablet.entries.forEach { tablet ->
            shots.forEach { shot -> capture(tablet, shot) }
        }
    }

    private fun capture(tablet: Tablet, shot: Shot) {
        runWhilePumpingMainLooper {
            sessionRepository.clear()
            shot.prepare(this@TabletStoreArtCaptureTest)
        }
        RuntimeEnvironment.setQualifiers(tablet.qualifiers)
        val launchRequest = mutableStateOf<LaunchRequest?>(null)
        val scenario = ActivityScenario.launch(StoreArtHostActivity::class.java)
        scenario.onActivity { activity ->
            activity.setContent {
                QuietGridTheme(resolvedTheme = shot.theme) {
                    CompositionLocalProvider(LocalHapticsEnabled provides false) {
                        Box(Modifier.fillMaxSize()) {
                            AppNavHost(
                                launchRequest = launchRequest.value,
                                onLaunchRequestHandled = { launchRequest.value = null },
                            )
                        }
                    }
                }
            }
        }
        composeRule.mainClock.autoAdvance = false
        settle(2_000)
        launchRequest.value = shot.launch
        settle(1_000)
        shot.waitForText?.let { text -> settleUntil { exists(text) } }
        shot.steps(this)
        settle(3_000)
        composeRule.onRoot().captureRoboImage("$OUTPUT_DIR/${tablet.folder}/${shot.name}.png")
        scenario.close()
    }

    private fun exists(text: String): Boolean =
        composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()

    private fun labelledOnly(text: String) = SemanticsMatcher("only text '$text'") { node ->
        node.config.getOrNull(SemanticsProperties.Text)?.map { it.text } == listOf(text)
    }

    private fun visibleLabelIndex(text: String): Int? {
        val root = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        return composeRule.onAllNodes(labelledOnly(text)).fetchSemanticsNodes()
            .indexOfFirst { it.boundsInRoot.height > 0f && it.boundsInRoot.overlaps(root) }
            .takeIf { it >= 0 }
    }

    private fun clickText(text: String) {
        repeat(MAX_SCROLLS) {
            if (visibleLabelIndex(text) != null) return@repeat
            composeRule.onRoot().performTouchInput { swipeUp(startY = height * 0.7f, endY = height * 0.4f) }
            settle(1_000)
        }
        val index = visibleLabelIndex(text) ?: return
        composeRule.onAllNodes(labelledOnly(text))[index].performClick()
        settle(1_500)
    }

    private fun textNodes(predicate: (String) -> Boolean): List<Pair<String, Rect>> =
        composeRule.onAllNodes(SemanticsMatcher("text") { true }, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .mapNotNull { node ->
                val text = node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }
                    ?: return@mapNotNull null
                if (predicate(text)) text to node.boundsInRoot else null
            }

    private fun letterNodes() = textNodes { it.length == 1 && it[0].isLetter() }

    private fun clusters(values: List<Float>): List<Float> =
        values.sorted()
            .fold(mutableListOf<MutableList<Float>>()) { groups, value ->
                if (groups.isEmpty() || value - groups.last().last() > CLUSTER_TOLERANCE_PX) {
                    groups.add(mutableListOf(value))
                } else {
                    groups.last().add(value)
                }
                groups
            }
            .map { it.average().toFloat() }

    private fun List<Float>.nearestIndex(value: Float): Int = indices.minBy { abs(this[it] - value) }

    private fun selectEmptySudokuCell() {
        val root = composeRule.onRoot().fetchSemanticsNode().size
        val dock = textNodes { it == "Undo" }.firstOrNull()?.second ?: return
        val landscape = root.width > root.height
        val givens = textNodes { it.length == 1 && it[0].isDigit() }
            .map { it.second.center }
            .filter { if (landscape) it.x < dock.left else it.y < dock.top }
        val rowYs = clusters(givens.map { it.y })
        val colXs = clusters(givens.map { it.x })
        if (rowYs.size != SUDOKU_SIZE || colXs.size != SUDOKU_SIZE) return
        val taken = givens.map { rowYs.nearestIndex(it.y) to colXs.nearestIndex(it.x) }.toSet()
        val middle = SUDOKU_SIZE / 2
        val target = rowYs.indices.flatMap { row -> colXs.indices.map { col -> row to col } }
            .filter { it !in taken }
            .minByOrNull { (row, col) -> abs(row - middle) + abs(col - middle) } ?: return
        composeRule.onRoot().performTouchInput {
            click(Offset(colXs[target.second], rowYs[target.first]))
        }
        settle(1_000)
    }

    private fun findWordSearchWords(count: Int) {
        val letters = letterNodes().map { (text, bounds) -> text to bounds.center }
        val rowYs = clusters(letters.map { it.second.y })
        val colXs = clusters(letters.map { it.second.x })
        val grid = letters.associate { (text, center) ->
            (rowYs.nearestIndex(center.y) to colXs.nearestIndex(center.x)) to (text.uppercase() to center)
        }
        val words = textNodes { text -> text.length >= 3 && text.all { it in 'A'..'Z' } }.map { it.first }.distinct()
        val directions = listOf(0 to 1, 1 to 0, 1 to 1, -1 to 1, 0 to -1, -1 to 0, -1 to -1, 1 to -1)
        val found = words.mapNotNull { word ->
            grid.keys.asSequence().flatMap { start -> directions.asSequence().map { start to it } }
                .firstOrNull { (start, dir) ->
                    word.indices.all { i ->
                        grid[(start.first + dir.first * i) to (start.second + dir.second * i)]?.first == word[i].toString()
                    }
                }
                ?.let { (start, dir) ->
                    val end = (start.first + dir.first * (word.length - 1)) to (start.second + dir.second * (word.length - 1))
                    grid.getValue(start).second to grid.getValue(end).second
                }
        }.take(count)
        found.forEach { (from, to) ->
            composeRule.onRoot().performTouchInput {
                down(from)
                repeat(DRAG_STEPS) { step -> moveTo(lerp(from, to, (step + 1f) / DRAG_STEPS)) }
                up()
            }
            settle(800)
        }
    }

    private fun settle(millis: Long) {
        repeat((millis / FRAME_STEP_MS).toInt()) {
            composeRule.mainClock.advanceTimeBy(FRAME_STEP_MS)
            ShadowLooper.idleMainLooper()
            Thread.sleep(REAL_STEP_MS)
        }
    }

    private fun settleUntil(condition: () -> Boolean) {
        repeat(MAX_WAIT_STEPS) {
            if (condition()) return
            settle(FRAME_STEP_MS * 5)
        }
    }

    private fun millisOn(date: LocalDate, hour: Int): Long =
        date.atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private suspend fun seed() {
        GameId.entries.forEach {
            settingsRepository.markQuickStartSeen(it)
            settingsRepository.markNewBadgeSeen(it)
        }
        settingsRepository.markDailyReminderPermissionAsked()
        settingsRepository.setShowTimerInPlay(false)
        dailyRepository.setSubscribed(GameId.SUDOKU, true)
        dailyRepository.setSubscribed(GameId.WORDSEARCH, Difficulty.EASY, true)
        dailyRepository.setSubscribed(GameId.WORDSEARCH, Difficulty.MEDIUM, true)
        val today = LocalDate.now()
        seedDailies(today)
        seedHistory(today)
        seedJourney(today)
        seedBadges(today)
    }

    private suspend fun seedDailies(today: LocalDate) {
        (1..10).forEach { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            historyRepository.appendRecord(
                PlayRecord(
                    gameId = GameId.SUDOKU.key,
                    difficulty = Difficulty.EASY.key,
                    puzzleId = "daily-$daysAgo",
                    solved = true,
                    score = 9_100 + daysAgo * 7,
                    elapsedSeconds = 240 + daysAgo * 11,
                    timestampMillis = millisOn(date, 8),
                    dailyDate = date.toString(),
                ),
            )
        }
    }

    private suspend fun seedHistory(today: LocalDate) {
        val history = listOf(
            Triple(GameId.SUDOKU, Difficulty.EASY, 18 to 1),
            Triple(GameId.SUDOKU, Difficulty.MEDIUM, 8 to 2),
            Triple(GameId.SUDOKU, Difficulty.HARD, 2 to 1),
            Triple(GameId.SUDOKU, Difficulty.EXPERT, 2 to 1),
            Triple(GameId.WORDSEARCH, Difficulty.EASY, 6 to 0),
            Triple(GameId.WORDSEARCH, Difficulty.MEDIUM, 3 to 0),
        )
        var offset = 11L
        history.forEach { (gameId, difficulty, results) ->
            val (solved, lost) = results
            repeat(solved + lost) { index ->
                historyRepository.appendRecord(
                    PlayRecord(
                        gameId = gameId.key,
                        difficulty = difficulty.key,
                        puzzleId = "${gameId.key}-${difficulty.key}-$index",
                        solved = index >= lost,
                        score = 8_800 + index * 13,
                        elapsedSeconds = 300 + index * 17,
                        timestampMillis = millisOn(today.minusDays(offset++), 20),
                    ),
                )
            }
        }
    }

    private suspend fun seedJourney(today: LocalDate) {
        val track = settingsRepository.journeyTrack(GameId.SUDOKU).first()
        val stops = loadJourneyFile(app, track)?.chapters?.firstOrNull()?.stops ?: return
        val cleared = stops.take(minOf(JOURNEY_STOPS_CLEARED, stops.size - 1))
        cleared.forEachIndexed { index, stop ->
            val elapsedSeconds = if (index % 3 == 2) stop.targetSeconds + 45 else stop.targetSeconds * 2 / 3
            val score = 9_200 + index * 11
            val stars = journeyStore.recordWin(GameId.SUDOKU, stop.puzzleId, stop.puzzleId, JourneySignals(0, 0), elapsedSeconds, score)
            historyRepository.appendRecord(
                PlayRecord(
                    gameId = GameId.SUDOKU.key,
                    difficulty = stop.difficulty,
                    puzzleId = stop.puzzleId,
                    solved = true,
                    score = score,
                    elapsedSeconds = elapsedSeconds,
                    timestampMillis = millisOn(today.minusDays((cleared.size - index).toLong()), 19),
                    journeyStop = stop.puzzleId,
                    journeyStars = stars,
                ),
            )
        }
    }

    private suspend fun seedBadges(today: LocalDate) {
        val finishes = listOf(
            badgeFinish(GameId.SUDOKU, Difficulty.HARD, 1_900, today.minusDays(9)),
            badgeFinish(GameId.WORDSEARCH, Difficulty.MEDIUM, 310, today.minusDays(6), BadgeFacts(hiddenWordSolved = true)),
            badgeFinish(GameId.SUDOKU, Difficulty.EXPERT, 1_260, today.minusDays(4)),
            badgeFinish(GameId.SUDOKU, Difficulty.MEDIUM, 640, today.minusDays(2), startedDate = today.minusDays(3)),
        )
        finishes.forEach { badgeRepository.recordFinish(it) }
        badgeRepository.clearAnnouncement()
        badgeRepository.earned.first().maxByOrNull { it.value }?.let { badgeRepository.markSeen(it.key) }
    }

    private fun badgeFinish(
        gameId: GameId,
        difficulty: Difficulty,
        elapsedSeconds: Int,
        date: LocalDate,
        facts: BadgeFacts = BadgeFacts(),
        startedDate: LocalDate? = null,
    ) = FinishFacts(
        gameId = gameId,
        difficulty = difficulty,
        solved = true,
        elapsedSeconds = elapsedSeconds,
        date = date,
        startedDate = startedDate,
        journeyStopId = null,
        mixId = null,
        facts = facts,
    )

    private suspend fun seedStarBattleSession() {
        val puzzle = StarBattlePuzzleBank.ratedPool(app, Difficulty.MEDIUM)
            .mapNotNull { (id, _) -> StarBattlePuzzleBank.puzzleById(app, Difficulty.MEDIUM, id) }
            .minByOrNull { entry -> entry.regions.flatten().groupingBy { it }.eachCount().values.max() }
            ?: StarBattlePuzzleBank.randomPuzzle(app, Difficulty.MEDIUM)
            ?: return
        val regionSizes = puzzle.regions.flatten().groupingBy { it }.eachCount()
        val placed = puzzle.solution
            .flatMapIndexed { row, cols -> cols.map { col -> row to col } }
            .sortedBy { (row, col) -> regionSizes.getValue(puzzle.regions[row][col]) }
            .take(STARS_PLACED)
            .toSet()
        val cells = List(puzzle.size) { row ->
            List(puzzle.size) { col ->
                val cell = row to col
                when {
                    cell in placed -> StarBattleCellState.LOCKED_CORRECT
                    placed.any { star -> ruledOutBy(puzzle, star, cell) } -> StarBattleCellState.MARKED
                    else -> StarBattleCellState.EMPTY
                }
            }
        }
        val persisted = StarBattlePersistedSession(
            puzzle = puzzle,
            cells = cells.flatten().map { it.ordinal },
            lives = STARBATTLE_STARTING_LIVES,
            status = StarBattleStatus.PLAYING.name,
        )
        sessionRepository.save(
            ActiveSessionEnvelope(
                gameId = GameId.STARBATTLE.key,
                elapsedSeconds = STARBATTLE_ELAPSED_SECONDS,
                payload = Json.encodeToString(StarBattlePersistedSession.serializer(), persisted),
                difficulty = Difficulty.MEDIUM.key,
            ),
        )
    }

    private fun ruledOutBy(puzzle: StarBattlePuzzleEntry, star: Pair<Int, Int>, cell: Pair<Int, Int>): Boolean {
        val (starRow, starCol) = star
        val (row, col) = cell
        val touching = abs(row - starRow) <= 1 && abs(col - starCol) <= 1
        val sameRegion = puzzle.regions[row][col] == puzzle.regions[starRow][starCol]
        return touching || (puzzle.k == 1 && sameRegion)
    }

    private companion object {
        const val OUTPUT_DIR = "../tools/store-art/captures"
        const val FRAME_STEP_MS = 100L
        const val REAL_STEP_MS = 40L
        const val MAX_WAIT_STEPS = 60
        const val WORDS_TO_FIND = 5
        const val MAX_SCROLLS = 8
        const val DRAG_STEPS = 12
        const val CLUSTER_TOLERANCE_PX = 6f
        const val SUDOKU_SIZE = 9
        const val JOURNEY_STOPS_CLEARED = 7
        const val STARS_PLACED = 3
        const val STARBATTLE_ELAPSED_SECONDS = 140.0
    }
}
