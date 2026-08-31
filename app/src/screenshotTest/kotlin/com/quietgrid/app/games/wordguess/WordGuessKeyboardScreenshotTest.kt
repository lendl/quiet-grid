package com.quietgrid.app.games.wordguess

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.quietgrid.app.ui.theme.QuietGridTheme
import com.quietgrid.app.ui.theme.ResolvedTheme

@Composable
private fun WordGuessKeyboardPreview() {
    QuietGridTheme(resolvedTheme = ResolvedTheme.LIGHT) {
        WordGuessKeyboard(
            keyboardState = emptyMap(),
            onLetter = {},
            onBackspace = {},
            onEnter = {},
        )
    }
}

@PreviewTest
@Preview(name = "Narrow phone (320dp)", widthDp = 320, showBackground = true)
@Composable
fun WordGuessKeyboardNarrowPhonePreview() {
    WordGuessKeyboardPreview()
}

@PreviewTest
@Preview(name = "Standard phone (411dp)", widthDp = 411, showBackground = true)
@Composable
fun WordGuessKeyboardStandardPhonePreview() {
    WordGuessKeyboardPreview()
}
