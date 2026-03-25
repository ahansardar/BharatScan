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
import androidx.appcompat.app.AppCompatDelegate
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale

object TextRecognitionHelper {

    suspend fun createEngine(context: Context): OcrEngine {
        val language = resolveAppLanguage()
        val tesseractLanguages = resolveTesseractLanguages(language)
        if (tesseractLanguages != null) {
            val withEnglish = (tesseractLanguages + "eng").distinct()
            TesseractOcrEngine.create(context, withEnglish)?.let { return it }
        }
        return createMlKitEngine(language)
    }

    private fun createMlKitEngine(language: String): OcrEngine {
        if (language in setOf("hi", "mr")) {
            createDevanagariRecognizer()?.let { return MlKitOcrEngine(it) }
        }
        return MlKitOcrEngine(TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS))
    }

    private fun resolveAppLanguage(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        val locale = if (!locales.isEmpty) locales[0] else Locale.getDefault()
        val language = (locale?.language ?: Locale.getDefault().language)
        return language.lowercase(Locale.ROOT)
    }

    private fun resolveTesseractLanguages(language: String): List<String>? {
        return when (language) {
            "hi" -> listOf("hin")
            "mr" -> listOf("mar")
            "bn" -> listOf("ben")
            "te" -> listOf("tel")
            "ta" -> listOf("tam")
            "ur" -> listOf("urd")
            "gu" -> listOf("guj")
            "kn" -> listOf("kan")
            "ml" -> listOf("mal")
            "or", "od" -> listOf("ori")
            "pa" -> listOf("pan")
            else -> null
        }
    }

    private fun createDevanagariRecognizer(): TextRecognizer? {
        return try {
            val optionsClass = Class.forName(
                "com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions"
            )
            val builderClass = Class.forName(
                "com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions\$Builder"
            )
            val builder = builderClass.getDeclaredConstructor().newInstance()
            val buildMethod = builderClass.getMethod("build")
            val options = buildMethod.invoke(builder)
            val getClientMethod = TextRecognition::class.java.getMethod("getClient", optionsClass)
            getClientMethod.invoke(null, options) as? TextRecognizer
        } catch (_: Exception) {
            null
        }
    }
}
