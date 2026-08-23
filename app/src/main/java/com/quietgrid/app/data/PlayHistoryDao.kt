package com.quietgrid.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayHistoryDao {
    @Query("SELECT * FROM play_records ORDER BY timestampMillis ASC")
    fun allRecords(): Flow<List<PlayRecordEntity>>

    @Query("SELECT * FROM play_records WHERE gameId = :gameId ORDER BY timestampMillis ASC")
    fun recordsFor(gameId: String): Flow<List<PlayRecordEntity>>

    @Query(
        "SELECT * FROM play_records WHERE gameId = :gameId AND puzzleId = :puzzleId AND difficulty = :difficulty " +
            "ORDER BY timestampMillis ASC",
    )
    fun recordsForPuzzle(gameId: String, puzzleId: String, difficulty: String): Flow<List<PlayRecordEntity>>

    @Insert
    suspend fun insert(record: PlayRecordEntity)

    @Insert
    suspend fun insertAll(records: List<PlayRecordEntity>)

    @Query("DELETE FROM play_records")
    suspend fun clear()
}
