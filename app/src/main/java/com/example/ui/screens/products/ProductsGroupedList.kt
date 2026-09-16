package com.example.ui.screens.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.ui.ProductFilter
import com.example.ui.components.GroupedProductListItemCard
import com.example.ui.components.ProductListItemCard
import com.example.ui.theme.CriticalOrange

import java.util.Calendar

@Composable
fun ProductsGroupedList(
    products: List<Product>,
    selectedFilter: ProductFilter,
    onProductClick: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onQuickAddSkt: (Product) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val todayMidnight = remember {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    val groupedProducts = remember(products) {
        products.groupBy {
            if (it.barkod.isNotBlank()) it.barkod.trim().lowercase() else it.urunAdi.trim().lowercase()
        }.values.toList()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            if (selectedFilter == ProductFilter.IMPORTANT) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, CriticalOrange)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔥", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ÖNEMLİ: YÜKSEK STOK + YAKIN SKT (Kategori)",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = CriticalOrange
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Stoğu çok (≥10 adet) ve SKT'sine 1 aydan az kalmış (≤30 gün) ürünler. Son 2-3 güne kalmadan acil satış, indirim veya reyon ön sırasına alma gerektirir.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        items(
            items = groupedProducts,
            key = { group: List<Product> ->
                val first = group.first()
                if (first.barkod.isNotBlank()) "b_${first.barkod.trim().lowercase()}" else "p_${first.id}_${first.urunAdi.trim().lowercase()}"
            }
        ) { group: List<Product> ->
            if (group.size == 1) {
                val product = group.first()
                ProductListItemCard(
                    product = product,
                    todayMidnight = todayMidnight,
                    onClick = { onProductClick(product) },
                    onQuickAddSkt = { onQuickAddSkt(product) },
                    onDelete = { onDeleteProduct(product) }
                )
            } else {
                GroupedProductListItemCard(
                    productList = group,
                    todayMidnight = todayMidnight,
                    onClick = { product -> onProductClick(product) },
                    onQuickAddSkt = { product -> onQuickAddSkt(product) },
                    onDeleteProduct = { product -> onDeleteProduct(product) }
                )
            }
        }
    }
}
