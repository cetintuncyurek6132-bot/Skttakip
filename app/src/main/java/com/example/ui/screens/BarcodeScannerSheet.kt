package com.example.ui.screens

import com.example.data.matchesSearchQuery
import com.example.data.parseShelfQrPayload
import com.example.data.findMatchingProducts

import android.Manifest
import android.annotation.SuppressLint
import android.widget.Toast
import android.media.AudioManager
import android.media.ToneGenerator
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
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.theme.SoonYellow
import com.example.ui.theme.SoonYellowContainer
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
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import kotlin.math.abs

@OptIn(ExperimentalPermissionsApi::class, ExperimentalLayoutApi::class)
@Composable
fun BarcodeScannerSheet(
    products: List<Product> = emptyList(),
    startInFixQrMode: Boolean = false,
    isBatterySaverMode: Boolean = false,
    onDismiss: () -> Unit,
    onBarcodeDetected: (String) -> Unit,
    onFixQrScanned: ((rawQr: String, onResult: (String, Boolean) -> Unit) -> Unit)? = null,
    onAddSkt: (Product, Long, Int) -> Unit = { _, _, _ -> }
) {
    var manualBarcode by remember { mutableStateOf("") }
    var activeBarcode by remember { mutableStateOf("") }
    var isFixQrMode by remember { mutableStateOf(startInFixQrMode) }
    var qrFixResultMsg by remember { mutableStateOf("") }
    var selectedProductOverride by remember { mutableStateOf<Product?>(null) }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    var lastScannedTime by remember { mutableStateOf(0L) }
    var isFlashOn by remember { mutableStateOf(false) }

    val toneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        } catch (e: Exception) {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                toneGenerator?.release()
            } catch (e: Exception) {
                // Ignore audio release error
            }
        }
    }

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
                val cameraWeight = if (isKeyboardVisible) 0.25f else if (hasProductDetail) 0.42f else 0.60f
                val bottomWeight = if (isKeyboardVisible) 0.75f else if (hasProductDetail) 0.58f else 0.40f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(cameraWeight)
                        .background(Color(0xFF0D121F))
                ) {
                    // CAMERA PREVIEW (Using COMPATIBLE mode to prevent black/blank screen issues)
                    if (cameraPermissionState.status.isGranted) {
                        CameraXBarcodeView(
                            isFlashOn = isFlashOn,
                            filterMode = ScannerFilterMode.ALL,
                            isBatterySaverMode = isBatterySaverMode,
                            onBarcodeScanned = { barcode ->
                                val now = System.currentTimeMillis()
                                val trimmedBar = barcode.trim()
                                if (trimmedBar.isNotBlank()) {
                                    val isSameRecent = lastScannedCode == trimmedBar && (now - lastScannedTime) < 1200L

                                    if (!isSameRecent) {
                                        try {
                                            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
                                        } catch (e: Exception) {
                                            // Ignore audio error
                                        }
                                        lastScannedCode = trimmedBar
                                        lastScannedTime = now
                                        activeBarcode = trimmedBar
                                        manualBarcode = trimmedBar

                                        if (isFixQrMode && onFixQrScanned != null) {
                                            onFixQrScanned(trimmedBar) { msg, _ ->
                                                qrFixResultMsg = msg
                                            }
                                        }
                                    }
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

                    // DYNAMIC FRAME DIMENSIONS BASED ON ACTIVE MODE
                    // ARAMA mode -> Horizontal rectangle (5:2 ratio, 280dp x 120dp)
                    // QR DUZELT mode -> Square (1:1 ratio, 220dp x 220dp)
                    val targetW = if (isFixQrMode) 220.dp else 280.dp
                    val targetH = if (isFixQrMode) {
                        if (isKeyboardVisible || hasProductDetail) 140.dp else 220.dp
                    } else {
                        if (isKeyboardVisible || hasProductDetail) 85.dp else 120.dp
                    }

                    val animatedFrameWidth by animateDpAsState(
                        targetValue = targetW,
                        animationSpec = tween(durationMillis = 250),
                        label = "frameWidth"
                    )
                    val animatedFrameHeight by animateDpAsState(
                        targetValue = targetH,
                        animationSpec = tween(durationMillis = 250),
                        label = "frameHeight"
                    )

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Prominent Corner Brackets Target Box
                        Box(
                            modifier = Modifier
                                .width(animatedFrameWidth)
                                .height(animatedFrameHeight)
                                .align(Alignment.Center)
                        ) {
                            CornerBracketsViewfinder(
                                modifier = Modifier.fillMaxSize(),
                                color = TurquoisePrimary,
                                strokeWidth = 5.dp,
                                cornerLength = 32.dp,
                                cornerRadius = 16.dp
                            )
                        }
                    }

                    // TOP OVERLAY BAR: UNIFORM DARK PILL/CIRCLE BUTTONS & SEGMENTED MODE SELECTOR
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Close Button (X)
                        Surface(
                            onClick = onDismiss,
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.50f),
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("cancel_scanner_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Kapat",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // 2. Mode Selector Segmented Control (Pill shape)
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.Black.copy(alpha = 0.50f),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (!isFixQrMode) TurquoisePrimary else Color.Transparent)
                                        .clickable { isFixQrMode = false }
                                        .padding(horizontal = 12.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "🔍 Arama",
                                        color = if (!isFixQrMode) Color.White else Color.White.copy(alpha = 0.70f),
                                        fontWeight = if (!isFixQrMode) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isFixQrMode) TurquoisePrimary else Color.Transparent)
                                        .clickable { isFixQrMode = true }
                                        .padding(horizontal = 12.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "🏷️ QR Düzelt",
                                        color = if (isFixQrMode) Color.White else Color.White.copy(alpha = 0.70f),
                                        fontWeight = if (isFixQrMode) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // 3. Flash Toggle Button
                        Surface(
                            onClick = { isFlashOn = !isFlashOn },
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.50f),
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("flash_toggle_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "Flaş",
                                    tint = if (isFlashOn) SoonYellow else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // QR FIX FEEDBACK BANNER (FULL-WIDTH FIXED STRIP AT BOTTOM OF CAMERA VIEW)
                    if (isFixQrMode) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                            color = Color.Black.copy(alpha = 0.80f),
                            border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        tint = TurquoisePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "ETİKET QR İLE BARKOD & FİYAT DÜZELTME MODU",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Raf etiketindeki QR kodunu okutun. Ürün koduna göre barkod ve fiyat otomatik güncellenir.",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White.copy(alpha = 0.75f),
                                    lineHeight = 14.sp
                                )
                                if (qrFixResultMsg.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (qrFixResultMsg.startsWith("✅")) NormalGreenContainer else SoonYellowContainer,
                                        border = BorderStroke(1.dp, if (qrFixResultMsg.startsWith("✅")) NormalGreen else SoonYellow)
                                    ) {
                                        Text(
                                            text = qrFixResultMsg,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (qrFixResultMsg.startsWith("✅")) NormalGreen else Slate900,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // =========================================================================
                // 2. BOTTOM SECTION: MANUAL SEARCH & PRODUCT DETAILS (%80 ÜRÜN AYRINTI, KAPALINKEN %40)
                // =========================================================================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(bottomWeight),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = TurquoisePrimary.copy(alpha = 0.12f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = TurquoisePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Manuel Arama & Ürün Ayrıntıları",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Slate900
                                )
                                Text(
                                    text = "Barkod okutun veya elle barkod numarası yazıp sorgulayın",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

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
                                        "Ürün Adı, Gramaj veya Barkod (Örn: Kola, Süt, 8690)",
                                        fontSize = 11.sp,
                                        color = Slate500
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Arama",
                                        tint = TurquoisePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (manualBarcode.isNotEmpty()) {
                                        IconButton(onClick = {
                                            manualBarcode = ""
                                            activeBarcode = ""
                                            selectedProductOverride = null
                                            lastScannedCode = null
                                            lastScannedTime = 0L
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
                                shape = RoundedCornerShape(12.dp),
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
                                shape = RoundedCornerShape(12.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
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
                                            lastScannedCode = null
                                            lastScannedTime = 0L
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
                                            lastScannedCode = null
                                            lastScannedTime = 0L
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
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr-TR")) }

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

            // Category, Barcode & Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kategori: ${product.kategori} | Barkod: ${product.barkod}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                product.getFormattedPrice()?.let { formattedPrice ->
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFE0F2FE),
                        border = BorderStroke(0.5.dp, Color(0xFF0284C7))
                    ) {
                        Text(
                            text = formattedPrice,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0369A1),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("En Yakın/Geçmiş", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ExpiredRed, textAlign = TextAlign.Center)
                        Text("$expiredOrNearCount Adet", fontSize = 11.sp, fontWeight = FontWeight.Black, color = ExpiredRed, textAlign = TextAlign.Center)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CriticalOrangeContainer,
                    border = BorderStroke(1.dp, CriticalOrange.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Kritik", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CriticalOrange, textAlign = TextAlign.Center)
                        Text("$criticalCount Adet", fontSize = 11.sp, fontWeight = FontWeight.Black, color = CriticalOrange, textAlign = TextAlign.Center)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = NormalGreenContainer,
                    border = BorderStroke(1.dp, NormalGreen.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Güvende", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NormalGreen, textAlign = TextAlign.Center)
                        Text("$safeCount Adet", fontSize = 11.sp, fontWeight = FontWeight.Black, color = NormalGreen, textAlign = TextAlign.Center)
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
                    text = "Tarih ve Adet Ekle",
                    fontSize = 13.sp,
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
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Tarih",
                                tint = TurquoisePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = dateFormat.format(Date(selectedSktMillis)),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
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
                        text = "SKT ve Adetini Ekle",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 13.sp
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
    val parsedData = remember(barcode) { parseShelfQrPayload(barcode) }
    val displayBarcode = parsedData.barcode.ifEmpty { barcode }

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
                            text = "Tarayıcı Barkodu: $displayBarcode",
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
                        text = "ℹ️ BİLGİ: Bu barkod ($displayBarcode) sistemde bulunamadı. Aşağıdaki 'Yeni Ürün Ekle' butonuna basarak yeni ürün kaydı oluşturabilirsiniz.",
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
                    text = "➕ YENİ ÜRÜN EKLE",
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
}

fun Modifier.dashedBorder(
    color: Color,
    strokeWidth: Dp = 1.5.dp,
    dashLength: Dp = 8.dp,
    gapLength: Dp = 6.dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp)
) = this.drawWithCache {
    val stroke = Stroke(
        width = strokeWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(dashLength.toPx(), gapLength.toPx()),
            0f
        )
    )
    val outline = shape.createOutline(size, layoutDirection, this)
    onDrawWithContent {
        drawContent()
        drawOutline(outline, color, style = stroke)
    }
}

@Composable
fun EmptyScannerGuidanceCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .dashedBorder(
                color = Color(0xFFCBD5E1),
                strokeWidth = 1.5.dp,
                dashLength = 8.dp,
                gapLength = 6.dp,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8FAFC)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = TurquoisePrimary.copy(alpha = 0.12f),
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = "Rehber",
                        tint = TurquoiseDark,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Ürün Ayrıntı Önizlemesi",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate900,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Kamerayı ürüne tuttuğunuzda veya arama kutusuna barkod yazdığınızda ürün bilgisi ve SKT durumu burada görünür.",
                fontSize = 12.sp,
                color = Slate500,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
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

enum class ScannerFilterMode {
    ALL,
    ONLY_1D_BARCODE,
    ONLY_QR_CODE
}

@Composable
fun CameraXBarcodeView(
    isFlashOn: Boolean,
    filterMode: ScannerFilterMode = ScannerFilterMode.ALL,
    isBatterySaverMode: Boolean = false,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    val cameraProviderRef = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val cameraRef = remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    val currentOnBarcodeScanned by rememberUpdatedState(onBarcodeScanned)

    val lastEmittedRef = remember { java.util.concurrent.atomic.AtomicReference<Pair<String, Long>>(Pair("", 0L)) }
    val pendingScanRef = remember { java.util.concurrent.atomic.AtomicReference<Pair<String, Long>?>(null) }
    val pendingRunnableRef = remember { java.util.concurrent.atomic.AtomicReference<Runnable?>(null) }
    val lastAnalyzedTimeRef = remember { java.util.concurrent.atomic.AtomicLong(0L) }

    val barcodeScanner = remember {
        val builder = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_AZTEC,
                Barcode.FORMAT_PDF417,
                Barcode.FORMAT_CODABAR
            )
        BarcodeScanning.getClient(builder.build())
    }

    LaunchedEffect(isFlashOn) {
        try {
            cameraRef.value?.cameraControl?.enableTorch(isFlashOn)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                cameraRef.value?.cameraControl?.enableTorch(false)
            } catch (e: Exception) {
                // Ignore torch disable failure
            }
            try {
                pendingRunnableRef.get()?.let { mainHandler.removeCallbacks(it) }
                cameraProviderRef.value?.unbindAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                barcodeScanner.close()
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

                    val resolutionSelector = androidx.camera.core.resolutionselector.ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            androidx.camera.core.resolutionselector.ResolutionStrategy(
                                android.util.Size(1280, 720),
                                androidx.camera.core.resolutionselector.ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                            )
                        )
                        .build()

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val minFrameIntervalMs = if (isBatterySaverMode) 200L else 90L

                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        val currentTime = System.currentTimeMillis()
                        val lastAnalyzed = lastAnalyzedTimeRef.get()
                        if (currentTime - lastAnalyzed < minFrameIntervalMs) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        lastAnalyzedTimeRef.set(currentTime)

                        processImageProxy(barcodeScanner, filterMode, imageProxy) { barcodes ->
                            val raw = barcodes.firstOrNull()?.rawValue?.trim()
                            if (!raw.isNullOrBlank()) {
                                val now = System.currentTimeMillis()
                                val (lastEmittedCode, lastEmittedTime) = lastEmittedRef.get()

                                // 1) Rate limit identical barcode within 1200ms to avoid re-trigger stutter
                                if (lastEmittedCode == raw && (now - lastEmittedTime) < 1200L) {
                                    return@processImageProxy
                                }

                                // 2) Minimal interval between any distinct scans (250ms)
                                if ((now - lastEmittedTime) < 250L) {
                                    return@processImageProxy
                                }

                                // 3) Stabilization buffer (80ms) to upgrade any partial frame scan to full length
                                val currentPending = pendingScanRef.get()
                                if (currentPending != null) {
                                    val (pCode, _) = currentPending
                                    if (raw.length > pCode.length && (raw.contains(pCode) || pCode.contains(raw))) {
                                        pendingScanRef.set(Pair(raw, now))
                                    }
                                } else {
                                    pendingScanRef.set(Pair(raw, now))
                                    val runnable = Runnable {
                                        val finalPending = pendingScanRef.getAndSet(null)
                                        if (finalPending != null) {
                                            val (finalCode, finalTime) = finalPending
                                            val (lCode, lTime) = lastEmittedRef.get()

                                            if (lCode != finalCode || (finalTime - lTime) >= 1200L) {
                                                lastEmittedRef.set(Pair(finalCode, finalTime))
                                                currentOnBarcodeScanned(finalCode)
                                            }
                                        }
                                    }
                                    pendingRunnableRef.get()?.let { mainHandler.removeCallbacks(it) }
                                    pendingRunnableRef.set(runnable)
                                    mainHandler.postDelayed(runnable, 80L)
                                }
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
    filterMode: ScannerFilterMode,
    imageProxy: ImageProxy,
    onSuccess: (List<Barcode>) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                val validBarcodes = barcodes.filter { b ->
                    when (filterMode) {
                        ScannerFilterMode.ONLY_1D_BARCODE -> {
                            b.format != Barcode.FORMAT_QR_CODE &&
                            b.format != Barcode.FORMAT_DATA_MATRIX &&
                            b.format != Barcode.FORMAT_AZTEC
                        }
                        ScannerFilterMode.ONLY_QR_CODE -> {
                            b.format == Barcode.FORMAT_QR_CODE ||
                            b.format == Barcode.FORMAT_DATA_MATRIX ||
                            b.format == Barcode.FORMAT_AZTEC
                        }
                        ScannerFilterMode.ALL -> true
                    }
                }
                if (validBarcodes.isNotEmpty()) {
                    onSuccess(validBarcodes)
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
