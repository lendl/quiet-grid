package com.quietgrid.shared

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StringResourceQuotesTest {
    private val backslashEscapes = listOf("'", "${'"'}").map { Char(92) + it }

    @Test
    fun `shared string files write quotes without backslashes`() {
        val files = File("src/commonMain/composeResources")
            .listFiles { dir -> dir.name.startsWith("values") }.orEmpty()
            .flatMap { dir -> dir.listFiles { file -> file.extension == "xml" }.orEmpty().toList() }
            .sorted()
        assertTrue(files.isNotEmpty())
        val offenders = files.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                "${file.parentFile.name}/${file.name}:${index + 1}".takeIf { backslashEscapes.any { it in line } }
            }
        }
        assertEquals(emptyList(), offenders.take(5), "${offenders.size} lines escape quotes with a backslash")
    }
}
