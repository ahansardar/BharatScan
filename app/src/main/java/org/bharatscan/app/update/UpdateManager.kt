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
package org.bharatscan.app.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import org.bharatscan.app.BuildConfig
import org.bharatscan.app.data.Logger
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val apkName: String,
    val downloadUrl: String,
    val releaseNotes: String?
)

data class UpdateDownloadStatus(
    val status: Int,
    val bytesDownloaded: Long,
    val bytesTotal: Long
) {
    val progress: Float?
        get() = if (bytesTotal > 0) bytesDownloaded.toFloat() / bytesTotal.toFloat() else null
}

object UpdateManager {
    private const val PREFS = "update_prefs"
    private const val KEY_DOWNLOAD_ID = "download_id"
    private const val KEY_APK_NAME = "apk_name"
    private const val KEY_LAST_CHECK = "last_check"
    private const val CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L

    private const val OWNER = "ahansardar"
    private const val REPO = "BharatScan"
    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/$OWNER/$REPO/releases/latest"

    suspend fun checkForUpdate(context: Context, logger: Logger, force: Boolean = false): UpdateInfo? {
        if (!force && !shouldCheck(context)) return null
        val currentVersion = BuildConfig.VERSION_NAME
        return try {
            val payload = fetchLatestRelease()
            val json = JSONObject(payload)
            val latestVersion = extractVersion(json.optString("tag_name"), json.optString("name"))
            if (compareVersions(latestVersion, currentVersion) <= 0) {
                markChecked(context)
                return null
            }
            val assets = json.optJSONArray("assets") ?: return null
            val (apkName, downloadUrl) = selectAsset(assets) ?: return null
            val notes = json.optString("body", null)
            markChecked(context)
            UpdateInfo(latestVersion, apkName, downloadUrl, notes)
        } catch (e: Exception) {
            logger.e("Update", "Failed to check updates", e)
            null
        }
    }

    fun registerDownloadReceiver(context: Context, logger: Logger): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (downloadId <= 0) return
                handleDownloadComplete(ctx, logger, downloadId)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }
        return receiver
    }

    fun unregisterDownloadReceiver(context: Context, receiver: BroadcastReceiver?) {
        if (receiver == null) return
        runCatching { context.unregisterReceiver(receiver) }
    }

    fun downloadUpdate(context: Context, info: UpdateInfo, logger: Logger): Long {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(info.downloadUrl))
            .setTitle("BharatScan update")
            .setDescription("Downloading ${info.apkName}")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, info.apkName)

        removeExistingDownload(manager, info.apkName)
        val id = manager.enqueue(request)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_DOWNLOAD_ID, id)
            .putString(KEY_APK_NAME, info.apkName)
            .apply()
        Log.i("Update", "Enqueued download id=$id")
        return id
    }

    private fun handleDownloadComplete(context: Context, logger: Logger, downloadId: Long) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val expectedId = prefs.getLong(KEY_DOWNLOAD_ID, -1L)
        if (expectedId != downloadId) return

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        manager.query(query)?.use { cursor ->
            if (!cursor.moveToFirst()) return
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            if (status != DownloadManager.STATUS_SUCCESSFUL) {
                Log.e("Update", "Download failed status=$status")
                return
            }
            val localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
            val file = localUri?.let { Uri.parse(it).path }?.let { File(it) }
            if (file == null || !file.exists()) {
                Log.e("Update", "Downloaded APK missing")
                return
            }
            promptInstall(context, file, logger)
            clearDownload(context)
        }
    }

    private fun promptInstall(context: Context, apkFile: File, logger: Logger) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData(Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        runCatching { context.startActivity(intent) }
            .onFailure { logger.e("Update", "Failed to launch installer", it) }
    }

    fun getDownloadStatus(context: Context): UpdateDownloadStatus? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val downloadId = prefs.getLong(KEY_DOWNLOAD_ID, -1L)
        if (downloadId <= 0L) return null

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        manager.query(query)?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val bytesDownloaded =
                cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val bytesTotal =
                cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            return UpdateDownloadStatus(status, bytesDownloaded, bytesTotal)
        }
        return null
    }

    fun clearDownload(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_DOWNLOAD_ID)
            .remove(KEY_APK_NAME)
            .apply()
    }

    private fun shouldCheck(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = prefs.getLong(KEY_LAST_CHECK, 0L)
        return System.currentTimeMillis() - last >= CHECK_INTERVAL_MS
    }

    private fun markChecked(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_CHECK, System.currentTimeMillis())
            .apply()
    }

    private fun fetchLatestRelease(): String {
        val connection = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "BharatScan/${BuildConfig.VERSION_NAME}")
        }
        connection.inputStream.use { stream ->
            return stream.bufferedReader().readText()
        }
    }

    private fun extractVersion(tag: String?, name: String?): String {
        val raw = tag?.takeIf { it.isNotBlank() } ?: name.orEmpty()
        return raw.trim().removePrefix("v").removePrefix("V")
    }

    private fun selectAsset(assets: org.json.JSONArray): Pair<String, String>? {
        val abis = Build.SUPPORTED_ABIS.toList()
        val candidates = mutableListOf<Pair<String, String>>()
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name = asset.optString("name")
            val url = asset.optString("browser_download_url")
            if (!name.endsWith(".apk", ignoreCase = true)) continue
            candidates += name to url
        }
        for (abi in abis) {
            candidates.firstOrNull { it.first.contains("-$abi", ignoreCase = true) }?.let { return it }
        }
        return candidates.firstOrNull()
    }

    private fun compareVersions(a: String, b: String): Int {
        val aParts = a.split('.', '-', '_').mapNotNull { it.toIntOrNull() }
        val bParts = b.split('.', '-', '_').mapNotNull { it.toIntOrNull() }
        val max = maxOf(aParts.size, bParts.size)
        for (i in 0 until max) {
            val ai = aParts.getOrElse(i) { 0 }
            val bi = bParts.getOrElse(i) { 0 }
            if (ai != bi) return ai.compareTo(bi)
        }
        return 0
    }

    private fun removeExistingDownload(manager: DownloadManager, apkName: String) {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), apkName)
        if (file.exists()) {
            file.delete()
        }
    }
}
