package com.elendheim.spark.data

import com.elendheim.spark.model.ColliderExport
import com.elendheim.spark.model.Deck
import com.elendheim.spark.model.SavedCollision
import com.elendheim.spark.model.Wheel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Turning your whole idea engine into a single file, and back again.
 *
 * Everything here is pure: it works on model lists and strings, with no
 * database and no Android I/O. That keeps the round-trip logic honest and easy
 * to test. Reading and writing the actual file is a separate, tiny concern
 * (see FileIo).
 */
object ExportImport {

    /**
     * The JSON format for the export file.
     * - prettyPrint: the file is human-readable if you open it.
     * - ignoreUnknownKeys: a file from a newer version still imports here.
     * - encodeDefaults: fields left at their default (empty note, weight 1)
     *   are still written, so the file is a complete, unambiguous record.
     */
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** How an import combines with what is already on the device. */
    enum class ImportMode {
        /** Add anything new; keep what you already have (vault keeps the newer copy). */
        MERGE,

        /** Wipe everything, then load the file exactly. A full restore. */
        REPLACE_ALL
    }

    /** The final set of data to persist after resolving an import. */
    data class Resolved(
        val decks: List<Deck>,
        val wheels: List<Wheel>,
        val vault: List<SavedCollision>
    )

    fun buildExport(
        decks: List<Deck>,
        wheels: List<Wheel>,
        vault: List<SavedCollision>,
        exportedAt: Long
    ): ColliderExport = ColliderExport(
        schemaVersion = 1,
        exportedAt = exportedAt,
        decks = decks,
        wheels = wheels,
        vault = vault
    )

    fun encode(export: ColliderExport): String = json.encodeToString(export)

    fun decode(text: String): ColliderExport = json.decodeFromString(text)

    /**
     * Work out what the database should contain after importing [incoming] on
     * top of the [current] data, according to [mode]. Pure -> the caller just
     * writes the result.
     */
    fun resolve(
        current: Resolved,
        incoming: ColliderExport,
        mode: ImportMode
    ): Resolved = when (mode) {
        ImportMode.REPLACE_ALL -> Resolved(incoming.decks, incoming.wheels, incoming.vault)
        ImportMode.MERGE -> Resolved(
            decks = mergeById(current.decks, incoming.decks) { it.id },
            wheels = mergeById(current.wheels, incoming.wheels) { it.id },
            vault = mergeVault(current.vault, incoming.vault)
        )
    }

    /**
     * Merge by id, keeping the existing item when both sides have the same id.
     * Used for decks and wheels: importing never silently overwrites edits you
     * made locally; it only adds ids you did not already have.
     */
    private fun <T> mergeById(existing: List<T>, incoming: List<T>, id: (T) -> String): List<T> {
        val known = existing.map(id).toSet()
        return existing + incoming.filter { id(it) !in known }
    }

    /**
     * Merge the vault by id, but when both sides hold the same id keep the
     * newer one (later createdAt). A saved spark should never regress to an
     * older copy on import.
     */
    private fun mergeVault(
        existing: List<SavedCollision>,
        incoming: List<SavedCollision>
    ): List<SavedCollision> {
        val byId = existing.associateBy { it.id }.toMutableMap()
        for (item in incoming) {
            val have = byId[item.id]
            if (have == null || item.createdAt > have.createdAt) {
                byId[item.id] = item
            }
        }
        return byId.values.toList()
    }

    /**
     * A readable, printable version of your saved sparks. Handy for keeping the
     * vault outside the app -> paste into notes, print, share.
     */
    fun vaultToMarkdown(export: ColliderExport): String {
        val deckNames = export.decks.associate { it.id to it.name }
        val sb = StringBuilder()
        sb.appendLine("# Elendheim Spark - Vault")
        sb.appendLine()
        if (export.vault.isEmpty()) {
            sb.appendLine("_No saved ideas yet._")
            return sb.toString()
        }
        for (item in export.vault.sortedByDescending { it.createdAt }) {
            val line = item.picks.joinToString(" x ") { it.text }
            val star = if (item.favorite) " (favorite)" else ""
            sb.appendLine("## $line$star")
            sb.appendLine()
            sb.appendLine("- Deck: ${deckNames[item.deckId] ?: "unknown"}")
            if (item.tags.isNotEmpty()) sb.appendLine("- Tags: ${item.tags.joinToString(", ")}")
            if (item.note.isNotBlank()) {
                sb.appendLine()
                sb.appendLine(item.note)
            }
            sb.appendLine()
        }
        return sb.toString()
    }
}
