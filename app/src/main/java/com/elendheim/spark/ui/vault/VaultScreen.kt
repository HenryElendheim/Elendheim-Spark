package com.elendheim.spark.ui.vault

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.StarBorder
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elendheim.spark.model.SavedCollision
import com.elendheim.spark.ui.common.asInline
import com.elendheim.spark.ui.common.asSpoken
import com.elendheim.spark.ui.theme.LocalSparkPalette

/** Wires the vault screen to its view-model and hosts the detail editor. */
@Composable
fun VaultRoute() {
    val vm: VaultViewModel = viewModel(factory = VaultViewModel.Factory)
    val state by vm.uiState.collectAsState()
    var editing by remember { mutableStateOf<SavedCollision?>(null) }

    VaultScreen(
        state = state,
        onQuery = vm::setQuery,
        onSelectAll = vm::selectAll,
        onSelectFavorites = vm::selectFavorites,
        onDeckFilter = vm::setDeckFilter,
        onOpen = { editing = it },
        onDeleteMany = { vm.deleteMany(it) }
    )

    // Keep the open editor pointed at the freshest copy of its item.
    val current = editing?.let { e -> state.items.firstOrNull { it.id == e.id } ?: e }
    if (current != null) {
        VaultDetailDialog(
            item = current,
            deckName = state.deckNames[current.deckId] ?: "unknown",
            onDismiss = { editing = null },
            onToggleFavorite = { vm.toggleFavorite(current) },
            onSaveTitle = { vm.updateTitle(current, it) },
            onSaveNote = { vm.updateNote(current, it) },
            onSaveTags = { vm.updateTags(current, it) },
            onDelete = {
                vm.delete(current)
                editing = null
            }
        )
    }
}

/** The vault list: search, filters, and every saved spark as a card. */
@Composable
fun VaultScreen(
    state: VaultUiState,
    onQuery: (String) -> Unit,
    onSelectAll: () -> Unit,
    onSelectFavorites: () -> Unit,
    onDeckFilter: (String?) -> Unit,
    onOpen: (SavedCollision) -> Unit,
    onDeleteMany: (List<SavedCollision>) -> Unit
) {
    val palette = LocalSparkPalette.current
    var showDecks by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confirmDeleteMany by remember { mutableStateOf(false) }

    // Leaving selection mode always clears the selection.
    fun exitSelection() { selectionMode = false; selectedIds = emptySet() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Vault", color = palette.onBackground, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (state.totalCount > 0) {
                TextButton(onClick = { if (selectionMode) exitSelection() else selectionMode = true }) {
                    Text(if (selectionMode) "Cancel" else "Select", color = palette.accent)
                }
            }
        }

        // Selection action bar: how many, delete, done.
        if (selectionMode) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surfaceElevated)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("${selectedIds.size} selected", color = palette.onBackground, fontSize = 14.sp, modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { selectedIds = state.items.map { it.id }.toSet() }
                ) { Text("Select all", color = palette.onBackground) }
                TextButton(
                    enabled = selectedIds.isNotEmpty(),
                    onClick = { confirmDeleteMany = true }
                ) { Text("Delete", color = if (selectedIds.isEmpty()) palette.onSurfaceMuted else palette.accent) }
            }
            Spacer(Modifier.size(8.dp))
        }

        Spacer(Modifier.size(12.dp))

        // Search.
        OutlinedTextField(
            value = state.query,
            onValueChange = onQuery,
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text("Search ideas, notes, tags") },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Search your saved ideas" }
        )

        Spacer(Modifier.size(10.dp))

        // Filters: All, Favorites, and a Decks button that opens a searchable
        // deck list. Only one filter is active at a time.
        val selectedDeckName = state.decks.firstOrNull { it.id == state.deckFilter }?.name
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip("All", state.deckFilter == null && !state.favoritesOnly) { onSelectAll() }
            FilterChip("Favorites", state.favoritesOnly) { onSelectFavorites() }
            FilterChip(
                label = selectedDeckName ?: "Decks",
                selected = state.deckFilter != null,
                trailingChevron = true
            ) { showDecks = true }
        }

        Spacer(Modifier.size(12.dp))

        if (state.items.isEmpty()) {
            EmptyVault(hasAny = state.totalCount > 0)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.items, key = { it.id }) { item ->
                    val isSelected = item.id in selectedIds
                    VaultCard(
                        item = item,
                        deckName = state.deckNames[item.deckId] ?: "",
                        selectionMode = selectionMode,
                        selected = isSelected,
                        onClick = {
                            if (selectionMode) {
                                selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                            } else {
                                onOpen(item)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showDecks) {
        DecksFilterDialog(
            decks = state.decks.map { it.id to it.name },
            selectedId = state.deckFilter,
            onPick = { onDeckFilter(it); showDecks = false },
            onDismiss = { showDecks = false }
        )
    }
    if (confirmDeleteMany) {
        val toDelete = state.items.filter { it.id in selectedIds }
        Dialog(onDismissRequest = { confirmDeleteMany = false }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.surface)
                    .padding(20.dp)
            ) {
                Text("Delete ${toDelete.size} saved ${if (toDelete.size == 1) "idea" else "ideas"}?", color = palette.onBackground, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.size(6.dp))
                Text("This can't be undone.", color = palette.onSurfaceMuted, fontSize = 14.sp)
                Spacer(Modifier.size(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { confirmDeleteMany = false }) { Text("Cancel", color = palette.onSurfaceMuted) }
                    TextButton(onClick = {
                        onDeleteMany(toDelete)
                        confirmDeleteMany = false
                        exitSelection()
                    }) { Text("Delete", color = palette.accent) }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, trailingChevron: Boolean = false, onClick: () -> Unit) {
    val palette = LocalSparkPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) palette.surfaceElevated else Color.Transparent)
            .border(1.dp, if (selected) palette.accent else palette.outline, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .semantics { contentDescription = if (selected) "$label filter, on" else "$label filter, off" }
    ) {
        Text(
            text = label,
            color = if (selected) palette.onBackground else palette.onSurfaceMuted,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (trailingChevron) {
            Spacer(Modifier.size(4.dp))
            Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = palette.onSurfaceMuted, modifier = Modifier.size(18.dp))
        }
    }
}

/** A searchable deck list for filtering the vault. "All decks" clears the filter. */
@Composable
private fun DecksFilterDialog(
    decks: List<Pair<String, String>>,
    selectedId: String?,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalSparkPalette.current
    var query by remember { mutableStateOf("") }
    val filtered = decks.filter { it.second.contains(query.trim(), ignoreCase = true) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.surface)
                .padding(20.dp)
        ) {
            Text("Filter by deck", color = palette.onBackground, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.size(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Search decks") },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Search decks" }
            )
            Spacer(Modifier.size(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DeckFilterRow("All decks", selectedId == null) { onPick(null) }
                filtered.forEach { (id, name) ->
                    DeckFilterRow(name, selectedId == id) { onPick(id) }
                }
                if (filtered.isEmpty()) {
                    Text("No decks match.", color = palette.onSurfaceMuted, fontSize = 14.sp, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DeckFilterRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalSparkPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) palette.surfaceElevated else Color.Transparent)
            .clickable { onClick() }
            .padding(12.dp)
            .semantics { contentDescription = if (selected) "$label, selected" else label }
    ) {
        Text(label, color = palette.onBackground, fontSize = 16.sp, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun VaultCard(
    item: SavedCollision,
    deckName: String,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalSparkPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surface)
            .border(1.dp, if (selected) palette.accent else palette.outline, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .semantics {
                contentDescription = if (selectionMode) {
                    "${savedName(item, deckName)}. ${if (selected) "Selected" else "Not selected"}. Tap to toggle."
                } else {
                    "${savedName(item, deckName)}. ${item.picks.asSpoken()}. Tap to open."
                }
            }
    ) {
        // A soft-red spine down the left edge -> gives the card a little identity.
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(palette.accent)
        )
        if (selectionMode) {
            Icon(
                imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = if (selected) palette.accent else palette.onSurfaceMuted,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(22.dp)
            )
        }
        Column(modifier = Modifier.padding(14.dp)) {
            // Name (custom title, or "Deck #n") plus the favourite star.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = savedName(item, deckName),
                    color = palette.onBackground,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                if (item.favorite) {
                    Icon(Icons.Filled.Star, contentDescription = "Favorite", tint = palette.accent, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.size(6.dp))
            // The idea itself.
            Text(
                text = item.picks.asInline(),
                color = palette.onBackground,
                fontSize = 15.sp,
                maxLines = 2
            )
            if (item.tags.isNotEmpty()) {
                Spacer(Modifier.size(6.dp))
                Text(
                    text = item.tags.joinToString(" ") { "#$it" },
                    color = palette.accent,
                    fontSize = 13.sp
                )
            }
            if (item.note.isNotBlank()) {
                Spacer(Modifier.size(6.dp))
                Text(text = item.note, color = palette.onSurfaceMuted, fontSize = 14.sp, maxLines = 2)
            }
        }
    }
}

/** The readable name for a saved idea: its custom title, or "Deck #number". */
private fun savedName(item: SavedCollision, deckName: String): String {
    if (item.title.isNotBlank()) return item.title
    val base = deckName.ifBlank { "Saved" }
    return if (item.saveNumber > 0) "$base #${item.saveNumber}" else base
}

@Composable
private fun EmptyVault(hasAny: Boolean) {
    val palette = LocalSparkPalette.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = if (hasAny) "Nothing matches your search yet."
            else "No saved sparks yet.\nRoll on the Randomize tab and save the good ones.",
            color = palette.onSurfaceMuted,
            fontSize = 16.sp,
            modifier = Modifier.padding(24.dp)
        )
    }
}

/**
 * The read-it-big detail editor for a saved spark: favourite, note, tags,
 * share as plain text, or delete.
 */
@Composable
private fun VaultDetailDialog(
    item: SavedCollision,
    deckName: String,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSaveTitle: (String) -> Unit,
    onSaveNote: (String) -> Unit,
    onSaveTags: (String) -> Unit,
    onDelete: () -> Unit
) {
    val palette = LocalSparkPalette.current
    val context = LocalContext.current
    var title by remember(item.id) { mutableStateOf(item.title) }
    var note by remember(item.id) { mutableStateOf(item.note) }
    var tags by remember(item.id) { mutableStateOf(item.tags.joinToString(", ")) }
    var confirmDelete by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.surface)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Name + which deck and save number this is.
            Text(
                text = savedName(item, deckName),
                color = palette.onBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.size(2.dp))
            Text(
                text = buildString {
                    append(deckName.ifBlank { "Saved" })
                    if (item.saveNumber > 0) append("  -  save #${item.saveNumber}")
                },
                color = palette.onSurfaceMuted,
                fontSize = 13.sp
            )

            Spacer(Modifier.size(12.dp))

            // The idea itself.
            Text(
                text = item.picks.asInline(),
                color = palette.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.size(16.dp))

            // Rename.
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; onSaveTitle(it) },
                singleLine = true,
                label = { Text("Name (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.size(14.dp))

            // Favourite toggle.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onToggleFavorite() }
                    .semantics {
                        contentDescription = if (item.favorite) "Favorited. Tap to unfavorite." else "Tap to favorite."
                    }
            ) {
                Icon(
                    imageVector = if (item.favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = null,
                    tint = palette.accent
                )
                Spacer(Modifier.size(8.dp))
                Text(if (item.favorite) "Favorite" else "Mark favorite", color = palette.onBackground)
            }

            Spacer(Modifier.size(14.dp))

            // Note: what you want to make of it.
            OutlinedTextField(
                value = note,
                onValueChange = { note = it; onSaveNote(it) },
                label = { Text("Note - what you'd make of it") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.size(10.dp))

            // Tags, comma-separated.
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it; onSaveTags(it) },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default,
                label = { Text("Tags (comma separated)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.size(16.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        val body = buildString {
                            append(item.picks.asInline())
                            if (item.note.isNotBlank()) append("\n\n").append(item.note)
                        }
                        putExtra(Intent.EXTRA_TEXT, body)
                    }
                    context.startActivity(Intent.createChooser(share, "Share idea"))
                }) { Text("Share", color = palette.accent) }

                TextButton(onClick = { confirmDelete = true }) {
                    Text("Delete", color = palette.accent)
                }

                TextButton(onClick = onDismiss) { Text("Done", color = palette.onBackground) }
            }

            // Confirm before deleting -> never lose a spark by accident.
            if (confirmDelete) {
                Spacer(Modifier.size(8.dp))
                Text("Delete this idea for good?", color = palette.onBackground)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDelete) { Text("Yes, delete", color = palette.accent) }
                    TextButton(onClick = { confirmDelete = false }) { Text("Keep it", color = palette.onBackground) }
                }
            }
        }
    }
}
