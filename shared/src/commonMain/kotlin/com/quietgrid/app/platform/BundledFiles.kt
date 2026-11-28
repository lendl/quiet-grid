package com.quietgrid.app.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.Source
import okio.buffer
import okio.use

interface BundledFiles {
    fun open(path: String): Source
}

suspend fun BundledFiles.readText(path: String): String =
    withContext(Dispatchers.IO) { open(path).buffer().use { it.readUtf8() } }
