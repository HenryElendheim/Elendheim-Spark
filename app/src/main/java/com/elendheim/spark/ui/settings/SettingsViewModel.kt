package com.elendheim.spark.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.elendheim.spark.SparkApp
import com.elendheim.spark.data.ExportImport
import com.elendheim.spark.data.FileIo
import com.elendheim.spark.data.SparkRepository
import com.elendheim.spark.model.Deck
import com.elendheim.spark.model.Wheel
import com.elendheim.spark.settings.SettingsRepository
import com.elendheim.spark.settings.SparkSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the settings screen needs: the current settings plus the deck/wheel lists. */
data class SettingsUiState(
    val settings: SparkSettings = SparkSettings(),
    val decks: List<Deck> = emptyList(),
    val wheels: List<Wheel> = emptyList()
)

/**
 * Backs the settings screen. Preference changes go to the [SettingsRepository];
 * export and import pair the pure [ExportImport] logic with the file the user
 * picked. File reads and writes run off the main thread.
 */
class SettingsViewModel(
    private val appContext: Context,
    private val repository: SparkRepository,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepo.settings,
        repository.decks,
        repository.wheels
    ) { settings, decks, wheels ->
        SettingsUiState(settings = settings, decks = decks, wheels = wheels)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    // --- Accessibility & behaviour toggles ---

    fun setTextScale(v: Float) = viewModelScope.launch { settingsRepo.setTextScale(v) }
    fun setHighContrast(v: Boolean) = viewModelScope.launch { settingsRepo.setHighContrast(v) }
    fun setReduceMotion(v: Boolean) = viewModelScope.launch { settingsRepo.setReduceMotion(v) }
    fun setColorblind(v: Boolean) = viewModelScope.launch { settingsRepo.setColorblindPalette(v) }
    fun setShowLabels(v: Boolean) = viewModelScope.launch { settingsRepo.setShowWheelLabels(v) }
    fun setLargerTargets(v: Boolean) = viewModelScope.launch { settingsRepo.setLargerTapTargets(v) }
    fun setHaptics(v: Boolean) = viewModelScope.launch { settingsRepo.setHaptics(v) }
    fun setAnnounce(v: Boolean) = viewModelScope.launch { settingsRepo.setAnnounceResult(v) }
    fun setLineByLine(v: Boolean) = viewModelScope.launch { settingsRepo.setLineByLineResult(v) }
    fun setWeighting(v: Boolean) = viewModelScope.launch { settingsRepo.setWeightingEnabled(v) }
    fun setDefaultDeck(id: String?) = viewModelScope.launch { settingsRepo.setDefaultDeckId(id) }

    // --- Quick add into rotation (same power as the editor, one tap away) ---

    fun quickAdd(wheel: Wheel, multiline: String) = viewModelScope.launch {
        val fresh = multiline.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (fresh.isEmpty()) return@launch
        val added = fresh.map { com.elendheim.spark.model.Entry(id = com.elendheim.spark.data.newId(), text = it) }
        repository.upsertWheel(wheel.copy(entries = wheel.entries + added))
    }

    // --- Export / import ---

    /** A friendly, dated default file name, e.g. elendheim-spark-2026-07-12.json. */
    fun suggestedFileName(now: Long): String {
        val date = java.time.Instant.ofEpochMilli(now)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .toString()
        return "elendheim-spark-$date.json"
    }

    fun exportJson(uri: Uri, now: Long, onResult: (String) -> Unit) = viewModelScope.launch {
        try {
            val export = repository.buildExport(now)
            val text = ExportImport.encode(export)
            withContext(Dispatchers.IO) {
                FileIo.writeText(appContext.contentResolver, uri, text)
            }
            onResult("Exported your idea engine")
        } catch (e: Exception) {
            onResult("Export failed: ${e.message}")
        }
    }

    fun exportMarkdown(uri: Uri, now: Long, onResult: (String) -> Unit) = viewModelScope.launch {
        try {
            val export = repository.buildExport(now)
            val text = ExportImport.vaultToMarkdown(export)
            withContext(Dispatchers.IO) {
                FileIo.writeText(appContext.contentResolver, uri, text)
            }
            onResult("Exported your vault as text")
        } catch (e: Exception) {
            onResult("Export failed: ${e.message}")
        }
    }

    fun importJson(uri: Uri, mode: ExportImport.ImportMode, onResult: (String) -> Unit) = viewModelScope.launch {
        try {
            val text = withContext(Dispatchers.IO) {
                FileIo.readText(appContext.contentResolver, uri)
            }
            val incoming = ExportImport.decode(text)
            repository.applyImport(incoming, mode)
            val what = if (mode == ExportImport.ImportMode.REPLACE_ALL) "Replaced everything from file" else "Merged file into your library"
            onResult(what)
        } catch (e: Exception) {
            onResult("Import failed: ${e.message}")
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SparkApp
                SettingsViewModel(app.applicationContext, app.container.repository, app.container.settings)
            }
        }
    }
}
