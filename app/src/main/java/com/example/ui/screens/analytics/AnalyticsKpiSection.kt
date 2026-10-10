package com.example.ui.screens.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyLira
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary

@Composable
fun AnalyticsKpiSection(
    financialMetrics: FinancialKpiMetrics,
    expiredProducts: List<Product>,
    criticalProducts: List<Product>,
    soonProducts: List<Product>,
    totalVarietyCount: Int,
    totalStockCount: Int,
    sktPerformanceItems: List<SktPerformanceItem> = emptyList(),
    onProductClick: (Product) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // =====================================================================
        // 1. ÜST KPI VE FİNANSAL METRİK KARTLARI (3'LÜ KOMPAKT PANEL)
        // =====================================================================
        FinancialKpiThreeColumnPanel(
            metrics = financialMetrics,
            modifier = Modifier.fillMaxWidth()
        )

        // =====================================================================
        // 2. ENVANTER DURUMU (4-GRID KOMPAKT YERLEŞİM)
        // =====================================================================
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

        // =====================================================================
        // 3. SON 30 GÜN SKT PERFORMANS & FİRE VİTRİNİ (CAROUSEL / PAGER)
        // =====================================================================
        SktPerformanceCarousel(
            items = sktPerformanceItems,
            onProductClick = onProductClick
        )
    }
}

/**
 * 3 Sütunlu Finansal Karar Destek Paneli:
 * KART 1: Kurtarma Oranı (%X) -> Dinamik renk eşikleri (<50 kırmızı, 50-75 turuncu, >=75 yeşil)
 * KART 2: Kurtarılan Ciro -> Satışı yapılan ürünlerin toplam parasal değeri (örn: "₺1.420")
 * KART 3: Fire / Risk Maliyeti -> Fireye çıkan veya süresi dolup satılamayan ürünlerin toplam TL zararı (örn: "₺340")
 */
@Composable
fun FinancialKpiThreeColumnPanel(
    metrics: FinancialKpiMetrics,
    modifier: Modifier = Modifier
) {
    val rate = metrics.recoveryRate
    val (rateColor, rateBg, rateBorder, rateLabel) = when {
        rate >= 75 -> Tuple4(
            Color(0xFF16A34A), // Zümrüt Yeşili
            Color(0xFFF0FDF4),
            Color(0xFFBBF7D0),
            "Yüksek"
        )
        rate >= 50 -> Tuple4(
            Color(0xFFEA580C), // Turuncu
            Color(0xFFFFF7ED),
            Color(0xFFFED7AA),
            "Orta"
        )
        else -> Tuple4(
            Color(0xFFDC2626), // Canlı Kırmızı
            Color(0xFFFEF2F2),
            Color(0xFFFECACA),
            "Kritik"
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("financial_kpi_three_column_panel"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // -------------------------------------------------------------
        // KART 1: KURTARMA ORANI
        // -------------------------------------------------------------
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            color = rateBg,
            border = BorderStroke(1.dp, rateBorder),
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 9.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kurtarma",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = rateColor,
                        maxLines = 1
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(rateColor)
                    )
                }

                Text(
                    text = "%$rate",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    color = rateColor,
                    maxLines = 1
                )

                Text(
                    text = "$rateLabel Başarı",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = rateColor.copy(alpha = 0.9f),
                    maxLines = 1
                )
            }
        }

        // -------------------------------------------------------------
        // KART 2: KURTARILAN CİRO
        // -------------------------------------------------------------
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFF0FDF4),
            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 9.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kurtarılan",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A),
                        maxLines = 1
                    )
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(13.dp)
                    )
                }

                Text(
                    text = metrics.formattedSavedRevenue,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF15803D),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Satılan Ciro",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF166534),
                    maxLines = 1
                )
            }
        }

        // -------------------------------------------------------------
        // KART 3: FİRE / RİSK MALİYETİ
        // -------------------------------------------------------------
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFFEF2F2),
            border = BorderStroke(1.dp, Color(0xFFFECACA)),
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 9.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fire Riski",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626),
                        maxLines = 1
                    )
                    Icon(
                        imageVector = Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(13.dp)
                    )
                }

                Text(
                    text = metrics.formattedFireRiskCost,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFB91C1C),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Zarar / Maliyet",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF991B1B),
                    maxLines = 1
                )
            }
        }
    }
}

private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun KpiMetricCard(
    title: String,
    value: String,
    subText: String,
    icon: ImageVector,
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
