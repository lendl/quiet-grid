package com.quietgrid.app.data

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DailyRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `defaults to no subscriptions`() = runTest {
        val repository = DailyRepository(newDataStore(backgroundScope))
        assertEquals(emptySet<GameId>(), repository.subscribedGames.first())
    }

    @Test
    fun `subscribe and unsubscribe persist`() = runTest {
        val repository = DailyRepository(newDataStore(backgroundScope))

        repository.setSubscribed(GameId.SUDOKU, true)
        repository.setSubscribed(GameId.TAKUZU, true)
        repository.setSubscribed(GameId.SUDOKU, false)

        assertEquals(setOf(GameId.TAKUZU), repository.subscribedGames.first())
    }

    @Test
    fun `per difficulty subscriptions persist and last unsubscribe drops game`() = runTest {
        val repository = DailyRepository(newDataStore(backgroundScope))

        repository.setSubscribed(GameId.SUDOKU, Difficulty.EASY, true)
        repository.setSubscribed(GameId.SUDOKU, Difficulty.HARD, true)
        repository.setSubscribed(GameId.TAKUZU, Difficulty.MEDIUM, true)
        repository.setSubscribed(GameId.TAKUZU, Difficulty.MEDIUM, false)

        assertEquals(mapOf(GameId.SUDOKU to setOf(Difficulty.EASY, Difficulty.HARD)), repository.subscriptions.first())
    }

    @Test
    fun `legacy whole game entry reads as every difficulty`() = runTest {
        val dataStore = newDataStore(backgroundScope)
        dataStore.edit { it[stringSetPreferencesKey("daily_subscriptions")] = setOf(GameId.SUDOKU.key) }
        val repository = DailyRepository(dataStore)

        repository.setSubscribed(GameId.SUDOKU, Difficulty.EXPERT, false)

        assertEquals(mapOf(GameId.SUDOKU to Difficulty.entries.toSet() - Difficulty.EXPERT), repository.subscriptions.first())
    }

    private fun newDataStore(scope: CoroutineScope) = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { tempFolder.newFile("daily.preferences_pb") },
    )
}
