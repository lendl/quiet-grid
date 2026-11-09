package com.quietgrid.app.games.arrowescape

import android.content.Context
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.data.AppSettings
import com.quietgrid.app.data.SettingsRepository
import com.quietgrid.app.testutil.FakeHistoryStore
import com.quietgrid.engine.arrowescape.ArrowEscapePieceData
import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

private val STOP = ArrowEscapePuzzleEntry(
    id = "ae-stop",
    difficulty = Difficulty.HARD.key,
    rows = 2,
    cols = 1,
    pieces = listOf(
        ArrowEscapePieceData(cells = listOf(listOf(0, 0)), headDirection = "down"),
        ArrowEscapePieceData(cells = listOf(listOf(1, 0)), headDirection = "down"),
    ),
)

private val ASYMMETRIC = ArrowEscapePuzzleEntry(
    id = "ae-mirror",
    difficulty = Difficulty.HARD.key,
    rows = 1,
    cols = 3,
    pieces = listOf(
        ArrowEscapePieceData(cells = listOf(listOf(0, 0), listOf(0, 1)), headDirection = "right"),
        ArrowEscapePieceData(cells = listOf(listOf(0, 2)), headDirection = "up"),
    ),
    shape = "test",
    mask = listOf("###"),
)

private val MIRRORED_CELLS = listOf(listOf(0, 2), listOf(0, 1))

class ArrowEscapeSessionRestoreTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val context = mockk<Context>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true).also {
        every { it.settings } returns MutableStateFlow(AppSettings())
    }
    private val adapter = ArrowEscapePuzzleAdapter(context, FakeHistoryStore(), settingsRepository)

    @Before
    fun setUp() {
        mockkObject(ArrowEscapePuzzleBank)
        coEvery { ArrowEscapePuzzleBank.puzzleById(any(), any(), any()) } returns null
        coEvery { ArrowEscapePuzzleBank.puzzleById(any(), Difficulty.HARD, "ae-stop") } returns STOP
        coEvery { ArrowEscapePuzzleBank.puzzleById(any(), Difficulty.HARD, "ae-mirror") } returns ASYMMETRIC
    }

    @After
    fun tearDown() {
        unmockkObject(ArrowEscapePuzzleBank)
    }

    @Test
    fun `a session saved with the whole puzzle still resumes`() = runBlocking {
        val legacy = json.encodeToString(
            ArrowEscapePersistedSession(
                puzzle = STOP,
                removedIndices = listOf(1),
                lives = 2,
                selectedIndex = null,
                status = ArrowEscapeStatus.PLAYING.name,
                hintsUsed = 1,
            ),
        )
        val restored = adapter.restoreSession(legacy, 0.0)!!
        assertEquals(STOP, restored.puzzle)
        assertEquals(setOf(1), restored.removedIndices)
        assertEquals(2, restored.lives)
        assertEquals(1, restored.hintsUsed)
        assertEquals(0, restored.transform)
    }

    @Test
    fun `a new save stores only the puzzle id and resumes from the bank`() = runBlocking {
        val session = createArrowEscapeSession(STOP).copy(removedIndices = setOf(0), wrongIndices = setOf(1), selectedIndex = 1, lives = 2)
        val payload = adapter.encode(session)
        assertFalse(payload.contains("pieces"))
        assertEquals(session, adapter.restoreSession(payload, 0.0))
    }

    @Test
    fun `a journey save with a transform comes back transformed`() = runBlocking {
        val session = adapter.journeySession(Difficulty.HARD, "ae-mirror", 1)!!.copy(removedIndices = setOf(1))
        assertEquals(1, session.transform)
        val restored = adapter.restoreSession(adapter.encode(session), 0.0)!!
        assertEquals(MIRRORED_CELLS, restored.puzzle.pieces.first().cells)
        assertEquals("left", restored.puzzle.pieces.first().headDirection)
        assertEquals(1, restored.transform)
        assertEquals(session, restored)
    }

    @Test
    fun `an old journey save with an inline mirrored puzzle keeps its transform after the next save`() = runBlocking {
        val mirrored = adapter.journeySession(Difficulty.HARD, "ae-mirror", 1)!!.puzzle
        val legacy = json.encodeToString(
            ArrowEscapePersistedSession(
                puzzle = mirrored,
                removedIndices = listOf(1),
                lives = 3,
                selectedIndex = null,
                status = ArrowEscapeStatus.PLAYING.name,
            ),
        )
        val restored = adapter.restoreSession(legacy, 0.0)!!
        assertEquals(1, restored.transform)
        val resaved = adapter.restoreSession(adapter.encode(restored), 0.0)!!
        assertEquals(mirrored, resaved.puzzle)
    }

    @Test
    fun `an unknown puzzle id starts fresh`() = runBlocking {
        val payload = adapter.encode(createArrowEscapeSession(STOP.copy(id = "ae-gone")))
        assertNull(adapter.restoreSession(payload, 0.0))
    }

    @Test
    fun `a save whose indices no longer fit the puzzle starts fresh`() = runBlocking {
        val payload = adapter.encode(createArrowEscapeSession(STOP).copy(removedIndices = setOf(5)))
        assertNull(adapter.restoreSession(payload, 0.0))
    }
}
