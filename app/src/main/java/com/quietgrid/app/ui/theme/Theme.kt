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

enum class ResolvedTheme { LIGHT, DARK, PENCIL }

@Composable
fun QuietGridTheme(
    resolvedTheme: ResolvedTheme = if (isSystemInDarkTheme()) ResolvedTheme.DARK else ResolvedTheme.LIGHT,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (resolvedTheme) {
        ResolvedTheme.DARK -> DarkColors
        ResolvedTheme.LIGHT -> LightColors
        ResolvedTheme.PENCIL -> PencilColors
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
