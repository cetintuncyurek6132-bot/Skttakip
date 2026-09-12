package com.example.ui.screens.scanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.example.data.findMatchingProducts
import com.example.data.parseShelfQrPayload
import com.example.ui.theme.TurquoisePrimary

@Composable
fun ScannerProductDetailsSection(
    activeBarcode: String,
    products: List<Product>,
    selectedProductOverride: Product?,
    todayMidnight: Long = remember {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    },
    onSelectOverride: (Product) -> Unit,
    onAddSkt: (Product, Long, Int) -> Unit,
    onDeductStock: (Product, Int, String) -> Unit = { _, _, _ -> },
    onBarcodeDetected: (String) -> Unit,
    onClearDetail: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        val qrData = remember(activeBarcode) { parseShelfQrPayload(activeBarcode) }

        val matchingProducts = remember(activeBarcode, products) {
            products.findMatchingProducts(activeBarcode)
        }

        // Group matching products by unique barcode/product name to detect if query matched multiple distinct items
        val distinctProducts = remember(matchingProducts) {
            matchingProducts.distinctBy { if (it.barkod.isNotBlank()) it.barkod else it.urunKodu.ifBlank { it.urunAdi } }
        }

        val baseFoundProduct = selectedProductOverride ?: matchingProducts.firstOrNull()
        val foundProduct = remember(baseFoundProduct, qrData) {
            if (baseFoundProduct != null && qrData.price != null && qrData.price > 0.0) {
                baseFoundProduct.copy(fiyat = qrData.price)
            } else {
                baseFoundProduct
            }
        }

        // If search query matched multiple different products (e.g. searching "kola" or "sut"), show product selector chips
        if (distinctProducts.size > 1) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "🔎 ARAMA SONUCU BULUNAN ÜRÜNLER (${distinctProducts.size} Çeşit):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = TurquoisePrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        distinctProducts.forEach { item ->
                            val isSelected = (foundProduct?.barkod == item.barkod)
                            Surface(
                                onClick = { onSelectOverride(item) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) TurquoisePrimary else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isSelected) TurquoisePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.urunAdi,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        when {
            // 1) A matching product is found in the database
            foundProduct != null -> {
                val sameProductSktList = remember(foundProduct.barkod, matchingProducts) {
                    matchingProducts.filter { it.barkod == foundProduct.barkod }
                }
                val parsedActiveQr = remember(activeBarcode) { parseShelfQrPayload(activeBarcode) }
                ProductDetailPreviewCard(
                    product = foundProduct,
                    matchingProducts = if (sameProductSktList.isNotEmpty()) sameProductSktList else listOf(foundProduct),
                    initialSktMillis = parsedActiveQr.expiryDateMillis,
                    todayMidnight = todayMidnight,
                    onAddSkt = { prod, millis, count ->
                        onAddSkt(prod, millis, count)
                    },
                    onDeductStock = { prod, amount, reason ->
                        onDeductStock(prod, amount, reason)
                    },
                    onSelectProduct = {
                        onBarcodeDetected(foundProduct.barkod)
                    },
                    onClearDetail = onClearDetail
                )
            }

            // 2) A search term is entered/scanned, but product not found
            activeBarcode.isNotBlank() -> {
                val parsedActiveQr = remember(activeBarcode) { parseShelfQrPayload(activeBarcode) }
                ProductNotFoundPreviewCard(
                    barcode = activeBarcode,
                    onAddProduct = {
                        val cleanBarcode = parsedActiveQr.barcode.ifBlank { activeBarcode }
                        onBarcodeDetected(cleanBarcode)
                    },
                    onClearDetail = onClearDetail
                )
            }

            // 3) No barcode selected/scanned yet
            else -> {
                EmptyScannerGuidanceCard()
            }
        }
    }
}
