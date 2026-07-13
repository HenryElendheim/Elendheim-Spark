package com.elendheim.spark.data

import com.elendheim.spark.model.Deck
import com.elendheim.spark.model.Entry
import com.elendheim.spark.model.Pick
import com.elendheim.spark.model.SavedCollision
import com.elendheim.spark.model.Wheel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The export/import contract. The headline guarantee: export -> wipe -> import
 * reproduces everything exactly. It is a real test, as the build plan insists.
 */
class ExportImportTest {

    private fun sampleData(): Triple<List<Deck>, List<Wheel>, List<SavedCollision>> {
        val wheels = listOf(
            Wheel("w1", "Domain", "#E0555A", listOf(Entry("e1", "pets"), Entry("e2", "music", weight = 3))),
            Wheel("w2", "Twist", "#4FA6D9", listOf(Entry("e3", "no cloud at all")))
        )
        val decks = listOf(
            Deck("d1", "App Ideas", listOf("w1", "w2"), createdAt = 100)
        )
        val vault = listOf(
            SavedCollision(
                id = "s1",
                deckId = "d1",
                picks = listOf(Pick("Domain", "pets"), Pick("Twist", "no cloud at all")),
                note = "a private, offline pet tracker",
                tags = listOf("keep", "mvp"),
                favorite = true,
                createdAt = 200
            )
        )
        return Triple(decks, wheels, vault)
    }

    @Test
    fun encode_then_decode_is_identical() {
        val (decks, wheels, vault) = sampleData()
        val export = ExportImport.buildExport(decks, wheels, vault, exportedAt = 12345)

        val text = ExportImport.encode(export)
        val back = ExportImport.decode(text)

        assertEquals(export, back)
    }

    @Test
    fun round_trip_replace_all_reproduces_everything_exactly() {
        val (decks, wheels, vault) = sampleData()
        val export = ExportImport.buildExport(decks, wheels, vault, exportedAt = 1)

        // Simulate export -> wipe -> import (replace all) onto an empty device.
        val text = ExportImport.encode(export)
        val incoming = ExportImport.decode(text)
        val empty = ExportImport.Resolved(emptyList(), emptyList(), emptyList())

        val resolved = ExportImport.resolve(empty, incoming, ExportImport.ImportMode.REPLACE_ALL)

        assertEquals(decks, resolved.decks)
        assertEquals(wheels, resolved.wheels)
        assertEquals(vault, resolved.vault)
    }

    @Test
    fun merge_adds_new_and_keeps_the_newer_vault_copy() {
        val (decks, wheels, vault) = sampleData()

        // Existing device already has the deck/wheels and an older copy of s1.
        val olderS1 = vault[0].copy(note = "old note", createdAt = 100)
        val current = ExportImport.Resolved(decks, wheels, listOf(olderS1))

        // Incoming file has a newer s1 plus a brand-new saved idea s2.
        val newerS1 = vault[0].copy(note = "new note", createdAt = 300)
        val s2 = SavedCollision(
            id = "s2", deckId = "d1",
            picks = listOf(Pick("Domain", "music")), createdAt = 400
        )
        val incoming = ExportImport.buildExport(decks, wheels, listOf(newerS1, s2), exportedAt = 5)

        val resolved = ExportImport.resolve(current, incoming, ExportImport.ImportMode.MERGE)

        // Decks and wheels are not duplicated.
        assertEquals(1, resolved.decks.size)
        assertEquals(2, resolved.wheels.size)
        // The newer s1 wins, and s2 is added.
        val mergedS1 = resolved.vault.first { it.id == "s1" }
        assertEquals("new note", mergedS1.note)
        assertTrue(resolved.vault.any { it.id == "s2" })
        assertEquals(2, resolved.vault.size)
    }

    @Test
    fun lenient_parsing_tolerates_unknown_fields_from_a_future_version() {
        // A file written by a hypothetical newer app with an extra field.
        val futureJson = """
            {
              "schemaVersion": 1,
              "exportedAt": 1,
              "decks": [],
              "wheels": [],
              "vault": [],
              "somethingFromTheFuture": true
            }
        """.trimIndent()

        val parsed = ExportImport.decode(futureJson)
        assertEquals(1, parsed.schemaVersion)
        assertTrue(parsed.decks.isEmpty())
    }
}
