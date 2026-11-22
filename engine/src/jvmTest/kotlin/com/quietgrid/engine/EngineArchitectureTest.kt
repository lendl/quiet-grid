package com.quietgrid.engine

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Test
import kotlin.test.assertTrue

class EngineArchitectureTest {
    @Test
    fun `engine module has no Android dependencies`() {
        val files = Konsist.scopeFromProduction(moduleName = "engine").files
        assertTrue(files.any { "commonMain" in it.path })
        files.assertFalse { file ->
            file.hasImport { import -> import.name.startsWith("android.") || import.name.startsWith("androidx.") }
        }
    }
}
