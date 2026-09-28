package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.data.getTodayMidnightMillis
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.CriticalOrangeDark
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.ExpiredRedDark
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.SoonYellow
import com.example.ui.theme.SoonYellowDark
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoiseLight
import com.example.ui.theme.TurquoisePrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailyExpiryData(
    val dayTimestamp: Long,
    val dateLabel: String,
    val shortLabel: String,
    val expiredCount: Int,
    val criticalCount: Int,
    val totalStock: Int,
    val isPast: Boolean,
    val isToday: Boolean,
    val products: List<Product> = emptyList()
)

enum class AnalyticsTimeframe(val label: String, val days: Int) {
    LAST_7_DAYS("Son 7 Gün", 7),
    LAST_30_DAYS("Son 30 Gün", 30),
    ALL_TIME("Son 60 Gün", 60)
}

enum class ChartDisplayMode(val label: String) {
    ALL("Tüm Dağılım"),
    EXPIRED_ONLY("Süresi Dolanlar"),
    UPCOMING("Yaklaşan SKT'ler")
}

enum class ProductListFilter(val label: String) {
    ALL_CRITICAL("En Acil İlk 15"),
    EXPIRED("Süresi Dolanlar"),
    CRITICAL_0_3("0-3 Gün"),
    SOON_4_7("4-7 Gün"),
    HIGH_STOCK_RISK("Yüksek Adetli Risk")
}

@Composable
fun AnalyticsScreen(
    products: List<Product>,
    onProductClick: (Product) -> Unit = {},
    onBackClick: () -> Unit
) {
    val todayMidnight = remember { getTodayMidnightMillis() }
    var selectedTimeframe by remember { mutableStateOf(AnalyticsTimeframe.LAST_30_DAYS) }
    var chartMode by remember { mutableStateOf(ChartDisplayMode.ALL) }
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }
    var productListFilter by remember { mutableStateOf(ProductListFilter.ALL_CRITICAL) }

    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.forLanguageTag("tr-TR")) }
    val fullDateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("tr-TR")) }

    // 1. Calculate General Inventory Stats
    val validProducts = remember(products) { products.filter { it.sktTarihi > 0L && it.stokAdedi > 0 } }
    val totalVarietyCount = validProducts.size
    val totalStockCount = remember(validProducts) { validProducts.sumOf { it.stokAdedi } }

    val expiredProducts = remember(validProducts, todayMidnight) {
        validProducts.filter { it.getRemainingDays(todayMidnight) < 0L }
    }
    val criticalProducts = remember(validProducts, todayMidnight) {
        validProducts.filter { it.getRemainingDays(todayMidnight) in 0L..3L }
    }
    val soonProducts = remember(validProducts, todayMidnight) {
        validProducts.filter { it.getRemainingDays(todayMidnight) in 4L..7L }
    }
    val safeProducts = remember(validProducts, todayMidnight) {
        validProducts.filter { it.getRemainingDays(todayMidnight) >= 8L }
    }

    // High stock risk (stock >= 5 and daysLeft <= 3)
    val highStockRiskProducts = remember(validProducts, todayMidnight) {
        validProducts.filter { it.stokAdedi >= 5 && it.getRemainingDays(todayMidnight) in 0L..3L }
            .sortedByDescending { it.stokAdedi }
    }

    // Filtered product list based on selection
    val displayedProducts = remember(validProducts, productListFilter, todayMidnight) {
        when (productListFilter) {
            ProductListFilter.ALL_CRITICAL -> validProducts.sortedBy { it.sktTarihi }.take(15)
            ProductListFilter.EXPIRED -> expiredProducts.sortedBy { it.sktTarihi }
            ProductListFilter.CRITICAL_0_3 -> criticalProducts.sortedBy { it.sktTarihi }
            ProductListFilter.SOON_4_7 -> soonProducts.sortedBy { it.sktTarihi }
            ProductListFilter.HIGH_STOCK_RISK -> highStockRiskProducts
        }
    }

    // 2. Build Daily Bar Chart Data for the Last 30 Days (including past and upcoming)
    val barChartData = remember(validProducts, selectedTimeframe, todayMidnight, chartMode) {
        val daysSpan = selectedTimeframe.days
        val pastDays = when (chartMode) {
            ChartDisplayMode.EXPIRED_ONLY -> daysSpan // show all past days
            ChartDisplayMode.UPCOMING -> 0 // start from today
            ChartDisplayMode.ALL -> (daysSpan * 0.65).toInt()
        }

        val list = mutableListOf<DailyExpiryData>()
        val cal = Calendar.getInstance().apply {
            timeInMillis = todayMidnight
            add(Calendar.DAY_OF_YEAR, -pastDays)
        }

        val oneDayMillis = 86400000L

        for (i in 0 until daysSpan) {
            val dayStart = cal.timeInMillis
            val dayEnd = dayStart + oneDayMillis

            val dayProducts = validProducts.filter { p ->
                p.sktTarihi in dayStart until dayEnd
            }

            val isToday = dayStart == todayMidnight
            val isPast = dayStart < todayMidnight

            val expired = if (isPast) dayProducts.size else 0
            val critical = if (!isPast) dayProducts.size else 0
            val stock = dayProducts.sumOf { it.stokAdedi }

            list.add(
                DailyExpiryData(
                    dayTimestamp = dayStart,
                    dateLabel = fullDateFormat.format(Date(dayStart)),
                    shortLabel = dateFormat.format(Date(dayStart)),
                    expiredCount = expired,
                    criticalCount = critical,
                    totalStock = stock,
                    isPast = isPast,
                    isToday = isToday,
                    products = dayProducts
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    // 3. Last 30 Days Expired Summary Metrics
    val thirtyDaysAgo = remember(todayMidnight) { todayMidnight - (30L * 86400000L) }
    val last30DaysExpiredProducts = remember(validProducts, todayMidnight, thirtyDaysAgo) {
        validProducts.filter { it.sktTarihi in thirtyDaysAgo until todayMidnight }
    }
    val last30DaysExpiredCount = last30DaysExpiredProducts.size
    val last30DaysExpiredStock = remember(last30DaysExpiredProducts) { last30DaysExpiredProducts.sumOf { it.stokAdedi } }

    // 4. Category Breakdown
    val categoryBreakdown = remember(validProducts, todayMidnight) {
        validProducts.groupBy { it.kategori.ifBlank { "Diğer" } }
            .map { (cat, list) ->
                val atRisk = list.count { it.getRemainingDays(todayMidnight) <= 7L }
                val totalStock = list.sumOf { it.stokAdedi }
                val riskStock = list.filter { it.getRemainingDays(todayMidnight) <= 7L }.sumOf { it.stokAdedi }
                Triple(cat, list.size, Pair(atRisk, totalStock))
            }
            .sortedByDescending { it.third.first }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // =====================================================================
        // 1. TOP APP BAR (İki Katmanlı Düzen)
        // =====================================================================
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(vertical = 6.dp)
            ) {
                // 1. Satır: Geri dönüş ikonu + "Analiz ve İstatistikler" (Tek satır, ferah)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("analytics_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = TurquoiseDark,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Analiz ve İstatistikler",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TurquoiseDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 2. Satır: Tarih filtre butonları (Son 7 Gün, Son 30 Gün, Son 60 Gün) tam genişlikte
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnalyticsTimeframe.values().forEach { tf ->
                        val isSelected = selectedTimeframe == tf
                        Surface(
                            onClick = {
                                selectedTimeframe = tf
                                selectedBarIndex = null
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) TurquoisePrimary else Slate100,
                            border = if (isSelected) null else BorderStroke(1.dp, Slate200),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tf.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else Slate700,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 2. SCROLLABLE CONTENT BODY
        // =====================================================================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // -----------------------------------------------------------------
            // KPI METRIC CARDS (4-GRID - Kompakt Yerleşim)
            // -----------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // KPI 1: Süresi Dolan & Kritik (0-3 Gün)
                KpiMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Kritik & Süresi Dolan",
                    value = "${expiredProducts.size + criticalProducts.size}",
                    subText = "${expiredProducts.sumOf { it.stokAdedi } + criticalProducts.sumOf { it.stokAdedi }} Adet",
                    icon = Icons.Default.Warning,
                    color = ExpiredRed,
                    bgColor = Color(0xFFFEF2F2),
                    borderColor = Color(0xFFFECACA)
                )

                // KPI 2: Yaklaşan (4-7 Gün)
                KpiMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Yaklaşan (4-7 Gün)",
                    value = "${soonProducts.size}",
                    subText = "${soonProducts.sumOf { it.stokAdedi }} Adet",
                    icon = Icons.Default.Schedule,
                    color = CriticalOrange,
                    bgColor = Color(0xFFFFFBEB),
                    borderColor = Color(0xFFFDE68A)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // KPI 3: Toplam Çeşit
                KpiMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Toplam Ürün Çeşidi",
                    value = "$totalVarietyCount",
                    subText = "Kayıtlı Aktif SKT",
                    icon = Icons.Default.Inventory2,
                    color = TurquoiseDark,
                    bgColor = Color(0xFFF0FDFA),
                    borderColor = Color(0xFF99F6E4)
                )

                // KPI 4: Toplam SKT Adedi
                KpiMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Toplam SKT Adedi",
                    value = "$totalStockCount",
                    subText = "Aktif Partiler",
                    icon = Icons.Default.Category,
                    color = Slate700,
                    bgColor = Slate100,
                    borderColor = Slate200
                )
            }

            // -----------------------------------------------------------------
            // SON 30 GÜN SÜRESİ DOLANLAR ÖZET KARTI
            // -----------------------------------------------------------------
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (last30DaysExpiredCount > 0) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
                border = BorderStroke(1.dp, if (last30DaysExpiredCount > 0) Color(0xFFFECACA) else Color(0xFFBBF7D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (last30DaysExpiredCount > 0) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (last30DaysExpiredCount > 0) Icons.Default.TrendingDown else Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = if (last30DaysExpiredCount > 0) ExpiredRed else EmeraldSuccess,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Son 30 Günde Süresi Dolan Ürün Durumu",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900
                        )
                        Text(
                            text = if (last30DaysExpiredCount > 0)
                                "Son 30 günde $last30DaysExpiredCount çeşit ($last30DaysExpiredStock adet) ürünün süresi doldu."
                            else
                                "Son 30 gün içinde süresi dolmuş ürün kaydı bulunmuyor.",
                            fontSize = 12.sp,
                            color = if (last30DaysExpiredCount > 0) ExpiredRedDark else Slate600
                        )
                    }
                }
            }

            // -----------------------------------------------------------------
            // BAR CHART: "Son 30 Günde Süresi Dolan ve Yaklaşan Ürünler"
            // -----------------------------------------------------------------
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("analytics_bar_chart_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Chart Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Analytics,
                                    contentDescription = null,
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${selectedTimeframe.label} SKT Dağılım Grafiği",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Günlük süresi dolan ve yaklaşan ürün adedi",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Legend
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LegendItem(color = ExpiredRed, label = "Geçen")
                            LegendItem(color = CriticalOrange, label = "Kritik")
                            LegendItem(color = TurquoisePrimary, label = "Bugün")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Chart Mode Selector Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ChartDisplayMode.values().forEach { mode ->
                            val isSel = chartMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) MaterialTheme.colorScheme.surface else Color.Transparent)
                                    .clickable {
                                        chartMode = mode
                                        selectedBarIndex = null
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) TurquoiseDark else Slate600
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Selected Bar Tooltip & Product List Preview
                    selectedBarIndex?.let { idx ->
                        if (idx in barChartData.indices) {
                            val item = barChartData[idx]
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = TurquoiseDark,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${item.dateLabel} ${if (item.isToday) "(BUGÜN)" else ""}",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val count = item.expiredCount + item.criticalCount
                                        Text(
                                            text = "$count Çeşit • ${item.totalStock} Adet",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }

                                    // Display list of products on that day if available
                                    if (item.products.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 0.5.dp)
                                        Spacer(modifier = Modifier.height(6.dp))

                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            item.products.take(4).forEach { p ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .clickable { onProductClick(p) }
                                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "• ${p.urunAdi}",
                                                        color = Color.White,
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Text(
                                                        text = "${p.stokAdedi} Adet",
                                                        color = TurquoiseLight,
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            if (item.products.size > 4) {
                                                Text(
                                                    text = "+${item.products.size - 4} ürün daha...",
                                                    color = Color.White.copy(alpha = 0.8f),
                                                    fontSize = 10.5.sp,
                                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Interactive Canvas Bar Chart
                    InteractiveBarChart(
                        data = barChartData,
                        selectedIndex = selectedBarIndex,
                        onSelectIndex = { selectedBarIndex = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "💡 Çubuklara dokunarak o günün süresi geçen / dolacak ürün ve parti adetlerini görebilirsiniz.",
                        fontSize = 11.sp,
                        color = Slate500,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // -----------------------------------------------------------------
            // SKT RISK DAĞILIMI (SEGMENTED PROGRESS METER)
            // -----------------------------------------------------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = TurquoiseDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SKT Risk ve Adet Dağılımı",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "Toplam $totalVarietyCount Ürün",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate600
                        )
                    }

                    // Segmented Visual Bar
                    SegmentedRiskProgressBar(
                        expiredCount = expiredProducts.size,
                        criticalCount = criticalProducts.size,
                        soonCount = soonProducts.size,
                        safeCount = safeProducts.size,
                        totalCount = totalVarietyCount
                    )

                    // Breakdown Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RiskBadgeItem(
                            label = "Süresi Geçen",
                            count = expiredProducts.size,
                            color = ExpiredRed,
                            modifier = Modifier.weight(1f)
                        )
                        RiskBadgeItem(
                            label = "Kritik (0-3 G)",
                            count = criticalProducts.size,
                            color = CriticalOrange,
                            modifier = Modifier.weight(1f)
                        )
                        RiskBadgeItem(
                            label = "Yaklaşan (4-7 G)",
                            count = soonProducts.size,
                            color = SoonYellowDark,
                            modifier = Modifier.weight(1f)
                        )
                        RiskBadgeItem(
                            label = "Güvenli (8+ G)",
                            count = safeProducts.size,
                            color = EmeraldSuccess,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // -----------------------------------------------------------------
            // EN ÇOK SÜRESİ YAKLAŞAN ÜRÜNLER & ADET DURUMLARI (LIST & TABS)
            // -----------------------------------------------------------------
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("analytics_top_expiring_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PriorityHigh,
                                contentDescription = null,
                                tint = ExpiredRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "En Çok Süresi Yaklaşan Ürünler",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF2F2)
                        ) {
                            Text(
                                text = "${displayedProducts.size} Ürün",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpiredRedDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Product Filter Pills (Horizontal Scroll)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ProductListFilter.values().forEach { filter ->
                            val isSel = productListFilter == filter
                            Surface(
                                onClick = { productListFilter = filter },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) TurquoisePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = if (isSel) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = filter.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (displayedProducts.isEmpty()) {
                        Text(
                            text = "Bu kategoride kayıtlı ürün bulunmuyor.",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        displayedProducts.forEachIndexed { index, product ->
                            val days = product.getRemainingDays(todayMidnight)
                            val (badgeBg, badgeColor, badgeText) = when {
                                days < 0L -> Triple(Color(0xFFFEF2F2), ExpiredRed, "SÜRESİ GEÇTİ")
                                days == 0L -> Triple(Color(0xFFFEF2F2), ExpiredRedDark, "BUGÜN")
                                days in 1L..3L -> Triple(Color(0xFFFFF7ED), CriticalOrange, "$days GÜN")
                                else -> Triple(Color(0xFFFFFBEB), SoonYellowDark, "$days GÜN")
                            }

                            val isHighStockRisk = product.stokAdedi >= 5 && days in 0L..3L

                            Surface(
                                onClick = { onProductClick(product) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isHighStockRisk) Color(0xFFFFF7ED) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isHighStockRisk) Color(0xFFFDBA74) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Index
                                    Text(
                                        text = "${index + 1}.",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate500,
                                        modifier = Modifier.width(22.dp)
                                    )

                                    // Product Name & Info
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product.urunAdi,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "SKT: ${product.getFormattedSkt()} • Barkod: ${product.barkod}",
                                            fontSize = 11.sp,
                                            color = Slate600
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Stock Pill
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isHighStockRisk) Color(0xFFFED7AA) else Slate200
                                    ) {
                                        Text(
                                            text = "${product.stokAdedi} Adet",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isHighStockRisk) Color(0xFF9A3412) else Slate800,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Expiry Days Badge
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = badgeBg,
                                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = badgeText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = badgeColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // KATEGORİ BAZLI RİSK ANALİZİ
            // -----------------------------------------------------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                            Text(
                                text = "Kategori Bazlı Risk ve Adet",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "${categoryBreakdown.size} Kategori",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate600
                        )
                    }

                    categoryBreakdown.take(8).forEach { (catName, varietyCount, riskInfo) ->
                        val (atRiskCount, totalStock) = riskInfo
                        val riskRatio = if (varietyCount > 0) atRiskCount.toFloat() / varietyCount.toFloat() else 0f

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = catName,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$varietyCount Çeşit • $totalStock Adet • $atRiskCount Riskli",
                                    fontSize = 11.5.sp,
                                    color = if (atRiskCount > 0) ExpiredRed else Slate600,
                                    fontWeight = if (atRiskCount > 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }

                            // Progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Slate200)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(riskRatio.coerceIn(0.04f, 1f))
                                        .fillMaxHeight()
                                        .background(
                                            if (riskRatio > 0.3f) ExpiredRed else if (riskRatio > 0f) CriticalOrange else EmeraldSuccess
                                        )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun KpiMetricCard(
    title: String,
    value: String,
    subText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    bgColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subText,
                fontSize = 10.5.sp,
                color = Slate600
            )
        }
    }
}

@Composable
private fun InteractiveBarChart(
    data: List<DailyExpiryData>,
    selectedIndex: Int?,
    onSelectIndex: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val maxCount = remember(data) {
        val maxVal = data.maxOfOrNull { it.expiredCount + it.criticalCount } ?: 1
        if (maxVal < 4) 4 else maxVal
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        val barWidth = size.width / data.size.toFloat()
                        val tappedIndex = (offset.x / barWidth).toInt().coerceIn(0, data.size - 1)
                        if (selectedIndex == tappedIndex) {
                            onSelectIndex(null)
                        } else {
                            onSelectIndex(tappedIndex)
                        }
                    }
                }
        ) {
            val canvasW = size.width
            val canvasH = size.height
            val bottomPadding = 36f
            val topPadding = 16f
            val chartH = canvasH - bottomPadding - topPadding

            val barCount = data.size
            val barSlotWidth = canvasW / barCount.toFloat()
            val barWidth = (barSlotWidth * 0.65f).coerceIn(4f, 24f)

            // 1. Draw horizontal grid lines
            val gridLines = 4
            for (g in 0..gridLines) {
                val gridY = topPadding + (chartH * (1f - (g / gridLines.toFloat())))
                drawLine(
                    color = Color(0xFFE2E8F0),
                    start = Offset(0f, gridY),
                    end = Offset(canvasW, gridY),
                    strokeWidth = 1f
                )
            }

            // 2. Draw Bars
            data.forEachIndexed { index, item ->
                val totalForDay = item.expiredCount + item.criticalCount
                val heightRatio = if (maxCount > 0) (totalForDay.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f) else 0f
                val barH = if (totalForDay > 0) (chartH * heightRatio).coerceAtLeast(8f) else 3f

                val barX = (index * barSlotWidth) + ((barSlotWidth - barWidth) / 2f)
                val barY = topPadding + chartH - barH

                val isSelected = selectedIndex == index

                val barColor = when {
                    item.isToday -> TurquoisePrimary
                    item.isPast && totalForDay > 0 -> ExpiredRed
                    !item.isPast && totalForDay > 0 -> CriticalOrange
                    else -> Color(0xFFCBD5E1)
                }

                // Draw Bar
                drawRoundRect(
                    color = if (isSelected) barColor else barColor.copy(alpha = if (totalForDay > 0) 0.85f else 0.4f),
                    topLeft = Offset(barX, barY),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Selection Ring
                if (isSelected) {
                    drawRoundRect(
                        color = TurquoiseDark,
                        topLeft = Offset(barX - 2f, barY - 2f),
                        size = Size(barWidth + 4f, barH + 4f),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(width = 2f)
                    )
                }

                // Draw Today Marker Indicator
                if (item.isToday) {
                    drawCircle(
                        color = TurquoisePrimary,
                        radius = 3.5f,
                        center = Offset(barX + (barWidth / 2f), topPadding + chartH + 10f)
                    )
                }
            }
        }

        // X-Axis Labels Row (Bottom)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (data.isNotEmpty()) {
                Text(
                    text = data.first().shortLabel,
                    fontSize = 10.sp,
                    color = Slate500
                )
                if (data.size > 2) {
                    val midIndex = data.size / 2
                    Text(
                        text = data[midIndex].shortLabel,
                        fontSize = 10.sp,
                        color = Slate500
                    )
                }
                Text(
                    text = data.last().shortLabel,
                    fontSize = 10.sp,
                    color = Slate500
                )
            }
        }
    }
}

@Composable
private fun SegmentedRiskProgressBar(
    expiredCount: Int,
    criticalCount: Int,
    soonCount: Int,
    safeCount: Int,
    totalCount: Int
) {
    if (totalCount == 0) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Slate200)
        )
        return
    }

    val expWeight = expiredCount.toFloat() / totalCount
    val critWeight = criticalCount.toFloat() / totalCount
    val soonWeight = soonCount.toFloat() / totalCount
    val safeWeight = safeCount.toFloat() / totalCount

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp)),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (expiredCount > 0) {
            Box(
                modifier = Modifier
                    .weight(expWeight.coerceAtLeast(0.05f))
                    .fillMaxHeight()
                    .background(ExpiredRed)
            )
        }
        if (criticalCount > 0) {
            Box(
                modifier = Modifier
                    .weight(critWeight.coerceAtLeast(0.05f))
                    .fillMaxHeight()
                    .background(CriticalOrange)
            )
        }
        if (soonCount > 0) {
            Box(
                modifier = Modifier
                    .weight(soonWeight.coerceAtLeast(0.05f))
                    .fillMaxHeight()
                    .background(SoonYellowDark)
            )
        }
        if (safeCount > 0) {
            Box(
                modifier = Modifier
                    .weight(safeWeight.coerceAtLeast(0.05f))
                    .fillMaxHeight()
                    .background(EmeraldSuccess)
            )
        }
    }
}

@Composable
private fun RiskBadgeItem(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$count",
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
        Text(
            text = label,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = Slate700,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Slate600
        )
    }
}
