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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.bharatscan.app.ocr.TextRecognitionHelper
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bharatscan.app.R
import org.bharatscan.app.BuildConfig
import org.bharatscan.app.ui.components.BackButton
import org.bharatscan.app.ui.screens.export.formatFileSize
import androidx.compose.runtime.mutableStateMapOf
import org.bharatscan.app.ui.theme.BharatNavy
import org.bharatscan.app.ui.theme.BharatWhite
import java.time.Duration
import java.time.Instant
import java.text.DateFormat
import java.util.Date
import java.io.File
import java.security.Security
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Canvas as AndroidCanvas
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import org.bouncycastle.jce.provider.BouncyCastleProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    uri: Uri,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onPrint: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    val bitmaps = remember { mutableStateListOf<Bitmap?>() }
    val listState = rememberLazyListState()
    val pageZoomScales = remember { mutableStateMapOf<Int, Float>() }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var searchHighlights by remember { mutableStateOf<Map<Int, List<RectF>>>(emptyMap()) }
    var ocrIndex by remember { mutableStateOf<Map<Int, List<OcrLineBox>>>(emptyMap()) }
    var ocrReady by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(0) }
    var renderKey by remember { mutableIntStateOf(0) }
    var pdfSource by remember(uri) { mutableStateOf<PdfSource>(PdfSource.ContentUri(uri)) }
    var showPasswordPrompt by remember { mutableStateOf(false) }
    var passwordValue by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }
    var decryptingPdf by remember { mutableStateOf(false) }
    var pdfPassword by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uri) {
        passwordValue = ""
        passwordError = false
        decryptingPdf = false
        pdfPassword = null
    }

    DisposableEffect(pdfSource, renderKey) {
        val sourceForDispose = pdfSource
        var localRenderer: PdfRenderer? = null
        val pfd = try {
            openPdfSourceDescriptor(context, sourceForDispose)
        } catch (_: Exception) {
            null
        }
        if (pfd != null) {
            try {
                val r = PdfRenderer(pfd)
                localRenderer = r
                renderer = r
                pageCount = r.pageCount
                if (BuildConfig.DEBUG) {
                    Log.d("PdfViewer", "Renderer opened with $pageCount pages for $sourceForDispose")
                }
                bitmaps.clear()
                pageZoomScales.clear()
                repeat(r.pageCount) { bitmaps.add(null) }
                showPasswordPrompt = false
                passwordError = false
            } catch (_: SecurityException) {
                showPasswordPrompt = true
                passwordError = false
                renderer = null
                pageCount = 0
                bitmaps.clear()
                pageZoomScales.clear()
                pfd.close()
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e("PdfViewer", "Failed to open PdfRenderer for $sourceForDispose", e)
                }
                renderer = null
                pageCount = 0
                bitmaps.clear()
                pageZoomScales.clear()
                pfd.close()
                Toast.makeText(
                    context,
                    context.getString(R.string.pdf_password_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else if (BuildConfig.DEBUG) {
            Log.e("PdfViewer", "Failed to open ParcelFileDescriptor for $sourceForDispose")
        }
        onDispose {
            localRenderer?.close()
            pfd?.close()
            if (sourceForDispose is PdfSource.FilePath) {
                sourceForDispose.file.delete()
            }
            bitmaps.forEach { it?.recycle() }
        }
    }

    BackHandler { onBack() }

    val document = remember(uri) { DocumentFile.fromSingleUri(context, uri) }
    val fileName = document?.name ?: uri.lastPathSegment ?: context.getString(R.string.document)
    val fileSize = document?.length()?.takeIf { it > 0 }?.let { formatFileSize(it, context) }
        ?: stringResource(R.string.unknown_size)
    val timestamp = resolveFileTimestamp(context, uri)
        ?: document?.lastModified()?.takeIf { it > 0 }
        ?: System.currentTimeMillis()
    val scannedText = formatTimestamp(timestamp)

    LaunchedEffect(pdfSource, renderKey) {
        ocrReady = false
        ocrIndex = buildOcrIndex(context, pdfSource)
        ocrReady = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                fileName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${fileSize} \u2022 ${scannedText}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            if (!ocrReady) {
                                Text(
                                    stringResource(R.string.scanning_text),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    },
                    navigationIcon = { BackButton(onClick = onBack) },
                    actions = {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            bottomBar = {
                PdfActionBar(
                    onShare = onShare,
                    onEdit = onEdit,
                    onPrint = onPrint,
                    onDelete = { showDeleteConfirm = true }
                )
            }
        ) { padding ->
            if (pageCount > 0) {
                LaunchedEffect(listState, pageCount) {
                    snapshotFlow { listState.layoutInfo }
                        .collectLatest { layoutInfo ->
                            val viewportStart = layoutInfo.viewportStartOffset
                            val viewportEnd = layoutInfo.viewportEndOffset
                            val mostVisible = layoutInfo.visibleItemsInfo.maxByOrNull { item ->
                                val itemStart = item.offset
                                val itemEnd = item.offset + item.size
                                val visible = min(itemEnd, viewportEnd) - max(itemStart, viewportStart)
                                visible
                            }
                            mostVisible?.let { currentPage = it.index.coerceIn(0, pageCount - 1) }
                        }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    if (!ocrReady) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                        )
                    }
                    LazyRow(
                        state = listState,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp),
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed((0 until pageCount).toList()) { index, _ ->
                            val bitmap = bitmaps[index] ?: remember(index) {
                                renderer?.let { r ->
                                    val page = r.openPage(index)
                                    val b = Bitmap.createBitmap(
                                        page.width * 2,
                                        page.height * 2,
                                        Bitmap.Config.ARGB_8888
                                    )
                                    b.eraseColor(android.graphics.Color.WHITE)
                                    page.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    page.close()
                                    bitmaps[index] = b
                                    b
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillParentMaxHeight()
                                    .fillParentMaxWidth()
                            ) {
                                if (bitmap != null) {
                                    ZoomablePdfPage(
                                        bitmap = bitmap,
                                        onZoomChange = { scale -> pageZoomScales[index] = scale },
                                        highlights = searchHighlights[index].orEmpty(),
                                        ocrLines = ocrIndex[index].orEmpty(),
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                    PageIndicator(
                        text = stringResource(
                            R.string.page_of,
                            currentPage + 1,
                            pageCount
                        ),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (showPasswordPrompt) {
        AlertDialog(
            onDismissRequest = {
                if (!decryptingPdf) {
                    showPasswordPrompt = false
                    onBack()
                }
            },
            title = { Text(stringResource(R.string.pdf_password_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.pdf_password_message))
                    OutlinedTextField(
                        value = passwordValue,
                        onValueChange = {
                            passwordValue = it
                            if (passwordError) {
                                passwordError = false
                            }
                        },
                        singleLine = true,
                        isError = passwordError,
                        label = { Text(stringResource(R.string.password_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (passwordError) {
                        Text(
                            text = stringResource(R.string.pdf_password_incorrect),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    if (decryptingPdf) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = stringResource(R.string.pdf_password_opening),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = passwordValue.trim().isNotEmpty() && !decryptingPdf,
                    onClick = {
                        val password = passwordValue.trim()
                        if (password.isEmpty()) {
                            passwordError = true
                            return@TextButton
                        }
                        decryptingPdf = true
                        passwordError = false
                        scope.launch {
                            when (val result = decryptPdfToCache(context, uri, password)) {
                                is DecryptResult.Success -> {
                                    pdfSource = PdfSource.FilePath(result.file)
                                    pdfPassword = password
                                    showPasswordPrompt = false
                                    passwordValue = ""
                                }
                                is DecryptResult.InvalidPassword -> {
                                    passwordError = true
                                }
                                is DecryptResult.Failed -> {
                                    val message = if (BuildConfig.DEBUG) {
                                        "${context.getString(R.string.pdf_password_failed)} (${result.reason})"
                                    } else {
                                        context.getString(R.string.pdf_password_failed)
                                    }
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            }
                            decryptingPdf = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.open))
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !decryptingPdf,
                    onClick = {
                        showPasswordPrompt = false
                        onBack()
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = !decryptingPdf,
                dismissOnClickOutside = !decryptingPdf
            )
        )
    }

    if (showSearch) {
        AlertDialog(
            onDismissRequest = { showSearch = false },
            title = { Text(stringResource(R.string.search_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.search_documents)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = searchQuery.trim().isNotEmpty() && !searching,
                    onClick = {
                        val query = searchQuery.trim()
                        searching = true
                        scope.launch {
                            val matches = if (ocrReady) {
                                findMatchesFromOcr(ocrIndex, query)
                            } else {
                                emptyMap()
                            }
                            searching = false
                            searchHighlights = matches
                            if (matches.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    if (!ocrReady) context.getString(R.string.scanning_text) else context.getString(R.string.no_results),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                val firstPage = matches.keys.minOrNull() ?: 0
                                listState.animateScrollToItem(firstPage)
                                Toast.makeText(
                                    context,
                                    "${matches.values.sumOf { it.size }} match(es)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            showSearch = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.search_title))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSearch = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_document_title)) },
            text = { Text(stringResource(R.string.delete_document_warning)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text(stringResource(R.string.delete_document))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

}

@Composable
private fun PdfActionBar(
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onPrint: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 14.dp,
        modifier = Modifier.padding(bottom = 48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ActionItem(Icons.Default.Share, stringResource(R.string.share), onShare)
            ActionItem(Icons.Default.Edit, stringResource(R.string.edit), onEdit)
            ActionItem(Icons.Default.Print, stringResource(R.string.print_pdf), onPrint)
            ActionItem(Icons.Default.Delete, stringResource(R.string.delete_document), onDelete)
        }
    }
}

@Composable
private fun ActionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.widthIn(min = 64.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.secondary)
        }
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

// Print button uses standard ActionItem styling now.

private fun relativeTimeLabel(timestamp: Long): String {
    val duration = Duration.between(Instant.ofEpochMilli(timestamp), Instant.now())
    val minutes = duration.toMinutes()
    val hours = duration.toHours()
    val days = duration.toDays()
    return when {
        minutes < 1 -> "SCANNED JUST NOW"
        minutes < 60 -> "SCANNED ${minutes} MIN AGO"
        hours < 24 -> "SCANNED ${hours} HR AGO"
        else -> "SCANNED ${days} DAY${if (days == 1L) "" else "S"} AGO"
    }
}

private sealed class PdfSource {
    data class ContentUri(val uri: Uri) : PdfSource()
    data class FilePath(val file: File) : PdfSource()
}

private sealed class DecryptResult {
    data class Success(val file: File) : DecryptResult()
    data object InvalidPassword : DecryptResult()
    data class Failed(val reason: String) : DecryptResult()
}

private fun ensureBouncyCastleProvider() {
    val provider = Security.getProvider("BC")
    if (provider !is BouncyCastleProvider) {
        Security.removeProvider("BC")
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }
}

private fun openPdfSourceDescriptor(context: Context, source: PdfSource): ParcelFileDescriptor? {
    return when (source) {
        is PdfSource.ContentUri -> context.contentResolver.openFileDescriptor(source.uri, "r")
        is PdfSource.FilePath -> ParcelFileDescriptor.open(source.file, ParcelFileDescriptor.MODE_READ_ONLY)
    }
}

private suspend fun decryptPdfToCache(
    context: Context,
    uri: Uri,
    password: String
): DecryptResult {
    return withContext(Dispatchers.IO) {
        ensureBouncyCastleProvider()
        val tempFile = kotlin.runCatching {
            File.createTempFile("bharatscan_pdf_", ".pdf", context.cacheDir)
        }.getOrNull() ?: return@withContext DecryptResult.Failed("Temp file creation failed")

        val input = try {
            context.contentResolver.openInputStream(uri)
        } catch (_: Exception) {
            null
        } ?: return@withContext DecryptResult.Failed("Cannot open input stream")

        return@withContext try {
            input.use { stream ->
                PDDocument.load(stream, password).use { document ->
                    document.setAllSecurityToBeRemoved(true)
                    document.save(tempFile)
                }
            }
            if (BuildConfig.DEBUG) {
                Log.d("PdfViewer", "Decrypted PDF saved ${tempFile.absolutePath} size=${tempFile.length()}")
            }
            DecryptResult.Success(tempFile)
        } catch (_: InvalidPasswordException) {
            tempFile.delete()
            DecryptResult.InvalidPassword
        } catch (e: Exception) {
            Log.e("PdfViewer", "Failed to decrypt PDF", e)
            tempFile.delete()
            val reason = "${e.javaClass.simpleName}: ${e.message ?: "unknown error"}"
            DecryptResult.Failed(reason)
        }
    }
}

private suspend fun buildOcrIndex(context: Context, source: PdfSource): Map<Int, List<OcrLineBox>> {
    return withContext(Dispatchers.Default) {
        val pfd = try {
            openPdfSourceDescriptor(context, source)
        } catch (_: Exception) {
            null
        } ?: return@withContext emptyMap()

        val renderer = try {
            PdfRenderer(pfd)
        } catch (_: SecurityException) {
            pfd.close()
            return@withContext emptyMap()
        }
        val engine = TextRecognitionHelper.createEngine(context)
        val index = mutableMapOf<Int, List<OcrLineBox>>()
        try {
            for (pageIndex in 0 until renderer.pageCount) {
                val page = renderer.openPage(pageIndex)
                val bitmap = Bitmap.createBitmap(
                    page.width * 2,
                    page.height * 2,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val lines = try {
                    engine.recognize(bitmap).map { line ->
                        OcrLineBox(
                            text = line.text,
                            rect = RectF(
                                (line.bounds.left / bitmap.width).coerceIn(0f, 1f),
                                (line.bounds.top / bitmap.height).coerceIn(0f, 1f),
                                (line.bounds.right / bitmap.width).coerceIn(0f, 1f),
                                (line.bounds.bottom / bitmap.height).coerceIn(0f, 1f)
                            )
                        )
                    }
                } catch (_: Exception) {
                    emptyList()
                } finally {
                    bitmap.recycle()
                }
                index[pageIndex] = lines
            }
        } finally {
            engine.close()
            renderer.close()
            pfd.close()
        }
        index
    }
}

private fun findMatchesFromOcr(
    index: Map<Int, List<OcrLineBox>>,
    query: String
): Map<Int, List<RectF>> {
    if (query.isBlank()) return emptyMap()
    val needle = query.trim().lowercase()
    val matches = mutableMapOf<Int, MutableList<RectF>>()
    index.forEach { (page, lines) ->
        lines.forEach { line ->
            if (line.text.lowercase().contains(needle)) {
                matches.getOrPut(page) { mutableListOf() }.add(line.rect)
            }
        }
    }
    return matches
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    return formatter.format(Date(timestamp))
}

private fun resolveFileTimestamp(context: Context, uri: Uri): Long? {
    val columns = arrayOf(
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        MediaStore.MediaColumns.DATE_MODIFIED,
        MediaStore.MediaColumns.DATE_ADDED
    )
    return try {
        context.contentResolver.query(uri, columns, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            for (i in columns.indices) {
                val idx = cursor.getColumnIndex(columns[i])
                if (idx >= 0) {
                    val value = cursor.getLong(idx)
                    if (value > 0) {
                        return if (value < 1_000_000_000_000L) value * 1000L else value
                    }
                }
            }
            null
        }
    } catch (_: Exception) {
        null
    }
}

private data class OcrLineBox(
    val text: String,
    val rect: RectF
)

@Composable
private fun ZoomablePdfPage(
    bitmap: Bitmap,
    onZoomChange: (Float) -> Unit,
    highlights: List<RectF> = emptyList(),
    ocrLines: List<OcrLineBox> = emptyList(),
    modifier: Modifier = Modifier
) {
    var scale by remember(bitmap) { mutableStateOf(1f) }
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(scale) {
        onZoomChange(scale)
    }

    BoxWithConstraints(modifier = modifier) {
        val containerW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val containerH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val imageAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
        val containerAspect = containerW / containerH
        val displayW: Float
        val displayH: Float
        if (imageAspect > containerAspect) {
            displayW = containerW
            displayH = containerW / imageAspect
        } else {
            displayH = containerH
            displayW = containerH * imageAspect
        }
        val imageLeft = (containerW - displayW) / 2f
        val imageTop = (containerH - displayH) / 2f

        fun clampOffset(raw: Offset, newScale: Float): Offset {
            val maxX = ((displayW * newScale) - containerW) / 2f
            val maxY = ((displayH * newScale) - containerH) / 2f
            val clampedX = raw.x.coerceIn(-maxX.coerceAtLeast(0f), maxX.coerceAtLeast(0f))
            val clampedY = raw.y.coerceIn(-maxY.coerceAtLeast(0f), maxY.coerceAtLeast(0f))
            return Offset(clampedX, clampedY)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(bitmap) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var hasZoomed = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val changes = event.changes
                            val pressed = changes.any { it.pressed }
                            if (!pressed) break

                            val pointerCount = changes.count { it.pressed }
                            if (pointerCount >= 2) {
                                hasZoomed = true
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val newScale = (scale * zoom).coerceIn(1f, 4f)
                                val newOffset = if (newScale <= 1.01f) {
                                    Offset.Zero
                                } else {
                                    clampOffset(offset + pan, newScale)
                                }
                                scale = newScale
                                offset = newOffset
                                changes.forEach { it.consume() }
                            } else if (scale > 1.01f || hasZoomed) {
                                val pan = event.calculatePan()
                                val newOffset = clampOffset(offset + pan, scale)
                                offset = newOffset
                                changes.forEach { it.consume() }
                            } else {
                                // Let parent (pager) handle single-finger swipes when not zoomed.
                                break
                            }
                        }
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                if (ocrLines.isNotEmpty()) {
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    SelectionContainer {
                        Box(modifier = Modifier.fillMaxSize()) {
                            ocrLines.forEach { line ->
                                val left = imageLeft + line.rect.left * displayW
                                val top = imageTop + line.rect.top * displayH
                                val right = imageLeft + line.rect.right * displayW
                                val bottom = imageTop + line.rect.bottom * displayH
                                val heightPx = (bottom - top).coerceAtLeast(1f)
                                val fontSize = with(density) { (heightPx * 0.9f).toSp() }
                                Text(
                                    text = line.text,
                                    color = Color(0x01000000),
                                    fontSize = fontSize,
                                    modifier = Modifier
                                        .offset { IntOffset(left.toInt(), top.toInt()) }
                                )
                            }
                        }
                    }
                }
                if (highlights.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 2.dp.toPx()
                        highlights.forEach { rect ->
                            val left = imageLeft + rect.left * displayW
                            val top = imageTop + rect.top * displayH
                            val right = imageLeft + rect.right * displayW
                            val bottom = imageTop + rect.bottom * displayH
                            val size = Size(
                                (right - left).coerceAtLeast(1f),
                                (bottom - top).coerceAtLeast(1f)
                            )
                            drawRect(
                                color = Color(0x66FFD54F),
                                topLeft = Offset(left, top),
                                size = size
                            )
                            drawRect(
                                color = Color(0xCCFFB300),
                                topLeft = Offset(left, top),
                                size = size,
                                style = Stroke(width = strokeWidth)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageIndicator(text: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            color = BharatNavy,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                color = BharatWhite,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        Canvas(
            modifier = Modifier
                .width(16.dp)
                .height(8.dp)
        ) {
            val path = Path().apply {
                moveTo(size.width / 2f, size.height)
                lineTo(0f, 0f)
                lineTo(size.width, 0f)
                close()
            }
            drawPath(path, color = BharatNavy)
        }
    }
}

private enum class SignatureTool { PENCIL, PEN, MARKER }

private data class SignatureStroke(
    val path: Path,
    val color: Color,
    val widthPx: Float,
    val alpha: Float
)

@Composable
private fun SignaturePadDialog(
    onDismiss: () -> Unit,
    onSave: (Bitmap) -> Unit,
) {
    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val scope = rememberCoroutineScope()
    val strokes = remember { mutableStateListOf<SignatureStroke>() }
    var drawTick by remember { mutableIntStateOf(0) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var tool by remember { mutableStateOf(SignatureTool.PEN) }
    var strokeWidth by remember { mutableStateOf(6f) }
    var color by remember { mutableStateOf(Color.Black) }
    var bounds by remember { mutableStateOf<RectF?>(null) }

    fun updateBounds(point: Offset) {
        val current = bounds
        val next = if (current == null) {
            RectF(point.x, point.y, point.x, point.y)
        } else {
            RectF(
                min(current.left, point.x),
                min(current.top, point.y),
                max(current.right, point.x),
                max(current.bottom, point.y)
            )
        }
        bounds = next
    }

    fun renderBitmap(): Bitmap? {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return null
        val trim = bounds ?: return null
        val paddingPx = with(density) { 16.dp.toPx() }
        val left = (trim.left - paddingPx).coerceAtLeast(0f)
        val top = (trim.top - paddingPx).coerceAtLeast(0f)
        val right = (trim.right + paddingPx).coerceAtMost(canvasSize.width.toFloat())
        val bottom = (trim.bottom + paddingPx).coerceAtMost(canvasSize.height.toFloat())
        val cropW = max(1, (right - left).roundToInt())
        val cropH = max(1, (bottom - top).roundToInt())
        val bitmap = Bitmap.createBitmap(cropW, cropH, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        canvas.save()
        canvas.translate(-left, -top)
        strokes.forEach { stroke ->
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                style = android.graphics.Paint.Style.STROKE
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
                strokeWidth = stroke.widthPx
                setColor(stroke.color.copy(alpha = stroke.alpha).toArgb())
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
            }
            canvas.drawPath(stroke.path.asAndroidPath(), paint)
        }
        canvas.restore()
        return bitmap
    }

    val toolAlpha = when (tool) {
        SignatureTool.PENCIL -> 0.5f
        SignatureTool.MARKER -> 0.7f
        SignatureTool.PEN -> 1f
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
                        text = stringResource(R.string.signature_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(
                        onClick = {
                            strokes.clear()
                            bounds = null
                            drawTick += 1
                        }
                    ) {
                        Text(stringResource(R.string.clear))
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .pointerInput(tool, strokeWidth, color) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val widthPx = with(density) { strokeWidth.dp.toPx() }
                                    val path = Path().apply { moveTo(offset.x, offset.y) }
                                    strokes.add(
                                        SignatureStroke(
                                            path = path,
                                            color = color,
                                            widthPx = widthPx,
                                            alpha = toolAlpha
                                        )
                                    )
                                    updateBounds(offset)
                                    drawTick += 1
                                },
                                onDrag = { change, _ ->
                                    val current = strokes.lastOrNull() ?: return@detectDragGestures
                                    current.path.lineTo(change.position.x, change.position.y)
                                    updateBounds(change.position)
                                    drawTick += 1
                                    change.consume()
                                }
                            )
                        }
                ) {
                    val newSize = IntSize(constraints.maxWidth, constraints.maxHeight)
                    if (canvasSize != newSize) {
                        canvasSize = newSize
                    }
                    drawTick
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        strokes.forEach { stroke ->
                            drawPath(
                                path = stroke.path,
                                color = stroke.color.copy(alpha = stroke.alpha),
                                style = Stroke(
                                    width = stroke.widthPx,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SignatureToolButton(
                            label = stringResource(R.string.signature_pencil),
                            selected = tool == SignatureTool.PENCIL,
                            onClick = { tool = SignatureTool.PENCIL }
                        )
                        SignatureToolButton(
                            label = stringResource(R.string.signature_pen),
                            selected = tool == SignatureTool.PEN,
                            onClick = { tool = SignatureTool.PEN }
                        )
                        SignatureToolButton(
                            label = stringResource(R.string.signature_marker),
                            selected = tool == SignatureTool.MARKER,
                            onClick = { tool = SignatureTool.MARKER }
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SignatureColorDot(Color.Black, color == Color.Black) { color = Color.Black }
                        SignatureColorDot(Color(0xFF1B5E20), color == Color(0xFF1B5E20)) {
                            color = Color(0xFF1B5E20)
                        }
                        SignatureColorDot(Color(0xFF0D47A1), color == Color(0xFF0D47A1)) {
                            color = Color(0xFF0D47A1)
                        }
                        SignatureColorDot(Color(0xFF4A148C), color == Color(0xFF4A148C)) {
                            color = Color(0xFF4A148C)
                        }
                        SignatureColorDot(Color(0xFFB71C1C), color == Color(0xFFB71C1C)) {
                            color = Color(0xFFB71C1C)
                        }
                    }

                    Column {
                        Text(
                            text = stringResource(R.string.signature_size),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Slider(
                            value = strokeWidth,
                            onValueChange = { strokeWidth = it },
                            valueRange = 2f..18f
                        )
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val bitmap = withContext(Dispatchers.Default) { renderBitmap() }
                                if (bitmap == null || strokes.isEmpty()) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.signature_empty),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    onSave(bitmap)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BharatNavy,
                            contentColor = BharatWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.signature_place))
                    }
                }
            }
        }
    }
}

@Composable
private fun SignatureToolButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) BharatNavy else MaterialTheme.colorScheme.surface,
            contentColor = if (selected) BharatWhite else MaterialTheme.colorScheme.onSurface
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
    ) {
        Text(label)
    }
}

@Composable
private fun SignatureColorDot(color: Color, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(32.dp),
        color = color,
        shape = androidx.compose.foundation.shape.CircleShape,
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, BharatNavy) else null,
        onClick = onClick
    ) {}
}

@Composable
private fun SignaturePlacementDialog(
    pageBitmap: Bitmap?,
    signatureBitmap: Bitmap,
    onCancel: () -> Unit,
    onApply: (RectF) -> Unit,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    var sigScale by remember { mutableStateOf(1f) }
    var sigOffset by remember { mutableStateOf(Offset.Zero) }
    var hasInit by remember { mutableStateOf(false) }
    var imageRectState by remember { mutableStateOf<RectF?>(null) }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.cancel))
                    }
                    Text(
                        text = stringResource(R.string.signature_place_on_page),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(
                        onClick = {
                            val rect = currentSignatureRect(
                                pageBitmap,
                                signatureBitmap,
                                sigOffset,
                                sigScale,
                                imageRectState
                            )
                            if (rect != null) onApply(rect)
                        },
                        enabled = pageBitmap != null && imageRectState != null
                    ) {
                        Text(stringResource(R.string.signature_apply))
                    }
                }

                if (pageBitmap == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                    return@Column
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    val containerW = constraints.maxWidth.toFloat()
                    val containerH = constraints.maxHeight.toFloat()
                    val pageW = pageBitmap.width.toFloat()
                    val pageH = pageBitmap.height.toFloat()
                    val scale = min(containerW / pageW, containerH / pageH)
                    val imageW = pageW * scale
                    val imageH = pageH * scale
                    val imageLeft = (containerW - imageW) / 2f
                    val imageTop = (containerH - imageH) / 2f
                    val imageRect = RectF(imageLeft, imageTop, imageLeft + imageW, imageTop + imageH)
                    imageRectState = imageRect
                    val maxScale = min(imageRect.width() / signatureBitmap.width, imageRect.height() / signatureBitmap.height)
                    val minScale = maxScale * 0.2f

                    LaunchedEffect(imageRect, signatureBitmap) {
                        if (!hasInit) {
                            val targetWidth = imageRect.width() * 0.35f
                            val initialScale = (targetWidth / signatureBitmap.width).coerceIn(minScale, maxScale)
                            val sigW = signatureBitmap.width * initialScale
                            val sigH = signatureBitmap.height * initialScale
                            val x = imageRect.left + (imageRect.width() - sigW) / 2f
                            val y = imageRect.top + (imageRect.height() - sigH) / 2f
                            sigScale = initialScale
                            sigOffset = Offset(x, y)
                            hasInit = true
                        }
                    }

                    val sigW = signatureBitmap.width * sigScale
                    val sigH = signatureBitmap.height * sigScale

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(sigScale, sigOffset, imageRect) {
                                detectDragGestures { change, dragAmount ->
                                    val newW = signatureBitmap.width * sigScale
                                    val newH = signatureBitmap.height * sigScale
                                    val rawOffset = sigOffset + dragAmount
                                    val clampedX = rawOffset.x.coerceIn(
                                        imageRect.left,
                                        imageRect.right - newW
                                    )
                                    val clampedY = rawOffset.y.coerceIn(
                                        imageRect.top,
                                        imageRect.bottom - newH
                                    )
                                    sigOffset = Offset(clampedX, clampedY)
                                    change.consume()
                                }
                            }
                    ) {
                        Image(
                            bitmap = pageBitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val sigW = (signatureBitmap.width * sigScale).roundToInt()
                            val sigH = (signatureBitmap.height * sigScale).roundToInt()
                            drawImage(
                                image = signatureBitmap.asImageBitmap(),
                                srcOffset = androidx.compose.ui.unit.IntOffset.Zero,
                                srcSize = IntSize(signatureBitmap.width, signatureBitmap.height),
                                dstOffset = androidx.compose.ui.unit.IntOffset(
                                    sigOffset.x.roundToInt(),
                                    sigOffset.y.roundToInt()
                                ),
                                dstSize = IntSize(sigW, sigH)
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.signature_size),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Slider(
                            value = sigScale,
                            valueRange = minScale..maxScale,
                            onValueChange = { newScale ->
                                val newW = signatureBitmap.width * newScale
                                val newH = signatureBitmap.height * newScale
                                val clampedX = sigOffset.x.coerceIn(
                                    imageRect.left,
                                    imageRect.right - newW
                                )
                                val clampedY = sigOffset.y.coerceIn(
                                    imageRect.top,
                                    imageRect.bottom - newH
                                )
                                sigScale = newScale
                                sigOffset = Offset(clampedX, clampedY)
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun currentSignatureRect(
    pageBitmap: Bitmap?,
    signatureBitmap: Bitmap,
    sigOffset: Offset,
    sigScale: Float,
    imageRect: RectF?
): RectF? {
    val page = pageBitmap ?: return null
    val rect = imageRect ?: return null
    val scale = rect.width() / page.width.toFloat()
    if (scale <= 0f) return null

    val sigW = signatureBitmap.width * sigScale
    val sigH = signatureBitmap.height * sigScale
    val left = ((sigOffset.x - rect.left) / scale).coerceIn(0f, page.width.toFloat())
    val top = ((sigOffset.y - rect.top) / scale).coerceIn(0f, page.height.toFloat())
    val width = (sigW / scale).coerceAtLeast(1f)
    val height = (sigH / scale).coerceAtLeast(1f)
    val right = (left + width).coerceAtMost(page.width.toFloat())
    val bottom = (top + height).coerceAtMost(page.height.toFloat())
    return RectF(left, top, right, bottom)
}

private suspend fun applySignatureToPdf(
    context: Context,
    uri: Uri,
    pageIndex: Int,
    signature: Bitmap,
    rect: RectF,
    pdfPassword: String? = null
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            ensureBouncyCastleProvider()
            val document = context.contentResolver.openInputStream(uri)?.use { input ->
                if (pdfPassword.isNullOrBlank()) {
                    PDDocument.load(input)
                } else {
                    PDDocument.load(input, pdfPassword)
                }
            } ?: return@withContext false

            document.use { doc ->
                val page = doc.getPage(pageIndex)
                val image = LosslessFactory.createFromImage(doc, signature)
                val pageHeight = page.mediaBox.height
                val pdfX = rect.left
                val pdfY = pageHeight - rect.bottom
                val contentStream = PDPageContentStream(
                    doc,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
                )
                contentStream.drawImage(image, pdfX, pdfY, rect.width(), rect.height())
                contentStream.close()

                if (!pdfPassword.isNullOrBlank()) {
                    val access = AccessPermission()
                    val policy = StandardProtectionPolicy(pdfPassword, pdfPassword, access)
                    policy.encryptionKeyLength = 128
                    doc.protect(policy)
                }

                val output = context.contentResolver.openOutputStream(uri, "rwt")
                    ?: context.contentResolver.openOutputStream(uri, "w")
                    ?: return@withContext false
                output.use { doc.save(it) }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
