package com.elendheim.spark.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elendheim.spark.branding.ElendheimSparkMark
import com.elendheim.spark.data.ExportImport
import com.elendheim.spark.ui.theme.LocalSparkPalette

/** Wires the settings screen to its view-model and the file pickers. */
@Composable
fun SettingsRoute() {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    // File pickers (Storage Access Framework). The user chooses the location;
    // we only touch the one document they pick.
    val createJson = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { vm.exportJson(it, System.currentTimeMillis(), ::toast) } }

    val createMarkdown = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri -> uri?.let { vm.exportMarkdown(it, System.currentTimeMillis(), ::toast) } }

    // Import mode is chosen before the picker opens, then applied to the result.
    var pendingMode by remember { mutableStateOf(ExportImport.ImportMode.MERGE) }
    val openJson = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.importJson(it, pendingMode, ::toast) } }

    SettingsScreen(
        state = state,
        onExportJson = { createJson.launch(vm.suggestedFileName(System.currentTimeMillis())) },
        onExportMarkdown = { createMarkdown.launch("elendheim-spark-vault.md") },
        onImport = { mode ->
            pendingMode = mode
            openJson.launch(arrayOf("application/json", "text/plain", "*/*"))
        },
        callbacks = SettingsCallbacks(
            setTextScale = vm::setTextScale,
            setHighContrast = vm::setHighContrast,
            setReduceMotion = vm::setReduceMotion,
            setColorblind = vm::setColorblind,
            setShowLabels = vm::setShowLabels,
            setLargerTargets = vm::setLargerTargets,
            setHaptics = vm::setHaptics,
            setAnnounce = vm::setAnnounce,
            setLineByLine = vm::setLineByLine,
            setWeighting = vm::setWeighting,
            setDefaultDeck = vm::setDefaultDeck
        )
    )
}

/** Bundled setters, to keep the screen signature readable. */
data class SettingsCallbacks(
    val setTextScale: (Float) -> Unit,
    val setHighContrast: (Boolean) -> Unit,
    val setReduceMotion: (Boolean) -> Unit,
    val setColorblind: (Boolean) -> Unit,
    val setShowLabels: (Boolean) -> Unit,
    val setLargerTargets: (Boolean) -> Unit,
    val setHaptics: (Boolean) -> Unit,
    val setAnnounce: (Boolean) -> Unit,
    val setLineByLine: (Boolean) -> Unit,
    val setWeighting: (Boolean) -> Unit,
    val setDefaultDeck: (String?) -> Unit
)

/** The full, grouped settings menu. Scrolls; every control is labelled. */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onExportJson: () -> Unit,
    onExportMarkdown: () -> Unit,
    onImport: (ExportImport.ImportMode) -> Unit,
    callbacks: SettingsCallbacks
) {
    val palette = LocalSparkPalette.current
    val s = state.settings
    var showDefaultDeck by remember { mutableStateOf(false) }
    var showImportChooser by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text("Settings", color = palette.onBackground, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)

        // --- Accessibility ---
        Group("Accessibility") {
            Column {
                Text("Text size", color = palette.onBackground, fontSize = 15.sp)
                Text("Scales on top of your system font size.", color = palette.onSurfaceMuted, fontSize = 12.sp)
                Slider(
                    value = s.textScale,
                    onValueChange = callbacks.setTextScale,
                    valueRange = 0.8f..1.6f,
                    steps = 7,
                    modifier = Modifier.semantics { contentDescription = "Text size" }
                )
            }
            SwitchRow("High contrast", "Brighter text and firmer borders.", s.highContrast, callbacks.setHighContrast)
            SwitchRow("Reduce motion", "Instant reveal, no collide animation.", s.reduceMotion, callbacks.setReduceMotion)
            SwitchRow("Colourblind-friendly colours", "Higher-separation wheel colours.", s.colorblindPalette, callbacks.setColorblind)
            SwitchRow("Always show wheel labels", "Never rely on colour alone.", s.showWheelLabels, callbacks.setShowLabels)
            SwitchRow("Larger tap targets", "Grow buttons and toggles.", s.largerTapTargets, callbacks.setLargerTargets)
            SwitchRow("Haptics", "Vibrate on collide and save.", s.haptics, callbacks.setHaptics)
            SwitchRow("Announce result", "Speak the new idea for screen readers.", s.announceResult, callbacks.setAnnounce)
        }

        // --- Your ideas & content ---
        Group("Your ideas & content") {
            SwitchRow("Use entry weighting", "Honour per-entry weights when rolling.", s.weightingEnabled, callbacks.setWeighting)
            ActionRow("Default deck", currentDefaultDeckName(state)) { showDefaultDeck = true }
        }

        // --- Data ---
        Group("Data") {
            ActionRow("Export to your files", "One JSON file: every deck, wheel and saved idea.") { onExportJson() }
            ActionRow("Import from your files", "Merge a file in, or replace everything.") { showImportChooser = true }
            ActionRow("Export vault as text", "A readable Markdown copy of your saved sparks.") { onExportMarkdown() }
        }

        // --- About ---
        Group("About") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ElendheimSparkMark(sizeDp = 40)
                Spacer(Modifier.size(12.dp))
                Column {
                    Text("Elendheim Spark", color = palette.onBackground, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("A mix-and-match idea generator you curate yourself.", color = palette.onSurfaceMuted, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.size(10.dp))
            Text(
                "No network, no accounts, no tracking. Your ideas stay on this device unless you export them yourself.",
                color = palette.onSurfaceMuted,
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.size(24.dp))
    }

    // --- Dialogs ---
    if (showDefaultDeck) {
        DefaultDeckDialog(
            state = state,
            onPick = { id -> callbacks.setDefaultDeck(id); showDefaultDeck = false },
            onDismiss = { showDefaultDeck = false }
        )
    }
    if (showImportChooser) {
        ImportModeDialog(
            onMerge = { onImport(ExportImport.ImportMode.MERGE); showImportChooser = false },
            onReplace = { onImport(ExportImport.ImportMode.REPLACE_ALL); showImportChooser = false },
            onDismiss = { showImportChooser = false }
        )
    }
}

// --- Building blocks ---

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    val palette = LocalSparkPalette.current
    Spacer(Modifier.size(20.dp))
    Text(title.uppercase(), color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.size(8.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surface)
            .border(1.dp, palette.outline, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) { content() }
}

@Composable
private fun SwitchRow(label: String, desc: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val palette = LocalSparkPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$label. $desc" }
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = palette.onBackground, fontSize = 15.sp)
            Text(desc, color = palette.onSurfaceMuted, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.onBackground,
                checkedTrackColor = palette.accent,
                uncheckedTrackColor = palette.surfaceElevated
            )
        )
    }
}

@Composable
private fun ActionRow(label: String, desc: String, onClick: () -> Unit) {
    val palette = LocalSparkPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .semantics { contentDescription = "$label. $desc" }
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = palette.onBackground, fontSize = 15.sp)
            Text(desc, color = palette.onSurfaceMuted, fontSize = 12.sp)
        }
    }
}

private fun currentDefaultDeckName(state: SettingsUiState): String {
    val id = state.settings.defaultDeckId ?: return "First deck"
    return state.decks.firstOrNull { it.id == id }?.name ?: "First deck"
}

@Composable
private fun DefaultDeckDialog(
    state: SettingsUiState,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalSparkPalette.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.surface)
                .padding(20.dp)
        ) {
            Text("Default deck", color = palette.onBackground, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.size(12.dp))
            DeckPickRow("First deck (default)", state.settings.defaultDeckId == null) { onPick(null) }
            state.decks.forEach { deck ->
                DeckPickRow(deck.name, state.settings.defaultDeckId == deck.id) { onPick(deck.id) }
            }
            Spacer(Modifier.size(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Close", color = palette.onBackground) }
            }
        }
    }
}

@Composable
private fun DeckPickRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalSparkPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp)
    ) {
        Text(label, color = palette.onBackground, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = palette.accent, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ImportModeDialog(onMerge: () -> Unit, onReplace: () -> Unit, onDismiss: () -> Unit) {
    val palette = LocalSparkPalette.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.surface)
                .padding(20.dp)
        ) {
            Text("Import", color = palette.onBackground, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.size(8.dp))
            Text("Merge keeps what you have and adds anything new. Replace all wipes this device and loads the file exactly.", color = palette.onSurfaceMuted, fontSize = 13.sp)
            Spacer(Modifier.size(16.dp))
            TextButton(onClick = onMerge) { Text("Merge in", color = palette.accent) }
            TextButton(onClick = onReplace) { Text("Replace everything", color = palette.accent) }
            Spacer(Modifier.size(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = palette.onSurfaceMuted) }
            }
        }
    }
}
