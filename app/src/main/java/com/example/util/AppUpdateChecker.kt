package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
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
    val hasUpdate: Boolean = false
)

object AppUpdateChecker {
    private const val TAG = "AppUpdateChecker"
    private const val GITHUB_API_URL = "https://api.github.com/repos/cetintuncyurek6132-bot/Skttakip/releases/latest"

    /**
     * GitHub Releases API üzerinden en son sürümü sorgular ve mevcut sürümle karşılaştırır.
     */
    suspend fun checkForUpdates(): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "A101-SKT-Takip-App")
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
            val cleanTagName = rawTagName.removePrefix("v").removePrefix("V").trim()
            val releaseNotes = json.optString("body", "")

            var downloadUrl = ""
            val assetsArray = json.optJSONArray("assets")
            if (assetsArray != null) {
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }

            val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").removePrefix("V").trim()
            val isNewer = isVersionNewer(cleanTagName, currentVersion)

            Log.d(TAG, "Mevcut: $currentVersion | GitHub: $cleanTagName | Güncelleme Var mı: $isNewer | URL: $downloadUrl")

            Result.success(
                AppUpdateInfo(
                    latestVersionName = cleanTagName.ifBlank { rawTagName },
                    downloadUrl = downloadUrl,
                    releaseNotes = releaseNotes,
                    hasUpdate = isNewer && downloadUrl.isNotBlank()
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Güncelleme kontrol hatası: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * İki sürüm numarasını (Örn: 1.0.2 vs 1.1 veya 1.0.5 vs 1.0.4) semantik olarak karşılaştırır.
     */
    fun isVersionNewer(remoteVersion: String, currentVersion: String): Boolean {
        if (remoteVersion.isBlank()) return false
        if (remoteVersion == currentVersion) return false

        try {
            val remoteParts = remoteVersion.split(".", "-").mapNotNull { it.toIntOrNull() }
            val currentParts = currentVersion.split(".", "-").mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
        } catch (e: Exception) {
            return remoteVersion > currentVersion
        }
        return false
    }

    /**
     * APK dosyasını indirir ve progress bildiriminde bulunur.
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (progressPercent: Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val url = URL(downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "A101-SKT-Takip-App")
                instanceFollowRedirects = true
                connectTimeout = 15000
                readTimeout = 20000
            }

            // GitHub releases redirectleri takip et
            var responseCode = connection.responseCode
            var redirectConn = connection
            var redirects = 0
            while ((responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                        responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                        responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                        responseCode == 307 || responseCode == 308) && redirects < 5
            ) {
                val newUrl = redirectConn.getHeaderField("Location")
                redirectConn = (URL(newUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "A101-SKT-Takip-App")
                    connectTimeout = 15000
                    readTimeout = 20000
                }
                responseCode = redirectConn.responseCode
                redirects++
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("İndirme başarısız (HTTP $responseCode)"))
            }

            val fileLength = redirectConn.contentLength
            val apkFile = File(context.cacheDir, "update.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            redirectConn.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val data = ByteArray(8192)
                    var total: Long = 0
                    var count: Int
                    while (input.read(data).also { count = it } != -1) {
                        total += count
                        if (fileLength > 0) {
                            val percent = ((total * 100) / fileLength).toInt()
                            onProgress(percent.coerceIn(0, 100))
                        }
                        output.write(data, 0, count)
                    }
                    output.flush()
                }
            }

            Result.success(apkFile)
        } catch (e: Exception) {
            Log.e(TAG, "APK indirme hatası: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * İndirilen APK dosyasını Android PackageInstaller üzerinden kurmak üzere intent başlatır.
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "APK kurulum intent hatası: ${e.message}", e)
        }
    }
}
