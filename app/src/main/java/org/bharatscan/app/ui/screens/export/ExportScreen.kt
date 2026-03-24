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
package org.bharatscan.app.ui.screens.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.MediaPlayer
import android.text.format.Formatter
import androidx.core.net.toUri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.with
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import org.bharatscan.app.R
import org.bharatscan.app.domain.ExportQuality
import org.bharatscan.app.ui.Navigation
import org.bharatscan.app.ui.components.*
import org.bharatscan.app.ui.model.DocumentCategory
import org.bharatscan.app.ui.screens.settings.ExportFormat
import org.bharatscan.app.ui.state.DocumentUiModel
import org.bharatscan.app.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun ExportScreenWrapper(
    navigation: Navigation,
    uiState: ExportUiState,
    currentDocument: DocumentUiModel,
    customCategories: List<String>,
    pdfActions: ExportActions,
    onCloseScan: () -> Unit,
) {
    BackHandler { navigation.back() }

    val showConfirmationDialog = rememberSaveable { mutableStateOf(false) }
    val showCategoryDialog = rememberSaveable { mutableStateOf(false) }
    var selectedCategory by rememberSaveable { mutableStateOf(DocumentCategory.ID_PROOFS) }
    var customCategoryName by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        pdfActions.prepareExportIfNeeded()
    }

    val onFilenameChange = { newName:String ->
        pdfActions.setFilename(newName)
    }

    val selectedCategoryLabel = if (selectedCategory == DocumentCategory.OTHER) {
        customCategoryName.trim().ifEmpty { stringResource(R.string.other) }
    } else {
        stringResource(selectedCategory.labelRes)
    }

    ExportScreen(
        onFilenameChange = onFilenameChange,
        onWatermarkEnabledChange = pdfActions.setWatermarkEnabled,
        onWatermarkTextChange = pdfActions.setWatermarkText,
        onPasswordProtectionEnabledChange = pdfActions.setPasswordProtectionEnabled,
        onPasswordChange = pdfActions.setPassword,
        onFormatChange = pdfActions.setExportFormat,
        onQualityChange = pdfActions.setExportQuality,
        onCategoryClick = { showCategoryDialog.value = true },
        selectedCategoryLabel = selectedCategoryLabel,
        uiState = uiState,
        currentDocument = currentDocument,
        navigation = navigation,
        onShare = {
            if (!uiState.isSaving) {
                pdfActions.share()
            }
        },
        onSave = {
            if (!uiState.isSaving) {
                val categoryValue = if (selectedCategory == DocumentCategory.OTHER) {
                    val resolved = DocumentCategory.resolveFromCustomName(customCategoryName)
                    if (resolved != null) {
                        resolved.id
                    } else {
                        pdfActions.registerCustomCategory(customCategoryName)
                        DocumentCategory.encodeCustom(customCategoryName)
                    }
                } else {
                    selectedCategory.id
                }
                pdfActions.save(categoryValue)
            }
        },
        onOpen = pdfActions.open,
        onCloseScan = {
            if (!uiState.isSaving) {
                if (uiState.hasSavedOrShared)
                    onCloseScan()
                else
                    showConfirmationDialog.value = true
            }
        },
    )

    if (showConfirmationDialog.value) {
        NewDocumentDialog(onCloseScan, showConfirmationDialog, stringResource(R.string.end_scan))
    }

    if (showCategoryDialog.value) {
        CategoryPickerDialog(
            selectedCategory = selectedCategory,
            customCategoryName = customCategoryName,
            customCategories = customCategories,
            onSelectCategory = { selectedCategory = it },
            onCustomCategoryChange = { customCategoryName = it },
            onDismiss = { showCategoryDialog.value = false },
            onConfirm = {
                showCategoryDialog.value = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ExportScreen(
    onFilenameChange: (String) -> Unit,
    onWatermarkEnabledChange: (Boolean) -> Unit,
    onWatermarkTextChange: (String) -> Unit,
    onPasswordProtectionEnabledChange: (Boolean) -> Unit,
    onPasswordChange: (String) -> Unit,
    onFormatChange: (ExportFormat) -> Unit,
    onQualityChange: (org.bharatscan.app.domain.ExportQuality) -> Unit,
    onCategoryClick: () -> Unit,
    selectedCategoryLabel: String,
    uiState: ExportUiState,
    currentDocument: DocumentUiModel,
    navigation: Navigation,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onOpen: (SavedItem) -> Unit,
    onCloseScan: () -> Unit,
) {
    DigitalBharatBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        BrandTitle(height = 26.dp)
                    },
                    navigationIcon = { BackButton(navigation.back) },
                    actions = {
                        AppOverflowMenu(navigation)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                        actionIconContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        ) { innerPadding ->
            val containerModifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
            val onThumbnailClick = navigation.toDocumentScreen
            val showSuccess = uiState.hasSavedOrShared
            AnimatedContent(
                targetState = showSuccess,
                transitionSpec = {
                    if (targetState) {
                        (fadeIn(animationSpec = tween(450)) + scaleIn(
                            initialScale = 0.96f,
                            animationSpec = tween(450, easing = FastOutSlowInEasing)
                        )) with (
                            fadeOut(animationSpec = tween(200)) + scaleOut(
                                targetScale = 1.02f,
                                animationSpec = tween(200)
                            )
                        )
                    } else {
                        (fadeIn(animationSpec = tween(250)) + scaleIn(
                            initialScale = 0.98f,
                            animationSpec = tween(250, easing = FastOutSlowInEasing)
                        )) with fadeOut(animationSpec = tween(150))
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { isSuccess ->
                if (isSuccess) {
                    ExportSuccessScreen(
                        uiState = uiState,
                        onOpen = onOpen,
                        onGoHome = onCloseScan,
                        modifier = containerModifier.fillMaxSize()
                    )
                } else if (!isLandscape(LocalConfiguration.current)) {
                    Column(
                        modifier = containerModifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        ExportHeader()
                        PdfInfosAndResultBar(uiState, currentDocument, onOpen, onThumbnailClick)
                    MainActions(
                        onFilenameChange = onFilenameChange,
                        onWatermarkEnabledChange = onWatermarkEnabledChange,
                        onWatermarkTextChange = onWatermarkTextChange,
                        onPasswordProtectionEnabledChange = onPasswordProtectionEnabledChange,
                        onPasswordChange = onPasswordChange,
                        onFormatChange = onFormatChange,
                        onQualityChange = onQualityChange,
                        onCategoryClick = onCategoryClick,
                            selectedCategoryLabel = selectedCategoryLabel,
                            uiState = uiState,
                            onShare = onShare,
                            onSave = onSave
                        )
                    }
                } else {
                    Row(
                        modifier = containerModifier.fillMaxHeight(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            ExportHeader()
                            PdfInfosAndResultBar(uiState, currentDocument, onOpen, onThumbnailClick)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            MainActions(
                                onFilenameChange = onFilenameChange,
                                onWatermarkEnabledChange = onWatermarkEnabledChange,
                                onWatermarkTextChange = onWatermarkTextChange,
                                onPasswordProtectionEnabledChange = onPasswordProtectionEnabledChange,
                                onPasswordChange = onPasswordChange,
                                onFormatChange = onFormatChange,
                                onQualityChange = onQualityChange,
                                onCategoryClick = onCategoryClick,
                                selectedCategoryLabel = selectedCategoryLabel,
                                uiState = uiState,
                                onShare = onShare,
                                onSave = onSave
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportSuccessScreen(
    uiState: ExportUiState,
    onOpen: (SavedItem) -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer.create(context, R.raw.success) }
    val result = uiState.result
    val savedItem = uiState.savedBundle?.items?.firstOrNull()
        ?: result?.files?.firstOrNull()?.let { file ->
            SavedItem(file.toUri(), file.name, uiState.format)
        }
    val fileName = savedItem?.fileName
        ?: result?.files?.firstOrNull()?.name
        ?: uiState.filename.ifBlank { stringResource(R.string.document) }
    val folderLabel = uiState.savedBundle?.saveDir?.name
        ?: stringResource(R.string.recent_scans)
    val sizeLabel = formatFileSize(result?.sizeInBytes, context)
    val formatLabel = if (uiState.format == ExportFormat.PDF) {
        stringResource(R.string.export_success_format_pdf)
    } else {
        stringResource(R.string.export_success_format_jpeg)
    }
    val title = if (uiState.savedBundle != null) {
        stringResource(R.string.export_success_title_saved)
    } else {
        stringResource(R.string.export_success_title_shared)
    }
    val description = if (uiState.savedBundle != null) {
        buildAnnotatedString {
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            ) {
                append(fileName)
            }
            append(" ")
            append(stringResource(R.string.export_success_available_in))
            append(" ")
            withStyle(
                SpanStyle(
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.primary
                )
            ) {
                append(folderLabel)
            }
            append(".")
        }
    } else {
        buildAnnotatedString {
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            ) {
                append(fileName)
            }
            append(" ")
            append(stringResource(R.string.export_success_shared_message))
        }
    }

    DisposableEffect(Unit) {
        onDispose { mediaPlayer.release() }
    }

    LaunchedEffect(uiState.hasSavedOrShared) {
        if (uiState.hasSavedOrShared) {
            mediaPlayer.setVolume(0.35f, 0.35f)
            mediaPlayer.seekTo(0)
            mediaPlayer.start()
        }
    }

    Box(
        modifier = modifier
            .padding(vertical = 12.dp)
    ) {
        SuccessBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            SuccessBadge()
            Spacer(Modifier.height(24.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            SuccessPrimaryButton(
                enabled = savedItem != null,
                onClick = { savedItem?.let(onOpen) },
                text = stringResource(R.string.export_success_view_document)
            )
            Spacer(Modifier.height(16.dp))
            SuccessSecondaryButton(
                onClick = onGoHome,
                text = stringResource(R.string.export_success_go_home)
            )
            Spacer(Modifier.height(24.dp))
            SuccessStatsRow(
                sizeLabel = sizeLabel,
                formatLabel = formatLabel
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.made_with_love_india),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SuccessBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        val tricolorSoft = Brush.linearGradient(
            listOf(
                BharatSaffron.copy(alpha = 0.12f),
                BharatWhite.copy(alpha = 0.06f),
                BharatGreen.copy(alpha = 0.12f)
            )
        )
        val tricolorReverse = Brush.linearGradient(
            listOf(
                BharatGreen.copy(alpha = 0.08f),
                BharatWhite.copy(alpha = 0.04f),
                BharatSaffron.copy(alpha = 0.08f)
            )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.Center)
                .graphicsLayer(rotationZ = -6f)
                .background(tricolorSoft, RoundedCornerShape(140.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .height(140.dp)
                .align(Alignment.Center)
                .offset(y = 120.dp)
                .graphicsLayer(rotationZ = 6f)
                .background(tricolorReverse, RoundedCornerShape(140.dp))
        )
    }
}

@Composable
private fun SuccessBadge() {
    val floatOffset by rememberInfiniteTransition(label = "successFloat").animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "successFloat"
    )
    var showCheck by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showCheck = true }
    val checkScale by animateFloatAsState(
        targetValue = if (showCheck) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 350f),
        label = "checkScale"
    )
    Box(
        modifier = Modifier
            .size(140.dp)
            .offset(y = floatOffset.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(118.dp)
                .shadow(24.dp, shape = RoundedCornerShape(90.dp), clip = false)
                .background(BharatGreen, shape = RoundedCornerShape(90.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer(scaleX = checkScale, scaleY = checkScale)
                    .background(Color.White, shape = RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = null,
                    tint = BharatGreenDeep,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun SuccessPrimaryButton(
    enabled: Boolean,
    onClick: () -> Unit,
    text: String,
) {
    val arrowOffset = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(enabled) {
        if (!enabled) {
            arrowOffset.snapTo(0f)
            return@LaunchedEffect
        }
        repeat(3) {
            arrowOffset.animateTo(10f, animationSpec = tween(140, easing = FastOutSlowInEasing))
            arrowOffset.animateTo(0f, animationSpec = tween(140, easing = FastOutSlowInEasing))
            delay(120)
        }
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BharatNavy,
            contentColor = Color.White,
            disabledContainerColor = BharatNavy.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        )
    ) {
        Icon(
            imageVector = Icons.Default.Visibility,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(text, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            modifier = Modifier
                .size(18.dp)
                .offset(x = arrowOffset.value.dp)
        )
    }
}

@Composable
private fun SuccessSecondaryButton(
    onClick: () -> Unit,
    text: String,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, BharatSaffronDeep.copy(alpha = 0.25f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = BharatSaffron.copy(alpha = 0.12f),
            contentColor = BharatSaffronDeep
        )
    ) {
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SuccessStatsRow(
    sizeLabel: String,
    formatLabel: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SuccessStatItem(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.export_success_size),
            value = sizeLabel
        )
        SuccessStatDivider()
        SuccessStatItem(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.export_success_format),
            value = formatLabel
        )
    }
}

@Composable
private fun SuccessStatItem(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SuccessStatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(DividerColor.copy(alpha = 0.6f))
    )
}

@Composable
private fun PdfInfosAndResultBar(
    uiState: ExportUiState,
    currentDocument: DocumentUiModel,
    onOpen: (SavedItem) -> Unit,
    onThumbnailClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        PdfInfoCard {
            PdfInfos(uiState, currentDocument, onThumbnailClick)
            SaveStatusBar(uiState, onOpen)
        }

        uiState.error?.let {
            ErrorBar(it)
        }
    }
}

@Composable
private fun ExportHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.export_portal).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = stringResource(R.string.finalize_document),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PdfInfoCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(20.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun PdfInfos(
    uiState: ExportUiState,
    currentDocument: DocumentUiModel,
    onThumbnailClick: () -> Unit,
) {
    val result = uiState.result

    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val thumbnail = currentDocument.loadThumbnail(0)
        thumbnail?.let {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .size(100.dp)
                    .clickable { onThumbnailClick() }
            ) {
                Image(
                    bitmap = thumbnail.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (uiState.isGenerating) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.creating_export),
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (result != null) {
                val context = LocalContext.current
                val formattedFileSize = formatFileSize(result.sizeInBytes, context)
                Text(
                    text = pageCountText(result.pageCount),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formattedFileSize,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SaveStatusBar(
    uiState: ExportUiState,
    onOpen: (SavedItem) -> Unit,
) {
    when {
        uiState.isSaving -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Saving...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        uiState.savedBundle != null -> {
            SaveInfoBar(uiState.savedBundle, onOpen)
        }
    }
}

@Composable
private fun FilenameTextField(
    filename: String,
    onFilenameChange: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    OutlinedTextField(
        value = filename,
        onValueChange = onFilenameChange,
        label = { Text(stringResource(R.string.filename)) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.secondary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedLabelColor = MaterialTheme.colorScheme.secondary,
            cursorColor = MaterialTheme.colorScheme.secondary
        ),
        trailingIcon = {
            if (filename.isNotEmpty()) {
                IconButton(onClick = {
                    onFilenameChange("")
                    focusRequester.requestFocus()
                }) {
                    Icon(Icons.Default.Clear, stringResource(R.string.clear_text))
                }
            }
        },
    )
}

@Composable
private fun MainActions(
    onFilenameChange: (String) -> Unit,
    onWatermarkEnabledChange: (Boolean) -> Unit,
    onWatermarkTextChange: (String) -> Unit,
    onPasswordProtectionEnabledChange: (Boolean) -> Unit,
    onPasswordChange: (String) -> Unit,
    onFormatChange: (ExportFormat) -> Unit,
    onQualityChange: (org.bharatscan.app.domain.ExportQuality) -> Unit,
    onCategoryClick: () -> Unit,
    selectedCategoryLabel: String,
    uiState: ExportUiState,
    onShare: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ActionSurface {
            FilenameTextField(uiState.filename, onFilenameChange)

            Text(
                text = stringResource(R.string.select_file_format),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormatChip(
                    label = "PDF",
                    selected = uiState.format == ExportFormat.PDF,
                    onClick = { onFormatChange(ExportFormat.PDF) }
                )
                FormatChip(
                    label = "JPEG",
                    selected = uiState.format == ExportFormat.JPEG,
                    onClick = { onFormatChange(ExportFormat.JPEG) }
                )
            }

            Text(
                text = stringResource(R.string.export_quality),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QualityChip(
                    label = stringResource(R.string.quality_high),
                    selected = uiState.quality == ExportQuality.HIGH,
                    onClick = { onQualityChange(ExportQuality.HIGH) }
                )
                QualityChip(
                    label = stringResource(R.string.quality_standard),
                    selected = uiState.quality == ExportQuality.BALANCED,
                    onClick = { onQualityChange(ExportQuality.BALANCED) }
                )
                QualityChip(
                    label = stringResource(R.string.quality_low),
                    selected = uiState.quality == ExportQuality.LOW,
                    onClick = { onQualityChange(ExportQuality.LOW) }
                )
            }

            Text(
                text = stringResource(R.string.export_settings),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant))
                    .clickable { onCategoryClick() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.save_category_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = selectedCategoryLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.include_watermark),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.include_watermark_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Switch(
                    checked = uiState.includeWatermark,
                    onCheckedChange = onWatermarkEnabledChange
                )
            }
            if (uiState.includeWatermark) {
                OutlinedTextField(
                    value = uiState.watermarkText,
                    onValueChange = onWatermarkTextChange,
                    label = { Text(stringResource(R.string.custom_watermark)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (uiState.format == ExportFormat.PDF) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.password_protection),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(R.string.password_protection_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Switch(
                        checked = uiState.passwordProtectionEnabled,
                        onCheckedChange = onPasswordProtectionEnabledChange
                    )
                }
                if (uiState.passwordProtectionEnabled) {
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = onPasswordChange,
                        label = { Text(stringResource(R.string.password_label)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    val strength = evaluatePasswordStrength(uiState.password)
                    PasswordStrengthIndicator(strength)
                    uiState.passwordErrorRes?.let { errorRes ->
                        Text(
                            text = stringResource(errorRes),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ExportButton(
                    onClick = onShare,
                    enabled = uiState.result != null,
                    isPrimary = !uiState.hasSavedOrShared,
                    icon = Icons.Default.Share,
                    text = stringResource(R.string.share),
                    modifier = Modifier.weight(1f).height(56.dp)
                )
                ExportButton(
                    onClick = onSave,
                    enabled = uiState.result != null,
                    isPrimary = !uiState.hasSavedOrShared,
                    icon = Icons.Default.Download,
                    text = stringResource(R.string.save),
                    modifier = Modifier.weight(1f).height(56.dp)
                        .alpha(if (uiState.isSaving) 0.6f else 1f)
                )
            }
        }
    }
}

@Composable
private fun FormatChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = BharatNavy,
            selectedLabelColor = BharatWhite
        )
    )
}

@Composable
private fun PasswordStrengthIndicator(strength: PasswordStrength) {
    val (progress, color, labelRes) = when (strength) {
        PasswordStrength.WEAK -> Triple(0.33f, MaterialTheme.colorScheme.error, R.string.password_strength_weak)
        PasswordStrength.MEDIUM -> Triple(0.66f, BharatSaffron, R.string.password_strength_medium)
        PasswordStrength.STRONG -> Triple(1f, BharatGreen, R.string.password_strength_strong)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        LinearProgressIndicator(
            progress = { progress },
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
        )
        Text(
            text = stringResource(R.string.password_strength, stringResource(labelRes)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun QualityChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = BharatNavy,
            selectedLabelColor = BharatWhite
        )
    )
}

@Composable
private fun CategoryPickerDialog(
    selectedCategory: DocumentCategory,
    customCategoryName: String,
    customCategories: List<String>,
    onSelectCategory: (DocumentCategory) -> Unit,
    onCustomCategoryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val categories = remember { DocumentCategory.values().toList() }
    val customOptions = remember(customCategories) {
        customCategories.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }
    val isCustomValid = selectedCategory != DocumentCategory.OTHER ||
        customCategoryName.trim().isNotEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.save_category_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.save_category_prompt),
                    color = TextSecondary
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.forEach { category ->
                        val label = stringResource(category.labelRes)
                        val isSelected = category == selectedCategory
                        CategoryOptionRow(
                            label = label,
                            visual = categoryVisual(category.id, label),
                            selected = isSelected,
                            onClick = {
                                onSelectCategory(category)
                                if (category != DocumentCategory.OTHER) {
                                    onCustomCategoryChange("")
                                }
                            }
                        )
                    }
                    customOptions.forEach { custom ->
                        val isSelected =
                            selectedCategory == DocumentCategory.OTHER &&
                                customCategoryName.trim().equals(custom, ignoreCase = true)
                        CategoryOptionRow(
                            label = custom,
                            visual = categoryVisual(DocumentCategory.encodeCustom(custom), custom),
                            selected = isSelected,
                            onClick = {
                                onSelectCategory(DocumentCategory.OTHER)
                                onCustomCategoryChange(custom)
                            }
                        )
                    }
                }
                if (selectedCategory == DocumentCategory.OTHER) {
                    OutlinedTextField(
                        value = customCategoryName,
                        onValueChange = onCustomCategoryChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.custom_category_hint)) },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = isCustomValid) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private data class CategoryVisual(
    val icon: ImageVector,
    val color: Color
)

private fun categoryVisual(categoryId: String?, label: String): CategoryVisual {
    val style = categoryStyleFor(categoryId, label)
    return CategoryVisual(style.icon, style.color)
}

@Composable
private fun CategoryOptionRow(
    label: String,
    visual: CategoryVisual,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(visual.color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = visual.icon,
                contentDescription = null,
                tint = visual.color
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun ActionSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        shadowElevation = 10.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(20.dp)
        ) {
            content()
        }
    }
}


@Composable
fun ExportButton(
    icon: ImageVector,
    text: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val containerColor by animateColorAsState(
        targetValue = if (isPrimary) MaterialTheme.colorScheme.secondary
        else Color.Transparent
    )
    val contentColor by animateColorAsState(
        targetValue = if (isPrimary) MaterialTheme.colorScheme.onSecondary
        else MaterialTheme.colorScheme.secondary
    )
    val borderColor by animateColorAsState(
        targetValue = if (isPrimary) Color.Transparent
        else MaterialTheme.colorScheme.secondary
    )

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.3f),
            disabledContentColor = contentColor.copy(alpha = 0.3f)
        ),
        border = BorderStroke(2.dp, borderColor),
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SaveInfoBar(
    savedBundle: SavedBundle,
    onOpen: (SavedItem) -> Unit,
) {
    val dirName = savedBundle.saveDir?.name ?: stringResource(R.string.download_dirname)
    val items = savedBundle.items
    val nbFiles = items.size
    val firstFileName = items[0].fileName

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = LocalResources.current.getQuantityString(
                        R.plurals.files_saved_to,
                        nbFiles,
                        nbFiles, firstFileName, dirName
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Medium
                )
            }

            if (nbFiles == 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    MainActionButton(
                        onClick = { onOpen(items[0]) },
                        text = stringResource(R.string.open),
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        modifier = Modifier.height(48.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorBar(error: ExportError) {
    val (summary, details) = error.toDisplayText()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Error.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .border(
                2.dp,
                Error.copy(alpha = 0.4f),
                RoundedCornerShape(16.dp)
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = summary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Error,
                modifier = Modifier.weight(1f)
            )

            if (details != null) {
                IconButton(
                    onClick = {
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val text = buildString {
                            append(summary)
                            append("\n\n")
                            append(details)
                        }
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText("Export error", text)
                        )
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.copy_logs),
                        modifier = Modifier.size(20.dp),
                        tint = Error
                    )
                }
            }
        }

        if (details != null) {
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                color = Error.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun ExportError.toDisplayText(): Pair<String, String?> {
    return when (this) {
        is ExportError.OnPrepareOrShare -> {
            val summary = message
            val details = throwable.message
            summary to details
        }

        is ExportError.OnSave -> {
            val summary = stringResource(messageRes)
            val contextLines = buildErrorContextLines(saveDir)
            val details = buildString {
                if (contextLines.isNotEmpty()) {
                    append(contextLines.joinToString("\n"))
                }
                throwable?.message?.let {
                    if (isNotEmpty()) append("\n\n")
                    append(it)
                }
            }.ifEmpty { null }

            summary to details
        }
    }
}

@Composable
private fun buildErrorContextLines(
    saveDir: SaveDir?,
): List<String> {
    val defaultDirName = stringResource(R.string.download_dirname)

    val folderLine = when {
        saveDir == null ->
            stringResource(R.string.error_context_folder, defaultDirName)

        saveDir.name != null ->
            stringResource(R.string.error_context_folder, saveDir.name)

        else -> null
    }

    val providerLine = saveDir?.uri?.authority
        ?.let(::providerLabel)
        ?.let { stringResource(R.string.error_context_provider, it) }

    return listOfNotNull(folderLine, providerLine)
}

fun providerLabel(authority: String): String =
    when {
        authority.contains("nextcloud", ignoreCase = true) ->
            "Nextcloud"
        authority == "com.android.externalstorage.documents" ->
            "Local storage"
        else ->
            authority
    }

fun formatFileSize(sizeInBytes: Long?, context: Context): String {
    return if (sizeInBytes == null) context.getString(R.string.unknown_size)
    else Formatter.formatShortFileSize(context, sizeInBytes)
}


