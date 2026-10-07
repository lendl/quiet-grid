package com.quietgrid.app.games.whodidit

import com.quietgrid.engine.whodidit.WhoDidItCellChange
import com.quietgrid.engine.whodidit.WhoDidItMark
import com.quietgrid.engine.whodidit.WhoDidItPuzzleEntry
import com.quietgrid.engine.whodidit.WhoDidItRef
import com.quietgrid.engine.whodidit.WhoDidItSource
import com.quietgrid.engine.whodidit.WhoDidItStep
import com.quietgrid.engine.whodidit.WhoDidItTechnique
import com.quietgrid.engine.whodidit.paired
import com.quietgrid.engine.whodidit.solveWhoDidIt
import com.quietgrid.engine.whodidit.toSpec
import com.quietgrid.engine.whodidit.whoDidItCategoryPairs
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun easyEntry(): WhoDidItPuzzleEntry = loadWhoDidItBankForTest().first { it.difficulty == "easy" }

private fun isPairedCell(puzzle: WhoDidItPuzzleEntry, block: Int, row: Int, col: Int): Boolean {
    val (rowCategory, colCategory) = whoDidItCategoryPairs(puzzle.categories.size)[block]
    return puzzle.paired(WhoDidItRef(rowCategory, row), WhoDidItRef(colCategory, col))
}

private val LIAR_TECHNIQUES = setOf(WhoDidItTechnique.STATEMENT_CHECK, WhoDidItTechnique.LIAR_HYPOTHESIS)

private fun sampleExperts(): List<WhoDidItPuzzleEntry> {
    val raw = WhoDidItHintTest::class.java.classLoader!!.getResource("whodidit_sample_puzzles.json")!!.readText()
    return Json { ignoreUnknownKeys = true }
        .decodeFromString(ListSerializer(WhoDidItPuzzleEntry.serializer()), raw)
        .filter { it.difficulty == "expert" }
}

private fun sessionAfter(puzzle: WhoDidItPuzzleEntry, steps: List<WhoDidItStep>): WhoDidItSession {
    val marks = steps.flatMap { it.changes }.associate { change ->
        whoDidItCellFor(puzzle.categories.size, change.a, change.b) to
            if (change.mark == WhoDidItMark.YES) WhoDidItCellMark.O else WhoDidItCellMark.X
    }
    val fresh = createWhoDidItSession(puzzle)
    return fresh.copy(
        blocks = fresh.blocks.mapIndexed { b, block ->
            block.mapIndexed { r, row -> row.mapIndexed { c, mark -> marks[Triple(b, r, c)] ?: mark } }
        },
    )
}

private fun solvedSession(puzzle: WhoDidItPuzzleEntry): WhoDidItSession {
    val fresh = createWhoDidItSession(puzzle)
    val blocks = fresh.blocks.mapIndexed { b, block ->
        block.mapIndexed { r, row ->
            row.mapIndexed { c, _ -> if (isPairedCell(puzzle, b, r, c)) WhoDidItCellMark.O else WhoDidItCellMark.EMPTY }
        }
    }
    return fresh.copy(blocks = blocks)
}

private fun WhoDidItSession.withMark(b: Int, r: Int, c: Int, mark: WhoDidItCellMark): WhoDidItSession =
    copy(
        blocks = blocks.mapIndexed { bi, block ->
            if (bi != b) block else block.mapIndexed { ri, row ->
                if (ri != r) row else row.mapIndexed { ci, existing -> if (ci == c) mark else existing }
            }
        },
    )

class WhoDidItHintTest {
    @Test
    fun `hint on a fresh board returns a step with changes`() {
        val hint = computeWhoDidItHint(createWhoDidItSession(easyEntry()))
        assertNotNull(hint.step)
        assertTrue(hint.step!!.changes.isNotEmpty())
        assertFalse(hint.hasWrongMarks)
    }

    @Test
    fun `hint with a wrong mark reports hasWrongMarks and still returns a correct step`() {
        val puzzle = easyEntry()
        val wrongCol = (0 until puzzle.size).first { !isPairedCell(puzzle, 0, 0, it) }
        val session = createWhoDidItSession(puzzle).withMark(0, 0, wrongCol, WhoDidItCellMark.O)
        val hint = computeWhoDidItHint(session)
        assertTrue(hint.hasWrongMarks)
        assertNotNull(hint.step)
        assertTrue(hint.step!!.changes.isNotEmpty())
    }

    @Test
    fun `hint on a solved board returns a null step`() {
        val session = solvedSession(easyEntry())
        assertTrue(isWhoDidItSolved(session))
        val hint = computeWhoDidItHint(session)
        assertNull(hint.step)
        assertFalse(hint.hasWrongMarks)
    }

    @Test
    fun `hint changes agree with the solution`() {
        loadWhoDidItBankForTest().groupBy { it.difficulty }.values.forEach { group ->
            group.take(3).forEach { puzzle ->
                val step = computeWhoDidItHint(createWhoDidItSession(puzzle)).step
                assertNotNull(puzzle.id, step)
                step!!.changes.forEach { change ->
                    val (block, row, col) = whoDidItCellFor(puzzle.categories.size, change.a, change.b)
                    val paired = isPairedCell(puzzle, block, row, col)
                    assertEquals(puzzle.id, change.mark == WhoDidItMark.YES, paired)
                }
            }
        }
    }

    @Test
    fun `expert hint at a liar deduction leads with the statement technique before the cell step`() {
        val experts = sampleExperts()
        assertTrue(experts.isNotEmpty())
        experts.forEach { puzzle ->
            val steps = solveWhoDidIt(puzzle.toSpec()).steps
            val liarIndex = steps.indexOfFirst { it.technique in LIAR_TECHNIQUES }
            assertTrue(puzzle.id, liarIndex >= 0)
            val session = sessionAfter(puzzle, steps.take(liarIndex))
            assertFalse(puzzle.id, hasWhoDidItWrongMarks(session))
            val hint = computeWhoDidItHint(session)
            val lead = hint.lead
            assertNotNull(puzzle.id, lead)
            assertTrue(puzzle.id, lead!!.technique in LIAR_TECHNIQUES)
            assertTrue(puzzle.id, lead.sources.all { it is WhoDidItSource.Statement })
            assertTrue(puzzle.id, lead.changes.isEmpty())
            assertNotNull(puzzle.id, hint.step)
            assertTrue(puzzle.id, hint.step!!.changes.isNotEmpty())
        }
    }

    @Test
    fun `a fresh easy hint has no lead`() {
        assertNull(computeWhoDidItHint(createWhoDidItSession(easyEntry())).lead)
    }

    @Test
    fun `hint cells map yes changes to O and no changes to X in grid coordinates`() {
        val step = WhoDidItStep(
            WhoDidItTechnique.CLUE_DIRECT,
            changes = listOf(
                WhoDidItCellChange(WhoDidItRef(0, 1), WhoDidItRef(2, 0), WhoDidItMark.YES),
                WhoDidItCellChange(WhoDidItRef(1, 2), WhoDidItRef(2, 1), WhoDidItMark.NO),
            ),
        )
        val cells = whoDidItHintCells(3, step)
        assertEquals(
            mapOf(
                whoDidItCellFor(3, WhoDidItRef(0, 1), WhoDidItRef(2, 0)) to WhoDidItCellMark.O,
                whoDidItCellFor(3, WhoDidItRef(1, 2), WhoDidItRef(2, 1)) to WhoDidItCellMark.X,
            ),
            cells,
        )
        assertTrue(whoDidItHintCells(3, null).isEmpty())
    }

    @Test
    fun `hint mark is the shared mark of every change or null when mixed`() {
        val yes = WhoDidItCellChange(WhoDidItRef(0, 0), WhoDidItRef(1, 0), WhoDidItMark.YES)
        val no = WhoDidItCellChange(WhoDidItRef(0, 1), WhoDidItRef(1, 0), WhoDidItMark.NO)
        assertEquals(WhoDidItMark.YES, whoDidItHintMark(WhoDidItStep(WhoDidItTechnique.SINGLE, changes = listOf(yes))))
        assertEquals(WhoDidItMark.NO, whoDidItHintMark(WhoDidItStep(WhoDidItTechnique.SINGLE, changes = listOf(no, no.copy(a = WhoDidItRef(0, 2))))))
        assertNull(whoDidItHintMark(WhoDidItStep(WhoDidItTechnique.SINGLE, changes = listOf(yes, no))))
        assertNull(whoDidItHintMark(WhoDidItStep(WhoDidItTechnique.SINGLE)))
    }
}
