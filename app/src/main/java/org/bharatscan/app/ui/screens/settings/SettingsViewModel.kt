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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.bharatscan.app.AppContainer
import org.bharatscan.app.domain.ExportQuality

data class SettingsUiState(
    val exportDirUri: String? = null,
    val exportDirName: String? = null,
    val exportFormat: ExportFormat = ExportFormat.PDF,
    val exportQuality: ExportQuality = ExportQuality.BALANCED,
    val requireAuth: Boolean = false,
    val languageOption: LanguageOption = LanguageOption.MATCH_DEVICE,
)

class SettingsViewModel(container: AppContainer) : ViewModel() {

    private val repo = container.settingsRepository

    private val _dirName = MutableStateFlow<String?>(null)
    val dirName: StateFlow<String?> = _dirName

    private data class BaseDirs(val uri: String?, val name: String?)
    private data class BaseFormat(val dirs: BaseDirs, val format: ExportFormat)
    private data class BaseQuality(val base: BaseFormat, val quality: ExportQuality)
    private data class BaseSecurity(val base: BaseQuality, val requireAuth: Boolean)

    val uiState = combine(repo.exportDirUri, dirName) { uri, name ->
        BaseDirs(uri, name)
    }.combine(repo.exportFormat) { dirs, format ->
        BaseFormat(dirs, format)
    }.combine(repo.exportQuality) { base, quality ->
        BaseQuality(base, quality)
    }.combine(repo.requireAuth) { base, requireAuth ->
        BaseSecurity(base, requireAuth)
    }.combine(repo.appLanguageTag) { base, languageTag ->
        SettingsUiState(
            exportDirUri = base.base.base.dirs.uri,
            exportDirName = base.base.base.dirs.name,
            exportFormat = base.base.base.format,
            exportQuality = base.base.quality,
            requireAuth = base.requireAuth,
            languageOption = LanguageOption.fromStoredTag(languageTag),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingsUiState()
    )

    fun setExportDirUri(uri: String?) {
        viewModelScope.launch {
            repo.setExportDirUri(uri)
            refreshExportDirName()
        }
    }

    fun setExportFormat(format: ExportFormat) {
        viewModelScope.launch {
            repo.setExportFormat(format)
        }
    }

    fun setExportQuality(quality: ExportQuality) {
        viewModelScope.launch {
            repo.setExportQuality(quality)
        }
    }

    fun setRequireAuth(enabled: Boolean) {
        viewModelScope.launch {
            repo.setRequireAuth(enabled)
        }
    }

    fun setLanguageOption(option: LanguageOption) {
        viewModelScope.launch {
            repo.setAppLanguage(option.storedTag())
            applyAppLanguage(option.tag)
        }
    }

    fun refreshExportDirName() {
        viewModelScope.launch {
            val uri = repo.exportDirUri.first()
            _dirName.value = uri?.let { repo.resolveExportDirName(it) }
        }
    }
}


