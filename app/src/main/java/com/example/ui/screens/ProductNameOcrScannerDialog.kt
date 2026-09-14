package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.NormalGreen
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoisePrimary
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale
import java.util.concurrent.Executors
import java.util.regex.Pattern

/**
 * Supermarket Shelf Label & Product Name OCR Scanner.
 * Features:
 * 1. Compact bottom control panel for maximum camera visibility.
 * 2. Instant manual freeze/lock ("Dondur / Tekrar Tara") and auto-stabilization (locks on 3 stable matches).
 * 3. Aggressive shelf junk filtering (FİYAT, KDV, D724, vb.).
 * 4. Editable OutlinedTextField for quick manual touches before sending.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ProductNameOcrScannerDialog(
    onDismiss: () -> Unit,
    onProductNameDetected: (String) -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var isFlashOn by remember { mutableStateOf(false) }
    var currentDetectedName by remember { mutableStateOf("") }
    var isLocked by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // History of consecutive scanned values for auto-stabilization
    var consecutiveMatchCount by remember { mutableIntStateOf(0) }
    var lastCandidateText by remember { mutableStateOf("") }

    val toneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (_: Exception) {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            isFlashOn = false
            try {
                toneGenerator?.release()
            } catch (_: Exception) {}
        }
    }

    val safeDismiss = {
        isFlashOn = false
        onDismiss()
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    val viewfinderColor by animateColorAsState(
        targetValue = if (isLocked) NormalGreen else TurquoisePrimary,
        animationSpec = tween(300),
        label = "viewfinderColor"
    )

    Dialog(
        onDismissRequest = safeDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Slate900
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                // =========================================================================
                // 1) TOP CAMERA VIEWPORT (EXPANDED TO OCCUPY ~75% OF SCREEN)
                // =========================================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF0B101B))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            // Tap on camera toggles lock/freeze
                            if (currentDetectedName.isNotBlank()) {
                                isLocked = !isLocked
                                if (isLocked) {
                                    try { toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 70) } catch (_: Exception) {}
                                }
                            }
                        }
                ) {
                    if (cameraPermissionState.status.isGranted) {
                        CameraXProductNameOcrView(
                            isFlashOn = isFlashOn,
                            isLocked = isLocked,
                            onParsedResult = { bestCandidate ->
                                if (!isLocked && bestCandidate.isNotBlank()) {
                                    // Similarity check with last candidate
                                    val sim = calculateTextSimilarity(lastCandidateText, bestCandidate)
                                    if (sim >= 0.75) {
                                        consecutiveMatchCount++
                                        // If 3 consecutive stable reads occur, auto-lock to stop flickering
                                        if (consecutiveMatchCount >= 3) {
                                            currentDetectedName = bestCandidate
                                            isLocked = true
                                            try { toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 80) } catch (_: Exception) {}
                                        } else if (currentDetectedName.isBlank() || currentDetectedName.length < bestCandidate.length) {
                                            currentDetectedName = bestCandidate
                                        }
                                    } else {
                                        consecutiveMatchCount = 1
                                        lastCandidateText = bestCandidate
                                        if (currentDetectedName.isBlank() || sim < 0.4) {
                                            currentDetectedName = bestCandidate
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
                                contentDescription = "Kamera",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Etiket metni okumak için kamera izni gerekiyor",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
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

                    // Shelf Label Viewfinder Frame with Status Badge
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Viewfinder Top Badge
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isLocked) NormalGreen.copy(alpha = 0.95f) else Color.Black.copy(alpha = 0.75f),
                            border = BorderStroke(1.dp, viewfinderColor.copy(alpha = 0.8f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isLocked) "🔒 METİN DONDURULDU (KİLİTLİ)" else "⚡ Canlı Okuma • Ekrana dokunarak dondur",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Target Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .border(2.5.dp, viewfinderColor, RoundedCornerShape(16.dp))
                                .background(viewfinderColor.copy(alpha = 0.06f))
                        )
                    }

                    // Top Bar Header Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = safeDismiss,
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier.size(42.dp)
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

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TextFields,
                                    contentDescription = null,
                                    tint = TurquoisePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "🏷️ ETİKET İSMİ OKUYUCU",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Surface(
                            onClick = { isFlashOn = !isFlashOn },
                            shape = CircleShape,
                            color = if (isFlashOn) Color(0xFFFFD54F) else Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "Flaş",
                                    tint = if (isFlashOn) Slate900 else Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // =========================================================================
                // 2) COMPACT BOTTOM CONTROL PANEL (SINGLE RESULT BOX & DUAL BUTTONS)
                // =========================================================================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = Slate900,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    border = BorderStroke(1.dp, Slate800)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Result Header with Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "OKUNAN ÜRÜN İSMİ:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                color = TurquoisePrimary
                            )
                            if (isLocked) {
                                Text(
                                    text = "🔒 Sabitlendi (Düzenlenebilir)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NormalGreen
                                )
                            } else if (currentDetectedName.isNotBlank()) {
                                Text(
                                    text = "⚡ Canlı Okunuyor...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFFD54F)
                                )
                            }
                        }

                        // Editable Result TextField
                        OutlinedTextField(
                            value = currentDetectedName,
                            onValueChange = {
                                currentDetectedName = it.uppercase(Locale.forLanguageTag("tr-TR"))
                                isLocked = true // User manual editing locks camera overwrite
                            },
                            placeholder = {
                                Text("Etiket üzerindeki metin taranıyor...", color = Color.Gray, fontSize = 13.sp)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ocr_detected_product_name_input"),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            ),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = if (isLocked) NormalGreen else TurquoisePrimary,
                                unfocusedBorderColor = if (isLocked) NormalGreen.copy(alpha = 0.6f) else Slate700,
                                focusedContainerColor = Slate800,
                                unfocusedContainerColor = Slate800
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Dual Action Buttons: [Dondur / Tekrar Tara] and [Bu İsmi Kullan]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Freeze / Rescan Button
                            Button(
                                onClick = {
                                    if (isLocked) {
                                        // Unlock to rescan
                                        isLocked = false
                                        consecutiveMatchCount = 0
                                        lastCandidateText = ""
                                    } else {
                                        // Freeze current read
                                        isLocked = true
                                        try { toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 70) } catch (_: Exception) {}
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("ocr_freeze_rescan_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isLocked) Slate800 else TurquoisePrimary.copy(alpha = 0.25f)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isLocked) Slate700 else TurquoisePrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLocked) Icons.Default.Refresh else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isLocked) Color.White else TurquoisePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isLocked) "TEKRAR TARA" else "DONDUR",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLocked) Color.White else TurquoisePrimary,
                                    fontSize = 12.5.sp
                                )
                            }

                            // Use This Name Button
                            Button(
                                onClick = {
                                    val cleaned = currentDetectedName.trim()
                                    if (cleaned.isNotBlank()) {
                                        isFlashOn = false
                                        try {
                                            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                                        } catch (_: Exception) {}
                                        onProductNameDetected(cleaned)
                                    }
                                },
                                enabled = currentDetectedName.trim().isNotBlank(),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                                    .testTag("confirm_ocr_product_name_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TurquoisePrimary,
                                    disabledContainerColor = Slate800
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Kullan",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "BU İSMİ KULLAN",
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontSize = 12.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraXProductNameOcrView(
    isFlashOn: Boolean,
    isLocked: Boolean,
    onParsedResult: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val cameraProviderRef = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val cameraRef = remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    val lastAnalyzedTimeRef = remember { java.util.concurrent.atomic.AtomicLong(0L) }
    val previewViewRef = remember { mutableStateOf<PreviewView?>(null) }
    val previewUseCaseRef = remember { mutableStateOf<Preview?>(null) }
    val imageAnalysisRef = remember { mutableStateOf<ImageAnalysis?>(null) }

    fun rebindCamera() {
        val provider = cameraProviderRef.value ?: return
        val preview = previewUseCaseRef.value ?: return
        val analysis = imageAnalysisRef.value ?: return
        try {
            provider.unbindAll()
            val camera = provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )
            cameraRef.value = camera
            camera.cameraControl.enableTorch(isFlashOn)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    LaunchedEffect(isFlashOn) {
        try {
            cameraRef.value?.cameraControl?.enableTorch(isFlashOn)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    try {
                        cameraRef.value?.cameraControl?.enableTorch(false)
                        cameraProviderRef.value?.unbindAll()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    rebindCamera()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                cameraRef.value?.cameraControl?.enableTorch(false)
            } catch (_: Exception) {}
            try {
                imageAnalysisRef.value?.clearAnalyzer()
                cameraProviderRef.value?.unbindAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                recognizer.close()
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
            previewViewRef.value = previewView
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProviderRef.value = cameraProvider
                    val preview = Preview.Builder().build().apply {
                        setSurfaceProvider(previewView.surfaceProvider)
                    }
                    previewUseCaseRef.value = preview

                    val resolutionSelector = ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(1280, 720),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                            )
                        )
                        .build()

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    imageAnalysisRef.value = imageAnalysis

                    val minFrameIntervalMs = 200L

                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        if (isLocked) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val currentTime = System.currentTimeMillis()
                        val lastAnalyzed = lastAnalyzedTimeRef.get()
                        if (currentTime - lastAnalyzed < minFrameIntervalMs) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        lastAnalyzedTimeRef.set(currentTime)

                        processImageForProductName(recognizer, imageProxy) { bestCandidate ->
                            if (bestCandidate.isNotBlank()) {
                                onParsedResult(bestCandidate)
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
private fun processImageForProductName(
    recognizer: com.google.mlkit.vision.text.TextRecognizer,
    imageProxy: ImageProxy,
    onParsedResult: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val fullText = visionText.text
                if (fullText.isNotBlank()) {
                    val (bestCandidate, _) = extractProductNameAndGramaj(visionText)
                    if (bestCandidate.isNotBlank()) {
                        onParsedResult(bestCandidate)
                    }
                }
            }
            .addOnFailureListener {
                // ignore
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

/**
 * Filter list of keywords that commonly appear on Turkish supermarket price tags & shelf labels
 * but are NOT part of the product name.
 */
private val EXCLUDED_LABEL_KEYWORDS = listOf(
    "FIYAT", "FİYAT", "FIYATI", "FİYATI",
    "GECERLILIK", "GEÇERLİLİK", "GECERLILIK TARIHI", "GEÇERLİLİK TARİHİ",
    "TARIHI", "TARİHİ", "TARIH", "TARİH",
    "KDV", "DAHIL", "DAHİL", "DAHILDIR", "DAHİLDİR", "HARIC", "HARİÇ",
    "MENSEI", "MENŞEİ", "MENSE", "MENŞE",
    "TURKIYE", "TÜRKİYE", "TURKİYE", "TURKIYE'DE", "TÜRKİYE'DE",
    "URETIM", "ÜRETİM", "URETIM YERI", "ÜRETİM YERİ",
    "YERLI", "YERLİ", "YERLI URETIM", "YERLİ ÜRETİM",
    "PARTI", "PARTİ", "SERI", "SERİ", "LOT", "NO",
    "ICINDEKILER", "İÇİNDEKİLER", "ALERJEN",
    "TAVSIYE", "TAVSİYE", "TETT", "SKT", "SON TUKETIM", "SON TÜKETİM",
    "URUN KODU", "ÜRÜN KODU", "BARKOD", "BARKOD NO",
    "ISLETME", "İŞLETME", "KAYIT", "ONAY",
    "1 KG =", "1 LT =", "1 ADET =", "100 G =", "100 ML =", "BIRIM FIYAT", "BİRİM FİYAT", "BIRIM FIYATI", "BİRİM FİYATI",
    "TL", "KRŞ", "KRS", "KURUS", "KURUŞ", "₺",
    "INDIRIM", "İNDİRİM", "KAMPANYA", "ETIKET", "ETİKET",
    "RAF FIYATI", "RAF FİYATI", "SATIS FIYATI", "SATIŞ FİYATI",
    "MAGAZA", "MAĞAZA", "SUBE", "ŞUBE", "REYON",
    "D724", "D-724", "M101", "A101", "BIM", "SOK", "ŞOK", "CARREFOUR", "MIGROS", "MİGROS"
)

private val GRAMAJ_PATTERN = Pattern.compile(
    "\\b(\\d+([.,]\\d+)?\\s*(?:KG|GR|GRAM|G|L|LT|LITRE|LİTRE|ML|CL|ADET|PK|PAKET|'L[IUÜİ]|X\\s*\\d+\\s*(?:ML|G|L|GR)?))\\b",
    Pattern.CASE_INSENSITIVE
)

/**
 * Extracts the cleanest product name and gramaj from ML Kit VisionText
 */
fun extractProductNameAndGramaj(visionText: com.google.mlkit.vision.text.Text): Pair<String, List<String>> {
    val rawLines = mutableListOf<String>()
    val suggestions = mutableListOf<String>()

    for (block in visionText.textBlocks) {
        for (line in block.lines) {
            val lineWords = line.elements.map { it.text.trim() }.filter { it.isNotBlank() }
            val lineText = if (lineWords.isNotEmpty()) {
                lineWords.joinToString(" ")
            } else {
                line.text.trim()
            }.replace(Regex("\\s+"), " ")

            if (lineText.isNotBlank() && lineText.length >= 2) {
                rawLines.add(lineText)
            }
        }
    }

    if (rawLines.isEmpty() && visionText.text.isNotBlank()) {
        rawLines.addAll(
            visionText.text
                .split("\n", "\r")
                .map { it.trim().replace(Regex("\\s+"), " ") }
                .filter { it.length >= 2 }
        )
    }

    var extractedGramaj: String? = null
    val filteredProductLines = mutableListOf<String>()

    for (rawLine in rawLines) {
        val upperTr = rawLine.uppercase(Locale.forLanguageTag("tr-TR"))

        // 1) Filter shelf codes & barcodes (e.g., D724-8690574117291-275,00-)
        if (upperTr.contains(Regex("\\b[A-Z0-9]{3,8}-[0-9]{8,14}")) ||
            upperTr.matches(Regex("^[0-9\\-/.]{6,}$"))
        ) {
            continue
        }

        // 2) Filter date stamps like 01.01.2025 or 12/2026
        if (upperTr.matches(Regex(".*\\b\\d{1,2}[./\\-]\\d{1,2}[./\\-]\\d{2,4}\\b.*")) &&
            !upperTr.contains(Regex("[A-ZĞÜŞİÖÇ]{4,}"))
        ) {
            continue
        }

        // 3) Filter standalone price stamps like "275,00 TL" or "275.00"
        if (rawLine.matches(Regex("^[0-9.,\\s]+(TL|₺|kr|krş)?$", RegexOption.IGNORE_CASE))) {
            continue
        }

        // 4) Check if line is predominantly junk label keywords
        val words = upperTr.split(" ").filter { it.isNotBlank() }
        val junkWordCount = words.count { word ->
            EXCLUDED_LABEL_KEYWORDS.any { kw -> word == kw || word.startsWith(kw) || kw.startsWith(word) }
        }
        if (words.isNotEmpty() && (junkWordCount.toDouble() / words.size.toDouble()) >= 0.5) {
            continue
        }

        // 5) Search for gramaj pattern
        val mat = GRAMAJ_PATTERN.matcher(rawLine)
        if (mat.find()) {
            val foundGramaj = mat.group(1)?.trim()
            if (!foundGramaj.isNullOrBlank() && extractedGramaj == null) {
                extractedGramaj = foundGramaj.uppercase(Locale.forLanguageTag("tr-TR"))
            }
        }

        // 6) Clean isolated junk words from the line itself
        val cleanedWords = words.filterNot { word ->
            val w = word.trim(',', '.', ':', ';', '-', '!', '?', '(', ')')
            EXCLUDED_LABEL_KEYWORDS.contains(w)
        }
        val cleanedLine = cleanedWords.joinToString(" ").trim()

        if (cleanedLine.isNotBlank() && cleanedLine.length >= 2 && !cleanedLine.all { it.isDigit() }) {
            filteredProductLines.add(cleanedLine)
            suggestions.add(cleanedLine)
        }
    }

    // Build best candidate
    val combinedCandidate = when {
        filteredProductLines.isEmpty() -> ""
        filteredProductLines.size == 1 -> filteredProductLines[0]
        else -> {
            filteredProductLines.take(3).joinToString(" ").replace(Regex("\\s+"), " ").trim()
        }
    }

    // Ensure gramaj is included if found and not yet part of candidate
    val finalBestCandidate = if (!extractedGramaj.isNullOrBlank() && combinedCandidate.isNotBlank()) {
        val cleanGramajNorm = extractedGramaj.replace(" ", "")
        val upperCombinedNorm = combinedCandidate.uppercase(Locale.forLanguageTag("tr-TR")).replace(" ", "")

        if (!upperCombinedNorm.contains(cleanGramajNorm)) {
            "$combinedCandidate $extractedGramaj"
        } else {
            combinedCandidate
        }
    } else {
        combinedCandidate
    }

    if (finalBestCandidate.isNotBlank() && !suggestions.contains(finalBestCandidate)) {
        suggestions.add(0, finalBestCandidate)
    }

    return Pair(finalBestCandidate, suggestions.distinct())
}

/**
 * Calculates word-based Jaccard similarity between two strings
 */
private fun calculateTextSimilarity(s1: String, s2: String): Double {
    if (s1 == s2) return 1.0
    if (s1.isBlank() || s2.isBlank()) return 0.0
    val words1 = s1.uppercase(Locale.forLanguageTag("tr-TR")).split(" ").filter { it.isNotBlank() }.toSet()
    val words2 = s2.uppercase(Locale.forLanguageTag("tr-TR")).split(" ").filter { it.isNotBlank() }.toSet()
    if (words1.isEmpty() || words2.isEmpty()) return 0.0
    val intersection = words1.intersect(words2).size
    val union = words1.union(words2).size
    return intersection.toDouble() / union.toDouble()
}
