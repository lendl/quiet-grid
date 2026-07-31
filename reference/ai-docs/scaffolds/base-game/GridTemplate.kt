package com.quietgrid.app.games.__gameId__

import androidx.compose.runtime.Composable

// __GameName__Grid.kt template — the board composable only. No screen chrome here (see PlayScreenTemplate.kt).

@Composable
fun __GameName__Grid(
    state: __GameName__ActiveState,
    onCellAction: (/* __CELL_ARGS__ */) -> Unit,
    // isPencilTheme: Boolean = LocalIsPencilTheme.current  — branch to grayscale variants where a
    // hardcoded color would otherwise bypass the pencil theme, see CLAUDE.md Theming section.
) {
    TODO("__RENDER_BOARD__")
}
