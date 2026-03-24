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
package org.bharatscan.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bharatscan.app.data.ImageRepository
import org.bharatscan.app.domain.CapturedPage
import org.bharatscan.app.domain.ImageSegmentationService
import org.bharatscan.app.domain.PageMetadata
import org.bharatscan.app.domain.PageViewKey
import org.bharatscan.app.domain.Rotation
import org.bharatscan.app.ui.NavigationState
import org.bharatscan.app.ui.Screen
import org.bharatscan.app.ui.state.DocumentUiModel
import org.bharatscan.app.ui.screens.camera.extractDocumentFromBitmap
import org.bharatscan.imageprocessing.ImageSize
import org.bharatscan.imageprocessing.Mask
import org.bharatscan.imageprocessing.Point
import org.bharatscan.imageprocessing.Quad
import org.bharatscan.imageprocessing.biggestContour
import org.bharatscan.imageprocessing.createQuad
import org.bharatscan.imageprocessing.quad.minAreaRect
import org.bharatscan.imageprocessing.detectDocumentQuad
import org.bharatscan.imageprocessing.encodeJpeg
import org.bharatscan.imageprocessing.scaledTo
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class MainViewModel(
    val imageRepository: ImageRepository,
    private val imageSegmentationService: ImageSegmentationService,
    launchMode: LaunchMode
): ViewModel() {

    private val _navigationState = MutableStateFlow(NavigationState.initial(launchMode))
    val currentScreen: StateFlow<Screen> = _navigationState.map { it.current }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _navigationState.value.current)

    private val _pages = MutableStateFlow(imageRepository.pages())
    val documentUiModel: StateFlow<DocumentUiModel> =
        _pages.map { pages ->
            DocumentUiModel(
                pageKeys = pages.map { p ->
                    PageViewKey(p.id, p.manualRotation)
                }.toImmutableList(),
                imageLoader = ::getBitmap,
                thumbnailLoader = ::getThumbnail,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = DocumentUiModel(persistentListOf(), ::getBitmap, ::getThumbnail)
        )

    fun navigateTo(destination: Screen) {
        _navigationState.update { it.navigateTo(destination) }
    }

    fun navigateBack() {
        _navigationState.update { stack -> stack.navigateBack() }
    }

    fun rotateImage(id: String, clockwise: Boolean) {
        viewModelScope.launch {
            imageRepository.rotate(id, clockwise)
            _pages.value = imageRepository.pages()
        }
    }

    fun autoCropPage(id: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.Default) {
            val success = runCatching {
                val sourceBytes = imageRepository.sourceJpegBytes(id)
                    ?: imageRepository.jpegBytes(id)
                    ?: return@runCatching false
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inMutable = false
                }
                val decoded = BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.size, options)
                    ?: return@runCatching false
                val sourceBitmap = if (decoded.config == Bitmap.Config.HARDWARE || decoded.config == null) {
                    val copy = decoded.copy(Bitmap.Config.ARGB_8888, false)
                    decoded.recycle()
                    copy ?: return@runCatching false
                } else {
                    decoded
                }
                if (sourceBitmap.width <= 0 || sourceBitmap.height <= 0) {
                    sourceBitmap.recycle()
                    return@runCatching false
                }

                val segmentation = imageSegmentationService.runSegmentationAndReturn(sourceBitmap)
                val mask = segmentation?.segmentation ?: FullMask(sourceBitmap.width, sourceBitmap.height)
                val quad = segmentation?.let {
                    detectDocumentQuad(
                        it.segmentation,
                        ImageSize(sourceBitmap.width.toDouble(), sourceBitmap.height.toDouble()),
                        false
                    )
                }
                val scaledQuad = quad?.scaledTo(
                    fromWidth = segmentation?.segmentation?.width ?: 1,
                    fromHeight = segmentation?.segmentation?.height ?: 1,
                    toWidth = sourceBitmap.width,
                    toHeight = sourceBitmap.height
                )
                val finalQuad = scaledQuad ?: detectQuadFallback(sourceBitmap)
                    ?: run {
                        sourceBitmap.recycle()
                        return@runCatching false
                    }

                val captured = extractDocumentFromBitmap(
                    sourceBitmap,
                    finalQuad,
                    0,
                    mask
                )
                sourceBitmap.recycle()
                imageRepository.replace(
                    id,
                    compressJpeg(captured.page, 85),
                    compressJpeg(captured.source, 90),
                    captured.metadata
                )
                _pages.value = imageRepository.pages()
                true
            }.getOrElse { false }

            withContext(Dispatchers.Main) {
                onComplete(success)
            }
        }
    }

    private class FullMask(override val width: Int, override val height: Int) : Mask {
        override fun toMat(): Mat = Mat(height, width, CvType.CV_8UC1, Scalar(255.0))
    }

    private fun detectQuadFallback(sourceBitmap: Bitmap): Quad? {
        val rgba = Mat()
        Utils.bitmapToMat(sourceBitmap, rgba)
        val gray = Mat()
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
        val edges = Mat()
        Imgproc.Canny(blurred, edges, 75.0, 200.0)
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        Imgproc.dilate(edges, edges, kernel)

        val contour: MatOfPoint? = biggestContour(edges)
        val quad = contour
            ?.toList()
            ?.map { Point(it.x, it.y) }
            ?.let { polygon -> minAreaRect(polygon, sourceBitmap.width, sourceBitmap.height) }
            ?.let { rect -> if (rect.size == 4) createQuad(rect) else null }

        kernel.release()
        edges.release()
        blurred.release()
        gray.release()
        rgba.release()
        return quad
    }

    fun movePage(id: String, newIndex: Int) {
        viewModelScope.launch {
            imageRepository.movePage(id, newIndex)
            _pages.value = imageRepository.pages()
        }
    }

    fun deletePage(id: String) {
        viewModelScope.launch {
            imageRepository.delete(id)
            _pages.value = imageRepository.pages()
        }
    }

    fun startNewDocument() {
        _pages.value = persistentListOf()
        viewModelScope.launch {
            imageRepository.clear()
        }
    }

    fun getBitmap(key: PageViewKey): Bitmap? {
        val bytes = imageRepository.jpegBytes(key)
        return bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    }

    fun getThumbnail(key: PageViewKey): Bitmap? {
        val bytes = imageRepository.getThumbnail(key)
        return bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    }

    fun handleImageCaptured(capturedPage: CapturedPage) {
        viewModelScope.launch {
            imageRepository.add(
                compressJpeg(capturedPage.page, 75),
                compressJpeg(capturedPage.source, 90),
                capturedPage.metadata,
            )
            _pages.value = imageRepository.pages()
        }
    }

    fun importPdfForEditing(
        context: Context,
        uri: android.net.Uri,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _pages.value = persistentListOf()
            imageRepository.clear()
            val pfd = try {
                context.contentResolver.openFileDescriptor(uri, "r")
            } catch (_: Exception) {
                null
            } ?: return@launch

            val renderer = PdfRenderer(pfd)
            try {
                val meta = PageMetadata(
                    normalizedQuad = Quad(
                        Point(0.0, 0.0),
                        Point(1.0, 0.0),
                        Point(1.0, 1.0),
                        Point(0.0, 1.0)
                    ),
                    baseRotation = Rotation.R0,
                    isColored = true
                )
                repeat(renderer.pageCount) { index ->
                    val page = renderer.openPage(index)
                    val bitmap = Bitmap.createBitmap(
                        page.width * 2,
                        page.height * 2,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    val bytes = compressJpeg(bitmap, 90)
                    imageRepository.add(bytes, bytes, meta)
                    bitmap.recycle()
                }
            } finally {
                renderer.close()
                pfd.close()
            }
            _pages.value = imageRepository.pages()
            withContext(Dispatchers.Main) { onComplete() }
        }
    }

    private fun compressJpeg(bitmap: Bitmap, quality: Int): ByteArray {
        val rgba = Mat()
        Utils.bitmapToMat(bitmap, rgba)
        val bgr = Mat()
        Imgproc.cvtColor(rgba, bgr, Imgproc.COLOR_RGBA2BGR)
        rgba.release()
        return try {
            encodeJpeg(bgr, quality)
        } finally {
            bgr.release()
        }
    }
}



