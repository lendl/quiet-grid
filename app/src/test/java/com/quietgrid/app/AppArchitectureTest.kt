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

    @Test
    fun `view models do not take a Context`() {
        val offenders = Konsist.scopeFromProduction(moduleName = "app")
            .classes()
            .filter { it.name.endsWith("ViewModel") }
            .filter { viewModel ->
                viewModel.primaryConstructor?.parameters.orEmpty().any { it.type.name.substringAfterLast('.') == "Context" }
            }
            .map { it.name }
            .sorted()
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `code that moves to shared uses no Android or JVM API`() {
        val offenders = Konsist.scopeFromProduction(moduleName = "app").files
            .filter { it.isPortable() }
            .flatMap { file -> file.imports.map { it.name }.filter(::isPlatformImport).map { "${file.path}: $it" } }
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun `code that moves to shared calls no JVM-only API that needs no import`() {
        val offenders = Konsist.scopeFromProduction(moduleName = "app").files
            .filter { it.isPortable() }
            .flatMap { file ->
                val volatileImported = file.imports.any { it.name == "kotlin.concurrent.Volatile" }
                jvmOnlyCalls
                    .filter { (name, pattern) -> pattern.containsMatchIn(file.text) && !(name == "@Volatile" && volatileImported) }
                    .map { (name, _) -> "${file.path}: $name" }
            }
        assertEquals(emptyList<String>(), offenders)
    }

    private val jvmOnlyCalls = listOf(
        "System" to Regex("""(?<![\w.])System(\.|::)"""),
        "Charsets" to Regex("""(?<![\w.])Charsets\."""),
        "@Volatile" to Regex("""@Volatile\b"""),
        "synchronized" to Regex("""(?<![\w.])synchronized\s*\("""),
        "Math" to Regex("""(?<![\w.])Math\."""),
        "Thread" to Regex("""(?<![\w.])Thread\."""),
        "::class.java" to Regex("""::class\.java\b"""),
        "javaClass" to Regex("""\.javaClass\b"""),
        "String.format" to Regex("""String\.format\(|"\.format\("""),
    )

    private fun com.lemonappdev.konsist.api.declaration.KoFileDeclaration.isPortable(): Boolean {
        val path = path.replace(Char(92), '/').substringAfter("com/quietgrid/app/", "")
        if (path.isEmpty()) return false
        val name = path.substringAfterLast('/')
        return path.startsWith("data/") || path.startsWith("core/") || path.startsWith("session/") ||
            (path.startsWith("games/") && portableGameSuffixes.any(name::endsWith)) ||
            (path.startsWith("ui/") && name.endsWith("ViewModel.kt"))
    }

    private fun isPlatformImport(name: String): Boolean {
        if (platformPrefixes.any(name::startsWith)) return true
        if (!name.startsWith("androidx.")) return false
        return allowedAndroidx.none(name::startsWith)
    }

    private val portableGameSuffixes = listOf("ViewModel.kt", "PuzzleBank.kt", "Logic.kt", "Models.kt")
    private val platformPrefixes = listOf("android.", "java.", "javax.")
    private val allowedAndroidx = listOf(
        "androidx.lifecycle.ViewModel",
        "androidx.lifecycle.viewModelScope",
        "androidx.lifecycle.SavedStateHandle",
        "androidx.annotation.",
        "androidx.room.",
        "androidx.datastore.",
        "androidx.compose.runtime.",
        "androidx.compose.ui.graphics.",
    )

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
