package com.quietgrid.app.games.blockfill

import kotlin.random.Random

internal data class BlockFillPlacementOutcome(
    val board: BlockFillBoard,
    val linesCleared: Int,
    val gained: Int,
    val boardEmptied: Boolean,
)

internal fun resolvePlacement(
    board: BlockFillBoard,
    piece: BlockFillPiece,
    anchorRow: Int,
    anchorCol: Int,
    comboStreak: Int,
    multiplier: Int,
): BlockFillPlacementOutcome? {
    if (!canPlacePieceAt(board, piece.cells, anchorRow, anchorCol)) return null
    val (boardAfterClear, linesCleared) = clearFullLines(placePieceAt(board, piece.cells, anchorRow, anchorCol, piece.family))
    val boardEmptied = isBoardEmpty(boardAfterClear)
    val gained = scorePlacement(linesCleared, comboStreak, boardEmptied, multiplier)
    return BlockFillPlacementOutcome(boardAfterClear, linesCleared, gained, boardEmptied)
}

fun applyBlockFillPlacement(
    session: BlockFillSession,
    pieceIndex: Int,
    anchorRow: Int,
    anchorCol: Int,
    random: Random = Random.Default,
): BlockFillSession? {
    if (session.status != BlockFillStatus.PLAYING) return null
    val piece = session.tray.getOrNull(pieceIndex) ?: return null
    val outcome = resolvePlacement(session.board, piece, anchorRow, anchorCol, session.comboStreak, session.multiplier) ?: return null
    val boardAfterClear = outcome.board
    val linesCleared = outcome.linesCleared

    val nextScore = session.score + outcome.gained
    val nextComboStreak = if (linesCleared > 0) session.comboStreak + 1 else 0
    val nextMultiplier = if (outcome.boardEmptied) session.multiplier + 1 else session.multiplier

    val trayAfterRemoval = session.tray.mapIndexed { index, p -> if (index == pieceIndex) null else p }
    val trayIsEmpty = trayAfterRemoval.all { it == null }
    val nextTray = if (trayIsEmpty) {
        drawTray(session.puzzle.difficulty, boardAfterClear, random = random)
    } else {
        trayAfterRemoval
    }

    val status = when {
        nextScore >= session.puzzle.scoreTarget -> BlockFillStatus.WON
        nextTray.any { p -> p != null && pieceFitsAnywhere(boardAfterClear, p.cells) } -> BlockFillStatus.PLAYING
        else -> BlockFillStatus.LOST
    }

    return session.copy(
        board = boardAfterClear,
        tray = nextTray,
        score = nextScore,
        comboStreak = nextComboStreak,
        status = status,
        multiplier = nextMultiplier,
        moves = session.moves + 1,
    )
}
