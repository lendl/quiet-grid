package com.quietgrid.app.data

import com.quietgrid.app.core.GameId
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
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

    private fun newDataStore(scope: CoroutineScope) = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { tempFolder.newFile("daily.preferences_pb") },
    )
}
