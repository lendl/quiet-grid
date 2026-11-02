package com.quietgrid.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [PlayRecordEntity::class, JourneyProgressEntity::class], version = 4, exportSchema = false)
abstract class QuietGridDatabase : RoomDatabase() {
    abstract fun playHistoryDao(): PlayHistoryDao
    abstract fun journeyProgressDao(): JourneyProgressDao
}

val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `play_records` ADD COLUMN `dailyDate` TEXT")
        db.execSQL("ALTER TABLE `play_records` ADD COLUMN `shareDetail` TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_records_gameId_dailyDate` ON `play_records` (`gameId`, `dailyDate`)")
    }
}

val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `journey_progress` (`scope` TEXT NOT NULL, `puzzleId` TEXT NOT NULL, " +
                "`stars` INTEGER NOT NULL, `cleared` INTEGER NOT NULL, `clean` INTEGER NOT NULL, `underTarget` INTEGER NOT NULL, " +
                "`bestSeconds` INTEGER, `skipped` INTEGER NOT NULL, `attempts` INTEGER NOT NULL, `nextPuzzleId` TEXT, " +
                "`lastPlayedMillis` INTEGER NOT NULL, PRIMARY KEY(`scope`, `puzzleId`))",
        )
        db.execSQL("ALTER TABLE `play_records` ADD COLUMN `journeyStop` TEXT")
    }
}
