package com.quietgrid.app.platform

import com.quietgrid.shared.resources.Res
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.Source
import platform.Foundation.NSURL

class IosBundledFiles : BundledFiles {
    override fun open(path: String): Source = FileSystem.SYSTEM.source(bundlePath(path).toPath())

    private fun bundlePath(path: String): String =
        requireNotNull(NSURL.URLWithString(Res.getUri("files/$path"))?.path) { "No bundled file $path" }
}
