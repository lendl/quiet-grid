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

// Brand hue seeds (single-accent violet family, matches the RN app's src/app/theme/index.ts —
// no separate brand hues, secondary/tertiary are just other violet tones): primary #7C3AED,
// secondary #A78BFA, tertiary #9061E8. Each role below is generated from these seeds via proper
// M3 HCT tone assignment (light: base tone 40, container tone 90; dark: base tone 80, container
// tone 30) rather than reusing one hex across both schemes — a flat saturated violet at tone ~50
// reads fine on white but glows/vibrates on a dark surface.
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
