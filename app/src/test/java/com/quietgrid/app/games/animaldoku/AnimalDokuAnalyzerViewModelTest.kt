package com.quietgrid.app.games.animaldoku

import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.ui.analyzer.AnalyzerState
import com.quietgrid.engine.animaldoku.AnimalDokuHint
import com.quietgrid.engine.animaldoku.AnimalDokuPuzzleEntry
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private val ANALYZER_PUZZLE = AnimalDokuPuzzleEntry(
    id = "analyzer-test",
    size = 5,
    difficulty = "easy",
    regions = listOf(
        listOf(4, 4, 4, 2, 2),
        listOf(4, 4, 2, 2, 2),
        listOf(4, 4, 2, 2, 0),
        listOf(1, 1, 3, 3, 0),
        listOf(1, 1, 1, 0, 0),
    ),
    solution = listOf(3, 1, 4, 2, 0),
)

private typealias AnimalDokuReady = AnalyzerState.Ready<AnimalDokuPuzzleEntry, List<List<AnimalDokuCellState>>, AnimalDokuHint>

private val AnimalDokuAnalyzerViewModel.ready: AnimalDokuReady
    get() = state as AnimalDokuReady

private fun AnimalDokuSession.withCell(row: Int, col: Int, state: AnimalDokuCellState): AnimalDokuSession =
    copy(cells = cells.mapIndexed { r, line -> line.mapIndexed { c, cell -> if (r == row && c == col) state else cell } })

class AnimalDokuAnalyzerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val json = Json { ignoreUnknownKeys = true }

    private fun snapshotOf(session: AnimalDokuSession) = json.encodeToString(
        AnimalDokuPersistedSession(
            puzzle = session.puzzle,
            cells = session.cells.flatten().map { it.ordinal },
            lives = session.lives,
            status = session.status.name,
        ),
    )

    private fun loadedViewModel(snapshot: String?): AnimalDokuAnalyzerViewModel {
        val viewModel = AnimalDokuAnalyzerViewModel(snapshot)
        viewModel.replayDispatcher = StandardTestDispatcher(mainDispatcherRule.dispatcher.scheduler)
        viewModel.load()
        mainDispatcherRule.dispatcher.scheduler.runCurrent()
        return viewModel
    }

    private fun List<List<AnimalDokuCellState>>.animalCells(): Set<Pair<Int, Int>> =
        flatMapIndexed { row, line -> line.indices.filter { line[it] == AnimalDokuCellState.LOCKED_CORRECT }.map { row to it } }.toSet()

    private val solutionCells = ANALYZER_PUZZLE.solution.mapIndexed { row, col -> row to col }.toSet()

    @Test
    fun `a lost board replays to Ready and ends with every animal placed`() {
        val lost = createAnimalDokuSession(ANALYZER_PUZZLE)
            .withCell(0, 3, AnimalDokuCellState.LOCKED_CORRECT)
            .withCell(1, 0, AnimalDokuCellState.LOCKED_WRONG)
            .withCell(2, 0, AnimalDokuCellState.LOCKED_WRONG)
            .withCell(3, 0, AnimalDokuCellState.LOCKED_WRONG)
            .copy(lives = 0, status = AnimalDokuStatus.LOST)

        val ready = loadedViewModel(snapshotOf(lost)).ready

        assertTrue(ready.replay.steps.isNotEmpty())
        assertEquals(lost.cells, ready.replay.steps.first().boardBefore)
        assertEquals(solutionCells, ready.replay.finalBoard.animalCells())
        assertTrue(ready.replay.steps.none { it.hint is AnimalDokuHint.Reveal || it.hint is AnimalDokuHint.WrongMark })
    }

    @Test
    fun `an empty board is solved by logic alone`() {
        val ready = loadedViewModel(snapshotOf(createAnimalDokuSession(ANALYZER_PUZZLE))).ready

        assertEquals(solutionCells, ready.replay.finalBoard.animalCells())
        assertTrue(ready.replay.steps.all { it.hint is AnimalDokuHint.Deduction })
    }

    @Test
    fun `an X on an animal's cell is dropped before the replay starts`() {
        val session = createAnimalDokuSession(ANALYZER_PUZZLE)
            .withCell(2, 4, AnimalDokuCellState.MARKED)
            .withCell(0, 0, AnimalDokuCellState.MARKED)

        val firstBoard = loadedViewModel(snapshotOf(session)).ready.replay.steps.first().boardBefore

        assertEquals(AnimalDokuCellState.EMPTY, firstBoard[2][4])
        assertEquals(AnimalDokuCellState.MARKED, firstBoard[0][0])
    }

    @Test
    fun `elimination steps add X marks and placement steps add animals`() {
        val ready = loadedViewModel(snapshotOf(createAnimalDokuSession(ANALYZER_PUZZLE))).ready
        val boards = ready.replay.steps.map { it.boardBefore } + listOf(ready.replay.finalBoard)

        ready.replay.steps.forEachIndexed { index, step ->
            val before = boards[index]
            val after = boards[index + 1]
            val changed = before.indices.flatMap { row -> before[row].indices.filter { before[row][it] != after[row][it] }.map { row to it } }
            assertTrue(changed.isNotEmpty())
            changed.forEach { (row, col) ->
                assertEquals(AnimalDokuCellState.EMPTY, before[row][col])
                val expected = if ((row to col) in solutionCells) AnimalDokuCellState.LOCKED_CORRECT else AnimalDokuCellState.MARKED
                assertEquals(expected, after[row][col])
            }
        }
    }

    @Test
    fun `a fully placed board is AlreadySolved`() {
        var session = createAnimalDokuSession(ANALYZER_PUZZLE)
        solutionCells.forEach { (row, col) -> session = session.withCell(row, col, AnimalDokuCellState.LOCKED_CORRECT) }

        assertEquals(AnalyzerState.AlreadySolved, loadedViewModel(snapshotOf(session)).state)
    }

    @Test
    fun `a missing or undecodable snapshot is LoadFailed`() {
        assertEquals(AnalyzerState.LoadFailed, loadedViewModel(null).state)
        assertEquals(AnalyzerState.LoadFailed, loadedViewModel("not valid json").state)
    }
}
