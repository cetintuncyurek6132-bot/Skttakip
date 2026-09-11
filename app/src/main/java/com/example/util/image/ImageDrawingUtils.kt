package com.example.util.image

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

object ImageDrawingUtils {
    fun truncateText(paint: Paint, text: String, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var truncated = text
        while (truncated.isNotEmpty() && paint.measureText("$truncated...") > maxWidth) {
            truncated = truncated.dropLast(1)
        }
        return "$truncated..."
    }

    fun drawChecklistIcon(canvas: Canvas, paint: Paint, left: Float, top: Float, size: Float) {
        val iconBox = RectF(left, top, left + size, top + size)
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(40, 255, 255, 255)
        canvas.drawRoundRect(iconBox, size * 0.25f, size * 0.25f, paint)

        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3.5f
        paint.strokeCap = Paint.Cap.ROUND

        val padding = size * 0.22f
        val innerW = size - (padding * 2f)
        val lineSpacing = innerW / 2.5f

        for (i in 0..2) {
            val y = top + padding + (i * lineSpacing)
            canvas.drawLine(left + padding, y + 2f, left + padding + 6f, y + 8f, paint)
            canvas.drawLine(left + padding + 6f, y + 8f, left + padding + 16f, y - 4f, paint)
            canvas.drawLine(left + padding + 22f, y + 2f, left + size - padding, y + 2f, paint)
        }
        paint.style = Paint.Style.FILL
    }
}
