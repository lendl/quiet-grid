package com.quietgrid.app.games.__gameId__

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.weight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// __GameName__PlayScreen.kt template — screen composition + chrome. Every game's Play screen follows the
// same shape: header row -> optional hint card -> ZoomableBoardSurface { Grid } inside a weighted Box.
// See reference/ai-docs/context/context-maps.md, "Play screen and board" packet.

@Composable
fun __GameName__PlayScreen(
    viewModel: __GameName__ViewModel,
    onBack: () -> Unit,
) {
    // __HEADER_ROW__ — GameBackButton, difficulty/progress, hint toggle, end-puzzle button (AppTopBar-style)

    // __OPTIONAL_HINT_CARD__ — only if this game has a __GameName__NextMove.kt hint surface

    Box(Modifier.weight(1f)) {
        // ZoomableBoardSurface { __GameName__Grid(state = ..., onCellAction = ...) }
    }
}
