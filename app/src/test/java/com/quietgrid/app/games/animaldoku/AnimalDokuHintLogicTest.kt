package com.quietgrid.app.games.animaldoku

import com.quietgrid.app.R
import com.quietgrid.engine.animaldoku.AnimalDokuHint
import com.quietgrid.engine.animaldoku.AnimalDokuPuzzleEntry
import com.quietgrid.engine.animaldoku.AnimalDokuStep
import com.quietgrid.engine.animaldoku.AnimalDokuTechnique
import com.quietgrid.engine.animaldoku.AnimalDokuUnit
import com.quietgrid.engine.animaldoku.AnimalDokuUnitKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val HINT_PUZZLE = AnimalDokuPuzzleEntry(
    id = "hint-test",
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

private fun row(index: Int) = AnimalDokuUnit(AnimalDokuUnitKind.ROW, index)
private fun column(index: Int) = AnimalDokuUnit(AnimalDokuUnitKind.COLUMN, index)
private fun region(index: Int) = AnimalDokuUnit(AnimalDokuUnitKind.REGION, index)

private fun AnimalDokuSession.withCell(row: Int, col: Int, state: AnimalDokuCellState): AnimalDokuSession =
    copy(cells = cells.mapIndexed { r, line -> line.mapIndexed { c, cell -> if (r == row && c == col) state else cell } })

private fun elimination(
    sources: List<AnimalDokuUnit>,
    targets: List<AnimalDokuUnit>,
    technique: AnimalDokuTechnique = AnimalDokuTechnique.CONFINEMENT,
    chainDepth: Int = 0,
) = AnimalDokuHint.Deduction(AnimalDokuStep.Elimination(listOf(0 to 0), technique, sources, targets, chainDepth), setOf(1 to 1))

private val CHAIN_HINT = AnimalDokuHint.Deduction(
    AnimalDokuStep.Elimination(
        listOf(0 to 0),
        AnimalDokuTechnique.CHAIN,
        sources = listOf(region(3)),
        targets = emptyList(),
        chainDepth = 2,
        forced = listOf(AnimalDokuStep.Placement(2, 3, AnimalDokuTechnique.SINGLETON, row(2))),
    ),
    openCells = setOf(3 to 2, 3 to 3),
    chainOpenCells = setOf(2 to 2, 2 to 3, 2 to 4, 3 to 2, 3 to 3),
)

class AnimalDokuHintLogicTest {
    @Test
    fun `a chain has a frame to suppose, one per forced animal, one when stuck and one to conclude`() {
        assertEquals(4, animalDokuChainFrameCount(CHAIN_HINT))
        assertEquals(0, animalDokuChainFrameCount(elimination(listOf(region(0)), listOf(row(3)))))
        assertEquals(0, animalDokuChainFrameCount(AnimalDokuHint.Reveal(1, 1)))
    }

    @Test
    fun `chain frame copy names the unit each frame is about`() {
        assertEquals(
            AnimalDokuHintCopy(R.string.animaldoku_chain_suppose_title, R.string.animaldoku_chain_suppose_body),
            animalDokuChainFrameCopy(CHAIN_HINT, 0),
        )
        assertEquals(
            AnimalDokuHintCopy(R.string.animaldoku_chain_forced_title, R.string.animaldoku_chain_forced_body, listOf(listOf(row(2)))),
            animalDokuChainFrameCopy(CHAIN_HINT, 1),
        )
        assertEquals(
            AnimalDokuHintCopy(R.string.animaldoku_chain_stuck_title, R.string.animaldoku_chain_stuck_body, listOf(listOf(region(3)))),
            animalDokuChainFrameCopy(CHAIN_HINT, 2),
        )
        assertEquals(
            AnimalDokuHintCopy(R.string.animaldoku_chain_conclusion_title, R.string.animaldoku_chain_conclusion_body),
            animalDokuChainFrameCopy(CHAIN_HINT, 3),
        )
    }

    @Test
    fun `later forced animals get their own title`() {
        val longer = CHAIN_HINT.copy(
            step = (CHAIN_HINT.step as AnimalDokuStep.Elimination).let {
                it.copy(forced = it.forced + AnimalDokuStep.Placement(4, 4, AnimalDokuTechnique.SINGLETON, column(4)), chainDepth = 3)
            },
        )
        assertEquals(R.string.animaldoku_chain_forced_title, animalDokuChainFrameCopy(longer, 1).titleRes)
        assertEquals(R.string.animaldoku_chain_forced_again_title, animalDokuChainFrameCopy(longer, 2).titleRes)
    }

    @Test
    fun `the suppose frame shows a numbered faded animal on the tried cell`() {
        val highlight = animalDokuChainFrameHighlight(CHAIN_HINT, HINT_PUZZLE.regions, 0)
        assertEquals(listOf(0 to 0), highlight.animalCells)
        assertTrue(highlight.numberedAnimals)
        assertTrue(highlight.crossCells.isEmpty())
        assertEquals(AnimalDokuHintTone.FOCUS, highlight.toneOf(0 to 0))
        assertEquals(AnimalDokuHintTone.CONTEXT, highlight.toneOf(2 to 1))
        assertEquals(AnimalDokuHintTone.DIMMED, highlight.toneOf(4 to 4))
    }

    @Test
    fun `a forced frame crosses the open cells the animals block and adds the forced animal`() {
        val highlight = animalDokuChainFrameHighlight(CHAIN_HINT, HINT_PUZZLE.regions, 1)
        assertEquals(listOf(0 to 0, 2 to 3), highlight.animalCells)
        assertEquals(setOf(2 to 2, 2 to 4), highlight.crossCells)
        assertEquals(AnimalDokuHintTone.FOCUS, highlight.toneOf(2 to 2))
        assertEquals(AnimalDokuHintTone.FOCUS, highlight.toneOf(0 to 0))
        assertEquals(AnimalDokuHintTone.CONTEXT, highlight.toneOf(2 to 0))
        assertEquals(AnimalDokuHintTone.DIMMED, highlight.toneOf(3 to 2))
    }

    @Test
    fun `the stuck frame crosses every open cell of the unit left without room`() {
        val highlight = animalDokuChainFrameHighlight(CHAIN_HINT, HINT_PUZZLE.regions, 2)
        assertEquals(listOf(0 to 0, 2 to 3), highlight.animalCells)
        assertEquals(setOf(3 to 2, 3 to 3), highlight.crossCells)
        assertEquals(AnimalDokuHintTone.FOCUS, highlight.toneOf(3 to 3))
    }

    @Test
    fun `the conclusion frame drops the faded animals and crosses the tried cell`() {
        val highlight = animalDokuChainFrameHighlight(CHAIN_HINT, HINT_PUZZLE.regions, 3)
        assertTrue(highlight.animalCells.isEmpty())
        assertEquals(setOf(0 to 0), highlight.crossCells)
    }

    @Test
    fun `the chain summary shows a faded animal on the tried cell instead of an X`() {
        val highlight = animalDokuHintHighlight(CHAIN_HINT, HINT_PUZZLE.regions)
        assertEquals(listOf(0 to 0), highlight.animalCells)
        assertTrue(highlight.crossCells.isEmpty())
        assertEquals(AnimalDokuHintTone.FOCUS, highlight.toneOf(3 to 2))
        assertEquals(AnimalDokuHintTone.FOCUS, highlight.toneOf(0 to 0))
    }

    @Test
    fun `an X on an animal's cell becomes a wrong mark hint`() {
        val session = createAnimalDokuSession(HINT_PUZZLE).withCell(2, 4, AnimalDokuCellState.MARKED)
        assertEquals(AnimalDokuHint.WrongMark(2, 4), findAnimalDokuSessionHint(session))
    }

    @Test
    fun `locked animals count as placed and locked wrong cells as X marks`() {
        val session = createAnimalDokuSession(HINT_PUZZLE)
            .withCell(0, 3, AnimalDokuCellState.LOCKED_CORRECT)
            .withCell(1, 0, AnimalDokuCellState.LOCKED_WRONG)
        val hint = findAnimalDokuSessionHint(session) as AnimalDokuHint.Deduction
        val blocked = (0 until 5).map { 0 to it } + (0 until 5).map { it to 3 } + listOf(1 to 0, 1 to 2, 1 to 4, 2 to 2)
        assertTrue(hint.openCells.isNotEmpty())
        assertTrue(hint.openCells.none { it in blocked })
    }

    @Test
    fun `no hint once the puzzle is over`() {
        val session = createAnimalDokuSession(HINT_PUZZLE).copy(status = AnimalDokuStatus.WON)
        assertNull(findAnimalDokuSessionHint(session))
    }

    @Test
    fun `elimination highlight keeps open and cross cells in focus and named units as context`() {
        val hint = AnimalDokuHint.Deduction(
            AnimalDokuStep.Elimination(listOf(3 to 2, 3 to 3), AnimalDokuTechnique.CONFINEMENT, listOf(region(0)), listOf(row(3))),
            setOf(3 to 4),
        )
        val highlight = animalDokuHintHighlight(hint, HINT_PUZZLE.regions)
        assertEquals(AnimalDokuHintTone.FOCUS, highlight.toneOf(3 to 4))
        assertEquals(AnimalDokuHintTone.FOCUS, highlight.toneOf(3 to 2))
        assertEquals(AnimalDokuHintTone.CONTEXT, highlight.toneOf(4 to 4))
        assertEquals(AnimalDokuHintTone.CONTEXT, highlight.toneOf(3 to 0))
        assertEquals(AnimalDokuHintTone.DIMMED, highlight.toneOf(0 to 0))
        assertEquals(setOf(3 to 2, 3 to 3), highlight.crossCells)
    }

    @Test
    fun `placement highlight shows the animal in its unit`() {
        val hint = AnimalDokuHint.Deduction(AnimalDokuStep.Placement(2, 4, AnimalDokuTechnique.SINGLETON, column(4)), setOf(2 to 4))
        val highlight = animalDokuHintHighlight(hint, HINT_PUZZLE.regions)
        assertEquals(listOf(2 to 4), highlight.animalCells)
        assertEquals(AnimalDokuHintTone.FOCUS, highlight.toneOf(2 to 4))
        assertEquals(AnimalDokuHintTone.CONTEXT, highlight.toneOf(0 to 4))
        assertEquals(AnimalDokuHintTone.DIMMED, highlight.toneOf(2 to 3))
    }

    @Test
    fun `wrong mark and reveal highlights focus only their cell`() {
        val wrong = animalDokuHintHighlight(AnimalDokuHint.WrongMark(2, 4), HINT_PUZZLE.regions)
        assertEquals(AnimalDokuHintTone.FOCUS, wrong.toneOf(2 to 4))
        assertEquals(AnimalDokuHintTone.DIMMED, wrong.toneOf(2 to 3))
        val reveal = animalDokuHintHighlight(AnimalDokuHint.Reveal(1, 1), HINT_PUZZLE.regions)
        assertEquals(listOf(1 to 1), reveal.animalCells)
        assertEquals(AnimalDokuHintTone.DIMMED, reveal.toneOf(0 to 0))
    }

    @Test
    fun `placement copy names its unit`() {
        val hint = AnimalDokuHint.Deduction(AnimalDokuStep.Placement(2, 4, AnimalDokuTechnique.SINGLETON, column(4)), setOf(2 to 4))
        assertEquals(
            AnimalDokuHintCopy(R.string.animaldoku_technique_last_spot, R.string.animaldoku_hint_last_spot_body, listOf(listOf(column(4)))),
            animalDokuHintCopy(hint),
        )
    }

    @Test
    fun `elimination copy picks the sentence for the source and target shape`() {
        assertEquals(R.string.animaldoku_hint_confined_to_line_body, animalDokuHintCopy(elimination(listOf(region(0)), listOf(row(3)))).bodyRes)
        assertEquals(R.string.animaldoku_hint_regions_fill_lines_body, animalDokuHintCopy(elimination(listOf(region(0), region(3)), listOf(row(2), row(3)))).bodyRes)
        assertEquals(R.string.animaldoku_hint_line_confined_to_region_body, animalDokuHintCopy(elimination(listOf(row(3)), listOf(region(3)))).bodyRes)
        assertEquals(R.string.animaldoku_hint_lines_fill_regions_body, animalDokuHintCopy(elimination(listOf(column(0), column(1)), listOf(region(1), region(4)))).bodyRes)
        assertEquals(listOf(listOf(row(3)), listOf(region(3))), animalDokuHintCopy(elimination(listOf(row(3)), listOf(region(3)))).unitArgs)
    }

    @Test
    fun `chain copy names the unit left without room and mentions forced animals only for longer chains`() {
        val short = animalDokuHintCopy(elimination(listOf(row(4)), emptyList(), AnimalDokuTechnique.CHAIN, chainDepth = 1))
        assertEquals(R.string.animaldoku_technique_chain, short.titleRes)
        assertEquals(R.string.animaldoku_hint_chain_body, short.bodyRes)
        assertEquals(listOf(listOf(row(4))), short.unitArgs)
        val long = animalDokuHintCopy(elimination(listOf(region(2)), emptyList(), AnimalDokuTechnique.CHAIN, chainDepth = 3))
        assertEquals(R.string.animaldoku_hint_chain_long_body, long.bodyRes)
    }

    @Test
    fun `pairing sizes get their own titles`() {
        assertEquals(R.string.animaldoku_technique_pairing, animalDokuHintCopy(elimination(listOf(region(0), region(3)), listOf(row(2), row(3)), AnimalDokuTechnique.PAIRING_2)).titleRes)
        assertEquals(R.string.animaldoku_technique_triple, animalDokuHintCopy(elimination(listOf(region(0), region(3)), listOf(row(2), row(3)), AnimalDokuTechnique.PAIRING_3)).titleRes)
        assertEquals(R.string.animaldoku_technique_group, animalDokuHintCopy(elimination(listOf(region(0), region(3)), listOf(row(2), row(3)), AnimalDokuTechnique.PAIRING_4_PLUS)).titleRes)
    }
}
