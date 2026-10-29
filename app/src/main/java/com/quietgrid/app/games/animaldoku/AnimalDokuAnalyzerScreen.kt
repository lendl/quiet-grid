package com.quietgrid.app.games.animaldoku

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.ui.analyzer.AnalyzerScreenFrame
import com.quietgrid.app.ui.seasonal.rememberHalloweenActive

@Composable
fun AnimalDokuAnalyzerScreen(snapshot: String?) {
    val viewModel = hiltViewModel<AnimalDokuAnalyzerViewModel, AnimalDokuAnalyzerViewModel.Factory>(
        creationCallback = { factory -> factory.create(snapshot) },
    )
    val emojiPalette = animalDokuEmojiPalette(rememberHalloweenActive())

    AnalyzerScreenFrame(
        viewModel = viewModel,
        hintText = { hint, frame ->
            val message = if (frame == null) animalDokuHintMessage(hint) else animalDokuChainFrameMessage(hint, frame)
            message.title to message.body
        },
        board = { puzzle, cells, hint, frame ->
            AnimalDokuGrid(
                size = puzzle.size,
                regions = puzzle.regions,
                cells = cells,
                hintHighlight = hint?.let {
                    if (frame == null) animalDokuHintHighlight(it, puzzle.regions) else animalDokuChainFrameHighlight(it, puzzle.regions, frame)
                },
                onCellTap = { _, _ -> },
                onCellDrag = { _, _ -> },
                onCellDoubleTap = { _, _ -> },
                emojiPalette = emojiPalette,
            )
        },
    )
}
