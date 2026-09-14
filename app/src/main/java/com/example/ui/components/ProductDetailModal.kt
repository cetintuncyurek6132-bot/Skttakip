package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Product
import com.example.data.getDisplayName
import com.example.ui.components.detail.DeductStockDialog
import com.example.ui.components.detail.ProductDetailPriceInfoTabContent
import com.example.ui.components.detail.ProductDetailSktTabContent
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary

enum class ProductDetailTab(val title: String, val emoji: String) {
    SKT_BATCHES("Partiler", "📅"),
    PRICE_INFO("Ürün Bilgileri", "🏷️")
}

@Composable
fun ProductDetailModal(
    product: Product?,
    matchingProducts: List<Product>,
    onDismiss: () -> Unit,
    onEditClick: (Product) -> Unit,
    onAddNewSktClick: (Product) -> Unit,
    onEditSktItem: (product: Product, sktTarihi: Long, stokAdedi: Int) -> Unit = { _, _, _ -> },
    onDeleteSkt: (Product) -> Unit,
    onUpdatePrice: (product: Product, price: Double) -> Unit = { _, _ -> },
    onAddToAdetsel: (Product) -> Unit = {},
    onDeductStock: (product: Product, amount: Int, reason: String) -> Unit = { _, _, _ -> }
) {
    if (product == null) return

    val context = LocalContext.current

    var localMatchingProducts by remember(matchingProducts) { mutableStateOf(matchingProducts) }
    var localProduct by remember(product) { mutableStateOf(product) }

    var selectedTab by remember { mutableStateOf(ProductDetailTab.SKT_BATCHES) }
    var editingSktItem by remember { mutableStateOf<Product?>(null) }
    var selectedBatchForDeduct by remember { mutableStateOf<Product?>(null) }
    var showDetailPriceQrScanner by remember { mutableStateOf(false) }

    val totalStockCount = remember(localMatchingProducts) {
        localMatchingProducts.filter { it.sktTarihi > 0L }.sumOf { maxOf(0, it.stokAdedi) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .padding(vertical = 8.dp)
                .imePadding()
                .navigationBarsPadding()
                .clip(RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, TurquoisePrimary),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 1. KOMPAKT VE TEMİZ BAŞLIK ÇUBUĞU
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = TurquoisePrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ürün Detayı",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TurquoiseDark
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Adetsel Sayıma Aktar Butonu
                        val adetselShape = RoundedCornerShape(8.dp)
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(adetselShape)
                                .background(TurquoiseDark)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = Color.White)
                                ) {
                                    onAddToAdetsel(localProduct)
                                    Toast.makeText(context, "Ürün sayım listesine eklendi", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 8.dp)
                                .testTag("detail_add_to_adetsel_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = "Sayıma Ekle",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Sayıma Ekle",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = Color.White
                                )
                            }
                        }

                        // Düzenle Butonu
                        val editShape = RoundedCornerShape(8.dp)
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(editShape)
                                .border(BorderStroke(1.2.dp, TurquoiseDark), editShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = TurquoiseDark)
                                ) { onEditClick(localProduct) }
                                .padding(horizontal = 8.dp)
                                .testTag("detail_edit_product_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Düzenle",
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Düzenle",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = TurquoiseDark
                                )
                            }
                        }

                        // Kapat Butonu
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, radius = 16.dp)
                                ) { onDismiss() }
                                .testTag("close_detail_modal_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = Slate500,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. FERAH ÜRÜN ÖZET KARTI (Ürün Adı + Kod + Toplam Stok)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, Slate200),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(TurquoiseDark, Color(0xFF0D9488))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = localProduct.getDisplayName().take(1).uppercase(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = localProduct.getDisplayName().uppercase(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val productCodeDisplay = localProduct.urunKodu.ifBlank { localProduct.barkod }
                                if (productCodeDisplay.isNotBlank()) {
                                    Text(
                                        text = "Kod: $productCodeDisplay",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TurquoiseDark
                                    )
                                }
                                Text(
                                    text = "•",
                                    fontSize = 10.sp,
                                    color = Slate500
                                )
                                Text(
                                    text = "Toplam: $totalStockCount Adet",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalStockCount >= 10) Color(0xFF4338CA) else Slate700
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. İKİ AŞAMALI SEKMELİ KONTROL (TABS)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProductDetailTabButton(
                        modifier = Modifier.weight(1f),
                        tab = ProductDetailTab.SKT_BATCHES,
                        isSelected = selectedTab == ProductDetailTab.SKT_BATCHES,
                        onClick = { selectedTab = ProductDetailTab.SKT_BATCHES }
                    )
                    ProductDetailTabButton(
                        modifier = Modifier.weight(1f),
                        tab = ProductDetailTab.PRICE_INFO,
                        isSelected = selectedTab == ProductDetailTab.PRICE_INFO,
                        onClick = { selectedTab = ProductDetailTab.PRICE_INFO }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. SEKME İÇERİKLERİ (Scrollable Body)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "detail_tab_content_anim"
                    ) { currentTab ->
                        when (currentTab) {
                            ProductDetailTab.SKT_BATCHES -> {
                                ProductDetailSktTabContent(
                                    product = localProduct,
                                    matchingProducts = localMatchingProducts,
                                    onAddNewSktClick = { onAddNewSktClick(localProduct) },
                                    onEditSktItem = { editingSktItem = it },
                                    onDeleteSkt = { prod ->
                                        localMatchingProducts = localMatchingProducts.filter { it.id != prod.id }
                                        onDeleteSkt(prod)
                                    },
                                    onBatchClick = { batch ->
                                        selectedBatchForDeduct = batch
                                    }
                                )
                            }
                            ProductDetailTab.PRICE_INFO -> {
                                ProductDetailPriceInfoTabContent(
                                    product = localProduct,
                                    matchingProducts = localMatchingProducts,
                                    onQrPriceClick = { showDetailPriceQrScanner = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedBatchForDeduct != null) {
        DeductStockDialog(
            batch = selectedBatchForDeduct!!,
            onDismiss = { selectedBatchForDeduct = null },
            onDeduct = { amount, reason ->
                val batchToDeduct = selectedBatchForDeduct!!
                localMatchingProducts = localMatchingProducts.map {
                    if (it.id == batchToDeduct.id) it.copy(stokAdedi = maxOf(0, it.stokAdedi - amount)) else it
                }
                if (localProduct.id == batchToDeduct.id) {
                    localProduct = localProduct.copy(stokAdedi = maxOf(0, localProduct.stokAdedi - amount))
                }
                onDeductStock(batchToDeduct, amount, reason)
                val remaining = maxOf(0, batchToDeduct.stokAdedi - amount)
                Toast.makeText(context, "$amount adet $reason olarak kaydedildi (Kalan: $remaining)", Toast.LENGTH_SHORT).show()
                selectedBatchForDeduct = null
            }
        )
    }

    if (editingSktItem != null) {
        AddSktModal(
            product = editingSktItem!!,
            isEditMode = true,
            onDismiss = { editingSktItem = null },
            onSaveSkt = { prod, newSkt, newCount ->
                localMatchingProducts = localMatchingProducts.map {
                    if (it.id == prod.id) it.copy(sktTarihi = newSkt, stokAdedi = newCount) else it
                }
                if (localProduct.id == prod.id) {
                    localProduct = localProduct.copy(sktTarihi = newSkt, stokAdedi = newCount)
                }
                onEditSktItem(prod, newSkt, newCount)
                editingSktItem = null
            }
        )
    }

    if (showDetailPriceQrScanner) {
        PriceQrScannerDialog(
            expectedBarcode = localProduct.barkod,
            expectedProductCode = localProduct.urunKodu,
            expectedProductName = localProduct.urunAdi,
            onDismiss = { showDetailPriceQrScanner = false },
            onPriceScanned = { scannedPrice, _ ->
                val updatedProduct = localProduct.copy(fiyat = scannedPrice)
                localProduct = updatedProduct
                localMatchingProducts = localMatchingProducts.map { it.copy(fiyat = scannedPrice) }
                onUpdatePrice(updatedProduct, scannedPrice)
            }
        )
    }
}

@Composable
private fun ProductDetailTabButton(
    modifier: Modifier = Modifier,
    tab: ProductDetailTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tabShape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(tabShape)
            .background(if (isSelected) TurquoiseDark else Slate100)
            .border(BorderStroke(1.dp, if (isSelected) TurquoiseDark else Slate200), tabShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = if (isSelected) Color.White else TurquoiseDark)
            ) { onClick() }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = tab.emoji, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = tab.title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                color = if (isSelected) Color.White else Slate700
            )
        }
    }
}
