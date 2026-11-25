package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InventoryAuditEntity
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.ExpiredRedContainer
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoiseLight
import com.example.ui.theme.TurquoisePrimary
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

data class RevenueHistoryItem(
    val audit: InventoryAuditEntity,
    val categoryKey: String,
    val categoryLabel: String,
    val categoryColor: Color,
    val categoryBgColor: Color,
    val timestampMillis: Long,
    val formattedDate: String,
    val totalRevenue: Double,
    val dailySales: Double?
)

private fun formatModalRevenueTl(amount: Double): String {
    val safe = if (amount.isNaN() || amount.isInfinite() || amount < 0.0) 0.0 else amount
    val nf = NumberFormat.getIntegerInstance(Locale.forLanguageTag("tr-TR"))
    return "₺${nf.format(safe.roundToInt())}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllRevenueHistoryModal(
    items: List<RevenueHistoryItem>,
    showDailySales: Boolean,
    onDeleteRequest: (RevenueHistoryItem) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }

    val filteredItems = remember(items, selectedCategoryFilter) {
        if (selectedCategoryFilter == "ALL") {
            items
        } else {
            items.filter { it.categoryKey == selectedCategoryFilter }
        }
    }

    val filterChips = remember {
        listOf(
            "ALL" to "Tümü",
            "GIDA" to "Gıda",
            "DOLAP" to "Dolap",
            "GIDA DIŞI" to "Gıda Dışı"
        )
    }

    AppBottomSheetWrapper(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = Color.White
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .statusBarsPadding()
        ) {
        // Üst Başlık
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(TurquoiseLight.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = TurquoiseDark,
                        modifier = Modifier.size(21.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tüm Satış Geçmişi",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            fontSize = 19.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${filteredItems.size} kayıt • En yeniden en eskiye",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("all_revenue_history_close_button")
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Slate50)
                        .border(1.dp, Slate200, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = Slate600,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Kategori Filtreleri
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterChips.forEach { (key, label) ->
                val isSelected = selectedCategoryFilter == key
                Surface(
                    onClick = { selectedCategoryFilter = key },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) TurquoisePrimary else Slate50,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) TurquoisePrimary else Slate200
                    ),
                    modifier = Modifier.testTag("history_filter_$key")
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) Color.White else Slate700,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Slate100, thickness = 1.dp)

        // Kendi içinde kaydırılabilir (scrollable) liste
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Bu kategoride ciro kaydı bulunmuyor.",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate500
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("all_revenue_history_lazy_column"),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(
                    items = filteredItems,
                    key = { index, item ->
                        if (item.audit.id != 0) item.audit.id else "rev_${index}_${item.timestampMillis}"
                    }
                ) { _, item ->
                    val displayAmount = if (showDailySales && item.dailySales != null) {
                        item.dailySales
                    } else {
                        item.totalRevenue
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Slate200),
                        shadowElevation = 0.5.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("modal_revenue_history_item_${item.audit.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = item.categoryBgColor,
                                        shape = RoundedCornerShape(7.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(item.categoryColor)
                                            )
                                            Text(
                                                text = item.categoryLabel,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = item.categoryColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Text(
                                        text = item.formattedDate,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Slate600,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (item.audit.notes.isNotBlank()) {
                                    Text(
                                        text = item.audit.notes.trim(),
                                        fontSize = 11.sp,
                                        color = Slate500,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    AutoSizeMetricText(
                                        text = if (showDailySales && item.dailySales != null) {
                                            "+${formatModalRevenueTl(displayAmount)}"
                                        } else {
                                            formatModalRevenueTl(displayAmount)
                                        },
                                        maxFontSize = 14.5.sp,
                                        minFontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TurquoiseDark
                                    )
                                    if (showDailySales && item.dailySales != null) {
                                        Text(
                                            text = "Toplam: ${formatModalRevenueTl(item.totalRevenue)}",
                                            fontSize = 10.sp,
                                            color = Slate500,
                                            maxLines = 1
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onDeleteRequest(item) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("modal_delete_revenue_audit_${item.audit.id}")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(ExpiredRedContainer.copy(alpha = 0.6f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "Kaydı Sil",
                                            tint = ExpiredRed,
                                            modifier = Modifier.size(18.dp)
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
    }
}
