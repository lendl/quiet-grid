package com.quietgrid.cli.wordsearch

import org.junit.Assert.assertTrue
import org.junit.Test

class WordSearchSeedCorpusTest {
    @Test
    fun `loadWordSearchSeedCorpus has at least 15 themes with at least 50 words each for every language`() {
        val corpus = loadWordSearchSeedCorpus()
        for (language in listOf("en", "nl", "de", "fr", "es")) {
            val themes = corpus[language]
            assertTrue("missing language $language", themes != null)
            assertTrue("$language has too few themes", themes!!.size >= 15)
            assertTrue("$language has a theme with too few words", themes.all { it.words.size >= 50 })
        }
    }
}
