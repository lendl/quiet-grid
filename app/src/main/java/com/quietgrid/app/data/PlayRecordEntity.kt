package com.quietgrid.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "play_records",
    indices = [
        Index("gameId"),
        Index(value = ["gameId", "difficulty"]),
        Index(value = ["gameId", "puzzleId", "difficulty"]),
        Index(value = ["gameId", "dailyDate"]),
    ],
)
data class PlayRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: String,
    val difficulty: String,
    val puzzleId: String?,
    val solved: Boolean,
    val score: Int,
    val elapsedSeconds: Int,
    val timestampMillis: Long,
    val lossReason: String?,
    val isChallenger: Boolean = false,
    val puzzlesSolved: Int? = null,
    val dailyDate: String? = null,
    val shareDetail: String? = null,
)

fun PlayRecord.toEntity(): PlayRecordEntity = PlayRecordEntity(
    gameId = gameId,
    difficulty = difficulty,
    puzzleId = puzzleId,
    solved = solved,
    score = score,
    elapsedSeconds = elapsedSeconds,
    timestampMillis = timestampMillis,
    lossReason = lossReason,
    isChallenger = isChallenger,
    puzzlesSolved = puzzlesSolved,
    dailyDate = dailyDate,
    shareDetail = shareDetail,
)

fun PlayRecordEntity.toRecord(): PlayRecord = PlayRecord(
    gameId = gameId,
    difficulty = difficulty,
    puzzleId = puzzleId,
    solved = solved,
    score = score,
    elapsedSeconds = elapsedSeconds,
    timestampMillis = timestampMillis,
    lossReason = lossReason,
    isChallenger = isChallenger,
    puzzlesSolved = puzzlesSolved,
    dailyDate = dailyDate,
    shareDetail = shareDetail,
)
