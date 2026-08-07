package com.example.ui.screens

import com.example.data.matchesSearchQuery

import android.Manifest
import android.annotation.SuppressLint
import android.widget.Toast
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Size
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.Product
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.CriticalOrangeContainer
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.ExpiredRedContainer
import com.example.ui.theme.NormalGreen
import com.example.ui.theme.NormalGreenContainer
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import kotlin.math.abs

@OptIn(ExperimentalPermissionsApi::class, ExperimentalLayoutApi::class)
@Composable
fun BarcodeScannerSheet(
    products: List<Product> = emptyList(),
    onDismiss: () -> Unit,
    onBarcodeDetected: (String) -> Unit,
    onAddSkt: (Product, Long, Int) -> Unit = { _, _, _ -> }
) {
    var manualBarcode by remember { mutableStateOf("") }
    var activeBarcode by remember { mutableStateOf("") }
    var selectedProductOverride by remember { mutableStateOf<Product?>(null) }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }

    val isKeyboardVisible = WindowInsets.isImeVisible

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // FULL SCREEN DIALOG (Tam Sayfa)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            color = Slate900
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // =========================================================================
                // 1. TOP SECTION: CAMERA PREVIEW & TARGETING VIEWFINDER (Klavye veya Ürün Detayında %20, Normalde %60)
                // =========================================================================
                val hasProductDetail = activeBarcode.isNotBlank()
                val isExpandedMode = isKeyboardVisible || hasProductDetail

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(if (isExpandedMode) 0.20f else 0.60f)
                        .background(Color(0xFF0D121F))
                ) {
                    // CAMERA PREVIEW (Using COMPATIBLE mode to prevent black/blank screen issues)
                    if (cameraPermissionState.status.isGranted) {
                        CameraXBarcodeView(
                            isFlashOn = isFlashOn,
                            onBarcodeScanned = { barcode ->
                                if (lastScannedCode != barcode) {
                                    try {
                                        val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                                        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                                    } catch (e: Exception) {
                                        // Ignore audio error
                                    }
                                    lastScannedCode = barcode
                                    activeBarcode = barcode
                                    manualBarcode = barcode
                                }
                            }
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Kamera İzni",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Kamera erişim izni bekleniyor...",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Kamera açılmadığında aşağıdaki elle barkod arama kutusunu kullanabilirsiniz.",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { cameraPermissionState.launchPermissionRequest() },
                                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
                            ) {
                                Text("KAMERA İZNİ VER", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    // TURQUOISE TARGETING FRAME (Without moving laser line)
                    Box(
                        modifier = Modifier
                            .width(260.dp)
                            .height(if (isExpandedMode) 80.dp else 140.dp)
                            .align(Alignment.Center)
                    ) {
                        CornerBracketsViewfinder(
                            modifier = Modifier.fillMaxSize(),
                            color = TurquoisePrimary,
                            strokeWidth = 4.dp,
                            cornerLength = 28.dp,
                            cornerRadius = 14.dp
                        )
                    }

                    // TOP OVERLAY BAR: CLOSE BUTTON, FLASH TOGGLE, TITLE & LIVE BADGE
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Close Button
                            Surface(
                                onClick = onDismiss,
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("cancel_scanner_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Kapat",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Flash Toggle Button
                            Surface(
                                onClick = { isFlashOn = !isFlashOn },
                                shape = CircleShape,
                                color = if (isFlashOn) TurquoisePrimary else Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("flash_toggle_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                        contentDescription = "Flaş / Işık",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Title
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "📸 BARKOD TARAYICI",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }

                        // Status Badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = TurquoisePrimary.copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = "CANLI",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // =========================================================================
                // 2. BOTTOM SECTION: MANUAL SEARCH & PRODUCT DETAILS (%80 ÜRÜN AYRINTI, KAPALINKEN %40)
                // =========================================================================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(if (isExpandedMode) 0.80f else 0.40f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = Slate50,
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // HEADER: TITLE & SUBTITLE
                        Text(
                            text = "🔍 Manuel Arama & Ürün Ayrıntıları",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Slate900
                        )
                        Text(
                            text = "Barkod okutun veya elle barkod numarası yazıp sorgulayın",
                            fontSize = 12.sp,
                            color = Slate500
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // MANUAL ENTRY SEARCH BAR
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = manualBarcode,
                                onValueChange = {
                                    manualBarcode = it
                                    activeBarcode = it.trim()
                                    selectedProductOverride = null
                                },
                                placeholder = {
                                    Text(
                                        "Ürün Adı, Gramaj veya Barkod (Örn: Kola 1.5, Süt, 8690)",
                                        fontSize = 11.sp,
                                        color = Slate500
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Arama",
                                        tint = TurquoisePrimary
                                    )
                                },
                                trailingIcon = {
                                    if (manualBarcode.isNotEmpty()) {
                                        IconButton(onClick = {
                                            manualBarcode = ""
                                            activeBarcode = ""
                                            selectedProductOverride = null
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Temizle",
                                                tint = Slate500,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("manual_barcode_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Slate900,
                                    unfocusedTextColor = Slate900,
                                    focusedBorderColor = TurquoisePrimary,
                                    unfocusedBorderColor = Slate300,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )

                            Button(
                                onClick = {
                                    if (manualBarcode.isNotBlank()) {
                                        activeBarcode = manualBarcode.trim()
                                    }
                                },
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("manual_barcode_search_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
                            ) {
                                Text("ARA", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // PRODUCT DETAILS AREA ("ÜRÜN AYRINTI YERİ")
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            val matchingProducts = remember(activeBarcode, products) {
                                val query = activeBarcode.trim()
                                if (query.isEmpty()) emptyList()
                                else {
                                    val queryDigits = query.filter { it.isDigit() }
                                    val queryNoLeadingZeros = queryDigits.trimStart('0')

                                    // 1. Exact or normalized barcode match
                                    val exactBarcode = products.filter { p ->
                                        val pBarcodeDigits = p.barkod.trim().filter { it.isDigit() }
                                        p.barkod.equals(query, ignoreCase = true) ||
                                        (queryDigits.isNotEmpty() && pBarcodeDigits == queryDigits) ||
                                        (queryNoLeadingZeros.isNotEmpty() && pBarcodeDigits.trimStart('0') == queryNoLeadingZeros)
                                    }

                                    if (exactBarcode.isNotEmpty()) {
                                        exactBarcode.sortedBy { it.sktTarihi }
                                    } else {
                                        // 2. Exact product code match
                                        val exactCode = products.filter { it.urunKodu.equals(query, ignoreCase = true) }
                                        if (exactCode.isNotEmpty()) {
                                            exactCode.sortedBy { it.sktTarihi }
                                        } else {
                                            // 3. Fallback search query match
                                            products.filter { it.matchesSearchQuery(query) }.sortedBy { it.sktTarihi }
                                        }
                                    }
                                }
                            }

                            // Group matching products by unique barcode/product name to detect if query matched multiple distinct items
                            val distinctProducts = remember(matchingProducts) {
                                matchingProducts.distinctBy { it.barkod }
                            }

                            val foundProduct = selectedProductOverride ?: matchingProducts.firstOrNull()

                            // If search query matched multiple different products (e.g. searching "kola" or "sut"), show product selector chips
                            if (distinctProducts.size > 1) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
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
                                                    onClick = { selectedProductOverride = item },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSelected) TurquoisePrimary else Slate100,
                                                    border = BorderStroke(1.dp, if (isSelected) TurquoisePrimary else Slate300)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = item.urunAdi,
                                                            fontSize = 11.sp,
                                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                                            color = if (isSelected) Color.White else Slate900
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
                                    ProductDetailPreviewCard(
                                        product = foundProduct,
                                        matchingProducts = if (sameProductSktList.isNotEmpty()) sameProductSktList else listOf(foundProduct),
                                        onAddSkt = { prod, millis, count ->
                                            onAddSkt(prod, millis, count)
                                        },
                                        onSelectProduct = {
                                            onBarcodeDetected(foundProduct.barkod)
                                        },
                                        onClearDetail = {
                                            activeBarcode = ""
                                            manualBarcode = ""
                                            selectedProductOverride = null
                                        }
                                    )
                                }

                                // 2) A search term is entered/scanned, but product not found
                                activeBarcode.isNotBlank() -> {
                                    ProductNotFoundPreviewCard(
                                        barcode = activeBarcode,
                                        onAddProduct = {
                                            onBarcodeDetected(activeBarcode)
                                        },
                                        onClearDetail = {
                                            activeBarcode = ""
                                            manualBarcode = ""
                                            selectedProductOverride = null
                                        }
                                    )
                                }

                                // 3) No barcode selected/scanned yet
                                else -> {
                                    EmptyScannerGuidanceCard()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailPreviewCard(
    product: Product,
    matchingProducts: List<Product> = listOf(product),
    onAddSkt: (Product, Long, Int) -> Unit = { _, _, _ -> },
    onSelectProduct: () -> Unit,
    onClearDetail: () -> Unit = {}
) {
    val context = LocalContext.current
    val daysRemaining = getDaysRemaining(product.sktTarihi)
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("tr", "TR")) }

    var selectedSktMillis by remember(product.barkod) {
        mutableStateOf(System.currentTimeMillis())
    }
    var sktAdediStr by remember(product.barkod) { mutableStateOf("1") }
    var isOcrScannerOpen by remember { mutableStateOf(false) }
    var isDatePickerOpen by remember { mutableStateOf(false) }

    if (isDatePickerOpen) {
        com.example.ui.components.CustomBoxedCalendarDialog(
            initialDateMillis = selectedSktMillis,
            onDismissRequest = { isDatePickerOpen = false },
            onDateSelected = { selectedMillis ->
                selectedSktMillis = selectedMillis
            }
        )
    }

    // Risk Level Breakdown
    val expiredOrNearCount = remember(matchingProducts) {
        matchingProducts.filter { getDaysRemaining(it.sktTarihi) <= 3 }.sumOf { it.stokAdedi }
    }
    val criticalCount = remember(matchingProducts) {
        matchingProducts.filter { getDaysRemaining(it.sktTarihi) in 4..15 }.sumOf { it.stokAdedi }
    }
    val safeCount = remember(matchingProducts) {
        matchingProducts.filter { getDaysRemaining(it.sktTarihi) >= 16 }.sumOf { it.stokAdedi }
    }

    val (badgeText, badgeBg, badgeTextColor) = when {
        daysRemaining < 0 -> Triple(
            "🚨 SÜRESİ GEÇTİ (${abs(daysRemaining)} gün)",
            ExpiredRedContainer,
            ExpiredRed
        )
        daysRemaining in 0..7 -> Triple(
            "⚠️ KRİTİK ($daysRemaining Gün)",
            CriticalOrangeContainer,
            CriticalOrange
        )
        else -> Triple(
            "✅ NORMAL ($daysRemaining Gün)",
            NormalGreenContainer,
            NormalGreen
        )
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, TurquoisePrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top info: Name, SKT Badge & Clear Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.urunAdi,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = badgeText,
                        color = badgeTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onClearDetail,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Temizle",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Category & Barcode
            Text(
                text = "Kategori: ${product.kategori} | Barkod: ${product.barkod}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // COLORED RISK BREAKDOWN BADGES
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ExpiredRedContainer,
                    border = BorderStroke(1.dp, ExpiredRed.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("En Yakın/Geçmiş", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ExpiredRed)
                        Text("$expiredOrNearCount Adet", fontSize = 11.sp, fontWeight = FontWeight.Black, color = ExpiredRed)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CriticalOrangeContainer,
                    border = BorderStroke(1.dp, CriticalOrange.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Kritik", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CriticalOrange)
                        Text("$criticalCount Adet", fontSize = 11.sp, fontWeight = FontWeight.Black, color = CriticalOrange)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = NormalGreenContainer,
                    border = BorderStroke(1.dp, NormalGreen.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Güvende", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NormalGreen)
                        Text("$safeCount Adet", fontSize = 11.sp, fontWeight = FontWeight.Black, color = NormalGreen)
                    }
                }
            }

            // Registered SKT list
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Kayıtlı SKT Tarihleri (${matchingProducts.size} tarih):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TurquoisePrimary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    matchingProducts.forEach { item ->
                        val dateStr = if (item.sktTarihi > 0L) dateFormat.format(Date(item.sktTarihi)) else "SKT Girilmedi"
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "📅 $dateStr (${item.stokAdedi} adet)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // SKT & ADEDİ EKLEME ALANI
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "➕ YENİ SKT VE ADEDİ EKLE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = TurquoiseDark
                )

                // Date Picker & OCR Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { isDatePickerOpen = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("skt_date_picker_button"),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, TurquoisePrimary),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Tarih Seç",
                                    tint = TurquoisePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = dateFormat.format(Date(selectedSktMillis)),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Tarih Seç 📅",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = TurquoiseDark
                            )
                        }
                    }

                    Button(
                        onClick = { isOcrScannerOpen = true },
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("open_skt_ocr_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "SKT Tara",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "OCR",
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }

                // SKT ADEDİ GİRİŞİ (Label + Stepper + Compact Text Input)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SKT Adedi (Miktar):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val current = sktAdediStr.toIntOrNull() ?: 0
                                if (current > 1) sktAdediStr = (current - 1).toString()
                            },
                            modifier = Modifier.size(38.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("-", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                        }

                        OutlinedTextField(
                            value = sktAdediStr,
                            onValueChange = { sktAdediStr = it },
                            modifier = Modifier.width(80.dp),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = TurquoisePrimary,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        Button(
                            onClick = {
                                val current = sktAdediStr.toIntOrNull() ?: 0
                                sktAdediStr = (current + 1).toString()
                            },
                            modifier = Modifier.size(38.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }

                Button(
                    onClick = {
                        val count = sktAdediStr.toIntOrNull() ?: 1
                        onAddSkt(product, selectedSktMillis, count)
                        Toast.makeText(context, "✓ SKT eklendi ($count adet)", Toast.LENGTH_SHORT).show()
                        sktAdediStr = "1"
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("save_skt_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "SKT Ekle",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+ FARKLI SKT VE ADEDİ EKLE",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Button(
                    onClick = {
                        Toast.makeText(context, "✓ SKT Kayıtları Kaydedildi", Toast.LENGTH_SHORT).show()
                        onClearDetail()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("save_finished_skt_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Kaydet",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "KAYDET",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ProductNotFoundPreviewCard(
    barcode: String,
    onAddProduct: () -> Unit,
    onClearDetail: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CriticalOrangeContainer),
        border = BorderStroke(1.dp, CriticalOrange.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Bulunamadı",
                        tint = CriticalOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Sistemde Kayıtlı Ürün Bulunamadı",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Slate900
                        )
                        Text(
                            text = "Tarayıcı Barkodu: $barcode",
                            fontSize = 12.sp,
                            color = Slate700
                        )
                    }
                }
                IconButton(
                    onClick = onClearDetail,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Temizle",
                        tint = Slate500,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💡 TAVSİYE: Bu barkod ($barcode) sistemde bulunamadı. Aşağıdaki butona dokunarak bu barkod için hemen yeni ürün kaydı oluşturabilirsiniz.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }
            }

            Button(
                onClick = onAddProduct,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("add_scanned_barcode_product_button"),
                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Yeni Ürün Ekle",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "➕ TAVSİYE: YENİ ÜRÜN OLARAK EKLE",
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun EmptyScannerGuidanceCard() {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = "Rehber",
                        tint = TurquoisePrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Ürün Ayrıntı Önizlemesi",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Kamerayı ürüne tuttuğunuzda veya test barkoduna dokunduğunuzda ürün bilgisi ve SKT durumu burada görünür.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TestBarcodeChip(
    name: String,
    barcode: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1F293D),
        border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TurquoisePrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = barcode,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun CameraXBarcodeView(
    isFlashOn: Boolean,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val cameraProviderRef = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val cameraRef = remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    LaunchedEffect(isFlashOn) {
        cameraRef.value?.cameraControl?.enableTorch(isFlashOn)
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                cameraProviderRef.value?.unbindAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                executor.shutdown()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                // COMPATIBLE mode uses TextureView instead of SurfaceView, preventing
                // black screen / rendering failures in Compose across devices & emulators
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProviderRef.value = cameraProvider
                    val preview = Preview.Builder().build().apply {
                        setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val barcodeScanner = BarcodeScanning.getClient()

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        processImageProxy(barcodeScanner, imageProxy) { barcodes ->
                            barcodes.firstOrNull()?.rawValue?.let { raw ->
                                onBarcodeScanned(raw)
                            }
                        }
                    }

                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                    cameraRef.value = camera
                    camera.cameraControl.enableTorch(isFlashOn)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@SuppressLint("UnsafeOptInUsageError")
private fun processImageProxy(
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onSuccess: (List<Barcode>) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    onSuccess(barcodes)
                }
            }
            .addOnFailureListener {
                // Ignore silent scan failures
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

private fun getDaysRemaining(sktTarihi: Long): Long {
    val now = System.currentTimeMillis()
    val diff = sktTarihi - now
    return diff / (1000 * 60 * 60 * 24)
}

@Composable
fun CornerBracketsViewfinder(
    modifier: Modifier = Modifier,
    color: Color = TurquoisePrimary,
    strokeWidth: Dp = 4.dp,
    cornerLength: Dp = 28.dp,
    cornerRadius: Dp = 14.dp
) {
    Canvas(modifier = modifier) {
        val sw = strokeWidth.toPx()
        val cl = cornerLength.toPx()
        val cr = cornerRadius.toPx()
        val w = size.width
        val h = size.height

        // Top-Left corner
        drawPath(
            path = Path().apply {
                moveTo(0f, cl)
                lineTo(0f, cr)
                quadraticTo(0f, 0f, cr, 0f)
                lineTo(cl, 0f)
            },
            color = color,
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )

        // Top-Right corner
        drawPath(
            path = Path().apply {
                moveTo(w - cl, 0f)
                lineTo(w - cr, 0f)
                quadraticTo(w, 0f, w, cr)
                lineTo(w, cl)
            },
            color = color,
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )

        // Bottom-Left corner
        drawPath(
            path = Path().apply {
                moveTo(0f, h - cl)
                lineTo(0f, h - cr)
                quadraticTo(0f, h, cr, h)
                lineTo(cl, h)
            },
            color = color,
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )

        // Bottom-Right corner
        drawPath(
            path = Path().apply {
                moveTo(w - cl, h)
                lineTo(w - cr, h)
                quadraticTo(w, h, w, h - cr)
                lineTo(w, h - cl)
            },
            color = color,
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )
    }
}
