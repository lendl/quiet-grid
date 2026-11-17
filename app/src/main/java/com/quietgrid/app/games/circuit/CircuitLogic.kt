package com.quietgrid.app.games.circuit

import com.quietgrid.engine.circuit.circuitLitCells
import com.quietgrid.engine.circuit.circuitTurnsBetween
import com.quietgrid.engine.circuit.findCircuitHint
import com.quietgrid.engine.circuit.isCircuitSolved

private const val CIRCUIT_BASE_SCORE = 10_000
private const val CIRCUIT_HINT_PENALTY = 1_000
private const val CIRCUIT_TIME_PENALTY_PER_SECOND = 10

internal fun circuitScore(hintsUsed: Int, elapsedSeconds: Int): Int =
    maxOf(0, CIRCUIT_BASE_SCORE - hintsUsed * CIRCUIT_HINT_PENALTY - elapsedSeconds * CIRCUIT_TIME_PENALTY_PER_SECOND)

sealed interface CircuitTapOutcome {
    data class Changed(val session: CircuitSession) : CircuitTapOutcome
    data object Blocked : CircuitTapOutcome
}

private fun CircuitSession.withStatus(): CircuitSession =
    copy(status = if (isCircuitSolved(board, masks)) CircuitStatus.WON else CircuitStatus.PLAYING)

fun applyCircuitRotate(session: CircuitSession, cell: Int): CircuitTapOutcome? {
    if (session.status != CircuitStatus.PLAYING || cell !in session.turns.indices) return null
    if (session.locked[cell]) return CircuitTapOutcome.Blocked
    val turns = session.turns.toMutableList().also { it[cell] = it[cell] + 1 }
    return CircuitTapOutcome.Changed(
        session.copy(turns = turns, moves = session.moves + 1).withStatus(),
    )
}

fun applyCircuitLockToggle(session: CircuitSession, cell: Int): CircuitSession? {
    if (session.status != CircuitStatus.PLAYING || cell !in session.locked.indices) return null
    val locked = session.locked.toMutableList().also { it[cell] = !it[cell] }
    return session.copy(locked = locked)
}

fun applyCircuitHint(session: CircuitSession): CircuitHintResult? {
    if (session.status != CircuitStatus.PLAYING) return null
    val masks = session.masks
    val hint = findCircuitHint(session.puzzle, masks, session.locked.toBooleanArray()) ?: return null
    val extra = circuitTurnsBetween(masks[hint.cell], hint.mask) ?: return null
    val turns = session.turns.toMutableList().also { it[hint.cell] = it[hint.cell] + extra }
    val locked = session.locked.toMutableList().also { it[hint.cell] = true }
    val next = session.copy(turns = turns, locked = locked, hintsUsed = session.hintsUsed + 1).withStatus()
    return CircuitHintResult(next, hint)
}

fun circuitLit(session: CircuitSession): BooleanArray = circuitLitCells(session.board, session.masks)

fun resolveCircuitTap(rows: Int, cols: Int, tapRow: Float, tapCol: Float): Int? {
    val row = tapRow.toInt()
    val col = tapCol.toInt()
    if (tapRow < 0f || tapCol < 0f || row !in 0 until rows || col !in 0 until cols) return null
    return row * cols + col
}
