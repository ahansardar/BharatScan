package org.bharatscan.app.ui.screens

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bharatscan.app.BuildConfig
import org.bharatscan.app.R
import org.bharatscan.app.platform.ExcelToPdfConverter
import org.bharatscan.app.ui.uriForFile
import java.io.File

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ExcelViewerScreen(
    uri: Uri,
    onBack: () -> Unit,
    onOpenPdf: (Uri) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val displayName = remember(uri) {
        kotlin.runCatching { DocumentFile.fromSingleUri(context, uri)?.name }.getOrNull()
            ?: uri.lastPathSegment
            ?: context.getString(R.string.document)
    }

    var convertingPreview by remember { mutableStateOf(true) }
    var previewPdfUri by remember { mutableStateOf<Uri?>(null) }
    var lastError by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destUri ->
        if (destUri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = exportExcelToPdf(context, uri, destUri)
            Toast.makeText(
                context,
                if (ok) context.getString(R.string.excel_export_done) else context.getString(R.string.excel_export_failed),
                Toast.LENGTH_SHORT
            ).show()
            if (ok) {
                // Let the user open the exported PDF.
                onOpenPdf(destUri)
            }
        }
    }

    LaunchedEffect(uri) {
        convertingPreview = true
        previewPdfUri = null
        lastError = null

        val file = withContext(Dispatchers.IO) {
            runCatching {
                val pdfDir = File(context.cacheDir, "pdfs").apply { mkdirs() }
                val outFile = File.createTempFile("excel_preview_", ".pdf", pdfDir)
                outFile.outputStream().use { out ->
                    val ok = ExcelToPdfConverter.convert(context, uri, out)
                    if (!ok) throw IllegalStateException("convert returned false")
                }
                outFile
            }.getOrNull()
        }

        if (file == null) {
            convertingPreview = false
            lastError = context.getString(R.string.excel_preview_failed)
        } else {
            previewPdfUri = uriForFile(context, file)
            convertingPreview = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = displayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = { BackPressIcon(onBack) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                convertingPreview -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = stringResource(R.string.excel_converting))
                    }
                }
                previewPdfUri != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.excel_preview_ready),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Button(
                            onClick = { onOpenPdf(previewPdfUri!!) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(R.string.excel_open_preview))
                        }
                        Button(
                            onClick = {
                                val suggested = "BharatScan ${displayName.removeSuffix(".xlsx").removeSuffix(".xls")}.pdf"
                                exportLauncher.launch(suggested)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(R.string.excel_export_pdf))
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = lastError ?: stringResource(R.string.excel_preview_failed))
                        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                            Text(text = stringResource(R.string.back))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackPressIcon(onBack: () -> Unit) {
    // Reuse existing BackButton component if available; fallback to empty icon-less.
    org.bharatscan.app.ui.components.BackButton(onClick = onBack)
}

private suspend fun exportExcelToPdf(context: Context, sourceUri: Uri, destUri: Uri): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val output = context.contentResolver.openOutputStream(destUri, "rwt")
                ?: context.contentResolver.openOutputStream(destUri, "w")
                ?: return@withContext false
            output.use { out ->
                ExcelToPdfConverter.convert(context, sourceUri, out)
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e("ExcelViewer", "Failed exporting excel to pdf destUri=$destUri", e)
            }
            false
        }
    }
}
