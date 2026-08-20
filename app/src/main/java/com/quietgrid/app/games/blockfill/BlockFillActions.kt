package com.quietgrid.app.games.blockfill

import kotlin.random.Random

fun applyBlockFillPlacement(
    session: BlockFillSession,
    pieceIndex: Int,
    anchorRow: Int,
    anchorCol: Int,
    random: Random = Random.Default,
): BlockFillSession? {
    if (session.status != BlockFillStatus.PLAYING) return null
    val piece = session.tray.getOrNull(pieceIndex) ?: return null
    if (!canPlacePieceAt(session.board, piece.cells, anchorRow, anchorCol)) return null

    val boardAfterPlace = placePieceAt(session.board, piece.cells, anchorRow, anchorCol, piece.family)
    val (boardAfterClear, linesCleared) = clearFullLines(boardAfterPlace)
    val boardEmptiedAfter = isBoardEmpty(boardAfterClear)

    val gained = scorePlacement(linesCleared, session.comboStreak, boardEmptiedAfter)
    val nextScore = session.score + gained
    val nextComboStreak = if (linesCleared > 0) session.comboStreak + 1 else 0

    val trayAfterRemoval = session.tray.mapIndexed { index, p -> if (index == pieceIndex) null else p }
    val trayIsEmpty = trayAfterRemoval.all { it == null }
    val nextTray = if (trayIsEmpty) {
        val config = BLOCKFILL_DIFFICULTY_CONFIG.getValue(session.puzzle.difficulty)
        drawTray(session.puzzle.difficulty, boardAfterClear, config.refillRetryCap, random)
    } else {
        trayAfterRemoval
    }

    if (nextScore >= session.puzzle.scoreTarget) {
        return session.copy(board = boardAfterClear, tray = nextTray, score = nextScore, comboStreak = nextComboStreak, status = BlockFillStatus.WON)
    }

    val hasAnyValidMove = nextTray.any { p -> p != null && findValidPlacements(boardAfterClear, p.cells).isNotEmpty() }
    val status = if (hasAnyValidMove) BlockFillStatus.PLAYING else BlockFillStatus.LOST

    return session.copy(board = boardAfterClear, tray = nextTray, score = nextScore, comboStreak = nextComboStreak, status = status)
}
