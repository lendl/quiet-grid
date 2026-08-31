package com.quietgrid.app.games.wordguess

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quietgrid.engine.wordguess.LetterState

private const val WORDGUESS_KEYBOARD_ROW_1 = "qwertyuiop"
private const val WORDGUESS_KEYBOARD_ROW_2 = "asdfghjkl"
private const val WORDGUESS_KEYBOARD_ROW_3 = "zxcvbnm"

@Composable
private fun RowScope.WordGuessKey(label: String, state: LetterState?, weight: Float = 1f, onClick: () -> Unit) {
    val (background, foreground) = wordGuessLetterColors(state)
    val icon = wordGuessLetterIcon(state)
    Surface(
        onClick = onClick,
        color = background,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.weight(weight).height(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label.uppercase(),
                color = foreground,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                fontSize = if (label.length > 1) 12.sp else TextUnit.Unspecified,
            )
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = foreground,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(1.dp)
                        .size(11.dp),
                )
            }
        }
    }
}

@Composable
fun WordGuessKeyboard(
    keyboardState: Map<Char, LetterState>,
    onLetter: (Char) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().background(Color.Transparent).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
            for (ch in WORDGUESS_KEYBOARD_ROW_1) {
                WordGuessKey(ch.toString(), keyboardState[ch]) { onLetter(ch) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
            for (ch in WORDGUESS_KEYBOARD_ROW_2) {
                WordGuessKey(ch.toString(), keyboardState[ch]) { onLetter(ch) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
            for (ch in WORDGUESS_KEYBOARD_ROW_3) {
                WordGuessKey(ch.toString(), keyboardState[ch]) { onLetter(ch) }
            }
            Surface(
                onClick = onBackspace,
                color = MaterialTheme.colorScheme.error,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(1.5f).height(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = null, tint = MaterialTheme.colorScheme.onError)
                }
            }
            WordGuessKey("Enter", state = null, weight = 1.5f) { onEnter() }
        }
    }
}
