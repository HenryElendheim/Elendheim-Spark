package com.elendheim.spark.branding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The shared Elendheim brand pieces, kept in one small module so they can be
 * dropped into any app in the suite. Dark-gray canvas, soft-red spark.
 *
 * This mirrors the reusable ElendheimSplash / ElendheimBrand idea from the rest
 * of the suite: one place owns the look, every app reuses it.
 */
object ElendheimBrand {
    // The core brand palette. These are the anchors the theme builds around.
    val Background = Color(0xFF14161A)   // near-black dark gray
    val Surface = Color(0xFF1C1F26)      // slightly lifted panels
    val SoftRed = Color(0xFFE0555A)      // the signature accent
    val SoftRedDim = Color(0xFFB4464A)   // pressed / secondary accent
    val OnDark = Color(0xFFECEEF2)       // primary text on dark
    val OnDarkMuted = Color(0xFF9AA0AC)  // secondary text
}

/**
 * The spark mark: a small soft-red burst. Drawn in code so it scales cleanly
 * and needs no image asset.
 */
@Composable
fun ElendheimSparkMark(
    modifier: Modifier = Modifier,
    color: Color = ElendheimBrand.SoftRed,
    sizeDp: Int = 48
) {
    Canvas(modifier = modifier.size(sizeDp.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val long = size.minDimension * 0.46f
        val short = size.minDimension * 0.12f
        // Draw a four-point star by overlapping two diamonds -> a clean spark.
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx, cy - long)
            lineTo(cx + short, cy - short)
            lineTo(cx + long, cy)
            lineTo(cx + short, cy + short)
            lineTo(cx, cy + long)
            lineTo(cx - short, cy + short)
            lineTo(cx - long, cy)
            lineTo(cx - short, cy - short)
            close()
        }
        drawPath(path, color)
        // A tiny second spark, offset, to hint at two ideas colliding.
        val s = size.minDimension * 0.16f
        drawCircle(
            color = ElendheimBrand.OnDark,
            radius = s * 0.35f,
            center = Offset(cx + long * 0.7f, cy - long * 0.7f)
        )
    }
}

/** The wordmark: the spark plus the app name, used on the splash and About. */
@Composable
fun ElendheimWordmark(appName: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ElendheimSparkMark(sizeDp = 40)
        Text(
            text = appName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = ElendheimBrand.OnDark
        )
    }
}
