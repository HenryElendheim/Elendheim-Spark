package com.elendheim.spark.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.elendheim.spark.ui.theme.colorblindFriendlyWheelColors

/**
 * Distinct shapes used in colourblind mode, so a wheel is told apart by its
 * symbol, not only its colour. Shared by the randomize screen and the editor.
 */
val wheelSymbols: List<ImageVector> = listOf(
    Icons.Filled.Circle,
    Icons.Filled.Square,
    Icons.Filled.Star,
    Icons.Filled.Favorite,
    Icons.Filled.Bolt,
    Icons.Filled.ChangeHistory
)

/**
 * The tint for a wheel glyph. In colourblind mode we use the higher-separation
 * palette by position; otherwise the wheel's own stored colour.
 */
fun glyphColor(index: Int, colorblind: Boolean, hex: String): Color =
    if (colorblind) {
        colorblindFriendlyWheelColors[index % colorblindFriendlyWheelColors.size].toColorOrDefault(Color(0xFFE0555A))
    } else {
        hex.toColorOrDefault(Color(0xFFE0555A))
    }

/**
 * A wheel's marker: a colour dot normally, or a distinct symbol in colourblind
 * mode. [index] is the wheel's position, which chooses the symbol.
 */
@Composable
fun WheelGlyph(index: Int, colorblind: Boolean, hex: String, sizeDp: Int) {
    if (colorblind) {
        Icon(
            imageVector = wheelSymbols[index % wheelSymbols.size],
            contentDescription = null,
            tint = glyphColor(index, true, hex),
            modifier = Modifier.size(sizeDp.dp)
        )
    } else {
        Box(
            Modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
                .background(hex.toColorOrDefault(Color(0xFFE0555A)))
        )
    }
}
