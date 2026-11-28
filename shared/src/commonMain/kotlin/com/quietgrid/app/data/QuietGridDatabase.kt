package com.quietgrid.app.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Database(entities = [PlayRecordEntity::class, JourneyProgressEntity::class, JourneyMilestoneEntity::class], version = 7, exportSchema = false)
@ConstructedBy(QuietGridDatabaseConstructor::class)
abstract class QuietGridDatabase : RoomDatabase() {
    abstract fun playHistoryDao(): PlayHistoryDao
    abstract fun journeyProgressDao(): JourneyProgressDao
}

@Suppress("KotlinNoActualForExpect")
expect object QuietGridDatabaseConstructor : RoomDatabaseConstructor<QuietGridDatabase> {
    override fun initialize(): QuietGridDatabase
}

val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `play_records` ADD COLUMN `dailyDate` TEXT")
        connection.execSQL("ALTER TABLE `play_records` ADD COLUMN `shareDetail` TEXT")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_play_records_gameId_dailyDate` ON `play_records` (`gameId`, `dailyDate`)")
    }
}

val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `journey_progress` (`scope` TEXT NOT NULL, `puzzleId` TEXT NOT NULL, " +
                "`stars` INTEGER NOT NULL, `cleared` INTEGER NOT NULL, `clean` INTEGER NOT NULL, `underTarget` INTEGER NOT NULL, " +
                "`bestSeconds` INTEGER, `skipped` INTEGER NOT NULL, `attempts` INTEGER NOT NULL, `nextPuzzleId` TEXT, " +
                "`lastPlayedMillis` INTEGER NOT NULL, PRIMARY KEY(`scope`, `puzzleId`))",
        )
        connection.execSQL("ALTER TABLE `play_records` ADD COLUMN `journeyStop` TEXT")
    }
}

val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `play_records` ADD COLUMN `journeyStars` INTEGER")
        connection.execSQL("ALTER TABLE `journey_progress` ADD COLUMN `bestScore` INTEGER")
    }
}

val MIGRATION_5_6: Migration = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `journey_milestone` (`scope` TEXT NOT NULL, `kind` TEXT NOT NULL, " +
                "`chapterIndex` INTEGER NOT NULL, `achievedMillis` INTEGER NOT NULL, `shownMillis` INTEGER, " +
                "PRIMARY KEY(`scope`, `kind`, `chapterIndex`))",
        )
    }
}

val MIGRATION_6_7: Migration = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `journey_progress` ADD COLUMN `retiredPuzzleIds` TEXT NOT NULL DEFAULT ''")
    }
}
