package com.example.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object FontDownloader {
    const val PREFS_NAME = "skt_font_prefs"
    const val KEY_SELECTED_FONT = "selected_font"
    private const val FONTS_DIR_NAME = "fonts"

    const val FONT_DEFAULT = "Varsayılan Sistem Fontu"
    const val FONT_POPPINS = "Poppins"
    const val FONT_DM_SANS = "DM Sans"
    const val FONT_INTER = "Inter"

    val AVAILABLE_FONTS = listOf(
        FONT_DEFAULT,
        FONT_POPPINS,
        FONT_DM_SANS,
        FONT_INTER
    )

    private val _fontChangeTrigger = MutableStateFlow(0)
    val fontChangeTrigger: StateFlow<Int> = _fontChangeTrigger.asStateFlow()

    data class FontFileSpec(
        val regularFileName: String,
        val mediumFileName: String,
        val boldFileName: String,
        val regularUrls: List<String>,
        val mediumUrls: List<String>,
        val boldUrls: List<String>
    )

    fun getFontsDir(context: Context): File {
        val dir = File(context.filesDir, FONTS_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getFontFileSpec(fontName: String): FontFileSpec? {
        return when (fontName) {
            FONT_POPPINS -> FontFileSpec(
                regularFileName = "poppins_regular.ttf",
                mediumFileName = "poppins_medium.ttf",
                boldFileName = "poppins_bold.ttf",
                regularUrls = listOf(
                    "https://github.com/google/fonts/raw/main/ofl/poppins/Poppins-Regular.ttf"
                ),
                mediumUrls = listOf(
                    "https://github.com/google/fonts/raw/main/ofl/poppins/Poppins-Medium.ttf"
                ),
                boldUrls = listOf(
                    "https://github.com/google/fonts/raw/main/ofl/poppins/Poppins-Bold.ttf"
                )
            )
            FONT_DM_SANS -> FontFileSpec(
                regularFileName = "dmsans_regular.ttf",
                mediumFileName = "dmsans_medium.ttf",
                boldFileName = "dmsans_bold.ttf",
                regularUrls = listOf(
                    "https://github.com/google/fonts/raw/main/ofl/dmsans/DMSans-Regular.ttf",
                    "https://github.com/google/fonts/raw/main/ofl/dmsans/static/DMSans-Regular.ttf",
                    "https://github.com/google/fonts/raw/main/ofl/dmsans/DMSans%5Bopsz%2Cwght%5D.ttf"
                ),
                mediumUrls = listOf(
                    "https://github.com/google/fonts/raw/main/ofl/dmsans/DMSans-Medium.ttf",
                    "https://github.com/google/fonts/raw/main/ofl/dmsans/static/DMSans-Medium.ttf",
                    "https://github.com/google/fonts/raw/main/ofl/dmsans/DMSans%5Bopsz%2Cwght%5D.ttf"
                ),
                boldUrls = listOf(
                    "https://github.com/google/fonts/raw/main/ofl/dmsans/DMSans-Bold.ttf",
                    "https://github.com/google/fonts/raw/main/ofl/dmsans/static/DMSans-Bold.ttf",
                    "https://github.com/google/fonts/raw/main/ofl/dmsans/DMSans%5Bopsz%2Cwght%5D.ttf"
                )
            )
            FONT_INTER -> FontFileSpec(
                regularFileName = "inter_regular.ttf",
                mediumFileName = "inter_medium.ttf",
                boldFileName = "inter_bold.ttf",
                regularUrls = listOf(
                    "https://github.com/google/fonts/raw/main/ofl/inter/Inter-Regular.ttf",
                    "https://github.com/google/fonts/raw/main/ofl/inter/static/Inter-Regular.ttf",
                    "https://github.com/google/fonts/raw/main/ofl/inter/Inter%5Bopsz%2Cwght%5D.ttf"
                ),
                mediumUrls = listOf(
                    "https://github.com/google/fonts/raw/main/ofl/inter/Inter-Medium.ttf",
                    "https://github.com/google/fonts/raw/main/ofl/inter/static/Inter-Medium.ttf",
                    "https://github.com/google/fonts/raw/main/ofl/inter/Inter%5Bopsz%2Cwght%5D.ttf"
                ),
                boldUrls = listOf(
                    "https://github.com/google/fonts/raw/main/ofl/inter/Inter-Bold.ttf",
                    "https://github.com/google/fonts/raw/main/ofl/inter/static/Inter-Bold.ttf",
                    "https://github.com/google/fonts/raw/main/ofl/inter/Inter%5Bopsz%2Cwght%5D.ttf"
                )
            )
            else -> null
        }
    }

    fun getSelectedFont(context: Context): String {
        return runCatching {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getString(KEY_SELECTED_FONT, FONT_DEFAULT) ?: FONT_DEFAULT
        }.getOrDefault(FONT_DEFAULT)
    }

    fun saveSelectedFont(context: Context, fontName: String) {
        runCatching {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_SELECTED_FONT, fontName).apply()
            _fontChangeTrigger.value += 1
        }
    }

    fun isFontDownloaded(context: Context, fontName: String): Boolean {
        if (fontName == FONT_DEFAULT) return true
        return runCatching {
            val spec = getFontFileSpec(fontName) ?: return false
            val fontsDir = getFontsDir(context)
            val regularFile = File(fontsDir, spec.regularFileName)
            val mediumFile = File(fontsDir, spec.mediumFileName)
            val boldFile = File(fontsDir, spec.boldFileName)
            regularFile.exists() && regularFile.length() > 1024L &&
                mediumFile.exists() && mediumFile.length() > 1024L &&
                boldFile.exists() && boldFile.length() > 1024L
        }.getOrDefault(false)
    }

    suspend fun downloadFont(context: Context, fontName: String): Boolean {
        return downloadAndSaveFont(context, fontName)
    }

    suspend fun downloadAndSaveFont(context: Context, fontName: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            if (fontName == FONT_DEFAULT) {
                return@withContext true
            }

            // 1. Önce filesDir/fonts/ içinde dosyaların varlığını kontrol et.
            // Dosyalar zaten indirilmişse interneti hiç kullanmadan doğrudan başarılı dön.
            if (isFontDownloaded(context, fontName)) {
                return@withContext true
            }

            val spec = getFontFileSpec(fontName) ?: return@withContext false
            val fontsDir = getFontsDir(context)

            val regularFile = File(fontsDir, spec.regularFileName)
            val mediumFile = File(fontsDir, spec.mediumFileName)
            val boldFile = File(fontsDir, spec.boldFileName)

            val regularOk = downloadFirstAvailableUrl(spec.regularUrls, regularFile)
            val mediumOk = downloadFirstAvailableUrl(spec.mediumUrls, mediumFile)
            val boldOk = downloadFirstAvailableUrl(spec.boldUrls, boldFile)

            if (regularOk && mediumOk && boldOk) {
                true
            } else {
                regularFile.delete()
                mediumFile.delete()
                boldFile.delete()
                false
            }
        }.getOrDefault(false)
    }

    private fun downloadFirstAvailableUrl(urls: List<String>, targetFile: File): Boolean {
        for (urlString in urls) {
            if (downloadSingleFile(urlString, targetFile)) {
                return true
            }
        }
        return false
    }

    private fun downloadSingleFile(urlString: String, targetFile: File): Boolean {
        val parentDir = targetFile.parentFile ?: return false
        if (!parentDir.exists()) {
            parentDir.mkdirs()
        }
        val tempFile = File(parentDir, "${targetFile.name}.tmp")
        var connection: HttpURLConnection? = null
        return try {
            var currentUrl = urlString
            var redirects = 0
            while (redirects < 5) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 12000
                    readTimeout = 15000
                    instanceFollowRedirects = true
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                }
                connection.connect()
                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location") ?: break
                    connection.disconnect()
                    currentUrl = location
                    redirects++
                } else {
                    break
                }
            }

            val finalConnection = connection ?: return false
            if (finalConnection.responseCode != HttpURLConnection.HTTP_OK) {
                return false
            }

            finalConnection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                    output.flush()
                }
            }

            if (tempFile.exists() && tempFile.length() > 1024L) {
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                tempFile.renameTo(targetFile)
            } else {
                tempFile.delete()
                false
            }
        } catch (_: Exception) {
            tempFile.delete()
            false
        } finally {
            connection?.disconnect()
        }
    }
}
