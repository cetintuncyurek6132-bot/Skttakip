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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.ui.components.AutoSizeMetricText
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
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

private val GidaLineColor = Color(0xFF16A34A)       // Yeşil
private val DolapLineColor = Color(0xFF2563EB)      // Mavi
private val GidaDisiLineColor = Color(0xFFF97316)   // Turuncu
private val PositiveDiffColor = Color(0xFF15803D)   // Yeşil (Pozitif Gelişim)
private val NegativeDiffColor = Color(0xFFDC2626)   // Kırmızı (0 veya Negatif)

private enum class RevenueChartMode {
    TOTAL_REVENUE,
    DAILY_SALES
}

private data class CategoryRevenueKpi(
    val latestRevenue: Double,
    val dailyDiff: Double?, // Son Ciro - Önceki Ciro (Önceki kayıt yoksa null)
    val recordCount: Int
)

private data class RevenueTimelineSlot(
    val timestampMillis: Long,
    val shortLabel: String,
    val fullDateLabel: String,
    val gidaRevenue: Double?,
    val dolapRevenue: Double?,
    val gidaDisiRevenue: Double?
)

private data class RevenueChartDataset(
    val totalSlots: List<RevenueTimelineSlot>,
    val dailySlots: List<RevenueTimelineSlot>,
    val gidaKpi: CategoryRevenueKpi,
    val dolapKpi: CategoryRevenueKpi,
    val gidaDisiKpi: CategoryRevenueKpi,
    val totalMaxAxisValue: Double,
    val dailyMaxAxisValue: Double
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

private fun resolveEntryTimestamp(
    audit: InventoryAuditEntity,
    useAuditDateDirectly: Boolean
): Long {
    return if (useAuditDateDirectly) {
        when {
            audit.auditDateMillis > 0L -> audit.auditDateMillis
            audit.updatedAt > 0L -> audit.updatedAt
            else -> audit.createdAt
        }
    } else {
        when {
            audit.updatedAt > 0L -> audit.updatedAt
            audit.createdAt > 0L -> audit.createdAt
            else -> audit.auditDateMillis
        }
    }
}

private fun computeCategoryKpi(
    chronologicalAudits: List<InventoryAuditEntity>
): CategoryRevenueKpi {
    if (chronologicalAudits.isEmpty()) {
        return CategoryRevenueKpi(
            latestRevenue = 0.0,
            dailyDiff = null,
            recordCount = 0
        )
    }
    // En yeniden en eskiye bakıldığında ilk kayıt en güncel toplam cirodur (sumOf kullanılmaz).
    val latest = chronologicalAudits.last()
    val latestRev = latest.revenue.coerceAtLeast(0.0)
    val previous = if (chronologicalAudits.size >= 2) {
        chronologicalAudits[chronologicalAudits.lastIndex - 1]
    } else {
        null
    }
    val diff = previous?.let { prev ->
        latestRev - prev.revenue.coerceAtLeast(0.0)
    }
    return CategoryRevenueKpi(
        latestRevenue = latestRev,
        dailyDiff = diff,
        recordCount = chronologicalAudits.size
    )
}

private fun calculateNiceMaxAxis(rawMax: Double): Double {
    if (rawMax <= 0.0 || rawMax.isNaN() || rawMax.isInfinite()) return 1000.0
    val padded = rawMax * 1.15
    val magnitude = 10.0.pow(floor(log10(padded)))
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

private fun formatSignedDiffTl(diff: Double): String {
    val safe = if (diff.isNaN() || diff.isInfinite()) 0.0 else diff
    val nf = NumberFormat.getIntegerInstance(Locale.forLanguageTag("tr-TR"))
    val rounded = safe.roundToInt()
    return when {
        rounded > 0 -> "+₺${nf.format(rounded)}"
        rounded < 0 -> "-₺${nf.format(abs(rounded))}"
        else -> "₺0"
    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevenueAnalyticsTabContent(
    revenueAudits: List<InventoryAuditEntity>,
    modifier: Modifier = Modifier
) {
    val trLocale = remember { Locale.forLanguageTag("tr-TR") }
    val shortDateFormat = remember { SimpleDateFormat("dd MMM", trLocale) }
    val fullDateFormat = remember { SimpleDateFormat("dd MMM yyyy", trLocale) }

    // SADECE GIDA, DOLAP ve GIDA DIŞI kayıtları (Sürekli Envanter kategorileri kesinlikle hariç)
    val dataset = remember(revenueAudits) {
        val strictPeriodic = revenueAudits.filter {
            normalizeInventoryCategoryKey(it.category) in PERIODIC_AUDIT_CATEGORIES
        }

        // auditDateMillis birden fazla farklı güne sahipse (örn: 16 Kasım, 17 Kasım) doğrudan auditDateMillis kullanılır;
        // Parçalı Envanter haftalık akışında tüm kayıtlar tek dönem başlangıç tarihine sabitlenmişse updatedAt kullanılır.
        val distinctAuditDays = strictPeriodic
            .map { normalizeToDayStartMillis(it.auditDateMillis) }
            .distinct()
            .size
        val distinctUpdateDays = strictPeriodic
            .map { normalizeToDayStartMillis(if (it.updatedAt > 0L) it.updatedAt else it.createdAt) }
            .distinct()
            .size
        val useAuditDateDirectly = distinctAuditDays > 1 && distinctAuditDays >= distinctUpdateDays

        val sortedAll = strictPeriodic.sortedWith(
            compareBy<InventoryAuditEntity> { resolveEntryTimestamp(it, useAuditDateDirectly) }
                .thenBy { it.auditDateMillis }
                .thenBy { it.updatedAt }
                .thenBy { it.createdAt }
                .thenBy { it.id }
        )

        val gidaAudits = sortedAll.filter { normalizeInventoryCategoryKey(it.category) == "GIDA" }
        val dolapAudits = sortedAll.filter { normalizeInventoryCategoryKey(it.category) == "DOLAP" }
        val gidaDisiAudits = sortedAll.filter { normalizeInventoryCategoryKey(it.category) == "GIDA DIŞI" }

        // 1 & 2) KPI Kartları: sumOf KULLANMADAN en son girilen toplam ciro ve (Son - Önceki) günlük satış farkı
        val gidaKpi = computeCategoryKpi(gidaAudits)
        val dolapKpi = computeCategoryKpi(dolapAudits)
        val gidaDisiKpi = computeCategoryKpi(gidaDisiAudits)

        // 3) Grafik Slotları:
        //    - Toplam Ciro: Her kaydın kendi kümülatif toplam cirosu (toplama yapmadan)
        //    - Günlük Satış: Her kategori için ayrı ayrı (Mevcut Ciro - Önceki Ciro), ilk kayıt 0 ve negatifler 0'a sabitlenir
        data class SlotKey(val dayMillis: Long, val seqIndex: Int)

        val totalSlotMap = linkedMapOf<SlotKey, Triple<Long, MutableMap<String, Double>, Long>>()
        val dailySlotMap = linkedMapOf<SlotKey, Triple<Long, MutableMap<String, Double>, Long>>()

        fun addSeriesToSlots(categoryKey: String, items: List<InventoryAuditEntity>) {
            val perDayCounter = mutableMapOf<Long, Int>()
            items.forEachIndexed { idx, audit ->
                val ts = resolveEntryTimestamp(audit, useAuditDateDirectly)
                val dayStart = normalizeToDayStartMillis(ts)
                val seq = perDayCounter[dayStart] ?: 0
                perDayCounter[dayStart] = seq + 1

                val key = SlotKey(dayStart, seq)
                val currentRev = audit.revenue.coerceAtLeast(0.0)
                val dailyDelta = if (idx == 0) {
                    0.0
                } else {
                    val prevRev = items[idx - 1].revenue.coerceAtLeast(0.0)
                    (currentRev - prevRev).coerceAtLeast(0.0)
                }

                val totalEntry = totalSlotMap.getOrPut(key) {
                    Triple(dayStart, mutableMapOf(), ts)
                }
                totalEntry.second[categoryKey] = currentRev

                val dailyEntry = dailySlotMap.getOrPut(key) {
                    Triple(dayStart, mutableMapOf(), ts)
                }
                dailyEntry.second[categoryKey] = dailyDelta
            }
        }

        addSeriesToSlots("GIDA", gidaAudits)
        addSeriesToSlots("DOLAP", dolapAudits)
        addSeriesToSlots("GIDA DIŞI", gidaDisiAudits)

        val orderedKeys = totalSlotMap.keys.sortedWith(
            compareBy<SlotKey> { it.dayMillis }.thenBy { it.seqIndex }
        )

        var maxTotalRevenue = 0.0
        var maxDailySales = 0.0

        val totalSlots = orderedKeys.map { key ->
            val (_, catValues, ts) = totalSlotMap.getValue(key)
            val gVal = catValues["GIDA"]
            val dVal = catValues["DOLAP"]
            val gdVal = catValues["GIDA DIŞI"]
            listOfNotNull(gVal, dVal, gdVal).forEach { v ->
                if (v > maxTotalRevenue) maxTotalRevenue = v
            }
            val dateObj = Date(ts)
            val baseShort = shortDateFormat.format(dateObj)
            val baseFull = fullDateFormat.format(dateObj)
            val hasMultiOnSameDay = orderedKeys.count { it.dayMillis == key.dayMillis } > 1
            RevenueTimelineSlot(
                timestampMillis = ts,
                shortLabel = if (hasMultiOnSameDay) "$baseShort #${key.seqIndex + 1}" else baseShort,
                fullDateLabel = if (hasMultiOnSameDay) "$baseFull (${key.seqIndex + 1}. kayıt)" else baseFull,
                gidaRevenue = gVal,
                dolapRevenue = dVal,
                gidaDisiRevenue = gdVal
            )
        }

        val dailySlots = orderedKeys.map { key ->
            val (_, catValues, ts) = dailySlotMap.getValue(key)
            val gVal = catValues["GIDA"]
            val dVal = catValues["DOLAP"]
            val gdVal = catValues["GIDA DIŞI"]
            listOfNotNull(gVal, dVal, gdVal).forEach { v ->
                if (v > maxDailySales) maxDailySales = v
            }
            val dateObj = Date(ts)
            val baseShort = shortDateFormat.format(dateObj)
            val baseFull = fullDateFormat.format(dateObj)
            val hasMultiOnSameDay = orderedKeys.count { it.dayMillis == key.dayMillis } > 1
            RevenueTimelineSlot(
                timestampMillis = ts,
                shortLabel = if (hasMultiOnSameDay) "$baseShort #${key.seqIndex + 1}" else baseShort,
                fullDateLabel = if (hasMultiOnSameDay) "$baseFull (${key.seqIndex + 1}. kayıt)" else baseFull,
                gidaRevenue = gVal,
                dolapRevenue = dVal,
                gidaDisiRevenue = gdVal
            )
        }

        RevenueChartDataset(
            totalSlots = totalSlots,
            dailySlots = dailySlots,
            gidaKpi = gidaKpi,
            dolapKpi = dolapKpi,
            gidaDisiKpi = gidaDisiKpi,
            totalMaxAxisValue = calculateNiceMaxAxis(maxTotalRevenue),
            dailyMaxAxisValue = calculateNiceMaxAxis(maxDailySales)
        )
    }

    var chartMode by rememberSaveable { mutableStateOf(RevenueChartMode.TOTAL_REVENUE) }
    val activeSlots = if (chartMode == RevenueChartMode.TOTAL_REVENUE) {
        dataset.totalSlots
    } else {
        dataset.dailySlots
    }
    val activeMaxAxisValue = if (chartMode == RevenueChartMode.TOTAL_REVENUE) {
        dataset.totalMaxAxisValue
    } else {
        dataset.dailyMaxAxisValue
    }

    var selectedSlotIndex by remember(activeSlots.size, chartMode) {
        mutableStateOf<Int?>(null)
    }

    // 3 ayrı kategorinin en güncel toplam cirolarının mağaza toplamı
    val latestCombinedStoreRevenue = dataset.gidaKpi.latestRevenue +
        dataset.dolapKpi.latestRevenue +
        dataset.gidaDisiKpi.latestRevenue

    val latestCombinedDailySales = (dataset.gidaKpi.dailyDiff?.coerceAtLeast(0.0) ?: 0.0) +
        (dataset.dolapKpi.dailyDiff?.coerceAtLeast(0.0) ?: 0.0) +
        (dataset.gidaDisiKpi.dailyDiff?.coerceAtLeast(0.0) ?: 0.0)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_revenue_tab_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. ÜST KISIM: 3 PASTEL RENKLİ KÜÇÜK KPI KARTI (SON CİRO + GÜNLÜK SATIŞ FARKI)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RevenueCategoryKpiCard(
                    title = "Gıda",
                    kpi = dataset.gidaKpi,
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
                    kpi = dataset.dolapKpi,
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
                    kpi = dataset.gidaDisiKpi,
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
                    // Dinamik Grafik Başlığı + Güncel Özet Rozeti
                    val chartTitle = if (chartMode == RevenueChartMode.TOTAL_REVENUE) {
                        "Kategori Bazlı Toplam Ciro"
                    } else {
                        "Kategori Bazlı Günlük Satış"
                    }
                    val chartSubtitle = if (chartMode == RevenueChartMode.TOTAL_REVENUE) {
                        "Parçalı envanter kümülatif ciro seyri (₺)"
                    } else {
                        "Kayıtlar arası günlük satış artışı (₺)"
                    }
                    val badgeAmount = if (chartMode == RevenueChartMode.TOTAL_REVENUE) {
                        latestCombinedStoreRevenue
                    } else {
                        latestCombinedDailySales
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = chartTitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = chartSubtitle,
                                fontSize = 11.5.sp,
                                color = Slate500
                            )
                        }

                        if (badgeAmount > 0.0) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF0FDFA),
                                border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = if (chartMode == RevenueChartMode.DAILY_SALES) {
                                        "+${formatRevenueFullTl(badgeAmount)}"
                                    } else {
                                        formatRevenueFullTl(badgeAmount)
                                    },
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TurquoiseDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Grafik Modu Seçici: Toplam Ciro / Günlük Satış (SingleChoiceSegmentedButtonRow)
                    val modeOptions = listOf(
                        RevenueChartMode.TOTAL_REVENUE to "Toplam Ciro",
                        RevenueChartMode.DAILY_SALES to "Günlük Satış"
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("revenue_chart_mode_segmented_row")
                    ) {
                        modeOptions.forEachIndexed { index, (mode, label) ->
                            val isSelected = chartMode == mode
                            SegmentedButton(
                                selected = isSelected,
                                onClick = { chartMode = mode },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = modeOptions.size,
                                    baseShape = RoundedCornerShape(12.dp)
                                ),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = TurquoisePrimary,
                                    activeContentColor = Color.White,
                                    activeBorderColor = TurquoisePrimary,
                                    inactiveContainerColor = Color(0xFFF8FAFC),
                                    inactiveContentColor = Slate700,
                                    inactiveBorderColor = Slate200
                                ),
                                icon = {},
                                modifier = Modifier.testTag(
                                    if (mode == RevenueChartMode.TOTAL_REVENUE) {
                                        "chart_mode_total_revenue"
                                    } else {
                                        "chart_mode_daily_sales"
                                    }
                                )
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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

                    if (activeSlots.isEmpty()) {
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
                        // Çoklu Çizgi + Area Grafik (Saf Compose Canvas - Path yapısı korunur)
                        MultiLineRevenueCanvasChart(
                            slots = activeSlots,
                            maxAxisValue = activeMaxAxisValue,
                            selectedIndex = selectedSlotIndex,
                            onSelectIndex = { idx ->
                                selectedSlotIndex = if (selectedSlotIndex == idx) null else idx
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(244.dp)
                                .testTag("revenue_multi_line_canvas")
                        )

                        // Seçili tarih detayı (Grafik üzerine dokunulursa)
                        val activeSlot = selectedSlotIndex?.let { activeSlots.getOrNull(it) }
                        if (activeSlot != null) {
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
                                        text = if (chartMode == RevenueChartMode.DAILY_SALES) {
                                            "${activeSlot.fullDateLabel} • Günlük Satış"
                                        } else {
                                            activeSlot.fullDateLabel
                                        },
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
                                            value = activeSlot.gidaRevenue,
                                            color = GidaLineColor
                                        )
                                        SelectedDateValuePill(
                                            label = "Dolap",
                                            value = activeSlot.dolapRevenue,
                                            color = DolapLineColor
                                        )
                                        SelectedDateValuePill(
                                            label = "Gıda Dışı",
                                            value = activeSlot.gidaDisiRevenue,
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

        // 3. DÖNEMSEL KAYIT GEÇMİŞİ (Seçili moda göre kümülatif toplam ciro veya günlük satış)
        if (activeSlots.isNotEmpty()) {
            item {
                val recentSlots = remember(activeSlots) {
                    activeSlots.asReversed().take(8)
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
                            text = if (chartMode == RevenueChartMode.TOTAL_REVENUE) {
                                "Tarih Bazlı Toplam Ciro Geçmişi"
                            } else {
                                "Tarih Bazlı Günlük Satış Geçmişi"
                            },
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Son ${recentSlots.size} parçalı envanter kaydı",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        recentSlots.forEachIndexed { idx, slot ->
                            if (idx > 0) {
                                HorizontalDivider(
                                    color = Slate200.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            Column(modifier = Modifier.fillMaxWidth()) {
                                val slotCombined = (slot.gidaRevenue ?: 0.0) +
                                    (slot.dolapRevenue ?: 0.0) +
                                    (slot.gidaDisiRevenue ?: 0.0)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = slot.fullDateLabel,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate800
                                    )
                                    Text(
                                        text = formatRevenueFullTl(slotCombined),
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
                                    slot.gidaRevenue?.let { rev ->
                                        CompactCategoryTag("Gıda", rev, GidaLineColor)
                                    }
                                    slot.dolapRevenue?.let { rev ->
                                        CompactCategoryTag("Dolap", rev, DolapLineColor)
                                    }
                                    slot.gidaDisiRevenue?.let { rev ->
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
    kpi: CategoryRevenueKpi,
    icon: ImageVector,
    accentColor: Color,
    titleColor: Color,
    valueColor: Color,
    gradientBottomColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    val formattedValue = formatRevenueFullTl(kpi.latestRevenue)
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
                AutoSizeMetricText(
                    text = formattedValue,
                    maxFontSize = valueFontSize,
                    minFontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    color = valueColor,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Günlük Satış / Son Gelişim Farkı (Son Ciro - Önceki Ciro)
                val dailyDiff = kpi.dailyDiff
                if (dailyDiff != null) {
                    val isPositive = dailyDiff > 0.0
                    val diffTextColor = if (isPositive) PositiveDiffColor else NegativeDiffColor
                    val diffBadgeBg = if (isPositive) {
                        Color(0xFF16A34A).copy(alpha = 0.12f)
                    } else {
                        Color(0xFFDC2626).copy(alpha = 0.12f)
                    }
                    val formattedDiff = formatSignedDiffTl(dailyDiff)

                    Text(
                        text = "Günlük Satış",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = titleColor.copy(alpha = 0.78f),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = diffBadgeBg
                    ) {
                        AutoSizeMetricText(
                            text = formattedDiff,
                            maxFontSize = if (formattedDiff.length <= 9) 10.sp else 9.sp,
                            minFontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = diffTextColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Text(
                        text = if (kpi.recordCount > 0) "İlk kayıt" else "Kayıt yok",
                        fontSize = 10.sp,
                        color = titleColor.copy(alpha = 0.75f),
                        maxLines = 1
                    )
                }
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
    slots: List<RevenueTimelineSlot>,
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
        modifier = modifier.pointerInput(slots) {
            detectTapGestures { tapOffset ->
                if (slots.isEmpty()) return@detectTapGestures
                val leftPaddingPx = 56.dp.toPx()
                val rightPaddingPx = 14.dp.toPx()
                val plotWidth = (size.width - leftPaddingPx - rightPaddingPx).coerceAtLeast(1f)
                if (slots.size == 1) {
                    onSelectIndex(0)
                } else {
                    val clampedX = (tapOffset.x - leftPaddingPx).coerceIn(0f, plotWidth)
                    val fraction = clampedX / plotWidth
                    val nearestIndex = (fraction * (slots.size - 1)).roundToInt()
                        .coerceIn(0, slots.lastIndex)
                    onSelectIndex(nearestIndex)
                }
            }
        }
    ) {
        val leftPadding = 56.dp.toPx()
        val rightPadding = 14.dp.toPx()
        val topPadding = 14.dp.toPx()
        val bottomPadding = 28.dp.toPx()

        val plotWidth = (size.width - leftPadding - rightPadding).coerceAtLeast(1f)
        val plotHeight = (size.height - topPadding - bottomPadding).coerceAtLeast(1f)
        val plotBottom = topPadding + plotHeight
        val safeMaxAxis = maxAxisValue.coerceAtLeast(1.0)

        val dashedGridEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)

        // 1. Yatay Izgara Çizgileri ve Dinamik Y Ekseni (₺) Ölçeklemesi
        for (step in 0..yStepCount) {
            val fraction = step.toFloat() / yStepCount.toFloat()
            val y = plotBottom - (fraction * plotHeight)
            val axisValue = safeMaxAxis * fraction

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

        // Koordinat hesaplayıcılar (çizgilerin ekran dışına taşmasını engelleyen güvenli sınırlar)
        fun xForIndex(index: Int): Float {
            return if (slots.size <= 1) {
                leftPadding + plotWidth / 2f
            } else {
                leftPadding + (index.toFloat() / (slots.size - 1).toFloat()) * plotWidth
            }
        }

        fun yForRevenue(revenue: Double): Float {
            val normalized = (revenue / safeMaxAxis).coerceIn(0.0, 1.0).toFloat()
            return (plotBottom - (normalized * plotHeight)).coerceIn(topPadding, plotBottom)
        }

        // Seçili tarih dikey kılavuz çizgisi
        if (selectedIndex != null && selectedIndex in slots.indices) {
            val selX = xForIndex(selectedIndex)
            drawLine(
                color = TurquoisePrimary.copy(alpha = 0.45f),
                start = Offset(selX, topPadding),
                end = Offset(selX, plotBottom),
                strokeWidth = 1.4.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
        }

        // 2. X Ekseni Tarih Etiketleri: Çakışmayı önlemek için yalnızca ilk, orta ve son tarih
        val labelIndices = buildSet {
            when {
                slots.size <= 3 -> {
                    for (i in slots.indices) add(i)
                }
                else -> {
                    add(0)
                    add(slots.size / 2)
                    add(slots.lastIndex)
                }
            }
        }

        labelIndices.forEach { idx ->
            val x = xForIndex(idx)
            val isSelected = idx == selectedIndex
            val measured = textMeasurer.measure(
                text = slots[idx].shortLabel,
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

        // 3. Her kategori için (x, y) noktalarını hazırla
        val seriesConfigs = listOf(
            GidaLineColor to slots.mapIndexedNotNull { idx, slot ->
                slot.gidaRevenue?.let { rev -> idx to Offset(xForIndex(idx), yForRevenue(rev)) }
            },
            DolapLineColor to slots.mapIndexedNotNull { idx, slot ->
                slot.dolapRevenue?.let { rev -> idx to Offset(xForIndex(idx), yForRevenue(rev)) }
            },
            GidaDisiLineColor to slots.mapIndexedNotNull { idx, slot ->
                slot.gidaDisiRevenue?.let { rev -> idx to Offset(xForIndex(idx), yForRevenue(rev)) }
            }
        )

        // 3a. Area Chart Dolgusu (hafif alpha = 0.1f saydamlık)
        seriesConfigs.forEach { (color, points) ->
            if (points.size >= 2) {
                val areaPath = Path().apply {
                    moveTo(points.first().second.x, plotBottom)
                    points.forEach { (_, pt) ->
                        lineTo(pt.x, pt.y)
                    }
                    lineTo(points.last().second.x, plotBottom)
                    close()
                }
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.12f),
                            color.copy(alpha = 0.01f)
                        ),
                        startY = topPadding,
                        endY = plotBottom
                    )
                )
            }
        }

        // 3b. Noktaları Birleştiren Çizgiler (Path + moveTo / lineTo + 3.dp Stroke)
        val lineStrokeWidth = 3.dp.toPx()
        seriesConfigs.forEach { (color, points) ->
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
                        width = lineStrokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        // 3c. Birleşim Noktalarına Yuvarlak Noktalar (Dot)
        val outerDotRadius = 5.dp.toPx()
        val innerDotRadius = 3.4.dp.toPx()
        seriesConfigs.forEach { (color, points) ->
            points.forEach { (idx, center) ->
                val isSelected = idx == selectedIndex
                val haloRadius = if (isSelected) 6.8.dp.toPx() else outerDotRadius
                val coreRadius = if (isSelected) 4.6.dp.toPx() else innerDotRadius
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
    }
}
