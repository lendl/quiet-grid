package com.quietgrid.app.games

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.games.arrowescape.decodeArrowEscapePool
import com.quietgrid.app.games.wordsearch.decodeWordSearchPool
import com.quietgrid.app.platform.AndroidBundledFiles
import com.quietgrid.app.platform.readText
import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import com.quietgrid.engine.wordsearch.WordSearchBankEntry
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class BankStreamingTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val files = AndroidBundledFiles(context)
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `word search stream decode matches whole text decode`() = runBlocking {
        val asset = "wordsearch_puzzles_easy.json"
        val streamed = decodeWordSearchPool({ files.open(asset) }, "en", "easy").map { it.id }
        val whole = json.decodeFromString<List<WordSearchBankEntry>>(files.readText(asset))
            .filter { it.locale == "en" }
            .map { it.id }
        assertTrue(streamed.isNotEmpty())
        assertEquals(whole, streamed)
    }

    @Test
    fun `arrow escape stream decode matches whole text decode`() = runBlocking {
        val asset = "arrowescape_puzzles_easy.json"
        val streamed = decodeArrowEscapePool(files.open(asset), Difficulty.EASY).map { it.id }
        val whole = json.decodeFromString<List<ArrowEscapePuzzleEntry>>(files.readText(asset))
            .filter { it.difficulty == Difficulty.EASY.key }
            .map { it.id }
        assertTrue(streamed.isNotEmpty())
        assertEquals(whole, streamed)
    }
}
