package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val latestVersionName: String,
    val downloadUrl: String,
    val releaseNotes: String = "",
    val hasUpdate: Boolean = false,
    val apkSizeBytes: Long = 0L,
    val apkSizeFormatted: String = ""
)

object AppUpdateChecker {
    private const val TAG = "AppUpdateChecker"
    private const val GITHUB_API_URL = "https://api.github.com/repos/cetintuncyurek6132-bot/Skttakip/releases/latest"

    /**
     * Bayt boyutunu okunabilir MB/KB metnine çevirir.
     */
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 MB"
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return if (mb >= 1.0) {
            String.format(java.util.Locale.US, "%.1f MB", mb)
        } else {
            val kb = bytes.toDouble() / 1024.0
            String.format(java.util.Locale.US, "%.0f KB", kb)
        }
    }

    /**
     * GitHub Releases API üzerinden en son sürümü sorgular ve mevcut sürümle karşılaştırır.
     */
    suspend fun checkForUpdates(): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "SKT-Takip-App")
                connectTimeout = 8000
                readTimeout = 8000
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("GitHub API Yanıt Kodu: $responseCode"))
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseBody)

            val rawTagName = json.optString("tag_name", "")
            val cleanTagName = rawTagName.trim().replace(Regex("(?i)beta|v|sürüm"), "").trim()
            val rawReleaseNotes = json.optString("body", "")
            val releaseNotes = ReleaseNotesTranslator.formatAsBulletsString(rawReleaseNotes, cleanTagName)

            var downloadUrl = ""
            var apkSizeBytes = 0L
            var apkSizeFormatted = ""
            val assetsArray = json.optJSONArray("assets")
            if (assetsArray != null) {
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        apkSizeBytes = asset.optLong("size", 0L)
                        if (apkSizeBytes > 0L) {
                            apkSizeFormatted = formatBytes(apkSizeBytes)
                        }
                        break
                    }
                }
            }

            val currentVersion = BuildConfig.VERSION_NAME.trim().replace(Regex("(?i)beta|v|sürüm"), "").trim()
            val isNewer = isVersionNewer(cleanTagName, currentVersion)

            Log.d(TAG, "Mevcut: $currentVersion | GitHub: $cleanTagName | Güncelleme Var mı: $isNewer | Boyut: $apkSizeFormatted | URL: $downloadUrl")

            Result.success(
                AppUpdateInfo(
                    latestVersionName = cleanTagName.ifBlank { rawTagName },
                    downloadUrl = downloadUrl,
                    releaseNotes = releaseNotes,
                    hasUpdate = isNewer && downloadUrl.isNotBlank(),
                    apkSizeBytes = apkSizeBytes,
                    apkSizeFormatted = apkSizeFormatted
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Güncelleme kontrol hatası: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * GitHub Releases veya commit geçmişinden gelen güncelleme notlarını
     * ReleaseNotesTranslator ile Türkçeleştirir, temizler ve anlaşılır madde imleri halinde formatlar.
     */
    fun formatReleaseNotesTurkish(rawNotes: String): String {
        return ReleaseNotesTranslator.formatAsBulletsString(rawNotes)
    }

    /**
     * İki sürüm numarasını (Örn: 1.0.8 vs 1.0.0 veya 1.0.8 vs 1.0.7) semantik olarak karşılaştırır.
     * Major, minor ve patch bileşenlerini sayısal olarak kıyaslar.
     */
    fun isVersionNewer(remoteVersion: String, currentVersion: String): Boolean {
        val cleanRemote = remoteVersion.trim().replace("beta", "", ignoreCase = true).removePrefix("v").removePrefix("V").trim()
        val cleanCurrent = currentVersion.trim().replace("beta", "", ignoreCase = true).removePrefix("v").removePrefix("V").trim()

        if (cleanRemote.isBlank()) return false
        if (cleanRemote.equals(cleanCurrent, ignoreCase = true)) return false

        try {
            val remoteParts = cleanRemote.split(".", "-", "_").mapNotNull { it.trim().toIntOrNull() }
            val currentParts = cleanCurrent.split(".", "-", "_").mapNotNull { it.trim().toIntOrNull() }

            if (remoteParts.isNotEmpty() && currentParts.isNotEmpty()) {
                val maxLen = maxOf(remoteParts.size, currentParts.size)
                for (i in 0 until maxLen) {
                    val r = remoteParts.getOrElse(i) { 0 }
                    val c = currentParts.getOrElse(i) { 0 }
                    if (r > c) return true
                    if (r < c) return false
                }
                return false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Semantik sürüm karşılaştırma istisnası: ${e.message}")
        }

        // Sayısal parse edilemeyen durumlarda fallback metin karşılaştırması
        return cleanRemote.compareTo(cleanCurrent, ignoreCase = true) > 0
    }

    /**
     * APK dosyasını indirir ve progress bildiriminde bulunur.
     * GitHub Releases yönlendirmelerini (301/302 S3 redirects) ve zaman aşımlarını güvenli şekilde yönetir.
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (percent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        var currentUrl = downloadUrl
        var connection: HttpURLConnection? = null
        try {
            var redirects = 0
            val maxRedirects = 8
            var responseCode: Int

            while (true) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                    setRequestProperty("Accept", "*/*")
                    instanceFollowRedirects = true
                    connectTimeout = 30000
                    readTimeout = 60000
                }

                responseCode = connection.responseCode

                // 301, 302, 303, 307, 308 yönlendirmelerini takip et
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                    responseCode == 307 ||
                    responseCode == 308
                ) {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (!location.isNullOrBlank() && redirects < maxRedirects) {
                        currentUrl = if (location.startsWith("http://") || location.startsWith("https://")) {
                            location
                        } else {
                            URL(url, location).toString()
                        }
                        redirects++
                        continue
                    } else {
                        return@withContext Result.failure(Exception("Yönlendirme hatası (HTTP $responseCode)"))
                    }
                }
                break
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("İndirme başarısız (HTTP $responseCode)"))
            }

            val finalConn = connection ?: return@withContext Result.failure(Exception("Bağlantı kurulamadı"))
            val fileLength = finalConn.contentLength.toLong()
            val apkFile = File(context.cacheDir, "update.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            finalConn.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val data = ByteArray(8192)
                    var total: Long = 0
                    var count: Int
                    while (input.read(data).also { count = it } != -1) {
                        total += count
                        val percent = if (fileLength > 0L) {
                            ((total * 100) / fileLength).toInt().coerceIn(0, 100)
                        } else {
                            0
                        }
                        onProgress(percent, total, if (fileLength > 0L) fileLength else total)
                        output.write(data, 0, count)
                    }
                    output.flush()
                }
            }

            Result.success(apkFile)
        } catch (e: Exception) {
            Log.e(TAG, "APK indirme hatası: ${e.message}", e)
            Result.failure(e)
        } finally {
            try {
                connection?.disconnect()
            } catch (_: Exception) {}
        }
    }

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgressPercent: (progressPercent: Int) -> Unit
    ): Result<File> = downloadApk(context, downloadUrl) { percent, _, _ -> onProgressPercent(percent) }

    /**
     * İndirilen APK dosyasını Android PackageInstaller üzerinden kurmak üzere intent başlatır.
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "APK kurulum intent hatası: ${e.message}", e)
        }
    }
}

