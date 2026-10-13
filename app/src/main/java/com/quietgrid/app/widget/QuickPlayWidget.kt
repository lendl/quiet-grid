package com.quietgrid.app.widget

import android.content.Context
import androidx.annotation.DrawableRes
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
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProviders
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.unit.ColorProvider
import com.quietgrid.app.LaunchRequest
import com.quietgrid.app.R
import com.quietgrid.app.core.AppLocale
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.difficultyColor
import com.quietgrid.app.core.gameDifficultyLabelRes
import com.quietgrid.app.data.ThemeMode
import com.quietgrid.app.toIntent
import com.quietgrid.app.ui.components.chunkyEdgeColor
import com.quietgrid.app.ui.theme.ResolvedTheme
import com.quietgrid.app.ui.theme.colorSchemeFor
import com.quietgrid.app.ui.theme.fixedResolvedTheme
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import androidx.glance.color.ColorProvider as DayNightColorProvider

private const val MAX_ICON_PX = 384
private val BUTTON_EDGE = 5.dp
private val BUTTON_PADDING = 14.dp
private val ICON_RING = 2.dp
private val ICON_TEXT_GAP = 12.dp
private val TILE_TEXT_GAP = 8.dp
private val LINE_GAP = 3.dp
private val DOT_GAP = 6.dp
private val MIN_TILE_ICON = 24.dp
private val TILE_CORNER = 16.dp
private const val DOT_TO_TEXT = 0.6f
private const val SOFT_TEXT_ALPHA = 0.85f
private const val GLYPH_TO_BADGE = 0.5f
private const val MIX_ICON_RATIO = 0.4f
private const val MIX_ICON_STEP = 0.8f

@EntryPoint
@InstallIn(SingletonComponent::class)
interface QuickPlayWidgetEntryPoint {
    fun quickPlaySource(): QuickPlaySource
}

fun quickPlayColors(themeMode: ThemeMode): ColorProviders {
    val fixed = themeMode.fixedResolvedTheme()
        ?: return ColorProviders(light = colorSchemeFor(ResolvedTheme.LIGHT), dark = colorSchemeFor(ResolvedTheme.DARK))
    return ColorProviders(colorSchemeFor(fixed))
}

fun quickPlayButtonEdge(themeMode: ThemeMode): ColorProvider {
    val fixed = themeMode.fixedResolvedTheme()
        ?: return DayNightColorProvider(
            chunkyEdgeColor(colorSchemeFor(ResolvedTheme.LIGHT).primary),
            chunkyEdgeColor(colorSchemeFor(ResolvedTheme.DARK).primary),
        )
    return ColorProvider(chunkyEdgeColor(colorSchemeFor(fixed).primary))
}

class QuickPlayWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val source = EntryPointAccessors.fromApplication(context, QuickPlayWidgetEntryPoint::class.java).quickPlaySource()
        val initial = source.render.first()
        provideContent {
            val render by source.render.collectAsState(initial)
            GlanceTheme(quickPlayColors(render.themeMode)) {
                QuickPlayContent(render.state, render.themeMode)
            }
        }
    }
}

private class QuickPlayUi(private val context: Context, themeMode: ThemeMode) {
    private val localized: Context = AppLocale.wrap(context)
    private val isPencilTheme = themeMode == ThemeMode.PENCIL
    val buttonEdge = quickPlayButtonEdge(themeMode)
    val text = QuickPlayTextRenderer(context)

    fun string(res: Int): String = localized.getString(res)

    fun title(gameId: GameId): String = string(GameCatalog.get(gameId).titleRes)

    fun difficultyLabel(gameId: GameId, difficulty: Difficulty): String = string(gameDifficultyLabelRes(gameId, difficulty))

    fun dotColor(difficulty: Difficulty): ColorProvider = ColorProvider(difficultyColor(difficulty, isPencilTheme))

    fun gameIcon(gameId: GameId, size: Dp): ImageProvider {
        val px = (size.value * context.resources.displayMetrics.density).toInt().coerceIn(1, MAX_ICON_PX)
        return ImageProvider(renderGameIconBitmap(context, gameId, px, isPencilTheme))
    }

    fun launch(request: LaunchRequest): Action = actionStartActivity(request.toIntent(context))
}

@Composable
fun QuickPlayContent(state: QuickPlayState, themeMode: ThemeMode) {
    val ui = QuickPlayUi(LocalContext.current, themeMode)
    val size = LocalSize.current
    val content = DpSize(size.width - QUICK_PLAY_PADDING * 2, size.height - QUICK_PLAY_PADDING * 2)

    Box(
        GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.background)
            .padding(QUICK_PLAY_PADDING),
    ) {
        when (state) {
            is QuickPlayState.Continue -> ContinueContent(state, content, ui)
            is QuickPlayState.PlayMix -> MixContent(state, content, ui)
            is QuickPlayState.Games -> GamesContent(state, content, ui)
        }
    }
}

@Composable
private fun GamesContent(state: QuickPlayState.Games, content: DpSize, ui: QuickPlayUi) {
    val grid = quickPlayGrid(content, state.games.size)
    val metrics = quickPlayTileMetrics(grid.cell)
    if (grid.vertical) {
        Column(GlanceModifier.fillMaxSize()) {
            state.games.forEachIndexed { index, game ->
                if (index > 0) Spacer(GlanceModifier.height(QUICK_PLAY_GAP))
                GameTile(game, grid.cell, metrics, ui, GlanceModifier.defaultWeight().fillMaxWidth())
            }
        }
    } else {
        Row(GlanceModifier.fillMaxSize()) {
            state.games.forEachIndexed { index, game ->
                if (index > 0) Spacer(GlanceModifier.width(QUICK_PLAY_GAP))
                GameTile(game, grid.cell, metrics, ui, GlanceModifier.defaultWeight().fillMaxHeight())
            }
        }
    }
}

@Composable
private fun ContinueContent(state: QuickPlayState.Continue, content: DpSize, ui: QuickPlayUi) {
    val split = quickPlaySplit(content)
    if (split.vertical) {
        Column(GlanceModifier.fillMaxSize()) {
            ContinueButton(state, split.main, ui, GlanceModifier.fillMaxWidth().height(split.main.height))
            Spacer(GlanceModifier.height(QUICK_PLAY_GAP))
            NewPuzzleTile(split.side, ui, GlanceModifier.defaultWeight().fillMaxWidth())
        }
    } else {
        Row(GlanceModifier.fillMaxSize()) {
            ContinueButton(state, split.main, ui, GlanceModifier.fillMaxHeight().width(split.main.width))
            Spacer(GlanceModifier.width(QUICK_PLAY_GAP))
            NewPuzzleTile(split.side, ui, GlanceModifier.defaultWeight().fillMaxHeight())
        }
    }
}

@Composable
private fun GameTile(game: QuickPlayGame, cell: DpSize, metrics: QuickPlayTileMetrics, ui: QuickPlayUi, modifier: GlanceModifier) {
    val title = ui.title(game.gameId)
    val difficulty = ui.difficultyLabel(game.gameId, game.difficulty)
    Tile(
        request = LaunchRequest.StartGame(game.gameId, game.difficulty),
        description = "$title, $difficulty",
        title = title,
        detail = game.difficulty to difficulty,
        cell = cell,
        metrics = metrics,
        ui = ui,
        modifier = modifier,
    ) { size ->
        Image(ui.gameIcon(game.gameId, size), contentDescription = null, modifier = GlanceModifier.size(size))
    }
}

@Composable
private fun NewPuzzleTile(cell: DpSize, ui: QuickPlayUi, modifier: GlanceModifier) {
    val label = ui.string(R.string.quick_play_new_puzzle)
    Tile(
        request = LaunchRequest.OpenGames,
        description = label,
        title = label,
        detail = null,
        cell = cell,
        metrics = quickPlayTileMetrics(cell),
        ui = ui,
        modifier = modifier,
    ) { size ->
        Box(GlanceModifier.size(size), contentAlignment = Alignment.Center) {
            Image(
                ImageProvider(R.drawable.ic_widget_tile),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.primaryContainer),
            )
            Image(
                ImageProvider(R.drawable.ic_widget_plus),
                contentDescription = null,
                modifier = GlanceModifier.size(size * GLYPH_TO_BADGE),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer),
            )
        }
    }
}

@Composable
private fun Tile(
    request: LaunchRequest,
    description: String,
    title: String,
    detail: Pair<Difficulty, String>?,
    cell: DpSize,
    metrics: QuickPlayTileMetrics,
    ui: QuickPlayUi,
    modifier: GlanceModifier,
    icon: @Composable (Dp) -> Unit,
) {
    val align = if (metrics.horizontal) TextAlign.Start else TextAlign.Center
    val textWidth = if (metrics.horizontal) cell.width - metrics.maxIconSize - TILE_TEXT_GAP else cell.width
    val titleLabel = ui.text.render(title, metrics.titleSize, FontWeight.SemiBold, textWidth, maxLines = 2, textAlign = align)
    val dotSize = (metrics.detailSize.value * DOT_TO_TEXT).dp
    val detailLabel = detail?.let { (_, label) ->
        ui.text.render(label, metrics.detailSize, FontWeight.Medium, textWidth - dotSize - DOT_GAP, textAlign = align)
    }
    val textHeight = titleLabel.height + (detailLabel?.let { it.height + LINE_GAP } ?: 0.dp)
    val iconSize = if (metrics.horizontal) {
        metrics.maxIconSize
    } else {
        min(metrics.maxIconSize, cell.height - textHeight - TILE_TEXT_GAP).coerceAtLeast(MIN_TILE_ICON)
    }
    val action = modifier
        .cornerRadius(TILE_CORNER)
        .semantics { contentDescription = description }
        .clickable(ui.launch(request))
    val texts = @Composable {
        Label(titleLabel, GlanceTheme.colors.onBackground)
        if (detail != null && detailLabel != null) {
            Spacer(GlanceModifier.height(LINE_GAP))
            DifficultyLine(detail.first, detailLabel, dotSize, GlanceTheme.colors.onSurfaceVariant, ui)
        }
    }

    if (metrics.horizontal) {
        Row(action, verticalAlignment = Alignment.CenterVertically) {
            icon(iconSize)
            Spacer(GlanceModifier.width(TILE_TEXT_GAP))
            Column { texts() }
        }
    } else {
        Column(action, verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
            icon(iconSize)
            Spacer(GlanceModifier.height(TILE_TEXT_GAP))
            texts()
        }
    }
}

@Composable
private fun ContinueButton(state: QuickPlayState.Continue, size: DpSize, ui: QuickPlayUi, modifier: GlanceModifier) {
    val continueText = ui.string(R.string.quick_play_continue)
    val title = ui.title(state.gameId)
    val difficulty = state.difficulty?.let { it to ui.difficultyLabel(state.gameId, it) }
    val inner = DpSize(size.width - BUTTON_PADDING * 2, size.height - BUTTON_EDGE)
    val metrics = quickPlayButtonMetrics(inner, CONTINUE_TITLE_FRACTION)
    val ringed = metrics.iconSize + ICON_RING * 2
    val textWidth = if (metrics.horizontal) inner.width - ringed - ICON_TEXT_GAP else inner.width
    val align = if (metrics.horizontal) TextAlign.Start else TextAlign.Center
    val overline = ui.text.render(continueText, metrics.labelSize, FontWeight.SemiBold, textWidth, textAlign = align, alpha = SOFT_TEXT_ALPHA)
    val titleLabel = ui.text.render(title, metrics.titleSize, FontWeight.ExtraBold, textWidth, maxLines = 2, textAlign = align)
    val dotSize = (metrics.labelSize.value * DOT_TO_TEXT).dp
    val detailLabel = difficulty?.let { (_, label) ->
        ui.text.render(label, metrics.labelSize, FontWeight.Medium, textWidth - dotSize - DOT_GAP, textAlign = align, alpha = SOFT_TEXT_ALPHA)
    }
    val description = listOfNotNull(continueText, title, difficulty?.second).joinToString(", ")

    ChunkyButton(LaunchRequest.Resume, description, ui, modifier) {
        val texts = @Composable {
            Label(overline, GlanceTheme.colors.onPrimary)
            Spacer(GlanceModifier.height(LINE_GAP))
            Label(titleLabel, GlanceTheme.colors.onPrimary)
            if (difficulty != null && detailLabel != null) {
                Spacer(GlanceModifier.height(LINE_GAP))
                DifficultyLine(difficulty.first, detailLabel, dotSize, GlanceTheme.colors.onPrimary, ui)
            }
        }
        val icon = @Composable {
            RingedGameIcon(
                state.gameId,
                metrics.iconSize,
                R.drawable.ic_widget_tile_soft,
                GlanceTheme.colors.onPrimary,
                ui,
                GlanceModifier.size(ringed),
            )
        }
        if (metrics.horizontal) {
            Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(GlanceModifier.width(ICON_TEXT_GAP))
                Column { texts() }
            }
        } else {
            Column(
                GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                icon()
                Spacer(GlanceModifier.height(TILE_TEXT_GAP))
                texts()
            }
        }
    }
}

@Composable
private fun MixContent(state: QuickPlayState.PlayMix, content: DpSize, ui: QuickPlayUi) {
    val playText = ui.string(R.string.quick_play_play)
    val inner = DpSize(content.width - BUTTON_PADDING * 2, content.height - BUTTON_EDGE)
    val metrics = quickPlayButtonMetrics(inner, MIX_TITLE_FRACTION)
    val textWidth = if (metrics.horizontal) inner.width - metrics.iconSize - ICON_TEXT_GAP else inner.width
    val align = if (metrics.horizontal) TextAlign.Start else TextAlign.Center
    val play = ui.text.render(playText, metrics.titleSize, FontWeight.ExtraBold, textWidth, textAlign = align)
    val name = ui.text.render(state.name, metrics.labelSize, FontWeight.SemiBold, textWidth, textAlign = align, alpha = SOFT_TEXT_ALPHA)

    ChunkyButton(LaunchRequest.StartMix(state.mixId), "$playText, ${state.name}", ui, GlanceModifier.fillMaxSize()) {
        val texts = @Composable {
            Label(play, GlanceTheme.colors.onPrimary)
            Spacer(GlanceModifier.height(LINE_GAP))
            Label(name, GlanceTheme.colors.onPrimary)
            if (state.gameIds.isNotEmpty()) {
                Spacer(GlanceModifier.height(TILE_TEXT_GAP))
                MixGameIcons(state.gameIds, metrics.iconSize * MIX_ICON_RATIO, ui)
            }
        }
        if (metrics.horizontal) {
            Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                MixBadge(metrics.iconSize)
                Spacer(GlanceModifier.width(ICON_TEXT_GAP))
                Column { texts() }
            }
        } else {
            Column(
                GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MixBadge(metrics.iconSize)
                Spacer(GlanceModifier.height(TILE_TEXT_GAP))
                texts()
            }
        }
    }
}

@Composable
private fun ChunkyButton(
    request: LaunchRequest,
    description: String,
    ui: QuickPlayUi,
    modifier: GlanceModifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .background(ImageProvider(R.drawable.widget_button_shape), colorFilter = ColorFilter.tint(ui.buttonEdge))
            .semantics { contentDescription = description }
            .clickable(ui.launch(request))
            .padding(bottom = BUTTON_EDGE),
    ) {
        Box(
            GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_button_shape), colorFilter = ColorFilter.tint(GlanceTheme.colors.primary))
                .padding(horizontal = BUTTON_PADDING),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun MixBadge(size: Dp) {
    Box(GlanceModifier.size(size), contentAlignment = Alignment.Center) {
        Image(
            ImageProvider(R.drawable.ic_widget_circle_soft),
            contentDescription = null,
            modifier = GlanceModifier.fillMaxSize(),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
        )
        Image(
            ImageProvider(R.drawable.ic_widget_mix),
            contentDescription = null,
            modifier = GlanceModifier.size(size * GLYPH_TO_BADGE),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
        )
    }
}

@Composable
private fun MixGameIcons(gameIds: List<GameId>, size: Dp, ui: QuickPlayUi) {
    val ringed = size + ICON_RING * 2
    val step = ringed * MIX_ICON_STEP
    Box {
        gameIds.forEachIndexed { index, gameId ->
            val offset = step * index
            RingedGameIcon(
                gameId,
                size,
                R.drawable.ic_widget_tile,
                GlanceTheme.colors.primary,
                ui,
                GlanceModifier.width(offset + ringed).height(ringed).padding(start = offset),
            )
        }
    }
}

@Composable
private fun RingedGameIcon(
    gameId: GameId,
    size: Dp,
    @DrawableRes ring: Int,
    ringColor: ColorProvider,
    ui: QuickPlayUi,
    modifier: GlanceModifier,
) {
    Box(modifier) {
        Image(ImageProvider(ring), contentDescription = null, modifier = GlanceModifier.fillMaxSize(), colorFilter = ColorFilter.tint(ringColor))
        Image(ui.gameIcon(gameId, size), contentDescription = null, modifier = GlanceModifier.fillMaxSize().padding(ICON_RING))
    }
}

@Composable
private fun DifficultyLine(difficulty: Difficulty, label: QuickPlayLabel, dotSize: Dp, textColor: ColorProvider, ui: QuickPlayUi) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            ImageProvider(R.drawable.ic_widget_dot),
            contentDescription = null,
            modifier = GlanceModifier.size(dotSize),
            colorFilter = ColorFilter.tint(ui.dotColor(difficulty)),
        )
        Spacer(GlanceModifier.width(DOT_GAP))
        Label(label, textColor)
    }
}

@Composable
private fun Label(label: QuickPlayLabel, color: ColorProvider) {
    Image(
        ImageProvider(label.bitmap),
        contentDescription = null,
        modifier = GlanceModifier.size(label.width, label.height),
        colorFilter = ColorFilter.tint(color),
    )
}
