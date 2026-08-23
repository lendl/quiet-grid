package com.quietgrid.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PlayRecordEntity::class], version = 1, exportSchema = false)
abstract class QuietGridDatabase : RoomDatabase() {
    abstract fun playHistoryDao(): PlayHistoryDao
}
