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
import com.elendheim.spark.data.newId
import com.elendheim.spark.engine.ColliderEngine
import com.elendheim.spark.model.Deck
import com.elendheim.spark.model.Pick
import com.elendheim.spark.model.SavedCollision
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

/** Everything the collide screen needs to draw itself, in one snapshot. */
data class ColliderUiState(
    val loading: Boolean = true,
    val decks: List<Deck> = emptyList(),
    val selectedDeck: Deck? = null,
    val wheels: List<Wheel> = emptyList(),        // the selected deck's wheels, in order
    val current: List<Pick> = emptyList(),        // the collision on screen right now
    val lockedWheelNames: Set<String> = emptySet(),
    val history: List<List<Pick>> = emptyList(),  // most-recent-first, capped
    val settings: SparkSettings = SparkSettings(),
    val landedNonce: Long = 0L                     // bumps each roll -> triggers haptic / announce
)

/**
 * Drives the collide screen. It owns the transient roll state (current picks,
 * which wheels are locked, the recent history) and asks the pure
 * [ColliderEngine] to do the actual rolling. No randomness or engine rules
 * leak into the UI.
 */
class ColliderViewModel(
    private val repository: SparkRepository,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    private val rng: Random = Random.Default

    // Transient, user-driven state.
    private val selectedDeckId = MutableStateFlow<String?>(null)
    private val current = MutableStateFlow<List<Pick>>(emptyList())
    private val locked = MutableStateFlow<Set<String>>(emptySet())
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
        combine(current, locked, history, landedNonce) { c, l, h, n -> Quad(c, l, h, n) }
    ) { decks, wheels, settings, selId, roll ->
        val deck = resolveDeck(decks, selId, settings.defaultDeckId)
        val deckWheels = wheelsForDeck(deck, wheels)
        ColliderUiState(
            loading = false,
            decks = decks,
            selectedDeck = deck,
            wheels = deckWheels,
            current = roll.current,
            lockedWheelNames = roll.locked,
            history = roll.history,
            settings = settings,
            landedNonce = roll.nonce
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ColliderUiState())

    // --- Actions ---

    /** Switch decks. Clears the current roll and locks so the new deck feels fresh. */
    fun selectDeck(deckId: String) {
        if (deckId == selectedDeckId.value) return
        selectedDeckId.value = deckId
        current.value = emptyList()
        locked.value = emptySet()
    }

    /** Roll every unlocked wheel. This is the COLLIDE button. */
    fun collide() {
        val deck = currentDeck() ?: return
        val wheels = wheelsForDeck(deck, wheelsSnapshot)
        val next = if (current.value.isEmpty()) {
            ColliderEngine.collide(deck, wheels, rng)
        } else {
            ColliderEngine.reroll(deck, wheels, current.value, locked.value, rng)
        }
        pushHistory(current.value)
        current.value = next
        landedNonce.value += 1
    }

    /** Reroll only the unlocked wheels, keeping locked picks. */
    fun reroll() = collide()

    /** Keep the whole collision but reroll exactly one wheel (named, or random). */
    fun mutate(wheelName: String? = null) {
        val deck = currentDeck() ?: return
        if (current.value.isEmpty()) { collide(); return }
        val wheels = wheelsForDeck(deck, wheelsSnapshot)
        val next = ColliderEngine.mutate(deck, wheels, current.value, wheelName, rng)
        pushHistory(current.value)
        current.value = next
        landedNonce.value += 1
    }

    /** Toggle a wheel's lock. Locked wheels keep their pick on the next roll. */
    fun toggleLock(wheelName: String) {
        locked.value = locked.value.toMutableSet().apply {
            if (!add(wheelName)) remove(wheelName)
        }
    }

    /** Bring a past collision back to the top as the current one. */
    fun restoreFromHistory(picks: List<Pick>) {
        if (picks.isEmpty()) return
        current.value = picks
        landedNonce.value += 1
    }

    /** Save the current collision to the vault. Snapshots the text, never ids. */
    fun saveCurrent(now: Long, onSaved: () -> Unit = {}) {
        val deck = currentDeck() ?: return
        val picks = current.value
        if (picks.isEmpty()) return
        viewModelScope.launch {
            repository.saveCollision(
                SavedCollision(
                    id = newId(),
                    deckId = deck.id,
                    picks = picks,
                    createdAt = now
                )
            )
            onSaved()
        }
    }

    /** Surprise me: jump to a random deck and roll it. */
    fun surpriseMe() {
        val decks = decksSnapshot
        if (decks.isEmpty()) return
        val pick = decks.random(rng)
        selectDeck(pick.id)
        // Roll immediately using the freshly chosen deck.
        val wheels = wheelsForDeck(pick, wheelsSnapshot)
        current.value = ColliderEngine.collide(pick, wheels, rng)
        locked.value = emptySet()
        landedNonce.value += 1
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
     * The wheels of [deck] that take part in a mix, in the deck's order.
     *
     * The mix limit (a user setting) caps how many wheels collide at once, so
     * the result stays readable instead of a wall of text. 0 means "all". We
     * take the first N in deck order -> predictable, and you choose which by
     * reordering wheels in the editor.
     *
     * When weighting is switched off globally, every entry is flattened to
     * weight 1 so rolls are pure random.
     */
    private fun wheelsForDeck(deck: Deck?, allWheels: List<Wheel>): List<Wheel> {
        if (deck == null) return emptyList()
        val byId = allWheels.associateBy { it.id }
        val ordered = deck.wheelIds.mapNotNull { byId[it] }

        val limit = settingsSnapshot.mixLimit
        val limited = if (limit in 1 until ordered.size) ordered.take(limit) else ordered

        return if (settingsSnapshot.weightingEnabled) {
            limited
        } else {
            limited.map { w -> w.copy(entries = w.entries.map { it.copy(weight = 1) }) }
        }
    }

    /** Change how many wheels mix at once (0 = all). Persisted in settings. */
    fun setMixLimit(value: Int) {
        viewModelScope.launch { settingsRepo.setMixLimit(value) }
    }

    private fun pushHistory(picks: List<Pick>) {
        if (picks.isEmpty()) return
        // Newest first, capped at 30: out with the old, in with the new. A great
        // roll is never lost to a reflex re-roll.
        history.value = (listOf(picks) + history.value).take(30)
    }

    // Small 4-tuple so we can combine four transient flows at once.
    private data class Quad(
        val current: List<Pick>,
        val locked: Set<String>,
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
