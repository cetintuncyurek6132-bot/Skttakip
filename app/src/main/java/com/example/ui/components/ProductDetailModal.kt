package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.data.StockLog
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
import kotlinx.coroutines.launch

enum class ProductDetailTab(val title: String, val emoji: String) {
    SKT_BATCHES("Partiler", "📅"),
    PRICE_INFO("Ürün Bilgileri", "🏷️")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailModal(
    product: Product?,
    matchingProducts: List<Product>,
    stockLogs: List<StockLog> = emptyList(),
    onDismiss: () -> Unit,
    onEditClick: (Product) -> Unit,
    onAddNewSktClick: (Product) -> Unit,
    onEditSktItem: (product: Product, sktTarihi: Long, stokAdedi: Int) -> Unit = { _, _, _ -> },
    onDeleteSkt: (Product) -> Unit,
    onUpdatePrice: (product: Product, price: Double) -> Unit = { _, _ -> },
    onAddToAdetsel: (Product, Int) -> Unit = { _, _ -> },
    onDeductStock: (product: Product, amount: Int, reason: String) -> Unit = { _, _, _ -> }
) {
    if (product == null) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val currentProduct = remember(product, matchingProducts) {
        matchingProducts.firstOrNull { it.id == product.id } ?: matchingProducts.firstOrNull() ?: product
    }

    var selectedTab by remember { mutableStateOf(ProductDetailTab.SKT_BATCHES) }
    var editingSktItem by remember { mutableStateOf<Product?>(null) }
    var selectedBatchForDeduct by remember { mutableStateOf<Product?>(null) }
    var showDetailPriceQrScanner by remember { mutableStateOf(false) }
    var showAddToAdetselDialog by remember { mutableStateOf(false) }

    val totalStockCount = remember(matchingProducts) {
        matchingProducts.filter { it.sktTarihi > 0L }.sumOf { maxOf(0, it.stokAdedi) }
    }

    // 3 Kademeli Dikey Sürükleme (PartiallyExpanded -> Expanded -> Dismiss)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun dismissSheet() {
        coroutineScope.launch {
            try {
                sheetState.hide()
            } catch (_: Exception) {}
            onDismiss()
        }
    }

    // Sistem Geri Tuşu / Hareketi (BackHandler)
    BackHandler(enabled = sheetState.isVisible) {
        dismissSheet()
    }

    // Çift Yönlü Yatay Kenar Kaydırma Eşiği (45.dp)
    val swipeThresholdPx = remember(density) { with(density) { 45.dp.toPx() } }
    var totalDragX by remember { mutableFloatStateOf(0f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.35f),
        dragHandle = {
            PremiumSheetDragHandle(
                modifier = Modifier.pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { totalDragX = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            totalDragX += dragAmount
                            if (kotlin.math.abs(totalDragX) > swipeThresholdPx) {
                                change.consume()
                                dismissSheet()
                            }
                        }
                    )
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. KOMPAKT VE TEMİZ BAŞLIK ÇUBUĞU (Dual Edge Swipe Destekli)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { totalDragX = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                totalDragX += dragAmount
                                if (kotlin.math.abs(totalDragX) > swipeThresholdPx) {
                                    change.consume()
                                    dismissSheet()
                                }
                            }
                        )
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = TurquoisePrimary.copy(alpha = 0.12f),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = TurquoiseDark,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ürün Detayı",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TurquoiseDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Adetsel Sayıma Aktar Butonu
                    val adetselShape = RoundedCornerShape(8.dp)
                    Box(
                        modifier = Modifier
                            .height(31.dp)
                            .clip(adetselShape)
                            .background(TurquoiseDark)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = Color.White)
                            ) {
                                showAddToAdetselDialog = true
                            }
                            .padding(horizontal = 7.dp)
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
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Sayıma Ekle",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }

                    // Düzenle Butonu
                    val editShape = RoundedCornerShape(8.dp)
                    Box(
                        modifier = Modifier
                            .height(31.dp)
                            .clip(editShape)
                            .border(BorderStroke(1.2.dp, TurquoiseDark), editShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = TurquoiseDark)
                            ) { onEditClick(currentProduct) }
                            .padding(horizontal = 7.dp)
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
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Düzenle",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = TurquoiseDark,
                                maxLines = 1
                            )
                        }
                    }

                    // Kapat Butonu
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, radius = 14.dp)
                            ) { dismissSheet() }
                            .testTag("close_detail_modal_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = Slate700,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            // 2. FERAH ÜRÜN ÖZET KARTI (Ürün Adı + Kod + Toplam Adet)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF8FAFC),
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { totalDragX = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                totalDragX += dragAmount
                                if (kotlin.math.abs(totalDragX) > swipeThresholdPx) {
                                    change.consume()
                                    dismissSheet()
                                }
                            }
                        )
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 2.dp
                    ) {
                        FallbackLetterBadge(currentProduct.getDisplayName())
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentProduct.getDisplayName().uppercase(java.util.Locale.forLanguageTag("tr-TR")),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 19.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val productCodeDisplay = currentProduct.urunKodu.ifBlank { currentProduct.barkod }
                            if (productCodeDisplay.isNotBlank()) {
                                Text(
                                    text = "Kod: $productCodeDisplay",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700
                                )
                            }
                            Text(
                                text = "•",
                                fontSize = 11.sp,
                                color = Slate700
                            )
                            Text(
                                text = "Toplam: $totalStockCount Adet",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalStockCount >= 10) Color(0xFF4338CA) else TurquoiseDark
                            )
                        }
                    }
                }
            }

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

            // 4. SEKME İÇERİKLERİ
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "detail_tab_content_anim"
            ) { currentTab ->
                when (currentTab) {
                    ProductDetailTab.SKT_BATCHES -> {
                        ProductDetailSktTabContent(
                            product = currentProduct,
                            matchingProducts = matchingProducts,
                            stockLogs = stockLogs,
                            onAddNewSktClick = { onAddNewSktClick(currentProduct) },
                            onEditSktItem = { editingSktItem = it },
                            onDeleteSkt = { prod ->
                                onDeleteSkt(prod)
                            },
                            onBatchClick = { batch ->
                                selectedBatchForDeduct = batch
                            }
                        )
                    }
                    ProductDetailTab.PRICE_INFO -> {
                        ProductDetailPriceInfoTabContent(
                            product = currentProduct,
                            matchingProducts = matchingProducts,
                            onQrPriceClick = { showDetailPriceQrScanner = true }
                        )
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
                val remaining = maxOf(0, batchToDeduct.stokAdedi - amount)
                onDeductStock(batchToDeduct, amount, reason)
                val toastMsg = if (remaining == 0) {
                    "$amount adet $reason olarak düşüldü (Parti tamamlandı)"
                } else {
                    "$amount adet $reason olarak kaydedildi (Kalan: $remaining)"
                }
                Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
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
                onEditSktItem(prod, newSkt, newCount)
                editingSktItem = null
            }
        )
    }

    if (showDetailPriceQrScanner) {
        PriceQrScannerDialog(
            expectedBarcode = currentProduct.barkod,
            expectedProductCode = currentProduct.urunKodu,
            expectedProductName = currentProduct.urunAdi,
            onDismiss = { showDetailPriceQrScanner = false },
            onPriceScanned = { scannedPrice, _ ->
                val updatedProduct = currentProduct.copy(fiyat = scannedPrice)
                onUpdatePrice(updatedProduct, scannedPrice)
            }
        )
    }

    if (showAddToAdetselDialog) {
        AddToAdetselQuantityDialog(
            product = currentProduct,
            onDismiss = { showAddToAdetselDialog = false },
            onConfirm = { countedQty ->
                onAddToAdetsel(currentProduct, countedQty)
                showAddToAdetselDialog = false
            }
        )
    }
}

@Composable
private fun AddToAdetselQuantityDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (countedQty: Int) -> Unit
) {
    var quantityText by remember { mutableStateOf("1") }

    androidx.compose.runtime.DisposableEffect(Unit) {
        AppModalBlurState.onModalOpened()
        onDispose {
            AppModalBlurState.onModalClosed()
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Başlık
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = TurquoisePrimary.copy(alpha = 0.14f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Sayıma Ekle",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Raftaki mevcut adedi girin",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate700
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = Slate700,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Ürün Bilgi Kartı
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = product.getDisplayName().uppercase(java.util.Locale.forLanguageTag("tr-TR")),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Barkod: ${product.barkod.ifBlank { "-" }}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )
                            if (product.urunKodu.isNotBlank()) {
                                Text(
                                    text = "Kod: ${product.urunKodu}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700
                                )
                            }
                        }
                    }
                }

                // Raftaki Adet Seçici (Stepper)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Raftaki Adet",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700
                    )

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Eksi (-)
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        val current = quantityText.toIntOrNull() ?: 0
                                        if (current > 0) {
                                            quantityText = (current - 1).toString()
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "−",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }

                            // Ortadaki Sayı Girişi
                            androidx.compose.material3.OutlinedTextField(
                                value = quantityText,
                                onValueChange = { newVal ->
                                    quantityText = newVal.filter { it.isDigit() }.take(5)
                                },
                                placeholder = {
                                    Text(
                                        text = "0",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Slate500,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TurquoiseDark,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                ),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                ),
                                shape = RoundedCornerShape(14.dp),
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TurquoisePrimary,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(56.dp)
                                    .testTag("detail_adetsel_qty_input")
                            )

                            // Artı (+)
                            Surface(
                                shape = CircleShape,
                                color = TurquoisePrimary.copy(alpha = 0.16f),
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        val current = quantityText.toIntOrNull() ?: 0
                                        quantityText = (current + 1).toString()
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "+",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TurquoiseDark
                                    )
                                }
                            }
                        }
                    }
                }

                // Alt Butonlar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Vazgeç",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )
                        }
                    }

                    androidx.compose.material3.Button(
                        onClick = {
                            val qty = quantityText.toIntOrNull() ?: 0
                            onConfirm(qty)
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = TurquoiseDark
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(50.dp)
                            .testTag("detail_adetsel_confirm_button")
                    ) {
                        Text(
                            text = "Sayıma Ekle",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductDetailTabButton(
    modifier: Modifier = Modifier,
    tab: ProductDetailTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tabShape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(tabShape)
            .background(if (isSelected) TurquoiseDark else Color(0xFFF1F5F9))
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
            Text(text = tab.emoji, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = tab.title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (isSelected) Color.White else Slate700
            )
        }
    }
}

@Composable
private fun FallbackLetterBadge(displayName: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(TurquoiseDark, Color(0xFF0D9488))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayName.take(1).uppercase(java.util.Locale.forLanguageTag("tr-TR")),
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
    }
}
