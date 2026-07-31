package com.quietgrid.engine

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Test

class EngineArchitectureTest {
    @Test
    fun `engine module has no Android dependencies`() {
        Konsist.scopeFromProduction(moduleName = "engine")
            .files
            .assertFalse { file ->
                file.hasImport { import -> import.name.startsWith("android.") || import.name.startsWith("androidx.") }
            }
    }
}
