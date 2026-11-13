package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = OxfordNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6F5),
    onPrimaryContainer = OxfordNavy,
    secondary = AcademicTeal,
    onSecondary = Color.White,
    secondaryContainer = AcademicTealContainer,
    onSecondaryContainer = AcademicTealOnContainer,
    tertiary = WarmAmber,
    onTertiary = Color.White,
    tertiaryContainer = WarmAmberContainer,
    onTertiaryContainer = WarmAmberOnContainer,
    error = ConflictCrimson,
    onError = Color.White,
    errorContainer = ConflictCrimsonContainer,
    onErrorContainer = ConflictCrimsonOnContainer,
    background = IvoryPaper,
    onBackground = SlateInk,
    surface = CrispSurface,
    onSurface = SlateInk,
    surfaceVariant = SoftAlabaster,
    onSurfaceVariant = SlateMuted,
    outline = SlateOutline,
    outlineVariant = Color(0xFFE7E5E0)
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimaryBlue,
    onPrimary = DarkObsidian,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkIvoryInk,
    secondary = DarkTeal,
    onSecondary = DarkObsidian,
    secondaryContainer = DarkTealContainer,
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = DarkAmber,
    onTertiary = DarkObsidian,
    tertiaryContainer = DarkAmberContainer,
    onTertiaryContainer = Color(0xFFFEF3C7),
    error = DarkCrimson,
    onError = DarkObsidian,
    errorContainer = DarkCrimsonContainer,
    onErrorContainer = Color(0xFFFEE2E2),
    background = DarkObsidian,
    onBackground = DarkIvoryInk,
    surface = DarkSurface,
    onSurface = DarkIvoryInk,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkSlateMuted,
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B)
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
private fun animatedColor(target: Color): Color {
    val color by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 280),
        label = "theme_color_transition"
    )
    return color
}

@Composable
private fun animatedColorScheme(target: ColorScheme): ColorScheme {
    return target.copy(
        primary = animatedColor(target.primary),
        onPrimary = animatedColor(target.onPrimary),
        primaryContainer = animatedColor(target.primaryContainer),
        onPrimaryContainer = animatedColor(target.onPrimaryContainer),
        secondary = animatedColor(target.secondary),
        onSecondary = animatedColor(target.onSecondary),
        secondaryContainer = animatedColor(target.secondaryContainer),
        onSecondaryContainer = animatedColor(target.onSecondaryContainer),
        tertiary = animatedColor(target.tertiary),
        background = animatedColor(target.background),
        onBackground = animatedColor(target.onBackground),
        surface = animatedColor(target.surface),
        onSurface = animatedColor(target.onSurface),
        surfaceVariant = animatedColor(target.surfaceVariant),
        onSurfaceVariant = animatedColor(target.onSurfaceVariant),
        outline = animatedColor(target.outline),
        outlineVariant = animatedColor(target.outlineVariant)
    )
}

@Composable
fun SsoTimetableTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val targetScheme = if (useDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = animatedColorScheme(targetScheme),
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
