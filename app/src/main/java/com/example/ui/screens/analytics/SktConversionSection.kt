package com.example.ui.screens.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.data.StockLog
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.TurquoiseDark

@Composable
fun SktConversionSection(
    stockLogs: List<StockLog>,
    validProducts: List<Product>,
    modifier: Modifier = Modifier
) {
    var akibetTimeframe by remember { mutableStateOf(AnalyticsTimeframe.LAST_30_DAYS) }

    val akibetCutoffTime = remember(akibetTimeframe) {
        System.currentTimeMillis() - (akibetTimeframe.days.toLong() * 86400000L)
    }

    val periodStockLogs = remember(stockLogs, akibetCutoffTime) {
        stockLogs.filter { it.timestamp >= akibetCutoffTime }
    }

    val periodSoldQty = remember(periodStockLogs) {
        periodStockLogs.filter { it.actionType == "SATIS" }.sumOf { it.quantity }
    }

    val periodFireQty = remember(periodStockLogs) {
        periodStockLogs.filter { it.actionType == "FIRE" }.sumOf { it.quantity }
    }

    val currentRaftaQty = remember(validProducts) {
        validProducts.sumOf { it.stokAdedi }
    }

    val totalAkibetQty = remember(periodSoldQty, periodFireQty, currentRaftaQty) {
        periodSoldQty + periodFireQty + currentRaftaQty
    }

    val soldRatio = if (totalAkibetQty > 0) periodSoldQty.toDouble() / totalAkibetQty.toDouble() else 0.0
    val fireRatio = if (totalAkibetQty > 0) periodFireQty.toDouble() / totalAkibetQty.toDouble() else 0.0
    val raftaRatio = if (totalAkibetQty > 0) currentRaftaQty.toDouble() / totalAkibetQty.toDouble() else 0.0

    val soldPercent = Math.round(soldRatio * 100).toInt()
    val firePercent = Math.round(fireRatio * 100).toInt()
    val raftaPercent = if (totalAkibetQty > 0) (100 - soldPercent - firePercent).coerceAtLeast(0) else 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("analytics_akibet_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Başlık ve 7/30/60 Gün Seçim Hapları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = null,
                        tint = TurquoiseDark,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "SKT Giriş & Akıbet Dağılımı",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Dönem: ${akibetTimeframe.label}",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }

                // 7/30/60 Gün Pill Selector
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    AnalyticsTimeframe.values().forEach { tf ->
                        val isSel = akibetTimeframe == tf
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) TurquoiseDark else Color.Transparent)
                                .clickable { akibetTimeframe = tf }
                                .padding(horizontal = 7.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${tf.days}G",
                                fontSize = 10.5.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else Slate600
                            )
                        }
                    }
                }
            }

            // Yatay segmentli ilerleme çubuğu (Linear Segmented Bar)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (totalAkibetQty == 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .background(Slate200)
                        )
                    } else {
                        if (periodSoldQty > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(soldRatio.toFloat().coerceAtLeast(0.04f))
                                    .fillMaxHeight()
                                    .background(EmeraldSuccess)
                            )
                        }
                        if (periodFireQty > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(fireRatio.toFloat().coerceAtLeast(0.04f))
                                    .fillMaxHeight()
                                    .background(ExpiredRed)
                            )
                        }
                        if (currentRaftaQty > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(raftaRatio.toFloat().coerceAtLeast(0.04f))
                                    .fillMaxHeight()
                                    .background(Color(0xFF0EA5E9))
                            )
                        }
                    }
                }

                // Dağılım Lejantı
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EmeraldSuccess))
                        Text(text = "Satılan (%$soldPercent)", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ExpiredRed))
                        Text(text = "Fire (%$firePercent)", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF0EA5E9)))
                        Text(text = "Rafta (%$raftaPercent)", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
                    }
                }
            }

            // 3 adet temiz özet kutusu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Kutu 1: Satılan / Kurtarılan
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldSuccess.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Satılan / Kurtarılan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$periodSoldQty Adet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "%$soldPercent Oran",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldSuccess
                        )
                    }
                }

                // Kutu 2: Fire / İmha Edilen
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = ExpiredRed.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, ExpiredRed.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Fire / İmha",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ExpiredRed,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$periodFireQty Adet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "%$firePercent Kayıp",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ExpiredRed
                        )
                    }
                }

                // Kutu 3: Rafta Satışta Olan
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0EA5E9).copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color(0xFF0EA5E9).copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Rafta Satışta",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$currentRaftaQty Adet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "%$raftaPercent Aktif",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0284C7)
                        )
                    }
                }
            }

            // Alt Özet Bilgisi
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Toplam Sisteme Girilen SKT Adedi: $totalAkibetQty Adet (${akibetTimeframe.label})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate600,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
