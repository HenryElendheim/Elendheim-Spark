package com.elendheim.spark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.elendheim.spark.settings.SparkSettings
import com.elendheim.spark.ui.AppRoot
import com.elendheim.spark.ui.theme.SparkTheme

/**
 * The single Activity. It reads the live settings, applies the theme (dark
 * always, plus high-contrast and text-scale when chosen), and hands off to the
 * Compose navigation in [AppRoot]. Everything else is Compose.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsRepo = (application as SparkApp).container.settings

        setContent {
            val settings: SparkSettings by settingsRepo.settings
                .collectAsState(initial = SparkSettings())

            SparkTheme(
                highContrast = settings.highContrast,
                textScale = settings.textScale
            ) {
                AppRoot(settings = settings)
            }
        }
    }
}
