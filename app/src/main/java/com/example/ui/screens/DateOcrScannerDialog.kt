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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoisePrimary
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.Executors
import java.util.regex.Pattern

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DateOcrScannerDialog(
    onDismiss: () -> Unit,
    onDateDetected: (Long, String) -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var detectedTextDisplay by remember { mutableStateOf("Tarih aranıyor... (Örn: 15/08/2026)") }
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
                // Top Camera View & Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.7f)
                        .background(Color(0xFF0D121F))
                ) {
                    if (cameraPermissionState.status.isGranted) {
                        CameraXDateOcrView(
                            isFlashOn = isFlashOn,
                            onDateFound = { millis, dateStr ->
                                try {
                                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
                                } catch (e: Exception) {
                                    // Ignore audio error
                                }
                                onDateDetected(millis, dateStr)
                            },
                            onTextUpdate = { txt ->
                                if (txt.isNotBlank()) {
                                    detectedTextDisplay = "Algılanan metin: $txt"
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
                                text = "Kamera erişim izni bekleniyor...",
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

                    // Targeting frame without moving line
                    Box(
                        modifier = Modifier
                            .width(280.dp)
                            .height(110.dp)
                            .align(Alignment.Center)
                            .border(2.5.dp, TurquoisePrimary, RoundedCornerShape(16.dp))
                            .background(TurquoisePrimary.copy(alpha = 0.05f))
                    )

                    // Top Bar
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
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Kapat",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "📅 SKT OCR",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = TurquoisePrimary.copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = "OCR",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // FLASH TOGGLE BUTTON (Bottom Right of Camera View)
                    Surface(
                        onClick = { isFlashOn = !isFlashOn },
                        shape = CircleShape,
                        color = if (isFlashOn) TurquoisePrimary else Color.Black.copy(alpha = 0.65f),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 52.dp)
                            .size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Flaş / Işık",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Bottom instruction inside camera area
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = detectedTextDisplay,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Bottom 30% section: Quick test dates
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.3f)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "⚡ Kamerada okunamazsa örnek bir SKT Tarihi seçin:",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TestDateChip(
                            label = "15/08/2026",
                            onClick = {
                                selectTestDate(15, 7, 2026, onDateDetected) // 0-indexed month
                            }
                        )
                        TestDateChip(
                            label = "10/11/2026",
                            onClick = {
                                selectTestDate(10, 10, 2026, onDateDetected)
                            }
                        )
                        TestDateChip(
                            label = "01/03/2027",
                            onClick = {
                                selectTestDate(1, 2, 2027, onDateDetected)
                            }
                        )
                        TestDateChip(
                            label = "25/12/2025",
                            onClick = {
                                selectTestDate(25, 11, 2025, onDateDetected)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("VAZGEÇ / KAPAT", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TestDateChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.5.dp, TurquoisePrimary)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = TurquoisePrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp
            )
        }
    }
}

private fun selectTestDate(day: Int, month0: Int, year: Int, onDateDetected: (Long, String) -> Unit) {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month0)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val formatted = String.format("%02d/%02d/%04d", day, month0 + 1, year)
    onDateDetected(cal.timeInMillis, formatted)
}

@Composable
private fun CameraXDateOcrView(
    isFlashOn: Boolean,
    onDateFound: (Long, String) -> Unit,
    onTextUpdate: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var hasFound by remember { mutableStateOf(false) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val cameraProviderRef = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val cameraRef = remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    val lastAnalyzedTimeRef = remember { java.util.concurrent.atomic.AtomicLong(0L) }
    val previewViewRef = remember { mutableStateOf<PreviewView?>(null) }
    val previewUseCaseRef = remember { mutableStateOf<Preview?>(null) }
    val imageAnalysisRef = remember { mutableStateOf<ImageAnalysis?>(null) }

    fun rebindCamera() {
        if (hasFound) return
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
            } catch (e: Exception) {
                // Ignore torch disable failure
            }
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

                    val minFrameIntervalMs = 250L // Throttles OCR processing to ~4 FPS to prevent rate limit and buffer overrun

                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        if (!hasFound) {
                            val currentTime = System.currentTimeMillis()
                            val lastAnalyzed = lastAnalyzedTimeRef.get()
                            if (currentTime - lastAnalyzed < minFrameIntervalMs) {
                                imageProxy.close()
                                return@setAnalyzer
                            }
                            lastAnalyzedTimeRef.set(currentTime)

                            processImageForDate(recognizer, imageProxy, onTextUpdate) { millis, dateStr ->
                                hasFound = true
                                try {
                                    imageAnalysis.clearAnalyzer()
                                    cameraProvider.unbindAll()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                onDateFound(millis, dateStr)
                            }
                        } else {
                            imageProxy.close()
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
private fun processImageForDate(
    recognizer: com.google.mlkit.vision.text.TextRecognizer,
    imageProxy: ImageProxy,
    onTextUpdate: (String) -> Unit,
    onDateFound: (Long, String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val fullText = visionText.text
                val textPieces = mutableListOf<String>()
                if (fullText.isNotBlank()) textPieces.add(fullText)

                for (block in visionText.textBlocks) {
                    textPieces.add(block.text)
                    for (line in block.lines) {
                        textPieces.add(line.text)
                    }
                }

                val previewSnippet = fullText.replace("\n", " ").trim()
                if (previewSnippet.isNotEmpty()) {
                    onTextUpdate(previewSnippet.take(60))
                }

                for (piece in textPieces) {
                    val found = parseDateString(piece)
                    if (found != null) {
                        onDateFound(found.first, found.second)
                        break
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

private val TURKISH_MONTH_MAP = mapOf(
    "OCAK" to 1, "OCA" to 1, "OCK" to 1, "JAN" to 1, "JANUARY" to 1,
    "SUBAT" to 2, "SUB" to 2, "SBT" to 2, "FEB" to 2, "FEBRUARY" to 2,
    "MART" to 3, "MAR" to 3, "MRT" to 3, "MARCH" to 3,
    "NISAN" to 4, "NIS" to 4, "NSN" to 4, "APR" to 4, "APRIL" to 4,
    "MAYIS" to 5, "MAY" to 5, "MYS" to 5,
    "HAZIRAN" to 6, "HAZ" to 6, "HZR" to 6, "JUN" to 6, "JUNE" to 6,
    "TEMMUZ" to 7, "TEM" to 7, "TMZ" to 7, "JUL" to 7, "JULY" to 7,
    "AGUSTOS" to 8, "AGU" to 8, "AGS" to 8, "AUG" to 8, "AUGUST" to 8,
    "EYLUL" to 9, "EYL" to 9, "SEP" to 9, "SEPTEMBER" to 9,
    "EKIM" to 10, "EKI" to 10, "EKM" to 10, "OCT" to 10, "OCTOBER" to 10,
    "KASIM" to 11, "KAS" to 11, "KSM" to 11, "NOV" to 11, "NOVEMBER" to 11,
    "ARALIK" to 12, "ARA" to 12, "ARL" to 12, "DEC" to 12, "DECEMBER" to 12
)

private fun buildDateResult(day: Int, month: Int, rawYear: Int): Pair<Long, String>? {
    var year = rawYear
    if (year < 100) {
        year += 2000
    }
    if (day in 1..31 && month in 1..12 && year in 2020..2040) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val formatted = String.format(Locale.US, "%02d/%02d/%04d", day, month, year)
        return Pair(cal.timeInMillis, formatted)
    }
    return null
}

private fun parseDateString(rawText: String): Pair<Long, String>? {
    if (rawText.isBlank()) return null

    val lines = rawText.split("\n", "\r", ";").filter { it.isNotBlank() }
    val candidates = mutableListOf<String>()
    candidates.add(rawText.replace("\n", " "))
    candidates.addAll(lines)

    for (candidate in candidates) {
        // 1. Direct numeric date DD.MM.YYYY or DD/MM/YY or DD-MM-YYYY (or spaces)
        val patNumeric = Pattern.compile("(\\d{1,2})[./\\-:,\\s]+(\\d{1,2})[./\\-:,\\s]+(\\d{2,4})")
        val matNumeric = patNumeric.matcher(candidate)
        while (matNumeric.find()) {
            val d = matNumeric.group(1)?.toIntOrNull() ?: continue
            val m = matNumeric.group(2)?.toIntOrNull() ?: continue
            val y = matNumeric.group(3)?.toIntOrNull() ?: continue
            val res = buildDateResult(d, m, y)
            if (res != null) return res
        }

        // 2. Year-First format YYYY.MM.DD
        val patYearFirst = Pattern.compile("(20\\d{2})[./\\-:,\\s]+(\\d{1,2})[./\\-:,\\s]+(\\d{1,2})")
        val matYearFirst = patYearFirst.matcher(candidate)
        while (matYearFirst.find()) {
            val y = matYearFirst.group(1)?.toIntOrNull() ?: continue
            val m = matYearFirst.group(2)?.toIntOrNull() ?: continue
            val d = matYearFirst.group(3)?.toIntOrNull() ?: continue
            val res = buildDateResult(d, m, y)
            if (res != null) return res
        }

        // 3. Day + Month Name + Year (e.g. 15 AĞUSTOS 2026 or 15 AGU 26)
        val patMonthName = Pattern.compile("(\\d{1,2})[./\\-:,\\s]+([A-Za-zçğıöşüÇĞİÖŞÜ]{3,12})[./\\-:,\\s]+(\\d{2,4})")
        val matMonthName = patMonthName.matcher(candidate)
        while (matMonthName.find()) {
            val d = matMonthName.group(1)?.toIntOrNull() ?: continue
            val mStr = matMonthName.group(2)?.uppercase(Locale.forLanguageTag("tr-TR"))
                ?.replace("İ", "I")?.replace("Ğ", "G")?.replace("Ü", "U")
                ?.replace("Ş", "S")?.replace("Ö", "O")?.replace("Ç", "C") ?: continue
            val y = matMonthName.group(3)?.toIntOrNull() ?: continue
            val m = TURKISH_MONTH_MAP[mStr] ?: continue
            val res = buildDateResult(d, m, y)
            if (res != null) return res
        }

        // 4. Month Name + Year (e.g. AGUSTOS 2026)
        val patMonthYearName = Pattern.compile("([A-Za-zçğıöşüÇĞİÖŞÜ]{3,12})[./\\-:,\\s]+(20\\d{2}|\\d{2})")
        val matMonthYearName = patMonthYearName.matcher(candidate)
        while (matMonthYearName.find()) {
            val mStr = matMonthYearName.group(1)?.uppercase(Locale.forLanguageTag("tr-TR"))
                ?.replace("İ", "I")?.replace("Ğ", "G")?.replace("Ü", "U")
                ?.replace("Ş", "S")?.replace("Ö", "O")?.replace("Ç", "C") ?: continue
            val y = matMonthYearName.group(2)?.toIntOrNull() ?: continue
            val m = TURKISH_MONTH_MAP[mStr] ?: continue
            val res = buildDateResult(1, m, y)
            if (res != null) return res
        }

        // 5. Month/Year numeric (e.g. 08/2026 or 08.26)
        val patMonthYearNum = Pattern.compile("\\b(0[1-9]|1[0-2])[./\\-:,\\s]+(20\\d{2}|\\d{2})\\b")
        val matMonthYearNum = patMonthYearNum.matcher(candidate)
        while (matMonthYearNum.find()) {
            val m = matMonthYearNum.group(1)?.toIntOrNull() ?: continue
            val y = matMonthYearNum.group(2)?.toIntOrNull() ?: continue
            val res = buildDateResult(1, m, y)
            if (res != null) return res
        }

        // 6. OCR Character Fixups: Replace confused letters (O/o->0, I/l/|->1, S/s->5, B->8, Z/z->2)
        val fixup = candidate
            .replace("O", "0").replace("o", "0")
            .replace("I", "1").replace("l", "1").replace("|", "1")
            .replace("S", "5").replace("s", "5")
            .replace("B", "8").replace("Z", "2").replace("z", "2")

        if (fixup != candidate) {
            val matFixup = patNumeric.matcher(fixup)
            while (matFixup.find()) {
                val d = matFixup.group(1)?.toIntOrNull() ?: continue
                val m = matFixup.group(2)?.toIntOrNull() ?: continue
                val y = matFixup.group(3)?.toIntOrNull() ?: continue
                val res = buildDateResult(d, m, y)
                if (res != null) return res
            }
        }
    }

    return null
}
