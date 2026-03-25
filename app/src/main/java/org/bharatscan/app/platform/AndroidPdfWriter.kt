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
package org.bharatscan.app.platform

import androidx.appcompat.app.AppCompatDelegate
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream.AppendMode
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import com.tom_roush.pdfbox.pdmodel.graphics.state.RenderingMode
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import org.bharatscan.app.BuildConfig
import org.bharatscan.app.data.PdfWriter
import org.bharatscan.app.domain.OcrPage
import java.io.OutputStream
import java.util.Calendar
import java.util.Locale

class AndroidPdfWriter : PdfWriter {
    override fun writePdfFromJpegs(
        jpegs: Sequence<ByteArray>,
        ocrPages: List<OcrPage>?,
        watermarkText: String?,
        password: String?,
        outputStream: OutputStream
    ): Int {
        val doc = PDDocument()
        doc.documentInformation.creationDate = Calendar.getInstance()
        doc.documentInformation.creator = "BharatScan ${BuildConfig.VERSION_NAME}"
        doc.use { document ->
            val font = loadOcrFont(document)
            val pages = jpegs.toList()
            pages.forEachIndexed { index, jpegBytes ->
                val image = JPEGFactory.createFromByteArray(document, jpegBytes)
                val page = PDPage(PDRectangle(image.width.toFloat(), image.height.toFloat()))
                document.addPage(page)
                val contentStream = PDPageContentStream(document, page, AppendMode.OVERWRITE, false)
                contentStream.drawImage(image, 0f, 0f)
                contentStream.close()

                if (!watermarkText.isNullOrBlank() && font != null) {
                    val watermark = watermarkText.trim()
                    val watermarkStream = PDPageContentStream(
                        document,
                        page,
                        AppendMode.APPEND,
                        true,
                        true
                    )
                    val gs = PDExtendedGraphicsState()
                    gs.nonStrokingAlphaConstant = 0.14f
                    gs.strokingAlphaConstant = 0.14f
                    watermarkStream.setGraphicsStateParameters(gs)
                    watermarkStream.setNonStrokingColor(140, 140, 140)
                    val fontSize = (minOf(page.mediaBox.width, page.mediaBox.height) / 6f).coerceAtLeast(18f)
                    val textWidth = font.getStringWidth(watermark) / 1000f * fontSize
                    val x = ((page.mediaBox.width - textWidth) / 2f).coerceAtLeast(16f)
                    val y = (page.mediaBox.height / 2f).coerceAtLeast(16f)
                    try {
                        watermarkStream.beginText()
                        watermarkStream.setFont(font, fontSize)
                        watermarkStream.newLineAtOffset(x, y)
                        watermarkStream.showText(watermark)
                        watermarkStream.endText()
                    } catch (_: Exception) {
                        // Skip watermark if font encoding fails.
                    } finally {
                        watermarkStream.close()
                    }
                }

                val ocrPage = ocrPages?.getOrNull(index)
                if (ocrPage != null && ocrPage.lines.isNotEmpty() && font != null) {
                    val textStream = PDPageContentStream(
                        document,
                        page,
                        AppendMode.APPEND,
                        true,
                        true
                    )
                    textStream.setRenderingMode(RenderingMode.NEITHER)
                    ocrPage.lines.forEach { line ->
                        val sanitized = line.text.trim()
                        if (sanitized.isBlank()) return@forEach
                        val fontSize = (line.bottom - line.top).coerceAtLeast(6f)
                        val x = line.left
                        val y = page.mediaBox.height - line.bottom
                        try {
                            textStream.beginText()
                            textStream.setFont(font, fontSize)
                            textStream.newLineAtOffset(x, y)
                            textStream.showText(sanitized)
                            textStream.endText()
                        } catch (_: Exception) {
                            // Skip lines that cannot be encoded by the current font.
                        }
                    }
                    textStream.close()
                }
            }
            if (!password.isNullOrBlank()) {
                val ownerPassword = java.util.UUID.randomUUID().toString()
                val access = AccessPermission()
                val policy = StandardProtectionPolicy(ownerPassword, password, access).apply {
                    encryptionKeyLength = 128
                }
                document.protect(policy)
            }
            // TODO So the whole document is in memory before this line...
            document.save(outputStream)
        }
        return doc.numberOfPages
    }

    private fun loadOcrFont(document: PDDocument): PDFont? {
        val language = resolveAppLanguage()
        val scriptCandidates = when (language) {
            "hi", "mr" -> listOf(
                "/system/fonts/NotoSansDevanagari-Regular.ttf",
                "/system/fonts/NotoSansDevanagariUI-Regular.ttf",
                "/system/fonts/NotoSerifDevanagari-Regular.ttf"
            )
            "bn" -> listOf(
                "/system/fonts/NotoSansBengali-Regular.ttf",
                "/system/fonts/NotoSansBengaliUI-Regular.ttf",
                "/system/fonts/NotoSerifBengali-Regular.ttf"
            )
            "te" -> listOf(
                "/system/fonts/NotoSansTelugu-Regular.ttf",
                "/system/fonts/NotoSansTeluguUI-Regular.ttf",
                "/system/fonts/NotoSerifTelugu-Regular.ttf"
            )
            "ta" -> listOf(
                "/system/fonts/NotoSansTamil-Regular.ttf",
                "/system/fonts/NotoSansTamilUI-Regular.ttf",
                "/system/fonts/NotoSerifTamil-Regular.ttf"
            )
            "ur" -> listOf(
                "/system/fonts/NotoNaskhArabic-Regular.ttf",
                "/system/fonts/NotoSansArabic-Regular.ttf"
            )
            "gu" -> listOf(
                "/system/fonts/NotoSansGujarati-Regular.ttf",
                "/system/fonts/NotoSansGujaratiUI-Regular.ttf",
                "/system/fonts/NotoSerifGujarati-Regular.ttf"
            )
            "kn" -> listOf(
                "/system/fonts/NotoSansKannada-Regular.ttf",
                "/system/fonts/NotoSansKannadaUI-Regular.ttf",
                "/system/fonts/NotoSerifKannada-Regular.ttf"
            )
            "ml" -> listOf(
                "/system/fonts/NotoSansMalayalam-Regular.ttf",
                "/system/fonts/NotoSansMalayalamUI-Regular.ttf",
                "/system/fonts/NotoSerifMalayalam-Regular.ttf"
            )
            "or", "od" -> listOf(
                "/system/fonts/NotoSansOriya-Regular.ttf",
                "/system/fonts/NotoSansOriyaUI-Regular.ttf",
                "/system/fonts/NotoSerifOriya-Regular.ttf"
            )
            "pa" -> listOf(
                "/system/fonts/NotoSansGurmukhi-Regular.ttf",
                "/system/fonts/NotoSansGurmukhiUI-Regular.ttf",
                "/system/fonts/NotoSerifGurmukhi-Regular.ttf"
            )
            else -> emptyList()
        }
        val candidates = scriptCandidates + listOf(
            "/system/fonts/NotoSans-Regular.ttf",
            "/system/fonts/Roboto-Regular.ttf",
            "/system/fonts/DroidSans.ttf"
        )
        for (path in candidates) {
            val file = java.io.File(path)
            if (file.exists()) {
                return try {
                    PDType0Font.load(document, file)
                } catch (_: Exception) {
                    null
                }
            }
        }
        return try {
            PDType1Font.HELVETICA
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveAppLanguage(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        val locale = if (!locales.isEmpty) locales[0] else Locale.getDefault()
        return (locale?.language ?: Locale.getDefault().language).lowercase(Locale.ROOT)
    }
}


