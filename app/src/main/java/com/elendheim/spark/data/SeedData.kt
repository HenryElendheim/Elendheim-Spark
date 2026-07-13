package com.elendheim.spark.data

import com.elendheim.spark.model.Deck
import com.elendheim.spark.model.Entry
import com.elendheim.spark.model.Wheel

/**
 * The starter decks shipped so the app sparks on first launch.
 *
 * A blank app is intimidating; these three decks (from the build plan's
 * Appendix A) show what good wheels look like and are fun immediately. Every
 * entry is fully editable and removable -> this is a starting palette, not a
 * fixed menu.
 *
 * Nothing here touches the database. It just builds model objects; the
 * repository decides when to write them in.
 */
object SeedData {

    /**
     * A small, colourblind-considerate chip palette. Each wheel also always
     * shows its name label in the UI, so meaning never rests on colour alone.
     */
    private val palette = listOf(
        "#E0555A", // soft red (brand)
        "#4FA6D9", // blue
        "#E0A44F", // amber
        "#6FBF73", // green
        "#B588E0", // violet
        "#E07FB0"  // pink
    )

    /** Build a wheel from a name and a plain list of entry texts (all weight 1). */
    private fun wheel(name: String, colorIndex: Int, items: List<String>): Wheel =
        Wheel(
            id = newId(),
            name = name,
            colorHex = palette[colorIndex % palette.size],
            entries = items.map { Entry(id = newId(), text = it, weight = 1) }
        )

    /** Assemble a deck from already-built wheels, preserving their order. */
    private fun deck(name: String, wheels: List<Wheel>, createdAt: Long): Pair<Deck, List<Wheel>> {
        val d = Deck(
            id = newId(),
            name = name,
            wheelIds = wheels.map { it.id },
            createdAt = createdAt
        )
        return d to wheels
    }

    /**
     * Build all starter content. Returns the decks and the flat list of every
     * wheel across them, ready to be written to the database.
     *
     * [now] is passed in (not read from the clock here) so seeding stays
     * deterministic and easy to reason about.
     */
    fun build(now: Long): Pair<List<Deck>, List<Wheel>> {
        val decks = mutableListOf<Deck>()
        val wheels = mutableListOf<Wheel>()

        // --- Deck: App Ideas ---
        val appIdeas = deck(
            "App Ideas",
            listOf(
                wheel("Domain", 0, listOf(
                    "music", "photos", "notes", "health", "focus", "money", "home",
                    "travel", "pets", "sound", "language", "games", "reflection",
                    "the two-of-us", "Nothing-phone"
                )),
                wheel("Mechanic", 1, listOf(
                    "track over time", "mix & match", "generate from a seed",
                    "one-tap capture", "decay unless revisited", "tap-to-reveal",
                    "streak/tally", "roulette/random", "timer/countdown",
                    "compare two things", "curated collection", "guided sequence"
                )),
                wheel("Twist", 2, listOf(
                    "in your note-name format", "no cloud at all", "Elendian flavor",
                    "brutally minimal", "only works while walking",
                    "resurfaces old entries", "fades over time",
                    "dark-gray/soft-red brand", "one screen only", "weirdly personal",
                    "secretly trains a skill"
                ))
            ),
            now
        )
        decks += appIdeas.first
        wheels += appIdeas.second

        // --- Deck: Song Prompts ---
        val songPrompts = deck(
            "Song Prompts",
            listOf(
                wheel("Feeling", 3, listOf(
                    "avoidance", "restless hope", "quiet grief", "defiance",
                    "homesickness", "numb", "tender", "unraveling", "steady", "haunted"
                )),
                wheel("Image", 4, listOf(
                    "a cold window", "an empty chair", "headlights on a wall",
                    "a packed bag", "static on a radio", "a frozen field",
                    "a phone that won't ring", "a door left open"
                )),
                wheel("Move", 5, listOf(
                    "reframe the feeling as installed from outside",
                    "concrete image over statement",
                    "repeat a line that shifts meaning",
                    "a bridge that turns the whole song",
                    "name a person who isn't there"
                )),
                wheel("Constraint", 0, listOf(
                    "three notes only", "no drums till 1:00", "tempo locked at 67",
                    "one-word chorus", "whole song in second person"
                ))
            ),
            now + 1
        )
        decks += songPrompts.first
        wheels += songPrompts.second

        // --- Deck: Game Mechanics ---
        val gameMechanics = deck(
            "Game Mechanics",
            listOf(
                wheel("Core loop", 1, listOf(
                    "turn-based combat", "deck-building", "idle/incremental",
                    "roguelike run", "life-sim tick", "resource conversion"
                )),
                wheel("Mix axis", 2, listOf(
                    "weapon x offhand", "status apply x consume", "trait x mutation",
                    "risk x reward", "time x reversal", "light x sound"
                )),
                wheel("Hook", 3, listOf(
                    "a death-message left for others", "charge-up telegraphs",
                    "fair RNG enemies", "generational inheritance", "emergent culture",
                    "a god who can rewind"
                )),
                wheel("Twist", 4, listOf(
                    "wordless (no text)", "art-free but juicy", "single-file",
                    "everything is data not hardcode", "breeds like evolution",
                    "one more turn"
                ))
            ),
            now + 2
        )
        decks += gameMechanics.first
        wheels += gameMechanics.second

        return decks to wheels
    }
}
