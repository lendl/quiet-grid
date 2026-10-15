package com.quietgrid.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.background
import androidx.glance.color.ColorProviders
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
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
import androidx.glance.color.ColorProvider as DayNightColorProvider

private const val MAX_ICON_PX = 384
internal val BUTTON_EDGE = 5.dp
internal val BUTTON_PADDING = 14.dp
internal val ICON_TEXT_GAP = 12.dp
internal val TILE_TEXT_GAP = 8.dp
internal val TILE_CORNER = 16.dp
internal val LINE_GAP = 3.dp
internal val DOT_GAP = 6.dp
internal const val DOT_TO_TEXT = 0.6f
internal const val SOFT_TEXT_ALPHA = 0.85f
internal const val GLYPH_TO_BADGE = 0.5f
private val STRIP_GAP = 8.dp
private val STRIP_END_PADDING = 12.dp
private val MIN_STRIP_TITLE = 28.dp
private const val PLAY_GLYPH_TO_CIRCLE = 0.55f

@EntryPoint
@InstallIn(SingletonComponent::class)
interface QuickPlayWidgetEntryPoint {
    fun quickPlaySource(): QuickPlaySource
}

internal fun quickPlaySource(context: Context): QuickPlaySource =
    EntryPointAccessors.fromApplication(context, QuickPlayWidgetEntryPoint::class.java).quickPlaySource()

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

internal class QuickPlayUi(private val context: Context, themeMode: ThemeMode) {
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
internal fun WidgetSurface(continueGame: QuickPlayContinue?, ui: QuickPlayUi, body: @Composable (DpSize) -> Unit) {
    val size = LocalSize.current
    val content = DpSize(size.width - QUICK_PLAY_PADDING * 2, size.height - QUICK_PLAY_PADDING * 2)

    Box(
        GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.background)
            .padding(QUICK_PLAY_PADDING),
    ) {
        if (continueGame == null) {
            body(content)
        } else {
            val stripHeight = quickPlayStripHeight(content.height)
            Column(GlanceModifier.fillMaxSize()) {
                ContinueStrip(continueGame, DpSize(content.width, stripHeight), ui)
                Spacer(GlanceModifier.height(QUICK_PLAY_STRIP_GAP))
                Box(GlanceModifier.fillMaxWidth().defaultWeight()) {
                    body(DpSize(content.width, content.height - stripHeight - QUICK_PLAY_STRIP_GAP))
                }
            }
        }
    }
}

@Composable
private fun ContinueStrip(game: QuickPlayContinue, size: DpSize, ui: QuickPlayUi) {
    val metrics = quickPlayStripMetrics(size.height)
    val startPadding = (size.height - metrics.circleSize) / 2
    val continueText = ui.string(R.string.quick_play_continue)
    val title = ui.title(game.gameId)
    val difficulty = game.difficulty?.let { it to ui.difficultyLabel(game.gameId, it) }
    val description = listOfNotNull(continueText, title, difficulty?.second).joinToString(", ")
    val fixed = startPadding + metrics.circleSize + STRIP_GAP + STRIP_END_PADDING
    val label = ui.text.render(continueText, metrics.labelSize, FontWeight.ExtraBold, size.width - fixed, textAlign = TextAlign.Start)
    val trailing = size.width - fixed - label.width - STRIP_GAP
    val dotSize = (metrics.titleSize.value * DOT_TO_TEXT).dp
    val titleRoom = trailing - metrics.iconSize - STRIP_GAP - (if (difficulty != null) dotSize + DOT_GAP else 0.dp)
    val titleLabel = if (titleRoom >= MIN_STRIP_TITLE) {
        ui.text.render(title, metrics.titleSize, FontWeight.SemiBold, titleRoom, textAlign = TextAlign.Start)
    } else {
        null
    }

    Row(
        GlanceModifier
            .fillMaxWidth()
            .height(size.height)
            .background(ImageProvider(R.drawable.widget_button_shape), colorFilter = ColorFilter.tint(GlanceTheme.colors.primaryContainer))
            .semantics { contentDescription = description }
            .clickable(ui.launch(LaunchRequest.Resume))
            .padding(start = startPadding, end = STRIP_END_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(GlanceModifier.size(metrics.circleSize), contentAlignment = Alignment.Center) {
            Image(
                ImageProvider(R.drawable.ic_widget_dot),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer),
            )
            Image(
                ImageProvider(R.drawable.ic_widget_continue),
                contentDescription = null,
                modifier = GlanceModifier.size(metrics.circleSize * PLAY_GLYPH_TO_CIRCLE),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.primaryContainer),
            )
        }
        Spacer(GlanceModifier.width(STRIP_GAP))
        Label(label, GlanceTheme.colors.onPrimaryContainer)
        Spacer(GlanceModifier.defaultWeight())
        if (trailing >= metrics.iconSize) {
            Image(ui.gameIcon(game.gameId, metrics.iconSize), contentDescription = null, modifier = GlanceModifier.size(metrics.iconSize))
        }
        if (titleLabel != null) {
            Spacer(GlanceModifier.width(STRIP_GAP))
            Label(titleLabel, GlanceTheme.colors.onPrimaryContainer)
            if (difficulty != null) {
                Spacer(GlanceModifier.width(DOT_GAP))
                Image(
                    ImageProvider(R.drawable.ic_widget_dot),
                    contentDescription = null,
                    modifier = GlanceModifier.size(dotSize),
                    colorFilter = ColorFilter.tint(ui.dotColor(difficulty.first)),
                )
            }
        }
    }
}

@Composable
internal fun DifficultyLine(difficulty: Difficulty, label: QuickPlayLabel, dotSize: Dp, textColor: ColorProvider, ui: QuickPlayUi) {
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
internal fun Label(label: QuickPlayLabel, color: ColorProvider) {
    Image(
        ImageProvider(label.bitmap),
        contentDescription = null,
        modifier = GlanceModifier.size(label.width, label.height),
        colorFilter = ColorFilter.tint(color),
    )
}
