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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.data.StockLog
import com.example.data.getTodayMidnightMillis
import com.example.ui.theme.*
import java.util.Locale

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
    val totalStockQty = remember(products) {
        products.sumOf { it.stokAdedi }
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

            // 1. ÖZET KARTLARI
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Satılan
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.ShoppingCart, null, tint = Color(0xFF16A34A), modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Satılan Ürün", fontSize = 12.sp, color = Color(0xFF166534))
                            Text("$totalSoldQty Adet", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF15803D))
                        }
                    }
                    // Raftaki
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.Analytics, null, tint = Color(0xFF2563EB), modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Raftaki Stok", fontSize = 12.sp, color = Color(0xFF1E40AF))
                            Text("$totalStockQty Adet", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF1D4ED8))
                        }
                    }
                }
            }

            // 2. SATIŞ GRAFİĞİ (Yer tutucu bir görsel/tasarım)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Satış Grafiği", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate900)
                        Text("Son $selectedDays günlük satış trendi", fontSize = 11.sp, color = Slate500)
                        Spacer(Modifier.height(16.dp))
                        // Buraya ileride gerçek bir çizgi grafik eklenebilir. Şimdilik şık bir yer tutucu:
                        Row(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val heights = listOf(0.3f, 0.5f, 0.2f, 0.8f, 0.6f, 0.9f, 0.4f)
                            heights.forEach { h ->
                                Box(
                                    modifier = Modifier.width(24.dp).fillMaxHeight(h).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(TurquoisePrimary)
                                )
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
