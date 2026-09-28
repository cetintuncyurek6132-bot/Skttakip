package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupExportHelper {

    fun generateBackupFileName(prefix: String = "skt_takip_yedek"): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        return "${prefix}_$timeStamp.json"
    }

    /**
     * Saves JSON backup string directly to the device's public Downloads directory.
     * Uses MediaStore on Android 10+ (API 29+) and direct File in Environment.DIRECTORY_DOWNLOADS on older devices.
     * Returns a Result containing the display path or file name.
     */
    fun saveJsonToDownloads(context: Context, jsonString: String, fileName: String): Result<String> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return Result.failure(Exception("Downloads klasöründe dosya oluşturulamadı."))

                resolver.openOutputStream(uri, "wt")?.use { os ->
                    os.write(jsonString.toByteArray(Charsets.UTF_8))
                    os.flush()
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                Result.success("İndirilenler (Downloads)/$fileName")
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val targetFile = File(downloadsDir, fileName)
                targetFile.writeText(jsonString, Charsets.UTF_8)
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf("application/json"),
                    null
                )
                Result.success("İndirilenler/$fileName")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Writes JSON string to a Uri obtained from ActivityResultContracts.CreateDocument.
     */
    fun writeJsonToUri(context: Context, uri: Uri, jsonString: String): Result<Unit> {
        return try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { os ->
                os.write(jsonString.toByteArray(Charsets.UTF_8))
                os.flush()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Shares JSON backup file via Android Share Chooser (FileProvider).
     */
    fun shareJsonBackup(context: Context, jsonString: String, fileName: String): Result<Unit> {
        return try {
            val cacheFile = File(context.cacheDir, fileName)
            cacheFile.writeText(jsonString, Charsets.UTF_8)

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "SKT Takip Tam Sistem Yedeği ($fileName)")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "SKT Takip uygulaması tam veri yedeği (Ürünler, SKT'ler, Fiyatlar, Takip ve Sayımlar)."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Yedeği Paylaş / Dışa Aktar")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
