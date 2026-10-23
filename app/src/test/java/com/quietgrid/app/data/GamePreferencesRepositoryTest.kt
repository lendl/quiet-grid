package com.quietgrid.app.data

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GamePreferencesRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `a game follows the defaults until it is changed`() = runTest {
        val repository = GamePreferencesRepository(newDataStore(backgroundScope))

        val overrides = repository.overrides(GameId.SUDOKU).first()

        assertTrue(overrides.isEmpty)
        assertEquals(GamePlayPreferences(), resolveGamePlayPreferences(AppSettings(), overrides))
    }

    @Test
    fun `an override wins over the default for that game only`() = runTest {
        val repository = GamePreferencesRepository(newDataStore(backgroundScope))

        repository.setToggle(GameId.SUDOKU, GameToggle.TIMER, false)
        repository.setRotation(GameId.SUDOKU, ScreenRotation.LANDSCAPE)

        val sudoku = resolveGamePlayPreferences(AppSettings(), repository.overrides(GameId.SUDOKU).first())
        val takuzu = resolveGamePlayPreferences(AppSettings(), repository.overrides(GameId.TAKUZU).first())
        assertFalse(sudoku.showTimer)
        assertEquals(ScreenRotation.LANDSCAPE, sudoku.rotation)
        assertTrue(takuzu.showTimer)
        assertEquals(ScreenRotation.AUTO, takuzu.rotation)
    }

    @Test
    fun `a game without an override picks up a changed default`() = runTest {
        val repository = GamePreferencesRepository(newDataStore(backgroundScope))
        val settings = AppSettings(showHintsInPlay = false, hapticsEnabled = true, screenRotation = ScreenRotation.PORTRAIT)

        repository.setToggle(GameId.SUDOKU, GameToggle.HINTS, true)

        val sudoku = resolveGamePlayPreferences(settings, repository.overrides(GameId.SUDOKU).first())
        assertTrue(sudoku.showHints)
        assertTrue(sudoku.haptics)
        assertEquals(ScreenRotation.PORTRAIT, sudoku.rotation)
    }

    @Test
    fun `clearing an override returns to the default`() = runTest {
        val repository = GamePreferencesRepository(newDataStore(backgroundScope))

        repository.setToggle(GameId.SUDOKU, GameToggle.KEEP_SCREEN_ON, true)
        repository.setToggle(GameId.SUDOKU, GameToggle.KEEP_SCREEN_ON, null)

        assertTrue(repository.overrides(GameId.SUDOKU).first().isEmpty)
    }

    @Test
    fun `use defaults clears every override for the game`() = runTest {
        val repository = GamePreferencesRepository(newDataStore(backgroundScope))

        repository.setToggle(GameId.HASHI, GameToggle.HINTS, false)
        repository.setToggle(GameId.HASHI, GameToggle.HAPTICS, true)
        repository.setRotation(GameId.HASHI, ScreenRotation.PORTRAIT)
        repository.setToggle(GameId.SUDOKU, GameToggle.TIMER, false)
        repository.clear(GameId.HASHI)

        assertTrue(repository.overrides(GameId.HASHI).first().isEmpty)
        assertEquals(mapOf(GameToggle.TIMER to false), repository.overrides(GameId.SUDOKU).first().toggles)
    }

    @Test
    fun `last difficulty is stored per game`() = runTest {
        val repository = GamePreferencesRepository(newDataStore(backgroundScope))

        repository.setLastDifficulty(GameId.SUDOKU, Difficulty.HARD)

        assertEquals(Difficulty.HARD, repository.lastDifficulty(GameId.SUDOKU).first())
        assertEquals(null, repository.lastDifficulty(GameId.TAKUZU).first())
    }

    @Test
    fun `all overrides lists only games that changed something`() = runTest {
        val repository = GamePreferencesRepository(newDataStore(backgroundScope))

        repository.setToggle(GameId.TAKUZU, GameToggle.LEFT_HANDED, true)

        val all = repository.allOverrides.first()
        assertEquals(setOf(GameId.TAKUZU), all.keys)
        assertTrue(resolveGamePlayPreferences(AppSettings(), all.getValue(GameId.TAKUZU)).leftHanded)
    }

    private fun newDataStore(scope: CoroutineScope) = preferencesDataStoreForTest(
        scope = scope,
        produceFile = { tempFolder.newFile("game_prefs.preferences_pb") },
    )
}
