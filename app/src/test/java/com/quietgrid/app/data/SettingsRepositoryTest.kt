package com.quietgrid.app.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `settings default to system theme, timer shown, beta games disabled`() = runTest {
        val repository = SettingsRepository(newDataStore(backgroundScope))

        val settings = repository.settings.first()

        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertTrue(settings.showTimerInPlay)
        assertFalse(settings.betaGamesEnabled)
    }

    @Test
    fun `setThemeMode persists the chosen mode`() = runTest {
        val repository = SettingsRepository(newDataStore(backgroundScope))

        repository.setThemeMode(ThemeMode.PENCIL)

        assertEquals(ThemeMode.PENCIL, repository.settings.first().themeMode)
    }

    @Test
    fun `setShowTimerInPlay persists false`() = runTest {
        val repository = SettingsRepository(newDataStore(backgroundScope))

        repository.setShowTimerInPlay(false)

        assertFalse(repository.settings.first().showTimerInPlay)
    }

    @Test
    fun `setBetaGamesEnabled persists true`() = runTest {
        val repository = SettingsRepository(newDataStore(backgroundScope))

        repository.setBetaGamesEnabled(true)

        assertTrue(repository.settings.first().betaGamesEnabled)
    }

    private fun newDataStore(scope: CoroutineScope) = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { tempFolder.newFile("settings.preferences_pb") },
    )
}
