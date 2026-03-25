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
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bharatscan.app.R
import org.bharatscan.app.ui.Navigation
import org.bharatscan.app.ui.components.AppOverflowMenu
import org.bharatscan.app.ui.components.BackButton
import org.bharatscan.app.ui.components.BrandTitle
import org.bharatscan.app.ui.components.CameraPermissionState
import org.bharatscan.app.ui.components.CategoryStyle
import org.bharatscan.app.ui.components.ConfirmationDialog
import org.bharatscan.app.ui.components.DigitalBharatBackground
import org.bharatscan.app.ui.components.MainActionButton
import org.bharatscan.app.ui.components.categoryStyleFor
import org.bharatscan.app.ui.components.formatDate
import org.bharatscan.app.ui.components.pageCountText
import org.bharatscan.app.ui.screens.export.formatFileSize
import org.bharatscan.app.ui.theme.BharatChakra
import org.bharatscan.app.ui.theme.BharatNavy
import org.bharatscan.app.ui.theme.BharatSaffron
import org.bharatscan.app.ui.theme.BharatSaffronDeep
import org.bharatscan.app.ui.theme.BharatWhite
import org.bharatscan.app.ui.theme.NavyGlow
import org.bharatscan.app.ui.theme.SaffronGlow
import org.bharatscan.app.ui.theme.SurfaceElevated
import org.bharatscan.app.ui.theme.TextSecondary
import org.bharatscan.app.ui.theme.TertiaryContainer
import org.bharatscan.app.ui.uriForFile
import org.bharatscan.app.ui.model.DocumentCategory
import org.bharatscan.app.ui.state.DocumentUiModel
import org.bharatscan.app.update.UpdateInfo
import org.bharatscan.app.update.UpdateUiState
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs

private data class QuickFolderCategory(
    val id: String,
    val titleRes: Int,
    val subtitleRes: Int?,
    val keywords: List<String>,
    val icon: ImageVector,
    val accent: Color,
    val categoryId: String,
)

private data class DocTag(
    val label: String,
    val container: Color,
    val content: Color,
)

private data class FilterOption(
    val id: String,
    val label: String,
)

private enum class HomeTab {
    HOME,
    DOCUMENTS,
    SCANNER,
    SETTINGS,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    cameraPermission: CameraPermissionState,
    currentDocument: DocumentUiModel,
    navigation: Navigation,
    onClearScan: () -> Unit,
    recentDocuments: List<RecentDocumentUiState>,
    customCategories: List<String>,
    onOpenPdf: (Uri) -> Unit,
    onDeleteDocument: (RecentDocumentUiState) -> Unit,
    updateState: UpdateUiState,
    onCheckForUpdates: () -> Unit,
    onInstallUpdate: (UpdateInfo) -> Unit,
) {
    val context = LocalContext.current

    val categories = remember {
        listOf(
            QuickFolderCategory(
                id = "id_proofs",
                titleRes = R.string.id_proofs,
                subtitleRes = null,
                keywords = listOf(
                    "aadhaar",
                    "aadhar",
                    "uidai",
                    "pan",
                    "passport",
                    "voter",
                    "license",
                    "driving",
                    "identity",
                    "id"
                ),
                icon = Icons.Outlined.Fingerprint,
                accent = BharatNavy,
                categoryId = DocumentCategory.ID_PROOFS.id,
            ),
            QuickFolderCategory(
                id = "education",
                titleRes = R.string.education,
                subtitleRes = R.string.education_subtitle,
                keywords = listOf("degree", "certificate", "education", "school", "college", "university", "transcript", "marksheet"),
                icon = Icons.Default.School,
                accent = TertiaryContainer,
                categoryId = DocumentCategory.EDUCATION.id,
            ),
        )
    }

    val categoryCounts = remember(recentDocuments) {
        categories.associateWith { category ->
            val mapped = DocumentCategory.fromId(category.categoryId)
            if (mapped == null) 0
            else recentDocuments.count { doc ->
                DocumentCategory.matchesFilter(mapped, doc.categoryId, doc.fileName)
            }
        }
    }

    DigitalBharatBackground {
        androidx.compose.material3.Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                HomeTopBar(
                    onSearchClick = { navigation.toSearchScreen() },
                    onProfileClick = {
                        navigation.toAboutScreen()
                    }
                )
            },
            bottomBar = {
                HomeBottomBar(
                    selectedTab = HomeTab.HOME,
                    onHomeClick = {},
                    onDocumentsClick = { navigation.toDocumentsScreen() },
                    onScannerClick = { navigation.toCameraScreen() },
                    onSettingsClick = {
                        navigation.toSettingsScreen?.invoke() ?: navigation.toAboutScreen()
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))

                if (!cameraPermission.isGranted) {
                    CameraPermissionRationale(cameraPermission)
                } else {
                    HeroScanCard(
                        onScanNow = {
                            onClearScan()
                            navigation.toCameraScreen()
                        }
                    )
                }

                if (!currentDocument.isEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    OngoingScanBanner(
                        currentDocument = currentDocument,
                        onResumeScan = navigation.toDocumentScreen,
                        onClearScan = onClearScan
                    )
                }

                Spacer(Modifier.height(22.dp))

                if (updateState.updateInfo != null) {
                    UpdateHomeCard(
                        updateState = updateState,
                        onCheckForUpdates = onCheckForUpdates,
                        onInstallUpdate = onInstallUpdate
                    )

                    Spacer(Modifier.height(20.dp))
                }

                QuickFoldersSection(
                    categories = categories,
                    categoryCounts = categoryCounts,
                    activeCategoryId = null,
                    onSelectCategory = { categoryId ->
                        navigation.toFiltersScreen(categoryId)
                    },
                    onViewAll = {
                        navigation.toFiltersScreen(null)
                    }
                )

                Spacer(Modifier.height(20.dp))

                RecentScansSection(
                    title = stringResource(R.string.recent_scans),
                    documents = recentDocuments.take(3),
                    onOpenPdf = onOpenPdf,
                    onSharePdf = { sharePdf(context, it) },
                    onDeletePdf = { onDeleteDocument(it) },
                    emptyMessage = if (recentDocuments.isEmpty())
                        stringResource(R.string.no_recent_scans)
                    else
                        stringResource(R.string.no_results),
                )

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    bitmap = ImageBitmap.imageResource(id = R.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(10.dp))
                BrandTitle(height = 24.dp)
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search_documents),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onProfileClick) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(R.string.about),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.primary,
            actionIconContentColor = MaterialTheme.colorScheme.primary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navigation: Navigation,
    recentDocuments: List<RecentDocumentUiState>,
    onOpenPdf: (Uri) -> Unit,
    onDeleteDocument: (RecentDocumentUiState) -> Unit,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val filteredDocuments = remember(recentDocuments, query, context) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) recentDocuments
        else recentDocuments.filter { doc ->
            val nameMatch = doc.fileName.contains(trimmed, ignoreCase = true)
            val categoryLabel = categoryLabelFor(doc.categoryId, context).orEmpty()
            val categoryMatch = categoryLabel.contains(trimmed, ignoreCase = true)
            nameMatch || categoryMatch
        }
    }

    DigitalBharatBackground {
        androidx.compose.material3.Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.search_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = { BackButton(navigation.back) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                        actionIconContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))
                SearchField(
                    query = query,
                    onQueryChange = { query = it },
                    onClear = { query = "" }
                )
                Spacer(Modifier.height(16.dp))
                RecentScansSection(
                    title = stringResource(R.string.recent_scans),
                    documents = filteredDocuments,
                    onOpenPdf = onOpenPdf,
                    onSharePdf = { sharePdf(context, it) },
                    onDeletePdf = { onDeleteDocument(it) },
                    emptyMessage = if (recentDocuments.isEmpty())
                        stringResource(R.string.no_recent_scans)
                    else
                        stringResource(R.string.no_results)
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    navigation: Navigation,
    recentDocuments: List<RecentDocumentUiState>,
    onOpenPdf: (Uri) -> Unit,
    onDeleteDocument: (RecentDocumentUiState) -> Unit,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(DocumentsFilter.ALL) }

    val filteredDocuments = remember(recentDocuments, query, selectedFilter) {
        val trimmed = query.trim()
        val base = if (trimmed.isBlank()) recentDocuments else recentDocuments.filter { doc ->
            doc.fileName.contains(trimmed, ignoreCase = true)
        }
        when (selectedFilter) {
            DocumentsFilter.ALL -> base
            DocumentsFilter.RECENT -> base.sortedByDescending { it.saveTimestamp }
            DocumentsFilter.VERIFIED -> base.filter { doc ->
                DocumentCategory.matchesFilter(DocumentCategory.ID_PROOFS, doc.categoryId, doc.fileName)
            }
        }
    }

    DigitalBharatBackground {
        androidx.compose.material3.Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.documents),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = { BackButton(navigation.back) },
                    actions = { AppOverflowMenu(navigation) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                        actionIconContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            bottomBar = {
                HomeBottomBar(
                    selectedTab = HomeTab.DOCUMENTS,
                    onHomeClick = { navigation.toHomeScreen() },
                    onDocumentsClick = {},
                    onScannerClick = { navigation.toCameraScreen() },
                    onSettingsClick = {
                        navigation.toSettingsScreen?.invoke() ?: navigation.toAboutScreen()
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navigation.toCameraScreen() },
                    containerColor = BharatNavy
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = stringResource(R.string.scan_now),
                        tint = BharatWhite
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))
                DocumentsSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    onClear = { query = "" }
                )
                Spacer(Modifier.height(16.dp))
                DocumentsFilterChips(
                    selectedFilter = selectedFilter,
                    onSelectFilter = { selectedFilter = it }
                )
                Spacer(Modifier.height(16.dp))
                if (filteredDocuments.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ) {
                        Text(
                            text = stringResource(R.string.no_results),
                            modifier = Modifier.padding(16.dp),
                            color = TextSecondary
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        filteredDocuments.forEach { document ->
                            DocumentsListCard(
                                document = document,
                                onOpenPdf = onOpenPdf,
                                onSharePdf = { sharePdf(context, it) },
                                onDeletePdf = { onDeleteDocument(it) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersScreen(
    navigation: Navigation,
    recentDocuments: List<RecentDocumentUiState>,
    customCategories: List<String>,
    initialCategoryId: String?,
    onOpenPdf: (Uri) -> Unit,
    onDeleteDocument: (RecentDocumentUiState) -> Unit,
) {
    val context = LocalContext.current
    var selectedFilterId by remember { mutableStateOf(initialCategoryId ?: "all") }

    val customCategoryPool = remember(recentDocuments, customCategories) {
        (customCategories + recentDocuments.mapNotNull { DocumentCategory.decodeCustom(it.categoryId) })
            .distinct()
            .sorted()
    }

    val filterOptions = remember(customCategoryPool, context) {
        val base = mutableListOf(
            FilterOption("all", context.getString(R.string.all)),
            FilterOption(DocumentCategory.ID_PROOFS.id, context.getString(R.string.id_proofs)),
            FilterOption(DocumentCategory.EDUCATION.id, context.getString(R.string.education)),
        )
        customCategoryPool.forEach { name ->
            base.add(FilterOption(DocumentCategory.encodeCustom(name), name))
        }
        base
    }

    val filteredDocuments = remember(selectedFilterId, recentDocuments) {
        when (selectedFilterId) {
            "all" -> recentDocuments
            DocumentCategory.ID_PROOFS.id -> recentDocuments.filter { doc ->
                DocumentCategory.matchesFilter(DocumentCategory.ID_PROOFS, doc.categoryId, doc.fileName)
            }
            DocumentCategory.EDUCATION.id -> recentDocuments.filter { doc ->
                DocumentCategory.matchesFilter(DocumentCategory.EDUCATION, doc.categoryId, doc.fileName)
            }
            else -> recentDocuments.filter { doc ->
                doc.categoryId?.equals(selectedFilterId, ignoreCase = true) == true
            }
        }
    }

    DigitalBharatBackground {
        androidx.compose.material3.Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.filters_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = { BackButton(navigation.back) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                        actionIconContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filterOptions.size) { index ->
                        val option = filterOptions[index]
                        val selected = option.id == selectedFilterId
                        val style: CategoryStyle = if (option.id == "all") {
                            CategoryStyle(Icons.Default.Description, MaterialTheme.colorScheme.primary)
                        } else {
                            categoryStyleFor(option.id, option.label)
                        }
                        FilterChip(
                            selected = selected,
                            onClick = { selectedFilterId = option.id },
                            label = {
                                Text(
                                    text = option.label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = style.icon,
                                    contentDescription = null,
                                    tint = if (selected) style.color else style.color.copy(alpha = 0.6f)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = style.color.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                RecentScansSection(
                    title = stringResource(R.string.documents),
                    documents = filteredDocuments,
                    onOpenPdf = onOpenPdf,
                    onSharePdf = { sharePdf(context, it) },
                    onDeletePdf = { onDeleteDocument(it) },
                    emptyMessage = if (recentDocuments.isEmpty())
                        stringResource(R.string.no_recent_scans)
                    else
                        stringResource(R.string.no_results)
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.search_documents)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.clear_text)
                    )
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        )
    )
}

private enum class DocumentsFilter {
    ALL,
    RECENT,
    VERIFIED,
}

@Composable
private fun DocumentsSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.search_within_documents)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.clear_text)
                    )
                }
            }
        },
        shape = RoundedCornerShape(20.dp),
        singleLine = true,
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        )
    )
}

@Composable
private fun DocumentsFilterChips(
    selectedFilter: DocumentsFilter,
    onSelectFilter: (DocumentsFilter) -> Unit,
) {
    val options = listOf(
        DocumentsFilter.ALL to stringResource(R.string.all),
        DocumentsFilter.RECENT to stringResource(R.string.recently_added),
        DocumentsFilter.VERIFIED to stringResource(R.string.verified)
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(options.size) { index ->
            val option = options[index]
            val selected = option.first == selectedFilter
            FilterChip(
                selected = selected,
                onClick = { onSelectFilter(option.first) },
                label = {
                    Text(
                        text = option.second,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BharatNavy.copy(alpha = 0.15f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun DocumentsListCard(
    document: RecentDocumentUiState,
    onOpenPdf: (Uri) -> Unit,
    onSharePdf: (RecentDocumentUiState) -> Unit,
    onDeletePdf: (RecentDocumentUiState) -> Unit,
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    val showDeleteDialog = rememberSaveable { mutableStateOf(false) }
    val sizeLabel = rememberFileSizeLabel(document)
    val tag = remember(document.categoryId, context) {
        tagForCategoryLabel(categoryLabelFor(document.categoryId, context))
    }

    ElevatedCard(
        onClick = { onOpenPdf(document.fileUri) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(avatarColorFor(document.fileName)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = BharatWhite,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatDate(document.saveTimestamp, context)} - $sizeLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(10.dp))
                TagChip(tag)
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.menu)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.open)) },
                        onClick = {
                            menuExpanded = false
                            onOpenPdf(document.fileUri)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share_document)) },
                        onClick = {
                            menuExpanded = false
                            onSharePdf(document)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_document)) },
                        onClick = {
                            menuExpanded = false
                            showDeleteDialog.value = true
                        }
                    )
                }
            }
        }
    }
    if (showDeleteDialog.value) {
        ConfirmationDialog(
            title = stringResource(R.string.delete_document_title),
            message = stringResource(R.string.delete_document_warning),
            showDialog = showDeleteDialog
        ) { onDeletePdf(document) }
    }
}

private fun tagForCategoryLabel(label: String?): DocTag {
    val safeLabel = label?.ifBlank { null } ?: "DOCUMENT"
    val style = categoryStyleFor(null, safeLabel)
    return DocTag(
        label = safeLabel,
        container = style.color.copy(alpha = 0.18f),
        content = style.color
    )
}

@Composable
private fun HeroScanCard(onScanNow: () -> Unit) {
    val gradient = Brush.linearGradient(
        colors = listOf(BharatSaffron, BharatSaffronDeep)
    )
    val rotation by rememberInfiniteTransition(label = "heroChakra").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing)
        ),
        label = "heroChakraRotation"
    )
    Surface(
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 12.dp,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .padding(24.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ashoka_chakra_loader),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(120.dp)
                    .graphicsLayer { rotationZ = rotation }
                    .alpha(0.12f),
                tint = Color.White
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = greetingMessage(),
                    color = BharatWhite,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                ScanNowButton(onClick = onScanNow)
            }
        }
    }
}

@Composable
private fun greetingMessage(): String {
    val hour = LocalTime.now().hour
    val resId = when (hour) {
        in 5..11 -> R.string.good_morning
        in 12..16 -> R.string.good_afternoon
        in 17..20 -> R.string.good_evening
        else -> R.string.good_night
    }
    val variants = stringArrayResource(R.array.greeting_variants).toList()
    val seed = LocalDate.now().dayOfYear + hour
    val tail = variants[seed % variants.size]
    return "${stringResource(resId)}, $tail"
}

@Composable
private fun ScanNowButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = BharatWhite,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CropFree,
                contentDescription = null,
                tint = BharatNavy,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = stringResource(R.string.scan_now),
                color = BharatNavy,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun OngoingScanBanner(
    currentDocument: DocumentUiModel,
    onResumeScan: () -> Unit,
    onClearScan: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.scan_in_progress),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = pageCountText(currentDocument.pageCount()),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            TextButton(onClick = onResumeScan) {
                Text(
                    text = stringResource(R.string.resume),
                    fontWeight = FontWeight.SemiBold
                )
            }
            TextButton(onClick = onClearScan) {
                Text(
                    text = stringResource(R.string.discard_scan),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun UpdateHomeCard(
    updateState: UpdateUiState,
    onCheckForUpdates: () -> Unit,
    onInstallUpdate: (UpdateInfo) -> Unit,
) {
    val info = updateState.updateInfo
    val isDownloading = updateState.downloadStatus?.status in listOf(
        android.app.DownloadManager.STATUS_RUNNING,
        android.app.DownloadManager.STATUS_PENDING,
        android.app.DownloadManager.STATUS_PAUSED
    )
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.update_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            when {
                info != null -> {
                    Text(
                        text = stringResource(R.string.update_available_message, info.versionName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                updateState.isChecking -> {
                    Text(
                        text = stringResource(R.string.checking_updates),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                !updateState.statusMessage.isNullOrBlank() -> {
                    Text(
                        text = updateState.statusMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                else -> {
                    Text(
                        text = stringResource(R.string.update_check_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            updateState.downloadStatus?.let { status ->
                when (status.status) {
                    android.app.DownloadManager.STATUS_RUNNING,
                    android.app.DownloadManager.STATUS_PENDING,
                    android.app.DownloadManager.STATUS_PAUSED -> {
                        val progress = status.progress
                        if (progress != null) {
                            LinearProgressIndicator(progress = progress)
                            Text(
                                text = "${(progress * 100).toInt()}% downloaded",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        } else {
                            LinearProgressIndicator()
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

            if (info != null) {
                Button(
                    onClick = { onInstallUpdate(info) },
                    enabled = !isDownloading,
                    colors = ButtonDefaults.buttonColors(containerColor = BharatSaffron)
                ) {
                    Text(
                        text = stringResource(R.string.update_install),
                        color = BharatWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
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
            }
        }
    }
}

@Composable
private fun QuickFoldersSection(
    categories: List<QuickFolderCategory>,
    categoryCounts: Map<QuickFolderCategory, Int>,
    activeCategoryId: String?,
    onSelectCategory: (String?) -> Unit,
    onViewAll: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stringResource(R.string.quick_folders),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(onClick = onViewAll) {
                Text(
                    text = stringResource(R.string.view_all),
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            categories.forEach { category ->
                QuickFolderCard(
                    category = category,
                    count = categoryCounts[category] ?: 0,
                    selected = category.id == activeCategoryId,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onSelectCategory(if (activeCategoryId == category.id) null else category.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun QuickFolderCard(
    category: QuickFolderCategory,
    count: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (selected) 0.98f else 0.95f),
        shadowElevation = if (selected) 10.dp else 6.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(category.accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = category.accent
                )
            }
            Text(
                text = stringResource(category.titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = documentsCountText(count),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ActiveFilterChip(label: String, onClear: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = NavyGlow,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClear() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.clear_filter),
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CameraPermissionRationale(state: CameraPermissionState) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 10.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.camera_permission_rationale),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            MainActionButton(
                onClick = { state.request() },
                text = stringResource(R.string.grant_permission),
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun RecentScansSection(
    title: String,
    documents: List<RecentDocumentUiState>,
    onOpenPdf: (Uri) -> Unit,
    onSharePdf: (RecentDocumentUiState) -> Unit,
    onDeletePdf: (RecentDocumentUiState) -> Unit,
    showHeader: Boolean = true,
    emptyMessage: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (showHeader) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
        }

        if (documents.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ) {
                Text(
                    text = emptyMessage,
                    modifier = Modifier.padding(16.dp),
                    color = TextSecondary
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                documents.forEach { document ->
                    RecentScanCard(
                        document = document,
                        onOpenPdf = onOpenPdf,
                        onSharePdf = onSharePdf,
                        onDeletePdf = onDeletePdf
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentScanCard(
    document: RecentDocumentUiState,
    onOpenPdf: (Uri) -> Unit,
    onSharePdf: (RecentDocumentUiState) -> Unit,
    onDeletePdf: (RecentDocumentUiState) -> Unit,
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    val showDeleteDialog = rememberSaveable { mutableStateOf(false) }
    val categoryLabel = remember(document.categoryId, context) {
        categoryLabelFor(document.categoryId, context)
    }
    val tag = remember(document.categoryId, context) {
        tagForCategoryLabel(categoryLabel)
    }
    val sizeLabel = rememberFileSizeLabel(document)

    ElevatedCard(
        onClick = { onOpenPdf(document.fileUri) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(avatarColorFor(document.fileName)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = BharatWhite
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatDate(document.saveTimestamp, context)} - $sizeLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(6.dp))
                TagChip(tag)
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.menu)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.open)) },
                        onClick = {
                            menuExpanded = false
                            onOpenPdf(document.fileUri)
                        }
                    )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.share_document)) },
                    onClick = {
                        menuExpanded = false
                        onSharePdf(document)
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete_document)) },
                    onClick = {
                        menuExpanded = false
                        showDeleteDialog.value = true
                    }
                )
            }
        }
    }
    if (showDeleteDialog.value) {
        ConfirmationDialog(
            title = stringResource(R.string.delete_document_title),
            message = stringResource(R.string.delete_document_warning),
            showDialog = showDeleteDialog
        ) { onDeletePdf(document) }
    }
}
}

@Composable
private fun TagChip(tag: DocTag) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = tag.container
    ) {
        Text(
            text = tag.label,
            color = tag.content,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun HomeBottomBar(
    selectedTab: HomeTab,
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
            selected = selectedTab == HomeTab.HOME,
            onClick = onHomeClick,
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.home_title)) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BharatWhite,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = BharatSaffron
            )
        )
        NavigationBarItem(
            selected = selectedTab == HomeTab.DOCUMENTS,
            onClick = onDocumentsClick,
            icon = { Icon(Icons.Default.Description, contentDescription = null) },
            label = { Text(stringResource(R.string.documents)) }
        )
        NavigationBarItem(
            selected = selectedTab == HomeTab.SCANNER,
            onClick = onScannerClick,
            icon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
            label = { Text(stringResource(R.string.scanner)) }
        )
        NavigationBarItem(
            selected = selectedTab == HomeTab.SETTINGS,
            onClick = onSettingsClick,
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.settings)) }
        )
    }
}

@Composable
private fun documentsCountText(count: Int): String {
    return androidx.compose.ui.res.pluralStringResource(
        id = R.plurals.document_count,
        count = count,
        count
    )
}

private fun avatarColorFor(fileName: String): Color {
    val palette = listOf(NavyGlow, SaffronGlow, TertiaryContainer, BharatChakra)
    val index = abs(fileName.hashCode()) % palette.size
    return palette[index]
}

private fun categoryLabelFor(categoryId: String?, context: Context): String? {
    val custom = DocumentCategory.decodeCustom(categoryId)
    if (!custom.isNullOrBlank()) return custom
    return DocumentCategory.fromId(categoryId)?.let { context.getString(it.labelRes) }
}

@Composable
private fun rememberFileSizeLabel(document: RecentDocumentUiState): String {
    val context = LocalContext.current
    val sizeLabel by produceState<String?>(initialValue = null, document.fileUri) {
        value = withContext(Dispatchers.IO) { readFileSize(context, document.fileUri) }
    }
    return sizeLabel ?: stringResource(R.string.unknown_size)
}

private fun readFileSize(context: Context, uri: Uri): String? {
    val document = DocumentFile.fromSingleUri(context, uri) ?: return null
    val length = document.length()
    if (length <= 0) return null
    return formatFileSize(length, context)
}

private fun sharePdf(context: Context, document: RecentDocumentUiState) {
    val shareUri = if (document.fileUri.scheme == "content") {
        document.fileUri
    } else {
        val filePath = document.fileUri.path ?: return
        uriForFile(context, File(filePath))
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, shareUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_document)))
}


