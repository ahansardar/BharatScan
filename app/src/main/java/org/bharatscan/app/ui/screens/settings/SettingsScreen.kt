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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.bharatscan.app.R
import org.bharatscan.app.domain.ExportQuality
import org.bharatscan.app.ui.Navigation
import org.bharatscan.app.ui.components.BrandTitle
import org.bharatscan.app.ui.components.BackButton
import org.bharatscan.app.ui.components.DigitalBharatBackground
import org.bharatscan.app.ui.theme.*
import org.bharatscan.app.update.UpdateUiState

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
    updateState: UpdateUiState,
    onCheckForUpdates: () -> Unit,
    onCheckAtStartupChanged: (Boolean) -> Unit,
    onStartTutorial: () -> Unit,
) {
    BackHandler { onBack() }
    DigitalBharatBackground(
        backgroundImageRes = R.drawable.settings_bg,
        backgroundImageContentScale = ContentScale.Crop,
        backgroundImageAlignment = Alignment.Center,
        backgroundImageTint = Color(0xFFF8F2EA),
        backgroundImageTintAlpha = 0.35f,
        backgroundImageScale = 3.08f,
        useDecorations = false,
        showChakra = false
    ) {
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
                        containerColor = Color(0xFFF8F5EF).copy(alpha = 0.55f),
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
                updateState,
                onCheckForUpdates,
                onCheckAtStartupChanged,
                onStartTutorial,
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
    updateState: UpdateUiState,
    onCheckForUpdates: () -> Unit,
    onCheckAtStartupChanged: (Boolean) -> Unit,
    onStartTutorial: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLanguageSheet by remember { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxSize()
            .padding(18.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SectionHeader(title = stringResource(R.string.settings))

        SettingsSectionCard {
            Text(
                stringResource(R.string.export_quality),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            SectionDivider()
            Spacer(Modifier.height(8.dp))

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
                modifier = Modifier.padding(bottom = 6.dp)
            )
            SectionDivider()
            Spacer(Modifier.height(8.dp))

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
                modifier = Modifier.padding(bottom = 6.dp)
            )
            SectionDivider()
            Spacer(Modifier.height(10.dp))

            LanguageSelectorRow(
                selected = uiState.languageOption,
                selectedLabel = stringResource(uiState.languageOption.labelRes),
                onClick = { showLanguageSheet = true }
            )
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

        SettingsSectionCard {
            Text(
                text = stringResource(R.string.update_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            SectionDivider()
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.update_check_startup),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.update_check_startup_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = uiState.checkUpdatesAtStartup,
                    onCheckedChange = onCheckAtStartupChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BharatWhite,
                        checkedTrackColor = BharatSaffron
                    )
                )
            }

            Spacer(Modifier.height(12.dp))
            SectionDivider()
            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onCheckForUpdates,
                enabled = !updateState.isChecking,
                colors = ButtonDefaults.buttonColors(containerColor = BharatSaffron)
            ) {
                if (updateState.isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = BharatWhite,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text = stringResource(R.string.check_for_updates),
                    color = BharatWhite,
                    fontWeight = FontWeight.Bold
                )
            }

            updateState.statusMessage?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            updateState.downloadStatus?.let { status ->
                Spacer(Modifier.height(10.dp))
                when (status.status) {
                    android.app.DownloadManager.STATUS_RUNNING,
                    android.app.DownloadManager.STATUS_PENDING,
                    android.app.DownloadManager.STATUS_PAUSED -> {
                        val progress = status.progress
                        if (progress != null) {
                            LinearProgressIndicator(progress = progress)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "${(progress * 100).toInt()}% downloaded",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        } else {
                            LinearProgressIndicator()
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.downloading_update),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    android.app.DownloadManager.STATUS_FAILED -> {
                        Text(
                            text = stringResource(R.string.update_download_failed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        SettingsSectionCard {
            Text(
                text = stringResource(R.string.tutorial_start_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            SectionDivider()
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.tutorial_start_desc),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onStartTutorial,
                colors = ButtonDefaults.buttonColors(containerColor = BharatSaffron)
            ) {
                Text(
                    text = stringResource(R.string.tutorial_start_button),
                    color = BharatWhite,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showLanguageSheet) {
        LanguagePickerSheet(
            current = uiState.languageOption,
            onDismiss = { showLanguageSheet = false },
            onSelect = { option ->
                onLanguageChanged(option)
                showLanguageSheet = false
            }
        )
    }
}

@Composable
private fun LanguageSelectorRow(
    selected: LanguageOption,
    selectedLabel: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        BharatSaffron.copy(alpha = 0.7f),
                        BharatWhite.copy(alpha = 0.9f),
                        BharatGreen.copy(alpha = 0.7f)
                    )
                )
            )
            .clickable { onClick() }
            .padding(1.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(15.dp),
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .heightIn(min = 52.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageBadge(option = selected, size = 38.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.language_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = selectedLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePickerSheet(
    current: LanguageOption,
    onDismiss: () -> Unit,
    onSelect: (LanguageOption) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.language_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(LanguageOption.entries) { option ->
                    Surface(
                        onClick = { onSelect(option) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (option == current)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        else
                            MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LanguageBadge(option = option, size = 30.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stringResource(option.labelRes),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (option == current) FontWeight.Bold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (option == current) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageBadge(option: LanguageOption, size: Dp) {
    val (bg, fg) = when (option) {
        LanguageOption.MATCH_DEVICE -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.primary
        LanguageOption.ENGLISH -> NavyGlow to MaterialTheme.colorScheme.primary
        LanguageOption.HINDI -> BharatSaffron.copy(alpha = 0.2f) to BharatSaffronDeep
        LanguageOption.BENGALI -> BharatGreen.copy(alpha = 0.2f) to BharatGreen
        LanguageOption.TELUGU -> BharatChakra.copy(alpha = 0.2f) to BharatChakra
        LanguageOption.MARATHI -> BharatSaffron.copy(alpha = 0.18f) to BharatSaffronDeep
        LanguageOption.TAMIL -> BharatGreen.copy(alpha = 0.18f) to BharatGreen
        LanguageOption.URDU -> BharatChakra.copy(alpha = 0.18f) to BharatChakra
        LanguageOption.GUJARATI -> BharatSaffron.copy(alpha = 0.18f) to BharatSaffronDeep
        LanguageOption.KANNADA -> BharatGreen.copy(alpha = 0.18f) to BharatGreen
        LanguageOption.MALAYALAM -> BharatChakra.copy(alpha = 0.18f) to BharatChakra
        LanguageOption.ODIA -> BharatSaffron.copy(alpha = 0.18f) to BharatSaffronDeep
        LanguageOption.PUNJABI -> BharatGreen.copy(alpha = 0.18f) to BharatGreen
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bg,
        modifier = Modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            val letter = when (option) {
                LanguageOption.MATCH_DEVICE -> "D"
                LanguageOption.ENGLISH -> "E"
                LanguageOption.HINDI -> "ह"
                LanguageOption.BENGALI -> "ব"
                LanguageOption.TELUGU -> "త"
                LanguageOption.MARATHI -> "म"
                LanguageOption.TAMIL -> "த"
                LanguageOption.URDU -> "ا"
                LanguageOption.GUJARATI -> "ગ"
                LanguageOption.KANNADA -> "ಕ"
                LanguageOption.MALAYALAM -> "മ"
                LanguageOption.ODIA -> "ଓ"
                LanguageOption.PUNJABI -> "ਪ"
            }
            Text(
                text = letter,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = fg
            )
        }
    }
}


@Composable
private fun SettingsSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, OutlineSoft),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        color = DividerColor.copy(alpha = 0.6f),
        thickness = 1.dp
    )
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


