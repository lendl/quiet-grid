package com.quietgrid.app.ui.screens

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.mix.Mix
import com.quietgrid.app.core.mix.MixEntry
import com.quietgrid.app.core.mix.MixEntryMode
import com.quietgrid.app.data.MixRepository
import com.quietgrid.app.data.preferencesDataStoreForTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EmptyMixCleanupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val emptyMix = Mix(id = "mix-1", name = "My Mix #1", entries = emptyList())

    @Test
    fun `an empty mix stays while the editor is open and is deleted when the editor closes`() = runTest {
        val repository = MixRepository(newDataStore(backgroundScope))
        repository.saveMix(emptyMix)
        val store = ViewModelStore()
        openCleanup(store, repository, emptyMix.id)
        repository.awaitPendingWrites()

        assertEquals(listOf(emptyMix), repository.mixes.first())

        store.clear()
        repository.awaitPendingWrites()

        assertTrue(repository.mixes.first().isEmpty())
    }

    @Test
    fun `a mix with entries is kept when the editor closes`() = runTest {
        val repository = MixRepository(newDataStore(backgroundScope))
        val filledMix = emptyMix.copy(
            entries = listOf(MixEntry(gameId = "sudoku", mode = MixEntryMode.PUZZLE, difficulty = "easy", weight = 1)),
        )
        repository.saveMix(filledMix)
        val store = ViewModelStore()
        openCleanup(store, repository, filledMix.id)

        store.clear()
        repository.awaitPendingWrites()

        assertEquals(listOf(filledMix), repository.mixes.first())
    }

    private suspend fun MixRepository.awaitPendingWrites() = clearActiveMix()

    private fun openCleanup(store: ViewModelStore, repository: MixRepository, mixId: String) {
        val factory = viewModelFactory { initializer { EmptyMixCleanupViewModel(mixId, repository) } }
        ViewModelProvider.create(store, factory)[EmptyMixCleanupViewModel::class]
    }

    private fun newDataStore(scope: CoroutineScope) = preferencesDataStoreForTest(
        scope = scope,
        produceFile = { tempFolder.newFile("mixes.preferences_pb") },
    )
}
