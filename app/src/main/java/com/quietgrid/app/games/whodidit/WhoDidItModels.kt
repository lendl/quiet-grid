package com.quietgrid.app.games.whodidit

import com.quietgrid.engine.whodidit.WhoDidItPuzzleEntry
import com.quietgrid.engine.whodidit.whoDidItCategoryPairs
import kotlinx.serialization.Serializable

enum class WhoDidItCellMark { EMPTY, X, O, AUTO_X }

enum class WhoDidItStatus { PLAYING, WON }

data class WhoDidItSession(
    val puzzle: WhoDidItPuzzleEntry,
    val blocks: List<List<List<WhoDidItCellMark>>>,
    val status: WhoDidItStatus,
)

@Serializable
data class WhoDidItPersistedSession(
    val puzzle: WhoDidItPuzzleEntry,
    val marks: List<Int>,
    val status: String,
)

fun createWhoDidItSession(puzzle: WhoDidItPuzzleEntry): WhoDidItSession = WhoDidItSession(
    puzzle = puzzle,
    blocks = List(whoDidItCategoryPairs(puzzle.categories.size).size) {
        List(puzzle.size) { List(puzzle.size) { WhoDidItCellMark.EMPTY } }
    },
    status = WhoDidItStatus.PLAYING,
)

fun encodeWhoDidItMarks(blocks: List<List<List<WhoDidItCellMark>>>): List<Int> =
    blocks.flatMap { block -> block.flatMap { row -> row.map { it.ordinal } } }

fun decodeWhoDidItMarks(flat: List<Int>, puzzle: WhoDidItPuzzleEntry): List<List<List<WhoDidItCellMark>>> {
    val size = puzzle.size
    val values = WhoDidItCellMark.entries
    return List(whoDidItCategoryPairs(puzzle.categories.size).size) { block ->
        List(size) { row ->
            List(size) { col ->
                val ordinal = flat.getOrElse((block * size + row) * size + col) { 0 }
                values.getOrElse(ordinal) { WhoDidItCellMark.EMPTY }
            }
        }
    }
}
