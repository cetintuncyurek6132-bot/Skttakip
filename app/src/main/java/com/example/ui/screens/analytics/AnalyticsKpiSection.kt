package com.example.ui.screens.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark

@Composable
fun AnalyticsKpiSection(
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
        // SON 30 GÜN SKT PERFORMANS & FİRE VİTRİNİ (CAROUSEL / PAGER)
        // -----------------------------------------------------------------
        SktPerformanceCarousel(
            items = sktPerformanceItems,
            onProductClick = onProductClick
        )
    }
}

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
