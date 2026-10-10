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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
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
    // Ürün Bazlı Satış & Fire Öngörüsü (Riskli Parti Öngörüleri - İlk 5 Ürün / Parti)
    val productPredictions = remember(validProducts, stockLogs) {
        val zone = ZoneId.systemDefault()
        val bugunLocalDate = LocalDate.now(zone)

        // Sadece stoğu olan ve geçerli SKT'ye sahip partileri bağımsız (batch bazlı) olarak ele al
        val activeBatches = validProducts.filter { it.stokAdedi > 0 && it.sktTarihi > 0L }
        val predictions = mutableListOf<ProductPrediction>()

        activeBatches.forEach { product ->
            // İlgili partinin barkodu veya adına ait geçmiş log kayıtlarını bul
            val logs = stockLogs.filter { log ->
                (product.barkod.isNotBlank() && log.barcode.equals(product.barkod, ignoreCase = true)) ||
                (log.productName.isNotBlank() && log.productName.trim().equals(product.urunAdi.trim(), ignoreCase = true))
            }

            val pastSold = logs.filter { it.actionType == "SATIS" }.sumOf { it.quantity }
            val pastFire = logs.filter { it.actionType == "FIRE" }.sumOf { it.quantity }

            // 1. Gözlem Süresi (Payda): Ürünün ilk eklenme tarihi veya en eski log zamanı
            val minProductEklenme = if (product.eklenmeTarihi > 0L) product.eklenmeTarihi else null
            val earliestLogTime = logs
                .filter { it.actionType == "SKT_GIRIS" || it.actionType == "SATIS" }
                .map { it.timestamp }
                .filter { it > 0L }
                .minOrNull()

            val observationStartTime = listOfNotNull(minProductEklenme, earliestLogTime).minOrNull() ?: product.eklenmeTarihi
            val eklenmeLocalDate = try {
                Instant.ofEpochMilli(observationStartTime).atZone(zone).toLocalDate()
            } catch (_: Exception) {
                bugunLocalDate
            }

            val gecenGun = maxOf(1L, ChronoUnit.DAYS.between(eklenmeLocalDate, bugunLocalDate))

            // 2. Gerçek Günlük Satış Hızı: Toplam satış adedi / geçen gün sayısı
            val gunlukSatisHizi = pastSold.toDouble() / gecenGun.toDouble()

            // 3. Kalan Gün ve Beklenen Satış / Fire
            val kalanGun = product.getRemainingDays(bugunLocalDate)

            val beklenenSatis: Int
            val beklenenFire: Int
            val firePercent: Int
            val isExpired: Boolean

            if (kalanGun <= 0L) {
                // KURAL A (Süresi Dolanlar): Ürün artık satılamaz, tamamı doğrudan %100 fire
                beklenenSatis = 0
                beklenenFire = product.stokAdedi
                firePercent = 100
                isExpired = true
            } else {
                // KURAL B (SKT'si Devam Edenler)
                val beklenenSatisDouble = Math.min(product.stokAdedi.toDouble(), gunlukSatisHizi * kalanGun.toDouble())
                beklenenSatis = Math.round(beklenenSatisDouble).toInt().coerceIn(0, product.stokAdedi)
                beklenenFire = Math.max(0, product.stokAdedi - beklenenSatis)
                firePercent = if (product.stokAdedi > 0) {
                    Math.round((beklenenFire.toDouble() / product.stokAdedi.toDouble()) * 100).toInt().coerceIn(0, 100)
                } else 0
                isExpired = false
            }

            val pastSaleRate = if (product.stokAdedi > 0) beklenenSatis.toDouble() / product.stokAdedi.toDouble() else 0.0
            val pastFireRate = if (product.stokAdedi > 0) beklenenFire.toDouble() / product.stokAdedi.toDouble() else 0.0
            val isHighRisk = isExpired || (beklenenFire > 0 && firePercent >= 30)

            // Geçmiş log kaydı olan veya satış/fire kaydı bulunan ürünleri listeye al
            if (logs.isNotEmpty() || pastSold > 0 || pastFire > 0) {
                predictions.add(
                    ProductPrediction(
                        product = product,
                        barcode = product.barkod,
                        productName = product.urunAdi,
                        currentStock = product.stokAdedi,
                        pastSold = pastSold,
                        pastFire = pastFire,
                        pastFireRate = pastFireRate,
                        pastSaleRate = pastSaleRate,
                        predictedSales = beklenenSatis,
                        predictedFire = beklenenFire,
                        firePercent = firePercent,
                        observationDays = gecenGun,
                        dailySalesVelocity = gunlukSatisHizi,
                        remainingDays = kalanGun,
                        isExpired = isExpired,
                        isHighRisk = isHighRisk
                    )
                )
            }
        }

        // Fire riski en yüksek olandan başlayarak sırala (önce firePercent, sonra predictedFire, sonra azalan gün), ilk 5'i al
        predictions.sortedWith(
            compareByDescending<ProductPrediction> { it.firePercent }
                .thenByDescending { it.predictedFire }
                .thenBy { it.remainingDays }
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
                            color = if (item.isHighRisk) androidx.compose.ui.graphics.Color(0xFFFEF2F2) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(
                                1.dp,
                                if (item.isHighRisk) androidx.compose.ui.graphics.Color(0xFFFECACA) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 1. Satır: Sıra + Ürün Adı & Parti SKT + Rozet
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
                                            color = if (item.isHighRisk) ExpiredRed else TurquoiseDark
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.productName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Parti SKT: ${item.product.getFormattedSkt()} • ${if (item.remainingDays <= 0) "Süresi Doldu" else "${item.remainingDays} gün kaldı"}",
                                                fontSize = 10.sp,
                                                color = if (item.isExpired || item.remainingDays <= 2) ExpiredRed else Slate500
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Uyarı Rozetleri
                                    if (item.isExpired) {
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
                                                    text = "SÜRESİ DOLDU / %100 FİRE",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = ExpiredRed
                                                )
                                            }
                                        }
                                    } else if (item.isHighRisk) {
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
                                                    text = "YÜKSEK FİRE RİSKİ",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = ExpiredRed
                                                )
                                            }
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = EmeraldSuccess.copy(alpha = 0.12f),
                                            border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = "DÜŞÜK RİSK / ERİTİLEBİLİR",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = EmeraldSuccess,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                // 2. Satır: 3 Metrik (Mevcut Stok, Tahmini Satış, Tahmini Fire)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "Mevcut Stok", fontSize = 10.sp, color = Slate500)
                                        Text(
                                            text = "${item.currentStock} Adet",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "Tahmini Satış", fontSize = 10.sp, color = EmeraldSuccess)
                                        Text(
                                            text = "${item.predictedSales} Adet",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = EmeraldSuccess
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Tahmini Fire",
                                            fontSize = 10.sp,
                                            color = if (item.isHighRisk) ExpiredRed else CriticalOrange
                                        )
                                        Text(
                                            text = "${item.predictedFire} Adet (%${item.firePercent})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (item.isHighRisk) ExpiredRed else CriticalOrangeDark
                                        )
                                    }
                                }

                                // 3. Satır: Satış Hızı Bilgilendirmesi
                                Text(
                                    text = "Satış Hızı: ${String.format(Locale.forLanguageTag("tr-TR"), "%.2f", item.dailySalesVelocity)} adet/gün (${item.observationDays} günde ${item.pastSold} satış)",
                                    fontSize = 9.5.sp,
                                    color = Slate500
                                )

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
                                                .background(if (item.isHighRisk) ExpiredRed else CriticalOrange)
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
