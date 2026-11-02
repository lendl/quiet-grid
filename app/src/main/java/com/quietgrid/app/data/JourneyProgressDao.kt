package com.quietgrid.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface JourneyProgressDao {
    @Query("SELECT * FROM journey_progress WHERE scope = :scope ORDER BY puzzleId ASC")
    fun progressFor(scope: String): Flow<List<JourneyProgressEntity>>

    @Query("SELECT * FROM journey_progress WHERE scope = :scope AND puzzleId = :puzzleId")
    suspend fun get(scope: String, puzzleId: String): JourneyProgressEntity?

    @Upsert
    suspend fun upsert(entity: JourneyProgressEntity)

    @Query("DELETE FROM journey_progress WHERE scope = :scope AND puzzleId = :puzzleId")
    suspend fun delete(scope: String, puzzleId: String)
}
