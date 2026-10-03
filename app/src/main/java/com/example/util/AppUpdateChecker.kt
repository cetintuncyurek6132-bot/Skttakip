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
            val cleanTagName = rawTagName.trim().replace(Regex("(?i)beta|v|sürüm"), "").trim()
            val rawReleaseNotes = json.optString("body", "")
            val releaseNotes = ReleaseNotesTranslator.formatAsBulletsString(rawReleaseNotes, cleanTagName)

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

            val currentVersion = BuildConfig.VERSION_NAME.trim().replace(Regex("(?i)beta|v|sürüm"), "").trim()
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

/**
 * GitHub Release ve Commit geçmişinden gelen metinleri dinamik olarak analiz eden,
 * yazılım terimlerini Türkçeleştiren ve gerçek değişiklik maddelerini listeyen motor.
 */
object ReleaseNotesTranslator {

    /**
     * Ham notları satır satır inceler, markdown ve teknik sembolleri temizler,
     * terimleri Türkçeleştirip liste döner. Asla sabit/uydurma metin eklemez.
     */
    fun translateToBulletPoints(rawNotes: String, versionName: String = ""): List<String> {
        val trimmed = rawNotes.trim()
        if (trimmed.isBlank() || trimmed.equals("null", ignoreCase = true)) {
            return if (versionName.isNotBlank()) {
                listOf("v$versionName sürüm güncellemesi.")
            } else {
                emptyList()
            }
        }

        // Markdown URL, hash, commit referansı ve kullanıcı etiketlerini temizle
        val lines = trimmed
            .replace(Regex("https?://\\S+"), "")
            .replace(Regex("\\b[0-9a-f]{7,40}\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("#\\d+"), "")
            .replace(Regex("@[a-zA-Z0-9_-]+"), "")
            .lines()
            .map { it.trim() }
            .filter { line ->
                line.isNotBlank() &&
                !line.startsWith("Full Changelog", ignoreCase = true) &&
                !line.startsWith("What's Changed", ignoreCase = true) &&
                !line.startsWith("See the assets", ignoreCase = true) &&
                !line.startsWith("Compare", ignoreCase = true) &&
                !line.startsWith("Assets", ignoreCase = true) &&
                !line.startsWith("```") &&
                line != "---"
            }

        val resultList = mutableListOf<String>()

        for (rawLine in lines) {
            val cleaned = rawLine
                .removePrefix("####")
                .removePrefix("###")
                .removePrefix("##")
                .removePrefix("#")
                .removePrefix("*")
                .removePrefix("-")
                .removePrefix("•")
                .removePrefix("+")
                .trim()

            if (cleaned.isBlank()) continue

            val translated = translateSingleLine(cleaned)
            if (translated.isNotBlank()) {
                resultList.add(translated)
            }
        }

        val distinctList = resultList.distinct()
        if (distinctList.isNotEmpty()) {
            return distinctList
        }

        return if (versionName.isNotBlank()) {
            listOf("v$versionName sürüm güncellemesi.")
        } else {
            emptyList()
        }
    }

    fun formatAsBulletsString(rawNotes: String, versionName: String = ""): String {
        val items = translateToBulletPoints(rawNotes, versionName)
        return items.joinToString("\n") { "• $it" }
    }

    private fun translateSingleLine(line: String): String {
        var text = line
            .replace("**", "")
            .replace("__", "")
            .replace("`", "")
            .trim()

        if (text.isBlank()) return ""

        // Eğer metin zaten Türkçe yazılmışsa (örneğin commit Türkçe atılmışsa) kelimelerini bozma
        val turkishWords = listOf(
            "ve", "ile", "için", "yapıldı", "eklendi", "düzeltildi", "güncellendi",
            "düzeltme", "hata", "buton", "sayfa", "ürün", "ekranı", "tasarımı",
            "ayarlar", "bildirim", "stok", "fire", "satış", "tarihi", "skt",
            "kayıt", "arama", "tarayıcı", "yenilendi", "geliştirildi", "kaldırıldı",
            "iyileştirildi", "kod", "düzenlendi", "öngörü", "tahmin", "yeni", "modülü",
            "hatırlatıcı", "rapor", "sayım", "reyon", "raf", "kullanıcı"
        )
        val hasTurkishChar = text.any { it in "çğıöşüÇĞİÖŞÜ" }
        val lower = text.lowercase()
        val isAlreadyTurkish = hasTurkishChar || turkishWords.any { lower.contains(it) }

        if (isAlreadyTurkish) {
            return text.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        // Yazılım geliştirme terimlerine ait aksiyon ön ekleri
        var actionPrefix = ""
        val prefixRules = listOf(
            Regex("^(bug fix|bugfix|bug-fix|bug_fix|fix|fixed|resolve|resolved)\\s*[:\\-]?\\s*", RegexOption.IGNORE_CASE) to "Düzeltildi: ",
            Regex("^(add|added|feature|feat|new)\\s*[:\\-]?\\s*", RegexOption.IGNORE_CASE) to "Eklendi: ",
            Regex("^(update|updated|improve|improved|enhancement|enhance|enhanced)\\s*[:\\-]?\\s*", RegexOption.IGNORE_CASE) to "İyileştirildi: ",
            Regex("^(remove|removed|delete|deleted)\\s*[:\\-]?\\s*", RegexOption.IGNORE_CASE) to "Kaldırıldı: ",
            Regex("^(refactor|refactored|optimize|optimized|perf)\\s*[:\\-]?\\s*", RegexOption.IGNORE_CASE) to "Optimize Edildi: "
        )

        for ((regex, prefix) in prefixRules) {
            if (regex.containsMatchIn(text)) {
                actionPrefix = prefix
                text = text.replace(regex, "").trim()
                break
            }
        }

        // Terim sözlüğü
        val dictionary = listOf(
            Regex("(?i)\\bduplicate\\b") to "mükerrer",
            Regex("(?i)\\bbadge\\b") to "rozet",
            Regex("(?i)\\bwaste\\b") to "fire",
            Regex("(?i)\\bspoilage\\b") to "fire",
            Regex("(?i)\\banalytics\\b") to "analiz ve tahminleme",
            Regex("(?i)\\bdashboard\\b") to "ana sayfa gösterge paneli",
            Regex("(?i)\\bscanner\\b") to "barkod tarayıcı",
            Regex("(?i)\\bbarcode\\b") to "barkod",
            Regex("(?i)\\bbackup\\b") to "yedekleme",
            Regex("(?i)\\brestore\\b") to "geri yükleme",
            Regex("(?i)\\breminders\\b") to "hatırlatıcılar",
            Regex("(?i)\\breminder\\b") to "hatırlatıcı",
            Regex("(?i)\\bcategory\\b") to "reyon/kategori",
            Regex("(?i)\\bcategories\\b") to "reyon/kategoriler",
            Regex("(?i)\\bcamera\\b") to "kamera",
            Regex("(?i)\\bnotification\\b") to "bildirim",
            Regex("(?i)\\bnotifications\\b") to "bildirimler",
            Regex("(?i)\\bhaptic feedback\\b") to "titreşimli geri bildirim",
            Regex("(?i)\\bdark mode\\b") to "karanlık tema",
            Regex("(?i)\\blight mode\\b") to "aydınlık tema",
            Regex("(?i)\\bsettings\\b") to "ayarlar",
            Regex("(?i)\\bproduct\\b") to "ürün",
            Regex("(?i)\\bproducts\\b") to "ürünler",
            Regex("(?i)\\bexpiry date\\b") to "son kullanma tarihi",
            Regex("(?i)\\bexpiration date\\b") to "son kullanma tarihi",
            Regex("(?i)\\bcalendar\\b") to "takvim",
            Regex("(?i)\\bdialog\\b") to "pencere",
            Regex("(?i)\\bmodal\\b") to "pencere",
            Regex("(?i)\\bui\\b") to "arayüz",
            Regex("(?i)\\bperformance\\b") to "performans",
            Regex("(?i)\\bdatabase\\b") to "veritabanı",
            Regex("(?i)\\bcrash\\b") to "kapanma sorunu",
            Regex("(?i)\\bbug\\b") to "hata",
            Regex("(?i)\\bbutton\\b") to "buton",
            Regex("(?i)\\bhistory\\b") to "geçmiş",
            Regex("(?i)\\bexport\\b") to "dışa aktarma",
            Regex("(?i)\\bimport\\b") to "içe aktarma"
        )

        for ((regex, replacement) in dictionary) {
            text = text.replace(regex, replacement)
        }

        text = text.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        return if (actionPrefix.isNotEmpty()) {
            "$actionPrefix$text"
        } else {
            text
        }
    }
}
