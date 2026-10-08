package com.example.ui.screens.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoiseLight
import com.example.ui.theme.TurquoisePrimary

@Composable
fun AnalyticsBarChartSection(
    selectedTimeframe: AnalyticsTimeframe,
    chartMode: ChartDisplayMode,
    onChartModeChange: (ChartDisplayMode) -> Unit,
    barChartData: List<DailyExpiryData>,
    onProductClick: (Product) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = modifier
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
            // Başlık ve Lejant Alanı
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                    // Sağ üstteki lejant alanı
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                            )
                            Text(
                                text = "Süresi Geçen",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF97316))
                            )
                            Text(
                                text = "Kritik / Yaklaşan",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Text(
                    text = "Günlük süresi dolan ve yaklaşan ürün adedi",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                                onChartModeChange(mode)
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

            // Interactive Modern Bar Chart
            InteractiveBarChart(
                data = barChartData,
                selectedIndex = selectedBarIndex,
                onSelectIndex = { selectedBarIndex = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
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
}

private fun shouldShowDateLabel(index: Int, totalSize: Int): Boolean {
    if (totalSize <= 7) {
        return index == 0 || index == 2 || index == 4 || index == totalSize - 1
    }
    val step = 7
    if (index % step == 0) {
        return (totalSize - 1 - index) >= 4 || index == 0
    }
    return index == totalSize - 1
}

@Composable
fun InteractiveBarChart(
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

    Column(modifier = modifier) {
        // Ana Grafik Alanı
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.Bottom
        ) {
            // Sol Taraf: Kılavuz Adetleri
            Column(
                modifier = Modifier
                    .width(24.dp)
                    .fillMaxHeight()
                    .padding(bottom = 26.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "$maxCount",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400
                )
                Text(
                    text = "${(maxCount + 1) / 2}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400
                )
                Text(
                    text = "0",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Sağ Taraf: Çubuklar, Kılavuz Çizgileri ve Hizalı Tarih Etiketleri
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Arka plan yatay kılavuz çizgileri
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 14.dp, bottom = 26.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    HorizontalDivider(color = Color(0xFFCBD5E1), thickness = 1.dp)
                }

                val scrollState = rememberScrollState()
                val isScrollable = data.size > 14

                LaunchedEffect(data) {
                    val todayIndex = data.indexOfFirst { it.isToday }
                    if (todayIndex > 0 && isScrollable) {
                        val approxScroll = (todayIndex * 18 * 2)
                        scrollState.scrollTo(approxScroll)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (isScrollable) Modifier.horizontalScroll(scrollState) else Modifier),
                    horizontalArrangement = if (isScrollable) Arrangement.spacedBy(6.dp) else Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val barWidth = if (data.size <= 7) 20.dp else if (data.size <= 14) 14.dp else 9.5.dp

                    data.forEachIndexed { index, item ->
                        val totalForDay = item.expiredCount + item.criticalCount
                        val heightFraction = if (maxCount > 0) {
                            (totalForDay.toFloat() / maxCount.toFloat()).coerceIn(0.04f, 1f)
                        } else 0.04f

                        val isSelected = selectedIndex == index
                        val showLabel = shouldShowDateLabel(index, data.size)

                        val gradientBrush = when {
                            item.isToday -> Brush.verticalGradient(
                                listOf(Color(0xFF14B8A6), Color(0xFF0F766E))
                            )
                            item.isPast && totalForDay > 0 -> Brush.verticalGradient(
                                listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                            )
                            !item.isPast && totalForDay > 0 -> Brush.verticalGradient(
                                listOf(Color(0xFFF97316), Color(0xFFEA580C))
                            )
                            else -> Brush.verticalGradient(
                                listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1))
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(if (isScrollable) barWidth + 6.dp else barWidth + 8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    onSelectIndex(if (isSelected) null else index)
                                }
                                .padding(horizontal = 1.dp)
                        ) {
                            // 1. Tepe Adet Metni
                            if (totalForDay > 0) {
                                Text(
                                    text = "$totalForDay",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSelected) TurquoiseDark else Slate700,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            } else {
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            // 2. Çubuk Alanı (Dikey Esnek Alan)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .width(barWidth),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(heightFraction)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(gradientBrush)
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(
                                                    width = 1.5.dp,
                                                    color = TurquoiseDark,
                                                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                                )
                                            } else Modifier
                                        )
                                )
                            }

                            // 3. Bugün Noktası
                            if (item.isToday) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(TurquoisePrimary)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            // 4. X Ekseni Referans Tick Çizgisi (Çubuğun Tam Merkezi)
                            Box(
                                modifier = Modifier
                                    .width(if (showLabel || isSelected) 1.5.dp else 1.dp)
                                    .height(if (showLabel) 4.dp else 2.dp)
                                    .background(
                                        if (isSelected) TurquoiseDark
                                        else if (showLabel) Slate500
                                        else Color(0xFFCBD5E1)
                                    )
                            )

                            // 5. X Ekseni Tarih Etiketi (Çubuğun Tam Merkezine Hizalı)
                            if (showLabel) {
                                Text(
                                    text = item.shortLabel,
                                    fontSize = 9.sp,
                                    fontWeight = if (item.isToday || isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) TurquoiseDark else if (item.isToday) TurquoisePrimary else Slate500,
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .wrapContentSize(unbounded = true, align = Alignment.Center)
                                        .padding(top = 2.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
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
