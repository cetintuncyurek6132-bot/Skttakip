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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryCategoryDetailModal(
    category: String,
    latestAudit: InventoryAuditEntity?,
    categoryHistory: List<InventoryAuditEntity> = emptyList(),
    targetRatio: Double = latestAudit?.targetRatio?.takeIf { it > 0.0 } ?: 1.0,
    onDismiss: () -> Unit,
    onEditClick: (InventoryAuditEntity?) -> Unit,
    onAddNewAuditClick: (String) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = remember { listOf("Özet & Analiz", "Gelişim & Geçmiş") }

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

    val sortedHistory = remember(categoryHistory, latestAudit) {
        val base = if (categoryHistory.isNotEmpty()) {
            categoryHistory
        } else if (latestAudit != null) {
            listOf(latestAudit)
        } else {
            emptyList()
        }
        base.sortedWith(
            compareByDescending<InventoryAuditEntity> { it.updatedAt }
                .thenByDescending { it.auditDateMillis }
                .thenByDescending { it.createdAt }
        )
    }

    // Son 4 haftalık kayıt (kronolojik soldan sağa: en eskiden en yeniye)
    val last4WeeksChronological = remember(sortedHistory) {
        sortedHistory.take(4).reversed()
    }

    val categoryIcon = when (category.uppercase(Locale.forLanguageTag("tr-TR"))) {
        "DOLAP" -> Icons.Default.AcUnit
        "GIDA DIŞI" -> Icons.Default.Category
        else -> Icons.Default.Fastfood
    }

    val (iconPastelBg, iconTint) = when (category.uppercase(Locale.forLanguageTag("tr-TR"))) {
        "DOLAP" -> Pair(Color(0xFFDBEAFE), Color(0xFF1D4ED8))
        "GIDA DIŞI" -> Pair(Color(0xFFEDE9FE), Color(0xFF6D28D9))
        else -> Pair(TurquoisePrimary.copy(alpha = 0.16f), TurquoiseDark)
    }

    val revenue = latestAudit?.revenue ?: 0.0
    val fireCost = latestAudit?.fireCost ?: 0.0
    val effectiveTargetRatio = if ((latestAudit?.targetRatio ?: 0.0) > 0.0) {
        latestAudit!!.targetRatio
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
    val isExceedingTarget = latestAudit != null && fireCost > maxAllowedFire

    AppBottomSheetWrapper(
        onDismissRequest = onDismiss,
        enableVerticalScroll = false
    ) { dismissSheet ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 16.dp)
                .navigationBarsPadding()
                .testTag("inventory_category_detail_modal"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. ÜST BAŞLIK: KATEGORİ ADI + SAĞ ÜST DÜZENLE (KALEM) VE KAPAT BUTONLARI
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
                        color = iconPastelBg,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = categoryIcon,
                                contentDescription = category,
                                tint = iconTint,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = category,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                                text = if (latestAudit != null) {
                                    "Envanter: ${dateFormatter.format(Date(latestAudit.auditDateMillis))}"
                                } else {
                                    "Henüz sayım kaydı yok"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate700
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
                        onClick = { onEditClick(latestAudit) },
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

            // 2. SEKME YAPISI (2 SEKMELİ KAPSÜL TASARIMLI TabRow)
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

            // 3. SEKMELER ARASI YUMUŞAK GEÇİŞLİ İÇERİK (AnimatedContent + verticalScroll)
            AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                label = "inventory_category_detail_tab_transition"
            ) { currentTab ->
                when (currentTab) {
                    0 -> {
                        // SEKME 1: ÖZET & ANALİZ
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
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
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = if (latestAudit != null) {
                                                    "%${ratioFormatter.format(currentRatioPercent)}"
                                                } else {
                                                    "%-"
                                                },
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Black,
                                                color = currentRatioColor,
                                                textAlign = TextAlign.Center
                                            )
                                            if (previousRatioPercent != null && latestAudit != null) {
                                                val diff = currentRatioPercent - previousRatioPercent
                                                if (kotlin.math.abs(diff) > 0.0001) {
                                                    val isDown = diff < 0.0
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (isDown) "↓" else "↑",
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = if (isDown) NormalGreenDark else ExpiredRedDark
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
                                        Text(
                                            text = "%${ratioFormatter.format(effectiveTargetRatio)}",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Slate900,
                                            textAlign = TextAlign.Center
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
                                        Text(
                                            text = "${moneyFormatter.format(revenue)} ₺",
                                            fontSize = 19.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Slate900,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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
                                                color = Slate700
                                            )
                                        }
                                        Text(
                                            text = "${moneyFormatter.format(fireCost)} ₺",
                                            fontSize = 19.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = ExpiredRedDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // Akıllı Finansal Analiz Kutusu
                            if (latestAudit != null && (revenue > 0.0 || fireCost > 0.0)) {
                                val analysisBg = if (isExceedingTarget) ExpiredRedContainer else NormalGreenContainer
                                val analysisBorder = if (isExceedingTarget) ExpiredRedBorder else NormalGreenBorder
                                val analysisText = if (isExceedingTarget) ExpiredRedDark else NormalGreenDark

                                val excessFireAmount = maxOf(0.0, fireCost - maxAllowedFire)
                                val remainingFireBudget = maxOf(0.0, maxAllowedFire - fireCost)

                                val smartMessage = if (isExceedingTarget) {
                                    "⚠️ Hedefi aştınız! Oranınızı hedefe çekmek için fire tutarınızı ${moneyFormatter.format(excessFireAmount)} TL azaltmalı veya cironuzu artırmalısınız."
                                } else {
                                    "✅ Harika! Hedefin altındasınız. Fire bütçenizde ${moneyFormatter.format(remainingFireBudget)} TL boşluk var."
                                }

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("category_detail_smart_analysis_box"),
                                    shape = RoundedCornerShape(16.dp),
                                    color = analysisBg,
                                    border = BorderStroke(1.2.dp, analysisBorder)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Akıllı Finansal Analiz",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = analysisText
                                        )
                                        Text(
                                            text = smartMessage,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = analysisText,
                                            lineHeight = 19.sp
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Maks. Hedef Fire: ${moneyFormatter.format(maxAllowedFire)} ₺",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = analysisText.copy(alpha = 0.9f)
                                            )
                                            Text(
                                                text = "Gerçekleşen: ${moneyFormatter.format(fireCost)} ₺",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = analysisText.copy(alpha = 0.9f)
                                            )
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
                        // SEKME 2: GELİŞİM & GEÇMİŞ
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (sortedHistory.isNotEmpty()) {
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

                                        // HAFTALIK GEÇMİŞ LİSTESİ (En son girilen veri en üstte)
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Haftalık Kayıt Geçmişi",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Slate800
                                            )

                                            sortedHistory.take(8).forEachIndexed { index, item ->
                                                val itemRatio = if (item.revenue > 0.0) {
                                                    (item.fireCost / item.revenue) * 100.0
                                                } else 0.0
                                                val olderItem = sortedHistory.getOrNull(index + 1)
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
                                                        width = if (index == 0) 1.2.dp else 0.8.dp,
                                                        color = if (index == 0) TurquoisePrimary.copy(alpha = 0.55f) else Slate200
                                                    )
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        // Sol: Tarih ve Hafta Kodu
                                                        Column(
                                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(
                                                                    text = dateStr,
                                                                    fontSize = 12.5.sp,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    color = Slate900
                                                                )
                                                                if (index == 0) {
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
                                                                color = Slate700
                                                            )
                                                        }

                                                        // Sağ: Oran ve Trend Oku
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
                                            text = "Henüz haftalık gelişim verisi bulunmuyor.",
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

            // 4. SABİT AKSİYON BUTONU (Hangi sekmede olursa olsun en altta her zaman sabit görünür)
            Button(
                onClick = { onAddNewAuditClick(category) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("category_detail_add_new_audit_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
            ) {
                Text(
                    text = "+ Yeni Haftalık Veri Gir",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
    }
}
