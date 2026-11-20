package com.example.ui.screens.scanner

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.ui.components.CustomBoxedCalendarDialog
import com.example.ui.screens.DateOcrScannerDialog
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.CriticalOrangeContainer
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.ExpiredRedContainer
import com.example.ui.theme.NormalGreen
import com.example.ui.theme.NormalGreenContainer
import com.example.ui.theme.Slate700
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProductDetailPreviewCard(
    product: Product,
    matchingProducts: List<Product> = listOf(product),
    initialSktMillis: Long? = null,
    todayMidnight: Long = com.example.util.getTodayMidnight(),
    onAddSkt: (Product, Long, Int) -> Unit = { _, _, _ -> },
    onDeductStock: (Product, Int, String) -> Unit = { _, _, _ -> },
    onSelectProduct: () -> Unit,
    onEditProduct: ((Product) -> Unit)? = null,
    onStartEditing: () -> Unit = {},
    onClearDetail: () -> Unit = {}
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr-TR")) }

    // Only products with a valid SKT date and stock > 0, sorted by SKT date (FIFO)
    val validSktProducts = remember(matchingProducts) {
        matchingProducts.filter { it.sktTarihi > 0L && it.stokAdedi > 0 }.sortedBy { it.sktTarihi }
    }

    val effectiveProduct = remember(product, validSktProducts) {
        validSktProducts.firstOrNull() ?: product
    }

    val totalStock = remember(matchingProducts) {
        matchingProducts.sumOf { it.stokAdedi.coerceAtLeast(0) }
    }

    var selectedSktMillis by remember(product.barkod, initialSktMillis) {
        mutableStateOf(initialSktMillis ?: (if (product.sktTarihi > 0L) product.sktTarihi else System.currentTimeMillis()))
    }
    var sktAdediStr by remember(product.barkod) { mutableStateOf("1") }
    var isOcrScannerOpen by remember { mutableStateOf(false) }
    var isDatePickerOpen by remember { mutableStateOf(false) }
    var selectedRiskFilter by remember(product.barkod) { mutableStateOf<String?>(null) }

    if (isDatePickerOpen) {
        CustomBoxedCalendarDialog(
            initialDateMillis = selectedSktMillis,
            onDismissRequest = { isDatePickerOpen = false },
            onDateSelected = { selectedMillis ->
                onStartEditing()
                selectedSktMillis = selectedMillis
            }
        )
    }

    // Risk Level Breakdown calculated only on valid SKT products
    val expiredOrNearCount = remember(validSktProducts, todayMidnight) {
        validSktProducts.filter { it.getRemainingDays(todayMidnight) <= 3 }.sumOf { it.stokAdedi }
    }
    val criticalCount = remember(validSktProducts, todayMidnight) {
        validSktProducts.filter { it.getRemainingDays(todayMidnight) in 4..15 }.sumOf { it.stokAdedi }
    }
    val safeCount = remember(validSktProducts, todayMidnight) {
        validSktProducts.filter { it.getRemainingDays(todayMidnight) >= 16 }.sumOf { it.stokAdedi }
    }
    val hasAnyStockBreakdown = (expiredOrNearCount + criticalCount + safeCount) > 0

    val filteredValidSktProducts = remember(validSktProducts, selectedRiskFilter, todayMidnight) {
        val filtered = when (selectedRiskFilter) {
            "EXPIRED" -> validSktProducts.filter { it.getRemainingDays(todayMidnight) <= 3 }
            "CRITICAL" -> validSktProducts.filter { it.getRemainingDays(todayMidnight) in 4..15 }
            "SAFE" -> validSktProducts.filter { it.getRemainingDays(todayMidnight) >= 16 }
            else -> validSktProducts
        }
        filtered.sortedBy { it.sktTarihi }
    }

    if (isOcrScannerOpen) {
        DateOcrScannerDialog(
            onDismiss = { isOcrScannerOpen = false },
            onDateDetected = { dateMillis, _ ->
                selectedSktMillis = dateMillis
                isOcrScannerOpen = false
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top info: Product Name + Optional Edit + Top-right 'X' Close Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = product.urunAdi,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val codePart = if (product.urunKodu.isNotBlank()) "Kod: ${product.urunKodu}" else "Barkod: ${product.barkod}"
                        Text(
                            text = "$codePart • Toplam Stok: $totalStock Adet",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        product.getFormattedPrice()?.let { formattedPrice ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = TurquoisePrimary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = formattedPrice,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TurquoiseDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (onEditProduct != null) {
                        IconButton(
                            onClick = { onEditProduct(effectiveProduct) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Ürünü Düzenle",
                                tint = TurquoisePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onClearDetail,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("close_preview_detail_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Compact single-row stock risk badge (hidden completely when all values are 0)
            if (hasAnyStockBreakdown) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isExpiredSelected = selectedRiskFilter == "EXPIRED"
                        val isCriticalSelected = selectedRiskFilter == "CRITICAL"
                        val isSafeSelected = selectedRiskFilter == "SAFE"

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isExpiredSelected) ExpiredRedContainer else ExpiredRedContainer.copy(alpha = 0.7f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedRiskFilter = if (selectedRiskFilter == "EXPIRED") null else "EXPIRED"
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isExpiredSelected) "🚨✓ $expiredOrNearCount ad." else "🚨 $expiredOrNearCount ad.",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpiredRed,
                                    maxLines = 1
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isCriticalSelected) CriticalOrangeContainer else CriticalOrangeContainer.copy(alpha = 0.7f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedRiskFilter = if (selectedRiskFilter == "CRITICAL") null else "CRITICAL"
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isCriticalSelected) "⚠️✓ $criticalCount ad." else "⚠️ $criticalCount ad.",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CriticalOrange,
                                    maxLines = 1
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSafeSelected) NormalGreenContainer else NormalGreenContainer.copy(alpha = 0.7f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedRiskFilter = if (selectedRiskFilter == "SAFE") null else "SAFE"
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isSafeSelected) "✅✓ $safeCount ad." else "✅ $safeCount ad.",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NormalGreen,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Registered SKT list (Horizontal Row)
            if (filteredValidSktProducts.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedRiskFilter != null) "Filtreli (${filteredValidSktProducts.size}):" else "Partiler (${validSktProducts.size}):",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TurquoiseDark
                    )
                    filteredValidSktProducts.forEach { item ->
                        val dateStr = dateFormat.format(Date(item.sktTarihi))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "$dateStr (${item.stokAdedi} ad.)",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (selectedRiskFilter != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TurquoisePrimary.copy(alpha = 0.15f),
                            modifier = Modifier.clickable { selectedRiskFilter = null }
                        ) {
                            Text(
                                text = "Tümü ✕",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TurquoiseDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Quick deduct bar (only shown when valid SKT stock exists)
            if (validSktProducts.isNotEmpty()) {
                val targetBatch = effectiveProduct
                val canDeduct = targetBatch.stokAdedi > 0
                val batchDateStr = if (targetBatch.sktTarihi > 0L) dateFormat.format(Date(targetBatch.sktTarihi)) else "SKT Girilmedi"

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "HIZLI İŞLEM ($batchDateStr)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )
                            Text(
                                text = "Mevcut: ${targetBatch.stokAdedi} Adet",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (canDeduct) TurquoiseDark else ExpiredRed
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = {
                                    if (canDeduct) {
                                        onDeductStock(targetBatch, 1, "SATILDI")
                                        Toast.makeText(context, "✅ 1 adet Satıldı olarak düşüldü ($batchDateStr)", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = canDeduct,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF16A34A),
                                    disabledContainerColor = Color(0xFFCBD5E1)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = "Satıldı Düş",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "-1 Satıldı",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Button(
                                onClick = {
                                    if (canDeduct) {
                                        onDeductStock(targetBatch, 1, "FİRE")
                                        Toast.makeText(context, "🗑️ 1 adet Fire olarak düşüldü ($batchDateStr)", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = canDeduct,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFDC2626),
                                    disabledContainerColor = Color(0xFFCBD5E1)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Fire Düş",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "-1 Fire",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Compact Date + Quantity Row & Single Primary Save Button
            ProductDetailSktInputSection(
                product = product,
                selectedSktMillis = selectedSktMillis,
                sktAdediStr = sktAdediStr,
                dateFormat = dateFormat,
                onDateClick = {
                    onStartEditing()
                    isDatePickerOpen = true
                },
                onSktAdediChange = {
                    onStartEditing()
                    sktAdediStr = it
                },
                onStartEditing = onStartEditing,
                onAddSkt = onAddSkt,
                onResetForm = {
                    selectedSktMillis = System.currentTimeMillis()
                    sktAdediStr = "1"
                }
            )
        }
    }
}
