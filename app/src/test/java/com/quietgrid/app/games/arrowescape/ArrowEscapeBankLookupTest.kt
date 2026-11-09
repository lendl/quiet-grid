package com.quietgrid.app.games.arrowescape

import android.content.Context
import android.content.res.AssetManager
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.data.AppSettings
import com.quietgrid.app.data.SettingsRepository
import com.quietgrid.app.testutil.FakeHistoryStore
import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import io.mockk.every
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private val bankJson = Json { ignoreUnknownKeys = true }

class ArrowEscapeBankLookupTest {

    private val assets = mockk<AssetManager>().also {
        every { it.open(any<String>()) } answers { File("src/main/assets/${firstArg<String>()}").inputStream() }
    }
    private val context = mockk<Context>(relaxed = true).also {
        every { it.assets } returns assets
    }
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true).also {
        every { it.settings } returns MutableStateFlow(AppSettings())
    }

    private val halloweenPuzzle: ArrowEscapePuzzleEntry = bankJson
        .decodeFromString<List<ArrowEscapePuzzleEntry>>(File("src/main/assets/arrowescape_halloween_puzzles.json").readText())
        .first()

    @Test
    fun `a halloween puzzle id resolves from the seasonal bank`() = runBlocking {
        val difficulty = Difficulty.fromKey(halloweenPuzzle.difficulty)
        assertEquals(halloweenPuzzle, ArrowEscapePuzzleBank.puzzleById(context, difficulty, halloweenPuzzle.id))
    }

    @Test
    fun `a halloween session saved by id resumes`() = runBlocking {
        val adapter = ArrowEscapePuzzleAdapter(context, FakeHistoryStore(), settingsRepository)
        val session = createArrowEscapeSession(halloweenPuzzle).copy(removedIndices = setOf(0))
        assertEquals(session, adapter.restoreSession(adapter.encode(session), 0.0))
    }

    @Test
    fun `an id in neither bank resolves to nothing`() = runBlocking {
        val adapter = ArrowEscapePuzzleAdapter(context, FakeHistoryStore(), settingsRepository)
        val payload = adapter.encode(createArrowEscapeSession(halloweenPuzzle.copy(id = "ae-not-in-any-bank")))
        assertNull(ArrowEscapePuzzleBank.puzzleById(context, Difficulty.fromKey(halloweenPuzzle.difficulty), "ae-not-in-any-bank"))
        assertNull(adapter.restoreSession(payload, 0.0))
    }
}
