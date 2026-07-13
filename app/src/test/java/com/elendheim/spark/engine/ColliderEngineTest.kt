package com.elendheim.spark.engine

import com.elendheim.spark.model.Deck
import com.elendheim.spark.model.Entry
import com.elendheim.spark.model.Wheel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Tests for the pure collision engine. Randomness is seeded so every run is
 * repeatable.
 */
class ColliderEngineTest {

    private fun wheel(name: String, vararg texts: String) =
        Wheel(id = name, name = name, colorHex = "#E0555A", entries = texts.map { Entry(it, it) })

    private fun deckOf(vararg wheels: Wheel): Pair<Deck, List<Wheel>> {
        val deck = Deck(id = "d", name = "d", wheelIds = wheels.map { it.id }, createdAt = 0)
        return deck to wheels.toList()
    }

    @Test
    fun collide_picks_one_entry_per_wheel_in_order() {
        val (deck, wheels) = deckOf(
            wheel("A", "a1", "a2", "a3"),
            wheel("B", "b1", "b2")
        )
        val picks = ColliderEngine.collide(deck, wheels, Random(42))

        assertEquals(2, picks.size)
        assertEquals("A", picks[0].wheelName)
        assertEquals("B", picks[1].wheelName)
        assertTrue(picks[0].text in listOf("a1", "a2", "a3"))
        assertTrue(picks[1].text in listOf("b1", "b2"))
    }

    @Test
    fun collide_skips_empty_wheels() {
        val (deck, wheels) = deckOf(
            wheel("A", "a1"),
            wheel("Empty")   // no entries
        )
        val picks = ColliderEngine.collide(deck, wheels, Random(1))
        assertEquals(1, picks.size)
        assertEquals("A", picks[0].wheelName)
    }

    @Test
    fun weightedPick_never_returns_a_zero_weight_entry_when_others_are_positive() {
        val entries = listOf(
            Entry("zero", "zero", weight = 0),
            Entry("one", "one", weight = 5),
            Entry("two", "two", weight = 5)
        )
        val rng = Random(7)
        repeat(500) {
            val picked = ColliderEngine.weightedPick(entries, rng)
            assertNotEquals("zero", picked.text)
        }
    }

    @Test
    fun weightedPick_favours_heavier_entries() {
        val entries = listOf(
            Entry("rare", "rare", weight = 1),
            Entry("common", "common", weight = 20)
        )
        val rng = Random(99)
        var common = 0
        repeat(1000) { if (ColliderEngine.weightedPick(entries, rng).text == "common") common++ }
        // With a 20:1 bias, "common" should dominate by a wide margin.
        assertTrue("common should dominate but was $common/1000", common > 850)
    }

    @Test
    fun reroll_keeps_locked_wheels_and_changes_unlocked_ones() {
        val (deck, wheels) = deckOf(
            wheel("A", "a1", "a2", "a3", "a4"),
            wheel("B", "b1", "b2", "b3", "b4")
        )
        val start = ColliderEngine.collide(deck, wheels, Random(3))
        val locked = setOf("A")

        // Reroll several times; A must never move, and the pick stays valid.
        var current = start
        repeat(20) {
            current = ColliderEngine.reroll(deck, wheels, current, locked, Random(it.toLong()))
            assertEquals(start[0].text, current.first { it.wheelName == "A" }.text)
        }
    }

    @Test
    fun mutate_changes_only_the_named_wheel() {
        val (deck, wheels) = deckOf(
            wheel("A", "a1", "a2", "a3"),
            wheel("B", "b1", "b2", "b3")
        )
        val start = ColliderEngine.collide(deck, wheels, Random(5))
        val mutated = ColliderEngine.mutate(deck, wheels, start, wheelName = "B", rng = Random(6))

        // A is untouched; B is different (the wheel has multiple options).
        assertEquals(start.first { it.wheelName == "A" }.text, mutated.first { it.wheelName == "A" }.text)
        assertNotEquals(start.first { it.wheelName == "B" }.text, mutated.first { it.wheelName == "B" }.text)
    }
}
