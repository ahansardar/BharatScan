/*
 * Copyright 2025-2026 Ahan Sardar
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.bharatscan.app.data

import android.content.ContentUris
import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.datastore.core.DataStore
import androidx.core.net.toUri
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.bharatscan.app.RecentDocument
import org.bharatscan.app.RecentDocuments
import java.io.File

private const val EXPORTS_SUBFOLDER = "BharatScan"
private const val MAX_BOOTSTRAP_ITEMS = 50

suspend fun bootstrapRecentDocumentsIfEmpty(
    context: Context,
    recentDocumentsDataStore: DataStore<RecentDocuments>,
) {
    val current = recentDocumentsDataStore.data.first()
    if (current.documentsCount > 0) return

    val bootstrap = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            queryExportedPdfsViaMediaStore(context)
        } else {
            queryExportedPdfsViaFileSystem()
        }
    }

    if (bootstrap.isEmpty()) return

    recentDocumentsDataStore.updateData { existing ->
        if (existing.documentsCount > 0) {
            existing
        } else {
            existing.toBuilder()
                .addAllDocuments(bootstrap)
                .build()
        }
    }
}

private fun queryExportedPdfsViaFileSystem(): List<RecentDocument> {
    val dir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        EXPORTS_SUBFOLDER
    )
    if (!dir.exists() || !dir.isDirectory) return emptyList()

    return dir.listFiles()
        ?.asSequence()
        ?.filter { it.isFile && it.name.lowercase().endsWith(".pdf") }
        ?.sortedByDescending { it.lastModified() }
        ?.take(MAX_BOOTSTRAP_ITEMS)
        ?.map { file ->
            RecentDocument.newBuilder()
                .setFileUri(file.toUri().toString())
                .setFileName(file.name)
                .setCreatedAt(file.lastModified().takeIf { it > 0 } ?: System.currentTimeMillis())
                .build()
        }
        ?.toList()
        ?: emptyList()
}

private fun queryExportedPdfsViaMediaStore(context: Context): List<RecentDocument> {
    val resolver = context.contentResolver
    val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
    val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/$EXPORTS_SUBFOLDER/"

    val projection = arrayOf(
        MediaStore.Downloads._ID,
        MediaStore.Downloads.DISPLAY_NAME,
        MediaStore.Downloads.MIME_TYPE,
        MediaStore.Downloads.DATE_ADDED,
        MediaStore.Downloads.DATE_MODIFIED,
    )

    val selection = "${MediaStore.Downloads.MIME_TYPE}=? AND ${MediaStore.Downloads.RELATIVE_PATH}=?"
    val selectionArgs = arrayOf("application/pdf", relativePath)
    val sortOrder = "${MediaStore.Downloads.DATE_MODIFIED} DESC, ${MediaStore.Downloads.DATE_ADDED} DESC"

    val docs = mutableListOf<RecentDocument>()
    resolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
        val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_ADDED)
        val modifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_MODIFIED)

        while (cursor.moveToNext() && docs.size < MAX_BOOTSTRAP_ITEMS) {
            val id = cursor.getLong(idCol)
            val name = cursor.getString(nameCol).orEmpty()
            val dateModifiedSec = cursor.getLong(modifiedCol)
            val dateAddedSec = cursor.getLong(addedCol)
            val createdAtMs = when {
                dateModifiedSec > 0 -> dateModifiedSec * 1000L
                dateAddedSec > 0 -> dateAddedSec * 1000L
                else -> System.currentTimeMillis()
            }
            val uri = ContentUris.withAppendedId(collection, id)

            docs += RecentDocument.newBuilder()
                .setFileUri(uri.toString())
                .setFileName(name.ifBlank { "document.pdf" })
                .setCreatedAt(createdAtMs)
                .setPageCount(resolvePageCountSafely(context, uri))
                .build()
        }
    }

    return docs
}

private fun resolvePageCountSafely(context: Context, uri: android.net.Uri): Int {
    return try {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return 0
        pfd.use {
            PdfRenderer(it).use { renderer -> renderer.pageCount }
        }
    } catch (_: Exception) {
        0
    }
}

