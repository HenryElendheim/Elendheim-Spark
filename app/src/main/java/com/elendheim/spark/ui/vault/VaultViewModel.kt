package com.elendheim.spark.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.elendheim.spark.SparkApp
import com.elendheim.spark.data.SparkRepository
import com.elendheim.spark.model.Deck
import com.elendheim.spark.model.SavedCollision
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the vault screen shows: the filtered list plus the controls' state. */
data class VaultUiState(
    val items: List<SavedCollision> = emptyList(),
    val deckNames: Map<String, String> = emptyMap(),
    val decks: List<Deck> = emptyList(),
    val query: String = "",
    val deckFilter: String? = null,     // deck id, or null for all decks
    val favoritesOnly: Boolean = false,
    val totalCount: Int = 0
)

/**
 * Drives the vault: search, filter, and the small edits you make to a saved
 * spark (note, tags, favourite, delete). All persistence goes through the
 * repository.
 */
class VaultViewModel(private val repository: SparkRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val deckFilter = MutableStateFlow<String?>(null)
    private val favoritesOnly = MutableStateFlow(false)

    val uiState: StateFlow<VaultUiState> = combine(
        repository.vault,
        repository.decks,
        query,
        deckFilter,
        favoritesOnly
    ) { vault, decks, q, deck, favOnly ->
        val names = decks.associate { it.id to it.name }
        val filtered = vault.filter { item ->
            (deck == null || item.deckId == deck) &&
                (!favOnly || item.favorite) &&
                matchesQuery(item, q)
        }
        VaultUiState(
            items = filtered,
            deckNames = names,
            decks = decks,
            query = q,
            deckFilter = deck,
            favoritesOnly = favOnly,
            totalCount = vault.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VaultUiState())

    fun setQuery(value: String) { query.value = value }
    fun setDeckFilter(deckId: String?) { deckFilter.value = deckId }
    fun toggleFavoritesOnly() { favoritesOnly.value = !favoritesOnly.value }

    fun toggleFavorite(item: SavedCollision) = viewModelScope.launch {
        repository.updateCollision(item.copy(favorite = !item.favorite))
    }

    fun updateNote(item: SavedCollision, note: String) = viewModelScope.launch {
        repository.updateCollision(item.copy(note = note))
    }

    /** Give a saved idea a custom name; blank falls back to deck name + number. */
    fun updateTitle(item: SavedCollision, title: String) = viewModelScope.launch {
        repository.updateCollision(item.copy(title = title.trim()))
    }

    /** Tags come in as a comma-separated string from the UI; tidy them here. */
    fun updateTags(item: SavedCollision, rawTags: String) = viewModelScope.launch {
        val tags = rawTags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        repository.updateCollision(item.copy(tags = tags))
    }

    fun delete(item: SavedCollision) = viewModelScope.launch {
        repository.deleteCollision(item)
    }

    /** Match the text of any pick, the note, or any tag, case-insensitively. */
    private fun matchesQuery(item: SavedCollision, q: String): Boolean {
        if (q.isBlank()) return true
        val needle = q.trim().lowercase()
        return item.picks.any { it.text.lowercase().contains(needle) } ||
            item.note.lowercase().contains(needle) ||
            item.tags.any { it.lowercase().contains(needle) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SparkApp
                VaultViewModel(app.container.repository)
            }
        }
    }
}
