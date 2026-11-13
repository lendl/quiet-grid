package com.quietgrid.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.difficultyColor
import com.quietgrid.app.core.mix.MixEntry
import com.quietgrid.app.core.mix.MixEntryMode
import com.quietgrid.app.core.mix.entrySharePercent
import com.quietgrid.app.games.gameDifficultyLabelRes

private data class ExampleMixRow(val gameId: GameId, val difficulty: Difficulty, val weight: Int)

private val exampleMixRows = listOf(
    ExampleMixRow(GameId.SUDOKU, Difficulty.MEDIUM, 5),
    ExampleMixRow(GameId.TAKUZU, Difficulty.EASY, 3),
    ExampleMixRow(GameId.WORDSEARCH, Difficulty.HARD, 2),
)

private const val EXAMPLE_NEXT_UP_INDEX = 1

@Composable
fun MixesEmptyState(onNewMix: () -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val minHeight = maxHeight
        Box(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .padding(horizontal = 8.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
            ) {
                ExampleMixCard()
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.mix_empty_title), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        stringResource(R.string.mix_empty_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Button(onClick = onNewMix, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.mix_empty_create))
                    }
                    Text(
                        stringResource(R.string.mix_empty_add_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExampleMixCard() {
    val entries = exampleMixRows.map {
        MixEntry(gameId = it.gameId.key, mode = MixEntryMode.PUZZLE, difficulty = it.difficulty.key, weight = it.weight)
    }
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 2.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.mix_empty_example_name), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.mix_empty_example_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        exampleMixRows.forEachIndexed { index, row ->
            ExampleMixShareRow(
                row = row,
                sharePercent = entrySharePercent(entries[index], entries),
                highlighted = index == EXAMPLE_NEXT_UP_INDEX,
            )
        }
        val nextUp = exampleMixRows[EXAMPLE_NEXT_UP_INDEX]
        Row(
            Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Shuffle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                stringResource(
                    R.string.mix_empty_next_up,
                    stringResource(GameCatalog.get(nextUp.gameId).titleRes),
                    stringResource(gameDifficultyLabelRes(nextUp.gameId, nextUp.difficulty)),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExampleMixShareRow(row: ExampleMixRow, sharePercent: Int, highlighted: Boolean) {
    val rowShape = RoundedCornerShape(10.dp)
    val contentColor = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val rowModifier = if (highlighted) {
        Modifier.background(MaterialTheme.colorScheme.primaryContainer, rowShape)
    } else {
        Modifier
    }
    Row(
        rowModifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(GameCatalog.get(row.gameId).titleRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                )
                Text(
                    stringResource(gameDifficultyLabelRes(row.gameId, row.difficulty)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.75f),
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(3.dp)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(sharePercent / 100f)
                        .fillMaxHeight()
                        .background(difficultyColor(row.difficulty), RoundedCornerShape(3.dp)),
                )
            }
        }
        Text(
            stringResource(R.string.mix_share_percent, sharePercent),
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            textAlign = TextAlign.End,
            modifier = Modifier.width(36.dp),
        )
    }
}
