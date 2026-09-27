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

    /**
     * Creates vertical PNG bitmaps formatted specifically for clean WhatsApp sharing
     * with exact dynamic height calculation (zero bottom white space rule) and clean row design.
     *
     * Rules:
     * - Fixed 1080px width, clean pure white background (#FFFFFF).
     * - NO top header/title/date/store/app name. NO bottom signature/footer.
     * - Exact calculated height: (pageProducts.size * itemHeight) + (topBottomPadding * 2)
     * - Left Badge: Compact Day Badge Capsule (0-2d: #EF4444, 3-4d: #F59E0B, 5-7d: #EAB308, >7d: #10B981)
     * - Middle: Product name (#0F172A bold 31px) & Sub-info (#64748B 23px "Kod: X • SKT: Y").
     * - Right Badge: Light green badge (#ECFDF5) with bold #059669 "[Adet] Adet".
     * - Divider: 1.5px light gray (#E2E8F0) between product rows.
     * - Pagination: Chunks of 20 products per page.
     */
    fun createCleanWhatsAppShareBitmaps(
        productList: List<Product>,
        todayMidnight: Long = com.example.data.getTodayMidnightMillis()
    ): List<Bitmap> {
        if (productList.isEmpty()) return emptyList()

        val chunks = productList.chunked(20)
        val width = 1080
        val itemHeight = 150f
        val topBottomPadding = 24f
        val horizontalPadding = 32f

        return chunks.map { chunkProducts ->
            val totalCardCount = chunkProducts.size
            val calculatedHeight = ((totalCardCount * itemHeight) + (topBottomPadding * 2)).toInt()
            val bitmap = Bitmap.createBitmap(width, calculatedHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Pure clean white background (#FFFFFF)
            canvas.drawColor(Color.WHITE)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val rectF = RectF()

            chunkProducts.forEachIndexed { index, product ->
                val rowTop = topBottomPadding + (index * itemHeight)
                val rowBottom = rowTop + itemHeight
                val rowLeft = horizontalPadding
                val rowRight = width.toFloat() - horizontalPadding

                val daysLeft = product.getRemainingDays(todayMidnight)

                // 1. SOL: Kompakt Gün Rozeti (Kapsül 104x104px)
                val badgeSize = 104f
                val badgeLeft = rowLeft + 8f
                val badgeTop = rowTop + ((itemHeight - badgeSize) / 2f)
                val badgeRight = badgeLeft + badgeSize
                val badgeBottom = badgeTop + badgeSize

                val badgeColorHex = when {
                    daysLeft < 0L -> "#DC2626"       // Süresi Geçmiş - Koyu Kırmızı
                    daysLeft in 0L..2L -> "#EF4444"   // 0-2 Gün - Kırmızı
                    daysLeft in 3L..4L -> "#F59E0B"   // 3-4 Gün - Turuncu
                    daysLeft in 5L..7L -> "#EAB308"   // 5-7 Gün - Kehribar/Sarı
                    else -> "#10B981"                 // >7 Gün - Turkuaz / Yeşil
                }

                rectF.set(badgeLeft, badgeTop, badgeRight, badgeBottom)
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor(badgeColorHex)
                canvas.drawRoundRect(rectF, 22f, 22f, paint)

                // Rozet İçi Metin
                paint.color = Color.WHITE
                paint.textAlign = Paint.Align.CENTER
                val badgeCenterX = badgeLeft + (badgeSize / 2f)

                if (daysLeft == 0L) {
                    paint.textSize = 24f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("BUGÜN", badgeCenterX, badgeTop + 46f, paint)
                    paint.textSize = 17f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("DOLUYOR", badgeCenterX, badgeTop + 76f, paint)
                } else if (daysLeft < 0L) {
                    paint.textSize = 22f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("SÜRESİ", badgeCenterX, badgeTop + 46f, paint)
                    paint.textSize = 19f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("GEÇTİ", badgeCenterX, badgeTop + 76f, paint)
                } else {
                    paint.textSize = 42f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("$daysLeft", badgeCenterX, badgeTop + 50f, paint)
                    paint.textSize = 19f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("GÜN", badgeCenterX, badgeTop + 82f, paint)
                }
                paint.textAlign = Paint.Align.LEFT

                // 2. SAĞ: Kompakt Adet Rozeti ([Adet] Adet)
                val stokText = "${product.stokAdedi} Adet"
                paint.textSize = 25f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val stokTextWidth = paint.measureText(stokText)
                val rightBadgeWidth = stokTextWidth + 34f
                val rightBadgeHeight = 48f
                val rightBadgeRight = rowRight - 8f
                val rightBadgeLeft = rightBadgeRight - rightBadgeWidth
                val rightBadgeTop = rowTop + ((itemHeight - rightBadgeHeight) / 2f)
                val rightBadgeBottom = rightBadgeTop + rightBadgeHeight

                rectF.set(rightBadgeLeft, rightBadgeTop, rightBadgeRight, rightBadgeBottom)
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#ECFDF5") // Hafif yeşil zemin
                canvas.drawRoundRect(rectF, 12f, 12f, paint)

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1.5f
                paint.color = Color.parseColor("#A7F3D0") // İnce yeşil çerçeve
                canvas.drawRoundRect(rectF, 12f, 12f, paint)
                paint.style = Paint.Style.FILL

                paint.color = Color.parseColor("#059669")
                val stokTextY = rightBadgeTop + (rightBadgeHeight / 2f) - ((paint.descent() + paint.ascent()) / 2f)
                canvas.drawText(stokText, rightBadgeLeft + 17f, stokTextY, paint)

                // 3. ORTA: Ürün Adı & Kod + SKT
                val contentLeft = badgeRight + 20f
                val maxNameWidth = rightBadgeLeft - contentLeft - 16f

                // Üst Satır: Koyu lacivert/siyah (#0F172A), Kalın Ürün Adı
                paint.textSize = 31f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = Color.parseColor("#0F172A")
                val truncatedName = ImageDrawingUtils.truncateText(paint, product.urunAdi, maxNameWidth)
                canvas.drawText(truncatedName, contentLeft, rowTop + 62f, paint)

                // Alt Satır: Açık gri/füme (#64748B) "Kod: [urunKodu] • SKT: [sktTarihi]"
                paint.textSize = 23f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = Color.parseColor("#64748B")
                val sktFormatted = product.getFormattedSkt()
                val codePrefix = if (product.urunKodu.isNotBlank()) "Kod: ${product.urunKodu}  •  " else ""
                val subInfoText = "${codePrefix}SKT: $sktFormatted"
                val truncatedSubInfo = ImageDrawingUtils.truncateText(paint, subInfoText, maxNameWidth)
                canvas.drawText(truncatedSubInfo, contentLeft, rowTop + 106f, paint)

                // 4. AYRAÇ: Satırlar arasına 1.5px inceliğinde açık gri (#E2E8F0) ayırıcı çizgi
                if (index < chunkProducts.size - 1) {
                    paint.color = Color.parseColor("#E2E8F0")
                    paint.strokeWidth = 1.5f
                    canvas.drawLine(rowLeft, rowBottom, rowRight, rowBottom, paint)
                }
            }

            bitmap
        }
    }
}
