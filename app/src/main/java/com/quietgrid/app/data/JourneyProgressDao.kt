package com.quietgrid.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMilestone(entity: JourneyMilestoneEntity)

    @Query("SELECT * FROM journey_milestone WHERE scope = :scope AND shownMillis IS NULL ORDER BY achievedMillis ASC, chapterIndex ASC")
    suspend fun pendingMilestones(scope: String): List<JourneyMilestoneEntity>

    @Query("UPDATE journey_milestone SET shownMillis = :shownMillis WHERE scope = :scope AND kind = :kind AND chapterIndex = :chapterIndex AND shownMillis IS NULL")
    suspend fun markMilestoneShown(scope: String, kind: String, chapterIndex: Int, shownMillis: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM journey_milestone WHERE scope = :scope AND kind = :kind)")
    fun hasMilestone(scope: String, kind: String): Flow<Boolean>
}
