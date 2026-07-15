package com.elendheim.spark.ui.collider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.elendheim.spark.SparkApp
import com.elendheim.spark.data.SparkRepository
import com.elendheim.spark.engine.ColliderEngine
import com.elendheim.spark.model.Deck
import com.elendheim.spark.model.Pick
import com.elendheim.spark.model.Wheel
import com.elendheim.spark.settings.SettingsRepository
import com.elendheim.spark.settings.SparkSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Everything the randomize screen needs to draw itself, in one snapshot. */
data class ColliderUiState(
    val loading: Boolean = true,
    val decks: List<Deck> = emptyList(),
    val selectedDeck: Deck? = null,
    val wheels: List<Wheel> = emptyList(),          // all wheels of the deck (shown as chips)
    val current: List<Pick> = emptyList(),          // the result on screen right now
    val excludedWheelNames: Set<String> = emptySet(), // wheels switched off -> not in the mix
    val history: List<List<Pick>> = emptyList(),    // most-recent-first, capped
    val settings: SparkSettings = SparkSettings(),
    val landedNonce: Long = 0L                       // bumps each roll -> triggers animation / haptic
)

/**
 * Drives the randomize screen. It owns the transient roll state (current picks,
 * which wheels are switched off, the recent history) and asks the pure
 * [ColliderEngine] to do the actual rolling.
 *
 * A wheel that is "excluded" simply does not take part in the mix and does not
 * appear in the result -> that is what tapping a chip does now.
 */
class ColliderViewModel(
    private val repository: SparkRepository,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    private val rng: Random = Random.Default

    // Transient, user-driven state.
    private val selectedDeckId = MutableStateFlow<String?>(null)
    private val current = MutableStateFlow<List<Pick>>(emptyList())
    private val excluded = MutableStateFlow<Set<String>>(emptySet())
    private val history = MutableStateFlow<List<List<Pick>>>(emptyList())
    private val landedNonce = MutableStateFlow(0L)

    // Snapshots kept for the action methods to read synchronously.
    private var decksSnapshot: List<Deck> = emptyList()
    private var wheelsSnapshot: List<Wheel> = emptyList()
    private var settingsSnapshot: SparkSettings = SparkSettings()

    init {
        viewModelScope.launch { repository.decks.collect { decksSnapshot = it } }
        viewModelScope.launch { repository.wheels.collect { wheelsSnapshot = it } }
        viewModelScope.launch { settingsRepo.settings.collect { settingsSnapshot = it } }
    }

    val uiState: StateFlow<ColliderUiState> = combine(
        repository.decks,
        repository.wheels,
        settingsRepo.settings,
        selectedDeckId,
        combine(current, excluded, history, landedNonce) { c, e, h, n -> Quad(c, e, h, n) }
    ) { decks, wheels, settings, selId, roll ->
        val deck = resolveDeck(decks, selId, settings.defaultDeckId)
        val deckWheels = wheelsForDeck(deck, wheels)
        ColliderUiState(
            loading = false,
            decks = decks,
            selectedDeck = deck,
            wheels = deckWheels,
            current = roll.current,
            excludedWheelNames = roll.excluded,
            history = roll.history,
            settings = settings,
            landedNonce = roll.nonce
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ColliderUiState())

    // --- Actions ---

    /** Switch decks. Clears the current roll and switched-off wheels for a fresh start. */
    fun selectDeck(deckId: String) {
        if (deckId == selectedDeckId.value) return
        selectedDeckId.value = deckId
        current.value = emptyList()
        excluded.value = emptySet()
    }

    /** Roll all the wheels that are switched on. This is the RANDOMIZE button. */
    fun collide() {
        val deck = currentDeck() ?: return
        val wheels = includedWheels(deck)
        if (wheels.isEmpty()) return
        pushHistory(current.value)
        current.value = ColliderEngine.collide(deck, wheels, rng)
        landedNonce.value += 1
    }

    /** Randomize just one wheel of the current result -> "randomize this one". */
    fun randomizePick(wheelName: String) {
        val deck = currentDeck() ?: return
        if (current.value.isEmpty()) { collide(); return }
        val wheels = includedWheels(deck)
        pushHistory(current.value)
        current.value = ColliderEngine.mutate(deck, wheels, current.value, wheelName, rng)
        landedNonce.value += 1
    }

    /**
     * Replace one pick with the user's own text, leaving the rest untouched.
     * A one-off override on the current result; it does not edit the wheel.
     */
    fun setCustomPick(wheelName: String, text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || current.value.isEmpty()) return
        pushHistory(current.value)
        current.value = current.value.map { pick ->
            if (pick.wheelName == wheelName) pick.copy(text = clean) else pick
        }
        landedNonce.value += 1
    }

    /**
     * Toggle whether a wheel takes part. An excluded wheel is greyed out and
     * does not appear in the result -> the accessible way to trim a mix down.
     */
    fun toggleExclude(wheelName: String) {
        excluded.value = excluded.value.toMutableSet().apply {
            if (!add(wheelName)) remove(wheelName)
        }
    }

    /** Bring a past collision back to the top as the current one. */
    fun restoreFromHistory(picks: List<Pick>) {
        if (picks.isEmpty()) return
        current.value = picks
        landedNonce.value += 1
    }

    /** Empty the recent-rolls list. */
    fun clearHistory() {
        history.value = emptyList()
    }

    /** Save the current result to the vault, auto-numbered. Snapshots the text. */
    fun saveCurrent(now: Long, onSaved: () -> Unit = {}) {
        val deck = currentDeck() ?: return
        val picks = current.value
        if (picks.isEmpty()) return
        viewModelScope.launch {
            repository.saveNewCollision(deck.id, picks, now)
            onSaved()
        }
    }

    /**
     * The dice: jump to a random deck. It does NOT roll for you -> you press
     * RANDOMIZE yourself once you see which deck landed.
     */
    fun pickRandomDeck() {
        val decks = decksSnapshot
        if (decks.isEmpty()) return
        val pick = decks.random(rng)
        selectedDeckId.value = pick.id
        current.value = emptyList()
        excluded.value = emptySet()
    }

    // --- Helpers ---

    private fun currentDeck(): Deck? =
        resolveDeck(decksSnapshot, selectedDeckId.value, settingsSnapshot.defaultDeckId)

    private fun resolveDeck(decks: List<Deck>, selId: String?, defaultId: String?): Deck? {
        if (decks.isEmpty()) return null
        selId?.let { id -> decks.firstOrNull { it.id == id }?.let { return it } }
        defaultId?.let { id -> decks.firstOrNull { it.id == id }?.let { return it } }
        return decks.first()
    }

    /**
     * Every wheel of [deck], in deck order. When weighting is off globally,
     * entries are flattened to weight 1 so rolls are pure random. These are the
     * wheels shown as chips; the excluded ones are filtered out only at roll time.
     */
    private fun wheelsForDeck(deck: Deck?, allWheels: List<Wheel>): List<Wheel> {
        if (deck == null) return emptyList()
        val byId = allWheels.associateBy { it.id }
        val ordered = deck.wheelIds.mapNotNull { byId[it] }
        return if (settingsSnapshot.weightingEnabled) {
            ordered
        } else {
            ordered.map { w -> w.copy(entries = w.entries.map { it.copy(weight = 1) }) }
        }
    }

    /** The wheels that actually roll: deck order, minus the switched-off ones. */
    private fun includedWheels(deck: Deck): List<Wheel> =
        wheelsForDeck(deck, wheelsSnapshot).filterNot { it.name in excluded.value }

    private fun pushHistory(picks: List<Pick>) {
        if (picks.isEmpty()) return
        // Newest first, capped at 30: out with the old, in with the new.
        history.value = (listOf(picks) + history.value).take(30)
    }

    // Small 4-tuple so we can combine four transient flows at once.
    private data class Quad(
        val current: List<Pick>,
        val excluded: Set<String>,
        val history: List<List<Pick>>,
        val nonce: Long
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SparkApp
                ColliderViewModel(app.container.repository, app.container.settings)
            }
        }
    }
}

// Small extension so screens can pull the CreationExtras application cleanly.
val CreationExtras.sparkApp: SparkApp get() = this[APPLICATION_KEY] as SparkApp
