package com.example.ui.components.report

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.util.ProductImageGenerator

private val BorderSubtle = Color(0xFFE2E8F0)
private val BorderStrong = Color(0xFFCBD5E1)
private val TextPrimary = Color(0xFF0F172A)
private val LightSurface = Color(0xFFFFFFFF)

@Composable
fun ReportBottomActionBar(
    context: Context,
    title: String,
    noteText: String,
    currentProducts: List<Product>,
    activeBitmap: Bitmap,
    isCropping: Boolean,
    onStartCrop: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = LightSurface,
        border = BorderStroke(1.dp, BorderSubtle),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. SOL: KIRP
            OutlinedButton(
                onClick = onStartCrop,
                modifier = Modifier
                    .height(46.dp)
                    .testTag("btn_preview_crop"),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = TextPrimary
                ),
                border = BorderStroke(1.dp, BorderStrong)
            ) {
                Icon(
                    imageVector = Icons.Default.Crop,
                    contentDescription = "Kırp",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "KIRP",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    color = TextPrimary
                )
            }

            // 2. ORTA: WHATSAPP'TA PAYLAŞ
            Button(
                onClick = {
                    val finalBmp = if (isCropping) activeBitmap else {
                        ProductImageGenerator.createSharePreviewReportBitmap(
                            title = title,
                            note = noteText,
                            productList = currentProducts
                        )
                    }
                    ProductImageGenerator.shareBitmap(
                        context = context,
                        bitmap = finalBmp,
                        fileNamePrefix = "skt_rapor"
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("btn_preview_whatsapp_share"),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF25D366),
                    contentColor = Color.White
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "WhatsApp",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "WhatsApp Paylaş",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                }
            }

            // 3. SAĞ: İNDİR (Cihaza görsel olarak kaydet)
            Surface(
                onClick = {
                    val finalBmp = if (isCropping) activeBitmap else {
                        ProductImageGenerator.createSharePreviewReportBitmap(
                            title = title,
                            note = noteText,
                            productList = currentProducts
                        )
                    }
                    ProductImageGenerator.saveBitmapToGallery(
                        context = context,
                        bitmap = finalBmp,
                        fileNamePrefix = "skt_rapor"
                    )
                },
                modifier = Modifier
                    .size(46.dp)
                    .testTag("btn_preview_download"),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "İndir",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
