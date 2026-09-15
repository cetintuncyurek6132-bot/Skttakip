package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.Product
import com.example.ui.ProductFilter
import com.example.ui.ProductGroupFilter
import com.example.ui.screens.products.DateRangePickerModal
import com.example.ui.screens.products.FilterChipItem
import com.example.ui.screens.products.ProductsEmptyState
import com.example.ui.screens.products.ProductsFilterBar
import com.example.ui.screens.products.ProductsGroupedList
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import com.example.util.ProductImageGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProductsScreen(
    products: List<Product>,
    searchQuery: String,
    selectedFilter: ProductFilter,
    selectedGroupFilter: ProductGroupFilter = ProductGroupFilter.ALL,
    startDateFilter: Long?,
    endDateFilter: Long?,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelect: (ProductFilter) -> Unit,
    onGroupFilterSelect: (ProductGroupFilter) -> Unit = {},
    onDateRangeSelect: (start: Long?, end: Long?) -> Unit = { _, _ -> },
    onClearDateRange: () -> Unit = {},
    onProductClick: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onAddProductClick: () -> Unit,
    onQuickAddSkt: (Product) -> Unit = {},
    onOpenQrFixMode: () -> Unit = {}
) {
    val context = LocalContext.current
    var isDateRangeModalOpen by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr-TR")) }

    val hasDateFilter = startDateFilter != null || endDateFilter != null

    if (isDateRangeModalOpen) {
        DateRangePickerModal(
            initialStart = startDateFilter,
            initialEnd = endDateFilter,
            onDismiss = { isDateRangeModalOpen = false },
            onApply = { start, end ->
                onDateRangeSelect(start, end)
                isDateRangeModalOpen = false
            },
            onClear = {
                onClearDateRange()
                isDateRangeModalOpen = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Search and Filters bar
        ProductsFilterBar(
            hasDateFilter = hasDateFilter,
            startDateFilter = startDateFilter,
            endDateFilter = endDateFilter,
            selectedFilter = selectedFilter,
            selectedGroupFilter = selectedGroupFilter,
            dateFormat = dateFormat,
            totalProductCount = products.size,
            onOpenDateRange = { isDateRangeModalOpen = true },
            onClearDateRange = onClearDateRange,
            onOpenQrFixMode = onOpenQrFixMode,
            onFilterSelect = onFilterSelect,
            onGroupFilterSelect = onGroupFilterSelect
        )

        // Active Date Range info banner if set
        if (hasDateFilter) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                color = TurquoisePrimary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(text = "🔍", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        val sStr = startDateFilter?.let { dateFormat.format(Date(it)) } ?: "Herhangi"
                        val eStr = endDateFilter?.let { dateFormat.format(Date(it)) } ?: "Herhangi"
                        Text(
                            text = "SKT Tarih Aralığı Filtresi Aktif: $sStr - $eStr (${products.size} ürün)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TurquoiseDark
                        )
                    }
                    val clearShape = RoundedCornerShape(4.dp)
                    Text(
                        text = "Temizle",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Red,
                        modifier = Modifier
                            .clip(clearShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = Color.Red)
                            ) { onClearDateRange() }
                            .padding(4.dp)
                    )
                }
            }
        }

        // Products list or empty state
        if (products.isEmpty()) {
            ProductsEmptyState(
                hasDateFilter = hasDateFilter,
                onClearDateRange = onClearDateRange,
                onAddProductClick = onAddProductClick
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                ProductsGroupedList(
                    products = products,
                    selectedFilter = selectedFilter,
                    onProductClick = onProductClick,
                    onDeleteProduct = onDeleteProduct,
                    onQuickAddSkt = onQuickAddSkt
                )

                ExtendedFloatingActionButton(
                    onClick = {
                        if (products.isEmpty()) {
                            Toast.makeText(context, "Paylaşılacak ürün bulunamadı", Toast.LENGTH_SHORT).show()
                        } else {
                            ProductImageGenerator.shareProductsAsImage(
                                context = context,
                                filterLabel = selectedFilter.label,
                                searchQuery = searchQuery,
                                productList = products
                            )
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_whatsapp),
                            contentDescription = "WhatsApp Görsel Paylaş",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    text = {
                        Text(
                            text = "Görsel Paylaş",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    containerColor = Color(0xFF25D366),
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 16.dp)
                        .testTag("whatsapp_share_list_image_button")
                )
            }
        }
    }
}
