package com.quietgrid.app.games.animaldoku

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.ui.analyzer.AnalyzerScreenFrame
import com.quietgrid.app.ui.seasonal.rememberHalloweenActive

@Composable
fun AnimalDokuAnalyzerScreen(snapshot: String?, onBack: () -> Unit) {
    val viewModel = hiltViewModel<AnimalDokuAnalyzerViewModel, AnimalDokuAnalyzerViewModel.Factory>(
        creationCallback = { factory -> factory.create(snapshot) },
    )
    val emojiPalette = animalDokuEmojiPalette(rememberHalloweenActive())

    AnalyzerScreenFrame(
        viewModel = viewModel,
        onBack = onBack,
        hintText = { hint -> animalDokuHintMessage(hint).let { it.title to it.body } },
        board = { puzzle, cells, hint ->
            AnimalDokuGrid(
                size = puzzle.size,
                regions = puzzle.regions,
                cells = cells,
                hintHighlight = hint?.let { animalDokuHintHighlight(it, puzzle.regions) },
                onCellTap = { _, _ -> },
                onCellDrag = { _, _ -> },
                onCellDoubleTap = { _, _ -> },
                emojiPalette = emojiPalette,
            )
        },
    )
}
