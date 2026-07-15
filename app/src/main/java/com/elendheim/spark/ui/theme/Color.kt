package com.elendheim.spark.ui.theme

import androidx.compose.ui.graphics.Color
import com.elendheim.spark.branding.ElendheimBrand

/**
 * The colour roles the app uses, in two strengths: the default dark palette and
 * a high-contrast variant for low-vision use. Dark mode is the only mode -> the
 * app is dark first and always.
 */

// Default dark palette, built on the brand anchors.
object SparkColors {
    val background = ElendheimBrand.Background
    val surface = ElendheimBrand.Surface
    val surfaceElevated = Color(0xFF2A1417)
    val accent = ElendheimBrand.SoftRed
    val accentPressed = ElendheimBrand.SoftRedDim
    val onBackground = ElendheimBrand.OnDark
    val onSurfaceMuted = ElendheimBrand.OnDarkMuted
    val outline = Color(0xFF3D2428)
}

// High-contrast variant: brighter text, firmer borders, punchier accent.
object SparkColorsHighContrast {
    val background = Color(0xFF000000)
    val surface = Color(0xFF12141A)
    val surfaceElevated = Color(0xFF1E212A)
    val accent = Color(0xFFFF6E73)
    val accentPressed = Color(0xFFFF9498)
    val onBackground = Color(0xFFFFFFFF)
    val onSurfaceMuted = Color(0xFFCED3DC)
    val outline = Color(0xFF7A828F)
}

/**
 * A colourblind-considerate wheel-chip palette with high separation between
 * neighbours. Meaning is never carried by colour alone -> wheel name labels are
 * shown alongside these -> but better-separated hues still help.
 */
val colorblindFriendlyWheelColors = listOf(
    "#E0555A", // red
    "#3F8EDE", // strong blue
    "#F2B705", // gold
    "#2AA198", // teal
    "#9D6BE0", // purple
    "#EE7733"  // orange
)
