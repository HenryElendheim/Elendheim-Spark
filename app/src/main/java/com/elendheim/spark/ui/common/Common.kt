package com.elendheim.spark.ui.common

import androidx.compose.ui.graphics.Color
import com.elendheim.spark.model.Pick

/**
 * Small shared helpers used across screens: colour parsing and turning a
 * collision into readable text.
 */

/** Parse a "#RRGGBB" (or "#AARRGGBB") string into a Compose [Color]. */
fun String.toColorOrDefault(default: Color): Color = try {
    val hex = removePrefix("#")
    val value = hex.toLong(16)
    when (hex.length) {
        6 -> Color(0xFF000000 or value)   // add full alpha
        8 -> Color(value)
        else -> default
    }
} catch (e: Exception) {
    default
}

/** The inline form of a collision: "pets x decay over time x Elendian flavor". */
fun List<Pick>.asInline(): String = joinToString("  ×  ") { it.text }

/** The stacked, line-by-line form, easier for some to parse (and for screen readers). */
fun List<Pick>.asLines(): List<String> = map { it.text }

/** A spoken description: names each wheel and its pick, for TalkBack. */
fun List<Pick>.asSpoken(): String = joinToString(", ") { "${it.wheelName}: ${it.text}" }
