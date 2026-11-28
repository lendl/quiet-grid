package com.quietgrid.app.platform

import android.content.Context
import okio.Source
import okio.source

class AndroidBundledFiles(private val context: Context) : BundledFiles {
    override fun open(path: String): Source = context.assets.open(path).source()
}
