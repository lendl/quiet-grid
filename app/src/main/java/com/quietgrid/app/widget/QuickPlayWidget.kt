package com.quietgrid.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import com.quietgrid.app.LaunchRequest
import com.quietgrid.app.data.ThemeMode
import kotlinx.coroutines.flow.first

private val MIN_TILE_ICON = 24.dp

class QuickPlayWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val source = quickPlaySource(context)
        val initial = source.render.first()
        provideContent {
            val render by source.render.collectAsState(initial)
            GlanceTheme(quickPlayColors(render.themeMode)) {
                QuickPlayContent(render.quickPlay, render.themeMode)
            }
        }
    }
}

private class TileText(val title: QuickPlayLabel, val detail: QuickPlayLabel, val dotSize: Dp)

private class TileLayout(val horizontal: Boolean, val iconSize: Dp, val titleHeight: Dp)

@Composable
fun QuickPlayContent(state: QuickPlayState, themeMode: ThemeMode) {
    val ui = QuickPlayUi(LocalContext.current, themeMode)
    WidgetSurface(state.continueGame, ui) { body -> GamesContent(state.games, body, ui) }
}

@Composable
private fun GamesContent(games: List<QuickPlayGame>, content: DpSize, ui: QuickPlayUi) {
    val grid = quickPlayGrid(content, games.size)
    val metrics = quickPlayTileMetrics(grid.cell)
    val texts = games.map { tileText(it, grid.cell, metrics, ui) }
    val titleHeight = texts.maxOfOrNull { it.title.height } ?: 0.dp
    val textHeight = titleHeight + LINE_GAP + (texts.maxOfOrNull { it.detail.height } ?: 0.dp)
    val iconSize = if (metrics.horizontal) {
        metrics.maxIconSize
    } else {
        min(metrics.maxIconSize, grid.cell.height - textHeight - TILE_TEXT_GAP).coerceAtLeast(MIN_TILE_ICON)
    }
    val layout = TileLayout(metrics.horizontal, iconSize, titleHeight)

    if (grid.vertical) {
        Column(GlanceModifier.fillMaxSize()) {
            games.forEachIndexed { index, game ->
                if (index > 0) Spacer(GlanceModifier.height(QUICK_PLAY_GAP))
                GameTile(game, texts[index], layout, ui, GlanceModifier.defaultWeight().fillMaxWidth())
            }
        }
    } else {
        Row(GlanceModifier.fillMaxSize()) {
            games.forEachIndexed { index, game ->
                if (index > 0) Spacer(GlanceModifier.width(QUICK_PLAY_GAP))
                GameTile(game, texts[index], layout, ui, GlanceModifier.defaultWeight().fillMaxHeight())
            }
        }
    }
}

private fun tileText(game: QuickPlayGame, cell: DpSize, metrics: QuickPlayTileMetrics, ui: QuickPlayUi): TileText {
    val align = if (metrics.horizontal) TextAlign.Start else TextAlign.Center
    val textWidth = if (metrics.horizontal) cell.width - metrics.maxIconSize - TILE_TEXT_GAP else cell.width
    val dotSize = (metrics.detailSize.value * DOT_TO_TEXT).dp
    return TileText(
        title = ui.text.render(ui.title(game.gameId), metrics.titleSize, FontWeight.SemiBold, textWidth, maxLines = 2, textAlign = align),
        detail = ui.text.render(
            ui.difficultyLabel(game.gameId, game.difficulty),
            metrics.detailSize,
            FontWeight.Medium,
            textWidth - dotSize - DOT_GAP,
            textAlign = align,
        ),
        dotSize = dotSize,
    )
}

@Composable
private fun GameTile(game: QuickPlayGame, text: TileText, layout: TileLayout, ui: QuickPlayUi, modifier: GlanceModifier) {
    val description = "${ui.title(game.gameId)}, ${ui.difficultyLabel(game.gameId, game.difficulty)}"
    val action = modifier
        .cornerRadius(TILE_CORNER)
        .semantics { contentDescription = description }
        .clickable(ui.launch(LaunchRequest.StartGame(game.gameId, game.difficulty)))
    val icon = @Composable {
        Image(ui.gameIcon(game.gameId, layout.iconSize), contentDescription = null, modifier = GlanceModifier.size(layout.iconSize))
    }
    val texts = @Composable {
        Box(GlanceModifier.height(layout.titleHeight), contentAlignment = if (layout.horizontal) Alignment.TopStart else Alignment.TopCenter) {
            Label(text.title, GlanceTheme.colors.onBackground)
        }
        Spacer(GlanceModifier.height(LINE_GAP))
        DifficultyLine(game.difficulty, text.detail, text.dotSize, GlanceTheme.colors.onSurfaceVariant, ui)
    }

    if (layout.horizontal) {
        Row(action, verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(GlanceModifier.width(TILE_TEXT_GAP))
            Column { texts() }
        }
    } else {
        Column(action, verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
            icon()
            Spacer(GlanceModifier.height(TILE_TEXT_GAP))
            texts()
        }
    }
}
