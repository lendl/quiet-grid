package com.quietgrid.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.quietgrid.app.LaunchRequest
import com.quietgrid.app.R
import com.quietgrid.app.core.AppLocale
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.data.ThemeMode
import com.quietgrid.app.toIntent
import com.quietgrid.app.ui.theme.ResolvedTheme
import com.quietgrid.app.ui.theme.colorSchemeFor
import com.quietgrid.app.ui.theme.fixedResolvedTheme
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

private const val MAX_ICON_PX = 384

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

class QuickPlayWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val source = EntryPointAccessors.fromApplication(context, QuickPlayWidgetEntryPoint::class.java).quickPlaySource()
        val initial = source.render.first()
        provideContent {
            val render by source.render.collectAsState(initial)
            GlanceTheme(quickPlayColors(render.themeMode)) {
                QuickPlayContent(render.state, render.themeMode == ThemeMode.PENCIL)
            }
        }
    }
}

@Composable
fun QuickPlayContent(state: QuickPlayState, isPencilTheme: Boolean) {
    val context = LocalContext.current
    val localized = AppLocale.wrap(context)
    val size = LocalSize.current
    val buttonCount = if (state is QuickPlayState.Games) state.games.size else 1
    val metrics = quickPlayMetrics(size.width, size.height, buttonCount)

    Box(
        GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.background)
            .padding(8.dp),
    ) {
        when (state) {
            QuickPlayState.Continue -> QuickPlayButton(
                request = LaunchRequest.Resume,
                label = localized.getString(R.string.quick_play_continue),
                icon = ImageProvider(R.drawable.ic_widget_continue),
                iconTint = ColorFilter.tint(GlanceTheme.colors.primary),
                metrics = metrics,
                modifier = GlanceModifier.fillMaxSize(),
            )
            is QuickPlayState.PlayMix -> QuickPlayButton(
                request = LaunchRequest.StartMix(state.mixId),
                label = localized.getString(R.string.quick_play_play),
                icon = ImageProvider(R.drawable.ic_widget_mix),
                iconTint = ColorFilter.tint(GlanceTheme.colors.primary),
                metrics = metrics,
                modifier = GlanceModifier.fillMaxSize(),
            )
            is QuickPlayState.Games -> {
                val iconPx = (metrics.iconSize.value * context.resources.displayMetrics.density).toInt().coerceIn(1, MAX_ICON_PX)
                if (metrics.vertical) {
                    Column(GlanceModifier.fillMaxSize()) {
                        state.games.forEachIndexed { index, game ->
                            if (index > 0) Spacer(GlanceModifier.height(8.dp))
                            GameButton(game, localized, iconPx, isPencilTheme, metrics, GlanceModifier.defaultWeight().fillMaxWidth())
                        }
                    }
                } else {
                    Row(GlanceModifier.fillMaxSize()) {
                        state.games.forEachIndexed { index, game ->
                            if (index > 0) Spacer(GlanceModifier.width(8.dp))
                            GameButton(game, localized, iconPx, isPencilTheme, metrics, GlanceModifier.defaultWeight().fillMaxHeight())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GameButton(
    game: QuickPlayGame,
    localized: Context,
    iconPx: Int,
    isPencilTheme: Boolean,
    metrics: QuickPlayMetrics,
    modifier: GlanceModifier,
) {
    QuickPlayButton(
        request = LaunchRequest.StartGame(game.gameId, game.difficulty),
        label = localized.getString(GameCatalog.get(game.gameId).titleRes),
        icon = ImageProvider(renderGameIconBitmap(LocalContext.current, game.gameId, iconPx, isPencilTheme)),
        iconTint = null,
        metrics = metrics,
        modifier = modifier,
    )
}

@Composable
private fun QuickPlayButton(
    request: LaunchRequest,
    label: String,
    icon: ImageProvider,
    iconTint: ColorFilter?,
    metrics: QuickPlayMetrics,
    modifier: GlanceModifier,
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .cornerRadius(12.dp)
            .background(GlanceTheme.colors.surfaceVariant)
            .clickable(actionStartActivity(request.toIntent(context))),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            provider = icon,
            contentDescription = null,
            modifier = GlanceModifier.size(metrics.iconSize),
            colorFilter = iconTint,
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = label,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = metrics.textSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
            maxLines = 2,
        )
    }
}
