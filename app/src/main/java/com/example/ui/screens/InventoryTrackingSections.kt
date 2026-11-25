package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.draw.rotate
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
import com.example.ui.components.AutoSizeMetricText
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
    continuousTargetRatio: Double = 5.0,
    onEditCombinedTargetClick: () -> Unit = {},
    moneyFormatter: DecimalFormat,
    ratioFormatter: DecimalFormat,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "combined_card_chevron_rotation"
    )

    val hasCombinedRatio = summary.hasData && summary.combinedRatio != null
    val isCombinedOverTarget = hasCombinedRatio && (summary.combinedRatio ?: 0.0) > combinedTargetRatio
    val isContinuousOverTarget = summary.continuousRatio != null && summary.continuousRatio > continuousTargetRatio
    val isSystemRisky = isCombinedOverTarget

    val cardBg = Color.White
    val cardBorder = Slate200

    val totalAccentColor = when {
        !hasCombinedRatio -> TurquoiseDark
        isCombinedOverTarget -> ExpiredRedDark
        else -> NormalGreenDark
    }
    val totalBoxBorder = when {
        !hasCombinedRatio -> TurquoisePrimary.copy(alpha = 0.35f)
        isCombinedOverTarget -> ExpiredRedBorder.copy(alpha = 0.7f)
        else -> NormalGreenBorder.copy(alpha = 0.7f)
    }

    val statusChipText = when {
        !hasCombinedRatio -> "Veri Bekleniyor"
        isSystemRisky -> "Durum: Riskli"
        else -> "Durum: İyi"
    }
    val statusChipBg = when {
        !hasCombinedRatio -> TurquoisePrimary.copy(alpha = 0.12f)
        isSystemRisky -> ExpiredRedContainer
        else -> NormalGreenContainer
    }
    val statusChipBorder = when {
        !hasCombinedRatio -> TurquoisePrimary.copy(alpha = 0.3f)
        isSystemRisky -> ExpiredRedBorder
        else -> NormalGreenBorder
    }
    val statusChipTextColor = when {
        !hasCombinedRatio -> TurquoiseDark
        isSystemRisky -> ExpiredRedDark
        else -> NormalGreenDark
    }

    val formattedTotalRatio = if (summary.combinedRatio != null) {
        "%${ratioFormatter.format(summary.combinedRatio)}"
    } else {
        "%-"
    }

    Surface(
        onClick = { isExpanded = !isExpanded },
        modifier = modifier
            .fillMaxWidth()
            .testTag("combined_store_inventory_card"),
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 12.dp)
        ) {
            // Başlık + Sağ Üst Durum Rozeti + Aşağı/Yukarı Ok (Chevron)
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
                        color = Slate50,
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = TurquoiseDark,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Birleşik Envanter",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .testTag("combined_store_title")
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusChipBg,
                        border = BorderStroke(0.8.dp, statusChipBorder),
                        modifier = Modifier.testTag("combined_store_status_badge")
                    ) {
                        Text(
                            text = statusChipText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusChipTextColor,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Daralt" else "Genişlet",
                        tint = Slate700,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(chevronRotation)
                    )
                }
            }

            // Kapalı Durum Özeti: Büyük "Toplam Oran" (%X)
            AnimatedVisibility(
                visible = !isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Toplam Oran",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AutoSizeMetricText(
                        text = formattedTotalRatio,
                        maxFontSize = 20.sp,
                        minFontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = if (hasCombinedRatio) totalAccentColor else Slate900
                    )
                }
            }

            // Açılır İçerik: T, P, S Detay Kutuları + Hedefler + Toplam Ciro/Fire
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 3 Sütun: T (Toplam), P (Parçalı), S (Sürekli)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        // T (Toplam)
                        Surface(
                            modifier = Modifier
                                .weight(1.22f)
                                .fillMaxHeight()
                                .testTag("combined_metric_total"),
                            shape = RoundedCornerShape(13.dp),
                            color = Slate50,
                            border = BorderStroke(1.1.dp, totalBoxBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    MetricLetterBadge(
                                        letter = "T",
                                        bgColor = totalAccentColor,
                                        textColor = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Toplam",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Slate800,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                AutoSizeMetricText(
                                    text = formattedTotalRatio,
                                    maxFontSize = 21.sp,
                                    minFontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (hasCombinedRatio) totalAccentColor else Slate900,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Surface(
                                    onClick = onEditCombinedTargetClick,
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = BorderStroke(0.8.dp, Slate200),
                                    modifier = Modifier.testTag("edit_combined_target_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "Hedef: %${ratioFormatter.format(combinedTargetRatio)}",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Slate800,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Genel Hedefi Düzenle",
                                            tint = TurquoiseDark,
                                            modifier = Modifier.size(10.dp)
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
                            color = Slate50,
                            border = BorderStroke(1.dp, Slate200)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    MetricLetterBadge(
                                        letter = "P",
                                        bgColor = TurquoisePrimary.copy(alpha = 0.16f),
                                        textColor = TurquoiseDark
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Parçalı",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate700,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                AutoSizeMetricText(
                                    text = if (summary.periodicRatio != null) {
                                        "%${ratioFormatter.format(summary.periodicRatio)}"
                                    } else {
                                        "%-"
                                    },
                                    maxFontSize = 21.sp,
                                    minFontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Slate900,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
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
                            color = Slate50,
                            border = BorderStroke(1.dp, Slate200)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    MetricLetterBadge(
                                        letter = "S",
                                        bgColor = Color(0xFFDBEAFE),
                                        textColor = Color(0xFF1D4ED8)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Sürekli",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate700,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                AutoSizeMetricText(
                                    text = if (summary.continuousRatio != null) {
                                        "%${ratioFormatter.format(summary.continuousRatio)}"
                                    } else {
                                        "%-"
                                    },
                                    maxFontSize = 21.sp,
                                    minFontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isContinuousOverTarget) ExpiredRedDark else Slate900,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Akıllı Yönlendirme Uyarı Kutusu (Genel Hedef Aşıldığında)
                    if (isCombinedOverTarget) {
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
                            color = ExpiredRedContainer.copy(alpha = 0.65f),
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
                    HorizontalDivider(thickness = 0.8.dp, color = Slate200.copy(alpha = 0.8f))
                    AutoSizeMetricText(
                        text = "Toplam Ciro: ${moneyFormatter.format(summary.totalRevenue)} ₺ | Toplam Fire: ${moneyFormatter.format(summary.totalFireCost)} ₺",
                        maxFontSize = 11.5.sp,
                        minFontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
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
    val tabTitles = remember { listOf("Parçalı Envanter", "Sürekli Envanter") }

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
            contentColor = TurquoisePrimary,
            divider = {},
            indicator = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            tabTitles.forEachIndexed { index, title ->
                val isSelected = selectedTabIndex == index
                val animatedBgColor by animateColorAsState(
                    targetValue = if (isSelected) TurquoisePrimary else Color.Transparent,
                    animationSpec = tween(durationMillis = 250),
                    label = "tab_bg_$index"
                )
                val animatedTextColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Slate700,
                    animationSpec = tween(durationMillis = 250),
                    label = "tab_text_$index"
                )
                Tab(
                    selected = isSelected,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(animatedBgColor)
                        .testTag("inventory_tracking_tab_$index")
                ) {
                    Text(
                        text = title,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                        color = animatedTextColor,
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
    moneyFormatter: DecimalFormat,
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

            val effectivePeriodicTarget = if ((latestAudit?.targetRatio ?: 0.0) > 0.0) {
                latestAudit!!.targetRatio
            } else {
                1.0
            }
            val formattedPeriodicTarget = remember(effectivePeriodicTarget, ratioFormatter) {
                DecimalFormat("0.00", ratioFormatter.decimalFormatSymbols).format(effectivePeriodicTarget)
            }
            val isPeriodicRatioOverTarget = latestAudit != null && (
                (latestAudit.revenue > 0.0 && (currentRatioVal ?: 0.0) > effectivePeriodicTarget) ||
                    (latestAudit.revenue <= 0.0 && latestAudit.fireCost > 0.0)
                )
            val periodicRatioTextColor = when {
                latestAudit == null -> Slate500
                isPeriodicRatioOverTarget -> ExpiredRedDark
                else -> NormalGreenDark
            }
            val currentCiroText = if (latestAudit != null) {
                "${moneyFormatter.format(latestAudit.revenue)} ₺"
            } else {
                "- ₺"
            }
            val currentFireText = if (latestAudit != null) {
                "${moneyFormatter.format(latestAudit.fireCost)} ₺"
            } else {
                "- ₺"
            }

            Card(
                onClick = { onCategoryClick(spec) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("inventory_card_${spec.key}"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Slate200),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Slate50,
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = spec.icon,
                                contentDescription = spec.title,
                                tint = spec.iconTint,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Text(
                        text = spec.title,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AutoSizeMetricText(
                            text = ratioPercentText,
                            maxFontSize = 18.sp,
                            minFontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            color = periodicRatioTextColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (currentRatioVal != null && prevRatioVal != null) {
                            val diff = currentRatioVal - prevRatioVal
                            if (abs(diff) > 0.0001) {
                                val isDown = diff < 0.0
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = if (isDown) "↓" else "↑",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDown) NormalGreenDark else ExpiredRedDark,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = TurquoisePrimary.copy(alpha = 0.10f),
                        border = BorderStroke(0.8.dp, TurquoisePrimary.copy(alpha = 0.30f))
                    ) {
                        AutoSizeMetricText(
                            text = "Hedef: %$formattedPeriodicTarget",
                            maxFontSize = 9.5.sp,
                            minFontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = TurquoiseDark,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Mevcut Ciro",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate500,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            AutoSizeMetricText(
                                text = currentCiroText,
                                maxFontSize = 10.5.sp,
                                minFontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate800,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Mevcut Fire",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate500,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            AutoSizeMetricText(
                                text = currentFireText,
                                maxFontSize = 10.5.sp,
                                minFontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate800,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    AutoSizeMetricText(
                        text = elapsedText,
                        maxFontSize = 10.sp,
                        minFontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate50,
                        border = BorderStroke(0.8.dp, statusBorderColor.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AutoSizeMetricText(
                            text = remainingLabel,
                            maxFontSize = 10.sp,
                            minFontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 5.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatContinuousPeriodBadgeLabel(rawPeriod: String?, hasAudit: Boolean): String {
    if (!hasAudit) return "Kayıt Yok"
    val trimmed = rawPeriod.orEmpty().trim()
    if (trimmed.isEmpty()) return "Dönem Yok"

    val trLocale = java.util.Locale.forLanguageTag("tr-TR")
    val turkishMonths = listOf(
        "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
        "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
    )
    // "11-2026" veya "11/2026" formatı destekle
    val numericMatch = Regex("^(\\d{1,2})[./-](\\d{4})$").matchEntire(trimmed)
    if (numericMatch != null) {
        val m = numericMatch.groupValues[1].toIntOrNull()
        if (m != null && m in 1..12) {
            return turkishMonths[m - 1]
        }
    }

    val firstWord = trimmed.split(Regex("\\s+")).firstOrNull().orEmpty()
    val lowerWord = firstWord.lowercase(trLocale)
    val matchedMonth = turkishMonths.firstOrNull { it.lowercase(trLocale) == lowerWord }
    if (matchedMonth != null) {
        return matchedMonth
    }
    return lowerWord.replaceFirstChar { ch ->
        if (ch.isLowerCase()) ch.titlecase(trLocale) else ch.toString()
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
        horizontalArrangement = Arrangement.spacedBy(5.dp)
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

            val periodBadgeLabel = remember(latestAudit) {
                formatContinuousPeriodBadgeLabel(
                    rawPeriod = latestAudit?.auditPeriod,
                    hasAudit = latestAudit != null
                )
            }

            val ratioColor = when {
                latestAudit == null -> Slate500
                isOverTarget -> ExpiredRedDark
                else -> NormalGreenDark
            }

            Card(
                onClick = { onCategoryClick(spec, latestAudit) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("continuous_inventory_card_${spec.key}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Slate200),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Üst: İkon + Kategori Başlığı
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Slate50,
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = spec.icon,
                                    contentDescription = spec.title,
                                    tint = spec.iconTint,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Text(
                            text = spec.title,
                            fontSize = 9.5.sp,
                            lineHeight = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900,
                            minLines = 2,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    // Orta: Oran (%X) — Sadece hedef aşıldıysa kırmızı
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AutoSizeMetricText(
                            text = ratioPercentText,
                            maxFontSize = 15.sp,
                            minFontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            color = ratioColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (currentRatioVal != null && prevRatioVal != null) {
                            val diff = currentRatioVal - prevRatioVal
                            if (abs(diff) > 0.0001) {
                                val isDown = diff < 0.0
                                Spacer(modifier = Modifier.width(1.dp))
                                Text(
                                    text = if (isDown) "↓" else "↑",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDown) NormalGreenDark else ExpiredRedDark,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    // Alt Orta: Ciro ve Fire değerleri otomatik boyutlanarak kutuya tam sığar
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Ciro",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate500,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false
                            )
                            AutoSizeMetricText(
                                text = revenueText,
                                maxFontSize = 9.5.sp,
                                minFontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Fire",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate500,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false
                            )
                            AutoSizeMetricText(
                                text = fireText,
                                maxFontSize = 9.5.sp,
                                minFontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate800,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // En Alt: Standart Dönem Rozeti (örn. "Kasım")
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate50,
                        border = BorderStroke(0.8.dp, Slate200),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            AutoSizeMetricText(
                                text = periodBadgeLabel,
                                maxFontSize = 9.sp,
                                minFontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 3.dp)
                            )
                        }
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

    val cardBg = Color.White
    val cardBorder = Slate200
    val accentColor = when {
        generalRatio == null -> TurquoiseDark
        isOverTarget -> ExpiredRedDark
        else -> NormalGreenDark
    }
    val ratioBoxBg = Slate50
    val ratioBoxBorder = when {
        generalRatio == null -> Slate200
        isOverTarget -> ExpiredRedBorder.copy(alpha = 0.7f)
        else -> NormalGreenBorder.copy(alpha = 0.7f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("continuous_inventory_total_card"),
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 1.dp
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

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = Color.White,
                    border = BorderStroke(0.9.dp, cardBorder)
                ) {
                    Text(
                        text = "Hedef: %${ratioFormatter.format(continuousTargetRatio)} • ${latestAudits.size}/4",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
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
                        .weight(1.3f)
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
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Toplam Ciro",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            AutoSizeMetricText(
                                text = "${moneyFormatter.format(totalRevenue)} ₺",
                                maxFontSize = 13.sp,
                                minFontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate900,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
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
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = ExpiredRedDark,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Toplam Fire",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            AutoSizeMetricText(
                                text = "${moneyFormatter.format(totalFire)} ₺",
                                maxFontSize = 13.sp,
                                minFontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ExpiredRedDark,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Sağ Sütun: Genel Oran (%X) - Büyük ve tok vurgu
                Surface(
                    modifier = Modifier
                        .weight(0.85f)
                        .fillMaxHeight()
                        .testTag("continuous_total_general_ratio"),
                    shape = RoundedCornerShape(15.dp),
                    color = ratioBoxBg,
                    border = BorderStroke(1.1.dp, ratioBoxBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Genel Oran",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate700,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AutoSizeMetricText(
                            text = if (generalRatio != null) {
                                "%${ratioFormatter.format(generalRatio)}"
                            } else {
                                "%-"
                            },
                            maxFontSize = 22.sp,
                            minFontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeBg,
                    border = BorderStroke(1.dp, badgeBorder)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = badgeText,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
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
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        AutoSizeMetricText(
                            text = "Bu Hafta: Oran ${if (thisWeekRatio != null) "%${ratioFormatter.format(thisWeekRatio)}" else "%-"}",
                            maxFontSize = 12.5.sp,
                            minFontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Toplam Fire / Toplam Ciro",
                            fontSize = 10.sp,
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
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        AutoSizeMetricText(
                            text = "Geçen Hafta: Oran ${if (lastWeekRatio != null) "%${ratioFormatter.format(lastWeekRatio)}" else "%-"}",
                            maxFontSize = 12.5.sp,
                            minFontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate800,
                            modifier = Modifier.fillMaxWidth()
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
                            fontSize = 10.sp,
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

    val accentColor = if (isOverTarget) ExpiredRedDark else NormalGreenDark
    val statusBg = if (isOverTarget) {
        ExpiredRedContainer.copy(alpha = 0.65f)
    } else {
        NormalGreenContainer.copy(alpha = 0.65f)
    }
    val statusBorder = if (isOverTarget) ExpiredRedBorder else NormalGreenBorder
    val statusIcon = if (isOverTarget) Icons.Default.WarningAmber else Icons.Default.CheckCircle
    val targetRatioTwoDecimals = remember(targetRatio, ratioFormatter) {
        DecimalFormat("0.00", ratioFormatter.decimalFormatSymbols).format(targetRatio)
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("smart_analysis_card_${spec.key}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Üst Satır: Kurumsal Kategori Başlığı (Üstte KATEGORİ, Altta Hedef Oran) + Sağda Oran Rozeti
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        color = Slate50,
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier.size(36.dp)
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

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = spec.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Surface(
                                shape = RoundedCornerShape(7.dp),
                                color = TurquoisePrimary.copy(alpha = 0.12f),
                                border = BorderStroke(0.8.dp, TurquoisePrimary.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "Hedef: %$targetRatioTwoDecimals",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TurquoiseDark,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            AutoSizeMetricText(
                                text = "Mevcut Ciro: ${moneyFormatter.format(revenue)} ₺",
                                maxFontSize = 11.sp,
                                minFontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate700,
                                modifier = Modifier.fillMaxWidth()
                            )
                            AutoSizeMetricText(
                                text = "Mevcut Fire: ${moneyFormatter.format(fireCost)} ₺",
                                maxFontSize = 11.sp,
                                minFontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate700,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusBg,
                    border = BorderStroke(1.dp, statusBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = if (isOverTarget) "Hedef Üstü" else "Hedef Altı",
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "%${ratioFormatter.format(currentRatio)}",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Kurumsal 2 Sütunlu Rapor Tablosu: Gerekli Min. Ciro | Maks. İzin Verilen Fire
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(13.dp),
                color = Slate50,
                border = BorderStroke(0.8.dp, Slate200)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max)
                        .padding(vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sol Sütun: Gerekli Min. Ciro + Şu Anki Ciro
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = TurquoiseDark,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Gerekli Min. Ciro",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate500,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        AutoSizeMetricText(
                            text = "${moneyFormatter.format(gerekliMinCiro)} ₺",
                            maxFontSize = 14.5.sp,
                            minFontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900,
                            modifier = Modifier.fillMaxWidth()
                        )
                        AutoSizeMetricText(
                            text = "Şu An: ${moneyFormatter.format(revenue)} ₺",
                            maxFontSize = 10.5.sp,
                            minFontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate500,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .padding(vertical = 2.dp)
                            .background(Slate200)
                    )

                    // Sağ Sütun: Maks. İzin Verilen Fire + Şu Anki Fire
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = ExpiredRedDark,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Maks. İzin Verilen Fire",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate500,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        AutoSizeMetricText(
                            text = "${moneyFormatter.format(maksFire)} ₺",
                            maxFontSize = 14.5.sp,
                            minFontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900,
                            modifier = Modifier.fillMaxWidth()
                        )
                        AutoSizeMetricText(
                            text = "Şu An: ${moneyFormatter.format(fireCost)} ₺",
                            maxFontSize = 10.5.sp,
                            minFontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate500,
                            modifier = Modifier.fillMaxWidth()
                        )
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


