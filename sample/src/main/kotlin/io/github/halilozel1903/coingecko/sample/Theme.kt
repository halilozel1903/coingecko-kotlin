package io.github.halilozel1903.coingecko.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF14804A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC9F2D8),
    onPrimaryContainer = Color(0xFF00210F),
    secondary = Color(0xFF4E6355),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1E8D6),
    onSecondaryContainer = Color(0xFF0C1F14),
    tertiary = Color(0xFFB7791F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE2B8),
    onTertiaryContainer = Color(0xFF2C1700),
    background = Color(0xFFF6F8F7),
    onBackground = Color(0xFF161D19),
    surface = Color(0xFFF6F8F7),
    onSurface = Color(0xFF161D19),
    surfaceVariant = Color(0xFFE3EAE5),
    onSurfaceVariant = Color(0xFF424943),
    surfaceContainer = Color(0xFFEBF0EC),
    surfaceContainerHigh = Color(0xFFE5EBE7),
    outline = Color(0xFF727972),
    outlineVariant = Color(0xFFC2C9C2),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7DDBA3),
    onPrimary = Color(0xFF00391D),
    primaryContainer = Color(0xFF00522C),
    onPrimaryContainer = Color(0xFFC9F2D8),
    secondary = Color(0xFFB5CCBA),
    onSecondary = Color(0xFF213528),
    secondaryContainer = Color(0xFF374B3D),
    onSecondaryContainer = Color(0xFFD1E8D6),
    tertiary = Color(0xFFF4BE6E),
    onTertiary = Color(0xFF462A00),
    tertiaryContainer = Color(0xFF643F00),
    onTertiaryContainer = Color(0xFFFFE2B8),
    background = Color(0xFF0E1411),
    onBackground = Color(0xFFDDE4DE),
    surface = Color(0xFF0E1411),
    onSurface = Color(0xFFDDE4DE),
    surfaceVariant = Color(0xFF26302A),
    onSurfaceVariant = Color(0xFFC2C9C2),
    surfaceContainer = Color(0xFF19211C),
    surfaceContainerHigh = Color(0xFF212A24),
    outline = Color(0xFF8C938C),
    outlineVariant = Color(0xFF424943),
)

private val LocalDark = staticCompositionLocalOf { false }

/** Gain and loss colors that read well on the theme's surfaces. */
object LocalTrackerColors {
    val gain: Color
        @Composable @ReadOnlyComposable
        get() = if (LocalDark.current) Color(0xFF4ADE80) else Color(0xFF15803D)

    val loss: Color
        @Composable @ReadOnlyComposable
        get() = if (LocalDark.current) Color(0xFFF87171) else Color(0xFFDC2626)
}

@Composable
fun TrackerTheme(dark: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDark provides dark) {
        MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
    }
}
