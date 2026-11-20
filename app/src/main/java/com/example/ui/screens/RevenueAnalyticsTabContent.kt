package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InventoryAuditEntity
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import com.example.ui.viewmodel.PERIODIC_AUDIT_CATEGORIES
import com.example.ui.viewmodel.normalizeInventoryCategoryKey
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

private val GidaLineColor = Color(0xFF16A34A)       // Yeşil
private val DolapLineColor = Color(0xFF2563EB)      // Mavi
private val GidaDisiLineColor = Color(0xFFF97316)   // Turuncu

private data class RevenueDateBucket(
    val dayMillis: Long,
    val shortLabel: String,
    val fullDateLabel: String,
    val gidaRevenue: Double?,
    val dolapRevenue: Double?,
    val gidaDisiRevenue: Double?
)

private data class RevenueChartDataset(
    val buckets: List<RevenueDateBucket>,
    val totalGida: Double,
    val totalDolap: Double,
    val totalGidaDisi: Double,
    val gidaRecordCount: Int,
    val dolapRecordCount: Int,
    val gidaDisiRecordCount: Int,
    val maxAxisValue: Double
)

private fun normalizeToDayStartMillis(millis: Long): Long {
    val cal = Calendar.getInstance(Locale.forLanguageTag("tr-TR")).apply {
        timeInMillis = if (millis > 0L) millis else System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private fun calculateNiceMaxAxis(rawMax: Double): Double {
    if (rawMax <= 0.0 || rawMax.isNaN() || rawMax.isInfinite()) return 1000.0
    val padded = rawMax * 1.12
    val magnitude = 10.0.pow(kotlin.math.floor(log10(padded)))
    val normalized = padded / magnitude
    val niceStep = when {
        normalized <= 1.0 -> 1.0
        normalized <= 1.5 -> 1.5
        normalized <= 2.0 -> 2.0
        normalized <= 2.5 -> 2.5
        normalized <= 3.0 -> 3.0
        normalized <= 4.0 -> 4.0
        normalized <= 5.0 -> 5.0
        normalized <= 6.0 -> 6.0
        normalized <= 8.0 -> 8.0
        else -> 10.0
    }
    return ceil(niceStep * magnitude)
}

private fun formatRevenueFullTl(amount: Double): String {
    val safe = if (amount.isNaN() || amount.isInfinite() || amount < 0.0) 0.0 else amount
    val nf = NumberFormat.getIntegerInstance(Locale.forLanguageTag("tr-TR"))
    return "₺${nf.format(safe.roundToInt())}"
}

private fun formatRevenueAxisTl(amount: Double): String {
    val safe = if (amount.isNaN() || amount.isInfinite() || amount < 0.0) 0.0 else amount
    val trLocale = Locale.forLanguageTag("tr-TR")
    return when {
        safe >= 1_000_000.0 -> {
            val millions = safe / 1_000_000.0
            if (abs(millions - millions.roundToInt()) < 0.05) {
                "₺${millions.roundToInt()}M"
            } else {
                String.format(trLocale, "₺%.1fM", millions)
            }
        }
        safe >= 1_000.0 -> {
            val thousands = safe / 1_000.0
            if (abs(thousands - thousands.roundToInt()) < 0.05) {
                "₺${thousands.roundToInt()}B"
            } else {
                String.format(trLocale, "₺%.1fB", thousands)
            }
        }
        else -> "₺${safe.roundToInt()}"
    }
}

@Composable
fun RevenueAnalyticsTabContent(
    revenueAudits: List<InventoryAuditEntity>,
    modifier: Modifier = Modifier
) {
    val trLocale = remember { Locale.forLanguageTag("tr-TR") }
    val shortDateFormat = remember { SimpleDateFormat("dd MMM", trLocale) }
    val fullDateFormat = remember { SimpleDateFormat("dd MMM yyyy", trLocale) }

    // Yalnızca GIDA, DOLAP ve GIDA DIŞI kayıtları (savunmacı filtre + en eskiden en yeniye sıralı)
    val dataset = remember(revenueAudits) {
        val filtered = revenueAudits
            .filter { normalizeInventoryCategoryKey(it.category) in PERIODIC_AUDIT_CATEGORIES }
            .sortedWith(
                compareBy<InventoryAuditEntity> { it.auditDateMillis }
                    .thenBy { it.createdAt }
                    .thenBy { it.id }
            )

        var totalGida = 0.0
        var totalDolap = 0.0
        var totalGidaDisi = 0.0
        var gidaCount = 0
        var dolapCount = 0
        var gidaDisiCount = 0

        val dayToCategoryMap = linkedMapOf<Long, MutableMap<String, Double>>()

        filtered.forEach { audit ->
            val key = normalizeInventoryCategoryKey(audit.category)
            val safeRev = audit.revenue.coerceAtLeast(0.0)
            when (key) {
                "GIDA" -> {
                    totalGida += safeRev
                    gidaCount++
                }
                "DOLAP" -> {
                    totalDolap += safeRev
                    dolapCount++
                }
                "GIDA DIŞI" -> {
                    totalGidaDisi += safeRev
                    gidaDisiCount++
                }
            }
            val dayStart = normalizeToDayStartMillis(audit.auditDateMillis)
            val catMap = dayToCategoryMap.getOrPut(dayStart) { mutableMapOf() }
            catMap[key] = (catMap[key] ?: 0.0) + safeRev
        }

        val sortedDays = dayToCategoryMap.keys.sorted()
        var maxPoint = 0.0
        val buckets = sortedDays.map { dayMillis ->
            val catMap = dayToCategoryMap[dayMillis].orEmpty()
            val gVal = catMap["GIDA"]
            val dVal = catMap["DOLAP"]
            val gdVal = catMap["GIDA DIŞI"]
            listOfNotNull(gVal, dVal, gdVal).forEach { v ->
                if (v > maxPoint) maxPoint = v
            }
            val dateObj = Date(dayMillis)
            RevenueDateBucket(
                dayMillis = dayMillis,
                shortLabel = shortDateFormat.format(dateObj),
                fullDateLabel = fullDateFormat.format(dateObj),
                gidaRevenue = gVal,
                dolapRevenue = dVal,
                gidaDisiRevenue = gdVal
            )
        }

        RevenueChartDataset(
            buckets = buckets,
            totalGida = totalGida,
            totalDolap = totalDolap,
            totalGidaDisi = totalGidaDisi,
            gidaRecordCount = gidaCount,
            dolapRecordCount = dolapCount,
            gidaDisiRecordCount = gidaDisiCount,
            maxAxisValue = calculateNiceMaxAxis(maxPoint)
        )
    }

    var selectedBucketIndex by remember(dataset.buckets.size) {
        mutableStateOf<Int?>(null)
    }

    val grandTotalRevenue = dataset.totalGida + dataset.totalDolap + dataset.totalGidaDisi

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_revenue_tab_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. ÜST KISIM: 3 PASTEL RENKLİ KÜÇÜK KPI KARTI (GIDA, DOLAP, GIDA DIŞI)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RevenueCategoryKpiCard(
                    title = "Gıda",
                    totalRevenue = dataset.totalGida,
                    recordCount = dataset.gidaRecordCount,
                    icon = Icons.Default.Storefront,
                    accentColor = GidaLineColor,
                    titleColor = Color(0xFF166534),
                    valueColor = Color(0xFF15803D),
                    gradientBottomColor = Color(0xFFDCFCE7),
                    borderColor = Color(0xFFBBF7D0),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("kpi_card_gida")
                )
                RevenueCategoryKpiCard(
                    title = "Dolap",
                    totalRevenue = dataset.totalDolap,
                    recordCount = dataset.dolapRecordCount,
                    icon = Icons.Default.Kitchen,
                    accentColor = DolapLineColor,
                    titleColor = Color(0xFF1E40AF),
                    valueColor = Color(0xFF1D4ED8),
                    gradientBottomColor = Color(0xFFDBEAFE),
                    borderColor = Color(0xFFBFDBFE),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("kpi_card_dolap")
                )
                RevenueCategoryKpiCard(
                    title = "Gıda Dışı",
                    totalRevenue = dataset.totalGidaDisi,
                    recordCount = dataset.gidaDisiRecordCount,
                    icon = Icons.Default.Inventory2,
                    accentColor = GidaDisiLineColor,
                    titleColor = Color(0xFF9A3412),
                    valueColor = Color(0xFFC2410C),
                    gradientBottomColor = Color(0xFFFFEDD5),
                    borderColor = Color(0xFFFED7AA),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("kpi_card_gida_disi")
                )
            }
        }

        // 2. TAM GENİŞLİKTE, YUVARLAK KÖŞELİ (16.dp), BEYAZ ARKA PLANLI ÇOKLU ÇİZGİ GRAFİK KARTI
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("revenue_line_chart_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Slate200),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Grafik Başlığı + Toplam Ciro Rozeti
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Kategori Bazlı Ciro Grafiği",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Parçalı envanter ciro seyri (₺)",
                                fontSize = 11.5.sp,
                                color = Slate500
                            )
                        }

                        if (grandTotalRevenue > 0.0) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF0FDFA),
                                border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = formatRevenueFullTl(grandTotalRevenue),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TurquoiseDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Renk Lejantı (Yeşil kare = Gıda, Mavi kare = Dolap, Turuncu kare = Gıda Dışı)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF8FAFC))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RevenueLegendItem(color = GidaLineColor, label = "Gıda")
                        RevenueLegendItem(color = DolapLineColor, label = "Dolap")
                        RevenueLegendItem(color = GidaDisiLineColor, label = "Gıda Dışı")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (dataset.buckets.isEmpty()) {
                        // Boş durum görünümü
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFF0FDFA)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShowChart,
                                    contentDescription = "Ciro verisi yok",
                                    tint = TurquoisePrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Henüz ciro kaydı bulunmuyor",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate800,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Envanter Takip ekranından Gıda, Dolap veya Gıda Dışı ciro verisi girdiğinizde grafik otomatik olarak güncellenir.",
                                fontSize = 11.5.sp,
                                color = Slate500,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    } else {
                        // Çoklu Çizgi Grafik (Saf Compose Canvas)
                        MultiLineRevenueCanvasChart(
                            buckets = dataset.buckets,
                            maxAxisValue = dataset.maxAxisValue,
                            selectedIndex = selectedBucketIndex,
                            onSelectIndex = { idx ->
                                selectedBucketIndex = if (selectedBucketIndex == idx) null else idx
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .testTag("revenue_multi_line_canvas")
                        )

                        // Seçili tarih detayı (Grafik üzerine dokunulursa)
                        val activeBucket = selectedBucketIndex?.let { dataset.buckets.getOrNull(it) }
                        if (activeBucket != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Slate200)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = activeBucket.fullDateLabel,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate800
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        SelectedDateValuePill(
                                            label = "Gıda",
                                            value = activeBucket.gidaRevenue,
                                            color = GidaLineColor
                                        )
                                        SelectedDateValuePill(
                                            label = "Dolap",
                                            value = activeBucket.dolapRevenue,
                                            color = DolapLineColor
                                        )
                                        SelectedDateValuePill(
                                            label = "Gıda Dışı",
                                            value = activeBucket.gidaDisiRevenue,
                                            color = GidaDisiLineColor
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tarih detaylarını görmek için grafikteki noktalara dokunabilirsiniz",
                                fontSize = 10.5.sp,
                                color = Slate400,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // 3. DÖNEMSEL KAYIT ÖZETİ (En yeni en üstte, kompakt ve temiz)
        if (dataset.buckets.isNotEmpty()) {
            item {
                val recentBuckets = remember(dataset.buckets) {
                    dataset.buckets.asReversed().take(8)
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Tarih Bazlı Ciro Özeti",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Son ${recentBuckets.size} envanter tarihi",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        recentBuckets.forEachIndexed { idx, bucket ->
                            if (idx > 0) {
                                HorizontalDivider(
                                    color = Slate200.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            Column(modifier = Modifier.fillMaxWidth()) {
                                val dayTotal = (bucket.gidaRevenue ?: 0.0) +
                                    (bucket.dolapRevenue ?: 0.0) +
                                    (bucket.gidaDisiRevenue ?: 0.0)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = bucket.fullDateLabel,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate800
                                    )
                                    Text(
                                        text = formatRevenueFullTl(dayTotal),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TurquoiseDark
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    bucket.gidaRevenue?.let { rev ->
                                        CompactCategoryTag("Gıda", rev, GidaLineColor)
                                    }
                                    bucket.dolapRevenue?.let { rev ->
                                        CompactCategoryTag("Dolap", rev, DolapLineColor)
                                    }
                                    bucket.gidaDisiRevenue?.let { rev ->
                                        CompactCategoryTag("G. Dışı", rev, GidaDisiLineColor)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RevenueCategoryKpiCard(
    title: String,
    totalRevenue: Double,
    recordCount: Int,
    icon: ImageVector,
    accentColor: Color,
    titleColor: Color,
    valueColor: Color,
    gradientBottomColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    val formattedValue = formatRevenueFullTl(totalRevenue)
    val valueFontSize = when {
        formattedValue.length <= 8 -> 15.sp
        formattedValue.length <= 11 -> 13.sp
        else -> 11.5.sp
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.White, gradientBottomColor)
                    )
                )
                .padding(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.13f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp)
            )

            Column(horizontalAlignment = Alignment.Start) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formattedValue,
                    fontSize = valueFontSize,
                    fontWeight = FontWeight.Black,
                    color = valueColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (recordCount > 0) "$recordCount kayıt" else "Kayıt yok",
                    fontSize = 10.sp,
                    color = titleColor.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun RevenueLegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Slate700
        )
    }
}

@Composable
private fun SelectedDateValuePill(
    label: String,
    value: Double?,
    color: Color
) {
    Column(horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = Slate500,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = value?.let { formatRevenueFullTl(it) } ?: "—",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (value != null) Slate900 else Slate400
        )
    }
}

@Composable
private fun CompactCategoryTag(
    label: String,
    amount: Double,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label: ${formatRevenueAxisTl(amount)}",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            color = Slate700,
            maxLines = 1
        )
    }
}

@Composable
private fun MultiLineRevenueCanvasChart(
    buckets: List<RevenueDateBucket>,
    maxAxisValue: Double,
    selectedIndex: Int?,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val fontFamily = MaterialTheme.typography.bodySmall.fontFamily
    val yAxisTextStyle = remember(fontFamily) {
        TextStyle(
            color = Slate500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = fontFamily
        )
    }
    val xAxisTextStyle = remember(fontFamily) {
        TextStyle(
            color = Slate500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = fontFamily
        )
    }
    val selectedXAxisTextStyle = remember(fontFamily) {
        TextStyle(
            color = Slate900,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = fontFamily
        )
    }

    val yStepCount = 4

    Canvas(
        modifier = modifier.pointerInput(buckets) {
            detectTapGestures { tapOffset ->
                if (buckets.isEmpty()) return@detectTapGestures
                val leftPaddingPx = 54.dp.toPx()
                val rightPaddingPx = 14.dp.toPx()
                val plotWidth = (size.width - leftPaddingPx - rightPaddingPx).coerceAtLeast(1f)
                if (buckets.size == 1) {
                    onSelectIndex(0)
                } else {
                    val clampedX = (tapOffset.x - leftPaddingPx).coerceIn(0f, plotWidth)
                    val fraction = clampedX / plotWidth
                    val nearestIndex = (fraction * (buckets.size - 1)).roundToInt()
                        .coerceIn(0, buckets.lastIndex)
                    onSelectIndex(nearestIndex)
                }
            }
        }
    ) {
        val leftPadding = 54.dp.toPx()
        val rightPadding = 14.dp.toPx()
        val topPadding = 12.dp.toPx()
        val bottomPadding = 28.dp.toPx()

        val plotWidth = (size.width - leftPadding - rightPadding).coerceAtLeast(1f)
        val plotHeight = (size.height - topPadding - bottomPadding).coerceAtLeast(1f)
        val plotBottom = topPadding + plotHeight

        val dashedGridEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)

        // 1. Yatay Izgara Çizgileri ve Y Ekseni (₺) Değerleri
        for (step in 0..yStepCount) {
            val fraction = step.toFloat() / yStepCount.toFloat()
            val y = plotBottom - (fraction * plotHeight)
            val axisValue = maxAxisValue * fraction

            drawLine(
                color = if (step == 0) Slate200 else Slate200.copy(alpha = 0.75f),
                start = Offset(leftPadding, y),
                end = Offset(leftPadding + plotWidth, y),
                strokeWidth = if (step == 0) 1.2.dp.toPx() else 1.dp.toPx(),
                pathEffect = if (step == 0) null else dashedGridEffect
            )

            val label = formatRevenueAxisTl(axisValue)
            val measured = textMeasurer.measure(text = label, style = yAxisTextStyle)
            val labelX = (leftPadding - measured.size.width - 8.dp.toPx()).coerceAtLeast(0f)
            val labelY = (y - measured.size.height / 2f).coerceIn(0f, size.height - measured.size.height)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(labelX, labelY)
            )
        }

        // Yardımcı koordinat dönüştürücüler
        fun xForIndex(index: Int): Float {
            return if (buckets.size <= 1) {
                leftPadding + plotWidth / 2f
            } else {
                leftPadding + (index.toFloat() / (buckets.size - 1).toFloat()) * plotWidth
            }
        }

        fun yForRevenue(revenue: Double): Float {
            val normalized = (revenue / maxAxisValue).coerceIn(0.0, 1.0).toFloat()
            return plotBottom - (normalized * plotHeight)
        }

        // Seçili tarih dikey rehber çizgisi
        if (selectedIndex != null && selectedIndex in buckets.indices) {
            val selX = xForIndex(selectedIndex)
            drawLine(
                color = TurquoisePrimary.copy(alpha = 0.45f),
                start = Offset(selX, topPadding),
                end = Offset(selX, plotBottom),
                strokeWidth = 1.4.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
        }

        // 2. X Ekseni Tarih Etiketleri (Çakışmayı önlemek için en fazla 5 etiket)
        val labelIndices = buildSet {
            if (buckets.size <= 5) {
                for (i in buckets.indices) add(i)
            } else {
                val maxLabels = 5
                for (slot in 0 until maxLabels) {
                    val idx = ((slot.toFloat() / (maxLabels - 1).toFloat()) * (buckets.size - 1)).roundToInt()
                    add(idx.coerceIn(0, buckets.lastIndex))
                }
            }
            if (selectedIndex != null && selectedIndex in buckets.indices) {
                add(selectedIndex)
            }
        }

        labelIndices.forEach { idx ->
            val x = xForIndex(idx)
            val isSelected = idx == selectedIndex
            val measured = textMeasurer.measure(
                text = buckets[idx].shortLabel,
                style = if (isSelected) selectedXAxisTextStyle else xAxisTextStyle
            )
            val textX = (x - measured.size.width / 2f)
                .coerceIn(leftPadding - 6.dp.toPx(), size.width - measured.size.width)
            val textY = plotBottom + 8.dp.toPx()
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(textX, textY)
            )
        }

        // 3. Çoklu Seri Çizimi (Gıda = Yeşil, Dolap = Mavi, Gıda Dışı = Turuncu)
        fun drawCategorySeries(
            color: Color,
            selector: (RevenueDateBucket) -> Double?
        ) {
            val points = mutableListOf<Pair<Int, Offset>>()
            buckets.forEachIndexed { idx, bucket ->
                val value = selector(bucket)
                if (value != null) {
                    points.add(idx to Offset(xForIndex(idx), yForRevenue(value)))
                }
            }
            if (points.isEmpty()) return

            if (points.size >= 2) {
                val linePath = Path().apply {
                    moveTo(points.first().second.x, points.first().second.y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].second.x, points[i].second.y)
                    }
                }
                drawPath(
                    path = linePath,
                    color = color,
                    style = Stroke(
                        width = 2.6.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // Çizgi birleşim noktalarına küçük yuvarlak noktalar (dot)
            val outerDotRadius = 4.8.dp.toPx()
            val innerDotRadius = 3.2.dp.toPx()
            points.forEach { (idx, center) ->
                val isSelected = idx == selectedIndex
                val haloRadius = if (isSelected) 6.5.dp.toPx() else outerDotRadius
                val coreRadius = if (isSelected) 4.4.dp.toPx() else innerDotRadius
                drawCircle(
                    color = Color.White,
                    radius = haloRadius,
                    center = center
                )
                drawCircle(
                    color = color,
                    radius = coreRadius,
                    center = center
                )
            }
        }

        drawCategorySeries(GidaLineColor) { it.gidaRevenue }
        drawCategorySeries(DolapLineColor) { it.dolapRevenue }
        drawCategorySeries(GidaDisiLineColor) { it.gidaDisiRevenue }
    }
}
