package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.ui.ProductFilter
import com.example.ui.components.GroupedProductListItemCard
import com.example.ui.components.ProductListItemCard
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoisePrimary
import com.example.util.ProductImageGenerator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ProductsScreen(
    products: List<Product>,
    searchQuery: String,
    selectedFilter: ProductFilter,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelect: (ProductFilter) -> Unit,
    onProductClick: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onAddProductClick: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
    ) {
            // Top Search and Filters bar
            Surface(
                color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    // Horizontal filter chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ProductFilter.values().forEach { filter ->
                            FilterChipItem(
                                label = filter.label,
                                isSelected = selectedFilter == filter,
                                onClick = { onFilterSelect(filter) }
                            )
                        }
                    }
                }
            }

            // Products list or empty state
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "📦",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Eşleşen Ürün Bulunamadı",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Arama kriterini değiştirebilir, aşağıdaki kamera butonuyla barkod tarayabilir veya elle ekleyebilirsiniz.",
                            fontSize = 13.sp,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.Button(
                            onClick = onAddProductClick,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("manual_add_product_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Manuel Ürün Ekle",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("MANUEL YENİ ÜRÜN EKLE", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                val groupedProducts = remember(products) {
                    products.groupBy {
                        if (it.barkod.isNotBlank()) it.barkod.trim().lowercase() else it.urunAdi.trim().lowercase()
                    }.values.toList()
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        if (selectedFilter == ProductFilter.IMPORTANT) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
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
                                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val totalUnique = groupedProducts.size
                            val totalStock = products.sumOf { it.stokAdedi }
                            Text(
                                text = if (totalUnique == products.size) "${products.size} ÜRÜN LİSTELENİYOR" else "$totalUnique ÇEŞİT ($totalStock ADET)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            if (products.isNotEmpty()) {
                                Surface(
                                    onClick = {
                                        ProductImageGenerator.shareProductsAsImage(
                                            context = context,
                                            filterLabel = selectedFilter.label,
                                            searchQuery = searchQuery,
                                            productList = products
                                        )
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF25D366),
                                    modifier = Modifier.testTag("whatsapp_share_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Görsel Paylaş",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "WhatsApp",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
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
                            if (first.barkod.isNotBlank()) first.barkod else "${first.urunAdi}_${first.id}"
                        }
                    ) { group: List<Product> ->
                        if (group.size == 1) {
                            val product = group.first()
                            ProductListItemCard(
                                product = product,
                                onClick = { onProductClick(product) },
                                onDelete = { onDeleteProduct(product) }
                            )
                        } else {
                            GroupedProductListItemCard(
                                productList = group,
                                onClick = { product -> onProductClick(product) },
                                onDeleteProduct = { product -> onDeleteProduct(product) }
                            )
                        }
                    }
                }
            }
        }
    }

@Composable
fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) TurquoisePrimary else androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.testTag("filter_chip_$label")
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
