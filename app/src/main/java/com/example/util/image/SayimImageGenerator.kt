package com.example.util.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.data.AdetselKayit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SayimImageGenerator {

    fun createSayimListBitmap(
        records: List<AdetselKayit>
    ): Bitmap? {
        if (records.isEmpty()) return null

        val width = 1080
        val headerHeight = 220
        val summaryBarHeight = 110
        val itemHeight = 175
        val footerHeight = 90
        val totalHeight = headerHeight + summaryBarHeight + (records.size * itemHeight) + footerHeight

        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#F8FAFC"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rectF = RectF()

        // 1. ÜST HERO HEADER (Zümrüt Yeşil Degrade)
        val headerShader = LinearGradient(
            0f, 0f, 0f, headerHeight.toFloat(),
            intArrayOf(Color.parseColor("#0F766E"), Color.parseColor("#115E59"), Color.parseColor("#0F172A")),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = headerShader
        rectF.set(0f, 0f, width.toFloat(), headerHeight.toFloat())
        canvas.drawRect(rectF, paint)
        paint.shader = null

        // Başlık İkonu
        ImageDrawingUtils.drawChecklistIcon(canvas, paint, 40f, 40f, 72f)

        // Küçük Üst Başlık
        paint.color = Color.parseColor("#99F6E4")
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("MAĞAZA ENVANTER YÖNETİMİ", 132f, 66f, paint)

        // Ana Başlık
        paint.color = Color.WHITE
        paint.textSize = 34f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ADETSEL SAYIM RAPORU", 132f, 110f, paint)

        // Tarih ve Sistem Bilgisi
        paint.color = Color.parseColor("#CCFBF1")
        paint.textSize = 21f
        paint.typeface = Typeface.DEFAULT
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR"))
        val dateStr = sdf.format(Date())
        canvas.drawText("Rapor Tarihi: $dateStr   •   Toplam: ${records.size} Ürün", 40f, 180f, paint)

        // Header Alt Çizgisi
        paint.color = Color.parseColor("#14B8A6")
        paint.alpha = 90
        canvas.drawLine(0f, headerHeight.toFloat() - 2f, width.toFloat(), headerHeight.toFloat() - 2f, paint)
        paint.alpha = 255

        // 2. ÖZET İSTATİSTİK BARI (4 Kutu)
        val toplamUrun = records.size
        val toplamAdet = records.sumOf { if (it.yapildiMi) it.sayilanAdet else it.beklenenAdet }
        val fazlaSayisi = records.count { it.yapildiMi && it.farkAdet > 0 }
        val eksikSayisi = records.count { it.yapildiMi && it.farkAdet < 0 }

        val statY = headerHeight.toFloat() + 20f
        val statW = (width - (40f * 2f) - (14f * 3f)) / 4f

        val statItems = listOf(
            Triple("Toplam Ürün", "$toplamUrun", "#0F766E"),
            Triple("Toplam Adet", "$toplamAdet", "#0284C7"),
            Triple("Fazla Çıkan", "$fazlaSayisi", "#16A34A"),
            Triple("Eksik Çıkan", "$eksikSayisi", "#DC2626")
        )

        statItems.forEachIndexed { i, (label, count, colorHex) ->
            val left = 40f + (i * (statW + 14f))
            rectF.set(left, statY, left + statW, statY + 70f)

            // Kutu arka planı
            paint.color = Color.WHITE
            canvas.drawRoundRect(rectF, 12f, 12f, paint)

            // Kenarlık
            paint.color = Color.parseColor(colorHex)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            paint.alpha = 80
            canvas.drawRoundRect(rectF, 12f, 12f, paint)
            paint.style = Paint.Style.FILL
            paint.alpha = 255

            // Metinler
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 15f
            paint.typeface = Typeface.DEFAULT
            val labelW = paint.measureText(label)
            canvas.drawText(label, left + (statW - labelW) / 2f, statY + 28f, paint)

            paint.color = Color.parseColor(colorHex)
            paint.textSize = 24f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val countW = paint.measureText(count)
            canvas.drawText(count, left + (statW - countW) / 2f, statY + 58f, paint)
        }

        // 3. KAYIT LİSTESİ KARTLARI
        var currentY = headerHeight.toFloat() + summaryBarHeight.toFloat() + 10f

        records.forEachIndexed { index, record ->
            val cardTop = currentY
            val cardBottom = currentY + itemHeight - 16f
            val cardLeft = 36f
            val cardRight = width.toFloat() - 36f

            val (statusBg, statusBorder, statusText, statusLabel) = when {
                !record.yapildiMi -> Quadruple("#FFF7ED", "#FED7AA", "#EA580C", "Bekliyor")
                record.farkAdet == 0 -> Quadruple("#F0FDF4", "#BBF7D0", "#16A34A", "Tam / Eşit (0)")
                record.farkAdet > 0 -> Quadruple("#EFF6FF", "#BFDBFE", "#2563EB", "+${record.farkAdet} Fazla")
                else -> Quadruple("#FEF2F2", "#FECACA", "#DC2626", "${record.farkAdet} Eksik")
            }

            // Kart zemin
            rectF.set(cardLeft, cardTop, cardRight, cardBottom)
            paint.color = Color.WHITE
            canvas.drawRoundRect(rectF, 16f, 16f, paint)

            // Kart kenarlık
            paint.color = if (record.yapildiMi && record.farkAdet != 0) Color.parseColor(statusBorder) else Color.parseColor("#E2E8F0")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = if (record.yapildiMi && record.farkAdet != 0) 1.5f else 1f
            canvas.drawRoundRect(rectF, 16f, 16f, paint)
            paint.style = Paint.Style.FILL

            val innerLeft = cardLeft + 20f
            val innerRight = cardRight - 20f

            // Durum Rozeti (Sağ üst)
            paint.textSize = 15f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val badgeTextWidth = paint.measureText(statusLabel)
            val badgeW = badgeTextWidth + 24f
            val badgeH = 34f
            val badgeL = innerRight - badgeW
            val badgeT = cardTop + 16f

            rectF.set(badgeL, badgeT, innerRight, badgeT + badgeH)
            paint.color = Color.parseColor(statusBg)
            canvas.drawRoundRect(rectF, 8f, 8f, paint)

            paint.color = Color.parseColor(statusBorder)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.2f
            canvas.drawRoundRect(rectF, 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            paint.color = Color.parseColor(statusText)
            canvas.drawText(statusLabel, badgeL + 12f, badgeT + 23f, paint)

            // Sıra No ve Ürün Adı
            val seqText = "#${index + 1}"
            paint.color = Color.parseColor("#0F766E")
            paint.textSize = 17f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(seqText, innerLeft, cardTop + 38f, paint)
            val seqOffset = paint.measureText(seqText) + 12f

            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 23f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val maxTitleW = badgeL - innerLeft - seqOffset - 16f
            val safeTitle = ImageDrawingUtils.truncateText(paint, record.urunAdi, maxTitleW)
            canvas.drawText(safeTitle, innerLeft + seqOffset, cardTop + 40f, paint)

            // Satır 2: Barkod / Kod & Kategori
            paint.color = Color.parseColor("#475569")
            paint.textSize = 17f
            paint.typeface = Typeface.DEFAULT
            val codeStr = record.getDisplayCode()
            val catStr = record.kategori.ifBlank { "Genel Reyon" }
            val line2 = "Kod/Barkod: $codeStr   •   Reyon: $catStr"
            val safeLine2 = ImageDrawingUtils.truncateText(paint, line2, innerRight - innerLeft)
            canvas.drawText(safeLine2, innerLeft, cardTop + 82f, paint)

            // Satır 3: Stok Bilgileri & Notlar
            val sayilanStr = if (record.yapildiMi) "${record.sayilanAdet} Adet" else "Henüz Sayılmadı"
            val line3 = "Sistem Stoğu: ${record.beklenenAdet} Adet   |   Sayılan: $sayilanStr" +
                (if (record.notlar.isNotBlank()) "   |   Not: ${record.notlar}" else "")
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 16f
            val safeLine3 = ImageDrawingUtils.truncateText(paint, line3, innerRight - innerLeft)
            canvas.drawText(safeLine3, innerLeft, cardTop + 120f, paint)

            currentY += itemHeight
        }

        // 4. FOOTER
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 16f
        paint.typeface = Typeface.DEFAULT
        val footerText = "SKT & Envanter Yönetim Sistemi   •   Otomatik Rapor Çıktısı"
        val footerW = paint.measureText(footerText)
        canvas.drawText(footerText, (width - footerW) / 2f, totalHeight - 38f, paint)

        return bitmap
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
