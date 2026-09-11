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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
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
 * High-precision Camera OCR scanner specifically designed to extract Product Name & Gramaj
 * from supermarket shelf labels and product packaging.
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalLayoutApi::class)
@Composable
fun ProductNameOcrScannerDialog(
    onDismiss: () -> Unit,
    onProductNameDetected: (String) -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var isFlashOn by remember { mutableStateOf(false) }
    var currentDetectedName by remember { mutableStateOf("") }
    val detectedSuggestions = remember { mutableStateListOf<String>() }
    val focusManager = LocalFocusManager.current

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
                // Ignore
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
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
                modifier = Modifier.fillMaxSize()
            ) {
                // 1) Top Camera Viewport with Viewfinder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.58f)
                        .background(Color(0xFF0B101B))
                ) {
                    if (cameraPermissionState.status.isGranted) {
                        CameraXProductNameOcrView(
                            isFlashOn = isFlashOn,
                            onParsedResult = { bestCandidate, suggestions ->
                                if (bestCandidate.isNotBlank()) {
                                    if (currentDetectedName.isBlank() || currentDetectedName.length < bestCandidate.length) {
                                        currentDetectedName = bestCandidate
                                    }
                                }
                                suggestions.forEach { item ->
                                    if (item.isNotBlank() && !detectedSuggestions.contains(item)) {
                                        if (detectedSuggestions.size >= 8) {
                                            detectedSuggestions.removeAt(0)
                                        }
                                        detectedSuggestions.add(item)
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

                    // Shelf Label Viewfinder Frame
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .height(120.dp)
                            .align(Alignment.Center)
                            .border(2.5.dp, TurquoisePrimary, RoundedCornerShape(16.dp))
                            .background(TurquoisePrimary.copy(alpha = 0.06f))
                    ) {
                        Text(
                            text = "🏷️ Etiket İsim & Gramajını Bu Alana Hizalayın",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 6.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    // Top Bar Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = onDismiss,
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
                                    text = "🏷️ ETİKET METNİ OKUYUCU",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.5.sp
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

                // 2) Bottom Control & Suggestions Panel
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.42f),
                    color = Slate900,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Title / Instruction
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "OKUNAN ÜRÜN İSMİ & GRAMAJ:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TurquoisePrimary
                            )
                            if (currentDetectedName.isNotBlank()) {
                                Text(
                                    text = "✓ Metin Yakalandı",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }

                        // Editable input field for product name
                        OutlinedTextField(
                            value = currentDetectedName,
                            onValueChange = { currentDetectedName = it.uppercase(java.util.Locale.forLanguageTag("tr-TR")) },
                            placeholder = { Text("Kamera etiket üzerindeki metni okuyor...", color = Color.Gray, fontSize = 13.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ocr_detected_product_name_input"),
                            singleLine = false,
                            maxLines = 2,
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
                                focusedBorderColor = TurquoisePrimary,
                                unfocusedBorderColor = Slate700,
                                focusedContainerColor = Slate800,
                                unfocusedContainerColor = Slate800
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Quick Pick Chips from OCR suggestions
                        if (detectedSuggestions.isNotEmpty()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TouchApp,
                                        contentDescription = null,
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Dokunarak Hızlıca Seç:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.LightGray
                                    )
                                }
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    detectedSuggestions.take(6).forEach { chipText ->
                                        Surface(
                                            onClick = {
                                                currentDetectedName = chipText
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (currentDetectedName == chipText) TurquoiseDark else Slate800,
                                            border = BorderStroke(
                                                1.dp,
                                                if (currentDetectedName == chipText) TurquoisePrimary else Slate700
                                            )
                                        ) {
                                            Text(
                                                text = chipText,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Action Buttons: Cancel and Confirm
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(0.7f)
                                    .height(46.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("İPTAL", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.5.sp)
                            }

                            Button(
                                onClick = {
                                    val cleaned = currentDetectedName.trim()
                                    if (cleaned.isNotBlank()) {
                                        try {
                                            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                                        } catch (e: Exception) {
                                            // Ignore
                                        }
                                        onProductNameDetected(cleaned)
                                    }
                                },
                                enabled = currentDetectedName.trim().isNotBlank(),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(46.dp)
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
                                    fontSize = 13.sp
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
    onParsedResult: (String, List<String>) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val cameraProviderRef = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val cameraRef = remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    val lastAnalyzedTimeRef = remember { java.util.concurrent.atomic.AtomicLong(0L) }

    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

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
                // Ignore
            }
            try {
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
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProviderRef.value = cameraProvider
                    val preview = Preview.Builder().build().apply {
                        setSurfaceProvider(previewView.surfaceProvider)
                    }

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

                    val minFrameIntervalMs = 250L // Throttles OCR analysis to ~4 FPS to prevent rate limit and buffer overrun

                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        val currentTime = System.currentTimeMillis()
                        val lastAnalyzed = lastAnalyzedTimeRef.get()
                        if (currentTime - lastAnalyzed < minFrameIntervalMs) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        lastAnalyzedTimeRef.set(currentTime)

                        processImageForProductName(recognizer, imageProxy, onParsedResult)
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
    onParsedResult: (String, List<String>) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val fullText = visionText.text
                if (fullText.isNotBlank()) {
                    val (bestCandidate, suggestions) = extractProductNameAndGramaj(visionText)
                    if (bestCandidate.isNotBlank() || suggestions.isNotEmpty()) {
                        onParsedResult(bestCandidate, suggestions)
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
    "FIYAT", "FİYAT", "GECERLILIK", "GEÇERLİLİK", "TARIHI", "TARİHİ", "TARIH", "TARİH",
    "KDV", "DAHIL", "DAHİL", "HARIC", "HARİÇ", "MENSEI", "MENŞEİ", "TURKIYE", "TÜRKİYE",
    "URETIM", "ÜRETİM", "PARTI", "PARTİ", "SERI", "SERİ", "ICINDEKILER", "İÇİNDEKİLER",
    "ALERJEN", "TAVSIYE", "TAVSİYE", "TETT", "SKT", "SON TUKETIM", "SON TÜKETİM",
    "URUN KODU", "ÜRÜN KODU", "BARKOD", "ISLETME", "İŞLETME", "KAYIT", "ONAY",
    "1 KG =", "1 LT =", "1 ADET =", "100 G =", "100 ML =", "BIRIM FIYAT", "BİRİM FİYAT",
    "TL", "KRŞ", "KURUS", "KURUŞ", "INDIRIM", "İNDİRİM", "KAMPANYA", "ETIKET", "ETİKET",
    "RAF FIYATI", "RAF FİYATI", "SATIS FIYATI", "SATIŞ FİYATI", "MAĞAZA", "MAGAZA"
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
            val lineText = line.text.trim()
            if (lineText.isNotBlank() && lineText.length >= 2) {
                rawLines.add(lineText)
            }
        }
    }

    if (rawLines.isEmpty() && visionText.text.isNotBlank()) {
        rawLines.addAll(visionText.text.split("\n", "\r").map { it.trim() }.filter { it.length >= 2 })
    }

    var extractedGramaj: String? = null
    val filteredProductLines = mutableListOf<String>()

    for (rawLine in rawLines) {
        val upperTr = rawLine.uppercase(Locale.forLanguageTag("tr-TR"))

        // Check if line contains shelf metadata to exclude
        val isMetadata = EXCLUDED_LABEL_KEYWORDS.any { kw ->
            upperTr.contains(kw)
        }

        // Check if line is purely digits/hyphens/codes (e.g. 12003526-8699118079484-DE-7)
        val digitAndSymbolCount = rawLine.count { it.isDigit() || it == '-' || it == '.' || it == '/' }
        val isMostlyCode = (digitAndSymbolCount.toDouble() / rawLine.length) > 0.65

        // Check for isolated price like "129,90 TL" or "785,71"
        val isPriceOnly = rawLine.matches(Regex("^[0-9.,\\s]+(TL|₺|kr)?$", RegexOption.IGNORE_CASE))

        if (isMetadata || isMostlyCode || isPriceOnly) {
            continue
        }

        // Search for gramaj in this line
        val mat = GRAMAJ_PATTERN.matcher(rawLine)
        if (mat.find()) {
            val foundGramaj = mat.group(1)?.trim()
            if (!foundGramaj.isNullOrBlank() && extractedGramaj == null) {
                extractedGramaj = foundGramaj.uppercase(Locale.forLanguageTag("tr-TR"))
            }
        }

        // Clean line from stray punctuation
        val cleanedLine = rawLine
            .replace(Regex("[*#_~|•]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (cleanedLine.isNotBlank() && cleanedLine.length >= 2 && !cleanedLine.all { it.isDigit() }) {
            filteredProductLines.add(cleanedLine)
            suggestions.add(cleanedLine)
        }
    }

    // Build the combined best product name candidate
    val combinedCandidate = when {
        filteredProductLines.isEmpty() -> ""
        filteredProductLines.size == 1 -> filteredProductLines[0]
        else -> {
            // Join lines (e.g. "ÜSTAD" + "KLASİK EZİNE PEYNİRİ" + "350 G")
            val joined = filteredProductLines.take(3).joinToString(" ")
            joined
        }
    }

    // Ensure gramaj is included in candidate if detected and not already present
    val finalBestCandidate = if (!extractedGramaj.isNullOrBlank() && combinedCandidate.isNotBlank()) {
        val upperCombined = combinedCandidate.uppercase(Locale.forLanguageTag("tr-TR"))
        val cleanGramajNorm = extractedGramaj.replace(" ", "")
        val upperCombinedNorm = upperCombined.replace(" ", "")

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
