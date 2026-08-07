package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
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

    fun shareProductsAsImage(
        context: Context,
        filterLabel: String,
        searchQuery: String,
        productList: List<Product>
    ) {
        if (productList.isEmpty()) return

        val width = 1080
        val headerHeight = 210
        val itemHeight = 140
        val footerHeight = 80
        val totalHeight = headerHeight + (productList.size * itemHeight) + footerHeight

        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        canvas.drawColor(Color.parseColor("#F8FAFC")) // Slate50 background

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rectF = RectF()

        // 1. HEADER BANNER
        paint.color = Color.parseColor("#00B4D8") // Turquoise primary header
        rectF.set(0f, 0f, width.toFloat(), headerHeight.toFloat() - 15f)
        canvas.drawRect(rectF, paint)

        // Header Title
        paint.color = Color.WHITE
        paint.textSize = 36f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("📋 SKT TAKİP & STOK - EKİP RAPORU", 40f, 65f, paint)

        // Category & Date Info
        paint.textSize = 24f
        paint.typeface = Typeface.DEFAULT
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))
        val dateStr = sdf.format(Date())

        canvas.drawText("📂 Kategori: $filterLabel", 40f, 110f, paint)
        if (searchQuery.isNotBlank()) {
            val truncatedSearch = truncateText(paint, searchQuery, 450f)
            canvas.drawText("🔍 Arama Filtresi: \"$truncatedSearch\"", 40f, 148f, paint)
            canvas.drawText("📅 Tarih: $dateStr  •  📊 Toplam: ${productList.size} Ürün", 40f, 185f, paint)
        } else {
            canvas.drawText("📅 Tarih: $dateStr  •  📊 Toplam: ${productList.size} Ürün", 40f, 155f, paint)
        }

        // 2. PRODUCT CARDS
        var currentY = headerHeight.toFloat()

        productList.forEachIndexed { index, p ->
            val cardTop = currentY + 8f
            val cardBottom = currentY + itemHeight - 8f
            val cardLeft = 30f
            val cardRight = width.toFloat() - 30f

            // White Card Background
            paint.color = Color.WHITE
            rectF.set(cardLeft, cardTop, cardRight, cardBottom)
            canvas.drawRoundRect(rectF, 16f, 16f, paint)

            // Border
            paint.color = Color.parseColor("#E2E8F0")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawRoundRect(rectF, 16f, 16f, paint)
            paint.style = Paint.Style.FILL

            // Expiry calculation
            val days = p.getRemainingDays()
            val (accentColor, statusText) = when {
                days <= 0 -> Color.parseColor("#EF4444") to "SÜRESİ GEÇTİ"
                days <= 7 -> Color.parseColor("#F97316") to "$days GÜN KALDI"
                days <= 30 -> Color.parseColor("#F59E0B") to "$days GÜN KALDI"
                else -> Color.parseColor("#10B981") to "$days GÜN KALDI"
            }

            // Left colored indicator bar
            paint.color = accentColor
            rectF.set(cardLeft, cardTop, cardLeft + 14f, cardBottom)
            canvas.drawRoundRect(rectF, 16f, 0f, paint)

            // Max Title Width calculation to prevent overlapping with right side badges
            val badgeXStart = cardRight - 230f
            val titleXStart = cardLeft + 30f
            val maxTitleWidth = badgeXStart - titleXStart - 15f

            // Product Title (Truncated if long)
            paint.color = Color.parseColor("#0F172A") // Slate900
            paint.textSize = 28f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val fullTitle = "${index + 1}. ${p.urunAdi}"
            val safeTitle = truncateText(paint, fullTitle, maxTitleWidth)
            canvas.drawText(safeTitle, titleXStart, cardTop + 42f, paint)

            // Product Code & Category
            paint.color = Color.parseColor("#64748B") // Slate500
            paint.textSize = 21f
            paint.typeface = Typeface.DEFAULT
            val codeDisplay = if (p.urunKodu.isNotBlank()) p.urunKodu else p.barkod
            val subInfo = "🏷️ KOD: $codeDisplay  •  📁 ${p.kategori}"
            val safeSubInfo = truncateText(paint, subInfo, maxTitleWidth)
            canvas.drawText(safeSubInfo, titleXStart, cardTop + 76f, paint)

            // SKT Date
            val sktDateStr = try {
                val cal = Calendar.getInstance().apply { timeInMillis = p.sktTarihi }
                SimpleDateFormat("dd.MM.yyyy", Locale("tr", "TR")).format(cal.time)
            } catch (e: Exception) { "-" }

            paint.color = Color.parseColor("#334155")
            canvas.drawText("🗓️ SKT: $sktDateStr", titleXStart, cardTop + 110f, paint)

            // Expiry status badge (Right side top)
            paint.color = accentColor
            rectF.set(cardRight - 225f, cardTop + 12f, cardRight - 20f, cardTop + 54f)
            canvas.drawRoundRect(rectF, 12f, 12f, paint)

            paint.color = Color.WHITE
            paint.textSize = 19f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val textWidth = paint.measureText(statusText)
            canvas.drawText(statusText, (cardRight - 225f + cardRight - 20f) / 2f - textWidth / 2f, cardTop + 40f, paint)

            // Stock Count Badge (Right side bottom)
            paint.color = Color.parseColor("#0284C7") // Sky blue
            rectF.set(cardRight - 225f, cardTop + 62f, cardRight - 20f, cardTop + 106f)
            canvas.drawRoundRect(rectF, 12f, 12f, paint)

            paint.color = Color.WHITE
            paint.textSize = 21f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val stockText = "📦 ${p.stokAdedi} Adet"
            val stockTextWidth = paint.measureText(stockText)
            canvas.drawText(stockText, (cardRight - 225f + cardRight - 20f) / 2f - stockTextWidth / 2f, cardTop + 92f, paint)

            currentY += itemHeight
        }

        // 3. FOOTER
        paint.color = Color.parseColor("#334155")
        rectF.set(0f, currentY + 5f, width.toFloat(), totalHeight.toFloat())
        canvas.drawRect(rectF, paint)

        paint.color = Color.WHITE
        paint.textSize = 21f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("⚡ SKT Takip & Stok Yönetim Sistemi — Ekip Paylaşım Raporu", 40f, currentY + 48f, paint)

        saveAndShareBitmap(context, bitmap, "skt_urun_listesi")
    }

    fun shareTourReportAsImage(
        context: Context,
        rapor: com.example.data.TurRaporu,
        logs: List<com.example.data.TurKontrolKaydi> = emptyList()
    ) {
        val width = 1080
        val headerHeight = 220
        val summaryHeight = 160
        val itemHeight = 120
        val footerHeight = 80
        val totalHeight = headerHeight + summaryHeight + (logs.size * itemHeight) + footerHeight

        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        canvas.drawColor(Color.parseColor("#0F172A")) // Dark Navy background

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rectF = RectF()

        // 1. HEADER BANNER
        paint.color = Color.parseColor("#00B4D8") // Turquoise primary
        rectF.set(0f, 0f, width.toFloat(), headerHeight.toFloat() - 15f)
        canvas.drawRect(rectF, paint)

        paint.color = Color.WHITE
        paint.textSize = 36f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("📋 SABAH KONTROL TURU RAPORU", 40f, 65f, paint)

        paint.textSize = 24f
        paint.typeface = Typeface.DEFAULT
        val sdf = SimpleDateFormat("dd MMMM yyyy - HH:mm", Locale("tr", "TR"))
        val dateStr = sdf.format(Date(rapor.turTarihi))
        canvas.drawText("📂 Reyon: ${rapor.hedefReyon}  •  Status: ${if (rapor.tamamlandiMi) "TAMAMLANDI" else "YARIM KALDI"}", 40f, 115f, paint)
        canvas.drawText("📅 Tarih: $dateStr  •  📊 Toplam ${rapor.toplamUrunSayisi} Ürün Denetlendi", 40f, 160f, paint)

        // 2. STATS SUMMARY ROW
        var currentY = headerHeight.toFloat()
        val cardWidth = (width.toFloat() - 100f) / 3f

        // SATILDI STAT
        rectF.set(30f, currentY + 10f, 30f + cardWidth, currentY + summaryHeight - 10f)
        paint.color = Color.parseColor("#10B981") // Green
        canvas.drawRoundRect(rectF, 16f, 16f, paint)
        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SATILDI", 50f, currentY + 45f, paint)
        paint.textSize = 32f
        canvas.drawText("${rapor.satilanUrunSayisi} Ürün", 50f, currentY + 90f, paint)
        paint.textSize = 20f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("(${rapor.toplamSatilanAdet} Adet)", 50f, currentY + 125f, paint)

        // FIRE STAT
        rectF.set(50f + cardWidth, currentY + 10f, 50f + (cardWidth * 2), currentY + summaryHeight - 10f)
        paint.color = Color.parseColor("#EF4444") // Red
        canvas.drawRoundRect(rectF, 16f, 16f, paint)
        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("FİRE", 70f + cardWidth, currentY + 45f, paint)
        paint.textSize = 32f
        canvas.drawText("${rapor.fireUrunSayisi} Ürün", 70f + cardWidth, currentY + 90f, paint)
        paint.textSize = 20f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("(${rapor.toplamFireAdet} Adet)", 70f + cardWidth, currentY + 125f, paint)

        // NOTR STAT
        rectF.set(70f + (cardWidth * 2), currentY + 10f, width.toFloat() - 30f, currentY + summaryHeight - 10f)
        paint.color = Color.parseColor("#334155") // Slate
        canvas.drawRoundRect(rectF, 16f, 16f, paint)
        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("NÖTR", 90f + (cardWidth * 2), currentY + 45f, paint)
        paint.textSize = 32f
        canvas.drawText("${rapor.notrUrunSayisi} Ürün", 90f + (cardWidth * 2), currentY + 90f, paint)
        paint.textSize = 20f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Değişiklik Yok", 90f + (cardWidth * 2), currentY + 125f, paint)

        currentY += summaryHeight

        // 3. LOG ITEMS
        logs.forEachIndexed { index, log ->
            val cardTop = currentY + 5f
            val cardBottom = currentY + itemHeight - 5f
            val cardLeft = 30f
            val cardRight = width.toFloat() - 30f

            rectF.set(cardLeft, cardTop, cardRight, cardBottom)
            paint.color = Color.parseColor("#1E293B")
            canvas.drawRoundRect(rectF, 12f, 12f, paint)

            paint.color = Color.WHITE
            paint.textSize = 26f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val title = "${index + 1}. ${log.urunAdiSnapshot}"
            val safeTitle = truncateText(paint, title, 650f)
            canvas.drawText(safeTitle, cardLeft + 25f, cardTop + 42f, paint)

            if (log.barkodSnapshot.isNotBlank()) {
                paint.color = Color.parseColor("#94A3B8")
                paint.textSize = 20f
                paint.typeface = Typeface.DEFAULT
                canvas.drawText("Barkod: ${log.barkodSnapshot}", cardLeft + 25f, cardTop + 80f, paint)
            }

            // Status Badge
            val badgeColor = when (log.durum) {
                "SATILDI" -> Color.parseColor("#10B981")
                "FIRE" -> Color.parseColor("#EF4444")
                else -> Color.parseColor("#64748B")
            }
            rectF.set(cardRight - 220f, cardTop + 15f, cardRight - 15f, cardBottom - 15f)
            paint.color = badgeColor
            canvas.drawRoundRect(rectF, 10f, 10f, paint)

            paint.color = Color.WHITE
            paint.textSize = 22f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val badgeLabel = when (log.durum) {
                "SATILDI" -> "SAT: ${log.islemAdedi}"
                "FIRE" -> "FİRE: ${log.islemAdedi}"
                else -> "NÖTR"
            }
            val bWidth = paint.measureText(badgeLabel)
            canvas.drawText(badgeLabel, (cardRight - 220f + cardRight - 15f) / 2f - bWidth / 2f, cardTop + 55f, paint)

            currentY += itemHeight
        }

        // FOOTER
        paint.color = Color.parseColor("#0284C7")
        rectF.set(0f, currentY + 5f, width.toFloat(), totalHeight.toFloat())
        canvas.drawRect(rectF, paint)

        paint.color = Color.WHITE
        paint.textSize = 21f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("⚡ SKT Takip & Stok Yönetim Sistemi — Sabah Kontrol Turu Raporu", 40f, currentY + 48f, paint)

        saveAndShareBitmap(context, bitmap, "tur_raporu")
    }

    private fun saveAndShareBitmap(context: Context, bitmap: Bitmap, fileNamePrefix: String) {
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
        }
    }
}
