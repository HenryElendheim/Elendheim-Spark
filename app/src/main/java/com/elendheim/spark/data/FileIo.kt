package com.elendheim.spark.data

import android.content.ContentResolver
import android.net.Uri

/**
 * The entire Android file-system surface of the app, in one small place.
 *
 * We use the Storage Access Framework: the user picks the location and the
 * system hands us a Uri. We only ever read or write the exact document the user
 * chose -> no broad storage permissions, nothing behind their back.
 */
object FileIo {

    /** Write [text] into the document the user picked. */
    fun writeText(resolver: ContentResolver, uri: Uri, text: String) {
        // "wt" = write + truncate, so re-exporting over a file fully replaces it.
        resolver.openOutputStream(uri, "wt")?.use { out ->
            out.write(text.toByteArray(Charsets.UTF_8))
        } ?: error("Could not open the chosen file for writing.")
    }

    /** Read the whole text of the document the user picked. */
    fun readText(resolver: ContentResolver, uri: Uri): String =
        resolver.openInputStream(uri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        } ?: error("Could not open the chosen file for reading.")
}
