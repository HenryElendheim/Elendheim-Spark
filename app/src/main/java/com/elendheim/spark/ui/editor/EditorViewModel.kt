package com.elendheim.spark.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.elendheim.spark.SparkApp
import com.elendheim.spark.data.SparkRepository
import com.elendheim.spark.data.newId
import com.elendheim.spark.model.Deck
import com.elendheim.spark.model.Entry
import com.elendheim.spark.model.Wheel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The editor's data: every deck and a lookup of every wheel by id. */
data class EditorUiState(
    val decks: List<Deck> = emptyList(),
    val wheelsById: Map<String, Wheel> = emptyMap()
)

/**
 * Curation lives here: create and edit decks, wheels and entries. Wheels are
 * deck-specific (the simple model), so deleting a deck also deletes its wheels;
 * saved sparks are untouched because they snapshot their text.
 *
 * The palette used for new wheels rotates through a set of readable hues.
 */
class EditorViewModel(private val repository: SparkRepository) : ViewModel() {

    private val newWheelPalette = listOf(
        "#E0555A", "#4FA6D9", "#E0A44F", "#6FBF73", "#B588E0", "#E07FB0"
    )

    val uiState: StateFlow<EditorUiState> = combine(
        repository.decks,
        repository.wheels
    ) { decks, wheels ->
        EditorUiState(decks = decks, wheelsById = wheels.associateBy { it.id })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditorUiState())

    // --- Decks ---

    fun createDeck(name: String) = viewModelScope.launch {
        val clean = name.trim().ifEmpty { "New deck" }
        repository.upsertDeck(
            Deck(id = newId(), name = clean, wheelIds = emptyList(), createdAt = System.currentTimeMillis())
        )
    }

    fun renameDeck(deck: Deck, name: String) = viewModelScope.launch {
        repository.upsertDeck(deck.copy(name = name.trim().ifEmpty { deck.name }))
    }

    /** Delete a deck and all of its (deck-specific) wheels. */
    fun deleteDeck(deck: Deck) = viewModelScope.launch {
        val wheels = uiState.value.wheelsById
        deck.wheelIds.mapNotNull { wheels[it] }.forEach { repository.deleteWheel(it) }
        repository.deleteDeck(deck)
    }

    // --- Wheels ---

    fun addWheel(deck: Deck, name: String) = viewModelScope.launch {
        val color = newWheelPalette[deck.wheelIds.size % newWheelPalette.size]
        val wheel = Wheel(
            id = newId(),
            name = name.trim().ifEmpty { "New wheel" },
            colorHex = color,
            entries = emptyList()
        )
        repository.upsertWheel(wheel)
        repository.upsertDeck(deck.copy(wheelIds = deck.wheelIds + wheel.id))
    }

    fun renameWheel(wheel: Wheel, name: String) = viewModelScope.launch {
        repository.upsertWheel(wheel.copy(name = name.trim().ifEmpty { wheel.name }))
    }

    fun recolorWheel(wheel: Wheel, colorHex: String) = viewModelScope.launch {
        repository.upsertWheel(wheel.copy(colorHex = colorHex))
    }

    fun deleteWheel(deck: Deck, wheel: Wheel) = viewModelScope.launch {
        repository.upsertDeck(deck.copy(wheelIds = deck.wheelIds - wheel.id))
        repository.deleteWheel(wheel)
    }

    /** Move a wheel one step within its deck's order. */
    fun moveWheel(deck: Deck, wheelId: String, up: Boolean) = viewModelScope.launch {
        repository.upsertDeck(deck.copy(wheelIds = moveInList(deck.wheelIds, wheelId, up)))
    }

    // --- Entries ---

    fun addEntry(wheel: Wheel, text: String, weight: Int = 1) = viewModelScope.launch {
        val clean = text.trim()
        if (clean.isEmpty()) return@launch
        val entry = Entry(id = newId(), text = clean, weight = weight.coerceAtLeast(1))
        repository.upsertWheel(wheel.copy(entries = wheel.entries + entry))
    }

    /** Paste a whole list, one entry per line, and add them all at once. */
    fun bulkAddEntries(wheel: Wheel, multiline: String) = viewModelScope.launch {
        val newOnes = multiline.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { Entry(id = newId(), text = it, weight = 1) }
        if (newOnes.isEmpty()) return@launch
        repository.upsertWheel(wheel.copy(entries = wheel.entries + newOnes))
    }

    fun editEntry(wheel: Wheel, entryId: String, text: String, weight: Int) = viewModelScope.launch {
        val updated = wheel.entries.map {
            if (it.id == entryId) it.copy(text = text.trim().ifEmpty { it.text }, weight = weight.coerceAtLeast(1))
            else it
        }
        repository.upsertWheel(wheel.copy(entries = updated))
    }

    fun deleteEntry(wheel: Wheel, entryId: String) = viewModelScope.launch {
        repository.upsertWheel(wheel.copy(entries = wheel.entries.filterNot { it.id == entryId }))
    }

    fun moveEntry(wheel: Wheel, entryId: String, up: Boolean) = viewModelScope.launch {
        val ids = wheel.entries.map { it.id }
        val reordered = moveInList(ids, entryId, up)
        val byId = wheel.entries.associateBy { it.id }
        repository.upsertWheel(wheel.copy(entries = reordered.mapNotNull { byId[it] }))
    }

    /** Move [id] one place up or down in [list], staying in bounds. */
    private fun moveInList(list: List<String>, id: String, up: Boolean): List<String> {
        val index = list.indexOf(id)
        if (index < 0) return list
        val target = if (up) index - 1 else index + 1
        if (target < 0 || target >= list.size) return list
        val mutable = list.toMutableList()
        mutable.removeAt(index)
        mutable.add(target, id)
        return mutable
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SparkApp
                EditorViewModel(app.container.repository)
            }
        }
    }
}
