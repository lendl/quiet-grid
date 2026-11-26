package com.quietgrid.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quietgrid.app.LocalAppGraph
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.appVersionName
import com.quietgrid.app.core.buildPuzzleReportUrl
import com.quietgrid.app.data.PlayRecord

@Composable
fun ResultOverflowMenu(
    gameId: GameId,
    difficulty: Difficulty,
    onAddToMix: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenGamePage: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val language = LocalConfiguration.current.locales[0].toLanguageTag()
    val graph = LocalAppGraph.current
    val records by remember(graph, gameId) { graph.playHistoryRepository.recordsFor(gameId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var menuOpen by remember { mutableStateOf(false) }

    fun reportProblem() {
        val latest = records
            .filter { !it.isChallenger && it.difficulty == difficulty.key }
            .maxByOrNull { it.timestampMillis }
        val url = buildPuzzleReportUrl(
            gameKey = gameId.key,
            difficultyKey = difficulty.key,
            puzzleId = latest?.puzzleId,
            dailyDate = latest?.dailyDate,
            result = latest?.resultLabel() ?: "unknown",
            appVersion = appVersionName(context),
            appLanguage = language,
        )
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    Box(modifier) {
        IconButton(onClick = { menuOpen = true }) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.result_more_options),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            if (onOpenGamePage != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.result_game_page)) },
                    leadingIcon = { Icon(Icons.Outlined.GridView, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onOpenGamePage()
                    },
                )
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.mix_add_to_mix)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    onAddToMix()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.report_puzzle_problem)) },
                leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    reportProblem()
                },
            )
        }
    }
}

private fun PlayRecord.resultLabel(): String = when {
    solved -> "solved"
    lossReason != null -> "lost ($lossReason)"
    else -> "lost"
}
