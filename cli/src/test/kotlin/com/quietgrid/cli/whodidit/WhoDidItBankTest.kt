package com.quietgrid.cli.whodidit

import com.quietgrid.cli.readPuzzleEntries
import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.whodidit.WHODIDIT_POOL_SIZE
import com.quietgrid.engine.whodidit.WHODIDIT_SHAPES
import com.quietgrid.engine.whodidit.WHODIDIT_THEMES
import com.quietgrid.engine.whodidit.WhoDidItCategory
import com.quietgrid.engine.whodidit.WhoDidItClue
import com.quietgrid.engine.whodidit.WhoDidItPuzzleEntry
import com.quietgrid.engine.whodidit.WhoDidItRef
import com.quietgrid.engine.whodidit.classifyWhoDidIt
import com.quietgrid.engine.whodidit.countWhoDidItExtraClues
import com.quietgrid.engine.whodidit.countWhoDidItSolutions
import com.quietgrid.engine.whodidit.findWhoDidItSolution
import com.quietgrid.engine.whodidit.solveWhoDidIt
import com.quietgrid.engine.whodidit.solutionValue
import com.quietgrid.engine.whodidit.toSpec
import com.quietgrid.engine.whodidit.whoDidItAllowedClueTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhoDidItBankTest {
    private val bank: List<WhoDidItPuzzleEntry> by lazy {
        readPuzzleEntries("app/src/main/assets/whodidit_puzzles.json", WhoDidItPuzzleEntry.serializer())
    }

    private fun difficultyOf(entry: WhoDidItPuzzleEntry): Difficulty? = Difficulty.entries.firstOrNull { it.key == entry.difficulty }

    private fun refs(clue: WhoDidItClue): List<WhoDidItRef> = when (clue) {
        is WhoDidItClue.Same -> listOf(clue.a, clue.b)
        is WhoDidItClue.NotSame -> listOf(clue.a, clue.b)
        is WhoDidItClue.Either -> listOf(clue.a, clue.b, clue.c)
        is WhoDidItClue.HasTrait -> listOf(clue.a)
        is WhoDidItClue.LacksTrait -> listOf(clue.a)
        is WhoDidItClue.Crime -> listOf(clue.e)
        is WhoDidItClue.CrimeTrait -> emptyList()
    }

    private fun traitCategory(clue: WhoDidItClue): Int? = when (clue) {
        is WhoDidItClue.HasTrait -> clue.trait.category.ordinal
        is WhoDidItClue.LacksTrait -> clue.trait.category.ordinal
        is WhoDidItClue.CrimeTrait -> clue.trait.category.ordinal
        else -> null
    }

    private fun structuralErrors(entry: WhoDidItPuzzleEntry): List<String> {
        val errors = mutableListOf<String>()
        val difficulty = difficultyOf(entry)
        if (difficulty == null) {
            errors += "unknown difficulty ${entry.difficulty}"
            return errors
        }
        val shape = WHODIDIT_SHAPES.getValue(difficulty)
        val size = entry.size
        if (size != shape.size) errors += "size $size != ${shape.size}"
        if (entry.categories != WhoDidItCategory.entries.take(shape.categoryCount)) errors += "categories ${entry.categories} do not match shape"
        val categoryCount = entry.categories.size
        val theme = WHODIDIT_THEMES.find { it.id == entry.theme }
        if (theme == null) errors += "unknown theme ${entry.theme}" else if (entry.case !in 0 until theme.caseCount) errors += "case ${entry.case} out of range"
        if (entry.entries.size != categoryCount) errors += "entries has ${entry.entries.size} rows"
        entry.entries.forEachIndexed { category, row ->
            if (row.size != size) errors += "entries[$category] size ${row.size}"
            if (row.toSet().size != row.size) errors += "entries[$category] not distinct"
            if (row.any { it !in 0 until WHODIDIT_POOL_SIZE }) errors += "entries[$category] index out of pool"
        }
        if (entry.solution.size != categoryCount) errors += "solution has ${entry.solution.size} rows"
        entry.solution.forEachIndexed { category, row ->
            if (row.sorted() != (0 until size).toList()) errors += "solution[$category] not a permutation"
        }
        if (entry.solution.firstOrNull() != (0 until size).toList()) errors += "solution row 0 not identity"
        if (entry.culprit !in 0 until size) errors += "culprit ${entry.culprit} out of range"
        val allClues = entry.clues + entry.statements.map { it.clue }
        allClues.forEach { clue ->
            refs(clue).forEach { ref ->
                if (ref.category !in 0 until categoryCount || ref.slot !in 0 until size) errors += "ref out of range in $clue"
            }
            val traitCategory = traitCategory(clue)
            if (traitCategory != null && traitCategory !in 0 until categoryCount) errors += "trait category out of range in $clue"
        }
        if (entry.clues.any { it.type !in whoDidItAllowedClueTypes(difficulty) }) errors += "clue type not allowed for $difficulty"
        if (difficulty == Difficulty.EXPERT) {
            if (entry.statements.map { it.s } != (0 until size).toList()) errors += "statement speakers ${entry.statements.map { it.s }}"
        } else if (entry.statements.isNotEmpty()) {
            errors += "statements on $difficulty"
        }
        return errors
    }

    @Test
    fun `bank has at least 50 puzzles per tier`() {
        Difficulty.entries.forEach { difficulty ->
            val count = bank.count { it.difficulty == difficulty.key }
            assertTrue("${difficulty.key} has $count puzzles", count >= 50)
        }
    }

    @Test
    fun `bank ids are unique`() {
        val duplicates = bank.groupBy { it.id }.filterValues { it.size > 1 }.keys
        assertTrue("duplicate ids $duplicates", duplicates.isEmpty())
    }

    @Test
    fun `every bank puzzle references a known theme, a valid case and valid pool indexes`() {
        val failures = bank.mapNotNull { entry -> structuralErrors(entry).takeIf { it.isNotEmpty() }?.let { "${entry.id}: $it" } }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test(timeout = 120_000)
    fun `every bank puzzle is unique by brute force and its culprit matches`() {
        val failures = bank.filter { structuralErrors(it).isEmpty() }.mapNotNull { entry ->
            val spec = entry.toSpec()
            val count = countWhoDidItSolutions(spec)
            val found = findWhoDidItSolution(spec)
            when {
                count != 1 -> "${entry.id}: $count solutions"
                found != entry.solutionValue() -> "${entry.id}: stored solution differs from the unique solution"
                else -> null
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test(timeout = 120_000)
    fun `every bank puzzle is solved by the technique solver and classifies as its stored difficulty`() {
        val failures = bank.filter { structuralErrors(it).isEmpty() }.mapNotNull { entry ->
            val spec = entry.toSpec()
            val result = solveWhoDidIt(spec)
            if (!result.solved) return@mapNotNull "${entry.id}: not solved by techniques"
            val classified = classifyWhoDidIt(spec, result, countWhoDidItExtraClues(spec))
            if (classified?.key != entry.difficulty) "${entry.id}: classified $classified, stored ${entry.difficulty}" else null
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `every either clue offers two distinct options of one category other than its subject`() {
        val failures = bank.flatMap { entry ->
            (entry.clues + entry.statements.map { it.clue }).filterIsInstance<WhoDidItClue.Either>().mapNotNull { clue ->
                val wellFormed = clue.b.category == clue.c.category &&
                    clue.a.category != clue.b.category &&
                    clue.b != clue.c
                if (wellFormed) null else "${entry.id}: $clue"
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `expert statements only claim same, not-same or has-trait facts`() {
        val experts = bank.filter { it.difficulty == Difficulty.EXPERT.key }
        assertTrue(experts.isNotEmpty())
        val failures = experts.flatMap { entry ->
            entry.statements.mapNotNull { statement ->
                val allowed = statement.clue is WhoDidItClue.Same ||
                    statement.clue is WhoDidItClue.NotSame ||
                    statement.clue is WhoDidItClue.HasTrait
                if (allowed) null else "${entry.id}: speaker ${statement.s} says ${statement.clue}"
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `themes are spread across every tier`() {
        val problems = Difficulty.entries.flatMap { difficulty ->
            val tier = bank.filter { it.difficulty == difficulty.key }
            WHODIDIT_THEMES.mapNotNull { def ->
                val count = tier.count { it.theme == def.id }
                when {
                    count < 5 -> "${def.id}/${difficulty.key} has $count"
                    count * 10 > tier.size * 3 -> "${def.id}/${difficulty.key} has $count of ${tier.size}"
                    else -> null
                }
            }
        }
        assertEquals("theme spread problems $problems", emptyList<String>(), problems)
    }
}
