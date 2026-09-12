package com.quietgrid.app.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.quietgrid.app.core.mix.Mix
import com.quietgrid.app.core.mix.MixEntry
import com.quietgrid.app.core.mix.MixEntryMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MixRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val sampleMix = Mix(
        id = "mix-1",
        name = "Evenings",
        entries = listOf(MixEntry(gameId = "sudoku", mode = MixEntryMode.PUZZLE, difficulty = "hard", weight = 2)),
    )

    @Test
    fun `mixes is empty and activeMixId is null before anything is saved`() = runTest {
        val repository = MixRepository(newDataStore(backgroundScope))

        assertTrue(repository.mixes.first().isEmpty())
        assertNull(repository.activeMixId.first())
    }

    @Test
    fun `saveMix persists a mix that mixes then reflects`() = runTest {
        val repository = MixRepository(newDataStore(backgroundScope))

        repository.saveMix(sampleMix)

        assertEquals(listOf(sampleMix), repository.mixes.first())
    }

    @Test
    fun `saveMix with an existing id replaces that mix instead of duplicating it`() = runTest {
        val repository = MixRepository(newDataStore(backgroundScope))
        repository.saveMix(sampleMix)

        repository.saveMix(sampleMix.copy(name = "Renamed"))

        val stored = repository.mixes.first()
        assertEquals(1, stored.size)
        assertEquals("Renamed", stored.first().name)
    }

    @Test
    fun `deleteMix removes it from mixes`() = runTest {
        val repository = MixRepository(newDataStore(backgroundScope))
        repository.saveMix(sampleMix)

        repository.deleteMix(sampleMix.id)

        assertTrue(repository.mixes.first().isEmpty())
    }

    @Test
    fun `deleteMix clears activeMixId when it was the active mix`() = runTest {
        val repository = MixRepository(newDataStore(backgroundScope))
        repository.saveMix(sampleMix)
        repository.setActiveMix(sampleMix.id)

        repository.deleteMix(sampleMix.id)

        assertNull(repository.activeMixId.first())
    }

    @Test
    fun `setActiveMix then clearActiveMix round-trips`() = runTest {
        val repository = MixRepository(newDataStore(backgroundScope))

        repository.setActiveMix(sampleMix.id)
        assertEquals(sampleMix.id, repository.activeMixId.first())

        repository.clearActiveMix()
        assertNull(repository.activeMixId.first())
    }

    private fun newDataStore(scope: kotlinx.coroutines.CoroutineScope) = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { tempFolder.newFile("mixes.preferences_pb") },
    )
}
