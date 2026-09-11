package com.example.ui.screens.notification

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.ui.DashboardState
import com.example.ui.ProductFilter
import com.example.ui.screens.NotificationCategoryFilter
import com.example.ui.theme.*

fun LazyListScope.notificationCardsList(
    selectedCategory: NotificationCategoryFilter,
    overdue2DaysProducts: List<Product>,
    expiredProducts: List<Product>,
    criticalProducts: List<Product>,
    highStockNearExpiry: List<Product>,
    dashboardState: DashboardState,
    onShowBulkRemoveDialog: (List<Product>) -> Unit,
    onRemoveFromShelf: (Product) -> Unit,
    onAddToAdetsel: (Product) -> Unit,
    onOpenProductDetail: (Product) -> Unit,
    onFilterSelected: (ProductFilter) -> Unit,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // 1. OVERDUE 2+ DAYS ALERT (SKT +2 GÜN GEÇTİ)
    if ((selectedCategory == NotificationCategoryFilter.ALL || selectedCategory == NotificationCategoryFilter.CRITICAL_SKT) && overdue2DaysProducts.isNotEmpty()) {
        item {
            val context = LocalContext.current
            val totalOverdueStock = overdue2DaysProducts.sumOf { it.stokAdedi }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                border = BorderStroke(1.2.dp, ExpiredRed)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(ExpiredRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PriorityHigh,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🛑 ACİL: SKT +2 Gün Geçti!",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = ExpiredRedDark
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ExpiredRed
                        ) {
                            Text(
                                text = "${overdue2DaysProducts.size} ÜRÜN",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Toplam $totalOverdueStock adet ürünün SKT'si 2 günden fazla süredir geçmiş. Acilen raftan alıp imha/fire kaydı açılmalıdır.",
                        fontSize = 11.sp,
                        color = Slate900,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        overdue2DaysProducts.take(4).forEach { prod ->
                            ActionableProductNotificationRow(
                                product = prod,
                                subtitle = "${-prod.getRemainingDays()} gün geçti",
                                subtitleColor = ExpiredRedDark,
                                onRemove = {
                                    onRemoveFromShelf(prod)
                                    Toast.makeText(context, "${prod.urunAdi} raftan kaldırıldı", Toast.LENGTH_SHORT).show()
                                },
                                onAddToAdetsel = {
                                    onAddToAdetsel(prod)
                                    Toast.makeText(context, "${prod.urunAdi} sayıma eklendi", Toast.LENGTH_SHORT).show()
                                },
                                onDetail = {
                                    onDismiss()
                                    onOpenProductDetail(prod)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onShowBulkRemoveDialog(overdue2DaysProducts) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpiredRedDark),
                            border = BorderStroke(1.dp, ExpiredRedDark),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("⚡ Tümünü Raftan Kaldır", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                onDismiss()
                                onFilterSelected(ProductFilter.EXPIRED)
                                onNavigate("products")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ExpiredRed),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Tüm Listeyi Gör", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // 2. EXPIRED TODAY NOTIFICATION (SKT BUGÜN BİTTİ)
    if ((selectedCategory == NotificationCategoryFilter.ALL || selectedCategory == NotificationCategoryFilter.CRITICAL_SKT) && expiredProducts.isNotEmpty()) {
        item {
            val context = LocalContext.current
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                border = BorderStroke(1.dp, ExpiredRed.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = ExpiredRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🚨 SKT'si Dolan ${expiredProducts.size} Ürün Bulundu",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.5.sp,
                                color = ExpiredRed
                            )
                        }
                        Text(
                            text = "BUGÜN",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ExpiredRed.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Aşağıdaki ürünler müşteriye sunulamaz. Tek tıkla raftan kaldırabilir veya detayını inceleyebilirsiniz.",
                        fontSize = 11.sp,
                        color = Slate700,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        expiredProducts.take(3).forEach { prod ->
                            ActionableProductNotificationRow(
                                product = prod,
                                subtitle = "SKT Doldu",
                                subtitleColor = ExpiredRed,
                                onRemove = {
                                    onRemoveFromShelf(prod)
                                    Toast.makeText(context, "${prod.urunAdi} raftan kaldırıldı", Toast.LENGTH_SHORT).show()
                                },
                                onAddToAdetsel = {
                                    onAddToAdetsel(prod)
                                    Toast.makeText(context, "${prod.urunAdi} sayıma eklendi", Toast.LENGTH_SHORT).show()
                                },
                                onDetail = {
                                    onDismiss()
                                    onOpenProductDetail(prod)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                onDismiss()
                                onFilterSelected(ProductFilter.EXPIRED)
                                onNavigate("products")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ExpiredRed),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Ürünler Sayfasında Aç", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // 3. CRITICAL ITEMS NOTIFICATION (SKT ≤ 7 GÜN)
    if ((selectedCategory == NotificationCategoryFilter.ALL || selectedCategory == NotificationCategoryFilter.CRITICAL_SKT) && criticalProducts.isNotEmpty()) {
        item {
            val context = LocalContext.current
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                border = BorderStroke(1.dp, CriticalOrange.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = CriticalOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "⚠️ KRİTİK: ${criticalProducts.size} Ürünün SKT'sine ≤ 7 Gün Kaldı",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.5.sp,
                                color = CriticalOrange
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ön sıraya alma (FIFO), %30-%50 etiket indirimi veya sayım kontrolü yapılması önerilir.",
                        fontSize = 11.sp,
                        color = Slate700,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        criticalProducts.take(3).forEach { prod ->
                            ActionableProductNotificationRow(
                                product = prod,
                                subtitle = "${prod.getRemainingDays()} gün kaldı",
                                subtitleColor = CriticalOrange,
                                onRemove = {
                                    onRemoveFromShelf(prod)
                                    Toast.makeText(context, "${prod.urunAdi} raftan kaldırıldı", Toast.LENGTH_SHORT).show()
                                },
                                onAddToAdetsel = {
                                    onAddToAdetsel(prod)
                                    Toast.makeText(context, "${prod.urunAdi} sayıma eklendi", Toast.LENGTH_SHORT).show()
                                },
                                onDetail = {
                                    onDismiss()
                                    onOpenProductDetail(prod)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                onDismiss()
                                onFilterSelected(ProductFilter.CRITICAL)
                                onNavigate("products")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CriticalOrange),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Kritik Ürünleri Listele", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // 4. HIGH STOCK NEAR EXPIRY NOTIFICATION (≥10 ADET & ≤30 GÜN)
    if ((selectedCategory == NotificationCategoryFilter.ALL || selectedCategory == NotificationCategoryFilter.STOCK_DISCOUNT) && highStockNearExpiry.isNotEmpty()) {
        item {
            val context = LocalContext.current
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF)),
                border = BorderStroke(1.dp, Color(0xFFC7D2FE))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔥 YÜKSEK ADET & SKT RİSKİ (${highStockNearExpiry.size} Kalem)",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = Color(0xFF4338CA)
                        )
                        Text(
                            text = "BİLDİRİM",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4338CA)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Yüksek adette (≥10 Adet) ve 30 günden az süresi kalan ürünler için indirim veya sepet alanı oluşturulabilir.",
                        fontSize = 11.sp,
                        color = Slate700,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        highStockNearExpiry.take(3).forEach { prod ->
                            ActionableProductNotificationRow(
                                product = prod,
                                subtitle = "${prod.getRemainingDays()} gün | ${prod.stokAdedi} Adet",
                                subtitleColor = Color(0xFF4338CA),
                                onRemove = {
                                    onRemoveFromShelf(prod)
                                    Toast.makeText(context, "${prod.urunAdi} raftan kaldırıldı", Toast.LENGTH_SHORT).show()
                                },
                                onAddToAdetsel = {
                                    onAddToAdetsel(prod)
                                    Toast.makeText(context, "${prod.urunAdi} sayıma eklendi", Toast.LENGTH_SHORT).show()
                                },
                                onDetail = {
                                    onDismiss()
                                    onOpenProductDetail(prod)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                onDismiss()
                                onFilterSelected(ProductFilter.IMPORTANT)
                                onNavigate("products")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4338CA)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Riskli Adetleri Filtrele", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // 5. SYSTEM BACKUP & ROOM DB INFO
    if (selectedCategory == NotificationCategoryFilter.ALL || selectedCategory == NotificationCategoryFilter.SYSTEM_TASKS) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Slate50),
                border = BorderStroke(1.dp, Slate200)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = Slate700,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "💾 Çevrimdışı Room DB Aktif",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Toplam ${dashboardState.totalCount} ürün yerel bellekte güvenle saklanıyor.",
                            fontSize = 10.5.sp,
                            color = Slate500
                        )
                    }
                }
            }
        }
    }
}
