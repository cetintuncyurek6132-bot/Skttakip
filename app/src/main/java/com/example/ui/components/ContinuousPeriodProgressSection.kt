package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InventoryAuditEntity
import com.example.ui.theme.ExpiredRedBorder
import com.example.ui.theme.ExpiredRedContainer
import com.example.ui.theme.ExpiredRedDark
import com.example.ui.theme.NormalGreenBorder
import com.example.ui.theme.NormalGreenContainer
import com.example.ui.theme.NormalGreenDark
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun ContinuousPeriodProgressSection(
    activePeriod: String,
    sortedHistoryNewestFirst: List<InventoryAuditEntity>,
    effectiveTargetRatio: Double,
    moneyFormatter: DecimalFormat,
    ratioFormatter: DecimalFormat
) {
    val trLocale = remember { Locale.forLanguageTag("tr-TR") }
    val dayMonthFormatter = remember(trLocale) {
        SimpleDateFormat("d MMMM", trLocale)
    }
    val shortDayMonthFormatter = remember(trLocale) {
        SimpleDateFormat("d MMM", trLocale)
    }

    // Aktif döneme ait kayıtları kronolojik sırada (en eski en üstte) hazırla
    val periodChronological = remember(sortedHistoryNewestFirst, activePeriod) {
        val cleanPeriod = activePeriod.trim()
        val matching = if (cleanPeriod.isNotBlank()) {
            sortedHistoryNewestFirst.filter {
                it.auditPeriod.trim().equals(cleanPeriod, ignoreCase = true)
            }
        } else {
            sortedHistoryNewestFirst
        }
        matching.sortedWith(
            compareBy<InventoryAuditEntity> { it.auditDateMillis }
                .thenBy { it.createdAt }
                .thenBy { it.updatedAt }
        )
    }

    var showAllHistoryModal by remember { mutableStateOf(false) }

    if (periodChronological.isEmpty()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("continuous_period_progress_empty"),
            shape = RoundedCornerShape(16.dp),
            color = Slate50,
            border = BorderStroke(1.dp, Slate200)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                    contentDescription = null,
                    tint = TurquoiseDark,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Henüz geçmiş kaydı bulunmuyor",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Aşağıdaki butondan veri girdiğinizde dönem içi tarih bazlı gelişim burada listelenir.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate700,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val firstRecord = periodChronological.first()
    val latestRecord = periodChronological.last()
    val firstRatio = if (firstRecord.revenue > 0.0) (firstRecord.fireCost / firstRecord.revenue) * 100.0 else 0.0
    val latestRatio = if (latestRecord.revenue > 0.0) (latestRecord.fireCost / latestRecord.revenue) * 100.0 else 0.0
    val hasTrendComparison = periodChronological.size >= 2
    val ratioDiff = latestRatio - firstRatio

    val isImproved = hasTrendComparison && ratioDiff < -0.0001
    val isWorsened = hasTrendComparison && ratioDiff > 0.0001

    val trendBg = when {
        isImproved -> NormalGreenContainer
        isWorsened -> ExpiredRedContainer
        else -> TurquoisePrimary.copy(alpha = 0.12f)
    }
    val trendBorder = when {
        isImproved -> NormalGreenBorder
        isWorsened -> ExpiredRedBorder
        else -> TurquoisePrimary.copy(alpha = 0.35f)
    }
    val trendTextColor = when {
        isImproved -> NormalGreenDark
        isWorsened -> ExpiredRedDark
        else -> TurquoiseDark
    }
    val trendBadgeText = when {
        !hasTrendComparison -> "İlk Kayıt (%${ratioFormatter.format(firstRatio)})"
        isImproved -> "İyileşme Var (↓ %${ratioFormatter.format(abs(ratioDiff))})"
        isWorsened -> "Kötüleşme Var (↑ %${ratioFormatter.format(abs(ratioDiff))})"
        else -> "Değişim Yok (%0)"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("continuous_period_progress_section"),
        shape = RoundedCornerShape(16.dp),
        color = Slate50,
        border = BorderStroke(1.dp, Slate200)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. TREND BAŞLIĞI VE ROZETİ: "Dönem Başından Beri Gelişim"
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = null,
                            tint = TurquoiseDark,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = "Dönem Başından Beri Gelişim",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (activePeriod.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = BorderStroke(0.9.dp, Slate200)
                        ) {
                            Text(
                                text = activePeriod,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("continuous_period_trend_badge"),
                    shape = RoundedCornerShape(12.dp),
                    color = trendBg,
                    border = BorderStroke(1.dp, trendBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = trendBadgeText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = trendTextColor
                        )
                        if (hasTrendComparison) {
                            Text(
                                text = "%${ratioFormatter.format(firstRatio)} → %${ratioFormatter.format(latestRatio)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = trendTextColor
                            )
                        }
                    }
                }
            }

            // 2. ÇİZGİ GRAFİK (Listede 2'den fazla kayıt varsa: > 2)
            if (periodChronological.size > 2) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("continuous_period_line_chart"),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    val chartRatios = remember(periodChronological) {
                        periodChronological.map { item ->
                            if (item.revenue > 0.0) (item.fireCost / item.revenue) * 100.0 else 0.0
                        }
                    }
                    val lineColor = when {
                        chartRatios.last() < chartRatios.first() - 0.0001 -> NormalGreenDark
                        chartRatios.last() > chartRatios.first() + 0.0001 -> ExpiredRedDark
                        else -> TurquoiseDark
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Dönem İçi Oran Trendi",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate800
                            )
                            Text(
                                text = "${periodChronological.size} Kayıt",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )
                        }

                        // Üst oran etiketleri (kalabalıklaşmayı önlemek için en fazla 5 nokta gösterilir; >5 ise baş/orta/son)
                        if (chartRatios.size <= 5) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                chartRatios.forEachIndexed { idx, ratioVal ->
                                    val isLast = idx == chartRatios.lastIndex
                                    Text(
                                        text = "%${ratioFormatter.format(ratioVal)}",
                                        fontSize = 11.sp,
                                        fontWeight = if (isLast) FontWeight.Black else FontWeight.Bold,
                                        color = if (isLast) lineColor else Slate800,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Baş: %${ratioFormatter.format(chartRatios.first())}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate800
                                )
                                Text(
                                    text = "Min: %${ratioFormatter.format(chartRatios.minOrNull() ?: 0.0)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NormalGreenDark
                                )
                                Text(
                                    text = "Son: %${ratioFormatter.format(chartRatios.last())}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = lineColor
                                )
                            }
                        }

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(96.dp)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            val w = size.width
                            val h = size.height
                            val maxVal = (chartRatios.maxOrNull() ?: 1.0)
                                .coerceAtLeast(effectiveTargetRatio)
                                .coerceAtLeast(0.5) * 1.15
                            val minVal = 0.0
                            val range = (maxVal - minVal).coerceAtLeast(0.1)

                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            for (i in 0..2) {
                                val yPos = h * (i / 2f)
                                drawLine(
                                    color = Color(0xFFE2E8F0),
                                    start = Offset(0f, yPos),
                                    end = Offset(w, yPos),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = dashEffect
                                )
                            }

                            val points = chartRatios.mapIndexed { index, r ->
                                val x = if (chartRatios.size == 1) {
                                    w / 2f
                                } else {
                                    (index.toFloat() / (chartRatios.size - 1).toFloat()) * w
                                }
                                val normalizedY = ((r - minVal) / range).toFloat().coerceIn(0f, 1f)
                                val y = h - (normalizedY * h)
                                Offset(x, y)
                            }

                            if (points.size >= 2) {
                                val fillPath = Path().apply {
                                    moveTo(points.first().x, h)
                                    points.forEach { pt -> lineTo(pt.x, pt.y) }
                                    lineTo(points.last().x, h)
                                    close()
                                }
                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            lineColor.copy(alpha = 0.24f),
                                            lineColor.copy(alpha = 0.02f)
                                        ),
                                        startY = 0f,
                                        endY = h
                                    )
                                )

                                val strokePath = Path().apply {
                                    moveTo(points.first().x, points.first().y)
                                    for (i in 1 until points.size) {
                                        lineTo(points[i].x, points[i].y)
                                    }
                                }
                                drawPath(
                                    path = strokePath,
                                    color = lineColor,
                                    style = Stroke(
                                        width = 3.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }

                            points.forEachIndexed { idx, pt ->
                                val isLast = idx == points.lastIndex
                                drawCircle(
                                    color = Color.White,
                                    radius = if (isLast) 6.5.dp.toPx() else 4.8.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = if (isLast) lineColor else TurquoiseDark,
                                    radius = if (isLast) 4.5.dp.toPx() else 3.2.dp.toPx(),
                                    center = pt
                                )
                            }
                        }

                        // Alt tarih etiketleri
                        if (periodChronological.size <= 5) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                periodChronological.forEachIndexed { idx, item ->
                                    val label = shortDayMonthFormatter.format(Date(item.auditDateMillis))
                                    Text(
                                        text = if (idx == periodChronological.lastIndex) "$label (Son)" else label,
                                        fontSize = 10.sp,
                                        fontWeight = if (idx == periodChronological.lastIndex) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = Slate700,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        } else {
                            val midItem = periodChronological[periodChronological.size / 2]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = shortDayMonthFormatter.format(Date(periodChronological.first().auditDateMillis)),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate700
                                )
                                Text(
                                    text = shortDayMonthFormatter.format(Date(midItem.auditDateMillis)),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate700
                                )
                                Text(
                                    text = "${shortDayMonthFormatter.format(Date(periodChronological.last().auditDateMillis))} (Son)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Slate900
                                )
                            }
                        }
                    }
                }
            }

            // 3. DÖNEM KAYIT LİSTESİ (Sabit max 280.dp iç kaydırmalı kutu + Son 5 kayıt)
            val recentEntriesNewestFirst = remember(periodChronological) {
                periodChronological.asReversed().take(5)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("continuous_period_chronological_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Geçmiş Kayıtlar (Son ${recentEntriesNewestFirst.size} kayıt)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate800
                    )
                    if (periodChronological.size > recentEntriesNewestFirst.size) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White,
                            border = BorderStroke(0.8.dp, Slate200)
                        ) {
                            Text(
                                text = "Toplam ${periodChronological.size}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (recentEntriesNewestFirst.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(0.8.dp, Slate200)
                    ) {
                        Text(
                            text = "Henüz geçmiş kaydı bulunmuyor",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate700,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp, horizontal = 12.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = recentEntriesNewestFirst,
                            key = { index, item -> "${item.id}_$index" }
                        ) { index, item ->
                            val chronologicalIndex = periodChronological.size - 1 - index
                            val prevItem = if (chronologicalIndex > 0) {
                                periodChronological[chronologicalIndex - 1]
                            } else null
                            ContinuousHistoryEntryRow(
                                item = item,
                                prevItem = prevItem,
                                isFirstChronological = chronologicalIndex == 0 && periodChronological.size > 1,
                                isLatest = chronologicalIndex == periodChronological.lastIndex,
                                rowIndex = index,
                                dayMonthFormatter = dayMonthFormatter,
                                moneyFormatter = moneyFormatter,
                                ratioFormatter = ratioFormatter
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showAllHistoryModal = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("continuous_view_all_history_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = TurquoiseDark
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tüm Geçmişi Gör (${periodChronological.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }

    if (showAllHistoryModal) {
        val allNewestFirst = remember(periodChronological) { periodChronological.asReversed() }
        Dialog(
            onDismissRequest = { showAllHistoryModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                color = Color.White
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = TurquoisePrimary.copy(alpha = 0.14f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = TurquoiseDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Tüm Geçmiş Kayıtlar",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Slate900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (activePeriod.isNotBlank()) {
                                        "$activePeriod • ${periodChronological.size} kayıt"
                                    } else {
                                        "${periodChronological.size} kayıt"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Slate700,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = { showAllHistoryModal = false },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = Slate700
                            )
                        }
                    }

                    HorizontalDivider(color = Slate200, thickness = 1.dp)

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Slate50),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = allNewestFirst,
                            key = { index, item -> "all_cont_${item.id}_$index" }
                        ) { index, item ->
                            val chronologicalIndex = periodChronological.size - 1 - index
                            val prevItem = if (chronologicalIndex > 0) {
                                periodChronological[chronologicalIndex - 1]
                            } else null
                            ContinuousHistoryEntryRow(
                                item = item,
                                prevItem = prevItem,
                                isFirstChronological = chronologicalIndex == 0 && periodChronological.size > 1,
                                isLatest = chronologicalIndex == periodChronological.lastIndex,
                                rowIndex = index,
                                dayMonthFormatter = dayMonthFormatter,
                                moneyFormatter = moneyFormatter,
                                ratioFormatter = ratioFormatter
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinuousHistoryEntryRow(
    item: InventoryAuditEntity,
    prevItem: InventoryAuditEntity?,
    isFirstChronological: Boolean,
    isLatest: Boolean,
    rowIndex: Int,
    dayMonthFormatter: SimpleDateFormat,
    moneyFormatter: DecimalFormat,
    ratioFormatter: DecimalFormat
) {
    val itemRatio = if (item.revenue > 0.0) (item.fireCost / item.revenue) * 100.0 else 0.0
    val prevRatio = if (prevItem != null && prevItem.revenue > 0.0) {
        (prevItem.fireCost / prevItem.revenue) * 100.0
    } else null
    val diffFromPrev = if (prevRatio != null) itemRatio - prevRatio else null
    val entryDateLabel = dayMonthFormatter.format(Date(item.auditDateMillis))

    val isItemOverTarget = item.targetRatio > 0.0 && itemRatio > item.targetRatio
    val pillBg = if (isItemOverTarget) ExpiredRedContainer else NormalGreenContainer
    val pillColor = if (isItemOverTarget) ExpiredRedDark else NormalGreenDark

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("continuous_period_row_$rowIndex"),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(
            width = if (isLatest) 1.2.dp else 0.9.dp,
            color = if (isLatest) TurquoisePrimary.copy(alpha = 0.55f) else Slate200
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = entryDateLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                    if (isFirstChronological) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Slate50,
                            border = BorderStroke(0.7.dp, Slate200)
                        ) {
                            Text(
                                text = "İlk",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                            )
                        }
                    } else if (isLatest) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TurquoisePrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Güncel",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TurquoiseDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Ciro: ${moneyFormatter.format(item.revenue)} ₺ • Fire: ${moneyFormatter.format(item.fireCost)} ₺",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate700,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = pillBg
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "%${ratioFormatter.format(itemRatio)}",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = pillColor
                    )
                    if (diffFromPrev != null && abs(diffFromPrev) > 0.0001) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (diffFromPrev < 0.0) "↓" else "↑",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = if (diffFromPrev < 0.0) NormalGreenDark else ExpiredRedDark
                        )
                    }
                }
            }
        }
    }
}
