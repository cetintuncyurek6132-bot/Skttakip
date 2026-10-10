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
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.data.StockLog
import com.example.data.getTodayMidnightMillis
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary

@Composable
fun ReyonRiskSummarySection(
    products: List<Product>,
    stockLogs: List<StockLog>,
    modifier: Modifier = Modifier
) {
    val todayMidnight = remember { getTodayMidnightMillis() }

    // Dolap ve Gıda Ürün Grupları
    val dolapProducts = remember(products) {
        products.filter { it.stokAdedi > 0 && it.sktTarihi > 0L && it.kategori.equals("Dolap", ignoreCase = true) }
    }
    val gidaProducts = remember(products) {
        products.filter { it.stokAdedi > 0 && it.sktTarihi > 0L && !it.kategori.equals("Dolap", ignoreCase = true) }
    }

    // Dolap İstatistikleri
    val dolapCritical = remember(dolapProducts, todayMidnight) {
        dolapProducts.filter { it.getRemainingDays(todayMidnight) in 0L..3L }
    }
    val dolapExpired = remember(dolapProducts, todayMidnight) {
        dolapProducts.filter { it.getRemainingDays(todayMidnight) < 0L }
    }
    val dolapTotalStock = remember(dolapProducts) { dolapProducts.sumOf { it.stokAdedi } }
    val dolapCriticalStock = remember(dolapCritical, dolapExpired) {
        dolapCritical.sumOf { it.stokAdedi } + dolapExpired.sumOf { it.stokAdedi }
    }

    // Dolap Logları (Fire & Satış)
    val dolapFireLogs = remember(stockLogs, products) {
        stockLogs.filter { log ->
            log.actionType == "FIRE" && products.any { p ->
                (p.barkod.isNotBlank() && p.barkod == log.barcode && p.kategori.equals("Dolap", ignoreCase = true)) ||
                (p.urunAdi.equals(log.productName, ignoreCase = true) && p.kategori.equals("Dolap", ignoreCase = true))
            }
        }.sumOf { it.quantity }
    }
    val dolapFireRate = if (dolapTotalStock + dolapFireLogs > 0) {
        Math.round(((dolapCriticalStock + dolapFireLogs).toDouble() / (dolapTotalStock + dolapFireLogs).toDouble()) * 100).toInt().coerceIn(0, 100)
    } else 0

    // Gıda İstatistikleri
    val gidaCritical = remember(gidaProducts, todayMidnight) {
        gidaProducts.filter { it.getRemainingDays(todayMidnight) in 0L..3L }
    }
    val gidaExpired = remember(gidaProducts, todayMidnight) {
        gidaProducts.filter { it.getRemainingDays(todayMidnight) < 0L }
    }
    val gidaTotalStock = remember(gidaProducts) { gidaProducts.sumOf { it.stokAdedi } }
    val gidaCriticalStock = remember(gidaCritical, gidaExpired) {
        gidaCritical.sumOf { it.stokAdedi } + gidaExpired.sumOf { it.stokAdedi }
    }

    val gidaFireLogs = remember(stockLogs, products) {
        stockLogs.filter { log ->
            log.actionType == "FIRE" && products.any { p ->
                (p.barkod.isNotBlank() && p.barkod == log.barcode && !p.kategori.equals("Dolap", ignoreCase = true)) ||
                (p.urunAdi.equals(log.productName, ignoreCase = true) && !p.kategori.equals("Dolap", ignoreCase = true))
            }
        }.sumOf { it.quantity }
    }
    val gidaFireRate = if (gidaTotalStock + gidaFireLogs > 0) {
        Math.round(((gidaCriticalStock + gidaFireLogs).toDouble() / (gidaTotalStock + gidaFireLogs).toDouble()) * 100).toInt().coerceIn(0, 100)
    } else 0

    var selectedTab by remember { mutableStateOf(0) } // 0: Yan Yana Özet, 1: Dolap, 2: Gıda

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("reyon_risk_summary_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Başlık
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = TurquoiseDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Reyon Bazlı Risk Özeti",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Dolap (Soğuk Zincir) vs. Kuru Gıda Karşılaştırması",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TurquoiseDark.copy(alpha = 0.12f),
                    border = BorderStroke(0.8.dp, TurquoiseDark.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Reyon Sağlığı",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TurquoiseDark,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            // Yan Yana 2 Kutu: Dolap vs Kuru Gıda
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // KUTU 1: DOLAP REYONU (SOĞUK ZİNCİR)
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF0F9FF), // Hafif Buzul Mavisi
                    border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                    shadowElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Üst Başlık & İkon
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(
                                    imageVector = Icons.Default.AcUnit,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Dolap Reyonu",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0369A1)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFE0F2FE)
                            ) {
                                Text(
                                    text = "Soğuk Zincir",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0284C7),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        // Kritik Adet
                        Column {
                            Text(text = "Kritik & Dolmuş", fontSize = 10.sp, color = Slate600)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$dolapCriticalStock",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (dolapCriticalStock > 0) ExpiredRed else Color(0xFF0369A1)
                                )
                                Text(
                                    text = " / $dolapTotalStock Adet",
                                    fontSize = 11.sp,
                                    color = Slate500,
                                    modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
                                )
                            }
                        }

                        // Fire Oranı & Çubuk
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Fire Oranı", fontSize = 9.5.sp, color = Slate600)
                                Text(
                                    text = "%$dolapFireRate",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (dolapFireRate >= 20) ExpiredRed else if (dolapFireRate >= 10) CriticalOrange else EmeraldSuccess
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (dolapFireRate / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = if (dolapFireRate >= 20) ExpiredRed else if (dolapFireRate >= 10) CriticalOrange else EmeraldSuccess,
                                trackColor = Color(0xFFE0F2FE)
                            )
                        }
                    }
                }

                // KUTU 2: KURU GIDA REYONU
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFFBEB), // Hafif Saman / Kehribar Sarısı
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    shadowElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Üst Başlık & İkon
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Kitchen,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Kuru Gıda",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = "Raf & Bakliyat",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        // Kritik Adet
                        Column {
                            Text(text = "Kritik & Dolmuş", fontSize = 10.sp, color = Slate600)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$gidaCriticalStock",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (gidaCriticalStock > 0) CriticalOrange else Color(0xFFB45309)
                                )
                                Text(
                                    text = " / $gidaTotalStock Adet",
                                    fontSize = 11.sp,
                                    color = Slate500,
                                    modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
                                )
                            }
                        }

                        // Fire Oranı & Çubuk
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Fire Oranı", fontSize = 9.5.sp, color = Slate600)
                                Text(
                                    text = "%$gidaFireRate",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (gidaFireRate >= 20) ExpiredRed else if (gidaFireRate >= 10) CriticalOrange else EmeraldSuccess
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (gidaFireRate / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = if (gidaFireRate >= 20) ExpiredRed else if (gidaFireRate >= 10) CriticalOrange else EmeraldSuccess,
                                trackColor = Color(0xFFFEF3C7)
                            )
                        }
                    }
                }
            }
        }
    }
}
