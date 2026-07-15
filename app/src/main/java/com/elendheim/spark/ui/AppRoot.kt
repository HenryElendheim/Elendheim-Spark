package com.elendheim.spark.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elendheim.spark.SparkApp
import com.elendheim.spark.branding.ElendheimSplash
import com.elendheim.spark.settings.SparkSettings
import com.elendheim.spark.ui.collider.ColliderRoute
import com.elendheim.spark.ui.editor.EditorRoute
import com.elendheim.spark.ui.settings.SettingsRoute
import com.elendheim.spark.ui.theme.LocalSparkPalette
import com.elendheim.spark.ui.vault.VaultRoute

/** The four top-level destinations. */
private enum class Tab(val label: String, val icon: ImageVector, val desc: String) {
    Collide("Randomize", Icons.Filled.Bolt, "Randomize tab: roll new ideas"),
    Vault("Vault", Icons.Filled.Inventory2, "Vault tab: your saved ideas"),
    Editor("Editor", Icons.Filled.Tune, "Editor tab: curate decks and wheels"),
    Settings("Settings", Icons.Filled.Settings, "Settings tab")
}

/**
 * The app shell: shows the brand splash once, seeds the starter decks on first
 * run, then presents the four tabs. State-based tab switching keeps navigation
 * simple; the editor handles its own deeper drill-down internally.
 */
@Composable
fun AppRoot(settings: SparkSettings) {
    val palette = LocalSparkPalette.current
    val context = LocalContext.current
    val container = remember { (context.applicationContext as SparkApp).container }

    var showSplash by remember { mutableStateOf(true) }
    var tab by remember { mutableStateOf(Tab.Collide) }

    // First run: drop in the starter decks so the app sparks immediately.
    LaunchedEffect(Unit) {
        container.repository.seedIfEmpty(System.currentTimeMillis())
    }

    if (showSplash) {
        ElendheimSplash(
            appName = "Elendheim Spark",
            reduceMotion = settings.reduceMotion,
            onFinished = { showSplash = false }
        )
        return
    }

    Scaffold(
        bottomBar = {
            SparkBottomBar(current = tab, onSelect = { tab = it })
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
                .padding(innerPadding)
        ) {
            when (tab) {
                Tab.Collide -> ColliderRoute()
                Tab.Vault -> VaultRoute()
                Tab.Editor -> EditorRoute()
                Tab.Settings -> SettingsRoute()
            }
        }
    }
}

/** The bottom navigation bar. Labels are always shown, so no icon stands alone. */
@Composable
private fun SparkBottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    val palette = LocalSparkPalette.current
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface)
            .navigationBarsPadding()
            .height(64.dp)
    ) {
        Tab.entries.forEach { t ->
            val selected = t == current
            // The selected tab gets a solid accent pill so it is unmistakable
            // which screen you are on (the plain tint was too easy to miss).
            val onPill = Color(0xFF1A0B0C)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable { onSelect(t) }
                    .semantics {
                        contentDescription = if (selected) "${t.desc}, selected" else t.desc
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (selected) palette.accent else Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = t.icon,
                        contentDescription = null,
                        tint = if (selected) onPill else palette.onSurfaceMuted,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = t.label,
                        color = if (selected) onPill else palette.onSurfaceMuted,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
