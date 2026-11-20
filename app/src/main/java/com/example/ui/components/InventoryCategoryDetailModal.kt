package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.example.ui.viewmodel.isContinuousAuditCategory
import com.example.ui.viewmodel.normalizeInventoryCategoryKey
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun formatDetailPeriodHeader(
    rawPeriod: String,
    fallbackDateMillis: Long
): String {
    val trLocale = Locale.forLanguageTag("tr-TR")
    val turkishMonths = listOf(
        "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
        "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
    )
    val fallbackYear = Calendar.getInstance().apply {
        timeInMillis = if (fallbackDateMillis > 0L) fallbackDateMillis else System.currentTimeMillis()
    }.get(Calendar.YEAR)

    val cleaned = rawPeriod.substringBefore("•").trim()
    if (cleaned.isEmpty()) {
        val monthName = SimpleDateFormat("MMMM", trLocale)
            .format(Date(if (fallbackDateMillis > 0L) fallbackDateMillis else System.currentTimeMillis()))
            .lowercase(trLocale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(trLocale) else it.toString() }
        return "$monthName $fallbackYear"
    }

    val numericMatch = Regex("^(\\d{1,2})[./-](\\d{4})$").matchEntire(cleaned)
    if (numericMatch != null) {
        val monthIdx = (numericMatch.groupValues[1].toIntOrNull() ?: 1).coerceIn(1, 12) - 1
        return "${turkishMonths[monthIdx]} ${numericMatch.groupValues[2]}"
    }

    val parts = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
    val rawMonth = parts.firstOrNull().orEmpty().lowercase(trLocale)
    val formattedMonth = turkishMonths.firstOrNull { it.lowercase(trLocale) == rawMonth }
        ?: rawMonth.replaceFirstChar { if (it.isLowerCase()) it.titlecase(trLocale) else it.toString() }
    val yearPart = parts.getOrNull(1)?.takeIf { it.all(Char::isDigit) && it.length == 4 }
        ?: fallbackYear.toString()

    return "$formattedMonth $yearPart"
}

private fun resolveCategoryVisuals(normalizedCategoryKey: String): Triple<ImageVector, Color, Color> {
    val icon = when (normalizedCategoryKey) {
        "DOLAP", "DONUK_MEYVE_SEBZE" -> Icons.Default.AcUnit
        "GIDA DIŞI" -> Icons.Default.Category
        "ET_TAVUK" -> Icons.Default.SetMeal
        "EKMEK" -> Icons.Default.BakeryDining
        "MEYVE_SEBZE" -> Icons.Default.Eco
        else -> Icons.Default.Fastfood
    }
    val (pastelBg, tint) = when (normalizedCategoryKey) {
        "DOLAP" -> Pair(Color(0xFFDBEAFE), Color(0xFF1D4ED8))
        "GIDA DIŞI" -> Pair(Color(0xFFEDE9FE), Color(0xFF6D28D9))
        "ET_TAVUK" -> Pair(Color(0xFFFFF1F2), Color(0xFFBE123C))
        "EKMEK" -> Pair(Color(0xFFFFFBEB), Color(0xFFB45309))
        "MEYVE_SEBZE" -> Pair(Color(0xFFF0FDF4), Color(0xFF15803D))
        "DONUK_MEYVE_SEBZE" -> Pair(Color(0xFFF0F9FF), Color(0xFF0369A1))
        else -> Pair(TurquoisePrimary.copy(alpha = 0.16f), TurquoiseDark)
    }
    return Triple(icon, pastelBg, tint)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryCategoryDetailModal(
    initialCategory: String,
    categoryList: List<String> = listOf(initialCategory),
    allAudits: List<InventoryAuditEntity> = emptyList(),
    latestAudit: InventoryAuditEntity? = null,
    categoryHistory: List<InventoryAuditEntity> = emptyList(),
    targetRatio: Double = latestAudit?.targetRatio?.takeIf { it > 0.0 } ?: 1.0,
    onDismiss: () -> Unit,
    onEditClick: (category: String, latestAudit: InventoryAuditEntity?) -> Unit,
    onAddNewAuditClick: (category: String) -> Unit
) {
    val safeCategoryList = remember(categoryList, initialCategory) {
        if (categoryList.isNotEmpty()) categoryList else listOf(initialCategory)
    }
    val initialPageIndex = remember(safeCategoryList, initialCategory) {
        val targetNorm = normalizeInventoryCategoryKey(initialCategory)
        val idx = safeCategoryList.indexOfFirst {
            normalizeInventoryCategoryKey(it) == targetNorm
        }
        if (idx >= 0) idx else 0
    }
    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { safeCategoryList.size }
    )
    val coroutineScope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var fullHistoryModalData by remember { mutableStateOf<Pair<String, List<InventoryAuditEntity>>?>(null) }

    val activeCategory = safeCategoryList.getOrElse(pagerState.currentPage) { initialCategory }
    val activeIsContinuous = remember(activeCategory) { isContinuousAuditCategory(activeCategory) }
    val activeNormalizedKey = remember(activeCategory) { normalizeInventoryCategoryKey(activeCategory) }
    val tabTitles = remember(activeIsContinuous) {
        if (activeIsContinuous) {
            listOf("Özet & Analiz", "Dönem İçi Gelişim")
        } else {
            listOf("Özet & Analiz", "Gelişim & Geçmiş")
        }
    }

    val dateFormatter = remember {
        SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("tr-TR"))
    }
    val shortDateFormatter = remember {
        SimpleDateFormat("dd.MM.yyyy", Locale.forLanguageTag("tr-TR"))
    }
    val chartDateFormatter = remember {
        SimpleDateFormat("dd.MM", Locale.forLanguageTag("tr-TR"))
    }

    val moneyFormatter = remember {
        val symbols = DecimalFormatSymbols(Locale.forLanguageTag("tr-TR")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        DecimalFormat("#,##0.##", symbols)
    }

    val ratioFormatter = remember {
        val symbols = DecimalFormatSymbols(Locale.forLanguageTag("tr-TR")).apply {
            decimalSeparator = ','
        }
        DecimalFormat("0.##", symbols)
    }

    val sortedHistoryByPage = remember(allAudits, safeCategoryList, categoryHistory, latestAudit, initialCategory) {
        val comparator = compareByDescending<InventoryAuditEntity> { it.updatedAt }
            .thenByDescending { it.auditDateMillis }
            .thenByDescending { it.createdAt }
        safeCategoryList.map { catName ->
            val normKey = normalizeInventoryCategoryKey(catName)
            val fromAll = if (allAudits.isNotEmpty()) {
                allAudits.filter { normalizeInventoryCategoryKey(it.category) == normKey }
                    .sortedWith(comparator)
            } else if (normKey == normalizeInventoryCategoryKey(initialCategory)) {
                val base = if (categoryHistory.isNotEmpty()) {
                    categoryHistory
                } else if (latestAudit != null) {
                    listOf(latestAudit)
                } else {
                    emptyList()
                }
                base.sortedWith(comparator)
            } else {
                emptyList()
            }
            fromAll
        }
    }

    val activeSortedHistory = sortedHistoryByPage.getOrElse(pagerState.currentPage) { emptyList() }
    val activeLatestAudit = activeSortedHistory.firstOrNull()
    val (activeCategoryIcon, activeIconPastelBg, activeIconTint) = remember(activeNormalizedKey) {
        resolveCategoryVisuals(activeNormalizedKey)
    }

    AppBottomSheetWrapper(
        onDismissRequest = onDismiss,
        enableVerticalScroll = false,
        enableHorizontalSwipeDismiss = false
    ) { dismissSheet ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("inventory_category_detail_modal")
        ) {
            // SABİT ÜST BÖLÜM: BAŞLIK VE SEKMELER (HorizontalPager DIŞINDA, DİNAMİK)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
            // 1. ÜST BAŞLIK: DİNAMİK KATEGORİ ADI + SAĞ ÜST DÜZENLE VE KAPAT BUTONLARI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = activeIconPastelBg,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = activeCategoryIcon,
                                contentDescription = activeCategory,
                                tint = activeIconTint,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = activeCategory,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("category_detail_dynamic_title")
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Slate700,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = when {
                                    activeLatestAudit == null -> "Henüz sayım kaydı yok"
                                    activeIsContinuous ->
                                        "Dönem: ${formatDetailPeriodHeader(activeLatestAudit.auditPeriod, activeLatestAudit.auditDateMillis)}"
                                    else -> "Envanter: ${dateFormatter.format(Date(activeLatestAudit.auditDateMillis))}"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate700,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // SAĞ ÜST KÖŞE: BELİRGİN DÜZENLE (KALEM İKONU) BUTONU + KAPAT BUTONU
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = { onEditClick(activeCategory, activeLatestAudit) },
                        shape = RoundedCornerShape(12.dp),
                        color = TurquoisePrimary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.45f)),
                        modifier = Modifier.testTag("category_detail_edit_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Düzenle",
                                tint = TurquoiseDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Düzenle",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TurquoiseDark
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .clickable { dismissSheet() }
                            .testTag("category_detail_close_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = Slate700,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // KATEGORİLER ARASI KAYDIRMA GÖSTERGESİ (Birden fazla kategori varsa)
            if (safeCategoryList.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_detail_pager_indicators"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    safeCategoryList.forEachIndexed { idx, catLabel ->
                        val isCurrent = idx == pagerState.currentPage
                        Surface(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(idx)
                                }
                            },
                            shape = RoundedCornerShape(50),
                            color = if (isCurrent) TurquoisePrimary else Slate200.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 3.dp)
                        ) {
                            Text(
                                text = catLabel,
                                fontSize = 10.sp,
                                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isCurrent) Color.White else Slate700,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // 2. SEKME YAPISI (2 SEKMELİ KAPSÜL TASARIMLI TabRow - TÜM SAYFALAR İÇİN ORTAK)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_detail_tab_row"),
                shape = RoundedCornerShape(16.dp),
                color = Slate50,
                border = BorderStroke(1.dp, Slate200)
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = TurquoiseDark,
                    divider = {},
                    indicator = { tabPositions ->
                        if (selectedTabIndex in tabPositions.indices) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier
                                    .tabIndicatorOffset(tabPositions[selectedTabIndex])
                                    .padding(horizontal = 20.dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
                                height = 3.dp,
                                color = TurquoisePrimary
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        val isSelected = selectedTabIndex == index
                        val tabIcon = if (index == 0) {
                            Icons.Default.Analytics
                        } else {
                            Icons.AutoMirrored.Filled.ShowChart
                        }

                        Tab(
                            selected = isSelected,
                            onClick = { selectedTabIndex = index },
                            modifier = Modifier
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) TurquoisePrimary.copy(alpha = 0.14f) else Color.Transparent
                                )
                                .testTag("category_detail_tab_$index")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = tabIcon,
                                    contentDescription = title,
                                    tint = if (isSelected) TurquoiseDark else Slate700,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                    color = if (isSelected) TurquoiseDark else Slate700,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
            } // Sabit üst bölüm sonu

            // 3. KATEGORİLER ARASI SAĞA/SOLA KAYDIRMALI İÇERİK ALANI (HorizontalPager)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .testTag("category_detail_horizontal_pager"),
                verticalAlignment = Alignment.Top
            ) { page ->
                val pageCategory = safeCategoryList.getOrElse(page) { initialCategory }
                val isContinuous = isContinuousAuditCategory(pageCategory)
                val sortedHistory = sortedHistoryByPage.getOrElse(page) { emptyList() }
                val pageLatestAudit = sortedHistory.firstOrNull()
                val last4WeeksChronological = remember(sortedHistory) {
                    sortedHistory.take(4).reversed()
                }

                val revenue = pageLatestAudit?.revenue ?: 0.0
                val fireCost = pageLatestAudit?.fireCost ?: 0.0
                val effectiveTargetRatio = if ((pageLatestAudit?.targetRatio ?: 0.0) > 0.0) {
                    pageLatestAudit!!.targetRatio
                } else if (targetRatio > 0.0) {
                    targetRatio
                } else {
                    1.0
                }
                val currentRatioPercent = if (revenue > 0.0) {
                    (fireCost / revenue) * 100.0
                } else {
                    0.0
                }
                val previousAudit = sortedHistory.getOrNull(1)
                val previousRatioPercent = if (previousAudit != null && previousAudit.revenue > 0.0) {
                    (previousAudit.fireCost / previousAudit.revenue) * 100.0
                } else {
                    null
                }
                val maxAllowedFire = revenue * (effectiveTargetRatio / 100.0)
                val isExceedingTarget = pageLatestAudit != null && fireCost > maxAllowedFire

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    AnimatedContent(
                        targetState = selectedTabIndex,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        modifier = Modifier.fillMaxWidth(),
                        label = "inventory_category_detail_tab_transition_$page"
                    ) { currentTab ->
                        when (currentTab) {
                            0 -> {
                                // SEKME 1: ÖZET & ANALİZ
                                val latestAudit = pageLatestAudit
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Mevcut Envanter Oranı (%X) ve Hedef Oran (%Y) Yan Yana Kutular
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val currentRatioBg = if (latestAudit == null) {
                                    Slate50
                                } else if (isExceedingTarget) {
                                    ExpiredRedContainer.copy(alpha = 0.65f)
                                } else {
                                    NormalGreenContainer.copy(alpha = 0.65f)
                                }
                                val currentRatioBorder = if (latestAudit == null) {
                                    Slate200
                                } else if (isExceedingTarget) {
                                    ExpiredRedBorder
                                } else {
                                    NormalGreenBorder
                                }
                                val currentRatioColor = if (latestAudit == null) {
                                    Slate900
                                } else if (isExceedingTarget) {
                                    ExpiredRedDark
                                } else {
                                    NormalGreenDark
                                }

                                // Mevcut Envanter Oranı Kutusu
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    color = currentRatioBg,
                                    border = BorderStroke(1.2.dp, currentRatioBorder)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Percent,
                                                contentDescription = null,
                                                tint = currentRatioColor,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Mevcut Envanter Oranı",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate700,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            AutoSizeMetricText(
                                                text = if (latestAudit != null) {
                                                    "%${ratioFormatter.format(currentRatioPercent)}"
                                                } else {
                                                    "%-"
                                                },
                                                maxFontSize = 23.sp,
                                                minFontSize = 14.sp,
                                                fontWeight = FontWeight.Black,
                                                color = currentRatioColor,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (previousRatioPercent != null && latestAudit != null) {
                                                val diff = currentRatioPercent - previousRatioPercent
                                                if (kotlin.math.abs(diff) > 0.0001) {
                                                    val isDown = diff < 0.0
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    Text(
                                                        text = if (isDown) "↓" else "↑",
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = if (isDown) NormalGreenDark else ExpiredRedDark,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Hedef Oran Kutusu
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Slate50,
                                    border = BorderStroke(1.2.dp, Slate200)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.TrackChanges,
                                                contentDescription = null,
                                                tint = TurquoiseDark,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Hedef Oran",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate700,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        AutoSizeMetricText(
                                            text = "%${ratioFormatter.format(effectiveTargetRatio)}",
                                            maxFontSize = 23.sp,
                                            minFontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Slate900,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            // Ciro Tutarı ve Fire Tutarı Yan Yana Kutular
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Ciro Tutarı Kutusu
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Slate50,
                                    border = BorderStroke(1.dp, Slate200)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = CircleShape,
                                                color = TurquoisePrimary.copy(alpha = 0.14f),
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Payments,
                                                        contentDescription = null,
                                                        tint = TurquoiseDark,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(7.dp))
                                            Text(
                                                text = "Ciro Tutarı",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate700
                                            )
                                        }
                                        AutoSizeMetricText(
                                            text = "${moneyFormatter.format(revenue)} ₺",
                                            maxFontSize = 18.5.sp,
                                            minFontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Slate900,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                // Fire Tutarı Kutusu
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Slate50,
                                    border = BorderStroke(1.dp, Slate200)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = CircleShape,
                                                color = ExpiredRedContainer,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.LocalFireDepartment,
                                                        contentDescription = null,
                                                        tint = ExpiredRedDark,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(7.dp))
                                            Text(
                                                text = "Fire Tutarı",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate700,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        AutoSizeMetricText(
                                            text = "${moneyFormatter.format(fireCost)} ₺",
                                            maxFontSize = 18.5.sp,
                                            minFontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = ExpiredRedDark,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            // Akıllı Finansal Analiz Kutusu (Projeksiyon Bazlı Analiz + Ay Sonu Projeksiyonu + 2x2 Özet Tablo)
                            if (latestAudit != null && (revenue > 0.0 || fireCost > 0.0)) {
                                val ciro = revenue.coerceAtLeast(0.0)
                                val fire = fireCost.coerceAtLeast(0.0)
                                val hedefOran = effectiveTargetRatio
                                val hedefOndalik = hedefOran / 100.0
                                val hedefCiro = if (hedefOndalik > 0.0) fire / hedefOndalik else 0.0
                                val hedefFire = ciro * hedefOndalik

                                // Ay Sonu Projeksiyonu (Tahminleme) Hesaplaması
                                val entryCalendar = remember(latestAudit.auditDateMillis, latestAudit.createdAt) {
                                    Calendar.getInstance().apply {
                                        timeInMillis = when {
                                            latestAudit.auditDateMillis > 0L -> latestAudit.auditDateMillis
                                            latestAudit.createdAt > 0L -> latestAudit.createdAt
                                            else -> System.currentTimeMillis()
                                        }
                                    }
                                }
                                val girisGunu = entryCalendar.get(Calendar.DAY_OF_MONTH)
                                val gecenGun = maxOf(1, girisGunu - 1)
                                val ayinGunu = entryCalendar.getActualMaximum(Calendar.DAY_OF_MONTH).coerceAtLeast(1)

                                val gunlukOrtCiro = ciro / gecenGun.toDouble()
                                val gunlukOrtFire = fire / gecenGun.toDouble()
                                val tahminiAySonuCiro = gunlukOrtCiro * ayinGunu.toDouble()
                                val tahminiAySonuFire = gunlukOrtFire * ayinGunu.toDouble()
                                val tahminiAySonuOrani = if (tahminiAySonuCiro > 0.0) {
                                    (tahminiAySonuFire / tahminiAySonuCiro) * 100.0
                                } else if (tahminiAySonuFire > 0.0) {
                                    100.0
                                } else {
                                    0.0
                                }

                                val isOverTarget = (tahminiAySonuCiro > 0.0 && tahminiAySonuOrani > hedefOran) ||
                                    (tahminiAySonuCiro <= 0.0 && tahminiAySonuFire > 0.0)

                                val fireAzaltAmount = maxOf(0.0, fire - hedefFire)
                                val ciroArttirAmount = maxOf(0.0, hedefCiro - ciro)

                                val analysisBg = if (isOverTarget) {
                                    ExpiredRedContainer.copy(alpha = 0.45f)
                                } else {
                                    NormalGreenContainer.copy(alpha = 0.45f)
                                }
                                val analysisBorder = if (isOverTarget) {
                                    ExpiredRedBorder.copy(alpha = 0.85f)
                                } else {
                                    NormalGreenBorder.copy(alpha = 0.85f)
                                }
                                val analysisText = if (isOverTarget) ExpiredRedDark else NormalGreenDark
                                val dividerColor = analysisBorder.copy(alpha = 0.55f)

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("category_detail_smart_analysis_box"),
                                    shape = RoundedCornerShape(16.dp),
                                    color = analysisBg,
                                    border = BorderStroke(1.dp, analysisBorder)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // ÜST KISIM: SOLDA AKSİYONLAR, SAĞDA AY SONU TAHMİNİ KUTUCUĞU
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Sol Taraf (Aksiyonlar / Durum)
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                if (isOverTarget) {
                                                    Text(
                                                        text = "⚠️ Hedefi Aştınız!",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = ExpiredRedDark
                                                    )
                                                    Text(
                                                        text = buildAnnotatedString {
                                                            withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                                                append("FİRE: ")
                                                            }
                                                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                                append("${moneyFormatter.format(fireAzaltAmount)} ₺")
                                                            }
                                                            append(" azaltın")
                                                        },
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = ExpiredRedDark
                                                    )
                                                    Text(
                                                        text = buildAnnotatedString {
                                                            withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                                                append("CİRO: ")
                                                            }
                                                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                                append("${moneyFormatter.format(ciroArttirAmount)} ₺")
                                                            }
                                                            append(" arttırın")
                                                        },
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = ExpiredRedDark
                                                    )
                                                } else {
                                                    Text(
                                                        text = "✅ HEDEFTE",
                                                        fontSize = 14.5.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = NormalGreenDark
                                                    )
                                                    Text(
                                                        text = "Harika gidiyorsunuz, hedefin altındasınız.",
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        lineHeight = 17.sp,
                                                        color = NormalGreenDark
                                                    )
                                                }
                                            }

                                            // Sağ Taraf (Ay Sonu Tahmini Kutucuğu)
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("category_detail_month_end_projection_box"),
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color.White.copy(alpha = 0.94f),
                                                border = BorderStroke(0.9.dp, analysisBorder.copy(alpha = 0.7f))
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                                ) {
                                                    Text(
                                                        text = "Ay Sonu Tahmini",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Slate700,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "Ciro:",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Slate700,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        AutoSizeMetricText(
                                                            text = "${moneyFormatter.format(tahminiAySonuCiro)} ₺",
                                                            maxFontSize = 12.sp,
                                                            minFontSize = 8.5.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Slate900,
                                                            textAlign = TextAlign.End,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "Fire:",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Slate700,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        AutoSizeMetricText(
                                                            text = "${moneyFormatter.format(tahminiAySonuFire)} ₺",
                                                            maxFontSize = 12.sp,
                                                            minFontSize = 8.5.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = if (isOverTarget) ExpiredRedDark else Slate900,
                                                            textAlign = TextAlign.End,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        HorizontalDivider(
                                            thickness = 0.9.dp,
                                            color = dividerColor
                                        )

                                        // ALT KISIM: 2x2 ÖZET TABLO
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color.White,
                                            border = BorderStroke(0.9.dp, analysisBorder.copy(alpha = 0.75f))
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 10.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                // Üst Satır: HEDEF CİRO | GERÇEKLEŞEN CİRO
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(IntrinsicSize.Max),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    SummaryMetricCell(
                                                        label = "HEDEF CİRO:",
                                                        value = "${moneyFormatter.format(hedefCiro)} ₺",
                                                        valueColor = Slate900,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .width(1.dp)
                                                            .fillMaxHeight()
                                                            .background(dividerColor)
                                                    )
                                                    SummaryMetricCell(
                                                        label = "GERÇEKLEŞEN CİRO:",
                                                        value = "${moneyFormatter.format(ciro)} ₺",
                                                        valueColor = Slate900,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }

                                                HorizontalDivider(
                                                    thickness = 0.8.dp,
                                                    color = dividerColor,
                                                    modifier = Modifier.padding(horizontal = 10.dp)
                                                )

                                                // Alt Satır: HEDEF FİRE | GERÇEKLEŞEN FİRE
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(IntrinsicSize.Max),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    SummaryMetricCell(
                                                        label = "HEDEF FİRE:",
                                                        value = "${moneyFormatter.format(hedefFire)} ₺",
                                                        valueColor = Slate900,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .width(1.dp)
                                                            .fillMaxHeight()
                                                            .background(dividerColor)
                                                    )
                                                    SummaryMetricCell(
                                                        label = "GERÇEKLEŞEN FİRE:",
                                                        value = "${moneyFormatter.format(fire)} ₺",
                                                        valueColor = analysisText,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Slate50,
                                    border = BorderStroke(1.dp, Slate200)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Bu kategori için henüz finansal veri girilmemiş.",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate800,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Sağ üstteki \"Düzenle\" veya aşağıdaki \"+ Yeni Haftalık Veri Gir\" butonuna dokunarak ciro, fire ve hedef oran bilgilerini girebilirsiniz.",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Slate700,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // Varsa Not Kutusu
                            if (!latestAudit?.notes.isNullOrBlank()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Slate50,
                                    border = BorderStroke(1.dp, Slate200)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Envanter Notu",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate700
                                        )
                                        Text(
                                            text = latestAudit?.notes.orEmpty(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Slate900
                                        )
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        // SEKME 2: DÖNEM İÇİ GELİŞİM (Sürekli) veya GELİŞİM & GEÇMİŞ (Periyodik)
                        val latestAudit = pageLatestAudit
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (isContinuous) {
                                ContinuousPeriodProgressSection(
                                    activePeriod = latestAudit?.auditPeriod.orEmpty(),
                                    sortedHistoryNewestFirst = sortedHistory,
                                    effectiveTargetRatio = effectiveTargetRatio,
                                    moneyFormatter = moneyFormatter,
                                    ratioFormatter = ratioFormatter
                                )
                            } else if (sortedHistory.isNotEmpty()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("category_detail_weekly_progress_section"),
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
                                        // Başlık + Haftalık Trend Rozeti
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                                    contentDescription = null,
                                                    tint = TurquoiseDark,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                                Spacer(modifier = Modifier.width(7.dp))
                                                Text(
                                                    text = "Haftalık Gelişim",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Slate900
                                                )
                                            }

                                            if (previousRatioPercent != null) {
                                                val diff = currentRatioPercent - previousRatioPercent
                                                val isImproved = diff < 0.0
                                                val trendBg = if (isImproved) NormalGreenContainer else if (diff > 0.0) ExpiredRedContainer else Color.White
                                                val trendText = if (isImproved) NormalGreenDark else if (diff > 0.0) ExpiredRedDark else Slate700
                                                val trendLabel = when {
                                                    diff < -0.0001 -> "↓ %${ratioFormatter.format(kotlin.math.abs(diff))} İyileşme"
                                                    diff > 0.0001 -> "↑ %${ratioFormatter.format(diff)} Artış"
                                                    else -> "Değişim Yok"
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = trendBg
                                                ) {
                                                    Text(
                                                        text = trendLabel,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = trendText,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            } else {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = TurquoisePrimary.copy(alpha = 0.14f)
                                                ) {
                                                    Text(
                                                        text = "Son ${last4WeeksChronological.size} Hafta",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TurquoiseDark,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // SON 4 HAFTALIK ÇİZGİ GRAFİK (Compose Canvas)
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Slate200)
                                        ) {
                                            val chartRatios = remember(last4WeeksChronological) {
                                                last4WeeksChronological.map { item ->
                                                    if (item.revenue > 0.0) (item.fireCost / item.revenue) * 100.0 else 0.0
                                                }
                                            }
                                            val isDownwardTrend = chartRatios.size >= 2 && chartRatios.last() <= chartRatios.first()
                                            val lineColor = if (isDownwardTrend) NormalGreenDark else ExpiredRedDark

                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Üstte her haftanın oran değerleri
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceEvenly
                                                ) {
                                                    chartRatios.forEachIndexed { idx, ratioVal ->
                                                        val isLatestPoint = idx == chartRatios.lastIndex
                                                        Text(
                                                            text = "%${ratioFormatter.format(ratioVal)}",
                                                            fontSize = 11.5.sp,
                                                            fontWeight = if (isLatestPoint) FontWeight.Black else FontWeight.Bold,
                                                            color = if (isLatestPoint) lineColor else Slate800,
                                                            textAlign = TextAlign.Center,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }
                                                }

                                                // Çizgi Grafik Canvas
                                                Canvas(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(92.dp)
                                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                                ) {
                                                    val w = size.width
                                                    val h = size.height
                                                    val maxVal = (chartRatios.maxOrNull() ?: 1.0).coerceAtLeast(effectiveTargetRatio).coerceAtLeast(0.5) * 1.15
                                                    val minVal = 0.0
                                                    val range = (maxVal - minVal).coerceAtLeast(0.1)

                                                    // Yatay kılavuz çizgileri
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
                                                            radius = if (isLast) 6.5.dp.toPx() else 5.dp.toPx(),
                                                            center = pt
                                                        )
                                                        drawCircle(
                                                            color = if (isLast) lineColor else TurquoiseDark,
                                                            radius = if (isLast) 4.5.dp.toPx() else 3.5.dp.toPx(),
                                                            center = pt
                                                        )
                                                    }
                                                }

                                                // Altta hafta / tarih etiketleri
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceEvenly
                                                ) {
                                                    last4WeeksChronological.forEachIndexed { idx, item ->
                                                        val ts = if (item.updatedAt > 0L) item.updatedAt else item.auditDateMillis
                                                        val label = chartDateFormatter.format(Date(ts))
                                                        Text(
                                                            text = if (idx == last4WeeksChronological.lastIndex) "$label (Son)" else label,
                                                            fontSize = 10.5.sp,
                                                            fontWeight = if (idx == last4WeeksChronological.lastIndex) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                            color = Slate700,
                                                            textAlign = TextAlign.Center,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // HAFTALIK GEÇMİŞ LİSTESİ (Sabit max 280.dp iç kaydırmalı kutu + Son 5 kayıt)
                                        val recentPeriodicHistory = remember(sortedHistory) {
                                            sortedHistory.take(5)
                                        }
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Geçmiş Kayıtlar (Son ${recentPeriodicHistory.size} kayıt)",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Slate800
                                                )
                                                if (sortedHistory.size > recentPeriodicHistory.size) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color.White,
                                                        border = BorderStroke(0.8.dp, Slate200)
                                                    ) {
                                                        Text(
                                                            text = "Toplam ${sortedHistory.size}",
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Slate700,
                                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            if (recentPeriodicHistory.isEmpty()) {
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
                                                        .heightIn(max = 280.dp)
                                                        .testTag("category_detail_history_list"),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    itemsIndexed(
                                                        items = recentPeriodicHistory,
                                                        key = { index, item -> "${item.id}_$index" }
                                                    ) { index, item ->
                                                        val olderItem = sortedHistory.getOrNull(index + 1)
                                                        PeriodicHistoryItemRow(
                                                            item = item,
                                                            olderItem = olderItem,
                                                            isLatest = index == 0,
                                                            shortDateFormatter = shortDateFormatter,
                                                            moneyFormatter = moneyFormatter,
                                                            ratioFormatter = ratioFormatter
                                                        )
                                                    }
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        fullHistoryModalData = Pair(pageCategory, sortedHistory)
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(48.dp)
                                                        .testTag("category_detail_view_all_history_button"),
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
                                                        text = "Tüm Geçmişi Gör (${sortedHistory.size})",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
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
                                            text = "Aşağıdaki \"+ Yeni Haftalık Veri Gir\" butonuna dokunarak ilk haftalık kaydınızı oluşturabilirsiniz.",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Slate700,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                }
            } // Kaydırılabilir orta içerik sonu
            } // HorizontalPager sonu

            HorizontalDivider(color = Slate200.copy(alpha = 0.7f), thickness = 1.dp)

            // 4. SABİT AKSİYON BUTONU ALANI (Hangi sekmede olursa olsun en altta her zaman sabit görünür)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = { onAddNewAuditClick(activeCategory) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("category_detail_add_new_audit_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
                ) {
                    Text(
                        text = if (activeIsContinuous) "+ Yeni Dönem Verisi Gir" else "+ Yeni Haftalık Veri Gir",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }

    fullHistoryModalData?.let { (historyCategory, historyItems) ->
        AllAuditHistoryFullScreenDialog(
            title = "Tüm Geçmiş Kayıtlar",
            subtitle = "$historyCategory • ${historyItems.size} kayıt",
            items = historyItems,
            shortDateFormatter = shortDateFormatter,
            moneyFormatter = moneyFormatter,
            ratioFormatter = ratioFormatter,
            onDismiss = { fullHistoryModalData = null }
        )
    }
}

@Composable
private fun PeriodicHistoryItemRow(
    item: InventoryAuditEntity,
    olderItem: InventoryAuditEntity?,
    isLatest: Boolean,
    shortDateFormatter: SimpleDateFormat,
    moneyFormatter: DecimalFormat,
    ratioFormatter: DecimalFormat
) {
    val itemRatio = if (item.revenue > 0.0) {
        (item.fireCost / item.revenue) * 100.0
    } else 0.0
    val olderRatio = if (olderItem != null && olderItem.revenue > 0.0) {
        (olderItem.fireCost / olderItem.revenue) * 100.0
    } else null
    val diffFromOlder = if (olderRatio != null) itemRatio - olderRatio else null
    val displayTimestamp = if (item.updatedAt > 0L) item.updatedAt else item.auditDateMillis
    val dateStr = shortDateFormatter.format(Date(displayTimestamp))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(
            width = if (isLatest) 1.2.dp else 0.8.dp,
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
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateStr,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                    if (isLatest) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TurquoisePrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Güncel",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TurquoiseDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

            val isItemOverTarget = item.targetRatio > 0.0 && itemRatio > item.targetRatio
            val pillBg = if (isItemOverTarget) ExpiredRedContainer else NormalGreenContainer
            val pillColor = if (isItemOverTarget) ExpiredRedDark else NormalGreenDark

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = pillBg
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "%${ratioFormatter.format(itemRatio)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = pillColor
                    )
                    if (diffFromOlder != null && kotlin.math.abs(diffFromOlder) > 0.0001) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (diffFromOlder < 0.0) "↓" else "↑",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = if (diffFromOlder < 0.0) NormalGreenDark else ExpiredRedDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AllAuditHistoryFullScreenDialog(
    title: String,
    subtitle: String,
    items: List<InventoryAuditEntity>,
    shortDateFormatter: SimpleDateFormat,
    moneyFormatter: DecimalFormat,
    ratioFormatter: DecimalFormat,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
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
                                text = title,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = subtitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate700,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("all_history_dialog_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = Slate700
                        )
                    }
                }

                HorizontalDivider(color = Slate200, thickness = 1.dp)

                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Henüz geçmiş kaydı bulunmuyor",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate700,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Slate50),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = items,
                            key = { index, item -> "all_${item.id}_$index" }
                        ) { index, item ->
                            PeriodicHistoryItemRow(
                                item = item,
                                olderItem = items.getOrNull(index + 1),
                                isLatest = index == 0,
                                shortDateFormatter = shortDateFormatter,
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
private fun SummaryMetricCell(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Slate700,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        AutoSizeMetricText(
            text = value,
            maxFontSize = 13.5.sp,
            minFontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

