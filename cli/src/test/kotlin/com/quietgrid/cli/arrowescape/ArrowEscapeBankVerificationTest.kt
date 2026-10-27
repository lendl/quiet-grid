package com.quietgrid.cli.arrowescape

import com.quietgrid.engine.arrowescape.ARROW_DIRECTION_DELTA
import com.quietgrid.engine.arrowescape.ARROW_ESCAPE_MASK_ACTIVE
import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import com.quietgrid.engine.arrowescape.CellCoord
import com.quietgrid.engine.arrowescape.classifyArrowEscapeDifficulty
import com.quietgrid.engine.arrowescape.computeCorridor
import com.quietgrid.engine.arrowescape.isActiveCell
import com.quietgrid.engine.arrowescape.profileArrowEscapePuzzle
import com.quietgrid.engine.arrowescape.solveArrowEscape
import com.quietgrid.engine.arrowescape.toPiece
import com.quietgrid.engine.core.Difficulty
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.ceil

private fun readBank(path: String): List<ArrowEscapePuzzleEntry> = Json { ignoreUnknownKeys = true }
    .decodeFromString(ListSerializer(ArrowEscapePuzzleEntry.serializer()), File(path).readText())

class ArrowEscapeBankVerificationTest {
    private val tierBanks = Difficulty.entries.associateWith { readBank("app/src/main/assets/arrowescape_puzzles_${it.key}.json") }
    private val mainBank = tierBanks.values.flatten()
    private val halloweenBank = readBank("app/src/main/assets/arrowescape_halloween_puzzles.json")
    private val bank = mainBank + halloweenBank
    private val seasonByShape = loadArrowEscapeSilhouettes().associate { it.id to it.season }

    @Test
    fun `every tier has at least 55 puzzles`() {
        tierBanks.forEach { (difficulty, entries) ->
            assertTrue(difficulty.key, entries.size >= 55)
        }
    }

    @Test
    fun `every tier file holds only its own tier`() {
        tierBanks.forEach { (difficulty, entries) ->
            entries.forEach { assertEquals(it.id, difficulty.key, it.difficulty) }
        }
    }

    @Test
    fun `the halloween bank has a halloween shape for every tier and the main bank has none`() {
        Difficulty.entries.forEach { difficulty ->
            assertTrue(difficulty.key, halloweenBank.any { it.difficulty == difficulty.key })
        }
        halloweenBank.forEach { assertEquals(it.id, "halloween", seasonByShape[it.shape]) }
        mainBank.forEach { assertEquals(it.id, null, seasonByShape[it.shape]) }
    }

    @Test
    fun `every expert puzzle has at least the expert arrow minimum`() {
        bank.filter { it.difficulty == Difficulty.EXPERT.key }.forEach { entry ->
            assertTrue("${entry.id} has ${entry.pieces.size} arrows", entry.pieces.size >= ARROW_ESCAPE_SILHOUETTE_EXPERT_MIN_PIECES)
        }
    }

    @Test
    fun `ids and layouts are unique`() {
        assertEquals(bank.size, bank.map { it.id }.toSet().size)
        assertEquals(bank.size, bank.map { entry -> buildPuzzleFingerprint(entry.pieces.map { it.toPiece() }) }.toSet().size)
    }

    @Test
    fun `every square puzzle is sized for its tier and every puzzle is solvable and graded at its tier`() {
        bank.forEach { entry ->
            val pieces = entry.pieces.map { it.toPiece() }
            val tier = Difficulty.entries.first { it.key == entry.difficulty }
            if (entry.mask == null) {
                assertEquals(entry.id, entry.rows, entry.cols)
                assertTrue(entry.id, entry.rows in ARROW_ESCAPE_SIZE_RANGE_BY_DIFFICULTY.getValue(tier))
            }
            assertTrue(entry.id, solveArrowEscape(pieces, entry.rows, entry.cols).solvable)
            assertEquals(entry.id, entry.difficulty, classifyArrowEscapeDifficulty(profileArrowEscapePuzzle(pieces, entry.rows, entry.cols)).key)
        }
    }

    @Test
    fun `shaped puzzles name a bundled silhouette and keep every arrow inside the mask`() {
        val shapeIds = loadArrowEscapeSilhouettes().map { it.id }.toSet()
        bank.filter { it.mask != null }.forEach { entry ->
            assertTrue(entry.id, entry.shape in shapeIds)
            assertEquals(entry.id, entry.rows, entry.mask!!.size)
            assertTrue(entry.id, entry.mask!!.all { it.length == entry.cols })
            assertTrue(entry.id, entry.pieces.all { piece -> piece.cells.all { (row, col) -> entry.isActiveCell(row, col) } })
        }
    }

    @Test
    fun `no arrow points into its own body`() {
        bank.forEach { entry ->
            entry.pieces.map { it.toPiece() }.forEachIndexed { index, piece ->
                val head = piece.cells.last()
                val corridor = computeCorridor(head.row, head.col, piece.headDirection, entry.rows, entry.cols)
                assertTrue("${entry.id} piece $index", corridor.none { it in piece.cells })
            }
        }
    }

    @Test
    fun `every head continues its last segment and the board is covered within tolerance`() {
        bank.forEach { entry ->
            val pieces = entry.pieces.map { it.toPiece() }
            pieces.forEach { piece ->
                val head = piece.cells.last()
                val (dr, dc) = ARROW_DIRECTION_DELTA.getValue(piece.headDirection)
                assertEquals(entry.id, CellCoord(head.row - dr, head.col - dc), piece.cells[piece.cells.size - 2])
            }
            val cellCount = entry.mask?.sumOf { row -> row.count { it == ARROW_ESCAPE_MASK_ACTIVE } } ?: (entry.rows * entry.cols)
            val covered = pieces.sumOf { it.cells.size }
            assertTrue(entry.id, covered >= cellCount - ceil(cellCount * ARROW_ESCAPE_EMPTY_CELL_TOLERANCE).toInt())
        }
    }
}
