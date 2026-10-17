package com.quietgrid.app.games.wordsearch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quietgrid.app.R
import com.quietgrid.app.ui.components.FeedbackText
import com.quietgrid.app.ui.play.PlaySizeClass
import com.quietgrid.engine.wordsearch.WSWordEntry

private val LIST_FADE_HEIGHT = 16.dp
private val ALL_WORDS_ROW_HEIGHT = 36.dp
private val MINI_CELL_SIZE = 12.dp

internal fun wordSearchDockHeight(sizeClass: PlaySizeClass): Dp = when (sizeClass) {
    PlaySizeClass.Compact -> 84.dp
    PlaySizeClass.TabletPortrait -> 220.dp
    else -> 132.dp
}

internal fun wordsInPlayOrder(words: List<WSWordEntry>, foundWordIds: Collection<String>): List<WSWordEntry> {
    val alphabetical = words.sortedBy { it.word }
    return alphabetical.filter { it.id !in foundWordIds } + alphabetical.filter { it.id in foundWordIds }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WordSearchDock(
    session: WordSearchSession,
    onToggleHiddenWordMode: () -> Unit,
    wrongHiddenWordTap: Boolean,
) {
    var showAllWords by remember { mutableStateOf(false) }
    val words = wordsInPlayOrder(session.puzzle.words, session.foundWordIds)
    val leftCount = words.count { it.id !in session.foundWordIds }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().weight(1f)) {
            if (!session.hiddenWordSolved) {
                HiddenWordToggleIcon(session, onToggleHiddenWordMode)
            }
            Box(Modifier.weight(1f).fillMaxHeight().clipToBounds()) {
                if (session.hiddenWordMode) {
                    HiddenWordPanel(session, wrongHiddenWordTap, Modifier.fillMaxWidth().padding(top = 8.dp))
                } else {
                    FlowRow(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        words.forEach { word -> WordSearchWordLabel(word.word, found = word.id in session.foundWordIds) }
                    }
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(LIST_FADE_HEIGHT)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background))),
                    )
                }
            }
        }
        TextButton(
            onClick = { showAllWords = true },
            modifier = Modifier.align(Alignment.End).height(ALL_WORDS_ROW_HEIGHT),
        ) {
            Text(stringResource(R.string.play_all_words), fontWeight = FontWeight.SemiBold)
            Text(
                " · ${stringResource(R.string.play_words_left, leftCount)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (showAllWords) {
        WordSearchAllWordsSheet(session, onDismiss = { showAllWords = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun WordSearchAllWordsSheet(session: WordSearchSession, onDismiss: () -> Unit) {
    val words = wordsInPlayOrder(session.puzzle.words, session.foundWordIds)
    val toFind = words.filter { it.id !in session.foundWordIds }
    val found = words.filter { it.id in session.foundWordIds }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.play_words_to_find, toFind.size), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                toFind.forEach { WordSearchWordLabel(it.word, found = false) }
            }
            if (found.isNotEmpty()) {
                Text(
                    stringResource(R.string.play_words_found, found.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    found.forEach { WordSearchWordLabel(it.word, found = true) }
                }
            }
        }
    }
}

@Composable
private fun WordSearchWordLabel(word: String, found: Boolean) {
    Text(
        word,
        fontWeight = FontWeight.Bold,
        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
        textDecoration = if (found) TextDecoration.LineThrough else TextDecoration.None,
        color = if (found) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun HiddenWordToggleIcon(session: WordSearchSession, onToggleHiddenWordMode: () -> Unit) {
    val toggleDescription = stringResource(
        if (session.hiddenWordMode) R.string.wordsearch_hidden_word_exit_mode else R.string.wordsearch_hidden_word_enter_mode,
    )
    Icon(
        imageVector = Icons.Filled.VpnKey,
        contentDescription = toggleDescription,
        tint = if (session.hiddenWordMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onToggleHiddenWordMode),
    )
}

@Composable
private fun HiddenWordPanel(session: WordSearchSession, wrongHiddenWordTap: Boolean, modifier: Modifier = Modifier) {
    val progressText = session.puzzle.hiddenWord.word.indices.joinToString(" ") { index ->
        val cell = session.hiddenWordProgress.getOrNull(index)
        if (cell != null) session.puzzle.grid[cell.row][cell.col] else "_"
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        FeedbackText(
            text = progressText,
            style = MaterialTheme.typography.titleSmall,
            isCorrect = false,
            isIncorrect = wrongHiddenWordTap,
        )
        Text(
            stringResource(R.string.wordsearch_hidden_word_order_prompt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun WordSearchStraightLinePicture() {
    val letters = listOf("CXE", "QAM", "ZRT")
    WordSearchMiniGrid(letters, found = setOf(0 to 0, 1 to 1, 2 to 2), leftover = emptySet())
}

@Composable
internal fun WordSearchLeftoverPicture() {
    WordSearchMiniGrid(listOf("CAT", "OWL"), found = setOf(0 to 0, 0 to 1, 0 to 2), leftover = setOf(1 to 0, 1 to 1, 1 to 2))
}

@Composable
internal fun WordSearchReadingOrderPicture() {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        "OWL".forEachIndexed { index, letter ->
            if (index > 0) {
                Text(
                    "→",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 8.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            WordSearchMiniCell(letter, MaterialTheme.colorScheme.secondary, Color.White)
        }
    }
}

@Composable
private fun WordSearchMiniGrid(rows: List<String>, found: Set<Pair<Int, Int>>, leftover: Set<Pair<Int, Int>>) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        rows.forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEachIndexed { colIndex, letter ->
                    val key = rowIndex to colIndex
                    when (key) {
                        in found -> WordSearchMiniCell(letter, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f), MaterialTheme.colorScheme.onSurface)
                        in leftover -> WordSearchMiniCell(letter, MaterialTheme.colorScheme.secondary, Color.White)
                        else -> WordSearchMiniCell(letter, MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun WordSearchMiniCell(letter: Char, background: Color, textColor: Color) {
    Box(Modifier.size(MINI_CELL_SIZE).background(background, RoundedCornerShape(3.dp)), contentAlignment = Alignment.Center) {
        Text(
            letter.toString(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 8.sp, fontWeight = FontWeight.Bold),
            color = textColor,
        )
    }
}
