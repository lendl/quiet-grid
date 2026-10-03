package com.quietgrid.app.data

import com.quietgrid.app.core.GameId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ThemePreferencesRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `nothing is excluded by default`() = runTest {
        val repository = ThemePreferencesRepository(newDataStore(backgroundScope))

        assertEquals(emptySet<String>(), repository.excludedThemes(GameId.THEMECLEAR, "en").first())
    }

    @Test
    fun `excluded themes persist`() = runTest {
        val repository = ThemePreferencesRepository(newDataStore(backgroundScope))

        repository.setExcludedThemes(GameId.THEMECLEAR, "en", setOf("food", "space"))

        assertEquals(setOf("food", "space"), repository.excludedThemes(GameId.THEMECLEAR, "en").first())
    }

    @Test
    fun `games and locales are isolated`() = runTest {
        val repository = ThemePreferencesRepository(newDataStore(backgroundScope))

        repository.setExcludedThemes(GameId.THEMECLEAR, "en", setOf("food"))
        repository.setExcludedThemes(GameId.WORDSEARCH, "nl", setOf("dieren"))

        assertEquals(emptySet<String>(), repository.excludedThemes(GameId.THEMECLEAR, "nl").first())
        assertEquals(emptySet<String>(), repository.excludedThemes(GameId.WORDSEARCH, "en").first())
        assertEquals(setOf("dieren"), repository.excludedThemes(GameId.WORDSEARCH, "nl").first())
    }

    @Test
    fun `setting an empty set clears exclusions`() = runTest {
        val repository = ThemePreferencesRepository(newDataStore(backgroundScope))

        repository.setExcludedThemes(GameId.WORDSEARCH, "en", setOf("food"))
        repository.setExcludedThemes(GameId.WORDSEARCH, "en", emptySet())

        assertEquals(emptySet<String>(), repository.excludedThemes(GameId.WORDSEARCH, "en").first())
    }

    @Test
    fun `toggling two different themes in sequence excludes both`() = runTest {
        val repository = ThemePreferencesRepository(newDataStore(backgroundScope))
        val allThemeIds = setOf("animals", "food", "space")

        repository.toggleExcludedTheme(GameId.THEMECLEAR, "en", "food", allThemeIds)
        repository.toggleExcludedTheme(GameId.THEMECLEAR, "en", "space", allThemeIds)

        assertEquals(setOf("food", "space"), repository.excludedThemes(GameId.THEMECLEAR, "en").first())
    }

    @Test
    fun `toggling the last selected theme leaves the set unchanged`() = runTest {
        val repository = ThemePreferencesRepository(newDataStore(backgroundScope))
        val allThemeIds = setOf("animals", "food")
        repository.setExcludedThemes(GameId.THEMECLEAR, "en", setOf("food"))

        repository.toggleExcludedTheme(GameId.THEMECLEAR, "en", "animals", allThemeIds)

        assertEquals(setOf("food"), repository.excludedThemes(GameId.THEMECLEAR, "en").first())
    }

    @Test
    fun `toggling an excluded theme re-includes it`() = runTest {
        val repository = ThemePreferencesRepository(newDataStore(backgroundScope))
        val allThemeIds = setOf("animals", "food", "space")
        repository.setExcludedThemes(GameId.THEMECLEAR, "en", setOf("food", "space"))

        repository.toggleExcludedTheme(GameId.THEMECLEAR, "en", "food", allThemeIds)

        assertEquals(setOf("space"), repository.excludedThemes(GameId.THEMECLEAR, "en").first())
    }

    private fun newDataStore(scope: CoroutineScope) = preferencesDataStoreForTest(
        scope = scope,
        produceFile = { tempFolder.newFile("themes.preferences_pb") },
    )
}
