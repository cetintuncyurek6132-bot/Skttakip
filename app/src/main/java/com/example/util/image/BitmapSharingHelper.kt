package com.example.util.image

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object BitmapSharingHelper {

    fun shareBitmap(context: Context, bitmap: Bitmap, fileNamePrefix: String = "skt_rapor") {
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val imageFile = File(cachePath, "${fileNamePrefix}_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )

            if (contentUri != null) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val whatsappIntent = Intent(shareIntent).apply {
                    setPackage("com.whatsapp")
                }

                try {
                    context.startActivity(whatsappIntent)
                } catch (e: Exception) {
                    val chooser = Intent.createChooser(shareIntent, "Rapor Görselini Paylaş")
                    chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(chooser)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Paylaşım hatası: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareBitmapWithText(
        context: Context,
        bitmap: Bitmap,
        textMessage: String = "",
        fileNamePrefix: String = "skt_onizleme"
    ) {
        // Strict requirement: ONLY image is shared, no automated text / caption sent to WhatsApp
        shareBitmap(context, bitmap, fileNamePrefix)
    }

    fun shareBitmapsToWhatsApp(
        context: Context,
        bitmaps: List<Bitmap>,
        fileNamePrefix: String = "skt_paylasim"
    ) {
        if (bitmaps.isEmpty()) return
        try {
            val cachePath = File(context.cacheDir, "whatsapp_share")
            if (cachePath.exists()) {
                cachePath.listFiles()?.forEach { it.delete() }
            }
            cachePath.mkdirs()

            val uris = ArrayList<android.net.Uri>()
            val timestamp = System.currentTimeMillis()

            bitmaps.forEachIndexed { index, bmp ->
                val imageFile = File(cachePath, "${fileNamePrefix}_${timestamp}_p${index + 1}.png")
                FileOutputStream(imageFile).use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )
                uris.add(contentUri)
            }

            if (uris.isEmpty()) return

            val shareIntent = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uris[0])
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "image/png"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            val whatsappIntent = Intent(shareIntent).apply {
                setPackage("com.whatsapp")
            }

            try {
                context.startActivity(whatsappIntent)
            } catch (e: Exception) {
                // Try WhatsApp Business as secondary
                try {
                    val w4bIntent = Intent(shareIntent).apply {
                        setPackage("com.whatsapp.w4b")
                    }
                    context.startActivity(w4bIntent)
                } catch (e2: Exception) {
                    val chooser = Intent.createChooser(shareIntent, "Görselleri Paylaş")
                    chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(chooser)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Paylaşım hatası: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileNamePrefix: String = "skt_rapor") {
        try {
            val fileName = "${fileNamePrefix}_${System.currentTimeMillis()}.png"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SKT_Takip")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                Toast.makeText(context, "Görsel galeriye kaydedildi", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Görsel kaydedilemedi", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Hata: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
