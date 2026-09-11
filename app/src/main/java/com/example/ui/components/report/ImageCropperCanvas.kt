package com.example.ui.components.report

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

private enum class CropHandle {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER
}

@Composable
fun ImageCropperCanvas(
    bitmap: Bitmap,
    onCropApplied: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    var cropLeftFrac by remember { mutableFloatStateOf(0.03f) }
    var cropTopFrac by remember { mutableFloatStateOf(0.03f) }
    var cropRightFrac by remember { mutableFloatStateOf(0.97f) }
    var cropBottomFrac by remember { mutableFloatStateOf(0.97f) }

    var activeHandle by remember { mutableStateOf<CropHandle?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val containerWidthPx = constraints.maxWidth.toFloat()
            val containerHeightPx = constraints.maxHeight.toFloat()

            val imgAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
            val containerAspect = containerWidthPx / containerHeightPx

            val (imgWidthPx, imgHeightPx, imgLeftPx, imgTopPx) = if (containerAspect > imgAspect) {
                val h = containerHeightPx
                val w = h * imgAspect
                val l = (containerWidthPx - w) / 2f
                val t = 0f
                listOf(w, h, l, t)
            } else {
                val w = containerWidthPx
                val h = w / imgAspect
                val l = 0f
                val t = (containerHeightPx - h) / 2f
                listOf(w, h, l, t)
            }

            val boxLeft = imgLeftPx + cropLeftFrac * imgWidthPx
            val boxTop = imgTopPx + cropTopFrac * imgHeightPx
            val boxRight = imgLeftPx + cropRightFrac * imgWidthPx
            val boxBottom = imgTopPx + cropBottomFrac * imgHeightPx

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val touchThreshold = 60.dp.toPx()
                                val x = offset.x
                                val y = offset.y

                                val distTL = hypot(x - boxLeft, y - boxTop)
                                val distTR = hypot(x - boxRight, y - boxTop)
                                val distBL = hypot(x - boxLeft, y - boxBottom)
                                val distBR = hypot(x - boxRight, y - boxBottom)

                                activeHandle = when {
                                    distTL < touchThreshold -> CropHandle.TOP_LEFT
                                    distTR < touchThreshold -> CropHandle.TOP_RIGHT
                                    distBL < touchThreshold -> CropHandle.BOTTOM_LEFT
                                    distBR < touchThreshold -> CropHandle.BOTTOM_RIGHT
                                    x in boxLeft..boxRight && y in boxTop..boxBottom -> CropHandle.CENTER
                                    else -> null
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val handle = activeHandle ?: return@detectDragGestures

                                val dxFrac = dragAmount.x / imgWidthPx
                                val dyFrac = dragAmount.y / imgHeightPx

                                val minSize = 0.08f

                                when (handle) {
                                    CropHandle.TOP_LEFT -> {
                                        cropLeftFrac = (cropLeftFrac + dxFrac).coerceIn(0f, cropRightFrac - minSize)
                                        cropTopFrac = (cropTopFrac + dyFrac).coerceIn(0f, cropBottomFrac - minSize)
                                    }
                                    CropHandle.TOP_RIGHT -> {
                                        cropRightFrac = (cropRightFrac + dxFrac).coerceIn(cropLeftFrac + minSize, 1f)
                                        cropTopFrac = (cropTopFrac + dyFrac).coerceIn(0f, cropBottomFrac - minSize)
                                    }
                                    CropHandle.BOTTOM_LEFT -> {
                                        cropLeftFrac = (cropLeftFrac + dxFrac).coerceIn(0f, cropRightFrac - minSize)
                                        cropBottomFrac = (cropBottomFrac + dyFrac).coerceIn(cropTopFrac + minSize, 1f)
                                    }
                                    CropHandle.BOTTOM_RIGHT -> {
                                        cropRightFrac = (cropRightFrac + dxFrac).coerceIn(cropLeftFrac + minSize, 1f)
                                        cropBottomFrac = (cropBottomFrac + dyFrac).coerceIn(cropTopFrac + minSize, 1f)
                                    }
                                    CropHandle.CENTER -> {
                                        val widthFrac = cropRightFrac - cropLeftFrac
                                        val heightFrac = cropBottomFrac - cropTopFrac

                                        var newLeft = cropLeftFrac + dxFrac
                                        var newTop = cropTopFrac + dyFrac

                                        if (newLeft < 0f) newLeft = 0f
                                        if (newLeft + widthFrac > 1f) newLeft = 1f - widthFrac

                                        if (newTop < 0f) newTop = 0f
                                        if (newTop + heightFrac > 1f) newTop = 1f - heightFrac

                                        cropLeftFrac = newLeft
                                        cropRightFrac = newLeft + widthFrac
                                        cropTopFrac = newTop
                                        cropBottomFrac = newTop + heightFrac
                                    }
                                }
                            },
                            onDragEnd = { activeHandle = null },
                            onDragCancel = { activeHandle = null }
                        )
                    }
            ) {
                // Görseli çiz
                drawImage(
                    image = bitmap.asImageBitmap(),
                    dstOffset = androidx.compose.ui.unit.IntOffset(imgLeftPx.toInt(), imgTopPx.toInt()),
                    dstSize = androidx.compose.ui.unit.IntSize(imgWidthPx.toInt(), imgHeightPx.toInt())
                )

                // Karartma katmanları
                val darkOverlay = Color(0x99000000)
                drawRect(darkOverlay, Offset.Zero, Size(size.width, boxTop))
                drawRect(darkOverlay, Offset(0f, boxBottom), Size(size.width, size.height - boxBottom))
                drawRect(darkOverlay, Offset(0f, boxTop), Size(boxLeft, boxBottom - boxTop))
                drawRect(darkOverlay, Offset(boxRight, boxTop), Size(size.width - boxRight, boxBottom - boxTop))

                // Kırpma çerçevesi
                drawRect(
                    color = Color.White,
                    topLeft = Offset(boxLeft, boxTop),
                    size = Size(boxRight - boxLeft, boxBottom - boxTop),
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Izgara çizgileri (Üçte bir kuralı)
                val w = boxRight - boxLeft
                val h = boxBottom - boxTop
                val gridColor = Color.White.copy(alpha = 0.35f)
                drawLine(gridColor, Offset(boxLeft + w / 3f, boxTop), Offset(boxLeft + w / 3f, boxBottom), 1.dp.toPx())
                drawLine(gridColor, Offset(boxLeft + 2 * w / 3f, boxTop), Offset(boxLeft + 2 * w / 3f, boxBottom), 1.dp.toPx())
                drawLine(gridColor, Offset(boxLeft, boxTop + h / 3f), Offset(boxRight, boxTop + h / 3f), 1.dp.toPx())
                drawLine(gridColor, Offset(boxLeft, boxTop + 2 * h / 3f), Offset(boxRight, boxTop + 2 * h / 3f), 1.dp.toPx())

                // Köşe tutamaçları
                val handleRadius = 8.dp.toPx()
                val handleColor = Color(0xFF00C2AB)
                drawCircle(handleColor, handleRadius, Offset(boxLeft, boxTop))
                drawCircle(handleColor, handleRadius, Offset(boxRight, boxTop))
                drawCircle(handleColor, handleRadius, Offset(boxLeft, boxBottom))
                drawCircle(handleColor, handleRadius, Offset(boxRight, boxBottom))
            }
        }

        // Kırpma Butonları
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text("İptal", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        try {
                            val cropX = (cropLeftFrac * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
                            val cropY = (cropTopFrac * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
                            val cropW = ((cropRightFrac - cropLeftFrac) * bitmap.width).toInt().coerceIn(10, bitmap.width - cropX)
                            val cropH = ((cropBottomFrac - cropTopFrac) * bitmap.height).toInt().coerceIn(10, bitmap.height - cropY)

                            val cropped = Bitmap.createBitmap(bitmap, cropX, cropY, cropW, cropH)
                            onCropApplied(cropped)
                        } catch (e: Exception) {
                            onCancel()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Uygula", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
