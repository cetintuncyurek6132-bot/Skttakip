package com.example.ui.screens

import com.example.data.matchesSearchQuery
import com.example.data.parseShelfQrPayload
import com.example.data.findMatchingProducts
import com.example.ui.screens.scanner.CameraXBarcodeView
import com.example.ui.screens.scanner.CornerBracketsViewfinder
import com.example.ui.screens.scanner.EmptyScannerGuidanceCard
import com.example.ui.screens.scanner.ProductDetailPreviewCard
import com.example.ui.screens.scanner.ProductNotFoundPreviewCard
import com.example.ui.screens.scanner.ScannerFilterMode
import com.example.ui.screens.scanner.TestBarcodeChip
import com.example.ui.screens.scanner.ScannerTopControls
import com.example.ui.screens.scanner.QrFixFloatingHudBanner
import com.example.ui.screens.scanner.QrFixSummaryPanel
import com.example.ui.screens.scanner.ScannerManualSearchBar
import com.example.ui.screens.scanner.ScannerProductDetailsSection
import com.example.ui.screens.scanner.ScannerFeedbackHelper
import com.example.ui.screens.scanner.ScanResultRisk

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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import androidx.compose.runtime.mutableStateListOf
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
    userName: String = "Kullanıcı",
    startInFixQrMode: Boolean = false,
    isBatterySaverMode: Boolean = false,
    onDismiss: () -> Unit,
    onBarcodeDetected: (String) -> Unit,
    onFixQrScanned: ((rawQr: String, onResult: (String, Boolean) -> Unit) -> Unit)? = null,
    onAddSkt: (Product, Long, Int) -> Unit = { _, _, _ -> },
    onDeductStock: (Product, Int, String) -> Unit = { _, _, _ -> }
) {
    var manualBarcode by remember { mutableStateOf("") }
    var activeBarcode by remember { mutableStateOf("") }
    var isFixQrMode by remember { mutableStateOf(startInFixQrMode) }
    val isSerialScanMode = true
    var isBarcodeTooFar by remember { mutableStateOf(false) }
    var serialScanCount by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var qrFixResultMsg by remember { mutableStateOf("") }
    var qrFixSuccessCount by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var qrFixErrorCount by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var qrFixLastTime by remember { mutableStateOf("") }
    var qrFixLastInfo by remember { mutableStateOf<String?>(null) }
    var qrFixStoreCode by remember { mutableStateOf("D724") }
    val qrFixHistoryList = remember { mutableStateListOf<com.example.ui.screens.scanner.QrFixHistoryItem>() }
    var selectedProductOverride by remember { mutableStateOf<Product?>(null) }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    var lastScannedTime by remember { mutableStateOf(0L) }
    var lastScannedRisk by remember { mutableStateOf<ScanResultRisk?>(null) }
    var lastRemainingDays by remember { mutableStateOf<Long?>(null) }
    var resumeCooldownUntil by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var isCooldownActive by remember { mutableStateOf(false) }
    var cooldownRemainingSeconds by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    val todayMidnight = remember {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    LaunchedEffect(resumeCooldownUntil) {
        val remaining = resumeCooldownUntil - System.currentTimeMillis()
        if (remaining > 0) {
            isCooldownActive = true
            while (true) {
                val remMs = resumeCooldownUntil - System.currentTimeMillis()
                if (remMs <= 0) break
                cooldownRemainingSeconds = ((remMs + 999L) / 1000L).toInt()
                kotlinx.coroutines.delay(300L)
            }
            cooldownRemainingSeconds = 0
            isCooldownActive = false
        } else {
            cooldownRemainingSeconds = 0
            isCooldownActive = false
        }
    }
    var isFlashOn by remember { mutableStateOf(false) }
    var zoomRatio by remember { mutableStateOf(1.0f) }

    val toneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        } catch (e: Exception) {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            isFlashOn = false
            try {
                toneGenerator?.release()
            } catch (e: Exception) {
                // Ignore audio release error
            }
        }
    }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val safeDismiss = {
        isFlashOn = false
        focusManager.clearFocus()
        keyboardController?.hide()
        onDismiss()
    }

    val isKeyboardVisible = WindowInsets.isImeVisible

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // Debounce manual search input to prevent UI lag/freezing while typing
    LaunchedEffect(manualBarcode) {
        if (manualBarcode.isBlank()) {
            if (activeBarcode.isNotBlank()) {
                activeBarcode = ""
            }
        } else {
            kotlinx.coroutines.delay(220)
            activeBarcode = manualBarcode.trim()
        }
    }

    // FULL SCREEN DIALOG (Tam Sayfa)
    Dialog(
        onDismissRequest = safeDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // =========================================================================
                // 1. TOP SECTION: CAMERA PREVIEW & TARGETING VIEWFINDER
                // =========================================================================
                val hasProductDetail = activeBarcode.isNotBlank()
                // In serial scan mode, camera remains live and continuously scans close barcodes
                val isScannerPaused = (isFixQrMode && qrFixResultMsg.isNotBlank())
                val cameraWeight = if (isKeyboardVisible) 0.16f else if (hasProductDetail) 0.32f else 0.65f
                val bottomWeight = if (isKeyboardVisible) 0.84f else if (hasProductDetail) 0.68f else 0.35f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(cameraWeight)
                        .heightIn(min = 80.dp)
                        .background(Color(0xFF0D121F))
                ) {
                    // CAMERA PREVIEW (Continuous Scanning with Proximity Requirement)
                    if (cameraPermissionState.status.isGranted) {
                        CameraXBarcodeView(
                            isFlashOn = isFlashOn,
                            zoomRatio = zoomRatio,
                            filterMode = ScannerFilterMode.ALL,
                            isBatterySaverMode = isBatterySaverMode,
                            isPaused = isScannerPaused || isCooldownActive,
                            requireCloseDistance = true,
                            onDistanceStateChanged = { tooFar ->
                                isBarcodeTooFar = tooFar
                            },
                            onBarcodeScanned = { barcode ->
                                val now = System.currentTimeMillis()
                                if (now < resumeCooldownUntil || isCooldownActive) {
                                    return@CameraXBarcodeView
                                }
                                if (isScannerPaused || (isFixQrMode && qrFixResultMsg.isNotBlank())) {
                                    return@CameraXBarcodeView
                                }
                                val trimmedBar = barcode.trim()
                                if (trimmedBar.isNotBlank()) {
                                    // Debounce to prevent rapid repeated scans of same barcode (1800ms)
                                    val isSameRecent = lastScannedCode == trimmedBar && (now - lastScannedTime) < 1800L

                                    if (!isSameRecent) {
                                        val (risk, remainingDays) = ScannerFeedbackHelper.evaluateProductRisk(trimmedBar, products, todayMidnight)
                                        ScannerFeedbackHelper.playFeedback(
                                            context = context,
                                            toneGenerator = toneGenerator,
                                            risk = risk
                                        )
                                        lastScannedRisk = risk
                                        lastRemainingDays = remainingDays
                                        lastScannedCode = trimmedBar
                                        lastScannedTime = now
                                        activeBarcode = trimmedBar
                                        manualBarcode = trimmedBar
                                        selectedProductOverride = null
                                        serialScanCount++
                                        resumeCooldownUntil = now + 1200L

                                        if (isFixQrMode && onFixQrScanned != null) {
                                            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                            val formattedTime = timeFormat.format(Date(now))
                                            qrFixLastTime = formattedTime
                                            val shelfData = parseShelfQrPayload(trimmedBar)
                                            val sc = shelfData.storeCode
                                            if (!sc.isNullOrBlank()) {
                                                qrFixStoreCode = sc
                                            }
                                            val realBarcode = shelfData.barcode.ifBlank { trimmedBar }
                                            val pCode = shelfData.productCode
                                            val matchedProd = products.firstOrNull { p ->
                                                (realBarcode.isNotBlank() && p.barkod.equals(realBarcode, ignoreCase = true)) ||
                                                (pCode != null && pCode.isNotBlank() && p.urunKodu.equals(pCode, ignoreCase = true)) ||
                                                p.urunKodu.equals(realBarcode, ignoreCase = true)
                                            }
                                            val prodName = matchedProd?.urunAdi ?: shelfData.productName ?: "Barkod: $realBarcode"

                                            onFixQrScanned(trimmedBar) { msg, isSuccess ->
                                                qrFixResultMsg = msg
                                                qrFixLastInfo = msg
                                                if (isSuccess) {
                                                    qrFixSuccessCount++
                                                } else {
                                                    qrFixErrorCount++
                                                }
                                                qrFixHistoryList.add(
                                                    0,
                                                    com.example.ui.screens.scanner.QrFixHistoryItem(
                                                        time = formattedTime,
                                                        barcode = realBarcode,
                                                        productCode = pCode ?: matchedProd?.urunKodu,
                                                        productName = prodName,
                                                        message = msg,
                                                        isSuccess = isSuccess
                                                    )
                                                )
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

                    val viewfinderColor = when {
                        isBarcodeTooFar -> SoonYellow
                        lastScannedRisk != null -> lastScannedRisk!!.color
                        isScannerPaused -> SoonYellow
                        else -> TurquoisePrimary
                    }
                    val isViewfinderGlowing = isBarcodeTooFar || lastScannedRisk != null || isScannerPaused

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Prominent Corner Brackets Target Box with Top Guidance Badge
                        if (!isKeyboardVisible) {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Status / Proximity Guidance Badge (Positioned ABOVE the viewfinder box)
                                if (isCooldownActive && cooldownRemainingSeconds > 0) {
                                    Surface(
                                        onClick = {
                                            resumeCooldownUntil = 0L
                                            isCooldownActive = false
                                            cooldownRemainingSeconds = 0
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF0F172A).copy(alpha = 0.90f),
                                        border = BorderStroke(1.dp, TurquoisePrimary),
                                        shadowElevation = 6.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "⏳ Bekleme: ${cooldownRemainingSeconds}sn (Dokun: Hemen Oku)",
                                                color = TurquoisePrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                } else if (isBarcodeTooFar) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = SoonYellow.copy(alpha = 0.95f),
                                        shadowElevation = 6.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "📏 Barkoda Yaklaşın",
                                                color = Color(0xFF1A1A1A),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                } else if (lastScannedRisk != null) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = lastScannedRisk!!.color,
                                        shadowElevation = 6.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = lastScannedRisk!!.title + (if (lastRemainingDays != null && lastRemainingDays!! >= 0) " (${lastRemainingDays} Gün)" else ""),
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = NormalGreen.copy(alpha = 0.90f),
                                        shadowElevation = 4.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isFixQrMode) "⚡ Raf QR Kodu Okuma Modu" else "Otomatik Tarama Modu",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Unobstructed Clean Viewfinder Frame
                                Box(
                                    modifier = Modifier
                                        .width(animatedFrameWidth)
                                        .height(animatedFrameHeight)
                                ) {
                                    CornerBracketsViewfinder(
                                        modifier = Modifier.fillMaxSize(),
                                        color = viewfinderColor,
                                        strokeWidth = 5.dp,
                                        cornerLength = 32.dp,
                                        cornerRadius = 16.dp,
                                        isGlowing = isViewfinderGlowing
                                    )
                                }
                            }
                        }
                    }

                    // TOP OVERLAY BAR: UNIFORM DARK PILL/CIRCLE BUTTONS & SEGMENTED MODE SELECTOR
                    ScannerTopControls(
                        isFixQrMode = isFixQrMode,
                        isFlashOn = isFlashOn,
                        onCloseClick = safeDismiss,
                        onModeChange = { isFixMode ->
                            isFixQrMode = isFixMode
                            qrFixResultMsg = ""
                        },
                        onFlashToggle = { isFlashOn = !isFlashOn }
                    )
                }

                // =========================================================================
                // 2. BOTTOM SECTION: MANUAL SEARCH & PRODUCT DETAILS
                // =========================================================================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(bottomWeight),
                    shape = RectangleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    if (isFixQrMode) {
                        QrFixSummaryPanel(
                            storeCode = qrFixStoreCode,
                            userName = userName,
                            lastProcessTime = qrFixLastTime,
                            totalScannedCount = qrFixSuccessCount + qrFixErrorCount,
                            successCount = qrFixSuccessCount,
                            errorCount = qrFixErrorCount,
                            lastProcessedInfo = qrFixLastInfo,
                            historyList = qrFixHistoryList,
                            onClearHistory = { qrFixHistoryList.clear() }
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Top Grip Bar Indicator
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 6.dp)
                                    .width(32.dp)
                                    .height(3.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                            )

                            // HEADER: TITLE & SUBTITLE
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSerialScanMode) TurquoisePrimary.copy(alpha = 0.20f) else TurquoisePrimary.copy(alpha = 0.15f),
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = null,
                                                tint = TurquoisePrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "⚡ Seri Barkod Okuma Modu",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Telefonu barkoda yaklaştırarak sırayla okutabilirsiniz",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isSerialScanMode && serialScanCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = TurquoisePrimary.copy(alpha = 0.18f),
                                        border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = "$serialScanCount Ürün Okundu",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TurquoisePrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // MANUAL ENTRY SEARCH BAR (ALWAYS VISIBLE & EDITABLE - NEVER VANISHES)
                            ScannerManualSearchBar(
                                manualBarcode = manualBarcode,
                                onValueChange = {
                                    manualBarcode = it
                                    selectedProductOverride = null
                                },
                                onClear = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    manualBarcode = ""
                                    activeBarcode = ""
                                    selectedProductOverride = null
                                    lastScannedCode = null
                                    lastScannedTime = 0L
                                    lastScannedRisk = null
                                    qrFixResultMsg = ""
                                    resumeCooldownUntil = System.currentTimeMillis() + 600L
                                },
                                onSearch = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    if (manualBarcode.isNotBlank()) {
                                        activeBarcode = manualBarcode.trim()
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // PRODUCT DETAILS AREA ("ÜRÜN AYRINTI YERİ")
                            ScannerProductDetailsSection(
                                activeBarcode = activeBarcode,
                                products = products,
                                selectedProductOverride = selectedProductOverride,
                                todayMidnight = todayMidnight,
                                onSelectOverride = { selectedProductOverride = it },
                                onAddSkt = onAddSkt,
                                onDeductStock = onDeductStock,
                                onBarcodeDetected = onBarcodeDetected,
                                onClearDetail = {
                                    activeBarcode = ""
                                    manualBarcode = ""
                                    selectedProductOverride = null
                                    lastScannedCode = null
                                    lastScannedTime = 0L
                                    lastScannedRisk = null
                                    resumeCooldownUntil = 0L
                                    isCooldownActive = false
                                    cooldownRemainingSeconds = 0
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

