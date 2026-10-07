package com.quietgrid.app.games.whodidit

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WhoDidItBankRenderTest {
    @Test
    fun `every entry in the real bank renders every text without placeholders`() {
        val res = ApplicationProvider.getApplicationContext<Context>().resources
        val entries = loadWhoDidItBankForTest()
        assertTrue(entries.isNotEmpty())
        entries.forEach { puzzle ->
            val text = WhoDidItText(res, puzzle)
            val rendered = buildList {
                add(text.caseTitle())
                add(text.caseQuestion())
                add(text.reveal())
                puzzle.clues.indices.forEach { add(text.clue(it)) }
                puzzle.statements.indices.forEach { add(text.statement(it)) }
            }
            rendered.forEach {
                assertFalse("${puzzle.id}: $it", it.contains('%'))
                assertTrue("${puzzle.id}: blank", it.isNotBlank())
            }
        }
    }
}
