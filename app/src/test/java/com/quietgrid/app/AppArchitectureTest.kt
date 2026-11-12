package com.quietgrid.app

import com.lemonappdev.konsist.api.Konsist
import org.junit.Assert.assertEquals
import org.junit.Test

class AppArchitectureTest {
    private val gamesPackage = "com.quietgrid.app.games."

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
