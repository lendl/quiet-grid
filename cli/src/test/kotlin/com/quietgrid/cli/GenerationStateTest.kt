package com.quietgrid.cli

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class GenerationStateTest {
    @Test
    fun `hasTried is false until recordTried is called, then true, and survives a save-reload cycle`() {
        val path = Files.createTempDirectory("gen-state-test").resolve("takuzu_state.json").toString()
        val state = GenerationState(path)
        assertFalse(state.hasTried("abc123"))
        state.recordTried("abc123", "valid")
        assertTrue(state.hasTried("abc123"))
        state.save()

        val reloaded = GenerationState(path)
        assertTrue(reloaded.hasTried("abc123"))
    }

    @Test
    fun `recordDifficultyAudit accumulates entries and persists them on save`() {
        val path = Files.createTempDirectory("gen-state-test").resolve("sudoku_state.json").toString()
        val state = GenerationState(path)
        state.recordDifficultyAudit(DifficultyAuditEntry("key1", "hard", "x-wing", 42, 812, "2026-07-24T00:00:00Z"))
        state.save()

        val text = java.io.File(path).readText()
        assertTrue(text.contains("x-wing"))
    }
}
