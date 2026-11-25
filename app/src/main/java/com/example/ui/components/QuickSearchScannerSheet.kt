package com.example.ui.components

import android.Manifest
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.data.findMatchingProducts
import com.example.ui.screens.scanner.CameraXBarcodeView
import com.example.ui.screens.scanner.CornerBracketsViewfinder
import com.example.ui.screens.scanner.ScannerFilterMode
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.ExpiredRedDark
import com.example.ui.theme.NormalGreen
import com.example.ui.theme.NormalGreenContainer
import com.example.ui.theme.NormalGreenDark
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoiseLight
import com.example.ui.theme.TurquoisePrimary
import com.example.util.HapticFeedbackHelper
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.Locale

private enum class QuickSearchResultState {
    IDLE,
    FOUND,
    NOT_FOUND
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun QuickSearchScannerSheet(
    allProducts: List<Product>,
    onDismiss: () -> Unit,
    onProductClick: (Product) -> Unit,
    onAddNewProductWithBarcode: (String) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    var isFlashOn by remember { mutableStateOf(false) }
    var manualQuery by remember { mutableStateOf("") }
    var lastScannedBarcode by remember { mutableStateOf("") }
    var scannedProduct by remember { mutableStateOf<Product?>(null) }
    var isProductNotFound by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            isFlashOn = false
        }
    }

    val playScanBeep = remember {
        {
            com.example.util.SoundToneManager.playScannerFeedback(context, isSuccess = true)
            HapticFeedbackHelper.triggerSuccessHaptic(context)
        }
    }

    fun performManualLookup(rawQuery: String, forceShowNotFound: Boolean) {
        val clean = rawQuery.trim()
        if (clean.isEmpty()) {
            lastScannedBarcode = ""
            scannedProduct = null
            isProductNotFound = false
            return
        }

        lastScannedBarcode = clean
        val matched = allProducts.findMatchingProducts(clean).firstOrNull()
        if (matched != null) {
            scannedProduct = matched
            isProductNotFound = false
        } else {
            scannedProduct = null
            isProductNotFound = forceShowNotFound || clean.length >= 4
        }
    }

    AppBottomSheetWrapper(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = Color.White,
        contentModifier = Modifier
            .fillMaxHeight(0.90f)
            .statusBarsPadding()
    ) { _ ->
        // 1. ÜST BAŞLIK VE KAPAT BUTONU
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(TurquoiseLight.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = TurquoiseDark,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = "Hızlı Ürün Ara",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            fontSize = 20.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Barkod okutun veya kod ile arayın",
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
                    .testTag("quick_search_close_button")
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

        HorizontalDivider(color = Slate100, thickness = 1.dp)

        // 2. ANA İÇERİK: KAMERA VİZÖRÜ, MANUEL ARAMA VE SONUÇ KARTLARI
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // KAMERA ALANI (290.dp dengeli yükseklik, 16.dp yuvarlak köşeler, ince modern kenarlık)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.5.dp, Slate200),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    if (cameraPermissionState.status.isGranted) {
                        CameraXBarcodeView(
                            isFlashOn = isFlashOn,
                            zoomRatio = 1.0f,
                            filterMode = ScannerFilterMode.ALL,
                            isPaused = false,
                            requireCloseDistance = false,
                            onBarcodeScanned = { barcode ->
                                val clean = barcode.trim()
                                if (clean.isNotBlank() && clean != lastScannedBarcode) {
                                    manualQuery = ""
                                    lastScannedBarcode = clean
                                    val matched = allProducts.findMatchingProducts(clean).firstOrNull()
                                    if (matched != null) {
                                        scannedProduct = matched
                                        isProductNotFound = false
                                        playScanBeep()
                                    } else {
                                        scannedProduct = null
                                        isProductNotFound = true
                                        HapticFeedbackHelper.triggerWarningHaptic(context)
                                    }
                                }
                            }
                        )

                        // Modern Köşe Odaklama Vizörü
                        CornerBracketsViewfinder(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth(0.74f)
                                .height(132.dp),
                            color = if (scannedProduct != null) NormalGreen else TurquoisePrimary,
                            strokeWidth = 3.5.dp,
                            cornerLength = 26.dp,
                            cornerRadius = 14.dp,
                            isGlowing = scannedProduct != null
                        )

                        // Sağ Üst Flaş Butonu (48.dp dokunma alanı, yarı saydam dairesel buton)
                        IconButton(
                            onClick = { isFlashOn = !isFlashOn },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(48.dp)
                                .testTag("quick_search_flash_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .border(
                                        width = 1.dp,
                                        color = if (isFlashOn) {
                                            Color(0xFFFFD54F).copy(alpha = 0.6f)
                                        } else {
                                            Color.White.copy(alpha = 0.28f)
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = if (isFlashOn) "Flaşı Kapat" else "Flaşı Aç",
                                    tint = if (isFlashOn) Color(0xFFFFD54F) else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Kamera alt kenarında hafif rehber rozet (yalnızca boş durumda)
                        if (scannedProduct == null && !isProductNotFound) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = "Barkodu çerçeve içine hizalayın",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = TurquoisePrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Kamera İzni Gerekli",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Barkodları okutmak için kamera erişimine izin verin.",
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { cameraPermissionState.launchPermissionRequest() },
                                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("quick_search_camera_permission_button")
                            ) {
                                Text(
                                    text = "İzin Ver",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // MANUEL BARKOD / ÜRÜN KODU ARAMA KUTUSU
            OutlinedTextField(
                value = manualQuery,
                onValueChange = { input ->
                    val normalized = input.uppercase(Locale.forLanguageTag("tr-TR"))
                    manualQuery = normalized
                    performManualLookup(normalized, forceShowNotFound = false)
                },
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate900
                ),
                placeholder = {
                    Text(
                        text = "Barkod, ürün kodu veya adı yazın...",
                        fontSize = 13.sp,
                        color = Slate400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TurquoiseDark,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (manualQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                manualQuery = ""
                                lastScannedBarcode = ""
                                scannedProduct = null
                                isProductNotFound = false
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("quick_search_clear_input_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Aramayı Temizle",
                                tint = Slate500,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        performManualLookup(manualQuery, forceShowNotFound = true)
                        focusManager.clearFocus()
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Slate900,
                    unfocusedTextColor = Slate900,
                    focusedBorderColor = TurquoisePrimary,
                    unfocusedBorderColor = Slate200,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Slate50,
                    cursorColor = TurquoisePrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
                    .testTag("quick_search_manual_input")
            )

            // 3. ALT PANEL: SONUÇ KARTLARI (Tam beyaz / pastel kartlar, ince kenarlık)
            val resultState = when {
                scannedProduct != null -> QuickSearchResultState.FOUND
                isProductNotFound -> QuickSearchResultState.NOT_FOUND
                else -> QuickSearchResultState.IDLE
            }

            AnimatedContent(
                targetState = resultState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "quick_search_result_transition"
            ) { state ->
                when (state) {
                    QuickSearchResultState.FOUND -> {
                        val prod = scannedProduct
                        if (prod != null) {
                            QuickSearchFoundProductCard(
                                product = prod,
                                allProducts = allProducts,
                                onClick = { onProductClick(prod) }
                            )
                        }
                    }

                    QuickSearchResultState.NOT_FOUND -> {
                        QuickSearchNotFoundCard(
                            barcode = lastScannedBarcode,
                            onAddNewClick = { onAddNewProductWithBarcode(lastScannedBarcode) }
                        )
                    }

                    QuickSearchResultState.IDLE -> {
                        QuickSearchIdleCard()
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSearchFoundProductCard(
    product: Product,
    allProducts: List<Product>,
    onClick: () -> Unit
) {
    val totalStock = remember(allProducts, product) {
        allProducts
            .filter { it.barkod.isNotBlank() && it.barkod == product.barkod }
            .sumOf { it.stokAdedi }
            .let { if (it > 0) it else product.stokAdedi }
    }

    val earliestSkt = remember(allProducts, product) {
        allProducts
            .filter { it.barkod.isNotBlank() && it.barkod == product.barkod && it.sktTarihi > 0 }
            .minByOrNull { it.sktTarihi }
            ?.getFormattedSkt() ?: product.getFormattedSkt()
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Slate200),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .testTag("quick_search_found_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = product.urunAdi,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900,
                            fontSize = 16.sp,
                            lineHeight = 21.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    val subtitle = buildString {
                        if (product.urunKodu.isNotBlank()) {
                            append("Kod: ${product.urunKodu}")
                            if (product.barkod.isNotBlank()) {
                                append("  •  ${product.barkod}")
                            }
                        } else if (product.barkod.isNotBlank()) {
                            append("Barkod: ${product.barkod}")
                        }
                    }
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Pastel Satış Fiyatı Rozeti
                product.getFormattedPrice()?.let { formattedPrice ->
                    Surface(
                        color = TurquoiseLight.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.35f))
                    ) {
                        AutoSizeMetricText(
                            text = formattedPrice,
                            maxFontSize = 14.5.sp,
                            minFontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TurquoiseDark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pastel Amber SKT Rozeti
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(13.dp)
                            )
                            AutoSizeMetricText(
                                text = earliestSkt,
                                maxFontSize = 12.sp,
                                minFontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }

                    // Pastel Yeşil Stok Rozeti
                    Surface(
                        color = NormalGreenContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = NormalGreenDark,
                                modifier = Modifier.size(13.dp)
                            )
                            AutoSizeMetricText(
                                text = "$totalStock Adet",
                                maxFontSize = 12.sp,
                                minFontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NormalGreenDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = TurquoiseLight.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Detay",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TurquoiseDark,
                            maxLines = 1
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = TurquoiseDark,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSearchNotFoundCard(
    barcode: String,
    onAddNewClick: () -> Unit
) {
    Surface(
        color = Color(0xFFFEF2F2),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFFFECACA)),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quick_search_not_found_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEE2E2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = ExpiredRed,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Ürün Bulunamadı",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.5.sp,
                        color = ExpiredRedDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Barkod / Kod: $barcode",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate600,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                onClick = onAddNewClick,
                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                shape = RoundedCornerShape(14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("quick_search_add_new_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Yeni Ürün Olarak Ekle",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun QuickSearchIdleCard() {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Slate200),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quick_search_idle_card")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(TurquoiseLight.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = TurquoiseDark,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = "Barkodu vizöre hizalayın veya yukarıdaki alandan yazarak arayın",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Slate600,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
