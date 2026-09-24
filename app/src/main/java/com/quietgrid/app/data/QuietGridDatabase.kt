package com.quietgrid.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [PlayRecordEntity::class], version = 3, exportSchema = false)
abstract class QuietGridDatabase : RoomDatabase() {
    abstract fun playHistoryDao(): PlayHistoryDao
}

val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `play_records` ADD COLUMN `dailyDate` TEXT")
        db.execSQL("ALTER TABLE `play_records` ADD COLUMN `shareDetail` TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_records_gameId_dailyDate` ON `play_records` (`gameId`, `dailyDate`)")
    }
}
