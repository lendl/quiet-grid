package com.quietgrid.app.games.whodidit

import android.content.Context
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.quietgrid.app.R
import com.quietgrid.engine.whodidit.WHODIDIT_POOL_SIZE
import com.quietgrid.engine.whodidit.WHODIDIT_THEMES
import com.quietgrid.engine.whodidit.WhoDidItCategory
import com.quietgrid.engine.whodidit.WhoDidItCellChange
import com.quietgrid.engine.whodidit.WhoDidItClue
import com.quietgrid.engine.whodidit.WhoDidItMark
import com.quietgrid.engine.whodidit.WhoDidItPuzzleEntry
import com.quietgrid.engine.whodidit.WhoDidItRef
import com.quietgrid.engine.whodidit.WhoDidItSource
import com.quietgrid.engine.whodidit.WhoDidItStatement
import com.quietgrid.engine.whodidit.WhoDidItStep
import com.quietgrid.engine.whodidit.WhoDidItTechnique
import com.quietgrid.engine.whodidit.WhoDidItTrait
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WhoDidItTextTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val res: Resources = ApplicationProvider.getApplicationContext<Context>().resources

    private fun puzzle(themeId: String, categoryCount: Int = 4): WhoDidItPuzzleEntry {
        val categories = WhoDidItCategory.entries.take(categoryCount)
        val entries = categories.map { listOf(0, 1, 2, 3) }
        val clues = listOf(
            WhoDidItClue.Same(WhoDidItRef(0, 1), WhoDidItRef(1, 2)),
            WhoDidItClue.NotSame(WhoDidItRef(0, 0), WhoDidItRef(2, 3)),
            WhoDidItClue.NotSame(WhoDidItRef(1, 0), WhoDidItRef(2, 1)),
            WhoDidItClue.Either(WhoDidItRef(0, 2), WhoDidItRef(1, 0), WhoDidItRef(1, 3)),
            WhoDidItClue.Either(WhoDidItRef(1, 1), WhoDidItRef(2, 0), WhoDidItRef(2, 2)),
            WhoDidItClue.HasTrait(WhoDidItRef(0, 3), WhoDidItTrait.SUSPECT_GLASSES),
            WhoDidItClue.LacksTrait(WhoDidItRef(1, 3), WhoDidItTrait.ITEM_WOOD),
            WhoDidItClue.Crime(WhoDidItRef(1, 2)),
            WhoDidItClue.Crime(WhoDidItRef(2, 1)),
            WhoDidItClue.CrimeTrait(WhoDidItTrait.SUSPECT_TALL),
        ) + if (categoryCount == 4) listOf(
            WhoDidItClue.Same(WhoDidItRef(0, 0), WhoDidItRef(3, 1)),
            WhoDidItClue.Crime(WhoDidItRef(3, 2)),
        ) else emptyList()
        return WhoDidItPuzzleEntry(
            id = "t",
            difficulty = "hard",
            theme = themeId,
            case = 0,
            size = 4,
            categories = categories,
            entries = entries,
            solution = categories.map { listOf(0, 1, 2, 3) },
            culprit = 2,
            clues = clues,
            statements = listOf(
                WhoDidItStatement(1, WhoDidItClue.Same(WhoDidItRef(0, 1), WhoDidItRef(1, 2))),
                WhoDidItStatement(0, WhoDidItClue.NotSame(WhoDidItRef(0, 3), WhoDidItRef(1, 1))),
                WhoDidItStatement(2, WhoDidItClue.LacksTrait(WhoDidItRef(0, 2), WhoDidItTrait.SUSPECT_HAT)),
            ),
        )
    }

    private fun assertSentence(text: String) {
        assertFalse(text, text.contains('%'))
        assertTrue(text.isNotBlank())
        assertTrue(text, text.first().isUpperCase())
        assertTrue(text, text.endsWith("."))
    }

    @Test
    fun `every engine theme has resources with matching pool sizes and case count`() {
        WHODIDIT_THEMES.forEach { theme ->
            val r = WHODIDIT_THEME_RES.getValue(theme.id)
            listOf(r.suspects, r.places, r.placePreps, r.items, r.motives).forEach {
                assertEquals(theme.id, WHODIDIT_POOL_SIZE, res.getStringArray(it).size)
            }
            res.getStringArray(r.placePreps).forEach { assertTrue("${theme.id}: $it", it in setOf("in", "on", "at")) }
            assertEquals(theme.caseCount, r.cases.size)
        }
    }

    @Test
    fun `every pool name is at most 10 characters`() {
        WHODIDIT_THEMES.forEach { theme ->
            val r = WHODIDIT_THEME_RES.getValue(theme.id)
            listOf(r.suspects, r.places, r.items, r.motives).forEach { array ->
                res.getStringArray(array).forEach { assertTrue("${theme.id}: $it", it.length <= 10) }
            }
        }
    }

    @Test
    fun `every trait has resources`() {
        assertEquals(WhoDidItTrait.entries.toSet(), WHODIDIT_TRAIT_RES.keys)
    }

    @Test
    fun `every clue type renders without placeholders for every theme`() {
        WHODIDIT_THEMES.forEach { theme ->
            val p = puzzle(theme.id)
            val text = WhoDidItText(res, p)
            assertTrue(text.themeTitle().isNotBlank())
            assertTrue(text.caseTitle().isNotBlank())
            assertTrue(text.caseQuestion().isNotBlank())
            p.clues.indices.forEach { assertSentence(text.clue(it)) }
            p.statements.indices.forEach {
                val s = text.statement(it)
                assertFalse(s, s.contains('%'))
                assertTrue(s, s.endsWith(".\""))
                assertTrue(s, s.contains(": \""))
            }
        }
    }

    @Test
    fun `statements use first person only for the speaker`() {
        val text = WhoDidItText(res, puzzle("manor"))
        assertEquals("Dr Quill: \"I was in the Hedge Maze.\"", text.statement(0))
        assertEquals("Aunt Pru: \"Dr Gloom was not in the Crypt.\"", text.statement(1))
        assertEquals("Lord Snoot: \"I do not wear a hat.\"", text.statement(2))
    }

    @Test
    fun `entry names categories icons and card lines resolve`() {
        val text = WhoDidItText(res, puzzle("bakery"))
        assertEquals("Pip", text.entryName(WhoDidItRef(0, 0)))
        assertTrue(text.categoryLabel(WhoDidItCategory.PLACE).isNotBlank())
        assertEquals("↑👓", text.traitIcons(WhoDidItRef(0, 0)))
        assertTrue(text.cardLines(WhoDidItRef(0, 0)).isNotEmpty())
        assertTrue(text.traitIcons(WhoDidItRef(3, 0)).isEmpty())
    }

    @Test
    fun `reveal renders with and without motive`() {
        WHODIDIT_THEMES.forEach { theme ->
            val withMotive = WhoDidItText(res, puzzle(theme.id, 4)).reveal()
            val without = WhoDidItText(res, puzzle(theme.id, 3)).reveal()
            assertSentence(withMotive)
            assertSentence(without)
            assertTrue(withMotive.length > without.length)
        }
    }

    @Test
    fun `hint title and body render for every technique`() {
        val text = WhoDidItText(res, puzzle("castle"))
        val change = WhoDidItCellChange(WhoDidItRef(0, 0), WhoDidItRef(1, 0), WhoDidItMark.NO)
        WhoDidItTechnique.entries.forEach { technique ->
            val step = WhoDidItStep(
                technique,
                listOf(WhoDidItSource.Clue(1), WhoDidItSource.Clue(4), WhoDidItSource.Statement(1)),
                listOf(change),
            )
            assertTrue(text.hintTitle(step).isNotBlank())
            val body = text.hintBody(step)
            assertFalse(body, body.contains('%'))
            assertTrue(body, body.contains("2, 5"))
            val bare = text.hintBody(WhoDidItStep(technique, emptyList(), listOf(change)))
            assertEquals(res.getString(R.string.whodidit_hint_body_grid), bare)
        }
    }

    @Test
    fun `wrong marks warning is its own line and not the grid fallback`() {
        val warning = res.getString(R.string.whodidit_hint_wrong_marks)
        assertEquals("Heads up: one of your marks doesn't fit.", warning)
        assertNotEquals(res.getString(R.string.whodidit_hint_body_grid), warning)
    }

    @Test
    fun `hint lead names the statement technique and its speakers`() {
        val text = WhoDidItText(res, puzzle("manor"))
        val liar = WhoDidItStep(WhoDidItTechnique.LIAR_HYPOTHESIS, listOf(WhoDidItSource.Statement(1)), culpritRemovals = setOf(1))
        assertEquals("First: Suppose someone is lying. Statements: Dr Quill", text.hintLead(liar))
        val check = WhoDidItStep(WhoDidItTechnique.STATEMENT_CHECK, listOf(WhoDidItSource.Statement(2)), culpritRemovals = setOf(0))
        assertEquals("First: A statement gives itself away. Statements: Lord Snoot", text.hintLead(check))
    }

    @Test
    fun `hint action says which mark to place when every change agrees`() {
        val text = WhoDidItText(res, puzzle("bakery"))
        val yes = WhoDidItCellChange(WhoDidItRef(0, 0), WhoDidItRef(1, 0), WhoDidItMark.YES)
        val no = WhoDidItCellChange(WhoDidItRef(0, 1), WhoDidItRef(1, 0), WhoDidItMark.NO)
        assertEquals("Mark a match (O)", text.hintAction(WhoDidItStep(WhoDidItTechnique.SINGLE, changes = listOf(yes))))
        assertEquals("Cross out (X)", text.hintAction(WhoDidItStep(WhoDidItTechnique.SINGLE, changes = listOf(no))))
        assertNull(text.hintAction(WhoDidItStep(WhoDidItTechnique.SINGLE, changes = listOf(yes, no))))
    }

    private fun trainText(
        clues: List<WhoDidItClue>,
        statements: List<WhoDidItStatement> = emptyList(),
        categoryCount: Int = 4,
    ): WhoDidItText {
        val categories = WhoDidItCategory.entries.take(categoryCount)
        val entry = WhoDidItPuzzleEntry(
            id = "t",
            difficulty = "hard",
            theme = "train",
            case = 0,
            size = 4,
            categories = categories,
            entries = categories.map { listOf(0, 1, 2, 3) },
            solution = categories.map { if (it == WhoDidItCategory.PLACE) listOf(1, 0, 3, 2) else listOf(0, 1, 2, 3) },
            culprit = 2,
            clues = clues,
            statements = statements,
        )
        return WhoDidItText(res, entry)
    }

    @Test
    fun `either with suspect options renders for place item and motive`() {
        val text = trainText(
            listOf(
                WhoDidItClue.Either(WhoDidItRef(1, 3), WhoDidItRef(0, 0), WhoDidItRef(0, 1)),
                WhoDidItClue.Either(WhoDidItRef(1, 1), WhoDidItRef(0, 0), WhoDidItRef(0, 1)),
                WhoDidItClue.Either(WhoDidItRef(2, 1), WhoDidItRef(0, 2), WhoDidItRef(0, 3)),
                WhoDidItClue.Either(WhoDidItRef(3, 0), WhoDidItRef(0, 0), WhoDidItRef(0, 1)),
            ),
        )
        assertEquals("Either Madame Zu or Mr Coal was on the Roof.", text.clue(0))
        assertEquals("Either Madame Zu or Mr Coal was in the Caboose.", text.clue(1))
        assertEquals("Either Dr Steam or Miss Soot had the Whistle.", text.clue(2))
        assertEquals("Either Madame Zu or Mr Coal acted out of revenge.", text.clue(3))
    }

    @Test
    fun `places use their own preposition in relations subjects eithers and reveal`() {
        val text = trainText(
            listOf(
                WhoDidItClue.Same(WhoDidItRef(0, 1), WhoDidItRef(1, 3)),
                WhoDidItClue.NotSame(WhoDidItRef(1, 3), WhoDidItRef(2, 0)),
                WhoDidItClue.Either(WhoDidItRef(0, 0), WhoDidItRef(1, 0), WhoDidItRef(1, 3)),
                WhoDidItClue.Crime(WhoDidItRef(1, 3)),
                WhoDidItClue.Either(WhoDidItRef(2, 1), WhoDidItRef(1, 2), WhoDidItRef(1, 3)),
            ),
        )
        assertEquals("Mr Coal was on the Roof.", text.clue(0))
        assertEquals("Whoever was on the Roof did not have the Shovel.", text.clue(1))
        assertEquals("Madame Zu was either in the Dining Car or on the Roof.", text.clue(2))
        assertEquals("Count Crumb was found on the Roof.", text.clue(3))
        assertEquals("Whoever had the Whistle was either in the Engine or on the Roof.", text.clue(4))
        assertEquals("Dr Steam bonked Count Crumb on the Roof with the Suitcase, out of jealousy.", text.reveal())
        assertEquals(
            "Dr Steam bonked Count Crumb on the Roof with the Suitcase.",
            trainText(emptyList(), categoryCount = 3).reveal(),
        )
    }

    @Test
    fun `negated two valued traits read as the opposite value`() {
        val text = trainText(
            listOf(
                WhoDidItClue.LacksTrait(WhoDidItRef(0, 0), WhoDidItTrait.SUSPECT_SHORT),
                WhoDidItClue.LacksTrait(WhoDidItRef(2, 1), WhoDidItTrait.PLACE_INDOOR),
                WhoDidItClue.LacksTrait(WhoDidItRef(1, 0), WhoDidItTrait.ITEM_HEAVY),
                WhoDidItClue.LacksTrait(WhoDidItRef(0, 1), WhoDidItTrait.SUSPECT_HAT),
                WhoDidItClue.LacksTrait(WhoDidItRef(1, 1), WhoDidItTrait.ITEM_CLOTH),
            ),
            listOf(
                WhoDidItStatement(0, WhoDidItClue.LacksTrait(WhoDidItRef(0, 0), WhoDidItTrait.SUSPECT_TALL)),
                WhoDidItStatement(1, WhoDidItClue.LacksTrait(WhoDidItRef(0, 1), WhoDidItTrait.SUSPECT_GLASSES)),
                WhoDidItStatement(2, WhoDidItClue.LacksTrait(WhoDidItRef(0, 0), WhoDidItTrait.SUSPECT_TALL)),
            ),
        )
        assertEquals("Madame Zu is tall.", text.clue(0))
        assertEquals("Whoever had the Whistle was somewhere outdoors.", text.clue(1))
        assertEquals("Whoever was in the Dining Car had something light.", text.clue(2))
        assertEquals("Mr Coal does not wear a hat.", text.clue(3))
        assertEquals("Whoever was in the Caboose did not have anything made of cloth.", text.clue(4))
        assertEquals("Madame Zu: \"I am short.\"", text.statement(0))
        assertEquals("Mr Coal: \"I do not wear glasses.\"", text.statement(1))
        assertEquals("Dr Steam: \"Madame Zu is short.\"", text.statement(2))
    }

    @Test
    fun `every sample puzzle renders every sentence`() {
        val raw = javaClass.classLoader!!.getResource("whodidit_sample_puzzles.json")!!.readText()
        val puzzles = json.decodeFromString(ListSerializer(WhoDidItPuzzleEntry.serializer()), raw)
        assertEquals(WHODIDIT_THEMES.size * 4, puzzles.size)
        puzzles.forEach { p ->
            val text = WhoDidItText(res, p)
            val rendered = buildList {
                add(text.caseTitle())
                add(text.caseQuestion())
                add(text.reveal())
                p.clues.indices.forEach { add(text.clue(it)) }
                p.statements.indices.forEach { add(text.statement(it)) }
            }
            rendered.forEach {
                assertFalse("${p.id}: $it", it.contains('%'))
                assertTrue("${p.id}: $it", it.isNotBlank())
                assertTrue("${p.id}: $it", it.first().isUpperCase())
            }
        }
    }
}
