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
import com.elendheim.spark.settings.SettingsRepository
import com.elendheim.spark.settings.SparkSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The editor's data plus the current limits and colourblind flag. */
data class EditorUiState(
    val decks: List<Deck> = emptyList(),
    val wheelsById: Map<String, Wheel> = emptyMap(),
    val colorblind: Boolean = false,
    val maxWheels: Int = 15,
    val maxEntries: Int = 250
)

/**
 * Curation lives here: create and edit decks, wheels and entries. Wheels are
 * deck-specific, so deleting a deck also deletes its wheels; saved sparks are
 * untouched because they snapshot their text.
 *
 * The wheel and entry counts are capped by the limits in Settings (default
 * ceilings 15 wheels and 250 entries), enforced here.
 */
class EditorViewModel(
    private val repository: SparkRepository,
    settingsRepo: SettingsRepository
) : ViewModel() {

    private val newWheelPalette = listOf(
        "#E0555A", "#4FA6D9", "#E0A44F", "#6FBF73", "#B588E0", "#E07FB0"
    )

    private var settingsSnapshot: SparkSettings = SparkSettings()

    init {
        viewModelScope.launch { settingsRepo.settings.collect { settingsSnapshot = it } }
    }

    val uiState: StateFlow<EditorUiState> = combine(
        repository.decks,
        repository.wheels,
        settingsRepo.settings
    ) { decks, wheels, settings ->
        EditorUiState(
            decks = decks,
            wheelsById = wheels.associateBy { it.id },
            colorblind = settings.colorblindPalette,
            maxWheels = settings.maxWheelsPerDeck,
            maxEntries = settings.maxEntriesPerWheel
        )
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

    /** Add a wheel, unless the deck is already at the wheel limit. */
    fun addWheel(deck: Deck, name: String) = viewModelScope.launch {
        if (deck.wheelIds.size >= settingsSnapshot.maxWheelsPerDeck) return@launch
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

    /** Add one entry, unless the wheel is already at the entry limit. */
    fun addEntry(wheel: Wheel, text: String) = viewModelScope.launch {
        val clean = text.trim()
        if (clean.isEmpty() || wheel.entries.size >= settingsSnapshot.maxEntriesPerWheel) return@launch
        val entry = Entry(id = newId(), text = clean)
        repository.upsertWheel(wheel.copy(entries = wheel.entries + entry))
    }

    /**
     * Paste a whole list, one entry per line. Only as many as fit under the
     * entry limit are added; the rest are dropped.
     */
    fun bulkAddEntries(wheel: Wheel, multiline: String) = viewModelScope.launch {
        val room = (settingsSnapshot.maxEntriesPerWheel - wheel.entries.size).coerceAtLeast(0)
        if (room == 0) return@launch
        val newOnes = multiline.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(room)
            .map { Entry(id = newId(), text = it) }
        if (newOnes.isEmpty()) return@launch
        repository.upsertWheel(wheel.copy(entries = wheel.entries + newOnes))
    }

    fun editEntry(wheel: Wheel, entryId: String, text: String) = viewModelScope.launch {
        val updated = wheel.entries.map {
            if (it.id == entryId) it.copy(text = text.trim().ifEmpty { it.text }) else it
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
                EditorViewModel(app.container.repository, app.container.settings)
            }
        }
    }
}
