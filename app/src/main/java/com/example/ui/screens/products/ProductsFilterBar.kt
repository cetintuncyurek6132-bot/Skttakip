package com.example.ui.screens.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.QrCodeScanner
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
fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) TurquoisePrimary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.testTag("filter_chip_$label")
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
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
    onOpenQrFixMode: () -> Unit,
    onFilterSelect: (ProductFilter) -> Unit,
    onGroupFilterSelect: (ProductGroupFilter) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            // Horizontal filter chips & Date Range button
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 📅 Tarih Aralığı Filtresi Butonu
                item {
                    Surface(
                        onClick = onOpenDateRange,
                        shape = RoundedCornerShape(12.dp),
                        color = if (hasDateFilter) TurquoisePrimary else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (hasDateFilter) TurquoiseDark else Color.Transparent),
                        modifier = Modifier.testTag("date_range_filter_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Tarih Filtresi",
                                tint = if (hasDateFilter) Color.White else TurquoisePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (hasDateFilter) {
                                    val startStr = startDateFilter?.let { dateFormat.format(Date(it)) } ?: "..."
                                    val endStr = endDateFilter?.let { dateFormat.format(Date(it)) } ?: "..."
                                    "$startStr - $endStr"
                                } else "SKT Tarih Aralığı",
                                color = if (hasDateFilter) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (hasDateFilter) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Filtreyi Temizle",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onClearDateRange() }
                                )
                            }
                        }
                    }
                }

                // 🏷️ QR ile Barkod & Fiyat Düzelt Butonu
                item {
                    Surface(
                        onClick = onOpenQrFixMode,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, TurquoisePrimary)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "QR Düzelt",
                                tint = TurquoisePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "🏷️ QR Düzelt",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Kategori/Durum Filtre Chip'leri
                items(ProductFilter.values()) { filter ->
                    val chipLabel = if (filter == ProductFilter.ALL && totalProductCount > 0) {
                        "TÜMÜ ($totalProductCount)"
                    } else {
                        filter.label
                    }
                    FilterChipItem(
                        label = chipLabel,
                        isSelected = selectedFilter == filter,
                        onClick = { onFilterSelect(filter) }
                    )
                }
            }
        }
    }
}
