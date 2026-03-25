/*
 * Copyright 2025 Ahan Sardar
 */
package org.bharatscan.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.Q
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import org.bharatscan.app.data.FileLogger
import org.bharatscan.app.data.ImageRepository
import org.bharatscan.app.ui.Navigation
import org.bharatscan.app.ui.Screen
import org.bharatscan.app.ui.components.rememberCameraPermissionState
import org.bharatscan.app.ui.screens.DocumentScreen
import org.bharatscan.app.ui.screens.LibrariesScreen
import org.bharatscan.app.ui.screens.PdfViewerScreen
import org.bharatscan.app.ui.screens.about.AboutEvent
import org.bharatscan.app.ui.screens.about.AboutScreen
import org.bharatscan.app.ui.screens.about.AboutViewModel
import org.bharatscan.app.ui.screens.about.createEmailWithImageIntent
import org.bharatscan.app.ui.screens.camera.CameraEvent
import org.bharatscan.app.ui.screens.camera.CameraScreen
import org.bharatscan.app.ui.screens.camera.CameraViewModel
import org.bharatscan.app.ui.screens.export.ExportActions
import org.bharatscan.app.ui.screens.export.ExportEvent
import org.bharatscan.app.ui.screens.export.ExportResult
import org.bharatscan.app.ui.screens.export.ExportScreenWrapper
import org.bharatscan.app.ui.screens.export.ExportViewModel
import org.bharatscan.app.ui.screens.home.HomeScreen
import org.bharatscan.app.ui.screens.home.HomeViewModel
import org.bharatscan.app.ui.screens.home.DocumentsScreen
import org.bharatscan.app.ui.screens.home.FiltersScreen
import org.bharatscan.app.ui.screens.home.SearchScreen
import org.bharatscan.app.ui.screens.splash.SplashScreen
import org.bharatscan.app.ui.screens.settings.ExportFormat
import org.bharatscan.app.ui.screens.settings.SettingsScreen
import org.bharatscan.app.ui.screens.settings.SettingsViewModel
import org.bharatscan.app.ui.screens.settings.SettingsRepository
import org.bharatscan.app.ui.theme.BharatScanTheme
import org.bharatscan.app.update.UpdateInfo
import org.bharatscan.app.update.UpdateManager
import org.bharatscan.app.update.UpdateDownloadStatus
import org.bharatscan.app.update.UpdateUiState
import org.opencv.android.OpenCVLoader
import java.io.File

class MainActivity : AppCompatActivity() {
    private var updateReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        initLibraries()

        val appContainer = (application as BharatScanApp).appContainer
        updateReceiver = UpdateManager.registerDownloadReceiver(this, appContainer.logger)
        val launchMode = resolveLaunchMode(intent)

        val sessionViewModel: SessionViewModel by viewModels {
            SessionViewModelFactory(
                application = application,
                launchMode = launchMode,
                appContainer = appContainer
            )
        }

        val imageRepository = sessionViewModel.imageRepository
        val viewModel: MainViewModel by viewModels {
            appContainer.viewModelFactory {
                MainViewModel(imageRepository, appContainer.imageSegmentationService, launchMode)
            }
        }
        val exportViewModel: ExportViewModel by viewModels {
            appContainer.viewModelFactory {
                ExportViewModel(appContainer, imageRepository)
            }
        }
        val aboutViewModel: AboutViewModel by viewModels {
            appContainer.viewModelFactory {
                AboutViewModel(appContainer, imageRepository)
            }
        }
        val homeViewModel: HomeViewModel by viewModels { appContainer.homeViewModelFactory }
        val cameraViewModel: CameraViewModel by viewModels { appContainer.cameraViewModelFactory }

        val settingsViewModel: SettingsViewModel
            by viewModels { appContainer.settingsViewModelFactory }
        lifecycleScope.launch(Dispatchers.IO) {
            exportViewModel.cleanUpOldPreparedFiles(1000 * 3600)
        }
        
        // Handle incoming intent (e.g., from other apps)
        handleIntent(intent, viewModel)

        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
            val liveAnalysisState by cameraViewModel.liveAnalysisState.collectAsStateWithLifecycle()
            val document by viewModel.documentUiModel.collectAsStateWithLifecycle()
            val exportUiState by exportViewModel.uiState.collectAsStateWithLifecycle()
            val customCategories by exportViewModel.customCategories.collectAsStateWithLifecycle()
            val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            val cameraPermission = rememberCameraPermissionState()
            CollectCameraEvents(cameraViewModel, viewModel)
            CollectExportEvents(context, exportViewModel)
            CollectAboutEvents(context, aboutViewModel, imageRepository)

            val updateScope = rememberCoroutineScope()
            var updateChecking by remember { mutableStateOf(false) }
            var updateMessage by remember { mutableStateOf<String?>(null) }
            var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
            var downloadStatus by remember { mutableStateOf<UpdateDownloadStatus?>(null) }
            var showUpdateDialog by remember { mutableStateOf(false) }

            fun requestUpdateCheck(force: Boolean) {
                if (updateChecking) return
                updateChecking = true
                if (force) {
                    updateMessage = null
                }
                updateScope.launch {
                    val info = withContext(Dispatchers.IO) {
                        UpdateManager.checkForUpdate(context, appContainer.logger, force = force)
                    }
                    updateChecking = false
                    if (info != null) {
                        updateInfo = info
                        showUpdateDialog = true
                    } else if (force) {
                        updateMessage = getString(R.string.latest_version)
                    }
                }
            }

            LaunchedEffect(Unit) {
                while (true) {
                    downloadStatus = withContext(Dispatchers.IO) {
                        UpdateManager.getDownloadStatus(this@MainActivity)
                    }
                    delay(1000)
                }
            }

            LaunchedEffect(settingsUiState.checkUpdatesAtStartup) {
                if (!settingsUiState.checkUpdatesAtStartup) return@LaunchedEffect
                if (updateChecking) return@LaunchedEffect
                updateChecking = true
                val info = withContext(Dispatchers.IO) {
                    UpdateManager.checkForUpdate(context, appContainer.logger)
                }
                updateChecking = false
                if (info != null) {
                    updateInfo = info
                    showUpdateDialog = true
                }
            }

            val updateState = UpdateUiState(
                isChecking = updateChecking,
                statusMessage = updateMessage,
                downloadStatus = downloadStatus,
                updateInfo = updateInfo
            )

            if (showUpdateDialog && updateInfo != null) {
                val info = updateInfo!!
                UpdateDialog(
                    updateInfo = info,
                    onConfirm = {
                        UpdateManager.downloadUpdate(context, info, appContainer.logger)
                        showUpdateDialog = false
                        updateMessage = getString(R.string.downloading_update)
                    },
                    onDismiss = { showUpdateDialog = false }
                )
            }

            BharatScanTheme {
                val navigation = navigation(viewModel, launchMode)
                val onExportClick = if (launchMode == LaunchMode.EXTERNAL_SCAN_TO_PDF) {
                    {
                        lifecycleScope.launch {
                            val result = exportViewModel.generatePdfForExternalCall()
                            sendActivityResult(result)
                            viewModel.startNewDocument()
                            finish()
                        }
                        Unit
                    }
                } else {
                    navigation.toExportScreen
                }

                BackHandler {
                    if (currentScreen is Screen.Main.Home && launchMode == LaunchMode.NORMAL) {
                        // Keep app open on home screen.
                        return@BackHandler
                    }
                    navigation.back()
                }

                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(2500)
                    showSplash = false
                }

                androidx.compose.foundation.layout.Box {
                    when (val screen = currentScreen) {
                        is Screen.Main.Home -> {
                            val recentDocs by homeViewModel.recentDocuments.collectAsStateWithLifecycle()
                            val customCategories by homeViewModel.customCategories.collectAsStateWithLifecycle()
                            HomeScreen(
                                cameraPermission = cameraPermission,
                                currentDocument = document,
                                navigation = navigation,
                                onClearScan = { viewModel.startNewDocument() },
                                recentDocuments = recentDocs,
                                customCategories = customCategories,
                                onOpenPdf = { fileUri -> navigation.toPdfViewer(fileUri) },
                                onDeleteDocument = { doc -> homeViewModel.deleteRecentDocument(doc) },
                                updateState = updateState,
                                onCheckForUpdates = { requestUpdateCheck(true) },
                                onInstallUpdate = { info ->
                                    UpdateManager.downloadUpdate(this@MainActivity, info, appContainer.logger)
                                    updateMessage = getString(R.string.downloading_update)
                                }
                            )
                        }
                        is Screen.Main.Documents -> {
                            val recentDocs by homeViewModel.recentDocuments.collectAsStateWithLifecycle()
                            DocumentsScreen(
                                navigation = navigation,
                                recentDocuments = recentDocs,
                                onOpenPdf = { fileUri -> navigation.toPdfViewer(fileUri) },
                                onDeleteDocument = { doc -> homeViewModel.deleteRecentDocument(doc) }
                            )
                        }
                        is Screen.Main.Filters -> {
                            val recentDocs by homeViewModel.recentDocuments.collectAsStateWithLifecycle()
                            val customCategories by homeViewModel.customCategories.collectAsStateWithLifecycle()
                            FiltersScreen(
                                navigation = navigation,
                                recentDocuments = recentDocs,
                                customCategories = customCategories,
                                initialCategoryId = screen.initialCategoryId,
                                onOpenPdf = { fileUri -> navigation.toPdfViewer(fileUri) },
                                onDeleteDocument = { doc -> homeViewModel.deleteRecentDocument(doc) }
                            )
                        }
                        is Screen.Main.Search -> {
                            val recentDocs by homeViewModel.recentDocuments.collectAsStateWithLifecycle()
                            SearchScreen(
                                navigation = navigation,
                                recentDocuments = recentDocs,
                                onOpenPdf = { fileUri -> navigation.toPdfViewer(fileUri) },
                                onDeleteDocument = { doc -> homeViewModel.deleteRecentDocument(doc) }
                            )
                        }
                        is Screen.Main.PdfViewer -> {
                            PdfViewerScreen(
                                uri = screen.uri,
                                onBack = navigation.back,
                                onShare = { sharePdf(screen.uri) },
                                onPrint = { printPdf(screen.uri) },
                                onEdit = {
                                    viewModel.importPdfForEditing(context, screen.uri) {
                                        viewModel.navigateTo(Screen.Main.Document())
                                    }
                                },
                                onDelete = {
                                    homeViewModel.deleteRecentDocumentByUri(screen.uri)
                                    navigation.back()
                                }
                            )
                        }
                        is Screen.Main.Camera -> {
                            CameraScreen(
                                viewModel,
                                cameraViewModel,
                                navigation,
                                liveAnalysisState,
                                onImageAnalyzed = { image -> cameraViewModel.liveAnalysis(image) },
                                onFinalizePressed = onExportClick,
                                cameraPermission = cameraPermission
                            )
                        }
                        is Screen.Main.Document -> {
                            DocumentScreen (
                                document = document,
                                initialPage = screen.initialPage,
                                navigation = navigation,
                                onExportClick = onExportClick,
                                onDeleteImage =  { id -> viewModel.deletePage(id) },
                                onRotateImage = { id, clockwise -> viewModel.rotateImage(id, clockwise) },
                                onPageReorder = { id, newIndex -> viewModel.movePage(id, newIndex) },
                                onCropImage = { id ->
                                    viewModel.autoCropPage(id) { success ->
                                        if (!success) {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.crop_failed),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                },
                            )
                        }
                        is Screen.Main.Export -> {
                            ExportScreenWrapper(
                                navigation = navigation,
                                uiState = exportUiState,
                                currentDocument = document,
                                customCategories = customCategories,
                                pdfActions = ExportActions(
                                    prepareExportIfNeeded = exportViewModel::prepareExportIfNeeded,
                                    setFilename = exportViewModel::setFilename,
                                    setExportFormat = exportViewModel::setExportFormat,
                                    setExportQuality = exportViewModel::setExportQuality,
                                    setWatermarkEnabled = exportViewModel::setWatermarkEnabled,
                                    setWatermarkText = exportViewModel::setWatermarkText,
                                    setPasswordProtectionEnabled = exportViewModel::setPasswordProtectionEnabled,
                                    setPassword = exportViewModel::setPassword,
                                    share = { exportViewModel.onShareClicked() },
                                    save = { categoryId -> exportViewModel.onSaveClicked(categoryId) },
                                    open = { item -> navigation.toPdfViewer(item.uri) },
                                    registerCustomCategory = exportViewModel::registerCustomCategory
                                ),
                                onCloseScan = {
                                    exportViewModel.resetFilename()
                                    viewModel.startNewDocument()
                                    viewModel.navigateTo(Screen.Main.Home)
                                }
                            )
                        }
                        is Screen.Overlay.About -> {
                            LaunchedEffect(Unit) {
                                aboutViewModel.refreshLastCapturedImageState()
                            }
                            val aboutUiState by aboutViewModel.uiState.collectAsStateWithLifecycle()
                            AboutScreen(
                                aboutUiState = aboutUiState,
                                onBack = navigation.back,
                                onCopyLogs = { aboutViewModel.onCopyLogsClicked() },
                                onSaveLogs = { aboutViewModel.onSaveLogsClicked() },
                                onContactWithLastImageClicked =
                                    { aboutViewModel.onContactWithLastImageClicked() },
                                onViewLibraries = navigation.toLibrariesScreen)
                        }
                        is Screen.Overlay.Libraries -> {
                            LibrariesScreen(onBack = navigation.back)
                        }
                        is Screen.Overlay.Settings -> {
                            SettingsScreenWrapper(
                                settingsViewModel,
                                navigation,
                                appContainer.logger,
                                updateState = updateState,
                                onCheckForUpdates = { requestUpdateCheck(true) },
                                onCheckAtStartupChanged = { enabled ->
                                    settingsViewModel.setCheckUpdatesAtStartup(enabled)
                                }
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showSplash,
                        enter = EnterTransition.None,
                        exit = slideOutHorizontally(
                            animationSpec = tween(350)
                        ) { -it }
                    ) {
                        SplashScreen()
                    }
                }
            }
        }
    }

    private fun initLibraries() {
        OpenCVLoader.initLocal()
        (application as BharatScanApp).appContainer.imageSegmentationService.initialize()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }

    private fun handleIntent(intent: Intent?, viewModel: MainViewModel) {
        if (intent?.action == Intent.ACTION_VIEW && intent.type == "application/pdf") {
            intent.data?.let { uri ->
                viewModel.navigateTo(Screen.Main.PdfViewer(uri))
            }
        }
    }

    private fun resolveLaunchMode(intent: Intent?): LaunchMode {
        return when (intent?.action) {
            "org.bharatscan.app.action.SCAN_TO_PDF" -> LaunchMode.EXTERNAL_SCAN_TO_PDF
            else -> LaunchMode.NORMAL
        }
    }

    @Composable
    private fun SettingsScreenWrapper(
        settingsViewModel: SettingsViewModel,
        nav: Navigation,
        logger: FileLogger,
        updateState: UpdateUiState,
        onCheckForUpdates: () -> Unit,
        onCheckAtStartupChanged: (Boolean) -> Unit,
    ) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            if (uri != null) {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                try {
                    contentResolver.takePersistableUriPermission(uri, flags)
                    settingsViewModel.setExportDirUri(uri.toString())
                } catch (e: Exception) {
                    logger.e("Settings", "Failed to set export dir to $uri", e)
                    val text = getString(R.string.error_file_picker_result)
                    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
                }
            }
        }
        val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) {
            settingsViewModel.refreshExportDirName()
        }

        SettingsScreen(
            settingsUiState,
            onChooseDirectoryClick = {
                try {
                    launcher.launch(null)
                } catch (e: Exception) {
                    val message = getString(R.string.error_file_picker_launch)
                    logger.e("Settings", message, e)
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            },
            onResetExportDirClick = { settingsViewModel.setExportDirUri(null) },
            onExportFormatChanged = { format -> settingsViewModel.setExportFormat(format) },
            onExportQualityChanged = { quality -> settingsViewModel.setExportQuality(quality) },
            onLanguageChanged = { option -> settingsViewModel.setLanguageOption(option) },
            onBack = nav.back,
            navigation = nav,
            onSecurityChanged = { enabled -> settingsViewModel.setRequireAuth(enabled) },
            updateState = updateState,
            onCheckForUpdates = onCheckForUpdates,
            onCheckAtStartupChanged = onCheckAtStartupChanged,
        )
    }

    @Composable
    private fun UpdateDialog(
        updateInfo: UpdateInfo,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.update_available_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(stringResource(R.string.update_available_message, updateInfo.versionName))
                    val notes = updateInfo.releaseNotes?.trim().orEmpty()
                    if (notes.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.update_release_notes_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        ReleaseNotesContent(notes)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(stringResource(R.string.update_install))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.update_later))
                }
            }
        )
    }

    private sealed class ReleaseNoteBlock {
        data class Heading(val level: Int, val text: String) : ReleaseNoteBlock()
        data class Bullet(val text: String) : ReleaseNoteBlock()
        data class Paragraph(val text: String) : ReleaseNoteBlock()
    }

    private fun parseReleaseNotes(raw: String): List<ReleaseNoteBlock> {
        val blocks = mutableListOf<ReleaseNoteBlock>()
        val lines = raw.lines()
        val paragraph = StringBuilder()

        fun flushParagraph() {
            if (paragraph.isNotBlank()) {
                blocks += ReleaseNoteBlock.Paragraph(paragraph.toString().trim())
                paragraph.setLength(0)
            }
        }

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) {
                flushParagraph()
                continue
            }

            val headingMatch = Regex("^(#{1,4})\\s+(.*)$").find(trimmed)
            if (headingMatch != null) {
                flushParagraph()
                val level = headingMatch.groupValues[1].length
                val text = headingMatch.groupValues[2].trim()
                if (text.isNotBlank()) {
                    blocks += ReleaseNoteBlock.Heading(level, text)
                }
                continue
            }

            val bullet = when {
                trimmed.startsWith("- ") -> trimmed.removePrefix("- ").trim()
                trimmed.startsWith("* ") -> trimmed.removePrefix("* ").trim()
                trimmed.startsWith("• ") -> trimmed.removePrefix("• ").trim()
                trimmed.matches(Regex("^\\d+\\.\\s+.*$")) ->
                    trimmed.replaceFirst(Regex("^\\d+\\.\\s+"), "").trim()
                else -> null
            }
            if (bullet != null) {
                flushParagraph()
                if (bullet.isNotBlank()) {
                    blocks += ReleaseNoteBlock.Bullet(bullet)
                }
                continue
            }

            if (paragraph.isNotEmpty()) paragraph.append(' ')
            paragraph.append(trimmed)
        }

        flushParagraph()
        return blocks
    }

    @Composable
    private fun ReleaseNotesContent(notes: String) {
        val blocks = remember(notes) { parseReleaseNotes(notes) }
        Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
            blocks.forEach { block ->
                when (block) {
                    is ReleaseNoteBlock.Heading -> {
                        val style = when (block.level) {
                            1 -> MaterialTheme.typography.titleMedium
                            2 -> MaterialTheme.typography.titleSmall
                            else -> MaterialTheme.typography.labelLarge
                        }
                        Text(
                            text = block.text,
                            style = style,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    is ReleaseNoteBlock.Bullet -> {
                        Text(
                            text = "• ${block.text}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    is ReleaseNoteBlock.Paragraph -> {
                        Text(
                            text = block.text,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun CollectAboutEvents(
        context: Context,
        aboutViewModel: AboutViewModel,
        imageRepository: ImageRepository,
    ) {
        val clipboard = LocalClipboard.current
        val msgCopiedLogs = stringResource(R.string.copied_logs)
        LaunchedEffect(Unit) {
            aboutViewModel.events.collect { event ->
                when (event) {
                    is AboutEvent.CopyLogs -> {
                        clipboard.setClipEntry(
                            ClipData.newPlainText("BharatScan logs", event.logs).toClipEntry()
                        )
                        Toast.makeText(context, msgCopiedLogs, Toast.LENGTH_SHORT).show()
                    }
                    is AboutEvent.SaveLogs -> {
                        shareLogs(event.logs)
                    }
                    is AboutEvent.PrepareEmailWithLastImage -> {
                        val file = imageRepository.lastAddedSourceFile()
                        if (file != null) {
                            startActivity(createEmailWithImageIntent(context, file))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CollectExportEvents(
        context: Context,
        exportViewModel: ExportViewModel,
    ) {
        var pendingCategoryId by remember { mutableStateOf<String?>(null) }
        val storagePermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                exportViewModel.onRequestSave(context, pendingCategoryId)
                pendingCategoryId = null
            } else {
                val message = getString(R.string.storage_permission_denied)
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
        LaunchedEffect(Unit) {
            exportViewModel.events.collect { event ->
                when (event) {
                    is ExportEvent.RequestSave -> {
                        pendingCategoryId = event.categoryId
                        checkPermissionThen(storagePermissionLauncher) {
                            exportViewModel.onRequestSave(context, event.categoryId)
                        }
                    }
                    is ExportEvent.Share -> {
                        share(event.result)
                    }
                }
            }
        }
    }

    @Composable
    private fun CollectCameraEvents(
        cameraViewModel: CameraViewModel,
        viewModel: MainViewModel,
    ) {
        LaunchedEffect(Unit) {
            cameraViewModel.events.collect { event ->
                when (event) {
                    is CameraEvent.ImageCaptured -> viewModel.handleImageCaptured(event.page)
                }
            }
        }
    }

    private fun share(result: ExportResult) {
        if (result.files.isEmpty()) return

        val uris = result.files.map(::uriForFile)
        val intent = Intent().apply {
            action = if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE
            type = result.format.mimeType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            if (uris.size == 1) {
                putExtra(Intent.EXTRA_STREAM, uris[0])
            } else {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            }
        }
        val chooser = Intent.createChooser(intent, getString(R.string.share_document))

        val resolveInfos = packageManager.queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY)
        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            for (uri in uris) {
                grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        startActivity(chooser)
    }

    private fun sharePdf(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_document)))
    }

    private fun printPdf(uri: Uri) {
        try {   
            val fileName = DocumentFile.fromSingleUri(this, uri)?.name
                ?: uri.lastPathSegment
                ?: getString(R.string.document)
            val printManager = getSystemService(PRINT_SERVICE) as PrintManager
            val adapter = PdfDocumentAdapter(this, uri, fileName)
            printManager.print(
                fileName,
                adapter,
                PrintAttributes.Builder().build()
            )
        } catch (_: Exception) {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), getString(R.string.open_file)))
        }
    }

    private class PdfDocumentAdapter(
        private val context: Context,
        private val uri: Uri,
        private val fileName: String
    ) : PrintDocumentAdapter() {

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes?,
            cancellationSignal: android.os.CancellationSignal?,
            callback: LayoutResultCallback?,
            extras: android.os.Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback?.onLayoutCancelled()
                return
            }
            val info = PrintDocumentInfo.Builder(fileName)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                .build()
            callback?.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<android.print.PageRange>,
            destination: android.os.ParcelFileDescriptor,
            cancellationSignal: android.os.CancellationSignal,
            callback: WriteResultCallback
        ) {
            try {
                if (cancellationSignal.isCanceled) {
                    callback.onWriteCancelled()
                    return
                }
                context.contentResolver.openInputStream(uri)?.use { input ->
                    java.io.FileOutputStream(destination.fileDescriptor).use { output ->
                        input.copyTo(output)
                    }
                }
                callback.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            }
        }
    }

    private fun signPdf(uri: Uri) {
        val intent = Intent(Intent.ACTION_EDIT).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(fallback, getString(R.string.open_file)))
        }
    }

    private fun shareLogs(logs: String) {
        val cacheDir = File(cacheDir, "pdfs").apply { mkdirs() }
        val file = File(cacheDir, "bharatscan_logs.txt")
        file.writeText(logs)
        val uri = uriForFile(file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_document)))
    }

    private fun sendActivityResult(result: ExportResult?) {
        val pdf = result as? ExportResult.Pdf ?: return

        val uri = uriForFile(pdf.file)
        val resultIntent = Intent().apply {
            data = uri
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        setResult(RESULT_OK, resultIntent)
    }

    private fun uriForFile(file: File): Uri {
        return org.bharatscan.app.ui.uriForFile(this, file)
    }

    private fun checkPermissionThen(
        requestPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
        action: () -> Unit
    ) {
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (SDK_INT < Q && checkSelfPermission(this, permission) != PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(permission)
        } else {
            action()
        }
    }

    private fun navigation(viewModel: MainViewModel, launchMode: LaunchMode): Navigation = Navigation(
        toHomeScreen = { viewModel.navigateTo(Screen.Main.Home) },
        toDocumentsScreen = { viewModel.navigateTo(Screen.Main.Documents) },
        toFiltersScreen = { categoryId -> viewModel.navigateTo(Screen.Main.Filters(categoryId)) },
        toSearchScreen = { viewModel.navigateTo(Screen.Main.Search) },
        toCameraScreen = { viewModel.navigateTo(Screen.Main.Camera) },
        toDocumentScreen = { viewModel.navigateTo(Screen.Main.Document()) },
        toExportScreen = { viewModel.navigateTo(Screen.Main.Export) },
        toPdfViewer = { uri -> openPdfWithSecurity(uri, viewModel, (application as BharatScanApp).appContainer.settingsRepository) },
        toAboutScreen = { viewModel.navigateTo(Screen.Overlay.About) },
        toLibrariesScreen = { viewModel.navigateTo(Screen.Overlay.Libraries) },
        toSettingsScreen = if (launchMode == LaunchMode.EXTERNAL_SCAN_TO_PDF) null else {
            {
                viewModel.navigateTo(Screen.Overlay.Settings)
            }
        },
        back = {
            val origin = viewModel.currentScreen.value
            viewModel.navigateBack()
            val destination = viewModel.currentScreen.value
            if (destination == origin && launchMode == LaunchMode.EXTERNAL_SCAN_TO_PDF) {
                setResult(RESULT_CANCELED)
                finish()
            }
        }
    )

    private fun openPdfWithSecurity(
        uri: Uri,
        viewModel: MainViewModel,
        settingsRepository: SettingsRepository,
    ) {
        lifecycleScope.launch {
            val requireAuth = settingsRepository.requireAuth.first()
            if (!requireAuth) {
                viewModel.navigateTo(Screen.Main.PdfViewer(uri))
                return@launch
            }

            val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            val biometricManager = BiometricManager.from(this@MainActivity)
            if (biometricManager.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.security_unavailable),
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.navigateTo(Screen.Main.PdfViewer(uri))
                return@launch
            }

            val executor = ContextCompat.getMainExecutor(this@MainActivity)
            val prompt = BiometricPrompt(
                this@MainActivity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        viewModel.navigateTo(Screen.Main.PdfViewer(uri))
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.security_unlock_title))
                .setSubtitle(getString(R.string.security_unlock_subtitle))
                .setAllowedAuthenticators(authenticators)
                .build()

            prompt.authenticate(promptInfo)
        }
    }

    override fun onDestroy() {
        UpdateManager.unregisterDownloadReceiver(this, updateReceiver)
        updateReceiver = null
        super.onDestroy()
    }
}
