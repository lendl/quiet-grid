package com.quietgrid.app.games.blockfill

import com.quietgrid.app.core.Difficulty
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockFillGeneratorTest {

    @Test
    fun `canPlaceTrayInSomeOrder is false when no piece fits anywhere`() {
        var fullBoard = createEmptyBoard()
        for (row in 0 until BLOCKFILL_BOARD_SIZE) {
            for (col in 0 until BLOCKFILL_BOARD_SIZE - 1) fullBoard = placePieceAt(fullBoard, listOf(0 to 0), row, col, BlockFillShapeFamily.SINGLE)
        }
        val straight5 = ALL_SHAPES.first { it.id == "straight5-h" }
        val tray = listOf(shapeDefToPiece(straight5), shapeDefToPiece(straight5), shapeDefToPiece(straight5))
        assertFalse(canPlaceTrayInSomeOrder(fullBoard, tray))
    }

    @Test
    fun `canPlaceTrayInSomeOrder is true on an empty board`() {
        val single = shapeDefToPiece(ALL_SHAPES.first { it.id == "single" })
        assertTrue(canPlaceTrayInSomeOrder(createEmptyBoard(), listOf(single, single, single)))
    }

    private fun piece(id: String) = shapeDefToPiece(ALL_SHAPES.first { it.id == id })

    private fun boardFullExcept(empty: Set<Pair<Int, Int>>): BlockFillBoard {
        var board = createEmptyBoard()
        for (row in 0 until BLOCKFILL_BOARD_SIZE) for (col in 0 until BLOCKFILL_BOARD_SIZE) {
            if (row to col !in empty) board = placePieceAt(board, listOf(0 to 0), row, col, BlockFillShapeFamily.SINGLE)
        }
        return board
    }

    private fun checkerHoles(rows: IntRange, cols: IntRange): Set<Pair<Int, Int>> =
        rows.flatMap { r -> cols.filter { c -> (r + c) % 2 == 0 }.map { r to it } }.toSet()

    @Test
    fun `canPlaceTrayInSomeOrder finds an order where the first piece clears room for the others`() {
        val board = boardFullExcept(checkerHoles(1..7, 0..7) + (0 to 0))
        val tray = listOf(piece("domino-h"), piece("domino-h"), piece("single"))
        assertTrue(canPlaceTrayInSomeOrder(board, tray))
    }

    @Test
    fun `canPlaceTrayInSomeOrder is false when no order makes room`() {
        val board = boardFullExcept(setOf(0 to 0, 0 to 2, 5 to 5))
        val tray = listOf(piece("domino-h"), piece("domino-h"), piece("domino-h"))
        assertFalse(canPlaceTrayInSomeOrder(board, tray))
    }

    @Test
    fun `canPlaceTrayInSomeOrder looks past the first line-clearing spot`() {
        val board = boardFullExcept(checkerHoles(1..7, 0..6) + (0 to 1) + (6 to 7))
        val tray = listOf(piece("single"), piece("straight3-v"), piece("straight3-v"))
        assertTrue(canPlaceTrayInSomeOrder(board, tray))
    }

    @Test
    fun `safeFirstMoveCount counts only first moves that keep the set placeable`() {
        val board = boardFullExcept(checkerHoles(1..7, 0..7) + (0 to 0))
        val tray = listOf(piece("domino-h"), piece("domino-h"), piece("single"))
        assertEquals(1, safeFirstMoveCount(board, tray, cap = 10))
    }

    @Test
    fun `safeFirstMoveCount stops counting at the cap`() {
        val single = piece("single")
        assertEquals(5, safeFirstMoveCount(createEmptyBoard(), listOf(single, single, single), cap = 5))
    }

    @Test
    fun `drawing more candidates never deals a looser set`() {
        for (seed in 1..20) {
            val board = generateClutter(BlockFillClutterStyle.MIRRORED_SHAPES, 12, Random(seed))
            val single = drawTray("expert", board, random = Random(seed), candidateCount = 1).filterNotNull()
            val tightest = drawTray("expert", board, random = Random(seed), candidateCount = 6).filterNotNull()
            assertTrue(
                "seed $seed",
                safeFirstMoveCount(board, tightest, cap = 1000) <= safeFirstMoveCount(board, single, cap = 1000),
            )
        }
    }

    @Test
    fun `harder levels never compare fewer candidate sets`() {
        val counts = Difficulty.entries.map { BLOCKFILL_SET_CANDIDATES.getValue(it.key) }
        assertEquals(counts.sorted(), counts)
        assertEquals(1, BLOCKFILL_SET_CANDIDATES.getValue("easy"))
    }

    private val twoHolesPerLine: BlockFillBoard
        get() = boardFullExcept((0 until BLOCKFILL_BOARD_SIZE).flatMap { r -> listOf(r to r, r to (r + 4) % BLOCKFILL_BOARD_SIZE) }.toSet())

    @Test
    fun `canPlaceAtLeast counts placeable subsets`() {
        val board = twoHolesPerLine
        val twoSingles = listOf(piece("single"), piece("single"), piece("square3x3"))
        assertTrue(canPlaceAtLeast(board, twoSingles, 2))
        assertFalse(canPlaceAtLeast(board, twoSingles, 3))
        val oneSingle = listOf(piece("square3x3"), piece("square3x3"), piece("single"))
        assertTrue(canPlaceAtLeast(board, oneSingle, 1))
        assertFalse(canPlaceAtLeast(board, oneSingle, 2))
    }

    @Test
    fun `drawTray honours a weaker guarantee`() {
        val board = twoHolesPerLine
        val rules = blockFillRulesFor("expert").copy(guarantee = BlockFillGuarantee.AT_LEAST_TWO)
        for (seed in 1..20) {
            val set = drawTray(rules, board, refillRetryCap = 500, random = Random(seed)).filterNotNull()
            assertTrue("seed $seed: ${set.map { it.shapeId }}", canPlaceAtLeast(board, set, 2))
        }
    }

    @Test
    fun `blockFillRulesFor matches the normal game`() {
        assertEquals(
            BlockFillDealRules(SHAPE_WEIGHTS_BY_DIFFICULTY.getValue("expert"), 4, BlockFillGuarantee.ALL_THREE),
            blockFillRulesFor("expert"),
        )
    }

    @Test
    fun `a set may hold at most one heavy piece`() {
        assertFalse(isBalancedSet(listOf(piece("plus"), piece("diagonal-staircase3-a"), piece("single"))))
        assertFalse(isBalancedSet(listOf(piece("square3x3"), piece("plus"), piece("t-down"))))
        assertTrue(isBalancedSet(listOf(piece("plus"), piece("l-0"), piece("single"))))
    }

    @Test
    fun `a set may not hold three pieces of the same type`() {
        assertFalse(isBalancedSet(listOf(piece("single"), piece("single"), piece("single"))))
        assertFalse(isBalancedSet(listOf(piece("l-0"), piece("j-90"), piece("l-180"))))
        assertTrue(isBalancedSet(listOf(piece("single"), piece("single"), piece("domino-h"))))
    }

    @Test
    fun `drawTray only deals balanced sets`() {
        for (difficulty in Difficulty.entries) {
            for (seed in 1..150) {
                val tray = drawTray(difficulty.key, createEmptyBoard(), random = Random(seed)).filterNotNull()
                assertTrue("${difficulty.key} seed $seed: ${tray.map { it.shapeId }}", isBalancedSet(tray))
            }
        }
    }

    @Test
    fun `drawTray hands out sets where all three pieces can be placed`() {
        val holes = (0 until 6).map { row -> row to row }.toSet()
        val bottomRows = (6 until BLOCKFILL_BOARD_SIZE).flatMap { row -> (0 until BLOCKFILL_BOARD_SIZE).map { row to it } }.toSet()
        val board = boardFullExcept(holes + bottomRows)
        for (seed in 1..40) {
            val tray = drawTray("medium", board, random = Random(seed)).filterNotNull()
            assertTrue("seed $seed gave an unplaceable set: ${tray.map { it.shapeId }}", canPlaceTrayInSomeOrder(board, tray))
        }
    }

    @Test
    fun `drawTray always returns 3 pieces`() {
        val tray = drawTray("medium", createEmptyBoard(), random = Random(1))
        assertEquals(3, tray.size)
        assertTrue(tray.all { it != null })
    }

    @Test
    fun `drawTray falls back to a guaranteed single piece when retry budget is exhausted`() {
        val loneHoles = (0 until BLOCKFILL_BOARD_SIZE).map { row -> row to (row * 3) % BLOCKFILL_BOARD_SIZE }.toSet()
        val board = boardFullExcept(loneHoles)
        for (seed in 1..20) {
            val tray = drawTray("expert", board, refillRetryCap = 0, random = Random(seed)).filterNotNull()
            assertEquals("single", tray[0].shapeId)
            assertTrue("seed $seed: ${tray.map { it.shapeId }}", isBalancedSet(tray))
        }
    }

    @Test
    fun `createBlockFillSession starts a playable session`() {
        val session = createBlockFillSession("easy", random = Random(3))
        assertEquals(3, session.tray.size)
        assertEquals(BlockFillStatus.PLAYING, session.status)
        assertEquals(1, session.multiplier)
    }

    @Test
    fun `score target rises with difficulty`() {
        val expected = mapOf(Difficulty.EASY to 1000, Difficulty.MEDIUM to 1500, Difficulty.HARD to 2000, Difficulty.EXPERT to 2500)
        for (difficulty in Difficulty.entries) {
            val session = createBlockFillSession(difficulty.key, random = Random(1))
            assertEquals("unexpected scoreTarget for ${difficulty.key}", expected.getValue(difficulty), session.puzzle.scoreTarget)
        }
    }

    @Test
    fun `starting blocks stay within the configured range for every difficulty`() {
        val expected = mapOf(Difficulty.EASY to 4..8, Difficulty.MEDIUM to 6..10, Difficulty.HARD to 8..12, Difficulty.EXPERT to 10..14)
        for (difficulty in Difficulty.entries) {
            for (seed in 1..30) {
                val blocks = countFilledCells(createBlockFillSession(difficulty.key, random = Random(seed)).board)
                assertTrue("${difficulty.key} seed $seed gave $blocks", blocks in expected.getValue(difficulty))
            }
        }
    }

    @Test
    fun `starting block counts vary between games and include odd counts`() {
        for (difficulty in Difficulty.entries) {
            val counts = (1..200).map { countFilledCells(createBlockFillSession(difficulty.key, random = Random(it)).board) }.toSet()
            assertEquals("${difficulty.key} only produced $counts", BLOCKFILL_STARTING_BLOCKS.getValue(difficulty.key).toSet(), counts)
        }
    }

    @Test
    fun `clutterCountsFor only gives odd counts to the diagonal mirror`() {
        assertEquals(listOf(4, 5, 6, 7, 8), clutterCountsFor(BlockFillClutterStyle.DIAGONAL_MIRROR, 4..8))
        assertEquals(listOf(4, 8), clutterCountsFor(BlockFillClutterStyle.FOUR_WAY, 4..8))
        assertEquals(listOf(4, 6, 8), clutterCountsFor(BlockFillClutterStyle.MIRROR, 4..8))
        assertEquals(listOf(4, 6, 8), clutterCountsFor(BlockFillClutterStyle.ROTATIONAL, 4..8))
        assertEquals(listOf(4, 6, 8), clutterCountsFor(BlockFillClutterStyle.MIRRORED_SHAPES, 4..8))
    }

    @Test
    fun `diagonal mirror clutter matches itself across the diagonal`() {
        for (count in listOf(5, 7, 12)) {
            for (seed in 1..20) {
                val cells = filledCells(generateClutter(BlockFillClutterStyle.DIAGONAL_MIRROR, count, Random(seed)))
                assertEquals(count, cells.size)
                assertTrue("seed $seed count $count not diagonal", cells.all { (r, c) -> c to r in cells })
            }
        }
    }

    @Test
    fun `four-way clutter mirrors both left-right and top-bottom`() {
        for (count in listOf(4, 8, 12)) {
            for (seed in 1..20) {
                val cells = filledCells(generateClutter(BlockFillClutterStyle.FOUR_WAY, count, Random(seed)))
                val last = BLOCKFILL_BOARD_SIZE - 1
                assertEquals(count, cells.size)
                assertTrue("seed $seed count $count not four-way", cells.all { (r, c) -> r to last - c in cells && last - r to c in cells })
            }
        }
    }

    @Test
    fun `starting blocks are stone`() {
        val session = createBlockFillSession("expert", random = Random(9))
        val families = session.board.flatten().filterNotNull().toSet()
        assertEquals(setOf(BlockFillShapeFamily.STONE), families)
    }

    private fun filledCells(board: BlockFillBoard): Set<Pair<Int, Int>> =
        (0 until BLOCKFILL_BOARD_SIZE).flatMap { r -> (0 until BLOCKFILL_BOARD_SIZE).filter { c -> board[r][c] != null }.map { r to it } }.toSet()

    private fun hasFullLine(board: BlockFillBoard): Boolean =
        (0 until BLOCKFILL_BOARD_SIZE).any { r -> board[r].all { it != null } } ||
            (0 until BLOCKFILL_BOARD_SIZE).any { c -> board.all { it[c] != null } }

    @Test
    fun `mirror clutter is mirrored left to right`() {
        for (seed in 1..20) {
            val cells = filledCells(generateClutter(BlockFillClutterStyle.MIRROR, 12, Random(seed)))
            assertEquals(12, cells.size)
            assertTrue("seed $seed not mirrored", cells.all { (r, c) -> r to BLOCKFILL_BOARD_SIZE - 1 - c in cells })
        }
    }

    @Test
    fun `rotational clutter matches itself turned half a circle`() {
        for (seed in 1..20) {
            val cells = filledCells(generateClutter(BlockFillClutterStyle.ROTATIONAL, 12, Random(seed)))
            assertEquals(12, cells.size)
            val last = BLOCKFILL_BOARD_SIZE - 1
            assertTrue("seed $seed not rotational", cells.all { (r, c) -> last - r to last - c in cells })
        }
    }

    @Test
    fun `mirrored-shapes clutter is mirrored and every block touches another`() {
        for (count in listOf(4, 8, 12)) {
            for (seed in 1..20) {
                val cells = filledCells(generateClutter(BlockFillClutterStyle.MIRRORED_SHAPES, count, Random(seed)))
                assertEquals(count, cells.size)
                assertTrue("seed $seed not mirrored", cells.all { (r, c) -> r to BLOCKFILL_BOARD_SIZE - 1 - c in cells })
                val lonely = cells.filter { (r, c) -> listOf(r - 1 to c, r + 1 to c, r to c - 1, r to c + 1).none { it in cells } }
                assertTrue("seed $seed count $count has lonely blocks $lonely", lonely.isEmpty())
            }
        }
    }

    @Test
    fun `clutter never starts with a full row or column`() {
        for (style in BlockFillClutterStyle.entries) {
            for (seed in 1..20) {
                assertFalse("$style seed $seed", hasFullLine(generateClutter(style, 24, Random(seed))))
            }
        }
    }

    @Test
    fun `every clutter style gets picked`() {
        val picked = (0 until 60).map { pickClutterStyle(Random(it)) }.toSet()
        assertEquals(BlockFillClutterStyle.entries.toSet(), picked)
    }

    @Test
    fun `starting block ranges never shift down as difficulty rises`() {
        val ranges = Difficulty.entries.map { BLOCKFILL_STARTING_BLOCKS.getValue(it.key) }
        assertEquals(ranges.map { it.first }.sorted(), ranges.map { it.first })
        assertEquals(ranges.map { it.last }.sorted(), ranges.map { it.last })
    }
}
