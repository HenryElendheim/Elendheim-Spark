package com.elendheim.spark.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// One DataStore file for the whole app's preferences.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "spark_settings")

/**
 * Reads and writes the [SparkSettings]. DataStore keeps this off the main
 * thread and gives us a Flow that emits whenever any preference changes, so the
 * UI (theme, motion, tap sizes) reacts live.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val textScale = floatPreferencesKey("text_scale")
        val highContrast = booleanPreferencesKey("high_contrast")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val colorblind = booleanPreferencesKey("colorblind_palette")
        val showLabels = booleanPreferencesKey("show_wheel_labels")
        val largerTargets = booleanPreferencesKey("larger_tap_targets")
        val haptics = booleanPreferencesKey("haptics")
        val announce = booleanPreferencesKey("announce_result")
        val lineByLine = booleanPreferencesKey("line_by_line")
        val weighting = booleanPreferencesKey("weighting_enabled")
        val defaultDeck = stringPreferencesKey("default_deck_id")
    }

    /** The live settings. Falls back to sensible defaults for anything unset. */
    val settings: Flow<SparkSettings> = context.dataStore.data.map { p ->
        val d = SparkSettings()
        SparkSettings(
            textScale = p[Keys.textScale] ?: d.textScale,
            highContrast = p[Keys.highContrast] ?: d.highContrast,
            reduceMotion = p[Keys.reduceMotion] ?: d.reduceMotion,
            colorblindPalette = p[Keys.colorblind] ?: d.colorblindPalette,
            showWheelLabels = p[Keys.showLabels] ?: d.showWheelLabels,
            largerTapTargets = p[Keys.largerTargets] ?: d.largerTapTargets,
            haptics = p[Keys.haptics] ?: d.haptics,
            announceResult = p[Keys.announce] ?: d.announceResult,
            lineByLineResult = p[Keys.lineByLine] ?: d.lineByLineResult,
            weightingEnabled = p[Keys.weighting] ?: d.weightingEnabled,
            defaultDeckId = p[Keys.defaultDeck] ?: d.defaultDeckId
        )
    }

    suspend fun setTextScale(value: Float) = edit { it[Keys.textScale] = value }
    suspend fun setHighContrast(value: Boolean) = edit { it[Keys.highContrast] = value }
    suspend fun setReduceMotion(value: Boolean) = edit { it[Keys.reduceMotion] = value }
    suspend fun setColorblindPalette(value: Boolean) = edit { it[Keys.colorblind] = value }
    suspend fun setShowWheelLabels(value: Boolean) = edit { it[Keys.showLabels] = value }
    suspend fun setLargerTapTargets(value: Boolean) = edit { it[Keys.largerTargets] = value }
    suspend fun setHaptics(value: Boolean) = edit { it[Keys.haptics] = value }
    suspend fun setAnnounceResult(value: Boolean) = edit { it[Keys.announce] = value }
    suspend fun setLineByLineResult(value: Boolean) = edit { it[Keys.lineByLine] = value }
    suspend fun setWeightingEnabled(value: Boolean) = edit { it[Keys.weighting] = value }

    suspend fun setDefaultDeckId(value: String?) = context.dataStore.edit { p ->
        if (value == null) p.remove(Keys.defaultDeck) else p[Keys.defaultDeck] = value
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
