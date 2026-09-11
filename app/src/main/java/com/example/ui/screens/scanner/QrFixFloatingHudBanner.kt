package com.example.ui.screens.scanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NormalGreen
import com.example.ui.theme.NormalGreenContainer
import com.example.ui.theme.Slate900
import com.example.ui.theme.SoonYellow
import com.example.ui.theme.SoonYellowContainer
import com.example.ui.theme.TurquoisePrimary

@Composable
fun QrFixFloatingHudBanner(
    qrFixResultMsg: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.5f)),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = TurquoisePrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ETİKET QR İLE BARKOD & FİYAT DÜZELTME MODU",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Raf etiketindeki QR kodunu okutun. Ürün koduna göre barkod ve fiyat otomatik güncellenir.",
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.75f),
                lineHeight = 14.sp
            )
            if (qrFixResultMsg.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (qrFixResultMsg.startsWith("✅")) NormalGreenContainer else SoonYellowContainer,
                    border = BorderStroke(1.dp, if (qrFixResultMsg.startsWith("✅")) NormalGreen else SoonYellow)
                ) {
                    Text(
                        text = qrFixResultMsg,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (qrFixResultMsg.startsWith("✅")) NormalGreen else Slate900,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
