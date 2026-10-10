package com.example.ui.screens.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.Slate200
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                                fontSize = 10.5.sp,
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
                                text = "Yaklaşan",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Sol: Geçmiş, Sağ: Gelecek yönlendirme rehberi
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "← Geçmiş (Süresi Dolan)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ExpiredRed
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = TurquoiseDark.copy(alpha = 0.12f),
                        border = BorderStroke(0.6.dp, TurquoiseDark.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "BUGÜN (Referans)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TurquoiseDark,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                    Text(
                        text = "Gelecek (Yaklaşan) →",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CriticalOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chart Mode Selector Tabs (Tüm Dağılım / Süresi Dolanlar / Yaklaşan SKT'ler)
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

            // 1. Interactive Modern Bar Chart (ÜSTTE SABİT ÇİZİM)
            InteractiveBarChart(
                data = barChartData,
                selectedIndex = selectedBarIndex,
                onSelectIndex = { selectedBarIndex = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            // 2. Bilgi Kartı Yerleşimi: Grafiğin altına yumuşak animasyonla yerleşir
            AnimatedVisibility(
                visible = selectedBarIndex != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                selectedBarIndex?.let { idx ->
                    if (idx in barChartData.indices) {
                        val item = barChartData[idx]
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = TurquoiseDark,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = item.dateLabel,
                                            color = Color.White,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (item.isToday) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color.White.copy(alpha = 0.25f)
                                            ) {
                                                Text(
                                                    text = "BUGÜN",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val count = item.expiredCount + item.criticalCount
                                        Text(
                                            text = "$count Çeşit • ${item.totalStock} Adet",
                                            color = TurquoiseLight,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { selectedBarIndex = null },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Kapat",
                                                tint = Color.White.copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                if (item.products.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.25f), thickness = 0.5.dp)
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
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "💡 Çubuklara dokunarak o günün süresi geçen / dolacak ürün detaylarını aşağıdaki kartta inceleyebilirsiniz.",
                fontSize = 11.sp,
                color = Slate500,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * X ekseni tarih etiketleri: 7'şer günlük adımlarla düzenli ve çubuk merkezine tam hizalı.
 */
private fun shouldShowDateLabel(index: Int, totalSize: Int): Boolean {
    if (totalSize <= 7) {
        return index == 0 || index == 3 || index == totalSize - 1
    }
    if (totalSize in 28..32) {
        // 30 günlük görünüm: Başlangıç (0), +7 gün (7), +14 gün (14), +21 gün (21), Bitiş (totalSize - 1)
        return index == 0 || index == 7 || index == 14 || index == 21 || index == totalSize - 1
    }
    // 60 günlük ve diğer aralıklar: 7'şer günlük düzenli adımlar
    if (index % 7 == 0) {
        return (totalSize - 1 - index) >= 3 || index == 0
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

            // Sağ Taraf: Çubuklar, Kılavuz Çizgileri, Bugün Göstergesi ve Hizalı Tarih Etiketleri
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Arka plan yatay kılavuz çizgileri
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 18.dp, bottom = 26.dp),
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
                                listOf(Color(0xFF0D9488), Color(0xFF0F766E))
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
                                .width(if (isScrollable) barWidth + 8.dp else barWidth + 8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    onSelectIndex(if (isSelected) null else index)
                                }
                                .then(
                                    if (item.isToday) {
                                        Modifier.background(
                                            TurquoisePrimary.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    } else Modifier
                                )
                                .padding(horizontal = 1.dp)
                        ) {
                            // 1. "BUGÜN" Referans Rozeti veya Tepe Adet Metni
                            if (item.isToday) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = TurquoiseDark,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                ) {
                                    Text(
                                        text = "BUGÜN",
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                    )
                                }
                            } else if (totalForDay > 0) {
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
                                // Bugün için dikey referans çizgisi
                                if (item.isToday) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(1.5.dp)
                                            .background(TurquoisePrimary.copy(alpha = 0.4f))
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(heightFraction)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(gradientBrush)
                                        .then(
                                            if (item.isToday) {
                                                Modifier.border(
                                                    width = 1.5.dp,
                                                    color = TurquoiseDark,
                                                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                                )
                                            } else if (isSelected) {
                                                Modifier.border(
                                                    width = 1.5.dp,
                                                    color = Color(0xFF6366F1),
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
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(TurquoisePrimary)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            // 4. X Ekseni Referans Tick Çizgisi (Çubuğun Tam Merkezi)
                            Box(
                                modifier = Modifier
                                    .width(if (item.isToday || isSelected) 2.dp else if (showLabel) 1.5.dp else 1.dp)
                                    .height(if (item.isToday || showLabel) 4.dp else 2.dp)
                                    .background(
                                        if (item.isToday) TurquoiseDark
                                        else if (isSelected) Color(0xFF6366F1)
                                        else if (showLabel) Slate500
                                        else Color(0xFFCBD5E1)
                                    )
                            )

                            // 5. X Ekseni Tarih Etiketi (Çubuğun Tam Merkezine Hizalı)
                            if (showLabel || item.isToday) {
                                Text(
                                    text = if (item.isToday) "Bugün" else item.shortLabel,
                                    fontSize = 9.sp,
                                    fontWeight = if (item.isToday || isSelected) FontWeight.Black else FontWeight.SemiBold,
                                    color = if (item.isToday) TurquoiseDark else if (isSelected) Color(0xFF6366F1) else Slate500,
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
