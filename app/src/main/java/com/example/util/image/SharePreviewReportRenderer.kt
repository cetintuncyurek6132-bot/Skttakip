package com.example.util.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.data.Product
import com.example.data.getDisplayName
import com.example.util.ProductImageGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SharePreviewReportRenderer {

    private data class SummaryCardData(
        val label: String,
        val value: String,
        val colHex: String,
        val bgHex: String,
        val borderHex: String
    )

    fun createSharePreviewReportBitmap(
        title: String = "KONTROL EDİLECEK ÜRÜNLER",
        note: String = "",
        productList: List<Product>
    ): Bitmap {
        val width = 1080
        val headerHeight = 310
        val itemHeight = 185
        val noteHeight = if (note.isNotBlank()) 140 else 0
        val footerHeight = 90
        val totalHeight = headerHeight + (productList.size * itemHeight) + noteHeight + footerHeight

        val bitmap = Bitmap.createBitmap(width, totalHeight.coerceAtLeast(600), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#F8FAFC"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rectF = RectF()

        // 1. TOP HEADER BANNER
        paint.color = Color.WHITE
        rectF.set(24f, 24f, width.toFloat() - 24f, headerHeight.toFloat())
        canvas.drawRoundRect(rectF, 24f, 24f, paint)

        paint.color = Color.parseColor("#E2E8F0")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRoundRect(rectF, 24f, 24f, paint)
        paint.style = Paint.Style.FILL

        // Header Title
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 32f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("KONTROL EDİLECEK ÜRÜNLER", 52f, 76f, paint)

        paint.color = Color.parseColor("#0D9488")
        paint.textSize = 19f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SKT TAKİP & STOK KONTROL RAPORU", 52f, 110f, paint)

        val sdfDate = SimpleDateFormat("dd.MM.yyyy", Locale.forLanguageTag("tr-TR"))
        val sdfTime = SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr-TR"))
        val now = Date()
        val dateStr = sdfDate.format(now)
        val timeStr = sdfTime.format(now)

        // 4 Summary Dashboard Cards in Header (Dengeli ve Orantılı Dizilim)
        val summaryTotalW = width.toFloat() - 72f // 1008f
        val summaryGap = 12f
        val cardW = (summaryTotalW - (3f * summaryGap)) / 4f // 243f
        val summaryY = 145f
        val summaryH = 125f

        val criticalProducts = productList.filter { it.getRemainingDays() <= 3L }
        val criticalStock = criticalProducts.sumOf { it.stokAdedi }
        val criticalVariants = criticalProducts.size

        val summaries = listOf(
            SummaryCardData("SEÇİLEN ÜRÜN", "${productList.size} ÇEŞİT", "#0D9488", "#F0FDFA", "#99F6E4"),
            SummaryCardData("KRİTİK STOK", "$criticalStock ADET", "#DC2626", "#FEF2F2", "#FECACA"),
            SummaryCardData("KRİTİK ÇEŞİT", "$criticalVariants ÇEŞİT", "#EA580C", "#FFF7ED", "#FED7AA"),
            SummaryCardData("TAKİP TARİHİ", "$dateStr\n$timeStr", "#2563EB", "#EFF6FF", "#BFDBFE")
        )

        summaries.forEachIndexed { i, s ->
            val left = 36f + (i * (cardW + summaryGap))
            rectF.set(left, summaryY, left + cardW, summaryY + summaryH)

            // Kart Arka Planı
            paint.color = Color.parseColor(s.bgHex)
            canvas.drawRoundRect(rectF, 14f, 14f, paint)

            // Kart Kenarlığı
            paint.color = Color.parseColor(s.borderHex)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawRoundRect(rectF, 14f, 14f, paint)
            paint.style = Paint.Style.FILL

            // Üst Renkli Vurgu Çizgisi
            rectF.set(left, summaryY, left + cardW, summaryY + 5f)
            paint.color = Color.parseColor(s.colHex)
            canvas.drawRoundRect(rectF, 5f, 5f, paint)

            // Başlık
            paint.color = Color.parseColor(s.colHex)
            paint.textSize = 13.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(s.label, left + 14f, summaryY + 30f, paint)

            // Değerler
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val lines = s.value.split("\n")
            if (lines.size > 1) {
                paint.color = Color.parseColor(s.colHex)
                paint.textSize = 18f
                canvas.drawText(lines[0], left + 14f, summaryY + 65f, paint)
                paint.color = Color.parseColor("#64748B")
                paint.textSize = 15f
                canvas.drawText(lines[1], left + 14f, summaryY + 95f, paint)
            } else {
                paint.color = Color.parseColor(s.colHex)
                paint.textSize = 22f
                canvas.drawText(s.value, left + 14f, summaryY + 75f, paint)
            }
        }

        // 2. PRODUCT CARDS
        var currentY = headerHeight.toFloat() + 20f

        productList.forEachIndexed { index, p ->
            val cardTop = currentY
            val cardBottom = currentY + itemHeight - 16f
            val cardLeft = 24f
            val cardRight = width.toFloat() - 24f

            val days = p.getRemainingDays()
            val info = ProductImageGenerator.getExpiryReportInfo(days)

            val (catBgHex, catTextHex) = when (p.kategori.lowercase(Locale.forLanguageTag("tr-TR"))) {
                "süt & kahvaltılık", "şarküteri" -> Pair("#E0F2FE", "#0284C7")
                "et & tavuk" -> Pair("#FEE2E2", "#DC2626")
                "unlu mamül", "ekmek" -> Pair("#FEF3C7", "#D97706")
                "meyve & sebze" -> Pair("#DCFCE7", "#16A34A")
                else -> Pair("#F1F5F9", "#475569")
            }

            // Ana Kart Arka Planı
            rectF.set(cardLeft, cardTop, cardRight, cardBottom)
            paint.color = Color.WHITE
            canvas.drawRoundRect(rectF, 18f, 18f, paint)

            paint.color = Color.parseColor(info.borderHex)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawRoundRect(rectF, 18f, 18f, paint)
            paint.style = Paint.Style.FILL

            // 1. SATIR: SIRA NO + ÜRÜN ADI
            val innerLeft = cardLeft + 16f
            val innerRight = cardRight - 16f

            // Sıra No Rozeti
            val seqText = "#${index + 1}"
            paint.textSize = 15f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val seqW = paint.measureText(seqText) + 16f

            rectF.set(innerLeft, cardTop + 16f, innerLeft + seqW, cardTop + 44f)
            paint.color = Color.parseColor("#CCFBF1")
            canvas.drawRoundRect(rectF, 6f, 6f, paint)
            paint.color = Color.parseColor("#0D9488")
            canvas.drawText(seqText, innerLeft + 8f, cardTop + 36f, paint)

            // Ürün Adı
            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 23f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val titleLeft = innerLeft + seqW + 12f
            val maxTitleW = innerRight - titleLeft
            val displayName = p.getDisplayName().uppercase()
            val truncatedName = ImageDrawingUtils.truncateText(paint, displayName, maxTitleW)
            canvas.drawText(truncatedName, titleLeft, cardTop + 37f, paint)

            // 2. SATIR: KATEGORİ & KOD BİLGİSİ
            val catText = p.kategori.ifBlank { "Genel" }
            paint.textSize = 13.5f
            val catW = paint.measureText(catText) + 18f
            val catTop = cardTop + 54f

            rectF.set(innerLeft, catTop, innerLeft + catW, catTop + 24f)
            paint.color = Color.parseColor(catBgHex)
            canvas.drawRoundRect(rectF, 6f, 6f, paint)
            paint.color = Color.parseColor(catTextHex)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(catText, innerLeft + 9f, catTop + 17f, paint)

            // Kod
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 13.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val codeStr = if (p.urunKodu.isNotBlank()) p.urunKodu else (if (p.barkod.isNotBlank()) p.barkod else p.id.toString())
            canvas.drawText("Kod: $codeStr", innerLeft + catW + 12f, catTop + 17f, paint)

            // 3. SATIR: DENGELİ 3 KART (SKT DURUMU, SKT TARİHİ, STOK MİKTARI)
            val totalCardSpace = innerRight - innerLeft
            val colGap = 12f
            val subCardW = (totalCardSpace - (2f * colGap)) / 3f
            val subCardTop = cardTop + 90f
            val subCardH = 50f

            // KART 1: SKT KALAN GÜN / DURUM KARTI
            val c1Left = innerLeft
            rectF.set(c1Left, subCardTop, c1Left + subCardW, subCardTop + subCardH)
            paint.color = Color.parseColor(info.bgHex)
            canvas.drawRoundRect(rectF, 10f, 10f, paint)
            paint.color = Color.parseColor(info.borderHex)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            canvas.drawRoundRect(rectF, 10f, 10f, paint)
            paint.style = Paint.Style.FILL

            paint.color = Color.parseColor(info.colorHex)
            paint.textSize = 15.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val statusText = "⏱ ${info.statusText}"
            val stW = paint.measureText(statusText)
            canvas.drawText(statusText, c1Left + ((subCardW - stW) / 2f).coerceAtLeast(8f), subCardTop + 31f, paint)

            // KART 2: SKT TARİH KARTI
            val c2Left = c1Left + subCardW + colGap
            rectF.set(c2Left, subCardTop, c2Left + subCardW, subCardTop + subCardH)
            paint.color = Color.parseColor("#F1F5F9")
            canvas.drawRoundRect(rectF, 10f, 10f, paint)
            paint.color = Color.parseColor("#CBD5E1")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            canvas.drawRoundRect(rectF, 10f, 10f, paint)
            paint.style = Paint.Style.FILL

            paint.color = Color.parseColor("#1E293B")
            paint.textSize = 15.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val sktText = "📅 SKT: ${p.getFormattedSkt()}"
            val sktW = paint.measureText(sktText)
            canvas.drawText(sktText, c2Left + ((subCardW - sktW) / 2f).coerceAtLeast(8f), subCardTop + 31f, paint)

            // KART 3: STOK MİKTARI KARTI
            val c3Left = c2Left + subCardW + colGap
            rectF.set(c3Left, subCardTop, c3Left + subCardW, subCardTop + subCardH)
            paint.color = Color.parseColor("#FEF3C7")
            canvas.drawRoundRect(rectF, 10f, 10f, paint)
            paint.color = Color.parseColor("#F59E0B")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            canvas.drawRoundRect(rectF, 10f, 10f, paint)
            paint.style = Paint.Style.FILL

            paint.color = Color.parseColor("#92400E")
            paint.textSize = 15.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val stokText = "📦 STOK: ${p.stokAdedi} ADET"
            val stokW = paint.measureText(stokText)
            canvas.drawText(stokText, c3Left + ((subCardW - stokW) / 2f).coerceAtLeast(8f), subCardTop + 31f, paint)

            // Alt Risk Çizgisi
            rectF.set(cardLeft, cardBottom - 4f, cardRight, cardBottom)
            paint.color = Color.parseColor(info.colorHex)
            canvas.drawRoundRect(rectF, 2f, 2f, paint)

            currentY += itemHeight
        }

        // 3. NOTE SECTION (IF PROVIDED)
        if (note.isNotBlank()) {
            rectF.set(24f, currentY + 10f, width.toFloat() - 24f, currentY + noteHeight - 10f)
            paint.color = Color.WHITE
            canvas.drawRoundRect(rectF, 18f, 18f, paint)

            paint.color = Color.parseColor("#E2E8F0")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawRoundRect(rectF, 18f, 18f, paint)
            paint.style = Paint.Style.FILL

            paint.color = Color.parseColor("#0D9488")
            paint.textSize = 19f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("📝 EKİP NOTU:", 48f, currentY + 48f, paint)

            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 21f
            paint.typeface = Typeface.DEFAULT
            val safeNote = ImageDrawingUtils.truncateText(paint, note, width.toFloat() - 120f)
            canvas.drawText(safeNote, 48f, currentY + 86f, paint)

            currentY += noteHeight
        }

        // 4. FOOTER (Rapor oluşturma tarih ve saati)
        paint.color = Color.parseColor("#E2E8F0")
        paint.strokeWidth = 2f
        canvas.drawLine(24f, currentY + 10f, width.toFloat() - 24f, currentY + 10f, paint)

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 18f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Rapor Tarihi: $dateStr $timeStr   •   SKT Takip & Stok Kontrol Sistemi", 36f, currentY + 52f, paint)

        return bitmap
    }
}
