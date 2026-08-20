package com.quietgrid.app.ui.screens

sealed interface CompletionHighlight {
    data class Picture(val solution: List<List<Boolean>>) : CompletionHighlight
    data class RevealWord(val word: String) : CompletionHighlight
    data class ThemeIcon(val icon: String) : CompletionHighlight
}

object CompletionExtras {
    private var pending: CompletionHighlight? = null

    fun set(highlight: CompletionHighlight?) {
        pending = highlight
    }

    fun consume(): CompletionHighlight? = pending.also { pending = null }
}
