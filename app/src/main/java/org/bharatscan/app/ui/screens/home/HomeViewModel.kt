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
package org.bharatscan.app.ui.screens.home

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.bharatscan.app.AppContainer
import org.bharatscan.app.RecentDocuments
import java.io.File

class HomeViewModel(appContainer: AppContainer, appContext: Context): ViewModel() {

    private val recentDocumentsDataStore = appContainer.recentDocumentsDataStore
    private val settingsRepository = appContainer.settingsRepository
    private val context = appContext

    val recentDocuments: StateFlow<List<RecentDocumentUiState>> =
        recentDocumentsDataStore.data.map {
            it.documentsList.mapNotNull { doc ->
                var fileName = doc.fileName
                var uri: Uri? = null
                if (doc.fileUri.isNullOrEmpty()) {
                    if (!doc.filePath.isNullOrEmpty()) {
                        val file = File(doc.filePath)
                        uri = file.toUri()
                        fileName = file.name
                    }
                } else {
                    uri = doc.fileUri.toUri()
                }
                if (uri != null) {
                    RecentDocumentUiState(
                        fileUri = uri,
                        fileName = fileName,
                        saveTimestamp = doc.createdAt,
                        pageCount = doc.pageCount,
                        categoryId = doc.category.takeIf { it.isNotBlank() },
                    )
                } else null
            }.filter { item -> uriExists(appContext, item.fileUri) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val customCategories: StateFlow<List<String>> =
        settingsRepository.customCategories
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    private fun uriExists(context: Context, uri: Uri): Boolean {
        return if (uri.scheme == "file") {
            File(uri.path.orEmpty()).exists()
        } else {
            try {
                DocumentFile.fromSingleUri(context, uri)?.exists() == true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun deleteRecentDocument(document: RecentDocumentUiState) {
        viewModelScope.launch {
            deleteDocumentFile(document.fileUri)
            recentDocumentsDataStore.updateData { current ->
                removeDocumentFromStore(current, document)
            }
        }
    }

    fun deleteRecentDocumentByUri(uri: Uri) {
        val target = recentDocuments.value.firstOrNull { it.fileUri == uri } ?: return
        deleteRecentDocument(target)
    }

    private fun deleteDocumentFile(uri: Uri) {
        if (uri.scheme == "file") {
            File(uri.path.orEmpty()).delete()
            return
        }
        if (uri.scheme == "content") {
            try {
                context.contentResolver.delete(uri, null, null)
            } catch (_: Exception) {
                // ignore
            }
            try {
                DocumentFile.fromSingleUri(context, uri)?.delete()
            } catch (_: Exception) {
                // ignore
            }
        }
    }

    private fun removeDocumentFromStore(
        current: RecentDocuments,
        document: RecentDocumentUiState
    ): RecentDocuments {
        val targetUri = document.fileUri.toString()
        val targetPath = document.fileUri.path
        val filtered = current.documentsList.filterNot { doc ->
            val matchesUri = doc.fileUri.isNotBlank() && doc.fileUri == targetUri
            val matchesPath = !targetPath.isNullOrBlank() && doc.filePath == targetPath
            matchesUri || matchesPath
        }
        return current.toBuilder()
            .clearDocuments()
            .addAllDocuments(filtered)
            .build()
    }

}


