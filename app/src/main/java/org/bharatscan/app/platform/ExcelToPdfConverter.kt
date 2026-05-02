package org.bharatscan.app.platform

import android.content.Context
import android.net.Uri
import android.util.Log
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import org.apache.poi.poifs.filesystem.FileMagic
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.bharatscan.app.BuildConfig
import java.io.File
import java.io.OutputStream
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object ExcelToPdfConverter {
    private const val TAG = "ExcelToPdf"

    suspend fun convert(
        context: Context,
        sourceUri: Uri,
        output: OutputStream,
        pageSize: PDRectangle = PDRectangle.A4,
    ): Boolean {
        val cacheDir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val ext = when {
            sourceUri.lastPathSegment?.lowercase(Locale.ROOT)?.endsWith(".xls") == true -> ".xls"
            else -> ".xlsx"
        }
        val temp = File.createTempFile("bharatscan_excel_", ext, cacheDir)
        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                temp.outputStream().use { fileOut -> input.copyTo(fileOut) }
            } ?: return false

            temp.inputStream().use { workbookStream ->
                openWorkbook(workbookStream).use { workbook ->
                    writeWorkbookToPdf(context, workbook, output, pageSize)
                }
            }
            true
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed converting excel to pdf for uri=$sourceUri", e)
            }
            false
        } finally {
            temp.delete()
        }
    }

    private fun openWorkbook(input: java.io.InputStream): Workbook {
        // Avoid relying on ServiceLoader-based providers which can be stripped in minified Android builds.
        val checked = FileMagic.prepareToCheckMagic(input)
        val magic = FileMagic.valueOf(checked)
        return when (magic) {
            FileMagic.OLE2 -> HSSFWorkbook(checked)
            FileMagic.OOXML -> XSSFWorkbook(checked)
            else -> throw IllegalArgumentException("Unsupported Excel file type: $magic")
        }
    }

    private fun writeWorkbookToPdf(
        context: Context,
        workbook: Workbook,
        output: OutputStream,
        pageSize: PDRectangle,
    ) {
        PDDocument().use { doc ->
            val font = loadDefaultFont(doc)
            val formatter = DataFormatter(Locale.getDefault(), true)
            val evaluator = workbook.creationHelper.createFormulaEvaluator()

            for (sheetIndex in 0 until workbook.numberOfSheets) {
                val sheet = workbook.getSheetAt(sheetIndex)
                if (sheet.physicalNumberOfRows <= 0) continue

                val used = computeUsedArea(sheet) ?: continue
                val firstRow = used.firstRow
                val lastRow = used.lastRow
                val firstCol = used.firstCol
                val lastCol = used.lastCol

                val margin = 36f
                val contentW = pageSize.width - margin * 2f
                val contentH = pageSize.height - margin * 2f

                val colWidths = (firstCol..lastCol).map { col ->
                    // Excel column width is 1/256th of a character width. Approximate to points.
                    val widthChars = (sheet.getColumnWidth(col).toFloat() / 256f).coerceAtLeast(1f)
                    widthChars * 7f
                }
                val rowHeights = (firstRow..lastRow).map { r ->
                    val row = sheet.getRow(r)
                    (row?.heightInPoints ?: sheet.defaultRowHeightInPoints.toFloat()).coerceAtLeast(10f)
                }

                val tableW = colWidths.sum().coerceAtLeast(1f)
                val fitScale = min(1f, contentW / tableW)
                val scaledColWidths = colWidths.map { it * fitScale }
                val scaledRowHeights = rowHeights.map { it * fitScale }

                val mergedLookup = buildMergedLookup(sheet)

                var rowPtr = 0
                while (rowPtr < scaledRowHeights.size) {
                    var heightAcc = 0f
                    var endRowPtr = rowPtr
                    while (endRowPtr < scaledRowHeights.size) {
                        val h = scaledRowHeights[endRowPtr]
                        if (heightAcc + h > contentH && endRowPtr > rowPtr) break
                        heightAcc += h
                        endRowPtr++
                        if (heightAcc >= contentH) break
                    }

                    val page = PDPage(pageSize)
                    doc.addPage(page)
                    PDPageContentStream(doc, page).use { cs ->
                        // Title on first page of each sheet.
                        if (rowPtr == 0) {
                            cs.beginText()
                            cs.setFont(font, 12f)
                            cs.newLineAtOffset(margin, pageSize.height - margin + 12f)
                            cs.showText(sheet.sheetName)
                            cs.endText()
                        }

                        val originX = margin
                        val originYTop = pageSize.height - margin

                        // Compute grid boundaries.
                        val xBoundaries = FloatArray(scaledColWidths.size + 1)
                        xBoundaries[0] = originX
                        for (i in scaledColWidths.indices) {
                            xBoundaries[i + 1] = xBoundaries[i] + scaledColWidths[i]
                        }
                        val yBoundaries = FloatArray((endRowPtr - rowPtr) + 1)
                        yBoundaries[0] = originYTop
                        for (i in 0 until (endRowPtr - rowPtr)) {
                            yBoundaries[i + 1] = yBoundaries[i] - scaledRowHeights[rowPtr + i]
                        }

                        // Draw grid.
                        cs.setStrokingColor(220)
                        cs.setLineWidth(0.5f)
                        for (x in xBoundaries) {
                            cs.moveTo(x, yBoundaries[0])
                            cs.lineTo(x, yBoundaries[yBoundaries.lastIndex])
                        }
                        for (y in yBoundaries) {
                            cs.moveTo(xBoundaries[0], y)
                            cs.lineTo(xBoundaries[xBoundaries.lastIndex], y)
                        }
                        cs.stroke()

                        val fontSize = max(6f, 10f * fitScale)
                        val padding = 2f

                        for (rowIndex in rowPtr until endRowPtr) {
                            val absoluteRow = firstRow + rowIndex
                            val row = sheet.getRow(absoluteRow)
                            val localRow = rowIndex - rowPtr
                            val cellTop = yBoundaries[localRow]
                            val cellBottom = yBoundaries[localRow + 1]
                            for (colIndex in firstCol..lastCol) {
                                val localCol = colIndex - firstCol
                                val cellLeft = xBoundaries[localCol]
                                val cellRight = xBoundaries[localCol + 1]

                                val merged = mergedLookup[absoluteRow to colIndex]
                                if (merged != null && (merged.firstRow != absoluteRow || merged.firstColumn != colIndex)) {
                                    continue
                                }

                                val drawLeft: Float
                                val drawRight: Float
                                val drawTop: Float
                                val drawBottom: Float

                                if (merged != null) {
                                    val startR = merged.firstRow - firstRow
                                    val endR = merged.lastRow - firstRow
                                    if (endR < rowPtr || startR >= endRowPtr) continue
                                    val startLocal = max(0, startR - rowPtr)
                                    val endLocal = min(endRowPtr - rowPtr - 1, endR - rowPtr)
                                    drawTop = yBoundaries[startLocal]
                                    drawBottom = yBoundaries[endLocal + 1]
                                    val startC = merged.firstColumn - firstCol
                                    val endC = merged.lastColumn - firstCol
                                    drawLeft = xBoundaries[startC]
                                    drawRight = xBoundaries[endC + 1]
                                } else {
                                    drawLeft = cellLeft
                                    drawRight = cellRight
                                    drawTop = cellTop
                                    drawBottom = cellBottom
                                }

                                val cell = row?.getCell(colIndex)
                                val text = cellText(cell, formatter, evaluator)
                                if (text.isBlank()) continue

                                val style = cell?.cellStyle
                                val align = style?.alignment ?: HorizontalAlignment.GENERAL

                                val availableW = (drawRight - drawLeft) - padding * 2f
                                val availableH = (drawTop - drawBottom) - padding * 2f
                                if (availableW <= 1f || availableH <= 1f) continue

                                val clipped = ellipsize(font, text, fontSize, availableW)
                                val textW = font.stringWidth(clipped, fontSize)
                                val x = when (align) {
                                    HorizontalAlignment.CENTER, HorizontalAlignment.CENTER_SELECTION -> drawLeft + (availableW - textW) / 2f + padding
                                    HorizontalAlignment.RIGHT -> drawRight - textW - padding
                                    else -> drawLeft + padding
                                }
                                val y = drawBottom + padding + (availableH - fontSize) / 2f

                                cs.beginText()
                                cs.setFont(font, fontSize)
                                cs.newLineAtOffset(x, y)
                                cs.showText(sanitizePdfText(clipped))
                                cs.endText()
                            }
                        }
                    }

                    rowPtr = endRowPtr
                }
            }

            doc.save(output)
        }
    }

    private data class UsedArea(val firstRow: Int, val lastRow: Int, val firstCol: Int, val lastCol: Int)

    private fun computeUsedArea(sheet: Sheet): UsedArea? {
        var firstRow = Int.MAX_VALUE
        var lastRow = -1
        var firstCol = Int.MAX_VALUE
        var lastCol = -1

        val rowIter = sheet.rowIterator()
        while (rowIter.hasNext()) {
            val row = rowIter.next()
            val r = row.rowNum
            val (minC, maxC) = rowUsedCols(row) ?: continue
            firstRow = min(firstRow, r)
            lastRow = max(lastRow, r)
            firstCol = min(firstCol, minC)
            lastCol = max(lastCol, maxC)
        }
        if (lastRow < 0 || lastCol < 0 || firstRow == Int.MAX_VALUE || firstCol == Int.MAX_VALUE) return null
        return UsedArea(firstRow, lastRow, firstCol, lastCol)
    }

    private fun rowUsedCols(row: Row): Pair<Int, Int>? {
        var minC = Int.MAX_VALUE
        var maxC = -1
        val cellIter = row.cellIterator()
        while (cellIter.hasNext()) {
            val cell = cellIter.next()
            if (cell.cellType == CellType.BLANK) continue
            val c = cell.columnIndex
            minC = min(minC, c)
            maxC = max(maxC, c)
        }
        if (maxC < 0 || minC == Int.MAX_VALUE) return null
        return minC to maxC
    }

    private fun buildMergedLookup(sheet: Sheet): Map<Pair<Int, Int>, CellRangeAddress> {
        if (sheet.numMergedRegions <= 0) return emptyMap()
        val map = HashMap<Pair<Int, Int>, CellRangeAddress>()
        for (i in 0 until sheet.numMergedRegions) {
            val region = sheet.getMergedRegion(i)
            for (r in region.firstRow..region.lastRow) {
                for (c in region.firstColumn..region.lastColumn) {
                    map[r to c] = region
                }
            }
        }
        return map
    }

    private fun cellText(
        cell: Cell?,
        formatter: DataFormatter,
        evaluator: org.apache.poi.ss.usermodel.FormulaEvaluator,
    ): String {
        if (cell == null) return ""
        return try {
            formatter.formatCellValue(cell, evaluator).orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    private fun loadDefaultFont(doc: PDDocument): PDFont {
        val candidates = listOf(
            "/system/fonts/NotoSans-Regular.ttf",
            "/system/fonts/Roboto-Regular.ttf",
            "/system/fonts/DroidSans.ttf",
        )
        for (path in candidates) {
            val file = File(path)
            if (file.exists()) {
                try {
                    return PDType0Font.load(doc, file)
                } catch (_: Exception) {
                    // ignore
                }
            }
        }
        return PDType1Font.HELVETICA
    }

    private fun sanitizePdfText(text: String): String {
        // PDFBox can't draw certain control chars.
        return buildString(text.length) {
            text.forEach { ch ->
                if (ch == '\n' || ch == '\r' || ch == '\t') append(' ')
                else if (ch.code in 0..31) append(' ')
                else append(ch)
            }
        }
    }

    private fun PDFont.stringWidth(text: String, fontSize: Float): Float {
        return try {
            (getStringWidth(text) / 1000f) * fontSize
        } catch (_: Exception) {
            0f
        }
    }

    private fun ellipsize(font: PDFont, text: String, fontSize: Float, maxWidth: Float): String {
        if (text.isEmpty()) return text
        if (font.stringWidth(text, fontSize) <= maxWidth) return text
        val ellipsis = "…"
        val target = maxWidth - font.stringWidth(ellipsis, fontSize)
        if (target <= 0f) return ellipsis
        var low = 0
        var high = text.length
        while (low < high) {
            val mid = (low + high + 1) / 2
            val candidate = text.substring(0, mid)
            if (font.stringWidth(candidate, fontSize) <= target) low = mid else high = mid - 1
        }
        return text.substring(0, low).trimEnd() + ellipsis
    }
}
