package com.quietgrid.engine.arrowescape

import com.quietgrid.engine.journey.GridTransform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrowEscapeTransformTest {
    private val entry = ArrowEscapePuzzleEntry(
        id = "ae-x",
        difficulty = "easy",
        rows = 3,
        cols = 4,
        pieces = listOf(
            ArrowEscapePieceData(listOf(listOf(0, 0), listOf(0, 1)), "right"),
            ArrowEscapePieceData(listOf(listOf(1, 1), listOf(2, 1)), "down"),
            ArrowEscapePieceData(listOf(listOf(1, 3), listOf(1, 2)), "left"),
            ArrowEscapePieceData(listOf(listOf(2, 3), listOf(1, 3)), "up"),
        ),
        shape = "test",
        mask = listOf("##..", "####", ".###"),
    )

    @Test
    fun `mirror swaps left and right and keeps up and down`() {
        val mirrored = entry.transformed(GridTransform.MIRROR_H)
        assertEquals(listOf("left", "down", "right", "up"), mirrored.pieces.map { it.headDirection })
        assertEquals(listOf(listOf(0, 3), listOf(0, 2)), mirrored.pieces[0].cells)
        assertEquals(entry.id, mirrored.id)
        assertEquals(3 to 4, mirrored.rows to mirrored.cols)
    }

    @Test
    fun `mirror reverses each mask row and keeps cells on active mask cells`() {
        val mirrored = entry.transformed(GridTransform.MIRROR_H)
        assertEquals(listOf("..##", "####", "###."), mirrored.mask)
        mirrored.pieces.flatMap { it.cells }.forEach { (r, c) -> assertTrue("$r,$c", mirrored.isActiveCell(r, c)) }
    }

    @Test
    fun `mirrored board keeps its raw score`() {
        assertEquals(arrowEscapeRawScore(entry), arrowEscapeRawScore(entry.transformed(GridTransform.MIRROR_H)))
    }

    @Test
    fun `rotate 90 turns heads with the cells`() {
        val rotated = entry.transformed(GridTransform.ROTATE_90)
        assertEquals(4 to 3, rotated.rows to rotated.cols)
        assertEquals(listOf("down", "left", "up", "right"), rotated.pieces.map { it.headDirection })
        assertEquals(4, rotated.mask!!.size)
        rotated.pieces.flatMap { it.cells }.forEach { (r, c) -> assertTrue("$r,$c", rotated.isActiveCell(r, c)) }
    }

    @Test
    fun `silhouette boards replay mirrored only`() {
        assertEquals(listOf(GridTransform.MIRROR_H), arrowEscapeReplayCandidates(entry))
    }
}
