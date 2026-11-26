package com.quietgrid.app

import com.lemonappdev.konsist.api.Konsist
import org.junit.Assert.assertEquals
import org.junit.Test

class AppArchitectureTest {
    private val gamesPackage = "com.quietgrid.app.games."
    private val emptySavedStateHandle = Regex("""(?<!\w)SavedStateHandle\(\s*\)""")

    private val knownCrossGameImports = setOf(
        "guessbynumbers" to "wordguess",
        "killersudoku" to "sudoku",
        "minesweeper" to "blockfill",
        "minisudoku" to "sudoku",
        "starbattle" to "animaldoku",
        "sudoku" to "minisudoku",
    )

    @Test
    fun `games do not import other games beyond the known list`() {
        val found = Konsist.scopeFromProduction(moduleName = "app")
            .files
            .flatMap { file ->
                val owner = gameOfPackage(file.packagee?.name) ?: return@flatMap emptyList()
                file.imports.mapNotNull { import ->
                    val target = gameOfImport(import.name)
                    if (target != null && target != owner) owner to target else null
                }
            }
            .toSet()

        assertEquals(
            "New cross-game imports; move shared code to a shared package instead",
            emptySet<Pair<String, String>>(),
            found - knownCrossGameImports,
        )
        assertEquals(
            "Cross-game imports removed; drop them from knownCrossGameImports",
            emptySet<Pair<String, String>>(),
            knownCrossGameImports - found,
        )
    }

    @Test
    fun `production code never builds an empty SavedStateHandle`() {
        val offenders = Konsist.scopeFromProduction(moduleName = "app").files
            .filter { emptySavedStateHandle.containsMatchIn(it.text) }
            .map { it.path }
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `app code does not use Hilt or Dagger`() {
        val offenders = Konsist.scopeFromModule("app").files
            .filter { file -> file.imports.any { i -> listOf("dagger.", "javax.inject.", "androidx.hilt.").any(i.name::startsWith) } }
            .map { it.path }
        assertEquals(emptyList<String>(), offenders)
    }

    private fun gameOfPackage(name: String?): String? =
        name?.takeIf { it.startsWith(gamesPackage) }
            ?.removePrefix(gamesPackage)
            ?.substringBefore('.')

    private fun gameOfImport(name: String): String? =
        name.takeIf { it.startsWith(gamesPackage) }
            ?.removePrefix(gamesPackage)
            ?.takeIf { '.' in it }
            ?.substringBefore('.')
}
