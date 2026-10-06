package com.example.ui.screens.analytics

import androidx.compose.runtime.Immutable
import com.example.data.Product

@Immutable
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

@Immutable
data class ProductPrediction(
    val product: Product,
    val barcode: String,
    val productName: String,
    val currentStock: Int,
    val pastSold: Int,
    val pastFire: Int,
    val pastFireRate: Double,
    val pastSaleRate: Double,
    val predictedSales: Int,
    val predictedFire: Int,
    val firePercent: Int
)

@Immutable
data class SktPerformanceItem(
    val product: Product? = null,
    val productName: String,
    val productCode: String = "",
    val barcode: String = "",
    val formattedSkt: String = "",
    val sktTimestamp: Long = 0L,
    val soldCount: Int = 0,
    val fireCount: Int = 0,
    val totalCount: Int = 0,
    val recoveryRate: Int = 0
)
