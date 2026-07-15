package com.elendheim.spark.branding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * The drop-in Elendheim splash. Shows the spark mark and app name on the dark
 * brand background for a beat, then calls [onFinished].
 *
 * When motion is reduced it skips the animation entirely and simply shows the
 * mark for a shorter moment -> respectful of motion sensitivity, and faster for
 * anyone who prefers it.
 */
@Composable
fun ElendheimSplash(
    appName: String,
    reduceMotion: Boolean,
    onFinished: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 450),
        label = "splashScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 350),
        label = "splashAlpha"
    )

    LaunchedEffect(Unit) {
        visible = true
        // Hold briefly so the brand registers, then move on. Shorter with reduced motion.
        delay(if (reduceMotion) 350L else 1100L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ElendheimBrand.Background),
        contentAlignment = Alignment.Center
    ) {
        // Just the logo, matching the launcher icon. No title, no subtitle.
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.scale(scale).alpha(alpha)) {
                ElendheimSparkMark(sizeDp = 128)
            }
        }
    }
}
