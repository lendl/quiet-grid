package com.quietgrid.app.data

import androidx.room.Entity

@Entity(tableName = "journey_progress", primaryKeys = ["scope", "puzzleId"])
data class JourneyProgressEntity(
    val scope: String,
    val puzzleId: String,
    val stars: Int,
    val cleared: Boolean,
    val clean: Boolean,
    val underTarget: Boolean,
    val bestSeconds: Int?,
    val skipped: Boolean,
    val attempts: Int,
    val nextPuzzleId: String?,
    val lastPlayedMillis: Long,
)
