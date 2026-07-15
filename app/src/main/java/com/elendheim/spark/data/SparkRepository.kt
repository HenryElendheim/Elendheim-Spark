package com.elendheim.spark.data

import com.elendheim.spark.model.ColliderExport
import com.elendheim.spark.model.Deck
import com.elendheim.spark.model.SavedCollision
import com.elendheim.spark.model.Wheel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The single door between the app and its stored data.
 *
 * Screens and view-models talk to this, never to Room directly. It hands out
 * clean model objects (not database rows), owns first-run seeding, and hosts
 * the export/import operations by pairing the pure logic in [ExportImport] with
 * the database.
 */
class SparkRepository(private val dao: SparkDao) {

    // Live streams the UI observes. Rows are mapped to pure model types on the way out.
    val decks: Flow<List<Deck>> = dao.observeDecks().map { list -> list.map { it.toModel() } }
    val wheels: Flow<List<Wheel>> = dao.observeWheels().map { list -> list.map { it.toModel() } }
    val vault: Flow<List<SavedCollision>> = dao.observeVault().map { list -> list.map { it.toModel() } }

    // --- First run ---

    /**
     * Seed the starter decks the first time the app opens (when there are no
     * decks yet). [now] is passed in so the caller controls the timestamps.
     */
    suspend fun seedIfEmpty(now: Long) {
        if (dao.allDecks().isNotEmpty()) return
        val (decks, wheels) = SeedData.build(now)
        wheels.forEach { dao.upsertWheel(it.toEntity()) }
        decks.forEach { dao.upsertDeck(it.toEntity()) }
    }

    // --- Decks & wheels (curation) ---

    suspend fun getDeck(id: String): Deck? = dao.getDeck(id)?.toModel()
    suspend fun upsertDeck(deck: Deck) = dao.upsertDeck(deck.toEntity())
    suspend fun deleteDeck(deck: Deck) = dao.deleteDeck(deck.toEntity())

    suspend fun getWheel(id: String): Wheel? = dao.getWheel(id)?.toModel()
    suspend fun upsertWheel(wheel: Wheel) = dao.upsertWheel(wheel.toEntity())
    suspend fun deleteWheel(wheel: Wheel) = dao.deleteWheel(wheel.toEntity())

    // --- Vault ---

    suspend fun saveCollision(collision: SavedCollision) =
        dao.upsertSavedCollision(collision.toEntity())

    /**
     * Save a fresh spark, assigning it the next sequential save number so the
     * vault can label it (e.g. "App Ideas #4"). Snapshots the picks' text.
     */
    suspend fun saveNewCollision(deckId: String, picks: List<com.elendheim.spark.model.Pick>, now: Long) {
        val number = dao.maxSaveNumber() + 1
        dao.upsertSavedCollision(
            SavedCollision(
                id = newId(),
                deckId = deckId,
                picks = picks,
                createdAt = now,
                saveNumber = number
            ).toEntity()
        )
    }

    suspend fun updateCollision(collision: SavedCollision) =
        dao.updateSavedCollision(collision.toEntity())

    suspend fun deleteCollision(collision: SavedCollision) =
        dao.deleteSavedCollision(collision.toEntity())

    // --- Export / import ---

    /** Gather the whole engine into an export object stamped with [now]. */
    suspend fun buildExport(now: Long): ColliderExport = ExportImport.buildExport(
        decks = dao.allDecks().map { it.toModel() },
        wheels = dao.allWheels().map { it.toModel() },
        vault = dao.allSavedCollisions().map { it.toModel() },
        exportedAt = now
    )

    /**
     * Apply an imported file. The merge/replace decision is resolved by the
     * pure [ExportImport.resolve]; here we just persist the outcome. On a
     * replace we clear first so removed items really disappear.
     */
    suspend fun applyImport(incoming: ColliderExport, mode: ExportImport.ImportMode) {
        val current = ExportImport.Resolved(
            decks = dao.allDecks().map { it.toModel() },
            wheels = dao.allWheels().map { it.toModel() },
            vault = dao.allSavedCollisions().map { it.toModel() }
        )
        val resolved = ExportImport.resolve(current, incoming, mode)

        if (mode == ExportImport.ImportMode.REPLACE_ALL) {
            dao.clearVault()
            dao.clearDecks()
            dao.clearWheels()
        }
        resolved.wheels.forEach { dao.upsertWheel(it.toEntity()) }
        resolved.decks.forEach { dao.upsertDeck(it.toEntity()) }
        resolved.vault.forEach { dao.upsertSavedCollision(it.toEntity()) }
    }

    /** Erase everything. Used by full-restore imports and by the round-trip test. */
    suspend fun wipeAll() {
        dao.clearVault()
        dao.clearDecks()
        dao.clearWheels()
    }
}
