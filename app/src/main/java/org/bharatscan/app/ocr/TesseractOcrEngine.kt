/*
 * Copyright 2025-2026 Ahan Sardar
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.bharatscan.app.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class TesseractOcrEngine private constructor(
    private val api: TessBaseAPI
) : OcrEngine {

    override suspend fun recognize(bitmap: Bitmap): List<OcrLineResult> {
        return withContext(Dispatchers.Default) {
            api.setImage(bitmap)
            try {
                val iterator = api.resultIterator ?: return@withContext emptyList()
                val level = TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE
                val lines = mutableListOf<OcrLineResult>()
                iterator.begin()
                do {
                    val text = iterator.getUTF8Text(level)?.trim().orEmpty()
                    val rect = iterator.getBoundingRect(level)
                    if (text.isNotBlank() && rect != null) {
                        lines.add(
                            OcrLineResult(
                                text = text,
                                bounds = RectF(
                                    rect.left.toFloat(),
                                    rect.top.toFloat(),
                                    rect.right.toFloat(),
                                    rect.bottom.toFloat()
                                )
                            )
                        )
                    }
                } while (iterator.next(level))
                lines
            } finally {
                api.clear()
            }
        }
    }

    override fun close() {
        api.end()
    }

    companion object {
        private const val TAG = "TesseractOcr"

        suspend fun create(
            context: Context,
            languages: List<String>
        ): TesseractOcrEngine? {
            val cleaned = languages.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            if (cleaned.isEmpty()) return null

            val dataRoot = File(context.filesDir, "tesseract")
            if (!dataRoot.exists() && !dataRoot.mkdirs()) {
                Log.w(TAG, "Failed to create tesseract data dir at ${dataRoot.absolutePath}")
                return null
            }

            if (!TessDataManager.ensureLanguages(context, cleaned)) {
                return null
            }

            val api = TessBaseAPI()
            val ok = api.init(dataRoot.absolutePath, cleaned.joinToString("+"))
            if (!ok) {
                api.end()
                return null
            }
            api.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
            return TesseractOcrEngine(api)
        }
    }
}
