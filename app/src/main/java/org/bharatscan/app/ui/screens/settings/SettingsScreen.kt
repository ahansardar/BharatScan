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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.bharatscan.app.R
import org.bharatscan.app.domain.ExportQuality
import org.bharatscan.app.ui.Navigation
import org.bharatscan.app.ui.components.BrandTitle
import org.bharatscan.app.ui.components.BackButton
import org.bharatscan.app.ui.components.DigitalBharatBackground
import org.bharatscan.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onChooseDirectoryClick: () -> Unit,
    onResetExportDirClick: () -> Unit,
    onExportFormatChanged: (ExportFormat) -> Unit,
    onExportQualityChanged: (ExportQuality) -> Unit,
    onLanguageChanged: (LanguageOption) -> Unit,
    onBack: () -> Unit,
    navigation: Navigation,
    onSecurityChanged: (Boolean) -> Unit,
) {
    BackHandler { onBack() }
    DigitalBharatBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        BrandTitle(height = 24.dp)
                    },
                    navigationIcon = {
                        Box(modifier = Modifier.padding(8.dp)) {
                            BackButton(onBack)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            ,bottomBar = {
                SettingsBottomBar(
                    onHomeClick = navigation.toHomeScreen,
                    onDocumentsClick = navigation.toDocumentsScreen,
                    onScannerClick = navigation.toCameraScreen,
                    onSettingsClick = navigation.toSettingsScreen ?: {},
                )
            }
        ) { paddingValues ->
            SettingsContent(
                uiState,
                onChooseDirectoryClick,
                onResetExportDirClick,
                onExportFormatChanged,
                onExportQualityChanged,
                onLanguageChanged,
                onSecurityChanged,
                modifier = Modifier.padding(paddingValues))
        }
    }
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onChooseDirectoryClick: () -> Unit,
    onResetExportDirClick: () -> Unit,
    onExportFormatChanged: (ExportFormat) -> Unit,
    onExportQualityChanged: (ExportQuality) -> Unit,
    onLanguageChanged: (LanguageOption) -> Unit,
    onSecurityChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        SectionHeader(title = stringResource(R.string.settings))

        SettingsSectionCard {
            Text(
                stringResource(R.string.export_quality),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ExportQuality.entries.reversed().forEach { quality ->
                Surface(
                    onClick = { onExportQualityChanged(quality) },
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = uiState.exportQuality == quality,
                            onClick = { onExportQualityChanged(quality) },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.secondary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(quality.labelResource),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (uiState.exportQuality == quality) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        SettingsSectionCard {
            Text(
                stringResource(R.string.export_format),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ExportFormat.entries.forEach { format ->
                Surface(
                    onClick = { onExportFormatChanged(format) },
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = uiState.exportFormat == format,
                            onClick = { onExportFormatChanged(format) },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.secondary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            format.name,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (uiState.exportFormat == format) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        SettingsSectionCard {
            Text(
                stringResource(R.string.language_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LanguageOption.entries.forEach { option ->
                Surface(
                    onClick = { onLanguageChanged(option) },
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = uiState.languageOption == option,
                            onClick = { onLanguageChanged(option) },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.secondary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(option.labelRes),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (uiState.languageOption == option) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        SettingsSectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.security),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.security_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = uiState.requireAuth,
                    onCheckedChange = onSecurityChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BharatWhite,
                        checkedTrackColor = BharatSaffron
                    )
                )
            }
        }
    }
}


@Composable
private fun SettingsSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SettingsBottomBar(
    onHomeClick: () -> Unit,
    onDocumentsClick: () -> Unit,
    onScannerClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 4.dp
    ) {
        NavigationBarItem(
            selected = false,
            onClick = onHomeClick,
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.home_title)) }
        )
        NavigationBarItem(
            selected = false,
            onClick = onDocumentsClick,
            icon = { Icon(Icons.Default.Description, contentDescription = null) },
            label = { Text(stringResource(R.string.documents)) }
        )
        NavigationBarItem(
            selected = false,
            onClick = onScannerClick,
            icon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
            label = { Text(stringResource(R.string.scanner)) }
        )
        NavigationBarItem(
            selected = true,
            onClick = onSettingsClick,
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = {
                Text(stringResource(R.string.settings))
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BharatWhite,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = BharatSaffron
            )
        )
    }
}


