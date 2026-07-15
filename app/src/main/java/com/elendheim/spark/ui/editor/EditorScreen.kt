package com.elendheim.spark.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elendheim.spark.model.Deck
import com.elendheim.spark.model.Entry
import com.elendheim.spark.model.Wheel
import com.elendheim.spark.ui.common.WheelGlyph
import com.elendheim.spark.ui.common.toColorOrDefault
import com.elendheim.spark.ui.theme.LocalSparkPalette

private val editorPalette = listOf(
    "#E0555A", "#4FA6D9", "#E0A44F", "#6FBF73", "#B588E0", "#E07FB0"
)

/**
 * The editor. It drives its own three-level drill-down internally (decks ->
 * one deck's wheels -> one wheel's entries) with simple local state, so the top
 * navigation stays a flat set of tabs.
 */
@Composable
fun EditorRoute() {
    val vm: EditorViewModel = viewModel(factory = EditorViewModel.Factory)
    val state by vm.uiState.collectAsState()

    var openDeckId by remember { mutableStateOf<String?>(null) }
    var openWheelId by remember { mutableStateOf<String?>(null) }

    val openDeck = state.decks.firstOrNull { it.id == openDeckId }
    val openWheel = openWheelId?.let { state.wheelsById[it] }

    when {
        openDeck != null && openWheel != null -> WheelDetail(
            wheel = openWheel,
            maxEntries = state.maxEntries,
            onBack = { openWheelId = null },
            onRename = { vm.renameWheel(openWheel, it) },
            onRecolor = { vm.recolorWheel(openWheel, it) },
            onAddEntry = { text -> vm.addEntry(openWheel, text) },
            onBulkAdd = { vm.bulkAddEntries(openWheel, it) },
            onEditEntry = { id, text -> vm.editEntry(openWheel, id, text) },
            onDeleteEntry = { vm.deleteEntry(openWheel, it) },
            onMoveEntry = { id, up -> vm.moveEntry(openWheel, id, up) }
        )

        openDeck != null -> DeckDetail(
            deck = openDeck,
            wheels = openDeck.wheelIds.mapNotNull { state.wheelsById[it] },
            colorblind = state.colorblind,
            maxWheels = state.maxWheels,
            onBack = { openDeckId = null },
            onRenameDeck = { vm.renameDeck(openDeck, it) },
            onAddWheel = { vm.addWheel(openDeck, it) },
            onOpenWheel = { openWheelId = it },
            onDeleteWheel = { vm.deleteWheel(openDeck, it) },
            onMoveWheel = { id, up -> vm.moveWheel(openDeck, id, up) }
        )

        else -> DeckList(
            decks = state.decks,
            wheelCount = { it.wheelIds.size },
            onOpen = { openDeckId = it },
            onCreate = { vm.createDeck(it) },
            onDelete = { vm.deleteDeck(it) }
        )
    }
}

// --- Level 1: decks ---

@Composable
private fun DeckList(
    decks: List<Deck>,
    wheelCount: (Deck) -> Int,
    onOpen: (String) -> Unit,
    onCreate: (String) -> Unit,
    onDelete: (Deck) -> Unit
) {
    val palette = LocalSparkPalette.current
    var creating by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Deck?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Header(title = "Editor", actionLabel = "New deck") { creating = true }
        Spacer(Modifier.size(12.dp))

        if (decks.isEmpty()) {
            EmptyHint("No decks yet. Create one to start building wheels.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(decks, key = { it.id }) { deck ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.surface)
                            .border(1.dp, palette.outline, RoundedCornerShape(14.dp))
                            .clickable { onOpen(deck.id) }
                            .padding(16.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(deck.name, color = palette.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                            Text("${wheelCount(deck)} wheels", color = palette.onSurfaceMuted, fontSize = 13.sp)
                        }
                        IconAction(Icons.Filled.Delete, "Delete deck ${deck.name}") { confirmDelete = deck }
                    }
                }
            }
        }
    }

    if (creating) {
        TextEntryDialog(
            title = "New deck",
            label = "Deck name",
            initial = "",
            onConfirm = { onCreate(it); creating = false },
            onDismiss = { creating = false }
        )
    }
    confirmDelete?.let { deck ->
        ConfirmDialog(
            message = "Delete \"${deck.name}\" and its wheels? Saved ideas are kept.",
            confirmLabel = "Delete",
            onConfirm = { onDelete(deck); confirmDelete = null },
            onDismiss = { confirmDelete = null }
        )
    }
}

// --- Level 2: one deck's wheels ---

@Composable
private fun DeckDetail(
    deck: Deck,
    wheels: List<Wheel>,
    colorblind: Boolean,
    maxWheels: Int,
    onBack: () -> Unit,
    onRenameDeck: (String) -> Unit,
    onAddWheel: (String) -> Unit,
    onOpenWheel: (String) -> Unit,
    onDeleteWheel: (Wheel) -> Unit,
    onMoveWheel: (String, Boolean) -> Unit
) {
    val palette = LocalSparkPalette.current
    var addingWheel by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Wheel?>(null) }
    val canAdd = wheels.size < maxWheels

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        BackHeader(title = deck.name, onBack = onBack, editLabel = "Rename deck") { renaming = true }
        Spacer(Modifier.size(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { if (canAdd) addingWheel = true }, enabled = canAdd) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = if (canAdd) palette.accent else palette.onSurfaceMuted)
                Spacer(Modifier.size(4.dp))
                Text("Add wheel", color = if (canAdd) palette.accent else palette.onSurfaceMuted)
            }
            Spacer(Modifier.weight(1f))
            Text("${wheels.size} / $maxWheels", color = palette.onSurfaceMuted, fontSize = 13.sp)
        }
        if (!canAdd) {
            Text("Wheel limit reached. Raise it in Settings.", color = palette.onSurfaceMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.size(8.dp))

        if (wheels.isEmpty()) {
            EmptyHint("No wheels yet. Add a wheel, then fill it with entries.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(wheels, key = { it.id }) { wheel ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.surface)
                            .border(1.dp, palette.outline, RoundedCornerShape(14.dp))
                            .clickable { onOpenWheel(wheel.id) }
                            .padding(14.dp)
                    ) {
                        WheelGlyph(index = wheels.indexOf(wheel), colorblind = colorblind, hex = wheel.colorHex, sizeDp = 14)
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(wheel.name, color = palette.onBackground, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                            Text("${wheel.entries.size} entries", color = palette.onSurfaceMuted, fontSize = 13.sp)
                        }
                        IconAction(Icons.Filled.ArrowUpward, "Move ${wheel.name} up") { onMoveWheel(wheel.id, true) }
                        IconAction(Icons.Filled.ArrowDownward, "Move ${wheel.name} down") { onMoveWheel(wheel.id, false) }
                        IconAction(Icons.Filled.Delete, "Delete wheel ${wheel.name}") { confirmDelete = wheel }
                    }
                }
            }
        }
    }

    if (addingWheel) {
        TextEntryDialog(
            title = "New wheel",
            label = "Wheel name (Domain, Mood, Twist...)",
            initial = "",
            onConfirm = { onAddWheel(it); addingWheel = false },
            onDismiss = { addingWheel = false }
        )
    }
    if (renaming) {
        TextEntryDialog(
            title = "Rename deck",
            label = "Deck name",
            initial = deck.name,
            onConfirm = { onRenameDeck(it); renaming = false },
            onDismiss = { renaming = false }
        )
    }
    confirmDelete?.let { wheel ->
        ConfirmDialog(
            message = "Delete the \"${wheel.name}\" wheel and its entries?",
            confirmLabel = "Delete",
            onConfirm = { onDeleteWheel(wheel); confirmDelete = null },
            onDismiss = { confirmDelete = null }
        )
    }
}

// --- Level 3: one wheel's entries ---

@Composable
private fun WheelDetail(
    wheel: Wheel,
    maxEntries: Int,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onRecolor: (String) -> Unit,
    onAddEntry: (String) -> Unit,
    onBulkAdd: (String) -> Unit,
    onEditEntry: (String, String) -> Unit,
    onDeleteEntry: (String) -> Unit,
    onMoveEntry: (String, Boolean) -> Unit
) {
    val palette = LocalSparkPalette.current
    var quickAdd by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf(false) }
    var bulk by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Entry?>(null) }
    val canAdd = wheel.entries.size < maxEntries
    val shownEntries = wheel.entries.filter { it.text.contains(search.trim(), ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        BackHeader(title = wheel.name, onBack = onBack, editLabel = "Rename wheel") { renaming = true }

        // Colour picker: the chip colour, chosen from a readable palette.
        Spacer(Modifier.size(8.dp))
        Text("Chip colour", color = palette.onSurfaceMuted, fontSize = 13.sp)
        Spacer(Modifier.size(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            editorPalette.forEach { hex ->
                val selected = hex.equals(wheel.colorHex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(hex.toColorOrDefault(palette.accent))
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) palette.onBackground else palette.outline,
                            shape = CircleShape
                        )
                        .clickable { onRecolor(hex) }
                        .semantics { contentDescription = "Set chip colour" }
                )
            }
        }

        Spacer(Modifier.size(14.dp))

        // Quick add + bulk paste -> curation must be frictionless.
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = quickAdd,
                onValueChange = { quickAdd = it },
                singleLine = true,
                enabled = canAdd,
                placeholder = { Text(if (canAdd) "Add an entry" else "Entry limit reached") },
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "New entry text" }
            )
            Spacer(Modifier.size(8.dp))
            TextButton(
                enabled = canAdd,
                onClick = { if (quickAdd.isNotBlank()) { onAddEntry(quickAdd); quickAdd = "" } }
            ) { Text("Add", color = if (canAdd) palette.accent else palette.onSurfaceMuted) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { bulk = true }, enabled = canAdd) {
                Text("Bulk paste (one per line)", color = if (canAdd) palette.accent else palette.onSurfaceMuted)
            }
            Spacer(Modifier.weight(1f))
            Text("${wheel.entries.size} / $maxEntries", color = palette.onSurfaceMuted, fontSize = 13.sp)
        }

        Spacer(Modifier.size(8.dp))

        // Search within this wheel's entries.
        if (wheel.entries.isNotEmpty()) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Search entries") },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Search this wheel's entries" }
            )
            Spacer(Modifier.size(8.dp))
        }

        if (wheel.entries.isEmpty()) {
            EmptyHint("No entries yet. Add a few, or bulk-paste a list.")
        } else if (shownEntries.isEmpty()) {
            EmptyHint("No entries match your search.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shownEntries, key = { it.id }) { entry ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.surface)
                            .border(1.dp, palette.outline, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(entry.text, color = palette.onBackground, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        IconAction(Icons.Filled.ArrowUpward, "Move ${entry.text} up") { onMoveEntry(entry.id, true) }
                        IconAction(Icons.Filled.ArrowDownward, "Move ${entry.text} down") { onMoveEntry(entry.id, false) }
                        IconAction(Icons.Filled.Edit, "Edit ${entry.text}") { editing = entry }
                        IconAction(Icons.Filled.Delete, "Delete ${entry.text}") { onDeleteEntry(entry.id) }
                    }
                }
            }
        }
    }

    if (renaming) {
        TextEntryDialog(
            title = "Rename wheel",
            label = "Wheel name",
            initial = wheel.name,
            onConfirm = { onRename(it); renaming = false },
            onDismiss = { renaming = false }
        )
    }
    if (bulk) {
        BulkAddDialog(
            onConfirm = { onBulkAdd(it); bulk = false },
            onDismiss = { bulk = false }
        )
    }
    editing?.let { entry ->
        EditEntryDialog(
            entry = entry,
            onConfirm = { text -> onEditEntry(entry.id, text); editing = null },
            onDismiss = { editing = null }
        )
    }
}

// --- Small shared building blocks ---

@Composable
private fun Header(title: String, actionLabel: String, onAction: () -> Unit) {
    val palette = LocalSparkPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = palette.onBackground, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        TextButton(onClick = onAction) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = palette.accent)
            Spacer(Modifier.size(4.dp))
            Text(actionLabel, color = palette.accent)
        }
    }
}

@Composable
private fun BackHeader(title: String, onBack: () -> Unit, editLabel: String, onEdit: () -> Unit) {
    val palette = LocalSparkPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconAction(Icons.AutoMirrored.Filled.ArrowBack, "Back") { onBack() }
        Spacer(Modifier.size(4.dp))
        Text(title, color = palette.onBackground, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        IconAction(Icons.Filled.Edit, editLabel) { onEdit() }
    }
}

@Composable
private fun IconAction(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    val palette = LocalSparkPalette.current
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable { onClick() }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = palette.onSurfaceMuted, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun EmptyHint(text: String) {
    val palette = LocalSparkPalette.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = palette.onSurfaceMuted, fontSize = 15.sp, modifier = Modifier.padding(24.dp))
    }
}

@Composable
private fun TextEntryDialog(
    title: String,
    label: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalSparkPalette.current
    var text by remember { mutableStateOf(initial) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.surface)
                .padding(20.dp)
        ) {
            Text(title, color = palette.onBackground, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.size(12.dp))
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.size(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = palette.onSurfaceMuted) }
                TextButton(onClick = { onConfirm(text) }) { Text("Save", color = palette.accent) }
            }
        }
    }
}

@Composable
private fun BulkAddDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val palette = LocalSparkPalette.current
    var text by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.surface)
                .padding(20.dp)
        ) {
            Text("Bulk add", color = palette.onBackground, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.size(4.dp))
            Text("One entry per line. Paste a whole list at once.", color = palette.onSurfaceMuted, fontSize = 13.sp)
            Spacer(Modifier.size(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Entries") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
            Spacer(Modifier.size(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = palette.onSurfaceMuted) }
                TextButton(onClick = { onConfirm(text) }) { Text("Add all", color = palette.accent) }
            }
        }
    }
}

@Composable
private fun EditEntryDialog(entry: Entry, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val palette = LocalSparkPalette.current
    var text by remember { mutableStateOf(entry.text) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.surface)
                .padding(20.dp)
        ) {
            Text("Edit entry", color = palette.onBackground, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.size(12.dp))
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Text") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.size(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = palette.onSurfaceMuted) }
                TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text("Save", color = palette.accent) }
            }
        }
    }
}

@Composable
private fun ConfirmDialog(message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val palette = LocalSparkPalette.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.surface)
                .padding(20.dp)
        ) {
            Text(message, color = palette.onBackground, fontSize = 16.sp)
            Spacer(Modifier.size(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = palette.onSurfaceMuted) }
                TextButton(onClick = onConfirm) { Text(confirmLabel, color = palette.accent) }
            }
        }
    }
}
