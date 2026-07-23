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

/** True when the grayscale "pencil" theme is active — use to strip hue from feedback/state colors. */
val LocalIsPencilTheme = compositionLocalOf { false }

// Matches the RN app's brand font (@expo-google-fonts/plus-jakarta-sans) — used there for headings
// and card titles while body text stays on the platform default, so only title/headline styles pick
// this up here too.
val PlusJakartaSansBold = FontFamily(Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold))
val PlusJakartaSansExtraBold = FontFamily(Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold))

private val BaseTypography = Typography()
private val QuietGridTypography = BaseTypography.copy(
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = PlusJakartaSansExtraBold, fontWeight = FontWeight.ExtraBold),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = PlusJakartaSansExtraBold, fontWeight = FontWeight.ExtraBold),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = PlusJakartaSansBold, fontWeight = FontWeight.Bold),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = PlusJakartaSansBold, fontWeight = FontWeight.Bold),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = PlusJakartaSansBold, fontWeight = FontWeight.Bold),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = PlusJakartaSansBold, fontWeight = FontWeight.Bold),
)

// Matches the RN app's single-accent violet branding (src/app/theme/index.ts): primary/primaryLight
// are the same across dark and light mode. Secondary/tertiary are derived violet tones — RN has no
// separate brand hues, so these stay in the same family instead of inventing unrelated colors.
val QuietGridPrimary = Color(0xFF7C3AED)
val QuietGridPrimaryLight = Color(0xFFA78BFA)
val QuietGridTertiaryAccent = Color(0xFF9061E8)

private val DarkColors = darkColorScheme(
    primary = QuietGridPrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF2C1F52),
    onPrimaryContainer = Color(0xFFE4D9FF),
    secondary = QuietGridPrimaryLight,
    onSecondary = Color(0xFF1F242D),
    secondaryContainer = Color(0xFF322A4A),
    onSecondaryContainer = Color(0xFFE4D9FF),
    tertiary = QuietGridTertiaryAccent,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF3A2960),
    onTertiaryContainer = Color(0xFFE4D9FF),
    background = Color(0xFF0D1117),
    onBackground = Color(0xFFF0F6FC),
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFF0F6FC),
    surfaceVariant = Color(0xFF1F242D),
    onSurfaceVariant = Color(0xFF8B949E),
    outline = Color(0xFF30363D),
    outlineVariant = Color(0xFF30363D),
)

private val LightColors = lightColorScheme(
    primary = QuietGridPrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFECE3FF),
    onPrimaryContainer = Color(0xFF3A1F7A),
    secondary = QuietGridPrimaryLight,
    onSecondary = Color(0xFF1F2328),
    secondaryContainer = Color(0xFFEFE9FF),
    onSecondaryContainer = Color(0xFF3A1F7A),
    tertiary = QuietGridTertiaryAccent,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF1E8FF),
    onTertiaryContainer = Color(0xFF3A1F7A),
    background = Color(0xFFF6F8FA),
    onBackground = Color(0xFF1F2328),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F2328),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF57606A),
    outline = Color(0xFFD0D7DE),
    outlineVariant = Color(0xFFD0D7DE),
)

// Grayscale palette (no hue anywhere) so the app reads correctly for colorblind players
// and matches the RN app's "pencil" theme intent — a paper-and-graphite look, not just dark/light.
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
    CompositionLocalProvider(LocalIsPencilTheme provides (resolvedTheme == ResolvedTheme.PENCIL)) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = QuietGridTypography,
            content = content,
        )
    }
}
