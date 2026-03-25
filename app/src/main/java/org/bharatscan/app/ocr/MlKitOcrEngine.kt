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

import android.graphics.Bitmap
import android.graphics.RectF
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer

class MlKitOcrEngine(
    private val recognizer: TextRecognizer
) : OcrEngine {

    override suspend fun recognize(bitmap: Bitmap): List<OcrLineResult> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = Tasks.await(recognizer.process(image))
        return result.textBlocks.flatMap { block ->
            block.lines.mapNotNull { line ->
                val box = line.boundingBox ?: return@mapNotNull null
                OcrLineResult(
                    text = line.text,
                    bounds = RectF(
                        box.left.toFloat(),
                        box.top.toFloat(),
                        box.right.toFloat(),
                        box.bottom.toFloat()
                    )
                )
            }
        }
    }

    override fun close() {
        recognizer.close()
    }
}
