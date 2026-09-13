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
import com.example.data.TurKontrolKaydi
import com.example.data.TurRaporu
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TourReportImageRenderer {

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    fun createTourReportBitmap(
        rapor: TurRaporu,
        logs: List<TurKontrolKaydi> = emptyList()
    ): Bitmap {
        val width = 1080
        val headerHeight = 220
        val summaryHeight = 160
        val itemHeight = 140
        val footerHeight = 100
        val totalHeight = headerHeight + summaryHeight + (logs.size * itemHeight) + footerHeight

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
        canvas.drawText("Reyon Kontrol Raporu", 124f, 102f, paint)

        val sdf = SimpleDateFormat("dd MMMM yyyy - HH:mm", Locale.forLanguageTag("tr-TR"))
        val dateStr = sdf.format(Date(rapor.turTarihi))
        val statusLabel = if (rapor.tamamlandiMi) "TAMAMLANDI" else "YARIM KALDI"

        paint.color = Color.parseColor("#E0F2FE")
        paint.textSize = 21f
        paint.typeface = Typeface.DEFAULT
        val metaStr = "Reyon: ${rapor.hedefReyon}   |   Durum: $statusLabel   |   Tarih: $dateStr"
        val safeMetaStr = ImageDrawingUtils.truncateText(paint, metaStr, width - 72f)
        canvas.drawText(safeMetaStr, 36f, 172f, paint)

        var currentY = headerHeight.toFloat()

        val cardWidth = (width.toFloat() - 96f) / 3f
        rectF.set(32f, currentY + 12f, 32f + cardWidth, currentY + summaryHeight - 12f)
        paint.color = Color.parseColor("#F0FDF4")
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

        val fireLeft = 48f + cardWidth
        rectF.set(fireLeft, currentY + 12f, fireLeft + cardWidth, currentY + summaryHeight - 12f)
        paint.color = Color.parseColor("#FEF2F2")
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

        val notrLeft = 64f + (cardWidth * 2)
        rectF.set(notrLeft, currentY + 12f, width.toFloat() - 32f, currentY + summaryHeight - 12f)
        paint.color = Color.parseColor("#FFFFFF")
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

            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 26f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val title = "${index + 1}. ${log.urunAdiSnapshot}"
            val safeTitle = ImageDrawingUtils.truncateText(paint, title, cardRight - cardLeft - 260f)
            canvas.drawText(safeTitle, cardLeft + 24f, cardTop + 46f, paint)

            if (log.barkodSnapshot.isNotBlank()) {
                paint.color = Color.parseColor("#64748B")
                paint.textSize = 20f
                paint.typeface = Typeface.DEFAULT
                canvas.drawText("Barkod: ${log.barkodSnapshot}", cardLeft + 24f, cardTop + 84f, paint)
            }

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

        paint.color = Color.parseColor("#E2E8F0")
        paint.strokeWidth = 2f
        canvas.drawLine(32f, currentY + 20f, width.toFloat() - 32f, currentY + 20f, paint)

        val formattedNow = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR")).format(Date())
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Rapor Tarihi: $formattedNow", 36f, currentY + 62f, paint)

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
        rapor: TurRaporu,
        logs: List<TurKontrolKaydi> = emptyList()
    ) {
        val bitmap = createTourReportBitmap(rapor, logs)
        BitmapSharingHelper.shareBitmap(context, bitmap, "tur_raporu")
    }
}
