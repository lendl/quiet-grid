package com.quietgrid.app.data

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
    journeyStop = journeyStop,
    journeyStars = journeyStars,
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
    journeyStop = journeyStop,
    journeyStars = journeyStars,
)
