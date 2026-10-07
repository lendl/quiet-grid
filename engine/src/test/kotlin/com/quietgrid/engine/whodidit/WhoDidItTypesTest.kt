package com.quietgrid.engine.whodidit

import com.quietgrid.engine.core.Difficulty
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhoDidItTypesTest {
    private val json = """{"id":"whodidit-hard-0007","difficulty":"hard","theme":"castle","case":0,"size":4,
      "categories":["suspect","place","item","motive"],
      "entries":[[3,0,6,1],[2,5,0,6],[4,1,3,0],[0,6,2,5]],
      "solution":[[0,1,2,3],[2,0,3,1],[1,3,0,2],[3,2,1,0]],"culprit":2,
      "clues":[{"t":"SAME","a":[0,1],"b":[1,2]},{"t":"TRAIT","a":[2,0],"trait":"suspect.height.tall"},{"t":"CRIME","e":[1,3]}]}"""
    private val entry = Json.decodeFromString<WhoDidItPuzzleEntry>(json)

    @Test fun `spec JSON example decodes with t discriminator and array refs`() {
        assertEquals(WhoDidItClue.Same(WhoDidItRef(0, 1), WhoDidItRef(1, 2)), entry.clues[0])
        assertEquals(WhoDidItTrait.SUSPECT_TALL, (entry.clues[1] as WhoDidItClue.HasTrait).trait)
        assertTrue(entry.statements.isEmpty())
        assertEquals(entry, Json.decodeFromString<WhoDidItPuzzleEntry>(Json.encodeToString(entry)))
    }

    @Test fun `category pairs follow the staircase order`() {
        assertEquals(listOf(0 to 1, 0 to 2, 2 to 1), whoDidItCategoryPairs(3))
        assertEquals(listOf(0 to 1, 0 to 2, 0 to 3, 3 to 1, 3 to 2, 2 to 1), whoDidItCategoryPairs(4))
    }

    @Test fun `paired uses the suspect-anchored solution`() {
        assertTrue(entry.paired(WhoDidItRef(0, 1), WhoDidItRef(1, 0)))
        assertFalse(entry.paired(WhoDidItRef(0, 1), WhoDidItRef(1, 1)))
        assertTrue(entry.paired(WhoDidItRef(1, 0), WhoDidItRef(2, 3)))
    }

    @Test fun `shapes match the spec`() {
        assertEquals(WhoDidItShape(5, 4), WHODIDIT_SHAPES.getValue(Difficulty.EXPERT))
        assertEquals(WhoDidItShape(3, 3), WHODIDIT_SHAPES.getValue(Difficulty.EASY))
    }
}
