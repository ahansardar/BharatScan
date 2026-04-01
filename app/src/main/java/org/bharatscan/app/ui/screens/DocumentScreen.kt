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
package org.bharatscan.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.engawapg.lib.zoomable.ZoomState
import net.engawapg.lib.zoomable.zoomable
import org.bharatscan.app.R
import org.bharatscan.app.domain.CropRect
import org.bharatscan.app.ui.Navigation
import org.bharatscan.app.ui.components.CommonPageListState
import org.bharatscan.app.ui.components.ConfirmationDialog
import org.bharatscan.app.ui.components.MainActionButton
import org.bharatscan.app.ui.components.MyScaffold
import org.bharatscan.app.ui.components.SecondaryActionButton
import org.bharatscan.app.ui.state.DocumentUiModel
import org.bharatscan.app.ui.theme.*
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScreen(
    document: DocumentUiModel,
    initialPage: Int,
    navigation: Navigation,
    onExportClick: () -> Unit,
    onDeleteImage: (String) -> Unit,
    onRotateImage: (String, Boolean) -> Unit,
    onPageReorder: (String, Int) -> Unit,
    onCropImage: (String, CropRect) -> Unit,
    onRetakeImage: (String) -> Unit,
) {
    val showDeletePageDialog = rememberSaveable { mutableStateOf(false) }
    val currentPageIndex = rememberSaveable { mutableIntStateOf(initialPage) }
    var cropTarget by remember { mutableStateOf<CropTarget?>(null) }
    if (currentPageIndex.intValue >= document.pageCount()) {
        currentPageIndex.intValue = document.pageCount() - 1
    }
    if (currentPageIndex.intValue < 0) {
        navigation.toCameraScreen()
        return
    }
    BackHandler { navigation.back() }

    val listState = rememberLazyListState()
    LaunchedEffect(initialPage) {
        listState.scrollToItem(initialPage)
    }

    MyScaffold(
        navigation = navigation,
        pageListState = CommonPageListState(
            document,
            onPageClick = { index -> currentPageIndex.intValue = index },
            onPageReorder = onPageReorder,
            currentPageIndex = currentPageIndex.intValue,
            listState = listState,
            showPageNumbers = true,
        ),
        onBack = navigation.back,
        bottomBar = {
            BottomBar(onExportClick, navigation.toCameraScreen)
        },
    ) { modifier ->
        DocumentPreview(
            document,
            currentPageIndex,
            { showDeletePageDialog.value = true },
            onRotateImage,
            onRequestCrop = { pageId ->
                val bitmap = document.load(currentPageIndex.intValue)
                if (bitmap != null) {
                    cropTarget = CropTarget(pageId, bitmap)
                }
            },
            onRetakeImage,
            modifier
        )
        if (showDeletePageDialog.value) {
            ConfirmationDialog(
                title = stringResource(R.string.delete_page),
                message = stringResource(R.string.delete_page_warning),
                showDialog = showDeletePageDialog
            ) { onDeleteImage(document.pageId(currentPageIndex.intValue)) }
        }
        cropTarget?.let { target ->
            CropImageDialog(
                bitmap = target.bitmap,
                onDismiss = { cropTarget = null },
                onApply = { rect ->
                    onCropImage(target.pageId, rect)
                    cropTarget = null
                }
            )
        }
    }
}

@Composable
private fun DocumentPreview(
    document: DocumentUiModel,
    currentPageIndex: MutableIntState,
    onDeleteImage: (String) -> Unit,
    onRotateImage: (String, Boolean) -> Unit,
    onRequestCrop: (String) -> Unit,
    onRetakeImage: (String) -> Unit,
    modifier: Modifier,
) {
    val imageId = document.pageId(currentPageIndex.intValue)
    Box (
        modifier = modifier.fillMaxSize()
    ) {
        val bitmap = document.load(currentPageIndex.intValue)
        if (bitmap != null) {
            val imageBitmap = bitmap.asImageBitmap()
            val zoomState = remember(imageId) {
                ZoomState(
                    contentSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat())
                )
            }

            Box(modifier = Modifier
                .fillMaxSize(0.92f)
                .align(Alignment.Center)
                .shadow(18.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .zoomable(zoomState)
            ) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // Control Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RotationButtons(imageId, onRotateImage, onRequestCrop)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                onClick = { onRetakeImage(imageId) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = stringResource(R.string.retake_page),
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.retake_page),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            SecondaryActionButton(
                Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.delete_page),
                onClick = { onDeleteImage(imageId) }
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
            shape = RoundedCornerShape(14.dp),
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            Text(
                "${currentPageIndex.intValue + 1} / ${document.pageCount()}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun RotationButtons(
    imageId: String,
    onRotateImage: (String, Boolean) -> Unit,
    onRequestCrop: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(18.dp))
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SecondaryActionButton(
                icon = Icons.Default.RotateLeft,
                contentDescription = stringResource(R.string.rotate_left),
                onClick = { onRotateImage(imageId, false) }
            )
            SecondaryActionButton(
                icon = Icons.Default.Crop,
                contentDescription = stringResource(R.string.crop_page),
                onClick = { onRequestCrop(imageId) }
            )
            SecondaryActionButton(
                icon = Icons.Default.RotateRight,
                contentDescription = stringResource(R.string.rotate_right),
                onClick = { onRotateImage(imageId, true) }
            )
        }
    }
}

private data class CropTarget(
    val pageId: String,
    val bitmap: android.graphics.Bitmap
)

@Composable
private fun CropImageDialog(
    bitmap: android.graphics.Bitmap,
    onDismiss: () -> Unit,
    onApply: (CropRect) -> Unit,
) {
    val density = LocalDensity.current
    val handleSize = 18.dp
    var cropRect by remember { mutableStateOf(CropRect(0.08f, 0.08f, 0.92f, 0.92f)) }

    fun clampRect(left: Float, top: Float, right: Float, bottom: Float): CropRect {
        val minSize = 0.08f
        val clampedLeft = left.coerceIn(0f, 1f - minSize)
        val clampedTop = top.coerceIn(0f, 1f - minSize)
        val clampedRight = right.coerceIn(clampedLeft + minSize, 1f)
        val clampedBottom = bottom.coerceIn(clampedTop + minSize, 1f)
        return CropRect(clampedLeft, clampedTop, clampedRight, clampedBottom)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Text(
                        text = stringResource(R.string.crop_page),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(
                        onClick = { onApply(cropRect) }
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    val containerW = constraints.maxWidth.toFloat()
                    val containerH = constraints.maxHeight.toFloat()
                    val imageAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val containerAspect = if (containerH == 0f) 1f else containerW / containerH
                    val imageW: Float
                    val imageH: Float
                    if (imageAspect >= containerAspect) {
                        imageW = containerW
                        imageH = if (imageAspect == 0f) containerH else containerW / imageAspect
                    } else {
                        imageH = containerH
                        imageW = containerH * imageAspect
                    }
                    val offsetX = (containerW - imageW) / 2f
                    val offsetY = (containerH - imageH) / 2f
                    val imageRect = Rect(offsetX, offsetY, offsetX + imageW, offsetY + imageH)
                    val rectPx = Rect(
                        left = imageRect.left + cropRect.left * imageRect.width,
                        top = imageRect.top + cropRect.top * imageRect.height,
                        right = imageRect.left + cropRect.right * imageRect.width,
                        bottom = imageRect.top + cropRect.bottom * imageRect.height
                    )

                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val dim = Color(0x88000000)
                        drawRect(dim, size = Size(size.width, rectPx.top))
                        drawRect(
                            dim,
                            topLeft = androidx.compose.ui.geometry.Offset(0f, rectPx.bottom),
                            size = Size(size.width, size.height - rectPx.bottom)
                        )
                        drawRect(
                            dim,
                            topLeft = androidx.compose.ui.geometry.Offset(0f, rectPx.top),
                            size = Size(rectPx.left, rectPx.height)
                        )
                        drawRect(
                            dim,
                            topLeft = androidx.compose.ui.geometry.Offset(rectPx.right, rectPx.top),
                            size = Size(size.width - rectPx.right, rectPx.height)
                        )

                        drawRect(
                            color = BharatSaffron,
                            topLeft = rectPx.topLeft,
                            size = rectPx.size,
                            style = Stroke(width = with(density) { 2.dp.toPx() })
                        )

                        val thirdW = rectPx.width / 3f
                        val thirdH = rectPx.height / 3f
                        for (i in 1..2) {
                            val x = rectPx.left + thirdW * i
                            drawLine(
                                color = BharatWhite.copy(alpha = 0.8f),
                                start = androidx.compose.ui.geometry.Offset(x, rectPx.top),
                                end = androidx.compose.ui.geometry.Offset(x, rectPx.bottom),
                                strokeWidth = with(density) { 1.dp.toPx() }
                            )
                            val y = rectPx.top + thirdH * i
                            drawLine(
                                color = BharatWhite.copy(alpha = 0.8f),
                                start = androidx.compose.ui.geometry.Offset(rectPx.left, y),
                                end = androidx.compose.ui.geometry.Offset(rectPx.right, y),
                                strokeWidth = with(density) { 1.dp.toPx() }
                            )
                        }
                    }

                    val handlePx = with(density) { handleSize.toPx() }
                    fun handleOffset(x: Float, y: Float): IntOffset =
                        IntOffset((x - handlePx / 2f).toInt(), (y - handlePx / 2f).toInt())

                    fun updateRect(newLeft: Float, newTop: Float, newRight: Float, newBottom: Float) {
                        cropRect = clampRect(newLeft, newTop, newRight, newBottom)
                    }

                    Box(
                        modifier = Modifier
                            .offset { handleOffset(rectPx.left, rectPx.top) }
                            .size(handleSize)
                            .background(BharatWhite, RoundedCornerShape(6.dp))
                            .border(1.dp, BharatSaffron, RoundedCornerShape(6.dp))
                            .pointerInput(cropRect, imageRect) {
                                detectDragGestures { change, dragAmount ->
                                    val dx = if (imageRect.width == 0f) 0f else dragAmount.x / imageRect.width
                                    val dy = if (imageRect.height == 0f) 0f else dragAmount.y / imageRect.height
                                    updateRect(
                                        cropRect.left + dx,
                                        cropRect.top + dy,
                                        cropRect.right,
                                        cropRect.bottom
                                    )
                                    change.consume()
                                }
                            }
                    )

                    Box(
                        modifier = Modifier
                            .offset { handleOffset(rectPx.right, rectPx.top) }
                            .size(handleSize)
                            .background(BharatWhite, RoundedCornerShape(6.dp))
                            .border(1.dp, BharatSaffron, RoundedCornerShape(6.dp))
                            .pointerInput(cropRect, imageRect) {
                                detectDragGestures { change, dragAmount ->
                                    val dx = if (imageRect.width == 0f) 0f else dragAmount.x / imageRect.width
                                    val dy = if (imageRect.height == 0f) 0f else dragAmount.y / imageRect.height
                                    updateRect(
                                        cropRect.left,
                                        cropRect.top + dy,
                                        cropRect.right + dx,
                                        cropRect.bottom
                                    )
                                    change.consume()
                                }
                            }
                    )

                    Box(
                        modifier = Modifier
                            .offset { handleOffset(rectPx.left, rectPx.bottom) }
                            .size(handleSize)
                            .background(BharatWhite, RoundedCornerShape(6.dp))
                            .border(1.dp, BharatSaffron, RoundedCornerShape(6.dp))
                            .pointerInput(cropRect, imageRect) {
                                detectDragGestures { change, dragAmount ->
                                    val dx = if (imageRect.width == 0f) 0f else dragAmount.x / imageRect.width
                                    val dy = if (imageRect.height == 0f) 0f else dragAmount.y / imageRect.height
                                    updateRect(
                                        cropRect.left + dx,
                                        cropRect.top,
                                        cropRect.right,
                                        cropRect.bottom + dy
                                    )
                                    change.consume()
                                }
                            }
                    )

                    Box(
                        modifier = Modifier
                            .offset { handleOffset(rectPx.right, rectPx.bottom) }
                            .size(handleSize)
                            .background(BharatWhite, RoundedCornerShape(6.dp))
                            .border(1.dp, BharatSaffron, RoundedCornerShape(6.dp))
                            .pointerInput(cropRect, imageRect) {
                                detectDragGestures { change, dragAmount ->
                                    val dx = if (imageRect.width == 0f) 0f else dragAmount.x / imageRect.width
                                    val dy = if (imageRect.height == 0f) 0f else dragAmount.y / imageRect.height
                                    updateRect(
                                        cropRect.left,
                                        cropRect.top,
                                        cropRect.right + dx,
                                        cropRect.bottom + dy
                                    )
                                    change.consume()
                                }
                            }
                    )

                    val midX = rectPx.left + rectPx.width / 2f
                    val midY = rectPx.top + rectPx.height / 2f

                    Box(
                        modifier = Modifier
                            .offset { handleOffset(midX, rectPx.top) }
                            .size(handleSize)
                            .background(BharatWhite, RoundedCornerShape(6.dp))
                            .border(1.dp, BharatSaffron, RoundedCornerShape(6.dp))
                            .pointerInput(cropRect, imageRect) {
                                detectDragGestures { change, dragAmount ->
                                    val dy = if (imageRect.height == 0f) 0f else dragAmount.y / imageRect.height
                                    updateRect(
                                        cropRect.left,
                                        cropRect.top + dy,
                                        cropRect.right,
                                        cropRect.bottom
                                    )
                                    change.consume()
                                }
                            }
                    )

                    Box(
                        modifier = Modifier
                            .offset { handleOffset(midX, rectPx.bottom) }
                            .size(handleSize)
                            .background(BharatWhite, RoundedCornerShape(6.dp))
                            .border(1.dp, BharatSaffron, RoundedCornerShape(6.dp))
                            .pointerInput(cropRect, imageRect) {
                                detectDragGestures { change, dragAmount ->
                                    val dy = if (imageRect.height == 0f) 0f else dragAmount.y / imageRect.height
                                    updateRect(
                                        cropRect.left,
                                        cropRect.top,
                                        cropRect.right,
                                        cropRect.bottom + dy
                                    )
                                    change.consume()
                                }
                            }
                    )

                    Box(
                        modifier = Modifier
                            .offset { handleOffset(rectPx.left, midY) }
                            .size(handleSize)
                            .background(BharatWhite, RoundedCornerShape(6.dp))
                            .border(1.dp, BharatSaffron, RoundedCornerShape(6.dp))
                            .pointerInput(cropRect, imageRect) {
                                detectDragGestures { change, dragAmount ->
                                    val dx = if (imageRect.width == 0f) 0f else dragAmount.x / imageRect.width
                                    updateRect(
                                        cropRect.left + dx,
                                        cropRect.top,
                                        cropRect.right,
                                        cropRect.bottom
                                    )
                                    change.consume()
                                }
                            }
                    )

                    Box(
                        modifier = Modifier
                            .offset { handleOffset(rectPx.right, midY) }
                            .size(handleSize)
                            .background(BharatWhite, RoundedCornerShape(6.dp))
                            .border(1.dp, BharatSaffron, RoundedCornerShape(6.dp))
                            .pointerInput(cropRect, imageRect) {
                                detectDragGestures { change, dragAmount ->
                                    val dx = if (imageRect.width == 0f) 0f else dragAmount.x / imageRect.width
                                    updateRect(
                                        cropRect.left,
                                        cropRect.top,
                                        cropRect.right + dx,
                                        cropRect.bottom
                                    )
                                    change.consume()
                                }
                            }
                    )

                    val rectWidthDp = with(density) { max(1f, rectPx.width).toDp() }
                    val rectHeightDp = with(density) { max(1f, rectPx.height).toDp() }
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(rectPx.left.toInt(), rectPx.top.toInt()) }
                            .size(width = rectWidthDp, height = rectHeightDp)
                            .pointerInput(cropRect, imageRect) {
                                detectDragGestures { change, dragAmount ->
                                    val dx = if (imageRect.width == 0f) 0f else dragAmount.x / imageRect.width
                                    val dy = if (imageRect.height == 0f) 0f else dragAmount.y / imageRect.height
                                    val width = cropRect.width()
                                    val height = cropRect.height()
                                    val newLeft = (cropRect.left + dx).coerceIn(0f, 1f - width)
                                    val newTop = (cropRect.top + dy).coerceIn(0f, 1f - height)
                                    cropRect = CropRect(newLeft, newTop, newLeft + width, newTop + height)
                                    change.consume()
                                }
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomBar(
    onExportClick: () -> Unit,
    onAddPageClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FilledTonalButton(
            onClick = onAddPageClick,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.add_page),
                maxLines = 1,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis
            )
        }
        MainActionButton(
            onClick = onExportClick,
            icon = Icons.Default.Done,
            text = stringResource(R.string.export),
            modifier = Modifier.weight(1.2f).height(56.dp)
        )
    }
}


