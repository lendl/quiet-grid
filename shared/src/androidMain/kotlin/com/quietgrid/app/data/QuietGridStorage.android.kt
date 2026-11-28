package com.quietgrid.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import java.io.File

fun quietGridDataStorePath(context: Context): String =
    File(context.filesDir, "datastore/$QUIET_GRID_DATA_STORE_FILE").absolutePath

fun quietGridDataStore(context: Context): DataStore<Preferences> =
    createQuietGridDataStore(quietGridDataStorePath(context))

fun quietGridDatabase(context: Context): QuietGridDatabase =
    Room.databaseBuilder<QuietGridDatabase>(context, QUIET_GRID_DATABASE_FILE)
        .buildQuietGrid(AndroidSQLiteDriver())
