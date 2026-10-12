package com.quietgrid.engine.sudoku

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniSudokuTest {
    private val solved6: List<List<Int>> = listOf(
        listOf(1, 2, 3, 4, 5, 6),
        listOf(4, 5, 6, 1, 2, 3),
        listOf(2, 3, 1, 5, 6, 4),
        listOf(5, 6, 4, 2, 3, 1),
        listOf(3, 1, 2, 6, 4, 5),
        listOf(6, 4, 5, 3, 1, 2),
    )

    private val solved4: List<List<Int>> = listOf(
        listOf(1, 2, 3, 4),
        listOf(3, 4, 1, 2),
        listOf(2, 1, 4, 3),
        listOf(4, 3, 2, 1),
    )

    private fun blank(grid: List<List<Int>>, vararg cells: Pair<Int, Int>): SudokuGrid =
        grid.mapIndexed { r, row -> row.mapIndexed { c, v -> if (r to c in cells) null else v } }

    @Test
    fun `six by six layout uses two by three boxes`() {
        val layout = SudokuLayout.MINI_6
        assertEquals(0, layout.boxOf(1, 2))
        assertEquals(1, layout.boxOf(0, 3))
        assertEquals(2, layout.boxOf(2, 0))
        assertEquals(5, layout.boxOf(5, 5))
        assertEquals(listOf(0, 1, 2, 6, 7, 8), layout.boxCellIndexes[0])
        assertEquals(12, layout.cellPeers[0].size)
        assertEquals(0b111111, layout.fullMask)
    }

    @Test
    fun `four by four layout uses two by two boxes`() {
        val layout = SudokuLayout.MINI_4
        assertEquals(3, layout.boxOf(3, 3))
        assertEquals(listOf(10, 11, 14, 15), layout.boxCellIndexes[3])
        assertEquals(7, layout.cellPeers[5].size)
    }

    @Test
    fun `bitmask state for a six by six board never offers digits above six`() {
        val state = createBitmaskStateFromBoard(blank(solved6, 0 to 0, 0 to 1))
        assertEquals(SudokuLayout.MINI_6, state.layout)
        assertEquals(listOf(1), getCellCandidates(state, 0, 0))
        assertEquals(listOf(2), getCellCandidates(state, 0, 1))
        assertEquals(2, state.unresolvedCount)
    }

    @Test
    fun `traceHumanSolve finishes a sparse four by four puzzle`() {
        val puzzle: SudokuGrid = listOf(
            listOf(1, 3, null, null),
            listOf(null, null, null, 1),
            listOf(null, 4, null, 3),
            listOf(null, 1, null, 2),
        )
        val trace = traceHumanSolve(puzzle)
        assertTrue(trace.solved)
        assertEquals(9, trace.moves.count { it is SudokuPlacementMove })
    }

    @Test
    fun `findNextMove on a six by six board places inside the grid`() {
        val move = findNextMove(blank(solved6, 2 to 4)) as SudokuPlacementMove
        assertEquals(2, move.targetRow)
        assertEquals(4, move.targetCol)
        assertEquals(6, move.digit)
        assertTrue(move.houses.contains(SudokuHouseRef("box", 3)))
    }

    private fun grid(cells: String): SudokuGrid {
        val size = if (cells.length == 16) 4 else 6
        return cells.chunked(size).map { row -> row.map { if (it == '.') null else it.digitToInt() } }
    }

    @Test
    fun `classifyMiniSudoku puts four by four grids at easy`() {
        val puzzle = blank(solved4, 0 to 0, 0 to 1, 0 to 2, 1 to 1, 1 to 3, 2 to 0, 2 to 2, 3 to 1, 3 to 3, 3 to 0)
        assertEquals(Difficulty.EASY, classifyMiniSudoku(puzzle))
        assertEquals(Difficulty.EASY, classifyMiniSudoku(grid("13.....1.4.3.1.2")))
    }

    @Test
    fun `naked singles only on six by six is medium`() {
        val grade = gradeMiniSudoku(grid(".3.12.6.1.3..6.4....3..135..1...6.4."))!!
        assertEquals(SudokuSkillStep.NAKED_SINGLES, grade.requiredStep)
        assertEquals(Difficulty.MEDIUM, grade.difficulty)
    }

    @Test
    fun `two hidden singles on six by six is hard`() {
        val grade = gradeMiniSudoku(grid(".54.3....4..61.......1..2...5......2"))!!
        assertEquals(SudokuSkillStep.HIDDEN_SINGLES, grade.requiredStep)
        assertEquals(2, grade.genuineUses)
        assertEquals(Difficulty.HARD, grade.difficulty)
    }

    @Test
    fun `one forced hidden single on six by six has no tier`() {
        val grade = gradeMiniSudoku(grid(".6..4.524.311.......23....6..5..5.6."))!!
        assertEquals(SudokuSkillStep.HIDDEN_SINGLES, grade.requiredStep)
        assertEquals(1, grade.genuineUses)
        assertNull(grade.difficulty)
    }

    @Test
    fun `a chain or coloring step on six by six is expert`() {
        val grade = gradeMiniSudoku(grid("..5...26.51.....5.6....1.32...1...3."))!!
        assertEquals(SudokuSkillStep.CHAINS_AND_COLORING, grade.requiredStep)
        assertEquals(Difficulty.EXPERT, grade.difficulty)
    }

    @Test
    fun `a single fish or wing stuck point on six by six has no tier`() {
        val grade = gradeMiniSudoku(grid(".1.6....3.5..3...2..4.3..4..6....2.."))!!
        assertEquals(SudokuSkillStep.FISH_AND_WINGS, grade.requiredStep)
        assertEquals(1, grade.genuineUses)
        assertNull(grade.difficulty)
    }

    @Test
    fun `classic nine by nine grids get no mini tier`() {
        assertNull(classifyMiniSudoku(List(9) { row -> List(9) { col -> (row * 3 + row / 3 + col) % 9 + 1 } }))
    }
}
