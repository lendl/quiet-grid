package com.quietgrid.app.data

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class QuietGridStorageTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `data store keeps the file the old preferencesDataStore delegate used`() {
        assertEquals(File(context.filesDir, "datastore/quiet_grid.preferences_pb").absolutePath, quietGridDataStorePath(context))
    }

    @Test
    fun `graph data store reads a value written to the legacy file`() = runBlocking {
        val key = stringPreferencesKey("probe")
        val legacyScope = CoroutineScope(Dispatchers.IO + Job())
        PreferenceDataStoreFactory.create(scope = legacyScope) { File(quietGridDataStorePath(context)) }.edit { it[key] = "kept" }
        legacyScope.cancel()
        assertEquals("kept", quietGridDataStore(context).data.first()[key])
    }
}
