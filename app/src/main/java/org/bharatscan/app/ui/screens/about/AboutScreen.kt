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
package org.bharatscan.app.ui.screens.about

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import org.bharatscan.app.BuildConfig
import org.bharatscan.app.R
import org.bharatscan.app.ui.components.BrandTitle
import org.bharatscan.app.ui.components.BackButton
import org.bharatscan.app.ui.components.DigitalBharatBackground
import org.bharatscan.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    aboutUiState: AboutUiState,
    onBack: () -> Unit,
    onCopyLogs: () -> Unit,
    onSaveLogs: () -> Unit,
    onContactWithLastImageClicked: () -> Unit,
    onViewLibraries: () -> Unit,
) {
    val showLicenseDialog = rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    BackHandler { onBack() }
    
    DigitalBharatBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        BrandTitle(height = 26.dp)
                    },
                    navigationIcon = { BackButton(onBack) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        ) { paddingValues ->
            AboutContent(
                modifier = Modifier.padding(paddingValues),
                aboutUiState,
                onCopyLogs,
                onSaveLogs,
                onContactWithLastImageClicked,
                showLicenseDialog,
                onViewLibraries)
        }
    }
    
    if (showLicenseDialog.value) {
        LicenseBottomSheet(sheetState, onDismiss = { showLicenseDialog.value = false })
    }
}

@Composable
fun AboutContent(
    modifier: Modifier = Modifier,
    aboutUiState: AboutUiState,
    onCopyLogs: () -> Unit,
    onSaveLogs: () -> Unit,
    onContactWithLastImageClicked: () -> Unit,
    showLicenseDialog: MutableState<Boolean>,
    onViewLibraries: () -> Unit,
    ) {

    val context = LocalContext.current
    val linkedProfileUrl = stringResource(R.string.creator_linked_url)
    val githubProfileUrl = stringResource(R.string.creator_github_url)
    val creatorEmail = stringResource(R.string.creator_email)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AboutHeroCard()

        FeatureCard(
            title = stringResource(R.string.secure_private_title),
            body = stringResource(R.string.secure_private_body),
            icon = Icons.Default.Shield,
            accent = Color(0xFF1B5E20),
            background = Color(0xFFF2F6F3)
        )

        AboutInfoCard(
            title = stringResource(R.string.creator_title),
            subtitle = stringResource(R.string.creator_name),
            icon = Icons.Default.Person
        )

        AboutInfoCard(
            title = stringResource(R.string.creator_linked_profile),
            subtitle = stringResource(R.string.creator_linked_label),
            icon = Icons.Default.Language,
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, linkedProfileUrl.toUri())
                context.startActivity(intent)
            }
        )

        AboutInfoCard(
            title = stringResource(R.string.creator_github_profile),
            subtitle = stringResource(R.string.creator_github_label),
            icon = Icons.Default.Code,
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, githubProfileUrl.toUri())
                context.startActivity(intent)
            }
        )

        AboutInfoCard(
            title = stringResource(R.string.report_errors_title),
            subtitle = stringResource(R.string.report_errors_body),
            icon = Icons.Default.Email,
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = "mailto:$creatorEmail".toUri()
                }
                context.startActivity(intent)
            }
        )

        SettingsActionRow(
            title = stringResource(R.string.copy_logs),
            icon = Icons.Default.ContentCopy,
            onClick = onCopyLogs
        )
        SettingsActionRow(
            title = stringResource(R.string.save_logs),
            icon = Icons.Default.Save,
            onClick = onSaveLogs
        )

        AboutInfoCard(
            title = stringResource(R.string.contact),
            subtitle = creatorEmail,
            icon = Icons.Default.Email,
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = "mailto:$creatorEmail".toUri()
                }
                context.startActivity(intent)
            }
        )

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun AboutHeroCard() {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(vertical = 28.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .background(BharatNavy, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = ImageBitmap.imageResource(id = R.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    contentScale = ContentScale.Fit
                )
            }
            BrandTitle(height = 36.dp)
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = stringResource(R.string.about_version_display),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Text(
                text = stringResource(R.string.about_tagline),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    body: String,
    icon: ImageVector,
    accent: Color,
    background: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = background),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(accent.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun AboutInfoCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        onClick = { onClick?.invoke() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AboutSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold
        )
        content()
    }
}

@Composable
private fun ContactLink(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.Underline
            )
        )
    }
}

@Composable
private fun ContactLink(icon: androidx.compose.ui.graphics.painter.Painter, text: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.Underline
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
) {
    val resources = LocalResources.current
    val licenseText by remember {
        mutableStateOf(
            resources.openRawResource(R.raw.gpl3)
                .bufferedReader()
                .use { it.readText() }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                "GNU General Public License v3.0",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = licenseText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun CopyLogsButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(stringResource(R.string.copy_logs), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun EmailImageButton(
    aboutUiState: AboutUiState,
    onContactWithLastImageClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = aboutUiState.hasLastCapturedImage,
                onClick = onContactWithLastImageClicked
            )
            .alpha(if (aboutUiState.hasLastCapturedImage) 1f else 0.5f)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(16.dp))
        Text(stringResource(R.string.support_last_image), color = MaterialTheme.colorScheme.onSurface)
    }
}


