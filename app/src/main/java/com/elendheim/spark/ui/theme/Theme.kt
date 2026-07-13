package com.elendheim.spark.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * The full brand palette exposed to the app, on top of the standard Material
 * roles. Screens read exact brand colours (accent, muted text, outlines,
 * elevated surfaces) from here through [LocalSparkPalette].
 */
data class SparkPalette(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val accent: Color,
    val accentPressed: Color,
    val onBackground: Color,
    val onSurfaceMuted: Color,
    val outline: Color,
    val highContrast: Boolean
)

val LocalSparkPalette = staticCompositionLocalOf {
    SparkPalette(
        background = SparkColors.background,
        surface = SparkColors.surface,
        surfaceElevated = SparkColors.surfaceElevated,
        accent = SparkColors.accent,
        accentPressed = SparkColors.accentPressed,
        onBackground = SparkColors.onBackground,
        onSurfaceMuted = SparkColors.onSurfaceMuted,
        outline = SparkColors.outline,
        highContrast = false
    )
}

/**
 * Elendheim Spark's theme. Always dark. Optionally high-contrast, and with an
 * extra text-scale multiplier layered on top of the system font size.
 *
 * @param highContrast swap to the stronger low-vision palette.
 * @param textScale multiply text size (1.0 = system size unchanged).
 */
@Composable
fun SparkTheme(
    highContrast: Boolean = false,
    textScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val palette = if (highContrast) {
        SparkPalette(
            background = SparkColorsHighContrast.background,
            surface = SparkColorsHighContrast.surface,
            surfaceElevated = SparkColorsHighContrast.surfaceElevated,
            accent = SparkColorsHighContrast.accent,
            accentPressed = SparkColorsHighContrast.accentPressed,
            onBackground = SparkColorsHighContrast.onBackground,
            onSurfaceMuted = SparkColorsHighContrast.onSurfaceMuted,
            outline = SparkColorsHighContrast.outline,
            highContrast = true
        )
    } else {
        SparkPalette(
            background = SparkColors.background,
            surface = SparkColors.surface,
            surfaceElevated = SparkColors.surfaceElevated,
            accent = SparkColors.accent,
            accentPressed = SparkColors.accentPressed,
            onBackground = SparkColors.onBackground,
            onSurfaceMuted = SparkColors.onSurfaceMuted,
            outline = SparkColors.outline,
            highContrast = false
        )
    }

    // Map our palette onto the Material3 dark scheme so standard components
    // (buttons, switches, text fields) inherit the brand automatically.
    val colorScheme = darkColorScheme(
        primary = palette.accent,
        onPrimary = Color(0xFF1A0B0C),
        secondary = palette.accent,
        background = palette.background,
        onBackground = palette.onBackground,
        surface = palette.surface,
        onSurface = palette.onBackground,
        surfaceVariant = palette.surfaceElevated,
        onSurfaceVariant = palette.onSurfaceMuted,
        outline = palette.outline,
        error = Color(0xFFE0555A)
    )

    // Layer the in-app text scale on top of whatever font scale the system is
    // already using, so both the OS setting and our control are honoured.
    val baseDensity = LocalDensity.current
    val scaledDensity = Density(
        density = baseDensity.density,
        fontScale = baseDensity.fontScale * textScale
    )

    CompositionLocalProvider(
        LocalSparkPalette provides palette,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SparkTypography,
            content = content
        )
    }
}
