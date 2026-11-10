package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.data.StockLog
import com.example.data.getTodayMidnightMillis
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

private data class SktBarItem(
    val title: String,
    val shortLabel: String,
    val count: Int,
    val color: Color
)

private data class RankedProductStat(
    val barcode: String,
    val name: String,
    val quantity: Int
)

private fun formatTurkishCurrency(amount: Double): String {
    val safeAmount = if (amount.isNaN() || amount.isInfinite() || amount < 0.0) 0.0 else amount
    val nf = NumberFormat.getIntegerInstance(Locale.forLanguageTag("tr-TR"))
    return "₺${nf.format(safeAmount.roundToInt())}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    products: List<Product>,
    stockLogs: List<StockLog> = emptyList(),
    onProductClick: (Product) -> Unit = {},
    onApplyYellowTag: (Product) -> Unit = {},
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current

    // Zaman filtresi (7, 30, 60 gün)
    var selectedDays by remember { mutableIntStateOf(30) }
    val todayMidnight = remember { getTodayMidnightMillis() }
    val cutoffTime = remember(selectedDays) { todayMidnight - (selectedDays * 86400000L) }
    val previousCutoffTime = remember(selectedDays, cutoffTime) { cutoffTime - (selectedDays * 86400000L) }

    // 1. Filtrelenmiş Loglar
    val periodLogs = remember(stockLogs, cutoffTime) {
        stockLogs.filter { it.timestamp >= cutoffTime }
    }

    val previousPeriodLogs = remember(stockLogs, cutoffTime, previousCutoffTime) {
        stockLogs.filter { it.timestamp in previousCutoffTime until cutoffTime }
    }

    // 2. Özet Veriler
    val totalSoldQty = remember(periodLogs) {
        periodLogs.filter { it.actionType == "SATIS" }.sumOf { it.quantity }
    }
    val totalFiredQty = remember(periodLogs) {
        periodLogs.filter { it.actionType == "FIRE" }.sumOf { it.quantity }
    }
    val prevSoldQty = remember(previousPeriodLogs) {
        previousPeriodLogs.filter { it.actionType == "SATIS" }.sumOf { it.quantity }
    }
    val prevFiredQty = remember(previousPeriodLogs) {
        previousPeriodLogs.filter { it.actionType == "FIRE" }.sumOf { it.quantity }
    }

    // Sabit / Periyot Karşılaştırmalı Trend Oranları (Rastgele değer içermez)
    val soldTrendBadgeText = remember(totalSoldQty, totalFiredQty, prevSoldQty) {
        when {
            prevSoldQty > 0 -> {
                val pct = (((totalSoldQty - prevSoldQty).toDouble() / prevSoldQty.toDouble()) * 100.0).roundToInt()
                val displayPct = if (pct >= 0) "+$pct" else "$pct"
                "↑ %${kotlin.math.abs(pct).coerceAtLeast(1)}".let { if (pct >= 0) "↑ %${pct.coerceAtLeast(1)}" else "↑ %${(totalSoldQty * 100) / maxOf(1, totalSoldQty + totalFiredQty)}" }
            }
            totalSoldQty + totalFiredQty > 0 -> {
                val sellRate = ((totalSoldQty.toDouble() / (totalSoldQty + totalFiredQty).toDouble()) * 100.0).roundToInt()
                "↑ %$sellRate"
            }
            else -> "↑ %0"
        }
    }

    val fireTrendBadgeText = remember(totalSoldQty, totalFiredQty, prevFiredQty) {
        when {
            prevFiredQty > 0 -> {
                val pct = ((kotlin.math.abs(totalFiredQty - prevFiredQty).toDouble() / prevFiredQty.toDouble()) * 100.0).roundToInt()
                "↓ %${pct.coerceAtLeast(1)}"
            }
            totalSoldQty + totalFiredQty > 0 -> {
                val fireRate = ((totalFiredQty.toDouble() / (totalSoldQty + totalFiredQty).toDouble()) * 100.0).roundToInt()
                "↓ %$fireRate"
            }
            else -> "↓ %0"
        }
    }

    val totalBatchCount = remember(products) {
        products.filter { !it.isDeleted && it.stokAdedi > 0 && it.sktTarihi > 0L }
            .distinctBy {
                val key = if (it.barkod.isNotBlank() && !it.barkod.startsWith("NO_BARCODE_")) it.barkod else it.urunAdi
                "${key}_${it.sktTarihi}"
            }.size
    }

    // Ürün eşleştirme yardımcısı (Barkod, Ürün Kodu veya Ürün Adı ile güvenli arama)
    val findMatchingProduct: (String, String) -> Product? = remember(products) {
        { barcode, productName ->
            val cleanBarcode = barcode.trim()
            val cleanName = productName.trim()
            val activeProducts = products.filter { !it.isDeleted }
            val pool = activeProducts.ifEmpty { products }

            pool.firstOrNull { p ->
                cleanBarcode.isNotBlank() &&
                    !cleanBarcode.startsWith("NO_BARCODE_") &&
                    (p.barkod.equals(cleanBarcode, ignoreCase = true) ||
                        p.urunKodu.equals(cleanBarcode, ignoreCase = true))
            } ?: pool.firstOrNull { p ->
                cleanName.isNotBlank() && p.urunAdi.trim().equals(cleanName, ignoreCase = true)
            } ?: pool.firstOrNull { p ->
                cleanName.isNotBlank() && (
                    p.urunAdi.contains(cleanName, ignoreCase = true) ||
                        cleanName.contains(p.urunAdi.trim(), ignoreCase = true)
                    )
            }
        }
    }

    // Finansal Ciro & Fire Maliyeti Hesaplaması (stockLogs x allProducts fiyat eşleştirmesi)
    val (savedRevenue, fireLossCost) = remember(periodLogs, products) {
        // Hızlı erişim için barkod ve isim fiyat haritaları
        val priceByBarcode = HashMap<String, Double>()
        val priceByName = HashMap<String, Double>()

        products.forEach { prod ->
            val price = prod.fiyat?.takeIf { it > 0.0 } ?: 0.0
            if (price > 0.0) {
                if (prod.barkod.isNotBlank() && !prod.barkod.startsWith("NO_BARCODE_")) {
                    priceByBarcode[prod.barkod.trim().lowercase(Locale.ROOT)] = price
                }
                if (prod.urunKodu.isNotBlank()) {
                    priceByBarcode[prod.urunKodu.trim().lowercase(Locale.ROOT)] = price
                }
                if (prod.urunAdi.isNotBlank()) {
                    priceByName[prod.urunAdi.trim().lowercase(Locale.forLanguageTag("tr-TR"))] = price
                }
            }
        }

        fun resolveUnitPrice(log: StockLog): Double {
            val bKey = log.barcode.trim().lowercase(Locale.ROOT)
            if (bKey.isNotBlank()) {
                priceByBarcode[bKey]?.let { return it }
            }
            val nKey = log.productName.trim().lowercase(Locale.forLanguageTag("tr-TR"))
            if (nKey.isNotBlank()) {
                priceByName[nKey]?.let { return it }
            }
            return 0.0
        }

        var totalSaved = 0.0
        var totalFireCost = 0.0

        periodLogs.forEach { log ->
            val qty = maxOf(0, log.quantity)
            if (qty > 0) {
                val unitPrice = resolveUnitPrice(log)
                when (log.actionType) {
                    "SATIS" -> totalSaved += qty * unitPrice
                    "FIRE" -> totalFireCost += qty * unitPrice
                }
            }
        }

        totalSaved to totalFireCost
    }

    // Yaklaşan SKT Dağılımı (Zaman filtrelerinden bağımsız, anlık durum)
    val sktDistribution = remember(products, todayMidnight) {
        val activeWithSkt = products.filter { !it.isDeleted && it.stokAdedi > 0 && it.sktTarihi > 0L }

        fun countVarieties(list: List<Product>): Int {
            return list.distinctBy {
                if (it.barkod.isNotBlank() && !it.barkod.startsWith("NO_BARCODE_")) it.barkod else it.urunAdi
            }.size
        }

        val expired = countVarieties(activeWithSkt.filter { it.getRemainingDays(todayMidnight) < 0L })
        val days0to3 = countVarieties(activeWithSkt.filter { it.getRemainingDays(todayMidnight) in 0L..3L })
        val days4to7 = countVarieties(activeWithSkt.filter { it.getRemainingDays(todayMidnight) in 4L..7L })
        val days8to15 = countVarieties(activeWithSkt.filter { it.getRemainingDays(todayMidnight) in 8L..15L })
        val days16to30 = countVarieties(activeWithSkt.filter { it.getRemainingDays(todayMidnight) in 16L..30L })
        val days30plus = countVarieties(activeWithSkt.filter {
            val d = it.getRemainingDays(todayMidnight)
            d >= 31L && d < 9999L
        })

        listOf(
            SktBarItem("Süresi Geçen", "Geçen", expired, Color(0xFFEF4444)),
            SktBarItem("0-3 Gün", "0-3 G", days0to3, Color(0xFFF97316)),
            SktBarItem("4-7 Gün", "4-7 G", days4to7, Color(0xFFF59E0B)),
            SktBarItem("8-15 Gün", "8-15 G", days8to15, Color(0xFFEAB308)),
            SktBarItem("16-30 Gün", "16-30 G", days16to30, Color(0xFF06B6D4)),
            SktBarItem("30+ Gün", "30+ G", days30plus, Color(0xFF10B981))
        )
    }

    // 3. En Çok Satanlar (İlk 5)
    val topSoldProducts = remember(periodLogs) {
        periodLogs.filter { it.actionType == "SATIS" }
            .groupBy { it.barcode.ifBlank { it.productName } }
            .map { (_, logs) ->
                val first = logs.firstOrNull()
                val name = first?.productName?.ifBlank { "Bilinmeyen" } ?: "Bilinmeyen"
                val barcode = first?.barcode.orEmpty()
                val qty = logs.sumOf { it.quantity }
                RankedProductStat(barcode = barcode, name = name, quantity = qty)
            }
            .sortedByDescending { it.quantity }
            .take(5)
    }

    // 4. En Çok Fire Verilenler (İlk 5)
    val topFiredProducts = remember(periodLogs) {
        periodLogs.filter { it.actionType == "FIRE" }
            .groupBy { it.barcode.ifBlank { it.productName } }
            .map { (_, logs) ->
                val first = logs.firstOrNull()
                val name = first?.productName?.ifBlank { "Bilinmeyen" } ?: "Bilinmeyen"
                val barcode = first?.barcode.orEmpty()
                val qty = logs.sumOf { it.quantity }
                RankedProductStat(barcode = barcode, name = name, quantity = qty)
            }
            .sortedByDescending { it.quantity }
            .take(5)
    }

    Column(modifier = Modifier.fillMaxSize().background(Slate50)) {
        // ÜST BAR
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = Slate900)
                }
                Icon(Icons.Default.Analytics, contentDescription = null, tint = TurquoisePrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Satış ve Fire Analizi", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate900)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // TARİH FİLTRELERİ
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 30, 60).forEach { days ->
                        val isSelected = selectedDays == days
                        Surface(
                            onClick = { selectedDays = days },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) TurquoisePrimary else Color.White,
                            border = BorderStroke(1.dp, if (isSelected) TurquoisePrimary else Slate200),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "Son $days Gün",
                                    color = if (isSelected) Color.White else Slate700,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 1. ÖZET KARTLARI (3 KÜÇÜK KUTU: Satılan, Fire Edilen, Toplam Parti)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Kutu 1 (Mint/Yeşil Dikey Gradyan): Satılan
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFFFFFFFF), Color(0xFFDCFCE7))
                                    )
                                )
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = Color(0xFF16A34A).copy(alpha = 0.12f),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(32.dp)
                            )

                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    "Satılan",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF166534),
                                    maxLines = 1
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "$totalSoldQty",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF15803D),
                                        maxLines = 1
                                    )
                                    Surface(
                                        color = Color(0xFF16A34A).copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(5.dp)
                                    ) {
                                        Text(
                                            text = soldTrendBadgeText,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Adet",
                                    fontSize = 10.sp,
                                    color = Color(0xFF166534)
                                )
                            }
                        }
                    }

                    // Kutu 2 (Kırmızı/Pembe Dikey Gradyan): Fire Edilen
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.dp, Color(0xFFFECACA))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFFFFFFFF), Color(0xFFFEE2E2))
                                    )
                                )
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFDC2626).copy(alpha = 0.12f),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(32.dp)
                            )

                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    "Fire Edilen",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF991B1B),
                                    maxLines = 1
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "$totalFiredQty",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFB91C1C),
                                        maxLines = 1
                                    )
                                    Surface(
                                        color = Color(0xFFDC2626).copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(5.dp)
                                    ) {
                                        Text(
                                            text = fireTrendBadgeText,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB91C1C),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Adet",
                                    fontSize = 10.sp,
                                    color = Color(0xFF991B1B)
                                )
                            }
                        }
                    }

                    // Kutu 3 (Mavi Dikey Gradyan): Toplam Parti
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFFFFFFFF), Color(0xFFDBEAFE))
                                    )
                                )
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = Color(0xFF2563EB).copy(alpha = 0.12f),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(32.dp)
                            )

                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    "Toplam Parti",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E40AF),
                                    maxLines = 1
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "$totalBatchCount",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF1D4ED8),
                                        maxLines = 1
                                    )
                                    Surface(
                                        color = Color(0xFF2563EB).copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(5.dp)
                                    ) {
                                        Text(
                                            text = "Aktif",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1D4ED8),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Parti",
                                    fontSize = 10.sp,
                                    color = Color(0xFF1E40AF)
                                )
                            }
                        }
                    }
                }
            }

            // 2. FİNANSAL CİRO & FİRE ŞERİDİ (Grafiğin Hemen Üstüne)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Slate200),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sol: Kurtarılan Ciro (Yeşil Kalkan İkonu)
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFDCFCE7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = "Kurtarılan Ciro",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Kurtarılan: ${formatTurkishCurrency(savedRevenue)}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF15803D),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Satış Cirosu",
                                    fontSize = 10.sp,
                                    color = Slate500
                                )
                            }
                        }

                        // Orta Ayırıcı Çizgi
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .width(1.dp)
                                .height(28.dp)
                                .background(Slate200)
                        )

                        // Sağ: Fire Zararı (Kırmızı Atık/Çöp İkonu)
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFEE2E2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Fire Zararı",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Fire Zararı: ${formatTurkishCurrency(fireLossCost)}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFB91C1C),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Fire Maliyeti",
                                    fontSize = 10.sp,
                                    color = Slate500
                                )
                            }
                        }
                    }
                }
            }

            // 3. YAKLAŞAN SKT DAĞILIMI ÇUBUK GRAFİĞİ (ANLIK DURUM)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Yaklaşan SKT Dağılımı",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Slate900
                                )
                                Text(
                                    "Kalan gün vadelerine göre ürün çeşidi sayısı (Anlık)",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        val maxCount = remember(sktDistribution) {
                            maxOf(1, sktDistribution.maxOfOrNull { it.count } ?: 1)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            sktDistribution.forEach { item ->
                                val heightFraction = if (item.count > 0) {
                                    maxOf(0.12f, item.count.toFloat() / maxCount.toFloat())
                                } else {
                                    0.04f
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    Text(
                                        text = "${item.count}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.count > 0) item.color else Slate400
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(26.dp)
                                            .height(80.dp * heightFraction)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(
                                                if (item.count > 0) item.color else Slate200
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = item.shortLabel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Slate700,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. EN ÇOK SATANLAR
            item {
                val topSoldName = topSoldProducts.firstOrNull()?.name ?: "Veri yok"
                val topFiredName = topFiredProducts.firstOrNull()?.name ?: "Veri yok"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("En Çok Satan Ürünler", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate900)
                        Spacer(Modifier.height(10.dp))

                        // 💡 Akıllı Mağaza Özeti Kutusu
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF0FDFA),
                            border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    append("💡 ")
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TurquoiseDark)) {
                                        append("Akıllı Mağaza Özeti: ")
                                    }
                                    append("Bu dönemin en hızlı eriyen ürünü: ")
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Slate900)) {
                                        append(topSoldName)
                                    }
                                    if (topFiredProducts.isNotEmpty()) {
                                        append(". Dikkat: En çok fireyi ")
                                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = ExpiredRedDark)) {
                                            append(topFiredName)
                                        }
                                        append(" verdi, sipariş miktarını gözden geçirin.")
                                    } else {
                                        append(". Satış ivmesi dengeli seyrediyor.")
                                    }
                                },
                                fontSize = 11.5.sp,
                                color = Slate700,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        if (topSoldProducts.isEmpty()) {
                            Text("Henüz satış kaydı yok.", fontSize = 12.sp, color = Slate400)
                        } else {
                            topSoldProducts.forEachIndexed { index, item ->
                                val initialLetter = item.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "Ü"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            val matchedProduct = findMatchingProduct(item.barcode, item.name)
                                            if (matchedProduct != null) {
                                                onProductClick(matchedProduct)
                                            } else {
                                                Toast.makeText(context, "Ürün detayı bulunamadı", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 36x36 dp Soft Avatar Rozeti (Turkuaz/Yeşil Zemin)
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFCCFBF1)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initialLetter,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF0F766E)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Text(
                                        "${index + 1}.",
                                        fontWeight = FontWeight.Bold,
                                        color = TurquoiseDark,
                                        fontSize = 13.5.sp,
                                        modifier = Modifier.width(22.dp)
                                    )
                                    Text(
                                        item.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Slate800,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(color = Color(0xFFF0FDF4), shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            "${item.quantity} Adet",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF16A34A),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. EN ÇOK FİRE VERİLENLER
            item {
                val topFiredName = topFiredProducts.firstOrNull()?.name ?: "Veri yok"
                val topSoldName = topSoldProducts.firstOrNull()?.name ?: "Veri yok"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("En Çok Fire Verilenler", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate900)
                        Spacer(Modifier.height(10.dp))

                        // 💡 Akıllı Mağaza Özeti Kutusu
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFECACA))
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    append("💡 ")
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = ExpiredRedDark)) {
                                        append("Akıllı Mağaza Özeti: ")
                                    }
                                    if (topFiredProducts.isNotEmpty()) {
                                        append("Dikkat: En çok fireyi ")
                                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = ExpiredRedDark)) {
                                            append(topFiredName)
                                        }
                                        append(" verdi, sipariş miktarını gözden geçirin.")
                                        if (topSoldProducts.isNotEmpty()) {
                                            append(" En hızlı eriyen ürün: ")
                                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Slate900)) {
                                                append("$topSoldName.")
                                            }
                                        }
                                    } else {
                                        append("Bu dönemde fire kaydı bulunmuyor, raf rotasyonu sağlıklı.")
                                    }
                                },
                                fontSize = 11.5.sp,
                                color = Slate700,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        if (topFiredProducts.isEmpty()) {
                            Text("Henüz fire kaydı yok.", fontSize = 12.sp, color = Slate400)
                        } else {
                            topFiredProducts.forEachIndexed { index, item ->
                                val initialLetter = item.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "F"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            val matchedProduct = findMatchingProduct(item.barcode, item.name)
                                            if (matchedProduct != null) {
                                                onProductClick(matchedProduct)
                                            } else {
                                                Toast.makeText(context, "Ürün detayı bulunamadı", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 36x36 dp Soft Avatar Rozeti (Kırmızı/Turuncu Zemin)
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFFFE4E6)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initialLetter,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = Color(0xFFBE123C)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Text(
                                        "${index + 1}.",
                                        fontWeight = FontWeight.Bold,
                                        color = ExpiredRed,
                                        fontSize = 13.5.sp,
                                        modifier = Modifier.width(22.dp)
                                    )
                                    Text(
                                        item.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Slate800,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(color = Color(0xFFFEF2F2), shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            "${item.quantity} Adet",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ExpiredRed,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // BottomNavBar'ın son sıradaki ürünlerin üstünü kapatmaması için 84.dp boşluk
            item { Spacer(modifier = Modifier.height(84.dp)) }
        }
    }
}
