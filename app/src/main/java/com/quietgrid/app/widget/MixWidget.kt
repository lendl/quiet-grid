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
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.unit.ColorProvider
import com.quietgrid.app.LaunchRequest
import com.quietgrid.app.R
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.ThemeMode
import kotlinx.coroutines.flow.first

private val ICON_RING = 2.dp
private const val MIX_ICON_RATIO = 0.4f
private const val MIX_ICON_STEP = 0.8f

class MixWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val source = quickPlaySource(context)
        val initial = source.render.first()
        provideContent {
            val render by source.render.collectAsState(initial)
            GlanceTheme(quickPlayColors(render.themeMode)) {
                MixWidgetContent(render.mix, render.themeMode)
            }
        }
    }
}

@Composable
fun MixWidgetContent(state: MixWidgetState, themeMode: ThemeMode) {
    val ui = QuickPlayUi(LocalContext.current, themeMode)
    WidgetSurface(state.continueGame, ui) { body ->
        val mix = state.mix
        if (mix != null) MixButton(mix, body, ui) else CreateMixTile(body, ui)
    }
}

@Composable
private fun MixButton(mix: QuickPlayMix, content: DpSize, ui: QuickPlayUi) {
    val playText = ui.string(R.string.quick_play_play)
    val inner = DpSize(content.width - BUTTON_PADDING * 2, content.height - BUTTON_EDGE)
    val metrics = quickPlayButtonMetrics(inner, MIX_TITLE_FRACTION)
    val textWidth = if (metrics.horizontal) inner.width - metrics.iconSize - ICON_TEXT_GAP else inner.width
    val align = if (metrics.horizontal) TextAlign.Start else TextAlign.Center
    val play = ui.text.render(playText, metrics.titleSize, FontWeight.ExtraBold, textWidth, textAlign = align)
    val name = ui.text.render(mix.name, metrics.labelSize, FontWeight.SemiBold, textWidth, textAlign = align, alpha = SOFT_TEXT_ALPHA)

    ChunkyButton(LaunchRequest.StartMix(mix.mixId), "$playText, ${mix.name}", ui, GlanceModifier.fillMaxSize()) {
        val texts = @Composable {
            Label(play, GlanceTheme.colors.onPrimary)
            Spacer(GlanceModifier.height(LINE_GAP))
            Label(name, GlanceTheme.colors.onPrimary)
            if (mix.gameIds.isNotEmpty()) {
                Spacer(GlanceModifier.height(TILE_TEXT_GAP))
                MixGameIcons(mix.gameIds, metrics.iconSize * MIX_ICON_RATIO, ui)
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
private fun CreateMixTile(content: DpSize, ui: QuickPlayUi) {
    val titleText = ui.string(R.string.mix_widget_create)
    val inner = DpSize(content.width - BUTTON_PADDING * 2, content.height)
    val metrics = quickPlayButtonMetrics(inner, CREATE_MIX_TITLE_FRACTION)
    val textWidth = if (metrics.horizontal) inner.width - metrics.iconSize - ICON_TEXT_GAP else inner.width
    val align = if (metrics.horizontal) TextAlign.Start else TextAlign.Center
    val title = ui.text.render(titleText, metrics.titleSize, FontWeight.ExtraBold, textWidth, maxLines = 2, textAlign = align)
    val hint = ui.text.render(ui.string(R.string.mix_widget_create_hint), metrics.labelSize, FontWeight.Medium, textWidth, maxLines = 2, textAlign = align)
    val modifier = GlanceModifier
        .fillMaxSize()
        .cornerRadius(TILE_CORNER)
        .semantics { contentDescription = titleText }
        .clickable(ui.launch(LaunchRequest.OpenMixes))
        .padding(horizontal = BUTTON_PADDING)
    val texts = @Composable {
        Label(title, GlanceTheme.colors.onBackground)
        Spacer(GlanceModifier.height(LINE_GAP))
        Label(hint, GlanceTheme.colors.onSurfaceVariant)
    }

    if (metrics.horizontal) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            PlusBadge(metrics.iconSize)
            Spacer(GlanceModifier.width(ICON_TEXT_GAP))
            Column { texts() }
        }
    } else {
        Column(modifier, verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
            PlusBadge(metrics.iconSize)
            Spacer(GlanceModifier.height(TILE_TEXT_GAP))
            texts()
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
private fun PlusBadge(size: Dp) {
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
