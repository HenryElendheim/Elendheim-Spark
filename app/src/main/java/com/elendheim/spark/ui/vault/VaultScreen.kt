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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
        onDeckFilter = vm::setDeckFilter,
        onToggleFavoritesOnly = vm::toggleFavoritesOnly,
        onOpen = { editing = it }
    )

    // Keep the open editor pointed at the freshest copy of its item.
    val current = editing?.let { e -> state.items.firstOrNull { it.id == e.id } ?: e }
    if (current != null) {
        VaultDetailDialog(
            item = current,
            deckName = state.deckNames[current.deckId] ?: "unknown",
            onDismiss = { editing = null },
            onToggleFavorite = { vm.toggleFavorite(current) },
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
    onDeckFilter: (String?) -> Unit,
    onToggleFavoritesOnly: () -> Unit,
    onOpen: (SavedCollision) -> Unit
) {
    val palette = LocalSparkPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text("Vault", color = palette.onBackground, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
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

        // Filters: deck chips + favourites toggle.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip("All", state.deckFilter == null) { onDeckFilter(null) }
            state.decks.forEach { d ->
                FilterChip(d.name, state.deckFilter == d.id) { onDeckFilter(d.id) }
            }
            FilterChip(
                label = "Favorites",
                selected = state.favoritesOnly,
                onClick = onToggleFavoritesOnly
            )
        }

        Spacer(Modifier.size(12.dp))

        if (state.items.isEmpty()) {
            EmptyVault(hasAny = state.totalCount > 0)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.items, key = { it.id }) { item ->
                    VaultCard(
                        item = item,
                        deckName = state.deckNames[item.deckId] ?: "",
                        onClick = { onOpen(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalSparkPalette.current
    Text(
        text = label,
        color = if (selected) palette.onBackground else palette.onSurfaceMuted,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) palette.surfaceElevated else Color.Transparent)
            .border(1.dp, if (selected) palette.accent else palette.outline, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .semantics { contentDescription = if (selected) "$label filter, on" else "$label filter, off" }
    )
}

@Composable
private fun VaultCard(item: SavedCollision, deckName: String, onClick: () -> Unit) {
    val palette = LocalSparkPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surface)
            .border(1.dp, palette.outline, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp)
            .semantics { contentDescription = "Saved idea: ${item.picks.asSpoken()}. Tap to open." }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.picks.asInline(),
                color = palette.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (item.favorite) {
                Icon(Icons.Filled.Star, contentDescription = "Favorite", tint = palette.accent, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.size(6.dp))
        Text(
            text = buildString {
                if (deckName.isNotEmpty()) append(deckName)
                if (item.tags.isNotEmpty()) {
                    if (isNotEmpty()) append("  -  ")
                    append(item.tags.joinToString(" ") { "#$it" })
                }
            },
            color = palette.onSurfaceMuted,
            fontSize = 13.sp
        )
        if (item.note.isNotBlank()) {
            Spacer(Modifier.size(6.dp))
            Text(text = item.note, color = palette.onSurfaceMuted, fontSize = 14.sp, maxLines = 2)
        }
    }
}

@Composable
private fun EmptyVault(hasAny: Boolean) {
    val palette = LocalSparkPalette.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = if (hasAny) "Nothing matches your search yet."
            else "No saved sparks yet.\nRoll on the Collide tab and save the good ones.",
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
    onSaveNote: (String) -> Unit,
    onSaveTags: (String) -> Unit,
    onDelete: () -> Unit
) {
    val palette = LocalSparkPalette.current
    val context = LocalContext.current
    var note by remember(item.id) { mutableStateOf(item.note) }
    var tags by remember(item.id) { mutableStateOf(item.tags.joinToString(", ")) }
    var confirmDelete by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.surface)
                .padding(20.dp)
        ) {
            // The idea, big.
            Text(
                text = item.picks.asInline(),
                color = palette.onBackground,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.size(4.dp))
            Text(text = deckName, color = palette.onSurfaceMuted, fontSize = 13.sp)

            Spacer(Modifier.size(16.dp))

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
