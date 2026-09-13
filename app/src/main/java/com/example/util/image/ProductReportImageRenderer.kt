package com.example.util.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.data.Product
import com.example.data.getDisplayName
import com.example.util.ProductImageGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ProductReportImageRenderer {

    fun createProductsBitmap(
        filterLabel: String,
        searchQuery: String,
        productList: List<Product>
    ): Bitmap? {
        if (productList.isEmpty()) return null

        val width = 1080
        val headerHeight = 220
        val itemHeight = 185
        val footerHeight = 100
        val totalHeight = headerHeight + (productList.size * itemHeight) + footerHeight

        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#F8FAFC"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rectF = RectF()

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

        ImageDrawingUtils.drawChecklistIcon(canvas, paint, 36f, 36f, 72f)

        paint.color = Color.parseColor("#BAE6FD")
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SKT TAKİP & STOK", 124f, 62f, paint)

        paint.color = Color.WHITE
        paint.textSize = 34f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("KONTROL EDİLECEK ÜRÜNLER", 124f, 102f, paint)

        paint.color = Color.parseColor("#E0F2FE")
        paint.textSize = 21f
        paint.typeface = Typeface.DEFAULT

        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR"))
        val dateStr = sdf.format(Date())
        val metaText = if (searchQuery.isNotBlank()) {
            val truncatedSearch = ImageDrawingUtils.truncateText(paint, searchQuery, 300f)
            "Kategori: $filterLabel   |   Filtre: \"$truncatedSearch\"   |   Tarih: $dateStr   |   Toplam: ${productList.size} Çeşit"
        } else {
            "Kategori: $filterLabel   |   Tarih: $dateStr   |   Toplam: ${productList.size} Çeşit"
        }
        val safeMetaText = ImageDrawingUtils.truncateText(paint, metaText, width - 72f)
        canvas.drawText(safeMetaText, 36f, 172f, paint)

        paint.color = Color.parseColor("#38BDF8")
        paint.alpha = 80
        canvas.drawLine(0f, headerHeight.toFloat() - 2f, width.toFloat(), headerHeight.toFloat() - 2f, paint)
        paint.alpha = 255

        var currentY = headerHeight.toFloat() + 15f

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

            rectF.set(cardLeft, cardTop, cardRight, cardBottom)
            paint.color = Color.WHITE
            canvas.drawRoundRect(rectF, 18f, 18f, paint)

            paint.color = Color.parseColor(info.borderHex)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawRoundRect(rectF, 18f, 18f, paint)
            paint.style = Paint.Style.FILL

            val innerLeft = cardLeft + 16f
            val innerRight = cardRight - 16f

            // 1. SATIR: Sıra No + Ürün Adı
            val seqText = "#${index + 1}"
            paint.textSize = 15f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val seqW = paint.measureText(seqText) + 16f

            rectF.set(innerLeft, cardTop + 16f, innerLeft + seqW, cardTop + 44f)
            paint.color = Color.parseColor("#CCFBF1")
            canvas.drawRoundRect(rectF, 6f, 6f, paint)
            paint.color = Color.parseColor("#0D9488")
            canvas.drawText(seqText, innerLeft + 8f, cardTop + 36f, paint)

            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 23f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val titleLeft = innerLeft + seqW + 12f
            val maxTitleW = innerRight - titleLeft
            val displayName = p.getDisplayName().uppercase()
            val truncatedName = ImageDrawingUtils.truncateText(paint, displayName, maxTitleW)
            canvas.drawText(truncatedName, titleLeft, cardTop + 37f, paint)

            // 2. SATIR: Kategori & Kod
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

            // KART 1: SKT Durumu
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

            // KART 2: SKT Tarihi
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

            // KART 3: Stok Miktarı
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

        paint.color = Color.parseColor("#E2E8F0")
        paint.strokeWidth = 2f
        canvas.drawLine(32f, currentY + 20f, width.toFloat() - 32f, currentY + 20f, paint)

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 19f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val footerLeft = "Rapor Tarihi: $dateStr"
        canvas.drawText(footerLeft, 36f, currentY + 62f, paint)

        paint.color = Color.parseColor("#0EA5B7")
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val footerRight = "SKT TAKİP & STOK"
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
        BitmapSharingHelper.shareBitmap(context, bitmap, "skt_urun_listesi")
    }
}
