package com.elendheim.spark.engine

import com.elendheim.spark.model.Deck
import com.elendheim.spark.model.Entry
import com.elendheim.spark.model.Pick
import com.elendheim.spark.model.Wheel
import kotlin.random.Random

/**
 * The collision engine: the soul of the app.
 *
 * It is a small, pure Kotlin object with no Android, no database and no UI in
 * sight. You give it a deck, its wheels and a source of randomness, and it
 * hands back picks. Because it is pure it is trivially testable and could be
 * reused anywhere.
 *
 * Randomness is passed in (not created inside) so callers -> and tests <- stay
 * in control of the outcome.
 */
object ColliderEngine {

    /**
     * The basic roll: pick one entry from each wheel in the deck, in the deck's
     * wheel order, and assemble the result.
     *
     * Wheels with no entries are skipped -> an empty wheel simply contributes
     * nothing rather than crashing the roll.
     */
    fun collide(deck: Deck, wheels: List<Wheel>, rng: Random): List<Pick> {
        val byId = wheels.associateBy { it.id }
        return deck.wheelIds
            .mapNotNull { byId[it] }              // keep deck order, ignore missing
            .filter { it.entries.isNotEmpty() }   // skip empty wheels
            .map { wheel ->
                val entry = weightedPick(wheel.entries, rng)
                Pick(wheel.name, entry.text)
            }
    }

    /**
     * Reroll only the wheels that are not locked.
     *
     * [current] is the collision on screen. [lockedWheelNames] are the wheels
     * the user pinned. Locked picks are carried over unchanged; everything else
     * is rolled fresh. This is the control that turns a dumb randomiser into a
     * tool: lock "pets", reroll the rest until the combo clicks.
     */
    fun reroll(
        deck: Deck,
        wheels: List<Wheel>,
        current: List<Pick>,
        lockedWheelNames: Set<String>,
        rng: Random
    ): List<Pick> {
        val fresh = collide(deck, wheels, rng)
        val currentByName = current.associateBy { it.wheelName }
        // For each freshly rolled pick, keep the old one if that wheel is locked.
        return fresh.map { newPick ->
            if (newPick.wheelName in lockedWheelNames) {
                currentByName[newPick.wheelName] ?: newPick
            } else {
                newPick
            }
        }
    }

    /**
     * Mutate: keep the whole current collision but reroll exactly one wheel.
     *
     * This is the melody-dice move -> nudge a nearly-great idea one axis at a
     * time instead of blowing it all up. Pass a [wheelName] to mutate a
     * specific wheel, or null to mutate a random eligible one.
     *
     * A wheel is only mutated to a genuinely different entry when it has more
     * than one option, so a mutate always feels like it did something.
     */
    fun mutate(
        deck: Deck,
        wheels: List<Wheel>,
        current: List<Pick>,
        wheelName: String? = null,
        rng: Random
    ): List<Pick> {
        if (current.isEmpty()) return current
        val byName = wheels.associateBy { it.name }

        // Which wheel are we changing? Either the requested one, or a random
        // pick from the wheels that actually have something to change.
        val target = wheelName ?: current
            .map { it.wheelName }
            .filter { name -> (byName[name]?.entries?.size ?: 0) > 1 }
            .let { candidates -> if (candidates.isEmpty()) return current else candidates.random(rng) }

        val wheel = byName[target] ?: return current
        if (wheel.entries.isEmpty()) return current

        val previousText = current.firstOrNull { it.wheelName == target }?.text
        val newEntry = rollDifferent(wheel.entries, previousText, rng)

        return current.map { pick ->
            if (pick.wheelName == target) pick.copy(text = newEntry.text) else pick
        }
    }

    /**
     * Weighted random pick. An entry's weight is how many "tickets" it holds;
     * more tickets means a better chance. With every weight at 1 this is just a
     * uniform random pick.
     *
     * Falls back gracefully: an empty list is a programmer error we guard by
     * returning a blank entry, and a total weight of zero degrades to uniform.
     */
    fun weightedPick(entries: List<Entry>, rng: Random): Entry {
        if (entries.isEmpty()) return Entry(id = "", text = "", weight = 1)

        val total = entries.sumOf { maxOf(it.weight, 0) }
        if (total <= 0) return entries.random(rng)   // all weights zero -> uniform

        var roll = rng.nextInt(total)
        for (e in entries) {
            roll -= maxOf(e.weight, 0)
            if (roll < 0) return e
        }
        return entries.last()                        // rounding safety net
    }

    /**
     * Pick an entry that differs from [avoidText] when possible, so a mutate on
     * a multi-entry wheel visibly changes. If avoiding is impossible (only one
     * entry, or all identical) it simply returns a normal weighted pick.
     */
    private fun rollDifferent(entries: List<Entry>, avoidText: String?, rng: Random): Entry {
        val options = if (avoidText == null) entries
        else entries.filter { it.text != avoidText }.ifEmpty { entries }
        return weightedPick(options, rng)
    }
}
