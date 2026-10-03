package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.data.StockLog
import com.example.data.StockMovement
import com.example.data.getTodayMidnightMillis
import com.example.ui.screens.analytics.AnalyticsBarChartSection
import com.example.ui.screens.analytics.AnalyticsKpiSection
import com.example.ui.screens.analytics.AnalyticsTimeframe
import com.example.ui.screens.analytics.ChartDisplayMode
import com.example.ui.screens.analytics.DailyExpiryData
import com.example.ui.screens.analytics.PredictiveAnalyticsSection
import com.example.ui.screens.analytics.SktConversionSection
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate700
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen(
    products: List<Product>,
    stockMovements: List<StockMovement> = emptyList(),
    stockLogs: List<StockLog> = emptyList(),
    onProductClick: (Product) -> Unit = {},
    onBackClick: () -> Unit
) {
    val todayMidnight = remember { getTodayMidnightMillis() }
    var selectedTimeframe by remember { mutableStateOf(AnalyticsTimeframe.LAST_30_DAYS) }
    var chartMode by remember { mutableStateOf(ChartDisplayMode.ALL) }

    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.forLanguageTag("tr-TR")) }
    val fullDateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("tr-TR")) }

    // 1. General Inventory Stats
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

    // 2. Last 30 Days Expired Summary Metrics
    val thirtyDaysAgo = remember(todayMidnight) { todayMidnight - (30L * 86400000L) }
    val last30DaysExpiredProducts = remember(validProducts, todayMidnight, thirtyDaysAgo) {
        validProducts.filter { it.sktTarihi in thirtyDaysAgo until todayMidnight }
    }
    val last30DaysExpiredCount = last30DaysExpiredProducts.size
    val last30DaysExpiredStock = remember(last30DaysExpiredProducts) { last30DaysExpiredProducts.sumOf { it.stokAdedi } }

    // 3. Daily Bar Chart Data
    val barChartData = remember(validProducts, selectedTimeframe, todayMidnight, chartMode) {
        val daysSpan = selectedTimeframe.days
        val pastDays = when (chartMode) {
            ChartDisplayMode.EXPIRED_ONLY -> daysSpan
            ChartDisplayMode.UPCOMING -> 0
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // =====================================================================
        // 1. TOP APP BAR
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

                // Tarih Filtre Hapları (Son 7 Gün, Son 30 Gün, Son 60 Gün)
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
                            onClick = { selectedTimeframe = tf },
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
        // 2. SCROLLABLE CONTENT BODY (4 ALT BİLEŞEN)
        // =====================================================================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // A) 4'lü KPI Özeti & Son 30 Gün Durumu
            AnalyticsKpiSection(
                expiredProducts = expiredProducts,
                criticalProducts = criticalProducts,
                soonProducts = soonProducts,
                totalVarietyCount = totalVarietyCount,
                totalStockCount = totalStockCount,
                last30DaysExpiredCount = last30DaysExpiredCount,
                last30DaysExpiredStock = last30DaysExpiredStock
            )

            // B) Çubuk Dağılım Grafiği & Günlük İnceleme
            AnalyticsBarChartSection(
                selectedTimeframe = selectedTimeframe,
                chartMode = chartMode,
                onChartModeChange = { chartMode = it },
                barChartData = barChartData,
                onProductClick = onProductClick
            )

            // C) SKT Giriş & Akıbet Dağılımı (Satılan vs Fire vs Rafta)
            SktConversionSection(
                stockLogs = stockLogs,
                validProducts = validProducts
            )

            // D) Ürün Bazlı Satış & Fire Öngörüsü (Riskli Ürün Öngörüleri)
            PredictiveAnalyticsSection(
                validProducts = validProducts,
                stockLogs = stockLogs,
                onProductClick = onProductClick
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
