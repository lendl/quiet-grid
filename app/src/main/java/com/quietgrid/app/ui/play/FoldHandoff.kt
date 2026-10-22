package com.quietgrid.app.ui.play

import androidx.compose.ui.geometry.Offset

object FoldHandoff {
    private var pending: Offset? = null

    fun set(center: Offset?) {
        pending = center
    }

    fun consume(): Offset? = pending.also { pending = null }
}
