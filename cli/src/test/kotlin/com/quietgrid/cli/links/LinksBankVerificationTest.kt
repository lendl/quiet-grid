package com.quietgrid.cli.links

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.links.LINKS_SIZE_BY_DIFFICULTY
import com.quietgrid.engine.links.LinksPuzzleEntry
import com.quietgrid.engine.links.countLinksSolutions
import com.quietgrid.engine.links.isValidLinksSolution
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LinksBankVerificationTest {
    private val bank: List<LinksPuzzleEntry> = Json { ignoreUnknownKeys = true }
        .decodeFromString(ListSerializer(LinksPuzzleEntry.serializer()), File("app/src/main/assets/links_puzzles.json").readText())

    @Test
    fun `every tier has at least 100 puzzles at its grid size`() {
        Difficulty.entries.forEach { difficulty ->
            val tier = bank.filter { it.difficulty == difficulty.key }
            assertTrue(difficulty.key, tier.size >= 100)
            assertTrue(difficulty.key, tier.all { it.size == LINKS_SIZE_BY_DIFFICULTY.getValue(difficulty) })
        }
    }

    @Test
    fun `ids and layouts are unique`() {
        assertEquals(bank.size, bank.map { it.id }.toSet().size)
        assertEquals(bank.size, bank.map { linksPuzzleKey(it.size, it.endpoints) }.toSet().size)
    }

    @Test
    fun `every puzzle solves by logic at its own tier`() {
        bank.forEach { entry -> assertTrue(entry.id, isLinksEntryAtTier(entry)) }
    }

    @Test
    fun `every puzzle has one answer that matches its stored solution`() {
        bank.forEach { entry ->
            assertTrue(entry.id, isValidLinksSolution(entry.size, entry.endpoints, entry.solution))
            val count = countLinksSolutions(entry.size, entry.endpoints)
            assertTrue(entry.id, count.exhausted)
            assertEquals(entry.id, 1, count.solutions)
        }
    }
}
