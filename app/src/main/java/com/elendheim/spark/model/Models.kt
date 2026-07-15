package com.elendheim.spark.model

import kotlinx.serialization.Serializable

/**
 * The core data shapes for Elendheim Spark.
 *
 * These are plain, serializable Kotlin classes with no Android or database
 * types in them at all. That is deliberate: the model is the shared language
 * between the engine, the storage layer and the export file, and none of those
 * should drag UI or framework code into it.
 *
 * A deck is a set of wheels for one purpose (App Ideas, Song Prompts...).
 * A wheel is a named list of entries. A collision is one pick from each wheel;
 * saved collisions become your idea vault.
 */

/** A deck groups the wheels you collide together for one kind of idea. */
@Serializable
data class Deck(
    val id: String,
    val name: String,            // "App Ideas", "Song Prompts", "Game Mechanics"
    val wheelIds: List<String>,  // ordered wheels that belong to this deck
    val createdAt: Long
)

/** A wheel is a named, coloured list of raw material you can pick from. */
@Serializable
data class Wheel(
    val id: String,
    val name: String,            // "Domain", "Mechanic", "Twist", "Mood"...
    val colorHex: String,        // drives the chip colour in the UI
    val entries: List<Entry>
)

/** One item on a wheel. Weight biases how often it is picked (see engine). */
@Serializable
data class Entry(
    val id: String,
    val text: String,            // "pets", "decay over time", "Elendian flavor"
    val weight: Int = 1          // higher -> more likely; default 1 is pure random
)

/**
 * A saved idea. The picks store the actual text, not entry ids, on purpose:
 * a spark you kept must stay readable forever even if you later edit or delete
 * the wheel entry it came from. The vault is a permanent record, decoupled
 * from the live wheels.
 */
@Serializable
data class SavedCollision(
    val id: String,
    val deckId: String,
    val picks: List<Pick>,       // snapshot of what was rolled (text, not ids)
    val note: String = "",       // what you want to make of it, added later
    val tags: List<String> = emptyList(),
    val favorite: Boolean = false,
    val createdAt: Long,
    val title: String = "",      // optional custom name; blank -> shown as deck name + number
    val saveNumber: Int = 0      // sequential save number, for a readable label
)

/** One wheel's contribution to a collision: which wheel, and the text picked. */
@Serializable
data class Pick(
    val wheelName: String,
    val text: String
)

/**
 * The whole export file: every deck, wheel and saved idea in one document.
 *
 * schemaVersion plus lenient parsing on import means an export written today
 * still loads into a future version of the app. Never renumber existing fields.
 */
@Serializable
data class ColliderExport(
    val schemaVersion: Int = 1,
    val exportedAt: Long,
    val decks: List<Deck>,
    val wheels: List<Wheel>,
    val vault: List<SavedCollision>
)
