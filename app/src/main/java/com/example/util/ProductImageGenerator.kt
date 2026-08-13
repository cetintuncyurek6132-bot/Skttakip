package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.Product
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ProductImageGenerator {

    private fun truncateText(paint: Paint, text: String, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var truncated = text
        while (truncated.isNotEmpty() && paint.measureText("$truncated...") > maxWidth) {
            truncated = truncated.dropLast(1)
        }
        return "$truncated..."
    }

    private fun drawChecklistIcon(canvas: Canvas, paint: Paint, left: Float, top: Float, size: Float) {
        // Draw icon container background (semi-transparent white)
        val iconBox = RectF(left, top, left + size, top + size)
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(40, 255, 255, 255)
        canvas.drawRoundRect(iconBox, size * 0.25f, size * 0.25f, paint)

        // Draw checklist lines & checkmarks
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3.5f
        paint.strokeCap = Paint.Cap.ROUND

        val padding = size * 0.22f
        val innerW = size - (padding * 2f)
        val lineSpacing = innerW / 2.5f

        for (i in 0..2) {
            val y = top + padding + (i * lineSpacing)
            // Checkmark
            canvas.drawLine(left + padding, y + 2f, left + padding + 6f, y + 8f, paint)
            canvas.drawLine(left + padding + 6f, y + 8f, left + padding + 16f, y - 4f, paint)
            // Line
            canvas.drawLine(left + padding + 22f, y + 2f, left + size - padding, y + 2f, paint)
        }

        paint.style = Paint.Style.FILL
    }

    fun createProductsBitmap(
        filterLabel: String,
        searchQuery: String,
        productList: List<Product>
    ): Bitmap? {
        if (productList.isEmpty()) return null

        val width = 1080
        val headerHeight = 220
        val itemHeight = 160
        val footerHeight = 100
        val totalHeight = headerHeight + (productList.size * itemHeight) + footerHeight

        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 0. Background Canvas - Slate 50 Neutral
        canvas.drawColor(Color.parseColor("#F8FAFC"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rectF = RectF()

        // 1. HEADER BANNER WITH GRADIENT (#0891B2 Turquoise -> #0F172A Dark Navy)
        val headerShader = LinearGradient(
            0f, 0f, 0f, headerHeight.toFloat(),
            intArrayOf(Color.parseColor("#0891B2"), Color.parseColor("#0E7490"), Color.parseColor("#0F172A")),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = headerShader
        rectF.set(0f, 0f, width.toFloat(), headerHeight.toFloat())
        canvas.drawRect(rectF, paint)
        paint.shader = null

        // Header Checklist Icon
        drawChecklistIcon(canvas, paint, 36f, 36f, 72f)

        // Brand Label
        paint.color = Color.parseColor("#BAE6FD") // Sky 200 light turquoise
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SKT TAKİP & STOK YÖNETİMİ", 124f, 62f, paint)

        // Main Title
        paint.color = Color.WHITE
        paint.textSize = 34f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Ekip Paylaşım Raporu", 124f, 102f, paint)

        // Single-line Meta Info Bar
        paint.color = Color.parseColor("#E0F2FE") // Sky 100
        paint.textSize = 21f
        paint.typeface = Typeface.DEFAULT
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR"))
        val dateStr = sdf.format(Date())

        val metaText = if (searchQuery.isNotBlank()) {
            val truncatedSearch = truncateText(paint, searchQuery, 300f)
            "Kategori: $filterLabel   |   Filtre: \"$truncatedSearch\"   |   Tarih: $dateStr   |   Toplam: ${productList.size} Ürün"
        } else {
            "Kategori: $filterLabel   |   Tarih: $dateStr   |   Toplam: ${productList.size} Ürün"
        }
        val safeMetaText = truncateText(paint, metaText, width - 72f)
        canvas.drawText(safeMetaText, 36f, 172f, paint)

        // Subtle Header Bottom Line Separator
        paint.color = Color.parseColor("#38BDF8")
        paint.alpha = 80
        canvas.drawLine(0f, headerHeight.toFloat() - 2f, width.toFloat(), headerHeight.toFloat() - 2f, paint)
        paint.alpha = 255

        // 2. PRODUCT CARDS
        var currentY = headerHeight.toFloat()

        productList.forEachIndexed { index, p ->
            val cardTop = currentY + 10f
            val cardBottom = currentY + itemHeight - 10f
            val cardLeft = 32f
            val cardRight = width.toFloat() - 32f

            val days = p.getRemainingDays()

            val bgColor: Int
            val borderColor: Int
            val badgeBgColor: Int
            val statusText: String

            when {
                days <= 0 -> {
                    bgColor = Color.parseColor("#FEF2F2") // Soft Red Tint
                    borderColor = Color.parseColor("#FECACA")
                    badgeBgColor = Color.parseColor("#DC2626") // Crimson Red
                    statusText = "SÜRESİ GEÇTİ"
                }
                days <= 7 -> {
                    bgColor = Color.parseColor("#FFF7ED") // Soft Orange Tint
                    borderColor = Color.parseColor("#FED7AA")
                    badgeBgColor = Color.parseColor("#EA580C") // Deep Orange
                    statusText = "$days GÜN KALDI"
                }
                days <= 30 -> {
                    bgColor = Color.parseColor("#FEFCE8") // Soft Amber Tint
                    borderColor = Color.parseColor("#FDE68A")
                    badgeBgColor = Color.parseColor("#D97706") // Amber
                    statusText = "$days GÜN KALDI"
                }
                else -> {
                    bgColor = Color.parseColor("#FFFFFF") // Pure White
                    borderColor = Color.parseColor("#E2E8F0")
                    badgeBgColor = Color.parseColor("#16A34A") // Emerald Green
                    statusText = "$days GÜN KALDI"
                }
            }

            // Card Shadow
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#0F172A")
            paint.alpha = 10
            rectF.set(cardLeft + 2f, cardTop + 4f, cardRight - 2f, cardBottom + 4f)
            canvas.drawRoundRect(rectF, 20f, 20f, paint)
            paint.alpha = 255

            // Card Background (Full Light Tint)
            paint.color = bgColor
            rectF.set(cardLeft, cardTop, cardRight, cardBottom)
            canvas.drawRoundRect(rectF, 20f, 20f, paint)

            // Card Border
            paint.color = borderColor
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.5f
            canvas.drawRoundRect(rectF, 20f, 20f, paint)
            paint.style = Paint.Style.FILL

            // Right Badges Column Layout
            val badgeRight = cardRight - 20f
            val badgeLeft = cardRight - 230f
            val badgeWidth = badgeRight - badgeLeft

            // Expiry Status Badge (Top Pill)
            rectF.set(badgeLeft, cardTop + 16f, badgeRight, cardTop + 64f)
            paint.color = badgeBgColor
            canvas.drawRoundRect(rectF, 24f, 24f, paint)

            paint.color = Color.WHITE
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val stWidth = paint.measureText(statusText)
            canvas.drawText(statusText, badgeLeft + (badgeWidth - stWidth) / 2f, cardTop + 46f, paint)

            // Stock Count Badge (Bottom Pill - Neutral Slate Tone)
            rectF.set(badgeLeft, cardTop + 76f, badgeRight, cardTop + 124f)
            paint.color = Color.parseColor("#334155") // Slate 700
            canvas.drawRoundRect(rectF, 24f, 24f, paint)

            val stockText = "STOK: ${p.stokAdedi} ADET"
            paint.color = Color.WHITE
            paint.textSize = 19f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val stockWidth = paint.measureText(stockText)
            canvas.drawText(stockText, badgeLeft + (badgeWidth - stockWidth) / 2f, cardTop + 106f, paint)

            // Left Info Content Area
            val titleXStart = cardLeft + 28f
            val maxTitleWidth = badgeLeft - titleXStart - 20f

            // Product Name
            paint.color = Color.parseColor("#0F172A") // Slate 900
            paint.textSize = 28f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val fullTitle = "${index + 1}. ${p.urunAdi}"
            val safeTitle = truncateText(paint, fullTitle, maxTitleWidth)
            canvas.drawText(safeTitle, titleXStart, cardTop + 45f, paint)

            // Product Code & Category
            paint.color = Color.parseColor("#64748B") // Slate 500
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val codeDisplay = if (p.urunKodu.isNotBlank()) p.urunKodu else p.barkod
            val subInfo = "KOD: $codeDisplay   •   KAT: ${p.kategori}"
            val safeSubInfo = truncateText(paint, subInfo, maxTitleWidth)
            canvas.drawText(safeSubInfo, titleXStart, cardTop + 82f, paint)

            // SKT Date
            val sktDateStr = try {
                val cal = Calendar.getInstance().apply { timeInMillis = p.sktTarihi }
                SimpleDateFormat("dd.MM.yyyy", Locale.forLanguageTag("tr-TR")).format(cal.time)
            } catch (e: Exception) { "-" }

            paint.color = Color.parseColor("#1E293B") // Slate 800
            paint.textSize = 22f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("SKT: $sktDateStr", titleXStart, cardTop + 118f, paint)

            currentY += itemHeight
        }

        // 3. FOOTER AREA
        paint.color = Color.parseColor("#E2E8F0")
        paint.strokeWidth = 2f
        canvas.drawLine(32f, currentY + 20f, width.toFloat() - 32f, currentY + 20f, paint)

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val footerLeft = "SKT Takip & Stok Yönetim Sistemi   •   Oluşturuldu: $dateStr"
        canvas.drawText(footerLeft, 36f, currentY + 62f, paint)

        paint.color = Color.parseColor("#0EA5B7")
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val footerRight = "EKİP RAPORU"
        val frWidth = paint.measureText(footerRight)
        canvas.drawText(footerRight, width.toFloat() - 36f - frWidth, currentY + 62f, paint)

        return bitmap
    }

    fun shareProductsAsImage(
        context: Context,
        filterLabel: String,
        searchQuery: String,
        productList: List<Product>
    ) {
        val bitmap = createProductsBitmap(filterLabel, searchQuery, productList) ?: return
        shareBitmap(context, bitmap, "skt_urun_listesi")
    }

    fun createTourReportBitmap(
        rapor: com.example.data.TurRaporu,
        logs: List<com.example.data.TurKontrolKaydi> = emptyList()
    ): Bitmap {
        val width = 1080
        val headerHeight = 220
        val summaryHeight = 160
        val itemHeight = 140
        val footerHeight = 100
        val totalHeight = headerHeight + summaryHeight + (logs.size * itemHeight) + footerHeight

        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        canvas.drawColor(Color.parseColor("#F8FAFC"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rectF = RectF()

        // 1. HEADER BANNER WITH GRADIENT
        val headerShader = LinearGradient(
            0f, 0f, 0f, headerHeight.toFloat(),
            intArrayOf(Color.parseColor("#0891B2"), Color.parseColor("#0E7490"), Color.parseColor("#0F172A")),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = headerShader
        rectF.set(0f, 0f, width.toFloat(), headerHeight.toFloat())
        canvas.drawRect(rectF, paint)
        paint.shader = null

        // Header Icon
        drawChecklistIcon(canvas, paint, 36f, 36f, 72f)

        paint.color = Color.parseColor("#BAE6FD")
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SKT TAKİP & STOK YÖNETİMİ", 124f, 62f, paint)

        paint.color = Color.WHITE
        paint.textSize = 34f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Sabah Kontrol Turu Raporu", 124f, 102f, paint)

        val sdf = SimpleDateFormat("dd MMMM yyyy - HH:mm", Locale.forLanguageTag("tr-TR"))
        val dateStr = sdf.format(Date(rapor.turTarihi))
        val statusLabel = if (rapor.tamamlandiMi) "TAMAMLANDI" else "YARIM KALDI"

        paint.color = Color.parseColor("#E0F2FE")
        paint.textSize = 21f
        paint.typeface = Typeface.DEFAULT
        val metaStr = "Reyon: ${rapor.hedefReyon}   |   Durum: $statusLabel   |   Tarih: $dateStr"
        val safeMetaStr = truncateText(paint, metaStr, width - 72f)
        canvas.drawText(safeMetaStr, 36f, 172f, paint)

        // 2. STATS SUMMARY ROW
        var currentY = headerHeight.toFloat()
        val cardWidth = (width.toFloat() - 96f) / 3f

        // SATILDI STAT
        rectF.set(32f, currentY + 12f, 32f + cardWidth, currentY + summaryHeight - 12f)
        paint.color = Color.parseColor("#F0FDF4") // Soft Green Tint
        canvas.drawRoundRect(rectF, 16f, 16f, paint)
        paint.color = Color.parseColor("#BBF7D0")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(rectF, 16f, 16f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#16A34A")
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SATILDI", 52f, currentY + 48f, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 30f
        canvas.drawText("${rapor.satilanUrunSayisi} Ürün", 52f, currentY + 90f, paint)

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 19f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("(${rapor.toplamSatilanAdet} Adet)", 52f, currentY + 122f, paint)

        // FIRE STAT
        val fireLeft = 48f + cardWidth
        rectF.set(fireLeft, currentY + 12f, fireLeft + cardWidth, currentY + summaryHeight - 12f)
        paint.color = Color.parseColor("#FEF2F2") // Soft Red Tint
        canvas.drawRoundRect(rectF, 16f, 16f, paint)
        paint.color = Color.parseColor("#FECACA")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(rectF, 16f, 16f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#DC2626")
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("FİRE", fireLeft + 20f, currentY + 48f, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 30f
        canvas.drawText("${rapor.fireUrunSayisi} Ürün", fireLeft + 20f, currentY + 90f, paint)

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 19f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("(${rapor.toplamFireAdet} Adet)", fireLeft + 20f, currentY + 122f, paint)

        // NOTR STAT
        val notrLeft = 64f + (cardWidth * 2)
        rectF.set(notrLeft, currentY + 12f, width.toFloat() - 32f, currentY + summaryHeight - 12f)
        paint.color = Color.parseColor("#FFFFFF") // White Tint
        canvas.drawRoundRect(rectF, 16f, 16f, paint)
        paint.color = Color.parseColor("#E2E8F0")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(rectF, 16f, 16f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#475569")
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("NÖTR", notrLeft + 20f, currentY + 48f, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 30f
        canvas.drawText("${rapor.notrUrunSayisi} Ürün", notrLeft + 20f, currentY + 90f, paint)

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 19f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Değişiklik Yok", notrLeft + 20f, currentY + 122f, paint)

        currentY += summaryHeight

        // 3. LOG ITEMS
        logs.forEachIndexed { index, log ->
            val cardTop = currentY + 8f
            val cardBottom = currentY + itemHeight - 8f
            val cardLeft = 32f
            val cardRight = width.toFloat() - 32f

            val (bgColor, borderColor, badgeColor, badgeLabel) = when (log.durum) {
                "SATILDI" -> Quadruple(
                    Color.parseColor("#F0FDF4"),
                    Color.parseColor("#BBF7D0"),
                    Color.parseColor("#16A34A"),
                    "SAT: ${log.islemAdedi}"
                )
                "FIRE" -> Quadruple(
                    Color.parseColor("#FEF2F2"),
                    Color.parseColor("#FECACA"),
                    Color.parseColor("#DC2626"),
                    "FİRE: ${log.islemAdedi}"
                )
                else -> Quadruple(
                    Color.parseColor("#FFFFFF"),
                    Color.parseColor("#E2E8F0"),
                    Color.parseColor("#475569"),
                    "NÖTR"
                )
            }

            rectF.set(cardLeft, cardTop, cardRight, cardBottom)
            paint.color = bgColor
            canvas.drawRoundRect(rectF, 16f, 16f, paint)

            paint.color = borderColor
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawRoundRect(rectF, 16f, 16f, paint)
            paint.style = Paint.Style.FILL

            // Product Name
            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 26f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val title = "${index + 1}. ${log.urunAdiSnapshot}"
            val safeTitle = truncateText(paint, title, cardRight - cardLeft - 260f)
            canvas.drawText(safeTitle, cardLeft + 24f, cardTop + 46f, paint)

            // Barcode
            if (log.barkodSnapshot.isNotBlank()) {
                paint.color = Color.parseColor("#64748B")
                paint.textSize = 20f
                paint.typeface = Typeface.DEFAULT
                canvas.drawText("Barkod: ${log.barkodSnapshot}", cardLeft + 24f, cardTop + 84f, paint)
            }

            // Status Badge
            rectF.set(cardRight - 220f, cardTop + 20f, cardRight - 20f, cardBottom - 20f)
            paint.color = badgeColor
            canvas.drawRoundRect(rectF, 20f, 20f, paint)

            paint.color = Color.WHITE
            paint.textSize = 22f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val bWidth = paint.measureText(badgeLabel)
            canvas.drawText(badgeLabel, cardRight - 120f - (bWidth / 2f), cardTop + 68f, paint)

            currentY += itemHeight
        }

        // 4. FOOTER AREA
        paint.color = Color.parseColor("#E2E8F0")
        paint.strokeWidth = 2f
        canvas.drawLine(32f, currentY + 20f, width.toFloat() - 32f, currentY + 20f, paint)

        val formattedNow = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR")).format(Date())
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("SKT Takip & Stok Yönetim Sistemi   •   Oluşturuldu: $formattedNow", 36f, currentY + 62f, paint)

        paint.color = Color.parseColor("#0EA5B7")
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val footerRight = "KONTROL RAPORU"
        val frWidth = paint.measureText(footerRight)
        canvas.drawText(footerRight, width.toFloat() - 36f - frWidth, currentY + 62f, paint)

        return bitmap
    }

    fun shareTourReportAsImage(
        context: Context,
        rapor: com.example.data.TurRaporu,
        logs: List<com.example.data.TurKontrolKaydi> = emptyList()
    ) {
        val bitmap = createTourReportBitmap(rapor, logs)
        shareBitmap(context, bitmap, "tur_raporu")
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    fun shareBitmap(context: Context, bitmap: Bitmap, fileNamePrefix: String) {
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
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    type = "image/png"
                }

                val whatsappIntent = Intent(shareIntent).apply {
                    setPackage("com.whatsapp")
                }

                try {
                    context.startActivity(whatsappIntent)
                } catch (e: Exception) {
                    val chooser = Intent.createChooser(shareIntent, "Görseli Paylaş (WhatsApp / Ekip)")
                    context.startActivity(chooser)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Paylaşım hatası: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileNamePrefix: String) {
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
