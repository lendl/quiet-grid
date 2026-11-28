package com.quietgrid.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import okio.Path.Companion.toPath

const val QUIET_GRID_DATA_STORE_FILE = "quiet_grid.preferences_pb"
const val QUIET_GRID_DATABASE_FILE = "quiet_grid.db"

fun createQuietGridDataStore(path: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { path.toPath() })

fun RoomDatabase.Builder<QuietGridDatabase>.buildQuietGrid(driver: SQLiteDriver): QuietGridDatabase =
    addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
        .fallbackToDestructiveMigration(dropAllTables = false)
        .setDriver(driver)
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
