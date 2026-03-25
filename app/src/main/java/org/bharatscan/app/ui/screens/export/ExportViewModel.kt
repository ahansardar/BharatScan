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

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.bharatscan.app.ocr.TextRecognitionHelper
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bharatscan.app.AppContainer
import org.bharatscan.app.R
import org.bharatscan.app.RecentDocument
import org.bharatscan.app.data.FileManager
import org.bharatscan.app.data.ImageRepository
import org.bharatscan.app.domain.ExportQuality
import org.bharatscan.app.domain.OcrLine
import org.bharatscan.app.domain.OcrPage
import org.bharatscan.app.domain.PageViewKey
import org.bharatscan.app.domain.jpegsForExport
import org.bharatscan.app.ui.screens.settings.ExportFormat
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

sealed interface ExportEvent {
    data class RequestSave(val categoryId: String?) : ExportEvent
    data class Share(val result: ExportResult) : ExportEvent
}

class ExportViewModel(container: AppContainer, val imageRepository: ImageRepository): ViewModel() {

    private val appContext = container.appContext
    private val preparationDir = container.preparationDir
    private val fileManager = container.fileManager
    private val settingsRepository = container.settingsRepository
    private val recentDocumentsDataStore = container.recentDocumentsDataStore
    private val logger = container.logger

    private val _events = MutableSharedFlow<ExportEvent>()
    val events = _events.asSharedFlow()

    private suspend fun generatePdf(
        exportQuality: ExportQuality
    ): ExportResult.Pdf = withContext(Dispatchers.IO) {
        val jpegs = jpegsForExport(imageRepository, exportQuality).toList()
        val ocrPages = buildOcrPages(jpegs)
        val password = if (uiState.value.passwordProtectionEnabled) {
            uiState.value.password.trim().ifBlank { null }
        } else {
            null
        }
        val watermark = if (uiState.value.includeWatermark) {
            uiState.value.watermarkText.trim().ifBlank { "BharatScan" }
        } else {
            null
        }
        val pdf = fileManager.generatePdf(jpegs.asSequence(), ocrPages, watermark, password)
        return@withContext ExportResult.Pdf(pdf.file, pdf.sizeInBytes, pdf.pageCount)
    }

    suspend fun generatePdfForExternalCall(): ExportResult.Pdf {
        return generatePdf(ExportQuality.BALANCED)
    }

    private suspend fun buildOcrPages(jpegs: List<ByteArray>): List<OcrPage> {
        return withContext(Dispatchers.Default) {
            val engine = TextRecognitionHelper.createEngine(appContext)
            try {
                jpegs.map { bytes ->
                    val options = BitmapFactory.Options().apply {
                        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                        inMutable = false
                    }
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    if (bitmap == null) {
                        return@map OcrPage(0, 0, emptyList())
                    }
                    val page = try {
                        val lines = engine.recognize(bitmap).map { line ->
                            OcrLine(
                                text = line.text,
                                left = line.bounds.left,
                                top = line.bounds.top,
                                right = line.bounds.right,
                                bottom = line.bounds.bottom
                            )
                        }
                        OcrPage(bitmap.width, bitmap.height, lines)
                    } catch (_: Exception) {
                        OcrPage(bitmap.width, bitmap.height, emptyList())
                    } finally {
                        bitmap.recycle()
                    }
                    page
                }
            } finally {
                engine.close()
            }
        }
    }

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    val customCategories: StateFlow<List<String>> =
        settingsRepository.customCategories
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    private var lastPreparationKey: ExportPreparationKey? = null
    private var preparationJob: Job? = null

    fun setFilename(name: String) {
        _uiState.update {
            it.copy(filename = name)
        }
    }

    fun setExportFormat(format: ExportFormat) {
        viewModelScope.launch {
            settingsRepository.setExportFormat(format)
            if (format != ExportFormat.PDF) {
                _uiState.update {
                    it.copy(
                        passwordProtectionEnabled = false,
                        password = "",
                        passwordErrorRes = null
                    )
                }
            }
            prepareExportIfNeeded()
        }
    }

    fun setExportQuality(quality: ExportQuality) {
        viewModelScope.launch {
            settingsRepository.setExportQuality(quality)
            prepareExportIfNeeded()
        }
    }

    fun setWatermarkEnabled(enabled: Boolean) {
        _uiState.update { it.copy(includeWatermark = enabled) }
        prepareExportIfNeeded()
    }

    fun setWatermarkText(text: String) {
        _uiState.update { it.copy(watermarkText = text) }
        prepareExportIfNeeded()
    }

    fun setPasswordProtectionEnabled(enabled: Boolean) {
        _uiState.update {
            it.copy(
                passwordProtectionEnabled = enabled,
                passwordErrorRes = if (!enabled) null else it.passwordErrorRes
            )
        }
        prepareExportIfNeeded()
    }

    fun setPassword(text: String) {
        _uiState.update {
            it.copy(password = text, passwordErrorRes = null)
        }
        prepareExportIfNeeded()
    }

    fun resetFilename() {
        _uiState.update {
            it.copy(filename = "")
        }
    }

    private fun defaultFilename(): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH.mm.ss", Locale.getDefault()).format(Date())
        return "Scan $timestamp"
    }

    private fun ensureValidFilename() {
        _uiState.update {
            val normalized = it.filename.trim().ifEmpty { defaultFilename() }
            if (normalized != it.filename) {
                it.copy(filename = normalized)
            } else it
        }
    }

    private fun currentPageKeys(): ImmutableList<PageViewKey> =
        imageRepository.pages().map {
            PageViewKey(it.id, it.manualRotation)
        }.toImmutableList()

    fun prepareExportIfNeeded() {
        ensureValidFilename()

        viewModelScope.launch {
            val exportQuality = settingsRepository.exportQuality.first()
            val exportFormat = settingsRepository.exportFormat.first()

            val key = ExportPreparationKey(
                currentPageKeys(),
                exportFormat,
                exportQuality,
                uiState.value.includeWatermark,
                uiState.value.watermarkText.trim(),
                uiState.value.passwordProtectionEnabled,
                uiState.value.password.trim()
            )
            if (key == lastPreparationKey) {
                return@launch
            }

            lastPreparationKey = key
            preparationJob?.cancel()

            preparationJob = launch {
                _uiState.update {
                    ExportUiState(
                        filename = it.filename,
                        format = exportFormat,
                        quality = exportQuality,
                        includeWatermark = it.includeWatermark,
                        watermarkText = it.watermarkText,
                        passwordProtectionEnabled = it.passwordProtectionEnabled,
                        password = it.password,
                        passwordErrorRes = it.passwordErrorRes,
                        isGenerating = true
                    )
                }
                try {
                    val t1 = System.currentTimeMillis()
                    val result = if (exportFormat == ExportFormat.JPEG) {
                        generateJpegs(exportQuality)
                    } else {
                        generatePdf(exportQuality)
                    }
                    _uiState.update { it.copy(result = result) }
                    val t2 = System.currentTimeMillis()
                    val pageCount = result.pageCount
                    Log.i("Export", "Generation: $pageCount pages, $exportQuality, ${t2 - t1} ms")
                } catch (e: CancellationException) {
                    // Preparation cancelled: do nothing
                    throw e
                } catch (e: Exception) {
                    val message = "Failed to prepare $exportFormat export"
                    logger.e("BharatScan", message, e)
                    _uiState.update {
                        it.copy(error = ExportError.OnPrepareOrShare(message, e))
                    }
                } finally {
                    _uiState.update { it.copy(isGenerating = false) }
                }
            }
        }
    }

    private suspend fun generateJpegs(
        exportQuality: ExportQuality
    ): ExportResult.Jpeg = withContext(Dispatchers.IO) {
        val jpegs = jpegsForExport(imageRepository, exportQuality)
        val timestamp = System.currentTimeMillis()
        preparationDir.mkdirs()
        val files = jpegs.mapIndexed { index, bytes ->
            val file = File(preparationDir, "$timestamp-${index + 1}.jpg")
            file.writeBytes(bytes)
            file
        }.toList()
        val sizeInBytes = files.sumOf { it.length() }
        ExportResult.Jpeg(files, sizeInBytes)
    }

    private fun renameFile(source: File, target: File) {
        if (source.absolutePath == target.absolutePath) return
        if (target.exists() && !target.delete()) {
            throw IOException("Cannot delete existing file ${target.absolutePath}")
        }
        if (!source.renameTo(target)) {
            throw IOException("Failed to rename ${source.name} to ${target.name}")
        }
    }

    private fun applyRenaming(): ExportResult {
        val result = _uiState.value.result
            ?: throw IllegalStateException("Export result missing")
        ensureValidFilename()
        val filename = _uiState.value.filename
        val updated = when (result) {
            is ExportResult.Pdf -> {
                val fileName = FileManager.addPdfExtensionIfMissing(filename)
                val newFile = File(result.file.parentFile, fileName)
                renameFile(result.file, newFile)
                ExportResult.Pdf(newFile, result.sizeInBytes, result.pageCount)
            }
            is ExportResult.Jpeg -> {
                val base = filename.removeSuffix(".jpg")
                val files = result.files
                val renamedFiles = files.mapIndexed { index, file ->
                    val indexSuffix = if (files.size == 1) "" else "_${index + 1}"
                    val newFile = File(file.parentFile, "${base}${indexSuffix}.jpg")
                    renameFile(file, newFile)
                    newFile
                }
                result.copy(jpegFiles = renamedFiles)
            }
        }
        _uiState.update { it.copy(result = updated) }
        return updated
    }

    fun onShareClicked() {
        viewModelScope.launch {
            try {
                if (!validatePasswordOrFlag()) return@launch
                val result = applyRenaming()
                _events.emit(ExportEvent.Share(result))
                _uiState.update { it.copy(hasShared = true) }
            } catch (e: Exception) {
                val message = "Failed to prepare share"
                logger.e("BharatScan", message, e)
                _uiState.update { it.copy(error = ExportError.OnPrepareOrShare(message, e)) }
            }
        }
    }

    fun onSaveClicked(categoryId: String?) {
        viewModelScope.launch {
            _events.emit(ExportEvent.RequestSave(categoryId))
        }
    }

    fun onRequestSave(context: Context, categoryId: String?) {
        viewModelScope.launch {
            if (!validatePasswordOrFlag()) return@launch
            _uiState.update {it.copy(isSaving = true, error = null, savedBundle = null) }
            val exportFormat = uiState.value.format
            val saveDir = saveDir(context)
            try {
                // Must not run on the main thread: some SAF providers (e.g. Nextcloud)
                // may perform network I/O
                withContext(Dispatchers.IO) {
                    save(context, saveDir, exportFormat, categoryId)
                }
            } catch (e: MissingExportDirPermissionException) {
                logger.e("BharatScan", "Missing export dir permission", e)
                _uiState.update {
                    it.copy(error =
                        ExportError.OnSave(R.string.error_export_dir_permission_lost, saveDir))
                }
            } catch (e: Exception) {
                logger.e("BharatScan", "Failed to save PDF", e)
                _uiState.update {
                    it.copy(error = ExportError.OnSave(R.string.error_save, saveDir, e))
                }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun validatePasswordOrFlag(): Boolean {
        if (!uiState.value.passwordProtectionEnabled) return true
        val password = uiState.value.password.trim()
        return if (password.isNotEmpty()) {
            true
        } else {
            _uiState.update { it.copy(passwordErrorRes = R.string.password_required) }
            false
        }
    }

    private suspend fun saveDir(context:Context): SaveDir? {
        val uri = settingsRepository.exportDirUri.first()?.toUri() ?: return null
        val name = resolveExportDirName(context, uri)
        return SaveDir(uri, name)
    }

    private suspend fun save(
        context: Context,
        saveDir: SaveDir?,
        exportFormat: ExportFormat,
        categoryId: String?
    ) {
        val result = applyRenaming()
        val savedItems = mutableListOf<SavedItem>()
        val filesForMediaScan = mutableListOf<File>()

        for (file in result.files) {
            val saved = if (saveDir == null) {
                // No export dir defined -> save to Downloads
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+: use MediaStore API
                    val uri = saveViaMediaStore(context, file, exportFormat)
                    SavedItem(uri, file.name, exportFormat)
                } else {
                    // Android 8 and 9: use File API
                    // (MediaStore doesn't allow to choose Downloads for Android<10)
                    val out = fileManager.copyToExternalDir(file)
                    filesForMediaScan.add(out)
                    SavedItem(out.toUri(), out.name, exportFormat)
                }
            } else {
                // Use Storage Access Framework to save to the chosen directory
                if (!context.contentResolver.persistedUriPermissions.any { perm ->
                        perm.uri == saveDir.uri && perm.isWritePermission
                    }) {
                    throw MissingExportDirPermissionException(saveDir.uri)
                }
                val safFile = saveViaSaf(context, file, saveDir.uri, exportFormat)
                SavedItem(safFile.uri, safFile.name ?: file.name, exportFormat)
            }
            savedItems += saved
        }

        val bundle = SavedBundle(savedItems, saveDir)
        _uiState.update { it.copy(savedBundle = bundle) }

        if (exportFormat == ExportFormat.PDF) {
            savedItems.forEach { item ->
                addRecentDocument(item.uri, item.fileName, result.pageCount, categoryId)
            }
        }

        filesForMediaScan.forEach { f -> mediaScan(context, f, exportFormat.mimeType) }
    }

    private suspend fun mediaScan(
        context: Context,
        file: File,
        mimeType: String
    ): Uri? = suspendCoroutine { cont ->
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf(mimeType)
        ) { _, uri ->
            cont.resume(uri)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveViaMediaStore(
        context: Context,
        source: File,
        format: ExportFormat
    ): Uri {
        val resolver = context.contentResolver

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, source.name)
            put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val uri = resolver.insert(collection, values)
            ?: throw IOException("Failed to create MediaStore entry")

        resolver.openOutputStream(uri)?.use { out ->
            source.inputStream().use { input ->
                input.copyTo(out)
            }
        } ?: throw IOException("Failed to open output stream")

        return uri
    }

    private fun saveViaSaf(
        context: Context,
        source: File,
        exportDirUri: Uri,
        exportFormat: ExportFormat,
    ): DocumentFile {
        val resolver = context.contentResolver

        val tree = DocumentFile.fromTreeUri(context, exportDirUri)
            ?: throw IllegalStateException("Invalid SAF directory")

        // Name collisions are handled automatically by SAF provider
        val target = tree.createFile(exportFormat.mimeType, source.name)
            ?: throw IllegalStateException("Unable to create SAF file")

        resolver.openOutputStream(target.uri)?.use { output ->
            FileInputStream(source).use { input ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Failed to open SAF output stream")

        return target
    }

    fun cleanUpOldPreparedFiles(thresholdInMillis: Int) {
        fileManager.cleanUpOldFiles(thresholdInMillis)
    }

    private fun resolveExportDirName(context: Context, exportDirUri: Uri?): String? {
        return if (exportDirUri == null) {
            null
        } else {
            DocumentFile.fromTreeUri(context, exportDirUri)?.name
        }
    }

    fun addRecentDocument(
        fileUri: Uri,
        fileName: String,
        pageCount: Int,
        categoryId: String?
    ) {
        viewModelScope.launch {
            recentDocumentsDataStore.updateData { current ->
                val builder = RecentDocument.newBuilder()
                    .setFileUri(fileUri.toString())
                    .setFileName(fileName)
                    .setPageCount(pageCount)
                    .setCreatedAt(System.currentTimeMillis())
                if (!categoryId.isNullOrBlank()) {
                    builder.setCategory(categoryId)
                }
                val newDoc = builder.build()
                current.toBuilder()
                    .addDocuments(0, newDoc)
                    .build()
            }
        }
    }

    fun registerCustomCategory(name: String) {
        viewModelScope.launch {
            settingsRepository.addCustomCategory(name)
        }
    }
}

data class ExportPreparationKey(
    val pages: ImmutableList<PageViewKey>,
    val format: ExportFormat,
    val quality: ExportQuality,
    val includeWatermark: Boolean,
    val watermarkText: String,
    val passwordProtectionEnabled: Boolean,
    val password: String
)

sealed class ExportResult {
    abstract val files: List<File>
    abstract val sizeInBytes: Long
    abstract val pageCount: Int
    abstract val format: ExportFormat

    data class Pdf(
        val file: File,
        override val sizeInBytes: Long,
        override val pageCount: Int,
    ) : ExportResult() {
        override val files get() = listOf(file)
        override val format: ExportFormat = ExportFormat.PDF
    }

    data class Jpeg(
        val jpegFiles: List<File>,
        override val sizeInBytes: Long,
    ) : ExportResult() {
        override val files get() = jpegFiles
        override val pageCount get() = jpegFiles.size
        override val format: ExportFormat = ExportFormat.JPEG
    }
}

data class ExportActions(
    val prepareExportIfNeeded: () -> Unit,
    val setFilename: (String) -> Unit,
    val setExportFormat: (ExportFormat) -> Unit,
    val setExportQuality: (ExportQuality) -> Unit,
    val setWatermarkEnabled: (Boolean) -> Unit,
    val setWatermarkText: (String) -> Unit,
    val setPasswordProtectionEnabled: (Boolean) -> Unit,
    val setPassword: (String) -> Unit,
    val share: () -> Unit,
    val save: (String?) -> Unit,
    val open: (SavedItem) -> Unit,
    val registerCustomCategory: (String) -> Unit,
)

class MissingExportDirPermissionException(
    val uri: Uri
) : IllegalStateException(
    "Missing persisted write permission for export dir: $uri"
)


