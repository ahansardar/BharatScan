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
package org.bharatscan.app.ui.screens.camera

import android.content.res.Configuration
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.bharatscan.app.MainViewModel
import org.bharatscan.app.R
import org.bharatscan.app.ui.components.BrandTitle
import org.bharatscan.app.ui.Screen
import org.bharatscan.app.ui.Navigation
import org.bharatscan.app.ui.components.*
import org.bharatscan.app.ui.theme.*

const val CAPTURED_IMAGE_DISPLAY_DURATION = 1500L
const val ANIMATION_DURATION = 200

@Composable
fun CameraScreen(
    viewModel: MainViewModel,
    cameraViewModel: CameraViewModel,
    navigation: Navigation,
    liveAnalysisState: LiveAnalysisState,
    onImageAnalyzed: (ImageProxy) -> Unit,
    onFinalizePressed: () -> Unit,
    cameraPermission: CameraPermissionState,
) {
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    val document by viewModel.documentUiModel.collectAsStateWithLifecycle()
    val thumbnailCoords = remember { mutableStateOf(Offset.Zero) }
    var isDebugMode by remember { mutableStateOf(false) }
    val isTorchEnabled by cameraViewModel.isTorchEnabled.collectAsStateWithLifecycle()
    var torchReapplied by remember { mutableStateOf(false) }
    val pageListState = rememberLazyListState()
    var selectedPageIndex by remember { mutableIntStateOf(-1) }

    BackHandler { navigation.back() }

    val captureController = remember { CameraCaptureController() }
    DisposableEffect(Unit) {
        onDispose {
            captureController.shutdown()
            torchReapplied = false
        }
    }
    LaunchedEffect(captureController.cameraControl, isTorchEnabled) {
        captureController.cameraControl?.enableTorch(isTorchEnabled)
    }

    val captureState by cameraViewModel.captureState.collectAsStateWithLifecycle()
    if (captureState is CaptureState.CapturePreview) {
        LaunchedEffect(captureState) {
            delay(CAPTURED_IMAGE_DISPLAY_DURATION)
            cameraViewModel.addProcessedImage()
        }
    }

    var showDetectionError by remember { mutableStateOf(false) }
    LaunchedEffect(captureState) {
        if (captureState is CaptureState.CaptureError) {
            showDetectionError = true
            delay(1000)
            showDetectionError = false
            cameraViewModel.afterCaptureError()
        }
    }

    LaunchedEffect(Unit) {
        cameraViewModel.resetLiveAnalysis()
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    CameraScreenScaffold(
        cameraPreview = {
            CameraPreview(
                onImageAnalyzed = {
                    onImageAnalyzed(it)
                    if (!torchReapplied) {
                        captureController.cameraControl?.enableTorch(isTorchEnabled)
                        torchReapplied = true
                    }
                },
                captureController = captureController,
                onPreviewViewReady = { view ->
                    previewView = view
                    captureController.previewView = view
                },
                cameraPermission = cameraPermission,
                onError = { message, throwable -> cameraViewModel.logError(message, throwable) }
            )
        },
        cameraUiState = CameraUiState(
            document.pageCount(),
            liveAnalysisState,
            captureState,
            showDetectionError,
            isLandscape = isLandscape,
            isDebugMode,
            isTorchEnabled),
        onCapture = {
            previewView?.bitmap?.let {
                Log.i("BharatScan", "Pressed <Capture>")
                cameraViewModel.onCapturePressed(it)
                captureController.takePicture(
                    onImageCaptured = { imageProxy -> cameraViewModel.onImageCaptured(imageProxy) }
                )
            }
        },
        onDebugModeSwitched = { isDebugMode = !isDebugMode },
        onTorchSwitched = {
            cameraViewModel.setTorchEnabled(!isTorchEnabled)
        },
        thumbnailCoords = thumbnailCoords,
        pageListState = pageListState,
        selectedPageIndex = selectedPageIndex,
        onPageSelected = { index ->
            selectedPageIndex = index
            viewModel.navigateTo(Screen.Main.Document(index))
        },
        document = document,
        onExportClick = onFinalizePressed,
        navigation = navigation,
        captureController
    )
}

@Composable
private fun CameraScreenScaffold(
    cameraPreview: @Composable () -> Unit,
    cameraUiState: CameraUiState,
    onCapture: () -> Unit,
    onDebugModeSwitched: () -> Unit,
    onTorchSwitched: () -> Unit,
    thumbnailCoords: MutableState<Offset>,
    pageListState: androidx.compose.foundation.lazy.LazyListState,
    selectedPageIndex: Int,
    onPageSelected: (Int) -> Unit,
    document: org.bharatscan.app.ui.state.DocumentUiModel,
    onExportClick: () -> Unit,
    navigation: Navigation,
    captureController: CameraCaptureController,
) {
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    LaunchedEffect(focusPoint) {
        if (focusPoint != null) {
            delay(1000)
            focusPoint = null
        }
    }

    var tapCount by remember { mutableLongStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    val tapThreshold = 500L
    val onPageCountClick = {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTapTime < tapThreshold) {
            tapCount++
            if (tapCount >= 3) {
                onDebugModeSwitched()
                tapCount = 0
            }
        } else {
            tapCount = 1
        }
        lastTapTime = currentTime
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            CameraTopBar(
                onClose = navigation.back,
                onTorchToggle = onTorchSwitched,
                onSettings = { navigation.toSettingsScreen?.invoke() ?: navigation.toAboutScreen() }
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 0.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                CameraPreviewBox(
                    cameraPreview,
                    cameraUiState,
                    focusPoint,
                    onCapture,
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures { offset ->
                            focusPoint = offset
                            captureController.tapToFocus(offset)
                            onPageCountClick()
                        }
                    }
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 6.dp
            ) {
                CameraThumbnailStrip(
                    document = document,
                    pageListState = pageListState,
                    selectedPageIndex = selectedPageIndex,
                    onPageSelected = onPageSelected,
                    onExportClick = onExportClick,
                    thumbnailCoords = thumbnailCoords
                )
            }
        }
        if (cameraUiState.captureState is CaptureState.CapturePreview) {
            val page = cameraUiState.captureState.capturedPage.page
            CapturedImage(page.asImageBitmap(), thumbnailCoords)
        }
    }
}

@Composable
private fun CameraThumbnailStrip(
    document: org.bharatscan.app.ui.state.DocumentUiModel,
    pageListState: androidx.compose.foundation.lazy.LazyListState,
    selectedPageIndex: Int,
    onPageSelected: (Int) -> Unit,
    onExportClick: () -> Unit,
    thumbnailCoords: MutableState<Offset>,
) {
    val totalPages = document.pageCount()
    val density = LocalDensity.current

    LaunchedEffect(totalPages) {
        if (totalPages > 4) {
            pageListState.animateScrollToItem(totalPages - 1)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LazyRow(
            modifier = Modifier
                .weight(1f)
                .height(72.dp),
            state = pageListState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(document.pageKeys, key = { _, item -> item.saveKey }) { index, _ ->
                val image = document.loadThumbnail(index)
                if (image != null) {
                    val isSelected = index == selectedPageIndex
                    val isLast = index == totalPages - 1
                    val captureTargetModifier = if (isLast) {
                        Modifier.onGloballyPositioned { coordinates ->
                            val size = coordinates.size
                            val center = coordinates.localToWindow(
                                Offset(size.width / 2f, size.height / 2f)
                            )
                            with(density) {
                                thumbnailCoords.value = Offset(center.x, center.y)
                            }
                        }
                    } else {
                        Modifier
                    }
                    CameraThumbnailItem(
                        bitmap = image,
                        isSelected = isSelected,
                        onClick = { onPageSelected(index) },
                        modifier = captureTargetModifier
                    )
                }
            }
        }

        if (totalPages > 4) {
            OverflowCountButton(
                count = totalPages,
                onClick = { if (totalPages > 0) onPageSelected(totalPages - 1) }
            )
        }

        CompactExportButton(
            onClick = onExportClick,
            enabled = totalPages > 0,
            modifier = Modifier.height(44.dp)
        )
    }
}

@Composable
private fun CameraThumbnailItem(
    bitmap: android.graphics.Bitmap,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor =
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .size(64.dp)
            .clickable { onClick() }
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun OverflowCountButton(
    count: Int,
    onClick: () -> Unit,
) {
    BadgedBox(
        badge = {
            Badge {
                Text(text = count.toString())
            }
        }
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
            modifier = Modifier
                .size(44.dp)
                .clickable { onClick() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun CompactExportButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = modifier.widthIn(min = 92.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Done,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.export),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CameraTopBar(
    onClose: () -> Unit,
    onTorchToggle: () -> Unit,
    onSettings: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.back)
                    )
                }
                Spacer(Modifier.width(8.dp))
                BrandTitle(height = 22.dp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onTorchToggle) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = stringResource(R.string.turn_on_torch)
                    )
                }
                IconButton(onClick = onSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings)
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPreviewBox(
    cameraPreview: @Composable (() -> Unit),
    cameraUiState: CameraUiState,
    focusPoint: Offset?,
    onCapture: () -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
    ) {
        CameraPreviewWithOverlay(
            cameraPreview,
            cameraUiState,
            Modifier.align(Alignment.BottomCenter)
        )
        if (cameraUiState.isDebugMode) {
            MessageBox(cameraUiState.liveAnalysisState.inferenceTime)
        }
        FocusOverlay(focusPoint)
        CaptureButton(
            onClick = onCapture,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
        )
    }
}

@Composable
private fun CapturedImage(image: ImageBitmap, thumbnailCoords: MutableState<Offset>) {
    Surface(
        color = Color.Black.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxSize(),
    ) {}

    var isAnimating by remember { mutableStateOf(false) }
    LaunchedEffect(image) {
        delay(CAPTURED_IMAGE_DISPLAY_DURATION - ANIMATION_DURATION)
        isAnimating = true
    }
    var targetOffsetX by remember { mutableStateOf(0f) }
    var targetOffsetY by remember { mutableStateOf(0f) }

    val transition = updateTransition(targetState = isAnimating, label = "captureAnimation")
    val tween = tween<Float>(durationMillis = ANIMATION_DURATION)
    val offsetX by transition.animateFloat({ tween }, "offsetX") { if (it) targetOffsetX else 0f }
    val offsetY by transition.animateFloat({ tween }, "offsetY") { if (it) targetOffsetY else 0f }
    val scale by transition.animateFloat({ tween }, "scale") { if (it) 0.3f else 1f }

    val configuration = LocalConfiguration.current
    val imageSize = (configuration.screenWidthDp * 0.62f).coerceAtMost(260f).dp
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
    ) {
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = Modifier
                .size(imageSize)
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInWindow()
                    val centerX = bounds.left + bounds.width / 2
                    val centerY = bounds.top + bounds.height / 2
                    targetOffsetX = thumbnailCoords.value.x - centerX
                    targetOffsetY = thumbnailCoords.value.y - centerY
                }
                .graphicsLayer {
                    translationX = offsetX
                    translationY = offsetY
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(12.dp))
                .border(3.dp, BharatWhite, RoundedCornerShape(12.dp))
        )
    }
}

@Composable
fun CaptureButton(onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .size(88.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(88.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(BharatSaffron.copy(alpha = 0.5f), Color.Transparent)
                ),
                radius = size.minDimension / 2
            )
            drawCircle(
                color = BharatSaffron,
                radius = size.minDimension / 2,
                style = Stroke(width = 4.dp.toPx())
            )
        }
        Surface(
            modifier = Modifier.size(68.dp),
            color = BharatSaffron,
            shape = CircleShape,
            shadowElevation = 10.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = BharatWhite,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun CameraPreviewWithOverlay(
    cameraPreview: @Composable () -> Unit,
    cameraUiState: CameraUiState,
    modifier: Modifier,
) {
    val captureState = cameraUiState.captureState

    var showShutter by remember { mutableStateOf(false) }
    LaunchedEffect(captureState.frozenImage) {
        if (captureState.frozenImage != null) {
            showShutter = true
            delay(200)
            showShutter = false
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .border(2.dp, MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
    ) {
        cameraPreview()
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.35f)
                        )
                    )
                )
        )
        AnalysisOverlay(cameraUiState.liveAnalysisState, cameraUiState.isDebugMode)
        captureState.frozenImage?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
            )
        }
        if (showShutter) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            )
        }
        if (cameraUiState.showDetectionError) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.error_no_document),
                    color = Color.White,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun FocusOverlay(focusPoint: Offset?) {
    if (focusPoint == null) return
    val focusColor = MaterialTheme.colorScheme.secondary
    Canvas(modifier = Modifier.fillMaxSize()) {
        val size = 100f
        drawRect(
            color = focusColor,
            topLeft = Offset(
                focusPoint.x - size / 2,
                focusPoint.y - size / 2
            ),
            size = Size(size, size),
            style = Stroke(width = 4f)
        )
    }
}

@Composable
fun MessageBox(inferenceTime: Long) {
    Surface(
        color = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = if(inferenceTime == 0L) "Initializing..." else "Inference: $inferenceTime ms",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun Bar(
    pageCount: Int,
    onFinalizePressed: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        MainActionButton(
            onClick = onFinalizePressed,
            enabled = pageCount > 0,
            text = if (pageCount > 0) pageCountText(pageCount) else "Done",
            icon = Icons.Default.Done,
            modifier = Modifier.widthIn(min = 140.dp)
        )
    }
}


