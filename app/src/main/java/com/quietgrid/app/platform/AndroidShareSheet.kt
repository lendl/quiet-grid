package com.quietgrid.app.platform

import android.content.Context
import android.content.Intent

class AndroidShareSheet(private val context: Context) : ShareSheet {
    override fun shareText(text: String, chooserTitle: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
