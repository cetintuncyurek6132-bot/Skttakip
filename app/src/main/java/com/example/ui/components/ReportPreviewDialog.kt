package com.example.ui.components

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.ProductImageGenerator
import kotlin.math.hypot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportPreviewDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val originalBitmap = remember(bitmap) { bitmap }
    var currentBitmap by remember(bitmap) { mutableStateOf(bitmap) }
    var isCropping by remember { mutableStateOf(false) }

    val isCropped = currentBitmap != originalBitmap

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // TOP BAR
                TopAppBar(
                    title = {
                        Text(
                            text = if (isCropping) "Resmi Kırp / Düzenle" else "Paylaşım Önizlemesi",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (isCropping) {
                                isCropping = false
                            } else {
                                onDismiss()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        if (!isCropping && isCropped) {
                            TextButton(
                                onClick = {
                                    currentBitmap = originalBitmap
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Sıfırla",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Sıfırla",
                                        color = Color(0xFF38BDF8),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1E293B)
                    )
                )

                // CENTER PREVIEW OR CROP AREA
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCropping) {
                        ImageCropperCanvas(
                            bitmap = currentBitmap,
                            onCropApplied = { cropped ->
                                currentBitmap = cropped
                                isCropping = false
                            },
                            onCancel = {
                                isCropping = false
                            }
                        )
                    } else {
                        var scale by remember { mutableFloatStateOf(1f) }
                        var offset by remember { mutableStateOf(Offset.Zero) }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(1f, 4f)
                                        offset = if (scale > 1f) {
                                            Offset(
                                                x = offset.x + pan.x,
                                                y = offset.y + pan.y
                                            )
                                        } else {
                                            Offset.Zero
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = currentBitmap.asImageBitmap(),
                                contentDescription = "Rapor Önizleme",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    )
                            )

                            if (scale == 1f) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 8.dp),
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = "🔍 Yakınlaştırmak için çimdikleyin",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // BOTTOM ACTION BAR (PREVIEW MODE)
                if (!isCropping) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF1E293B),
                        tonalElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // "KIRP / DÜZENLE" BUTTON
                                OutlinedButton(
                                    onClick = { isCropping = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFF475569))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Crop,
                                        contentDescription = "Kırp",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "KIRP",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                // "WHATSAPP'TA PAYLAŞ" BUTTON
                                Button(
                                    onClick = {
                                        ProductImageGenerator.shareBitmap(
                                            context = context,
                                            bitmap = currentBitmap,
                                            fileNamePrefix = "skt_rapor"
                                        )
                                        onDismiss()
                                    },
                                    modifier = Modifier
                                        .weight(1.4f)
                                        .height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF25D366),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Paylaş",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "WHATSAPP'TA PAYLAŞ",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }

                                // "GALERİYE KAYDET" BUTTON
                                IconButton(
                                    onClick = {
                                        ProductImageGenerator.saveBitmapToGallery(
                                            context = context,
                                            bitmap = currentBitmap,
                                            fileNamePrefix = "skt_rapor"
                                        )
                                    },
                                    modifier = Modifier
                                        .size(50.dp)
                                        .background(Color(0xFF334155), RoundedCornerShape(12.dp))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = "Galeriye Kaydet",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageCropperCanvas(
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
                            onDragEnd = {
                                activeHandle = null
                            }
                        )
                    }
            ) {
                drawImage(
                    image = bitmap.asImageBitmap(),
                    dstOffset = androidx.compose.ui.unit.IntOffset(imgLeftPx.toInt(), imgTopPx.toInt()),
                    dstSize = androidx.compose.ui.unit.IntSize(imgWidthPx.toInt(), imgHeightPx.toInt())
                )

                val overlayColor = Color.Black.copy(alpha = 0.65f)

                // Outer rects
                drawRect(overlayColor, topLeft = Offset(0f, 0f), size = Size(containerWidthPx, boxTop))
                drawRect(overlayColor, topLeft = Offset(0f, boxBottom), size = Size(containerWidthPx, containerHeightPx - boxBottom))
                drawRect(overlayColor, topLeft = Offset(0f, boxTop), size = Size(boxLeft, boxBottom - boxTop))
                drawRect(overlayColor, topLeft = Offset(boxRight, boxTop), size = Size(containerWidthPx - boxRight, boxBottom - boxTop))

                // Crop box border
                drawRect(
                    color = Color(0xFF0EA5B7),
                    topLeft = Offset(boxLeft, boxTop),
                    size = Size(boxRight - boxLeft, boxBottom - boxTop),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Grid lines
                val bw = boxRight - boxLeft
                val bh = boxBottom - boxTop
                val gridColor = Color.White.copy(alpha = 0.4f)
                val gridStroke = Stroke(width = 1.dp.toPx())

                drawLine(gridColor, Offset(boxLeft + bw / 3f, boxTop), Offset(boxLeft + bw / 3f, boxBottom), gridStroke.width)
                drawLine(gridColor, Offset(boxLeft + 2 * bw / 3f, boxTop), Offset(boxLeft + 2 * bw / 3f, boxBottom), gridStroke.width)
                drawLine(gridColor, Offset(boxLeft, boxTop + bh / 3f), Offset(boxRight, boxTop + bh / 3f), gridStroke.width)
                drawLine(gridColor, Offset(boxLeft, boxTop + 2 * bh / 3f), Offset(boxRight, boxTop + 2 * bh / 3f), gridStroke.width)

                // Corner Handles
                val handleColor = Color(0xFF0EA5B7)
                val cornerLength = 24.dp.toPx()
                val handleStrokeWidth = 5.dp.toPx()

                // TL
                drawLine(handleColor, Offset(boxLeft - 2f, boxTop), Offset(boxLeft + cornerLength, boxTop), handleStrokeWidth)
                drawLine(handleColor, Offset(boxLeft, boxTop - 2f), Offset(boxLeft, boxTop + cornerLength), handleStrokeWidth)

                // TR
                drawLine(handleColor, Offset(boxRight + 2f, boxTop), Offset(boxRight - cornerLength, boxTop), handleStrokeWidth)
                drawLine(handleColor, Offset(boxRight, boxTop - 2f), Offset(boxRight, boxTop + cornerLength), handleStrokeWidth)

                // BL
                drawLine(handleColor, Offset(boxLeft - 2f, boxBottom), Offset(boxLeft + cornerLength, boxBottom), handleStrokeWidth)
                drawLine(handleColor, Offset(boxLeft, boxBottom + 2f), Offset(boxLeft, boxBottom - cornerLength), handleStrokeWidth)

                // BR
                drawLine(handleColor, Offset(boxRight + 2f, boxBottom), Offset(boxRight - cornerLength, boxBottom), handleStrokeWidth)
                drawLine(handleColor, Offset(boxRight, boxBottom + 2f), Offset(boxRight, boxBottom - cornerLength), handleStrokeWidth)
            }
        }

        // CROP BUTTONS
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1E293B)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFF475569))
                ) {
                    Text("İptal", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Button(
                    onClick = {
                        val px = (cropLeftFrac * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
                        val py = (cropTopFrac * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
                        val pw = ((cropRightFrac - cropLeftFrac) * bitmap.width).toInt().coerceAtLeast(10).coerceAtMost(bitmap.width - px)
                        val ph = ((cropBottomFrac - cropTopFrac) * bitmap.height).toInt().coerceAtLeast(10).coerceAtMost(bitmap.height - py)

                        try {
                            val cropped = Bitmap.createBitmap(bitmap, px, py, pw, ph)
                            onCropApplied(cropped)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            onCancel()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5B7), contentColor = Color.White)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Uygula", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Uygula", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

private enum class CropHandle {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER
}
