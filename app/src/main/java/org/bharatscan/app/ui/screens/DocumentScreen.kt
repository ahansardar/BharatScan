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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import net.engawapg.lib.zoomable.ZoomState
import net.engawapg.lib.zoomable.zoomable
import org.bharatscan.app.R
import org.bharatscan.app.ui.Navigation
import org.bharatscan.app.ui.components.CommonPageListState
import org.bharatscan.app.ui.components.ConfirmationDialog
import org.bharatscan.app.ui.components.MainActionButton
import org.bharatscan.app.ui.components.MyScaffold
import org.bharatscan.app.ui.components.SecondaryActionButton
import org.bharatscan.app.ui.state.DocumentUiModel
import org.bharatscan.app.ui.theme.*

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
    onCropImage: (String) -> Unit,
) {
    val showDeletePageDialog = rememberSaveable { mutableStateOf(false) }
    val currentPageIndex = rememberSaveable { mutableIntStateOf(initialPage) }
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
            onCropImage,
            modifier
        )
        if (showDeletePageDialog.value) {
            ConfirmationDialog(
                title = stringResource(R.string.delete_page),
                message = stringResource(R.string.delete_page_warning),
                showDialog = showDeletePageDialog
            ) { onDeleteImage(document.pageId(currentPageIndex.intValue)) }
        }
    }
}

@Composable
private fun DocumentPreview(
    document: DocumentUiModel,
    currentPageIndex: MutableIntState,
    onDeleteImage: (String) -> Unit,
    onRotateImage: (String, Boolean) -> Unit,
    onCropImage: (String) -> Unit,
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
            RotationButtons(imageId, onRotateImage, onCropImage)
        }

        SecondaryActionButton(
            Icons.Outlined.Delete,
            contentDescription = stringResource(R.string.delete_page),
            onClick = { onDeleteImage(imageId) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        )

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
    onCropImage: (String) -> Unit,
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
                onClick = { onCropImage(imageId) }
            )
            SecondaryActionButton(
                icon = Icons.Default.RotateRight,
                contentDescription = stringResource(R.string.rotate_right),
                onClick = { onRotateImage(imageId, true) }
            )
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


