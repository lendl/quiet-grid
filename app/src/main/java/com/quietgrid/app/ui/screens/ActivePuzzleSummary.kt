package com.quietgrid.app.ui.screens

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.formatElapsed
import com.quietgrid.app.data.ActiveSessionEnvelope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

data class ActivePuzzleSummary(
    val gameId: GameId,
    val difficulty: Difficulty?,
    val dimensions: String?,
    val elapsedLabel: String,
)

private val json = Json { ignoreUnknownKeys = true }

/**
 * Reads just enough of the saved session's payload (via generic JSON tree navigation, not a full
 * typed decode) to show a summary chip row on the "continue puzzle" card — mirrors RN's
 * getActivePuzzleDifficulty/getActivePuzzleDimensions, which inspect the same saved-puzzle shape
 * per game.
 */
fun buildActivePuzzleSummary(envelope: ActiveSessionEnvelope): ActivePuzzleSummary? {
    val gameId = GameId.entries.firstOrNull { it.key == envelope.gameId } ?: return null
    val root = runCatching { json.parseToJsonElement(envelope.payload).jsonObject }.getOrNull()
    val puzzleNode = when (gameId) {
        GameId.NONOGRAM -> root?.get("entry")?.jsonObject
        else -> root?.get("puzzle")?.jsonObject
    }
    val difficultyKey = puzzleNode?.get("difficulty")?.jsonPrimitive?.contentOrNull
    val difficulty = difficultyKey?.let { key -> Difficulty.entries.firstOrNull { it.key == key } }
    val dimensions = when (gameId) {
        GameId.TAKUZU -> puzzleNode?.get("size")?.jsonPrimitive?.intOrNull?.let { "${it}x${it}" }
        GameId.CHIMPTEST -> puzzleNode?.get("gridSize")?.jsonPrimitive?.intOrNull?.let { "${it}x${it}" }
        GameId.SUDOKU -> "9x9"
        GameId.MINESWEEPER, GameId.NONOGRAM, GameId.WORDSEARCH -> {
            val rows = puzzleNode?.get("rows")?.jsonPrimitive?.intOrNull
            val cols = puzzleNode?.get("cols")?.jsonPrimitive?.intOrNull
            if (rows != null && cols != null) "${rows}x${cols}" else null
        }
    }
    return ActivePuzzleSummary(gameId, difficulty, dimensions, formatElapsed(envelope.elapsedSeconds.toInt()))
}
