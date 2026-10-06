package com.example.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

data class AppUpdateInfo(
    val latestVersionName: String,
    val downloadUrl: String,
    val releaseNotes: String = "",
    val hasUpdate: Boolean = false,
    val apkSizeBytes: Long = 0L,
    val apkSizeFormatted: String = ""
)

data class DownloadProgressInfo(
    val isDownloading: Boolean = false,
    val progress: Int = 0,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val isCompleted: Boolean = false,
    val downloadedFile: File? = null,
    val errorMessage: String? = null
)

object AppUpdateChecker {
    private const val TAG = "AppUpdateChecker"
    private const val GITHUB_API_URL = "https://api.github.com/repos/cetintuncyurek6132-bot/Skttakip/releases/latest"
    private const val USER_AGENT = "SKT-Takip-App-Android"
    private const val ACCEPT_HEADER = "application/vnd.github.v3+json"
    private const val CONNECT_TIMEOUT_MS = 30000 // 30 saniye
    private const val READ_TIMEOUT_MS = 30000 // 30 saniye

    private const val PREFS_NAME = "skt_app_update_prefs"
    private const val KEY_DOWNLOAD_ID = "active_download_id"
    private const val KEY_DOWNLOAD_VERSION = "active_download_version"

    private val _downloadProgressState = MutableStateFlow(DownloadProgressInfo())
    val downloadProgressState: StateFlow<DownloadProgressInfo> = _downloadProgressState.asStateFlow()

    private var progressTrackingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

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
     * Ağ kesintileri ve zaman aşımı (timeout) durumlarında 1 saniye sonra 1 kez otomatik retry uygular.
     */
    suspend fun checkForUpdates(): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        var lastException: Throwable? = null
        val maxAttempts = 2

        for (attempt in 1..maxAttempts) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(GITHUB_API_URL)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", USER_AGENT)
                    setRequestProperty("Accept", ACCEPT_HEADER)
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    useCaches = false
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
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

                    return@withContext Result.success(
                        AppUpdateInfo(
                            latestVersionName = cleanTagName.ifBlank { rawTagName },
                            downloadUrl = downloadUrl,
                            releaseNotes = releaseNotes,
                            hasUpdate = isNewer && downloadUrl.isNotBlank(),
                            apkSizeBytes = apkSizeBytes,
                            apkSizeFormatted = apkSizeFormatted
                        )
                    )
                } else if (responseCode == 403) {
                    val errorMsg = "GitHub API istek sınırı aşıldı. Lütfen daha sonra tekrar deneyin."
                    Log.w(TAG, errorMsg)
                    return@withContext Result.failure(Exception(errorMsg))
                } else if (responseCode == 404) {
                    val errorMsg = "Güncelleme sürümü bulunamadı."
                    Log.w(TAG, errorMsg)
                    return@withContext Result.failure(Exception(errorMsg))
                } else {
                    val errorMsg = "GitHub sunucusu yanıt vermedi (HTTP $responseCode)"
                    Log.w(TAG, errorMsg)
                    lastException = Exception(errorMsg)
                }
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Deneme $attempt/$maxAttempts başarısız: ${e.javaClass.simpleName} - ${e.message}")
            } finally {
                try {
                    connection?.disconnect()
                } catch (_: Exception) {}
            }

            // Yeniden deneme öncesi 1 saniye bekle (sadece 1. deneme başarısız olduysa)
            if (attempt < maxAttempts) {
                delay(1000L)
            }
        }

        // Tüm denemeler tükendi, anlaşılır Türkçe hata mesajı üret
        val friendlyMessage = when (lastException) {
            is SocketTimeoutException, is TimeoutException -> {
                "Sunucuya bağlanılamadı. İnternet bağlantınızı kontrol edip tekrar deneyin."
            }
            is UnknownHostException -> {
                "İnternet bağlantısı bulunamadı. Lütfen ağ bağlantınızı kontrol edin."
            }
            is ConnectException -> {
                "Sunucu bağlantısı kurulamadı. Lütfen internet bağlantınızı kontrol edip tekrar deneyin."
            }
            is IOException -> {
                "Sunucuya bağlanılamadı. İnternet bağlantınızı kontrol edip tekrar deneyin."
            }
            else -> {
                lastException?.message?.ifBlank { null }
                    ?: "Sunucuya bağlanılamadı. İnternet bağlantınızı kontrol edip tekrar deneyin."
            }
        }

        Log.e(TAG, "Güncelleme kontrolü başarısız oldu: $friendlyMessage", lastException)
        Result.failure(Exception(friendlyMessage, lastException))
    }

    /**
     * GitHub Releases veya commit geçmişinden gelen güncelleme notlarını
     * ReleaseNotesTranslator ile Türkçeleştirir, temizler ve anlaşılır madde imleri halinde formatlar.
     */
    fun formatChangelogToTurkish(rawNotes: String, versionName: String = ""): String {
        return ReleaseNotesTranslator.formatAsBulletsString(rawNotes, versionName)
    }

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

    // =========================================================================
    // DOWNLOADMANAGER İLE ARKA PLANDA KESİNTİSİZ İNDİRME MOTORU
    // =========================================================================

    /**
     * Android DownloadManager servisini kullanarak APK indirme işlemini başlatır.
     * Uygulama arka plana alınsa dahi indirme kesilmez ve sistem bildiriminde gösterilir.
     */
    fun startDownloadWithManager(context: Context, downloadUrl: String, versionName: String = ""): Long {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            // Varsa eski indirme dosyasını temizle
            val targetFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "skt-takip-update.apk")
            if (targetFile.exists()) {
                targetFile.delete()
            }

            val cleanVer = versionName.replace(Regex("(?i)beta|v|sürüm"), "").trim()
            val title = if (cleanVer.isNotBlank()) "SKT Takip Güncellemesi (v$cleanVer)" else "SKT Takip Güncellemesi"

            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle(title)
                setDescription("Yeni sürüm indiriliyor...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setMimeType("application/vnd.android.package-archive")
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
                setDestinationUri(Uri.fromFile(targetFile))
            }

            val downloadId = downloadManager.enqueue(request)

            // SharedPreferences'a aktif indirme ID'sini kaydet
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_DOWNLOAD_ID, downloadId)
                .putString(KEY_DOWNLOAD_VERSION, cleanVer)
                .apply()

            _downloadProgressState.value = DownloadProgressInfo(
                isDownloading = true,
                progress = 0,
                downloadedBytes = 0L,
                totalBytes = 0L
            )

            startProgressTracking(context, downloadId)
            return downloadId
        } catch (e: Exception) {
            Log.e(TAG, "DownloadManager başlatma hatası: ${e.message}", e)
            _downloadProgressState.value = DownloadProgressInfo(
                isDownloading = false,
                errorMessage = "İndirme başlatılamadı: ${e.localizedMessage}"
            )
            return -1L
        }
    }

    /**
     * Devam eden veya yeni başlatılan indirmenin durumunu ve ilerlemesini periyodik olarak sorgular.
     */
    fun startProgressTracking(context: Context, downloadId: Long) {
        progressTrackingJob?.cancel()
        val appContext = context.applicationContext

        progressTrackingJob = scope.launch {
            val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            while (isActive) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                var cursor: Cursor? = null
                try {
                    cursor = downloadManager.query(query)
                    if (cursor != null && cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                        val status = if (statusIndex >= 0) cursor.getInt(statusIndex) else -1
                        val downloaded = if (bytesDownloadedIndex >= 0) cursor.getLong(bytesDownloadedIndex) else 0L
                        val total = if (bytesTotalIndex >= 0) cursor.getLong(bytesTotalIndex) else 0L

                        val percent = if (total > 0L) {
                            ((downloaded * 100) / total).toInt().coerceIn(0, 100)
                        } else {
                            0
                        }

                        when (status) {
                            DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING -> {
                                _downloadProgressState.value = DownloadProgressInfo(
                                    isDownloading = true,
                                    progress = percent,
                                    downloadedBytes = downloaded,
                                    totalBytes = total
                                )
                            }
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                val targetFile = File(appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "skt-takip-update.apk")
                                _downloadProgressState.value = DownloadProgressInfo(
                                    isDownloading = false,
                                    progress = 100,
                                    downloadedBytes = total,
                                    totalBytes = total,
                                    isCompleted = true,
                                    downloadedFile = targetFile
                                )
                                // Otomatik kurulumu tetikle
                                if (targetFile.exists()) {
                                    installApk(appContext, targetFile)
                                }
                                break
                            }
                            DownloadManager.STATUS_FAILED -> {
                                val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                                val reason = if (reasonIndex >= 0) cursor.getInt(reasonIndex) else -1
                                _downloadProgressState.value = DownloadProgressInfo(
                                    isDownloading = false,
                                    errorMessage = "İndirme başarısız oldu (Hata Kodu: $reason)"
                                )
                                break
                            }
                        }
                    } else {
                        // İndirme kaydı bulunamadı
                        break
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "İlerleme sorgusu hatası: ${e.message}")
                } finally {
                    cursor?.close()
                }
                delay(400L)
            }
        }
    }

    /**
     * Uygulama veya Ayarlar ekranı açıldığında mevcut devam eden bir indirme olup olmadığını kontrol eder.
     */
    fun checkCurrentDownloadStatus(context: Context): DownloadProgressInfo {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val downloadId = prefs.getLong(KEY_DOWNLOAD_ID, -1L)
        if (downloadId == -1L) {
            return _downloadProgressState.value
        }

        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val statusIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val bytesDownloadedIndex = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val bytesTotalIndex = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                    val status = if (statusIndex >= 0) c.getInt(statusIndex) else -1
                    val downloaded = if (bytesDownloadedIndex >= 0) c.getLong(bytesDownloadedIndex) else 0L
                    val total = if (bytesTotalIndex >= 0) c.getLong(bytesTotalIndex) else 0L
                    val percent = if (total > 0L) ((downloaded * 100) / total).toInt().coerceIn(0, 100) else 0

                    if (status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PENDING) {
                        _downloadProgressState.value = DownloadProgressInfo(
                            isDownloading = true,
                            progress = percent,
                            downloadedBytes = downloaded,
                            totalBytes = total
                        )
                        startProgressTracking(context, downloadId)
                    } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        val targetFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "skt-takip-update.apk")
                        _downloadProgressState.value = DownloadProgressInfo(
                            isDownloading = false,
                            progress = 100,
                            downloadedBytes = total,
                            totalBytes = total,
                            isCompleted = true,
                            downloadedFile = targetFile
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Mevcut indirme kontrol hatası: ${e.message}")
        }
        return _downloadProgressState.value
    }

    /**
     * BroadcastReceiver tarafından tetiklendiğinde indirme tamamlanma işlemlerini yönetir.
     */
    fun handleDownloadComplete(context: Context, downloadId: Long) {
        val appContext = context.applicationContext
        try {
            val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val statusIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val status = if (statusIndex >= 0) c.getInt(statusIndex) else -1
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        val targetFile = File(appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "skt-takip-update.apk")
                        _downloadProgressState.value = DownloadProgressInfo(
                            isDownloading = false,
                            progress = 100,
                            isCompleted = true,
                            downloadedFile = targetFile
                        )
                        if (targetFile.exists()) {
                            installApk(appContext, targetFile)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download complete işleme hatası: ${e.message}", e)
        }
    }

    /**
     * Geriye uyumluluk için coroutine tabanlı indirme metodu.
     * DownloadManager'ı tetikler ve sonucu bekler.
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (percent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val targetFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "skt-takip-update.apk")
        val downloadId = startDownloadWithManager(context, downloadUrl)
        if (downloadId == -1L) {
            return@withContext Result.failure(Exception("DownloadManager başlatılamadı"))
        }

        // İndirme tamamlanana kadar progress callback'ini besle
        while (isActive) {
            val state = _downloadProgressState.value
            if (state.isDownloading) {
                onProgress(state.progress, state.downloadedBytes, state.totalBytes)
            }
            if (state.isCompleted && state.downloadedFile != null && state.downloadedFile.exists()) {
                return@withContext Result.success(state.downloadedFile)
            }
            if (state.errorMessage != null) {
                return@withContext Result.failure(Exception(state.errorMessage))
            }
            delay(300L)
        }
        Result.success(targetFile)
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
