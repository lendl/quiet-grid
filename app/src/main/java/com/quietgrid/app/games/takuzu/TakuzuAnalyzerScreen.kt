package com.quietgrid.app.games.takuzu

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.ui.analyzer.AnalyzerScreenFrame

@Composable
fun TakuzuAnalyzerScreen(snapshot: String?, onBack: () -> Unit) {
    val viewModel = hiltViewModel<TakuzuAnalyzerViewModel, TakuzuAnalyzerViewModel.Factory>(
        creationCallback = { factory -> factory.create(snapshot) },
    )

    AnalyzerScreenFrame(
        viewModel = viewModel,
        onBack = onBack,
        hintText = { hint -> resolveTakuzuHintText(hint) },
        board = { puzzle, board, hint ->
            TakuzuBoard(
                board = board,
                isGiven = puzzle.isGiven,
                finishedCells = List(puzzle.size) { List(puzzle.size) { false } },
                feedbackCorrectRows = emptySet(),
                feedbackCorrectCols = emptySet(),
                feedbackIncorrectRows = emptySet(),
                feedbackIncorrectCols = emptySet(),
                size = puzzle.size,
                onCellPress = { _, _ -> },
                hintEvidenceCells = hint?.evidenceCells?.toSet() ?: emptySet(),
                hintTargetCells = hint?.targetCells?.associate { (r, c, v) -> (r to c) to v } ?: emptyMap(),
                hintHighlightRows = hint?.highlightRows?.toSet() ?: emptySet(),
                hintHighlightCols = hint?.highlightCols?.toSet() ?: emptySet(),
            )
        },
    )
}
