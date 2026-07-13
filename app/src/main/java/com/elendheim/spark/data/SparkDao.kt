package com.elendheim.spark.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * The one data-access interface for the whole app. Room generates the
 * implementation. Reads return Flows so the UI updates itself whenever the
 * underlying data changes.
 */
@Dao
interface SparkDao {

    // --- Decks ---

    @Query("SELECT * FROM decks ORDER BY createdAt ASC")
    fun observeDecks(): Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks WHERE id = :id")
    suspend fun getDeck(id: String): DeckEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDeck(deck: DeckEntity)

    @Delete
    suspend fun deleteDeck(deck: DeckEntity)

    // --- Wheels ---

    @Query("SELECT * FROM wheels")
    fun observeWheels(): Flow<List<WheelEntity>>

    @Query("SELECT * FROM wheels WHERE id = :id")
    suspend fun getWheel(id: String): WheelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWheel(wheel: WheelEntity)

    @Delete
    suspend fun deleteWheel(wheel: WheelEntity)

    // --- Vault (saved collisions) ---

    @Query("SELECT * FROM saved_collisions ORDER BY createdAt DESC")
    fun observeVault(): Flow<List<SavedCollisionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavedCollision(collision: SavedCollisionEntity)

    @Update
    suspend fun updateSavedCollision(collision: SavedCollisionEntity)

    @Delete
    suspend fun deleteSavedCollision(collision: SavedCollisionEntity)

    // --- Bulk reads used by export ---

    @Query("SELECT * FROM decks")
    suspend fun allDecks(): List<DeckEntity>

    @Query("SELECT * FROM wheels")
    suspend fun allWheels(): List<WheelEntity>

    @Query("SELECT * FROM saved_collisions")
    suspend fun allSavedCollisions(): List<SavedCollisionEntity>

    // --- Wipe, used by "replace all" import and the round-trip test ---

    @Query("DELETE FROM decks")
    suspend fun clearDecks()

    @Query("DELETE FROM wheels")
    suspend fun clearWheels()

    @Query("DELETE FROM saved_collisions")
    suspend fun clearVault()
}
