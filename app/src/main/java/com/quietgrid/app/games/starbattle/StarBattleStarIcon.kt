package com.quietgrid.app.games.starbattle

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

@Composable
fun StarBattleStarIcon(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        BasicText(
            text = "⭐",
            style = TextStyle(fontSize = (maxWidth.value * 0.85f).sp, textAlign = TextAlign.Center),
        )
    }
}
