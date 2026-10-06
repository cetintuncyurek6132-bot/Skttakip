package com.example.ui.screens.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
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
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            if (selectedFilter == ProductFilter.IMPORTANT) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⭐", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ÖNEMLİ ÜRÜNLER",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF92400E)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Yıldızla işaretlenmiş, öncelikli takip edilen ürünler listeleniyor.",
                                fontSize = 11.sp,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }
            } else if (selectedFilter == ProductFilter.LAST_2_DAYS) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFF87171))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⏰", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ACİL MÜDAHALE (0-7 GÜN - KRİTİK 1 HAFTA)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF991B1B)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "SKT'sine 0 ile 7 gün dahil kalan acil müdahale, indirim ve sarı etiket gerektiren ürünler.",
                                fontSize = 11.sp,
                                color = Color(0xFFB91C1C)
                            )
                        }
                    }
                }
            }
        }

        items(
            items = groupedProducts,
            key = { group -> 
                val first = group.firstOrNull()
                if (first != null && first.barkod.isNotBlank()) "group_${first.barkod}" 
                else "group_${first?.id ?: System.identityHashCode(group)}"
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
