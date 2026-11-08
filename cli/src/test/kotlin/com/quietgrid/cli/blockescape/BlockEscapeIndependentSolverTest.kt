package com.quietgrid.cli.blockescape

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class BlockEscapeIndependentSolverTest {
    private class Piece(val lane: Int, val length: Int, val flat: Boolean)

    private val side = 6

    private fun cellOf(piece: Piece, along: Int): Int =
        if (piece.flat) piece.lane * side + along else along * side + piece.lane

    private fun fewestSlides(pieces: List<Piece>, start: IntArray): Int? {
        fun solved(state: IntArray) = state[0] + pieces[0].length == side
        if (solved(start)) return 0
        val seen = HashSet<String>()
        var frontier = listOf(start)
        seen += keyOf(pieces, start)
        var depth = 0
        while (frontier.isNotEmpty()) {
            depth++
            val next = ArrayList<IntArray>()
            for (state in frontier) {
                val board = IntArray(side * side) { -1 }
                pieces.forEachIndexed { index, piece ->
                    for (step in 0 until piece.length) board[cellOf(piece, state[index] + step)] = index
                }
                pieces.forEachIndexed { index, piece ->
                    for (direction in intArrayOf(-1, 1)) {
                        var along = state[index]
                        while (true) {
                            val probe = if (direction < 0) along - 1 else along + piece.length
                            if (probe !in 0 until side || board[cellOf(piece, probe)] != -1) break
                            along += direction
                            val moved = state.copyOf().also { it[index] = along }
                            if (seen.add(keyOf(pieces, moved))) {
                                if (solved(moved)) return depth
                                next += moved
                            }
                        }
                    }
                }
            }
            frontier = next
        }
        return null
    }

    private fun keyOf(pieces: List<Piece>, state: IntArray): String {
        val cells = CharArray(side * side) { '.' }
        pieces.forEachIndexed { index, piece ->
            for (step in 0 until piece.length) cells[cellOf(piece, state[index] + step)] = 'A' + index
        }
        return String(cells)
    }

    private fun solve(blocks: List<IntArray>): Int? {
        val pieces = blocks.map { (row, col, length, flat) -> Piece(if (flat == 1) row else col, length, flat == 1) }
        val start = IntArray(blocks.size) { if (blocks[it][3] == 1) blocks[it][1] else blocks[it][0] }
        return fewestSlides(pieces, start)
    }

    @Test
    fun `every bank puzzle needs exactly its stored move count`() {
        val bank = Json.parseToJsonElement(File("app/src/main/assets/blockescape_puzzles.json").readText()).jsonArray
        bank.forEach { element ->
            val entry = element.jsonObject
            val blocks = entry.getValue("blocks").jsonArray.map { block ->
                val fields = block.jsonObject
                intArrayOf(
                    fields.getValue("row").jsonPrimitive.int,
                    fields.getValue("col").jsonPrimitive.int,
                    fields.getValue("length").jsonPrimitive.int,
                    if (fields.getValue("horizontal").jsonPrimitive.boolean) 1 else 0,
                )
            }
            val id = entry.getValue("id").jsonPrimitive.content
            assertEquals(id, entry.getValue("minMoves").jsonPrimitive.int, solve(blocks))
        }
    }

    @Test
    fun `the independent solver agrees on a hand-checked board`() {
        val board = listOf(
            intArrayOf(2, 0, 2, 1),
            intArrayOf(1, 2, 2, 0),
            intArrayOf(2, 4, 3, 0),
            intArrayOf(5, 3, 2, 1),
            intArrayOf(0, 1, 3, 1),
            intArrayOf(3, 2, 2, 0),
            intArrayOf(4, 0, 2, 1),
        )
        assertEquals(5, solve(board))
    }
}
