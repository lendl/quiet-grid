package com.quietgrid.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

fun quietGridDataStore(): DataStore<Preferences> =
    createQuietGridDataStore("${documentDirectory()}/$QUIET_GRID_DATA_STORE_FILE")

fun quietGridDatabase(): QuietGridDatabase =
    Room.databaseBuilder<QuietGridDatabase>(name = "${documentDirectory()}/$QUIET_GRID_DATABASE_FILE")
        .buildQuietGrid(BundledSQLiteDriver())

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val url = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return requireNotNull(url?.path)
}
