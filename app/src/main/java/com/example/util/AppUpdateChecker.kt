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
            val cleanTagName = rawTagName.trim().removePrefix("v").removePrefix("V").trim()
            val rawReleaseNotes = json.optString("body", "")
            val releaseNotes = formatReleaseNotesTurkish(rawReleaseNotes)

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

            val currentVersion = BuildConfig.VERSION_NAME.trim().removePrefix("v").removePrefix("V").trim()
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
     * GitHub Releases veya commit geçmişinden gelen güncelleme notlarını
     * Türkçeleştirir, temizler ve anlaşılır madde imleri halinde formatlar.
     */
    fun formatReleaseNotesTurkish(rawNotes: String): String {
        if (rawNotes.isBlank()) {
            return "• Performans iyileştirmeleri ve hata düzeltmeleri yapıldı.\n• Barkod tarama ve veri işleme hızlandırıldı.\n• Arayüz kararlılığı ve kullanıcı deneyimi artırıldı."
        }

        // Markdown linkleri, PR numaraları ve GitHub kullanıcı etiketlerini temizle
        val lines = rawNotes
            .replace(Regex("https?://\\S+"), "")
            .replace(Regex("#\\d+"), "")
            .replace(Regex("@[a-zA-Z0-9_-]+"), "")
            .lines()
            .map { it.trim().removePrefix("*").removePrefix("-").removePrefix("#").trim() }
            .filter { line ->
                line.isNotBlank() &&
                !line.startsWith("Full Changelog", ignoreCase = true) &&
                !line.startsWith("What's Changed", ignoreCase = true) &&
                !line.startsWith("See the assets", ignoreCase = true) &&
                !line.startsWith("Compare", ignoreCase = true)
            }

        if (lines.isEmpty()) {
            return "• Performans iyileştirmeleri ve hata düzeltmeleri yapıldı.\n• Barkod tarama ve veri işleme hızlandırıldı.\n• Arayüz kararlılığı ve kullanıcı deneyimi artırıldı."
        }

        val turkishBullets = lines.mapNotNull { line ->
            translateOrCleanLine(line)
        }.distinct()

        return if (turkishBullets.isNotEmpty()) {
            turkishBullets.joinToString("\n") { if (it.startsWith("•")) it else "• $it" }
        } else {
            "• Performans iyileştirmeleri ve hata düzeltmeleri yapıldı.\n• Barkod tarama ve veri işleme hızlandırıldı.\n• Arayüz kararlılığı ve kullanıcı deneyimi artırıldı."
        }
    }

    private fun translateOrCleanLine(line: String): String? {
        val trimmed = line.trim().removePrefix("•").trim()
        if (trimmed.isBlank()) return null

        val lower = trimmed.lowercase()

        // Sık karşılaşılan İngilizce kalıpları Türkçeleştir
        return when {
            lower.contains("merge pull request") || lower.contains("merge branch") ->
                "Sistem güncellemeleri ve yeni geliştirmeler birleştirildi."
            lower.contains("bump version") ->
                "Sürüm numarası güncellendi ve optimize edildi."
            lower.contains("fix package conflict") || lower.contains("keystore") || lower.contains("signing") ->
                "APK imzalama ve paket güncelleme altyapısı kalıcı olarak düzeltildi."
            lower.startsWith("fix") || lower.contains("bug fix") || lower.contains("hata") -> {
                var cleaned = trimmed.replace(Regex("^(fix:|fix\\b|fixes\\b|fixed\\b)", RegexOption.IGNORE_CASE), "").trim()
                cleaned = translateKeywords(cleaned)
                "$cleaned düzeltildi."
            }
            lower.startsWith("add") || lower.startsWith("feat") || lower.contains("feature") || lower.contains("ekle") -> {
                var cleaned = trimmed.replace(Regex("^(add:|feat:|feature:|added\\b|adds\\b)", RegexOption.IGNORE_CASE), "").trim()
                cleaned = translateKeywords(cleaned)
                "$cleaned eklendi."
            }
            lower.startsWith("update") || lower.contains("güncelle") -> {
                var cleaned = trimmed.replace(Regex("^(update:|updated\\b|updates\\b)", RegexOption.IGNORE_CASE), "").trim()
                cleaned = translateKeywords(cleaned)
                "$cleaned güncellendi."
            }
            lower.startsWith("improve") || lower.startsWith("optimize") || lower.contains("performance") -> {
                var cleaned = trimmed.replace(Regex("^(improve:|optimize:|improved\\b|optimized\\b)", RegexOption.IGNORE_CASE), "").trim()
                cleaned = translateKeywords(cleaned)
                "$cleaned iyileştirildi ve hızlandırıldı."
            }
            lower.startsWith("refactor") || lower.startsWith("cleanup") || lower.startsWith("clean") -> {
                var cleaned = trimmed.replace(Regex("^(refactor:|cleanup:|cleaned\\b)", RegexOption.IGNORE_CASE), "").trim()
                cleaned = translateKeywords(cleaned)
                "$cleaned kod yapısı modernize edildi."
            }
            lower.startsWith("remove") || lower.startsWith("delete") -> {
                var cleaned = trimmed.replace(Regex("^(remove:|delete:|removed\\b|deleted\\b)", RegexOption.IGNORE_CASE), "").trim()
                cleaned = translateKeywords(cleaned)
                "$cleaned kaldırıldı."
            }
            else -> {
                val translated = translateKeywords(trimmed)
                if (!translated.endsWith(".") && !translated.endsWith("!")) "$translated." else translated
            }
        }
    }

    private fun translateKeywords(text: String): String {
        var t = text
        val map = mapOf(
            "barcode scanner" to "Barkod tarayıcı",
            "barcode" to "Barkod",
            "scanner" to "Barkod okuyucu",
            "camera" to "Kamera",
            "haptic feedback" to "Titreşimli geri bildirim",
            "dark mode" to "Karanlık tema",
            "light mode" to "Aydınlık tema",
            "settings" to "Ayarlar",
            "analytics" to "Analiz ve grafikler",
            "csv export" to "CSV dışa aktarma",
            "csv import" to "CSV içe aktarma",
            "backup" to "Yedekleme",
            "restore" to "Geri yükleme",
            "notification" to "Bildirimler",
            "performance" to "Performans",
            "database" to "Veritabanı",
            "product list" to "Ürün listesi",
            "products" to "Ürünler",
            "expiry date" to "Son kullanma tarihi",
            "calendar" to "Takvim",
            "dialog" to "Pencere",
            "modal" to "Pencere",
            "ui" to "Arayüz",
            "crash" to "Kapanma sorunu",
            "build" to "Derleme"
        )
        for ((en, tr) in map) {
            t = t.replace(Regex("(?i)\\b$en\\b"), tr)
        }
        return t.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
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
        onProgress: (progressPercent: Int) -> Unit
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
            val fileLength = finalConn.contentLength
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
        } finally {
            try {
                connection?.disconnect()
            } catch (_: Exception) {}
        }
    }

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
