package com.quietgrid.app.ui.screens

object AnalyzerHandoff {
    private var pending: String? = null

    fun set(snapshot: String?) {
        pending = snapshot
    }

    fun consume(): String? = pending.also { pending = null }
}
