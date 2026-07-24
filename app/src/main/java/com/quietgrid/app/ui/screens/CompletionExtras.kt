package com.quietgrid.app.ui.screens

/** Game-specific completion flourish that doesn't fit the flat scalar completion route args. */
sealed interface CompletionHighlight {
    data class WordList(val words: List<String>) : CompletionHighlight
    data class Picture(val solution: List<List<Boolean>>) : CompletionHighlight
}

/**
 * One-shot in-memory handoff from a game's finishAsWin to CompletionScreen. Not persisted, so a
 * process death between navigate() and composing CompletionScreen loses the highlight -- that's
 * fine, it's decorative flavor, not core completion data (which lives in the route args).
 */
object CompletionExtras {
    private var pending: CompletionHighlight? = null

    fun set(highlight: CompletionHighlight?) {
        pending = highlight
    }

    fun consume(): CompletionHighlight? = pending.also { pending = null }
}
