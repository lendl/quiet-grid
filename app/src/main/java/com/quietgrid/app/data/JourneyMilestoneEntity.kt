package com.quietgrid.app.data

import androidx.room.Entity

const val JOURNEY_MILESTONE_CHAPTER = "chapter"
const val JOURNEY_MILESTONE_JOURNEY = "journey"
const val JOURNEY_MILESTONE_MASTERED = "mastered"

@Entity(tableName = "journey_milestone", primaryKeys = ["scope", "kind", "chapterIndex"])
data class JourneyMilestoneEntity(
    val scope: String,
    val kind: String,
    val chapterIndex: Int,
    val achievedMillis: Long,
    val shownMillis: Long?,
)
