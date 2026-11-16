package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InventoryAuditEntity
import com.example.ui.theme.CriticalOrangeBorder
import com.example.ui.theme.CriticalOrangeContainer
import com.example.ui.theme.CriticalOrangeDark
import com.example.ui.theme.ExpiredRedBorder
import com.example.ui.theme.ExpiredRedContainer
import com.example.ui.theme.ExpiredRedDark
import com.example.ui.theme.NormalGreenBorder
import com.example.ui.theme.NormalGreenContainer
import com.example.ui.theme.NormalGreenDark
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import com.example.ui.viewmodel.CombinedInventorySummary
import com.example.ui.viewmodel.isContinuousAuditCategory
import com.example.ui.viewmodel.normalizeInventoryCategoryKey
import java.text.DecimalFormat
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class AuditCategorySpec(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val pastelBg: Color,
    val iconTint: Color
)

data class QuadrupleStatus(
    val label: String,
    val textColor: Color,
    val bgColor: Color,
    val borderColor: Color
)

fun formatAuditCategoryLabel(rawCategory: String): String {
    return when (normalizeInventoryCategoryKey(rawCategory)) {
        "ET_TAVUK" -> "ET-TAVUK"
        "EKMEK" -> "EKMEK"
        "MEYVE_SEBZE" -> "MEYVE-SEBZE"
        "DONUK_MEYVE_SEBZE" -> "DONUK MEYVE-SEBZE"
        else -> rawCategory.trim()
    }
}

fun calculateDaysSinceAudit(auditDateMillis: Long, todayMidnight: Long): Long {
    val auditCal = Calendar.getInstance().apply {
        timeInMillis = auditDateMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val diffMillis = maxOf(0L, todayMidnight - auditCal.timeInMillis)
    return TimeUnit.MILLISECONDS.toDays(diffMillis)
}

@Composable
fun CombinedStoreInventoryCard(
    summary: CombinedInventorySummary,
    combinedTargetRatio: Double = 2.0,
    onEditCombinedTargetClick: () -> Unit = {},
    moneyFormatter: DecimalFormat,
    ratioFormatter: DecimalFormat,
    modifier: Modifier = Modifier
) {
    val hasCombinedRatio = summary.hasData && summary.combinedRatio != null
    val isOverTarget = hasCombinedRatio && (summary.combinedRatio ?: 0.0) > combinedTargetRatio

    val cardBg = when {
        !hasCombinedRatio -> Slate50
        isOverTarget -> ExpiredRedContainer.copy(alpha = 0.32f)
        else -> NormalGreenContainer.copy(alpha = 0.35f)
    }
    val cardBorder = when {
        !hasCombinedRatio -> Slate200
        isOverTarget -> ExpiredRedBorder
        else -> NormalGreenBorder
    }

    val totalAccentColor = when {
        !hasCombinedRatio -> TurquoiseDark
        isOverTarget -> ExpiredRedDark
        else -> NormalGreenDark
    }
    val totalBoxBg = when {
        !hasCombinedRatio -> Color.White
        isOverTarget -> ExpiredRedContainer.copy(alpha = 0.72f)
        else -> NormalGreenContainer.copy(alpha = 0.72f)
    }
    val totalBoxBorder = when {
        !hasCombinedRatio -> TurquoisePrimary.copy(alpha = 0.35f)
        isOverTarget -> ExpiredRedBorder
        else -> NormalGreenBorder
    }

    val statusChipText = when {
        !hasCombinedRatio -> "Veri Bekleniyor"
        isOverTarget -> "Hedef Üstü (%${ratioFormatter.format(combinedTargetRatio)})"
        else -> "Hedef Altı (%${ratioFormatter.format(combinedTargetRatio)})"
    }
    val statusChipBg = when {
        !hasCombinedRatio -> TurquoisePrimary.copy(alpha = 0.12f)
        isOverTarget -> ExpiredRedContainer
        else -> NormalGreenContainer
    }
    val statusChipTextColor = when {
        !hasCombinedRatio -> TurquoiseDark
        isOverTarget -> ExpiredRedDark
        else -> NormalGreenDark
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("combined_store_inventory_card"),
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.2.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            // Başlık + Hedef Durumu Rozeti
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
                        color = Color.White,
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = totalAccentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mağaza Birleşik Envanteri",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusChipBg,
                    border = BorderStroke(0.8.dp, cardBorder)
                ) {
                    Text(
                        text = statusChipText,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusChipTextColor,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // 3 Sütun: T (Toplam), P (Parçalı), S (Sürekli)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                // T (Toplam) - En büyük ve tok font + Hedef & Kalem Düzenle
                Surface(
                    modifier = Modifier
                        .weight(1.28f)
                        .fillMaxHeight()
                        .testTag("combined_metric_total"),
                    shape = RoundedCornerShape(13.dp),
                    color = totalBoxBg,
                    border = BorderStroke(1.2.dp, totalBoxBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 7.dp, vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            MetricLetterBadge(
                                letter = "T",
                                bgColor = totalAccentColor,
                                textColor = Color.White
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Toplam",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate800,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = if (summary.combinedRatio != null) {
                                "%${ratioFormatter.format(summary.combinedRatio)}"
                            } else {
                                "%-"
                            },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = if (hasCombinedRatio) totalAccentColor else Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            onClick = onEditCombinedTargetClick,
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.9f),
                            border = BorderStroke(0.8.dp, totalBoxBorder),
                            modifier = Modifier.testTag("edit_combined_target_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Hedef: %${ratioFormatter.format(combinedTargetRatio)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = totalAccentColor,
                                    maxLines = 1
                                )
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Genel Hedefi Düzenle",
                                    tint = totalAccentColor,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
                }

                // P (Parçalı)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("combined_metric_periodic"),
                    shape = RoundedCornerShape(13.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            MetricLetterBadge(
                                letter = "P",
                                bgColor = TurquoisePrimary.copy(alpha = 0.16f),
                                textColor = TurquoiseDark
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Parçalı",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = if (summary.periodicRatio != null) {
                                "%${ratioFormatter.format(summary.periodicRatio)}"
                            } else {
                                "%-"
                            },
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // S (Sürekli)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("combined_metric_continuous"),
                    shape = RoundedCornerShape(13.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            MetricLetterBadge(
                                letter = "S",
                                bgColor = Color(0xFFDBEAFE),
                                textColor = Color(0xFF1D4ED8)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Sürekli",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = if (summary.continuousRatio != null) {
                                "%${ratioFormatter.format(summary.continuousRatio)}"
                            } else {
                                "%-"
                            },
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Akıllı Yönlendirme Uyarı Kutusu (Genel Hedef Aşıldığında)
            if (isOverTarget) {
                val worst = summary.worstDeviationCategory
                val guidanceText = if (worst != null) {
                    "⚠️ Mağaza hedefi aşıldı. En büyük sapma ${worst.displayName} reyonunda. Fire oranını düşürmek için öncelikle ${worst.displayName} ürünlerini kontrol edin."
                } else {
                    "⚠️ Mağaza hedefi aşıldı. Fire oranını düşürmek için reyonları kontrol edin."
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("combined_store_smart_guidance_banner"),
                    shape = RoundedCornerShape(12.dp),
                    color = ExpiredRedContainer,
                    border = BorderStroke(1.dp, ExpiredRedBorder)
                ) {
                    Text(
                        text = guidanceText,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ExpiredRedDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }

            // Alt Bilgi Satırı: Toplam Ciro ve Toplam Fire
            HorizontalDivider(thickness = 0.8.dp, color = cardBorder.copy(alpha = 0.55f))
            Text(
                text = "Toplam Ciro: ${moneyFormatter.format(summary.totalRevenue)} TL | Toplam Fire: ${moneyFormatter.format(summary.totalFireCost)} TL",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Slate700,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MetricLetterBadge(
    letter: String,
    bgColor: Color,
    textColor: Color
) {
    Surface(
        shape = CircleShape,
        color = bgColor,
        modifier = Modifier.size(20.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = letter,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                color = textColor
            )
        }
    }
}

@Composable
fun InventoryTrackingTabRow(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabTitles = remember { listOf("Periyodik Envanter", "Sürekli Envanter") }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("inventory_tracking_tab_row"),
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
                Tab(
                    selected = isSelected,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) TurquoisePrimary.copy(alpha = 0.14f) else Color.Transparent
                        )
                        .testTag("inventory_tracking_tab_$index")
                ) {
                    Text(
                        text = title,
                        fontSize = 12.5.sp,
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

@Composable
fun PeriodicCategoryCardsRow(
    categories: List<AuditCategorySpec>,
    audits: List<InventoryAuditEntity>,
    todayMidnight: Long,
    ratioFormatter: DecimalFormat,
    onCategoryClick: (AuditCategorySpec) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { spec ->
            val sortedCategoryAudits = remember(audits, spec.key) {
                audits.filter {
                    normalizeInventoryCategoryKey(it.category) == normalizeInventoryCategoryKey(spec.key)
                }.sortedWith(
                    compareByDescending<InventoryAuditEntity> { it.updatedAt }
                        .thenByDescending { it.auditDateMillis }
                        .thenByDescending { it.createdAt }
                )
            }
            val latestAudit = sortedCategoryAudits.getOrNull(0)
            val previousAudit = sortedCategoryAudits.getOrNull(1)

            val daysElapsed = remember(latestAudit, todayMidnight) {
                if (latestAudit == null || latestAudit.auditDateMillis <= 0L) {
                    null
                } else {
                    calculateDaysSinceAudit(latestAudit.auditDateMillis, todayMidnight)
                }
            }
            val remainingDays = remember(daysElapsed) {
                if (daysElapsed == null) null else 90L - daysElapsed
            }

            val currentRatioVal = remember(latestAudit) {
                if (latestAudit != null && latestAudit.revenue > 0.0) {
                    (latestAudit.fireCost / latestAudit.revenue) * 100.0
                } else null
            }
            val prevRatioVal = remember(previousAudit) {
                if (previousAudit != null && previousAudit.revenue > 0.0) {
                    (previousAudit.fireCost / previousAudit.revenue) * 100.0
                } else null
            }

            val ratioPercentText = remember(latestAudit, currentRatioVal) {
                when {
                    latestAudit == null -> "%-"
                    currentRatioVal != null -> "%${ratioFormatter.format(currentRatioVal)}"
                    else -> "%0"
                }
            }
            val elapsedText = if (daysElapsed == null) "Geçen: -" else "Geçen: $daysElapsed Gün"

            val (remainingLabel, statusColor, statusBgColor, statusBorderColor) = when {
                remainingDays == null -> QuadrupleStatus(
                    "Kayıt Yok",
                    ExpiredRedDark,
                    ExpiredRedContainer,
                    ExpiredRedBorder
                )
                remainingDays < 0L -> QuadrupleStatus(
                    "${abs(remainingDays)} Gün Gecikti",
                    ExpiredRedDark,
                    ExpiredRedContainer,
                    ExpiredRedBorder
                )
                remainingDays in 0L..30L -> QuadrupleStatus(
                    "Kalan: $remainingDays Gün",
                    CriticalOrangeDark,
                    CriticalOrangeContainer,
                    CriticalOrangeBorder
                )
                else -> QuadrupleStatus(
                    "Kalan: $remainingDays Gün",
                    NormalGreenDark,
                    NormalGreenContainer,
                    NormalGreenBorder
                )
            }

            Card(
                onClick = { onCategoryClick(spec) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("inventory_card_${spec.key}"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = statusBgColor.copy(alpha = 0.45f)
                ),
                border = BorderStroke(1.2.dp, statusBorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(1.dp, statusBorderColor),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = spec.icon,
                                contentDescription = spec.title,
                                tint = statusColor,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Text(
                        text = spec.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = ratioPercentText,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        if (currentRatioVal != null && prevRatioVal != null) {
                            val diff = currentRatioVal - prevRatioVal
                            if (abs(diff) > 0.0001) {
                                val isDown = diff < 0.0
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isDown) "↓" else "↑",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDown) NormalGreenDark else ExpiredRedDark
                                )
                            }
                        }
                    }

                    Text(
                        text = elapsedText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusBgColor,
                        border = BorderStroke(0.8.dp, statusBorderColor)
                    ) {
                        Text(
                            text = remainingLabel,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContinuousCategoryCardsRow(
    categories: List<AuditCategorySpec>,
    audits: List<InventoryAuditEntity>,
    moneyFormatter: DecimalFormat,
    ratioFormatter: DecimalFormat,
    onCategoryClick: (AuditCategorySpec, InventoryAuditEntity?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        categories.forEach { spec ->
            val sortedCategoryAudits = remember(audits, spec.key) {
                audits.filter {
                    normalizeInventoryCategoryKey(it.category) == normalizeInventoryCategoryKey(spec.key)
                }.sortedWith(
                    compareByDescending<InventoryAuditEntity> { it.updatedAt }
                        .thenByDescending { it.auditDateMillis }
                        .thenByDescending { it.createdAt }
                )
            }
            val latestAudit = sortedCategoryAudits.getOrNull(0)
            val previousAudit = sortedCategoryAudits.getOrNull(1)

            val currentRatioVal = remember(latestAudit) {
                if (latestAudit != null && latestAudit.revenue > 0.0) {
                    (latestAudit.fireCost / latestAudit.revenue) * 100.0
                } else null
            }
            val prevRatioVal = remember(previousAudit) {
                if (previousAudit != null && previousAudit.revenue > 0.0) {
                    (previousAudit.fireCost / previousAudit.revenue) * 100.0
                } else null
            }

            val ratioPercentText = remember(latestAudit, currentRatioVal) {
                when {
                    latestAudit == null -> "%-"
                    currentRatioVal != null -> "%${ratioFormatter.format(currentRatioVal)}"
                    else -> "%0"
                }
            }

            val revenueText = remember(latestAudit) {
                if (latestAudit != null) {
                    "${moneyFormatter.format(latestAudit.revenue.coerceAtLeast(0.0))} ₺"
                } else {
                    "-"
                }
            }
            val fireText = remember(latestAudit) {
                if (latestAudit != null) {
                    "${moneyFormatter.format(latestAudit.fireCost.coerceAtLeast(0.0))} ₺"
                } else {
                    "-"
                }
            }

            val effectiveTarget = if ((latestAudit?.targetRatio ?: 0.0) > 0.0) {
                latestAudit!!.targetRatio
            } else {
                1.0
            }
            val isOverTarget = latestAudit != null && (
                (latestAudit.revenue > 0.0 && (currentRatioVal ?: 0.0) > effectiveTarget) ||
                    (latestAudit.revenue <= 0.0 && latestAudit.fireCost > 0.0)
                )

            val periodBadgeLabel = when {
                latestAudit == null -> "Kayıt Yok"
                latestAudit.auditPeriod.isNotBlank() -> latestAudit.auditPeriod
                else -> "Dönem Yok"
            }

            val (statusColor, statusBgColor, statusBorderColor) = when {
                latestAudit == null -> Triple(Slate700, Slate50, Slate200)
                isOverTarget -> Triple(ExpiredRedDark, ExpiredRedContainer, ExpiredRedBorder)
                else -> Triple(NormalGreenDark, NormalGreenContainer, NormalGreenBorder)
            }

            val cardBg = if (latestAudit == null) {
                spec.pastelBg
            } else {
                statusBgColor.copy(alpha = 0.45f)
            }

            Card(
                onClick = { onCategoryClick(spec, latestAudit) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("continuous_inventory_card_${spec.key}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.2.dp, statusBorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(horizontal = 5.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            border = BorderStroke(1.dp, statusBorderColor),
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = spec.icon,
                                    contentDescription = spec.title,
                                    tint = if (latestAudit == null) spec.iconTint else statusColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Text(
                            text = spec.title,
                            fontSize = 10.sp,
                            lineHeight = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900,
                            minLines = 2,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = ratioPercentText,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Black,
                            color = if (latestAudit == null) Slate900 else statusColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        if (currentRatioVal != null && prevRatioVal != null) {
                            val diff = currentRatioVal - prevRatioVal
                            if (abs(diff) > 0.0001) {
                                val isDown = diff < 0.0
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = if (isDown) "↓" else "↑",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDown) NormalGreenDark else ExpiredRedDark
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Ciro",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate500,
                                maxLines = 1
                            )
                            Text(
                                text = revenueText,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Fire",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate500,
                                maxLines = 1
                            )
                            Text(
                                text = fireText,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (latestAudit != null) ExpiredRedDark else Slate700,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusBgColor,
                        border = BorderStroke(0.8.dp, statusBorderColor)
                    ) {
                        Text(
                            text = periodBadgeLabel,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContinuousTotalTargetStrip(
    continuousTargetRatio: Double,
    ratioFormatter: DecimalFormat,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onEditClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("continuous_total_target_strip"),
        shape = RoundedCornerShape(14.dp),
        color = TurquoisePrimary.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.TrackChanges,
                    contentDescription = null,
                    tint = TurquoiseDark,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = "Sürekli Envanter Toplam Hedefi: %${ratioFormatter.format(continuousTargetRatio)}",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TurquoiseDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = BorderStroke(0.9.dp, TurquoisePrimary.copy(alpha = 0.4f)),
                modifier = Modifier.testTag("edit_continuous_target_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Sürekli Hedefi Düzenle",
                        tint = TurquoiseDark,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Düzenle",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TurquoiseDark
                    )
                }
            }
        }
    }
}

@Composable
fun ContinuousInventoryTotalCard(
    latestAudits: List<InventoryAuditEntity>,
    continuousTargetRatio: Double = 5.0,
    moneyFormatter: DecimalFormat,
    ratioFormatter: DecimalFormat,
    modifier: Modifier = Modifier
) {
    val totalRevenue = remember(latestAudits) {
        latestAudits.sumOf { it.revenue.coerceAtLeast(0.0) }
    }
    val totalFire = remember(latestAudits) {
        latestAudits.sumOf { it.fireCost.coerceAtLeast(0.0) }
    }
    val generalRatio = remember(totalRevenue, totalFire) {
        if (totalRevenue > 0.0) (totalFire / totalRevenue) * 100.0 else null
    }
    val isOverTarget = generalRatio != null && generalRatio > continuousTargetRatio

    val cardBg = when {
        generalRatio == null -> Slate50
        isOverTarget -> ExpiredRedContainer.copy(alpha = 0.35f)
        else -> NormalGreenContainer.copy(alpha = 0.38f)
    }
    val cardBorder = when {
        generalRatio == null -> Slate200
        isOverTarget -> ExpiredRedBorder
        else -> NormalGreenBorder
    }
    val accentColor = when {
        generalRatio == null -> TurquoiseDark
        isOverTarget -> ExpiredRedDark
        else -> NormalGreenDark
    }
    val ratioBoxBg = when {
        generalRatio == null -> Color.White
        isOverTarget -> ExpiredRedContainer.copy(alpha = 0.78f)
        else -> NormalGreenContainer.copy(alpha = 0.78f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("continuous_inventory_total_card"),
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.2.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "Sürekli Envanter Toplamı",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = Color.White,
                    border = BorderStroke(0.9.dp, cardBorder)
                ) {
                    Text(
                        text = "Hedef: %${ratioFormatter.format(continuousTargetRatio)} • ${latestAudits.size}/4",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sol Sütun: Toplam Ciro ve Toplam Fire
                Column(
                    modifier = Modifier
                        .weight(1.25f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(13.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 11.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Toplam Ciro",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700
                                )
                            }
                            Text(
                                text = "${moneyFormatter.format(totalRevenue)} ₺",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(13.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 11.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = ExpiredRedDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Toplam Fire",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700
                                )
                            }
                            Text(
                                text = "${moneyFormatter.format(totalFire)} ₺",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ExpiredRedDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Sağ Sütun: Genel Oran (%X) - Büyük ve tok vurgu
                Surface(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                        .testTag("continuous_total_general_ratio"),
                    shape = RoundedCornerShape(15.dp),
                    color = ratioBoxBg,
                    border = BorderStroke(1.2.dp, cardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Genel Oran",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate700,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (generalRatio != null) {
                                "%${ratioFormatter.format(generalRatio)}"
                            } else {
                                "%-"
                            },
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyTrendSummaryCard(
    title: String,
    categories: List<AuditCategorySpec>,
    audits: List<InventoryAuditEntity>,
    ratioFormatter: DecimalFormat
) {
    val perCategorySorted = remember(audits, categories) {
        categories.map { spec ->
            audits.filter {
                normalizeInventoryCategoryKey(it.category) == normalizeInventoryCategoryKey(spec.key)
            }.sortedWith(
                compareByDescending<InventoryAuditEntity> { it.updatedAt }
                    .thenByDescending { it.auditDateMillis }
                    .thenByDescending { it.createdAt }
            )
        }
    }

    val thisWeekEntries = remember(perCategorySorted) {
        perCategorySorted.mapNotNull { it.getOrNull(0) }
    }
    val lastWeekEntries = remember(perCategorySorted) {
        perCategorySorted.mapNotNull { it.getOrNull(1) }
    }

    val thisWeekTotalRevenue = thisWeekEntries.sumOf { it.revenue }
    val thisWeekTotalFire = thisWeekEntries.sumOf { it.fireCost }
    val thisWeekRatio = if (thisWeekTotalRevenue > 0.0) {
        (thisWeekTotalFire / thisWeekTotalRevenue) * 100.0
    } else null

    val lastWeekTotalRevenue = lastWeekEntries.sumOf { it.revenue }
    val lastWeekTotalFire = lastWeekEntries.sumOf { it.fireCost }
    val lastWeekRatio = if (lastWeekTotalRevenue > 0.0) {
        (lastWeekTotalFire / lastWeekTotalRevenue) * 100.0
    } else null

    val hasComparison = thisWeekRatio != null && lastWeekRatio != null
    val ratioDiff = if (hasComparison) thisWeekRatio!! - lastWeekRatio!! else 0.0
    val isImproved = hasComparison && ratioDiff < -0.0001
    val isWorsened = hasComparison && ratioDiff > 0.0001

    val badgeBg = when {
        isImproved -> NormalGreenContainer
        isWorsened -> ExpiredRedContainer
        else -> TurquoisePrimary.copy(alpha = 0.14f)
    }
    val badgeBorder = when {
        isImproved -> NormalGreenBorder
        isWorsened -> ExpiredRedBorder
        else -> TurquoisePrimary.copy(alpha = 0.35f)
    }
    val badgeText = when {
        isImproved -> NormalGreenDark
        isWorsened -> ExpiredRedDark
        else -> TurquoiseDark
    }
    val statusLabel = when {
        isImproved -> "↓ İyileşme Var"
        isWorsened -> "↑ Kötüleşme Var"
        hasComparison -> "Değişim Yok"
        thisWeekRatio != null -> "İlk Hafta Verisi"
        else -> "Veri Bekleniyor"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("general_weekly_trend_card"),
        shape = RoundedCornerShape(18.dp),
        color = Slate50,
        border = BorderStroke(1.2.dp, Slate200)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeBg,
                    border = BorderStroke(1.dp, badgeBorder)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = badgeText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Bu Hafta: Oran ${if (thisWeekRatio != null) "%${ratioFormatter.format(thisWeekRatio)}" else "%-"}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Toplam Fire / Toplam Ciro",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate700,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Geçen Hafta: Oran ${if (lastWeekRatio != null) "%${ratioFormatter.format(lastWeekRatio)}" else "%-"}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate800,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (hasComparison) {
                                val diffStr = ratioFormatter.format(abs(ratioDiff))
                                if (isImproved) "%$diffStr düşüş sağlandı"
                                else if (isWorsened) "%$diffStr artış oluştu"
                                else "Oran sabit kaldı"
                            } else {
                                "Önceki hafta kaydı yok"
                            },
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isImproved) NormalGreenDark else if (isWorsened) ExpiredRedDark else Slate700,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SmartCategoryAnalysisCard(
    spec: AuditCategorySpec,
    latestAudit: InventoryAuditEntity?,
    moneyFormatter: DecimalFormat,
    ratioFormatter: DecimalFormat,
    onClick: () -> Unit = {}
) {
    if (latestAudit == null) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("smart_analysis_card_${spec.key}"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Slate50),
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = spec.pastelBg,
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = spec.icon,
                                contentDescription = spec.title,
                                tint = spec.iconTint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = spec.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Text(
                        text = "Henüz veri girilmedi",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
        return
    }

    val revenue = latestAudit.revenue.coerceAtLeast(0.0)
    val fireCost = latestAudit.fireCost.coerceAtLeast(0.0)
    val targetRatio = if (latestAudit.targetRatio > 0.0) latestAudit.targetRatio else 1.0
    val hedefOndalik = targetRatio / 100.0
    val gerekliMinCiro = if (fireCost <= 0.0 || hedefOndalik <= 0.0) 0.0 else fireCost / hedefOndalik
    val maksFire = if (revenue <= 0.0 || hedefOndalik <= 0.0) 0.0 else revenue * hedefOndalik
    val currentRatio = if (revenue > 0.0) (fireCost / revenue) * 100.0 else 0.0
    val isOverTarget = (revenue > 0.0 && currentRatio > targetRatio) || (revenue <= 0.0 && fireCost > 0.0)

    val cardBg = if (isOverTarget) {
        ExpiredRedContainer.copy(alpha = 0.45f)
    } else {
        NormalGreenContainer.copy(alpha = 0.45f)
    }
    val cardBorder = if (isOverTarget) ExpiredRedBorder else NormalGreenBorder
    val accentColor = if (isOverTarget) ExpiredRedDark else NormalGreenDark
    val statusBg = if (isOverTarget) ExpiredRedContainer else NormalGreenContainer
    val statusIcon = if (isOverTarget) Icons.Default.WarningAmber else Icons.Default.CheckCircle

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("smart_analysis_card_${spec.key}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.2.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Üst Satır: Kategori Adı - Hedef + Oran Durumu (ikonlu)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = spec.icon,
                                contentDescription = spec.title,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "${spec.title} - Hedef: %${ratioFormatter.format(targetRatio)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusBg,
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = if (isOverTarget) "Hedef Üstü" else "Hedef Altı",
                            tint = accentColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "%${ratioFormatter.format(currentRatio)}",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor
                        )
                    }
                }
            }

            // Analiz Kutusu: Gerekli Min. Ciro & Maks. İzin Verilen Fire
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(13.dp),
                color = Color.White.copy(alpha = 0.8f),
                border = BorderStroke(0.8.dp, cardBorder.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = TurquoiseDark,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(16.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = "Gerekli Min. Ciro",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )
                            Text(
                                text = buildAnnotatedString {
                                    append("Hedefe ulaşmak için en az ")
                                    withStyle(SpanStyle(fontWeight = FontWeight.Black, color = Slate900)) {
                                        append("${moneyFormatter.format(gerekliMinCiro)} ₺")
                                    }
                                    append(" ciro yapmalısın")
                                },
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate800,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    HorizontalDivider(
                        thickness = 0.8.dp,
                        color = cardBorder.copy(alpha = 0.5f)
                    )

                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = ExpiredRedDark,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(16.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = "Maks. İzin Verilen Fire",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )
                            Text(
                                text = buildAnnotatedString {
                                    append("Bu ciroyla en fazla ")
                                    withStyle(SpanStyle(fontWeight = FontWeight.Black, color = Slate900)) {
                                        append("${moneyFormatter.format(maksFire)} ₺")
                                    }
                                    append(" fire verebilirsin")
                                },
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate800,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TargetRatioEditDialog(
    title: String,
    subtitle: String,
    initialValue: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var inputText by remember(initialValue) {
        val formatted = if (initialValue % 1.0 == 0.0) {
            initialValue.toLong().toString()
        } else {
            initialValue.toString()
        }
        mutableStateOf(formatted)
    }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate900
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = subtitle,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate700
                )
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { raw ->
                        inputText = raw.filter { it.isDigit() || it == '.' || it == ',' }
                        showError = false
                    },
                    label = {
                        Text(
                            text = "Hedef Oran (%)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    placeholder = {
                        Text(text = "Örn: 2.0", fontSize = 13.sp, color = Slate500)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TurquoisePrimary,
                        unfocusedBorderColor = Slate200,
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("target_ratio_dialog_input")
                )
                if (showError) {
                    Text(
                        text = "Lütfen 0'dan büyük geçerli bir oran girin.",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ExpiredRedDark
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = inputText.replace(',', '.').trim().toDoubleOrNull()
                    if (parsed == null || parsed <= 0.0) {
                        showError = true
                    } else {
                        onSave(parsed)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TurquoiseDark),
                modifier = Modifier.testTag("target_ratio_dialog_save_button")
            ) {
                Text(
                    text = "Kaydet",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "İptal",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate700
                )
            }
        }
    )
}


