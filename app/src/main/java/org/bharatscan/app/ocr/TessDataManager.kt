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
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object TessDataManager {
    private const val TAG = "TessData"
    private const val BASE_URL = "https://github.com/tesseract-ocr/tessdata_fast/raw/main/"
    private val lock = Mutex()
    private val ensured = mutableSetOf<String>()

    suspend fun ensureLanguages(context: Context, languages: List<String>): Boolean {
        return withContext(Dispatchers.IO) {
            val tessdataDir = File(File(context.filesDir, "tesseract"), "tessdata")
            if (!tessdataDir.exists() && !tessdataDir.mkdirs()) {
                Log.w(TAG, "Failed to create tessdata directory at ${tessdataDir.absolutePath}")
                return@withContext false
            }
            for (language in languages.distinct()) {
                val ok = lock.withLock { ensureLanguageLocked(tessdataDir, language) }
                if (!ok) return@withContext false
            }
            true
        }
    }

    private fun ensureLanguageLocked(tessdataDir: File, language: String): Boolean {
        val target = File(tessdataDir, "$language.traineddata")
        if (target.exists()) {
            ensured.add(language)
            return true
        }
        if (ensured.contains(language)) return false

        val url = URL("$BASE_URL$language.traineddata")
        val tmp = File(tessdataDir, "$language.traineddata.download")
        try {
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 20000
            }
            connection.inputStream.use { input ->
                tmp.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Failed to download tessdata for $language", e)
            tmp.delete()
            return false
        }

        if (tmp.length() <= 0L) {
            tmp.delete()
            return false
        }

        if (target.exists()) {
            target.delete()
        }
        if (!tmp.renameTo(target)) {
            return try {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
                ensured.add(language)
                true
            } catch (e: IOException) {
                Log.w(TAG, "Failed to move tessdata for $language", e)
                tmp.delete()
                false
            }
        }

        ensured.add(language)
        return true
    }
}
