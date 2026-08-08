package com.quietgrid.app.ui.screens

/** Game-specific completion flourish that doesn't fit the flat scalar completion route args. */
sealed interface CompletionHighlight {
    data class WordList(val words: List<String>, val solutionWord: String? = null) : CompletionHighlight
    data class Picture(val solution: List<List<Boolean>>) : CompletionHighlight
    data class RevealWord(val word: String) : CompletionHighlight
}

/**
 * One-shot in-memory handoff from a game's finishAsWin/finishAsLoss to CompletionScreen/LossScreen.
 * Not persisted, so a process death between navigate() and composing the destination screen loses
 * the highlight -- that's fine, it's decorative flavor, not core completion data (which lives in
 * the route args).
 */
object CompletionExtras {
    private var pending: CompletionHighlight? = null

    fun set(highlight: CompletionHighlight?) {
        pending = highlight
    }

    fun consume(): CompletionHighlight? = pending.also { pending = null }
}
