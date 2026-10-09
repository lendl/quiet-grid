package com.quietgrid.app.widget

import com.quietgrid.app.core.mix.Mix
import com.quietgrid.app.core.mix.MixEntry
import com.quietgrid.app.core.mix.MixEntryMode
import com.quietgrid.app.data.ActiveSessionEnvelope
import com.quietgrid.app.data.MixRepository
import com.quietgrid.app.data.SessionRepository
import com.quietgrid.app.data.SettingsRepository
import com.quietgrid.app.data.ThemeMode
import com.quietgrid.app.data.preferencesDataStoreForTest
import com.quietgrid.app.testutil.FakeHistoryStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class QuickPlaySourceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private class Fixture(
        val sessionRepository: SessionRepository,
        val mixRepository: MixRepository,
        val settingsRepository: SettingsRepository,
    ) {
        fun source() = QuickPlaySource(sessionRepository, mixRepository, FakeHistoryStore(), settingsRepository)
    }

    private fun TestScope.fixture(): Fixture {
        val dataStore = preferencesDataStoreForTest(backgroundScope) { tempFolder.newFile("widget.preferences_pb") }
        return Fixture(SessionRepository(dataStore), MixRepository(dataStore), SettingsRepository(dataStore))
    }

    @Test
    fun `first render with nothing stored is default games in system theme`() = runTest {
        val render = fixture().source().render.first()

        assertEquals(ThemeMode.SYSTEM, render.themeMode)
        assertEquals(QUICK_PLAY_DEFAULT_GAMES, (render.state as QuickPlayState.Games).games.map { it.gameId })
    }

    @Test
    fun `saving a session switches to Continue and clearing it switches back`() = runTest {
        val fixture = fixture()

        fixture.sessionRepository.save(ActiveSessionEnvelope(gameId = "sudoku", elapsedSeconds = 10.0, payload = "{}"))
        assertEquals(QuickPlayState.Continue, fixture.source().render.first().state)

        fixture.sessionRepository.clear()
        assertTrue(fixture.source().render.first().state is QuickPlayState.Games)
    }

    @Test
    fun `started mix shows PlayMix`() = runTest {
        val fixture = fixture()

        fixture.mixRepository.saveMix(Mix("mix-1", "Evenings", listOf(MixEntry("sudoku", MixEntryMode.PUZZLE, "easy", 1))))
        fixture.mixRepository.setActiveMix("mix-1")

        assertEquals(QuickPlayState.PlayMix("mix-1"), fixture.source().render.first().state)
    }

    @Test
    fun `theme mode follows settings`() = runTest {
        val fixture = fixture()

        fixture.settingsRepository.setThemeMode(ThemeMode.PENCIL)

        assertEquals(ThemeMode.PENCIL, fixture.source().render.first().themeMode)
    }
}
