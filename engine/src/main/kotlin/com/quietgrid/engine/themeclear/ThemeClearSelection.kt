package com.quietgrid.engine.themeclear

enum class ThemeClearSelectionState { EMPTY, NO_MATCH, BUILDING, PENDING, ACCEPT }

fun evaluateThemeClearSelection(
    selection: String,
    remainingLetters: String,
    dictionary: ThemeClearDictionary,
): ThemeClearSelectionState = when {
    selection.isEmpty() -> ThemeClearSelectionState.EMPTY
    !dictionary.hasPrefix(selection) -> ThemeClearSelectionState.NO_MATCH
    !dictionary.contains(selection) -> ThemeClearSelectionState.BUILDING
    dictionary.canExtend(selection, remainingLetters) -> ThemeClearSelectionState.PENDING
    else -> ThemeClearSelectionState.ACCEPT
}
