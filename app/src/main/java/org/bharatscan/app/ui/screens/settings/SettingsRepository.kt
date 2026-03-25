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
package org.bharatscan.app.ui.screens.settings

import android.content.Context
import androidx.core.net.toUri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.bharatscan.app.domain.ExportQuality

private val Context.dataStore by preferencesDataStore(name = "BharatScan_settings")

class SettingsRepository(private val context: Context) {

    private val EXPORT_DIR_URI = stringPreferencesKey("export_dir_uri")
    private val EXPORT_FORMAT = stringPreferencesKey("export_format")
    private val EXPORT_QUALITY = stringPreferencesKey("export_quality")
    private val REQUIRE_AUTH = booleanPreferencesKey("require_auth")
    private val CUSTOM_CATEGORIES = stringSetPreferencesKey("custom_categories")
    private val APP_LANGUAGE = stringPreferencesKey("app_language")
    private val CHECK_UPDATES_STARTUP = booleanPreferencesKey("check_updates_startup")

    val exportDirUri: Flow<String?> =
        context.dataStore.data.map { prefs ->
            prefs[EXPORT_DIR_URI]
        }

    fun resolveExportDirName(uri: String): String? {
        return DocumentFile.fromTreeUri(context, uri.toUri())?.name
    }

    val exportFormat: Flow<ExportFormat> =
        context.dataStore.data.map { prefs ->
            when (prefs[EXPORT_FORMAT]) {
                "JPEG" -> ExportFormat.JPEG
                "PDF", null -> ExportFormat.PDF
                else -> ExportFormat.PDF
            }
        }

    val exportQuality: Flow<ExportQuality> =
        context.dataStore.data.map { prefs ->
            when (prefs[EXPORT_QUALITY]) {
                "LOW" -> ExportQuality.LOW
                "HIGH" -> ExportQuality.HIGH
                "BALANCED", null -> ExportQuality.BALANCED
                else -> ExportQuality.BALANCED
            }
        }

    val requireAuth: Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[REQUIRE_AUTH] ?: false
        }

    val appLanguageTag: Flow<String?> =
        context.dataStore.data.map { prefs ->
            prefs[APP_LANGUAGE] ?: "system"
        }

    val checkUpdatesAtStartup: Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[CHECK_UPDATES_STARTUP] ?: true
        }

    val customCategories: Flow<List<String>> =
        context.dataStore.data.map { prefs ->
            prefs[CUSTOM_CATEGORIES]
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.sorted()
                ?: emptyList()
        }

    suspend fun setExportDirUri(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri == null) {
                prefs.remove(EXPORT_DIR_URI)
            } else {
                prefs[EXPORT_DIR_URI] = uri
            }
        }
    }

    suspend fun setExportFormat(format: ExportFormat) {
        context.dataStore.edit { prefs ->
            prefs[EXPORT_FORMAT] = format.name
        }
    }

    suspend fun setExportQuality(quality: ExportQuality) {
        context.dataStore.edit { prefs ->
            prefs[EXPORT_QUALITY] = quality.name
        }
    }

    suspend fun setRequireAuth(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[REQUIRE_AUTH] = enabled
        }
    }

    suspend fun setAppLanguage(tag: String?) {
        context.dataStore.edit { prefs ->
            prefs[APP_LANGUAGE] = tag ?: "system"
        }
    }

    suspend fun setCheckUpdatesAtStartup(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[CHECK_UPDATES_STARTUP] = enabled
        }
    }

    suspend fun addCustomCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = prefs[CUSTOM_CATEGORIES] ?: emptySet()
            prefs[CUSTOM_CATEGORIES] = current + trimmed
        }
    }
}

enum class ExportFormat(val mimeType: String) {
    PDF("application/pdf"),
    JPEG("image/jpeg"),
}


