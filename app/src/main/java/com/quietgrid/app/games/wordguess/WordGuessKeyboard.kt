package com.quietgrid.app.games.wordguess

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import com.quietgrid.engine.wordguess.LetterState

private const val WORDGUESS_KEYBOARD_ROW_1 = "qwertyuiop"
private const val WORDGUESS_KEYBOARD_ROW_2 = "asdfghjkl"
private const val WORDGUESS_KEYBOARD_ROW_3 = "zxcvbnm"

@Composable
private fun WordGuessKey(label: String, state: LetterState?, widthDp: Int = 32, onClick: () -> Unit) {
    val (background, foreground) = wordGuessLetterColors(state)
    val icon = wordGuessLetterIcon(state)
    Surface(
        onClick = onClick,
        color = background,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.size(width = widthDp.dp, height = 42.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label.uppercase(), color = foreground, fontWeight = FontWeight.Bold)
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
    Column(modifier.background(Color.Transparent).padding(4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (ch in WORDGUESS_KEYBOARD_ROW_1) {
                WordGuessKey(ch.toString(), keyboardState[ch]) { onLetter(ch) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (ch in WORDGUESS_KEYBOARD_ROW_2) {
                WordGuessKey(ch.toString(), keyboardState[ch]) { onLetter(ch) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            WordGuessKey("Enter", state = null, widthDp = 56) { onEnter() }
            for (ch in WORDGUESS_KEYBOARD_ROW_3) {
                WordGuessKey(ch.toString(), keyboardState[ch]) { onLetter(ch) }
            }
            Surface(
                onClick = onBackspace,
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.size(width = 56.dp, height = 42.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = null)
                }
            }
        }
    }
}
