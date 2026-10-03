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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.data.StockLog
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.CriticalOrangeDark
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary

@Composable
fun PredictiveAnalyticsSection(
    validProducts: List<Product>,
    stockLogs: List<StockLog>,
    onProductClick: (Product) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Ürün Bazlı Satış & Fire Öngörüsü (Riskli Ürün Öngörüleri - İlk 5 Ürün)
    val productPredictions = remember(validProducts, stockLogs) {
        val activeByBarcode = validProducts
            .filter { it.barkod.isNotBlank() && it.stokAdedi > 0 }
            .groupBy { it.barkod.trim() }

        val predictions = mutableListOf<ProductPrediction>()

        activeByBarcode.forEach { (barcode, productList) ->
            val sampleProduct = productList.first()
            val currentStock = productList.sumOf { it.stokAdedi }
            val logs = stockLogs.filter { it.barcode.equals(barcode, ignoreCase = true) }
            val pastSold = logs.filter { it.actionType == "SATIS" }.sumOf { it.quantity }
            val pastFire = logs.filter { it.actionType == "FIRE" }.sumOf { it.quantity }
            val totalFinished = pastSold + pastFire

            if (totalFinished > 0) {
                val fRate = pastFire.toDouble() / totalFinished.toDouble()
                val sRate = pastSold.toDouble() / totalFinished.toDouble()
                val predictedSales = Math.round(currentStock * sRate).toInt()
                val predictedFire = Math.round(currentStock * fRate).toInt()
                val fPercent = Math.round(fRate * 100).toInt()

                predictions.add(
                    ProductPrediction(
                        product = sampleProduct,
                        barcode = barcode,
                        productName = sampleProduct.urunAdi,
                        currentStock = currentStock,
                        pastSold = pastSold,
                        pastFire = pastFire,
                        pastFireRate = fRate,
                        pastSaleRate = sRate,
                        predictedSales = predictedSales,
                        predictedFire = predictedFire,
                        firePercent = fPercent
                    )
                )
            }
        }

        // Fire riski en yüksek olandan başlayarak sırala, ilk 5 ürünü al
        predictions.sortedWith(
            compareByDescending<ProductPrediction> { it.pastFireRate }
                .thenByDescending { it.predictedFire }
                .thenByDescending { it.currentStock }
        ).take(5)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("analytics_predictions_card"),
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
            // Header
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
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = TurquoiseDark,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Ürün Bazlı Satış & Fire Öngörüsü",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Riskli Ürün Öngörüleri (İlk 5 Ürün)",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TurquoisePrimary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Akıllı Projeksiyon",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TurquoiseDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (productPredictions.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate100,
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "💡", fontSize = 22.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Henüz Yeterli Geçmiş Kayıt Yok",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Ürün detayından stok düşerken 'Satıldı' veya 'Fire' nedenleri seçildikçe, sistem ürünlerin geçmiş eğilimlerini analiz ederek yeni partiler için tahmini fire ve satış riskini burada listeleyecektir.",
                                fontSize = 11.5.sp,
                                color = Slate600,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    productPredictions.forEachIndexed { index, item ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onProductClick(item.product) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (item.firePercent >= 40) androidx.compose.ui.graphics.Color(0xFFFEF2F2) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(
                                1.dp,
                                if (item.firePercent >= 40) androidx.compose.ui.graphics.Color(0xFFFECACA) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 1. Satır: Sıra + Ürün Adı + Rozet
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (item.firePercent >= 40) ExpiredRed else TurquoiseDark
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = item.productName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Uyarı Rozeti: %40+ Fire Beklentisi
                                    if (item.firePercent >= 40) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = ExpiredRed.copy(alpha = 0.12f),
                                            border = BorderStroke(1.dp, ExpiredRed.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = ExpiredRed,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "%${item.firePercent}+ Fire Beklentisi",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = ExpiredRed
                                                )
                                            }
                                        }
                                    } else if (item.firePercent in 20..39) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = CriticalOrange.copy(alpha = 0.12f),
                                            border = BorderStroke(1.dp, CriticalOrange.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = "%${item.firePercent} Fire Riski",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = CriticalOrangeDark,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = EmeraldSuccess.copy(alpha = 0.12f),
                                            border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = "%${item.firePercent} Düşük Fire",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = EmeraldSuccess,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                // 2. Satır: 3 Metrik (Mevcut Stok, Tahmini Satacak, Olası Fire Riski)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "Mevcut Stok", fontSize = 10.sp, color = Slate500)
                                        Text(text = "${item.currentStock} Adet", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "Tahmini Satacak", fontSize = 10.sp, color = EmeraldSuccess)
                                        Text(text = "~${item.predictedSales} Adet", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldSuccess)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = "Olası Fire Riski", fontSize = 10.sp, color = if (item.firePercent >= 40) ExpiredRed else CriticalOrange)
                                        Text(
                                            text = "~${item.predictedFire} Adet (%${item.firePercent})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (item.firePercent >= 40) ExpiredRed else CriticalOrangeDark
                                        )
                                    }
                                }

                                // Mini görsel bar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(2.5.dp))
                                        .background(Slate200),
                                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    if (item.predictedSales > 0) {
                                        Box(
                                            modifier = Modifier
                                                .weight(item.pastSaleRate.toFloat().coerceAtLeast(0.05f))
                                                .fillMaxHeight()
                                                .background(EmeraldSuccess)
                                        )
                                    }
                                    if (item.predictedFire > 0) {
                                        Box(
                                            modifier = Modifier
                                                .weight(item.pastFireRate.toFloat().coerceAtLeast(0.05f))
                                                .fillMaxHeight()
                                                .background(if (item.firePercent >= 40) ExpiredRed else CriticalOrange)
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
}
