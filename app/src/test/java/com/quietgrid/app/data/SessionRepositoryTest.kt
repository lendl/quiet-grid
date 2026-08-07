package com.quietgrid.app.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SessionRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `activeSession is null when nothing has been saved`() = runTest {
        val repository = SessionRepository(newDataStore(backgroundScope))

        assertNull(repository.activeSession.first())
    }

    @Test
    fun `save persists an envelope that activeSession then reflects`() = runTest {
        val repository = SessionRepository(newDataStore(backgroundScope))
        val envelope = ActiveSessionEnvelope(gameId = "sudoku", elapsedSeconds = 12.0, payload = "abc")

        repository.save(envelope)

        assertEquals(envelope, repository.activeSession.first())
    }

    @Test
    fun `clear removes the persisted envelope`() = runTest {
        val repository = SessionRepository(newDataStore(backgroundScope))
        repository.save(ActiveSessionEnvelope(gameId = "sudoku", elapsedSeconds = 12.0, payload = "abc"))

        repository.clear()

        assertNull(repository.activeSession.first())
    }

    @Test
    fun `unparseable stored payload is treated as no active session`() = runTest {
        val dataStore = newDataStore(backgroundScope)
        dataStore.edit { it[stringPreferencesKey("active_session")] = "not valid json" }
        val repository = SessionRepository(dataStore)

        assertNull(repository.activeSession.first())
    }

    private fun newDataStore(scope: kotlinx.coroutines.CoroutineScope) = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { tempFolder.newFile("session.preferences_pb") },
    )
}
