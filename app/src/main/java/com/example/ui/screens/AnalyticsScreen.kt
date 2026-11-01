package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.data.StockLog
import com.example.data.getTodayMidnightMillis
import com.example.ui.theme.*
import java.util.Locale

private data class SktBarItem(
    val title: String,
    val shortLabel: String,
    val count: Int,
    val color: Color
)

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
    
    // Zaman filtresi (7, 30, 60 gün)
    var selectedDays by remember { mutableIntStateOf(30) }
    val todayMidnight = remember { getTodayMidnightMillis() }
    val cutoffTime = remember(selectedDays) { todayMidnight - (selectedDays * 86400000L) }

    // 1. Filtrelenmiş Loglar
    val periodLogs = remember(stockLogs, cutoffTime) {
        stockLogs.filter { it.timestamp >= cutoffTime }
    }

    // 2. Özet Veriler
    val totalSoldQty = remember(periodLogs) {
        periodLogs.filter { it.actionType == "SATIS" }.sumOf { it.quantity }
    }
    val totalFiredQty = remember(periodLogs) {
        periodLogs.filter { it.actionType == "FIRE" }.sumOf { it.quantity }
    }
    val totalBatchCount = remember(products) {
        products.filter { !it.isDeleted && it.stokAdedi > 0 && it.sktTarihi > 0L }
            .distinctBy { 
                val key = if (it.barkod.isNotBlank() && !it.barkod.startsWith("NO_BARCODE_")) it.barkod else it.urunAdi
                "${key}_${it.sktTarihi}"
            }.size
    }
    // Raftaki stok kontrolü: Sadece stokAdedi > 0 ve silinmemiş ürünler
    val totalStockQty = remember(products) {
        products.filter { !it.isDeleted && it.stokAdedi > 0 }.sumOf { it.stokAdedi }
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
            .map { (key, logs) -> 
                val name = logs.firstOrNull()?.productName ?: "Bilinmeyen"
                val qty = logs.sumOf { it.quantity }
                name to qty
            }
            .sortedByDescending { it.second }
            .take(5)
    }

    // 4. En Çok Fire Verilenler (İlk 5)
    val topFiredProducts = remember(periodLogs) {
        periodLogs.filter { it.actionType == "FIRE" }
            .groupBy { it.barcode.ifBlank { it.productName } }
            .map { (key, logs) -> 
                val name = logs.firstOrNull()?.productName ?: "Bilinmeyen"
                val qty = logs.sumOf { it.quantity }
                name to qty
            }
            .sortedByDescending { it.second }
            .take(5)
    }

    Column(modifier = Modifier.fillMaxSize().background(Slate50)) {
        // ÜST BAR
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
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
                            modifier = Modifier.weight(1f).height(36.dp)
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
                    // Kutu 1 (Yeşil): Satılan
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDCFCE7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Satılan",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF166534),
                                maxLines = 1
                            )
                            Text(
                                "$totalSoldQty",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF15803D),
                                maxLines = 1
                            )
                            Text(
                                "Adet",
                                fontSize = 10.sp,
                                color = Color(0xFF166534)
                            )
                        }
                    }

                    // Kutu 2 (Kırmızı): Fire Edilen
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        border = BorderStroke(1.dp, Color(0xFFFECACA))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEE2E2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Fire Edilen",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF991B1B),
                                maxLines = 1
                            )
                            Text(
                                "$totalFiredQty",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFB91C1C),
                                maxLines = 1
                            )
                            Text(
                                "Adet",
                                fontSize = 10.sp,
                                color = Color(0xFF991B1B)
                            )
                        }
                    }

                    // Kutu 3 (Mavi): Toplam Parti
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDBEAFE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Toplam Parti",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E40AF),
                                maxLines = 1
                            )
                            Text(
                                "$totalBatchCount",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1D4ED8),
                                maxLines = 1
                            )
                            Text(
                                "Parti",
                                fontSize = 10.sp,
                                color = Color(0xFF1E40AF)
                            )
                        }
                    }
                }
            }

            // 2. YAKLAŞAN SKT DAĞILIMI ÇUBUK GRAFİĞİ (ANLIK DURUM)
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

            // 3. EN ÇOK SATANLAR
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("En Çok Satan Ürünler", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate900)
                        Spacer(Modifier.height(12.dp))
                        
                        if (topSoldProducts.isEmpty()) {
                            Text("Henüz satış kaydı yok.", fontSize = 12.sp, color = Slate400)
                        } else {
                            topSoldProducts.forEachIndexed { index, (name, qty) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${index + 1}.", fontWeight = FontWeight.Bold, color = TurquoiseDark, fontSize = 14.sp, modifier = Modifier.width(24.dp))
                                    Text(name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Slate800, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Surface(color = Color(0xFFF0FDF4), shape = RoundedCornerShape(6.dp)) {
                                        Text("$qty Adet", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. EN ÇOK FİRE VERİLENLER
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("En Çok Fire Verilenler", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate900)
                        Spacer(Modifier.height(12.dp))
                        
                        if (topFiredProducts.isEmpty()) {
                            Text("Henüz fire kaydı yok.", fontSize = 12.sp, color = Slate400)
                        } else {
                            topFiredProducts.forEachIndexed { index, (name, qty) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${index + 1}.", fontWeight = FontWeight.Bold, color = ExpiredRed, fontSize = 14.sp, modifier = Modifier.width(24.dp))
                                    Text(name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Slate800, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Surface(color = Color(0xFFFEF2F2), shape = RoundedCornerShape(6.dp)) {
                                        Text("$qty Adet", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ExpiredRed, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
