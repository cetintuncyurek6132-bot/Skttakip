package com.example.ui.screens.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ProductFilter
import com.example.ui.ProductGroupFilter
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun FilterChipBadge(
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color,
    selectedContentColor: Color = Color.White,
    unselectedBorderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
    unselectedContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    icon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) selectedColor else unselectedContainerColor,
        border = if (selected) null else BorderStroke(1.dp, unselectedBorderColor),
        modifier = modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) selectedContentColor else MaterialTheme.colorScheme.onSurface
            )
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(4.dp))
                trailingIcon()
            }
        }
    }
}

@Composable
fun ProductsFilterBar(
    hasDateFilter: Boolean,
    startDateFilter: Long?,
    endDateFilter: Long?,
    selectedFilter: ProductFilter,
    selectedGroupFilter: ProductGroupFilter,
    dateFormat: SimpleDateFormat,
    totalProductCount: Int = 0,
    onOpenDateRange: () -> Unit,
    onClearDateRange: () -> Unit,
    onOpenQrFixMode: () -> Unit = {},
    onFilterSelect: (ProductFilter) -> Unit,
    onGroupFilterSelect: (ProductGroupFilter) -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. "Tarih Aralığı"
            item {
                val dateLabel = if (hasDateFilter) {
                    val startStr = startDateFilter?.let { dateFormat.format(Date(it)) } ?: "..."
                    val endStr = endDateFilter?.let { dateFormat.format(Date(it)) } ?: "..."
                    "$startStr - $endStr"
                } else {
                    "Tarih Aralığı"
                }

                FilterChipBadge(
                    selected = hasDateFilter,
                    onClick = onOpenDateRange,
                    selectedColor = TurquoisePrimary,
                    selectedContentColor = Color.White,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Tarih Aralığı",
                            tint = if (hasDateFilter) Color.White else TurquoisePrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    trailingIcon = if (hasDateFilter) {
                        {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Tarih Filtresini Temizle",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable { onClearDateRange() }
                            )
                        }
                    } else null,
                    label = dateLabel,
                    modifier = Modifier.testTag("date_range_filter_button")
                )
            }

            // 2. "Tümü (X)"
            item {
                val isAllSelected = selectedFilter == ProductFilter.ALL && !hasDateFilter
                val allLabel = if (totalProductCount > 0) "Tümü ($totalProductCount)" else "Tümü"

                FilterChipBadge(
                    selected = isAllSelected,
                    onClick = {
                        onFilterSelect(ProductFilter.ALL)
                        if (hasDateFilter) onClearDateRange()
                    },
                    selectedColor = TurquoisePrimary,
                    selectedContentColor = Color.White,
                    label = allLabel,
                    modifier = Modifier
                        .testTag("filter_chip_all")
                        .testTag("filter_chip_TÜMÜ")
                )
            }

            // 3. "Önemli"
            item {
                val isImportantSelected = selectedFilter == ProductFilter.IMPORTANT

                FilterChipBadge(
                    selected = isImportantSelected,
                    onClick = { onFilterSelect(ProductFilter.IMPORTANT) },
                    selectedColor = Color(0xFFD97706), // Kehribar / Sarı renk tonu
                    selectedContentColor = Color.White,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Önemli",
                            tint = if (isImportantSelected) Color.White else Color(0xFFD97706),
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    label = "Önemli",
                    modifier = Modifier
                        .testTag("filter_chip_important")
                        .testTag("filter_chip_ÖNEMLİ")
                )
            }

            // 4. "Acil (0-7 Gün)"
            item {
                val isLast2DaysSelected = selectedFilter == ProductFilter.LAST_2_DAYS

                FilterChipBadge(
                    selected = isLast2DaysSelected,
                    onClick = { onFilterSelect(ProductFilter.LAST_2_DAYS) },
                    selectedColor = Color(0xFFDC2626), // Kırmızı / Turuncu dikkat çekici container
                    selectedContentColor = Color.White,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Acil (0-7 Gün)",
                            tint = if (isLast2DaysSelected) Color.White else Color(0xFFDC2626),
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    label = "Acil (0-7 Gün)",
                    modifier = Modifier
                        .testTag("filter_chip_last_2_days")
                        .testTag("filter_chip_urgent")
                        .testTag("filter_chip_Acil (0-7 Gün)")
                        .testTag("filter_chip_Son 2 Gün")
                )
            }
        }
    }
}
