package com.example.ui.screens.scanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.NormalGreen
import com.example.ui.theme.NormalGreenContainer
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.SoonYellow
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary

@Composable
fun QrFixSummaryPanel(
    storeCode: String,
    userName: String,
    lastProcessTime: String,
    totalScannedCount: Int,
    successCount: Int,
    errorCount: Int,
    lastProcessedInfo: String?,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Grip Bar
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp)
                .width(36.dp)
                .height(3.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(2.dp))
        )

        // 1. ÜST BAŞLIK SATIRI (Mağaza Kodu, Personel, Saat + Toplam Sayaç Rozeti)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = TurquoisePrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Mağaza: $storeCode",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Personel: $userName",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    if (lastProcessTime.isNotBlank()) {
                        Text(
                            text = " • $lastProcessTime",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Toplam Sayaç Rozeti
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = TurquoisePrimary.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = TurquoisePrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Toplam: $totalScannedCount",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TurquoisePrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. SON İŞLEM BİLGİ ŞERİDİ (Varsa anlık yeşil/açık onay şeridi)
        if (!lastProcessedInfo.isNullOrBlank()) {
            val isSuccess = !lastProcessedInfo.startsWith("⚠️") && !lastProcessedInfo.startsWith("❌")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = if (isSuccess) NormalGreenContainer.copy(alpha = 0.85f) else CriticalOrange.copy(alpha = 0.15f),
                border = BorderStroke(
                    1.dp,
                    if (isSuccess) NormalGreen.copy(alpha = 0.6f) else CriticalOrange.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isSuccess) NormalGreen else CriticalOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = lastProcessedInfo,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSuccess) Slate900 else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 3. DURUM KARTLARI (Yan yana 2 özet kartı)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Yeşil Kart: Güncellenen / Fiyatı Eklenen
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = NormalGreen.copy(alpha = 0.12f)
                ),
                border = BorderStroke(1.dp, NormalGreen.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = NormalGreen.copy(alpha = 0.25f),
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = NormalGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$successCount",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = NormalGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Fiyat Güncellendi",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Kırmızı / Mavi Kart: Hatalı veya Okunamayan Sayacı
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (errorCount > 0) ExpiredRed.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(
                    1.dp,
                    if (errorCount > 0) ExpiredRed.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (errorCount > 0) ExpiredRed.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (errorCount > 0) ExpiredRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$errorCount",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = if (errorCount > 0) ExpiredRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Eşleşmeyen / Hatalı",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. ALT BUTON: Listeyi Gör / Tamamlanan İşlemleri Kapat
        Button(
            onClick = onCloseClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TurquoiseDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FormatListBulleted,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tamamlanan İşlemleri Kapat / Listeyi Gör",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
