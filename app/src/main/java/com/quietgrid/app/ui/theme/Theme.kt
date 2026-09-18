package com.quietgrid.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.quietgrid.app.R

val LocalIsPencilTheme = compositionLocalOf { false }
val LocalIsDarkTheme = compositionLocalOf { false }

val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
)

private val BaseTypography = Typography()
private val QuietGridTypography = BaseTypography.copy(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = PlusJakartaSans),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = PlusJakartaSans),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = PlusJakartaSans),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = PlusJakartaSans),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = PlusJakartaSans),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = PlusJakartaSans),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = PlusJakartaSans),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = PlusJakartaSans),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = PlusJakartaSans),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFDAB9FF),
    onPrimary = Color(0xFF360097),
    primaryContainer = Color(0xFF5110C6),
    onPrimaryContainer = Color(0xFFEDDCFF),
    secondary = Color(0xFFD0BCFF),
    onSecondary = Color(0xFF29207E),
    secondaryContainer = Color(0xFF463699),
    onSecondaryContainer = Color(0xFFE8DDFF),
    tertiary = Color(0xFFD7BAFF),
    onTertiary = Color(0xFF31108E),
    tertiaryContainer = Color(0xFF502AA9),
    onTertiaryContainer = Color(0xFFECDCFF),
    background = Color(0xFF0D1117),
    onBackground = Color(0xFFF0F6FC),
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFF0F6FC),
    surfaceVariant = Color(0xFF1F242D),
    onSurfaceVariant = Color(0xFF8B949E),
    outline = Color(0xFF30363D),
    outlineVariant = Color(0xFF30363D),
    surfaceTint = Color(0xFFDAB9FF),
    surfaceContainerLowest = Color(0xFF0A0D12),
    surfaceContainerLow = Color(0xFF161B22),
    surfaceContainer = Color(0xFF1A2029),
    surfaceContainerHigh = Color(0xFF1F242D),
    surfaceContainerHighest = Color(0xFF262C36),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF7131E3),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDDCFF),
    onPrimaryContainer = Color(0xFF1F0060),
    secondary = Color(0xFF624DB4),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DDFF),
    onSecondaryContainer = Color(0xFF000A63),
    tertiary = Color(0xFF6D42C5),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFECDCFF),
    onTertiaryContainer = Color(0xFF140067),
    background = Color(0xFFF6F8FA),
    onBackground = Color(0xFF1F2328),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F2328),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF57606A),
    outline = Color(0xFFD0D7DE),
    outlineVariant = Color(0xFFD0D7DE),
    surfaceTint = Color(0xFF7131E3),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F8FA),
    surfaceContainer = Color(0xFFF1F3F5),
    surfaceContainerHigh = Color(0xFFECEEF1),
    surfaceContainerHighest = Color(0xFFE6E9ED),
)

private val PencilColors = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6E6E6),
    onPrimaryContainer = Color(0xFF1A1A1A),
    secondary = Color(0xFF4D4D4D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE6E6E6),
    onSecondaryContainer = Color(0xFF1A1A1A),
    tertiary = Color(0xFF7A7A7A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF0F0F0),
    onTertiaryContainer = Color(0xFF1A1A1A),
    background = Color(0xFFF7F7F7),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFE6E6E6),
    onSurfaceVariant = Color(0xFF4D4D4D),
    outline = Color(0xFFC8C8C8),
    outlineVariant = Color(0xFFD0D0D0),
    error = Color(0xFF1A1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFE6E6E6),
    onErrorContainer = Color(0xFF1A1A1A),
    surfaceTint = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F2F2),
    surfaceContainer = Color(0xFFECECEC),
    surfaceContainerHigh = Color(0xFFE6E6E6),
    surfaceContainerHighest = Color(0xFFE0E0E0),
)

private val SilkColors = lightColorScheme(
    primary = Color(0xFF1C1C29),
    onPrimary = Color(0xFFE1FF00),
    primaryContainer = Color(0xFFDDF180),
    onPrimaryContainer = Color(0xFF262E00),
    secondary = Color(0xFF1C1C29),
    onSecondary = Color(0xFFFF7700),
    secondaryContainer = Color(0xFFFFD2A8),
    onSecondaryContainer = Color(0xFF621100),
    tertiary = Color(0xFF1C1C29),
    onTertiary = Color(0xFF00FFF8),
    tertiaryContainer = Color(0xFFAEF4EF),
    onTertiaryContainer = Color(0xFF003431),
    background = Color(0xFFF7F5F3),
    onBackground = Color(0xFF4B4743),
    surface = Color(0xFFFEFDFC),
    onSurface = Color(0xFF4B4743),
    surfaceVariant = Color(0xFFF3EDE9),
    onSurfaceVariant = Color(0xFF67625F),
    outline = Color(0xFFE2DDD9),
    outlineVariant = Color(0xFFECE7E2),
    error = Color(0xFFFF7878),
    onError = Color(0xFF800001),
    errorContainer = Color(0xFFFFDFDC),
    onErrorContainer = Color(0xFF800001),
    surfaceTint = Color(0xFF1C1C29),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F5F3),
    surfaceContainer = Color(0xFFF4EFEB),
    surfaceContainerHigh = Color(0xFFE9E3DF),
    surfaceContainerHighest = Color(0xFFD8D3CF),
)

private val NordColors = lightColorScheme(
    primary = Color(0xFF5E81AC),
    onPrimary = Color(0xFF03060B),
    primaryContainer = Color(0xFFC8E1FF),
    onPrimaryContainer = Color(0xFF122A46),
    secondary = Color(0xFF81A1C1),
    onSecondary = Color(0xFF06090D),
    secondaryContainer = Color(0xFFCAE1F8),
    onSecondaryContainer = Color(0xFF183046),
    tertiary = Color(0xFF88C0D0),
    onTertiary = Color(0xFF070D10),
    tertiaryContainer = Color(0xFFBEE6F2),
    onTertiaryContainer = Color(0xFF002F39),
    background = Color(0xFFECEFF4),
    onBackground = Color(0xFF2E3440),
    surface = Color(0xFFF5F7FA),
    onSurface = Color(0xFF2E3440),
    surfaceVariant = Color(0xFFE5E9F0),
    onSurfaceVariant = Color(0xFF4F5561),
    outline = Color(0xFFD8DEE9),
    outlineVariant = Color(0xFFE0E5ED),
    error = Color(0xFFBF616A),
    onError = Color(0xFF0D0304),
    errorContainer = Color(0xFFFFD8DA),
    onErrorContainer = Color(0xFF55101D),
    surfaceTint = Color(0xFF5E81AC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFECEFF4),
    surfaceContainer = Color(0xFFE6EAF0),
    surfaceContainerHigh = Color(0xFFD8DEE9),
    surfaceContainerHighest = Color(0xFFCBD1DC),
)

private val CoffeeColors = darkColorScheme(
    primary = Color(0xFFDB924C),
    onPrimary = Color(0xFF110802),
    primaryContainer = Color(0xFF5A2D00),
    onPrimaryContainer = Color(0xFFFFDDBC),
    secondary = Color(0xFF273E3F),
    onSecondary = Color(0xFFD0D5D5),
    secondaryContainer = Color(0xFF1B3234),
    onSecondaryContainer = Color(0xFFC9DCDC),
    tertiary = Color(0xFF11576D),
    onTertiary = Color(0xFFD0DBE0),
    tertiaryContainer = Color(0xFF003949),
    onTertiaryContainer = Color(0xFFC3DCE6),
    background = Color(0xFF261B25),
    onBackground = Color(0xFFC59F61),
    surface = Color(0xFF1E151D),
    onSurface = Color(0xFFC59F61),
    surfaceVariant = Color(0xFF2A2129),
    onSurfaceVariant = Color(0xFF9A825D),
    outline = Color(0xFF4E444D),
    outlineVariant = Color(0xFF342A33),
    error = Color(0xFFFC9581),
    onError = Color(0xFF150806),
    errorContainer = Color(0xFF642116),
    onErrorContainer = Color(0xFFFFD7CC),
    surfaceTint = Color(0xFFDB924C),
    surfaceContainerLowest = Color(0xFF0B050B),
    surfaceContainerLow = Color(0xFF170E16),
    surfaceContainer = Color(0xFF1E141D),
    surfaceContainerHigh = Color(0xFF281E27),
    surfaceContainerHighest = Color(0xFF322731),
)

private val DraculaColors = darkColorScheme(
    primary = Color(0xFFFF79C6),
    onPrimary = Color(0xFF16050E),
    primaryContainer = Color(0xFF631747),
    onPrimaryContainer = Color(0xFFFFCDF0),
    secondary = Color(0xFFBD93F9),
    onSecondary = Color(0xFF0D0815),
    secondaryContainer = Color(0xFF422862),
    onSecondaryContainer = Color(0xFFE6D4FF),
    tertiary = Color(0xFFFFB86C),
    onTertiary = Color(0xFF160D04),
    tertiaryContainer = Color(0xFF5E3806),
    onTertiaryContainer = Color(0xFFFFDEBB),
    background = Color(0xFF282A36),
    onBackground = Color(0xFFF8F8F3),
    surface = Color(0xFF232530),
    onSurface = Color(0xFFF8F8F3),
    surfaceVariant = Color(0xFF333540),
    onSurfaceVariant = Color(0xFFAAADBB),
    outline = Color(0xFF525461),
    outlineVariant = Color(0xFF333540),
    error = Color(0xFFFF5555),
    onError = Color(0xFF160202),
    errorContainer = Color(0xFF6F0D14),
    onErrorContainer = Color(0xFFFFCEC7),
    surfaceTint = Color(0xFFFF79C6),
    surfaceContainerLowest = Color(0xFF14151F),
    surfaceContainerLow = Color(0xFF20222D),
    surfaceContainer = Color(0xFF282A36),
    surfaceContainerHigh = Color(0xFF30323E),
    surfaceContainerHighest = Color(0xFF3A3C49),
)

enum class ResolvedTheme { LIGHT, DARK, PENCIL, SILK, NORD, COFFEE, DRACULA }

@Composable
fun QuietGridTheme(
    resolvedTheme: ResolvedTheme = if (isSystemInDarkTheme()) ResolvedTheme.DARK else ResolvedTheme.LIGHT,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (resolvedTheme) {
        ResolvedTheme.DARK -> DarkColors
        ResolvedTheme.LIGHT -> LightColors
        ResolvedTheme.PENCIL -> PencilColors
        ResolvedTheme.SILK -> SilkColors
        ResolvedTheme.NORD -> NordColors
        ResolvedTheme.COFFEE -> CoffeeColors
        ResolvedTheme.DRACULA -> DraculaColors
    }
    CompositionLocalProvider(
        LocalIsPencilTheme provides (resolvedTheme == ResolvedTheme.PENCIL),
        LocalIsDarkTheme provides (resolvedTheme == ResolvedTheme.DARK),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = QuietGridTypography,
            content = content,
        )
    }
}
