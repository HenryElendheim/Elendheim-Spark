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
    // The base is a very dark red rather than a neutral gray.
    val Background = Color(0xFF180B0D)   // very dark red-black
    val Surface = Color(0xFF211012)      // slightly lifted panels
    val SoftRed = Color(0xFFE0555A)      // the signature accent
    val SoftRedDim = Color(0xFFB4464A)   // pressed / secondary accent
    val OnDark = Color(0xFFF0E7E8)       // primary text (warm white)
    val OnDarkMuted = Color(0xFFB29CA0)  // secondary text
}

/**
 * The spark mark: a four-point spark with nested diamonds (red -> orange ->
 * gold -> white) and a small companion dot. Drawn in code so it scales cleanly
 * and matches the launcher icon. [color] is unused now (kept for callers).
 */
@Composable
fun ElendheimSparkMark(
    modifier: Modifier = Modifier,
    color: Color = ElendheimBrand.SoftRed,
    sizeDp: Int = 48
) {
    val red = Color(0xFFF5333A)
    val orange = Color(0xFFF4661F)
    val gold = Color(0xFFFBBA1F)
    val pink = Color(0xFFF6A9A0)

    Canvas(modifier = modifier.size(sizeDp.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val s = size.minDimension

        // A rotated square (diamond) reaching [half] from the centre.
        fun diamond(half: Float) = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx, cy - half)
            lineTo(cx + half, cy)
            lineTo(cx, cy + half)
            lineTo(cx - half, cy)
            close()
        }

        // Outer four-point spark: concave sides pinched toward the centre.
        val arm = s * 0.44f
        val spark = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx, cy - arm)
            quadraticBezierTo(cx, cy, cx + arm, cy)
            quadraticBezierTo(cx, cy, cx, cy + arm)
            quadraticBezierTo(cx, cy, cx - arm, cy)
            quadraticBezierTo(cx, cy, cx, cy - arm)
            close()
        }
        drawPath(spark, red)
        drawPath(diamond(s * 0.30f), orange)
        drawPath(diamond(s * 0.21f), gold)
        drawPath(diamond(s * 0.13f), Color.White)

        // Small companion dot, top-right: pink ring with a white centre.
        val dot = Offset(cx + s * 0.30f, cy - s * 0.34f)
        drawCircle(color = pink, radius = s * 0.10f, center = dot)
        drawCircle(color = Color.White, radius = s * 0.06f, center = dot)
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
