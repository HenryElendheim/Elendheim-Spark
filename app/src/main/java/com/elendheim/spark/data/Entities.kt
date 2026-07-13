package com.elendheim.spark.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.elendheim.spark.model.Deck
import com.elendheim.spark.model.Entry
import com.elendheim.spark.model.Pick
import com.elendheim.spark.model.SavedCollision
import com.elendheim.spark.model.Wheel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room storage classes.
 *
 * These mirror the pure model classes but carry the database annotations. Lists
 * (entries, tags, picks) are stored as small JSON strings via the converters
 * below -> that keeps the schema to a handful of simple tables without a wall of
 * join logic, which is plenty for a local, single-user idea tool.
 *
 * Mapping to and from the pure model happens in the extension functions at the
 * bottom, so the rest of the app only ever sees the clean model types.
 */

@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey val id: String,
    val name: String,
    val wheelIds: List<String>,
    val createdAt: Long
)

@Entity(tableName = "wheels")
data class WheelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorHex: String,
    val entries: List<Entry>
)

@Entity(tableName = "saved_collisions")
data class SavedCollisionEntity(
    @PrimaryKey val id: String,
    val deckId: String,
    val picks: List<Pick>,
    val note: String,
    val tags: List<String>,
    val favorite: Boolean,
    val createdAt: Long
)

/**
 * Converters that let Room store list-typed columns.
 *
 * We reuse kotlinx.serialization (the same library that powers export/import)
 * so there is exactly one definition of how our data turns into text.
 */
class Converters {
    // lenient so a value written by an older/newer build still reads back.
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun stringListToJson(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun jsonToStringList(value: String): List<String> = json.decodeFromString(value)

    @TypeConverter
    fun entryListToJson(value: List<Entry>): String = json.encodeToString(value)

    @TypeConverter
    fun jsonToEntryList(value: String): List<Entry> = json.decodeFromString(value)

    @TypeConverter
    fun pickListToJson(value: List<Pick>): String = json.encodeToString(value)

    @TypeConverter
    fun jsonToPickList(value: String): List<Pick> = json.decodeFromString(value)
}

// --- Mapping between storage rows and pure model objects ---

fun DeckEntity.toModel() = Deck(id, name, wheelIds, createdAt)
fun Deck.toEntity() = DeckEntity(id, name, wheelIds, createdAt)

fun WheelEntity.toModel() = Wheel(id, name, colorHex, entries)
fun Wheel.toEntity() = WheelEntity(id, name, colorHex, entries)

fun SavedCollisionEntity.toModel() = SavedCollision(id, deckId, picks, note, tags, favorite, createdAt)
fun SavedCollision.toEntity() = SavedCollisionEntity(id, deckId, picks, note, tags, favorite, createdAt)
