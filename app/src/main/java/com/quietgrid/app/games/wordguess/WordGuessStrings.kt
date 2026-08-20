package com.quietgrid.app.games.wordguess

import com.quietgrid.app.R
import com.quietgrid.app.ui.components.QuickStartContent
import com.quietgrid.app.ui.components.QuickStartExample

val WordGuessQuickStart = QuickStartContent(
    goalRes = R.string.wordguess_quickstart_goal,
    bulletRes = listOf(R.string.wordguess_quickstart_bullet_1, R.string.wordguess_quickstart_bullet_2),
    examples = listOf(
        QuickStartExample(R.string.wordguess_quickstart_example_1_word, R.string.wordguess_quickstart_example_1_hint),
        QuickStartExample(R.string.wordguess_quickstart_example_2_word, R.string.wordguess_quickstart_example_2_hint),
        QuickStartExample(R.string.wordguess_quickstart_example_3_word, R.string.wordguess_quickstart_example_3_hint),
    ),
    hookRes = R.string.wordguess_quickstart_hook,
)
