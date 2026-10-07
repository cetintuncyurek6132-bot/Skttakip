package com.example.ui.screens.scanner

import android.annotation.SuppressLint
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.TurquoisePrimary
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

enum class ScannerFilterMode {
    ALL,
    ONLY_1D_BARCODE,
    ONLY_QR_CODE
}

@Composable
fun CameraXBarcodeView(
    isFlashOn: Boolean,
    zoomRatio: Float = 1.0f,
    filterMode: ScannerFilterMode = ScannerFilterMode.ALL,
    isBatterySaverMode: Boolean = false,
    isPaused: Boolean = false,
    requireCloseDistance: Boolean = true,
    onDistanceStateChanged: ((isTooFar: Boolean) -> Unit)? = null,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    val cameraProviderRef = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val cameraRef = remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    val currentOnBarcodeScanned by rememberUpdatedState(onBarcodeScanned)
    val currentOnDistanceStateChanged by rememberUpdatedState(onDistanceStateChanged)
    val currentFilterMode by rememberUpdatedState(filterMode)
    val currentRequireCloseDistance by rememberUpdatedState(requireCloseDistance)
    val currentIsBatterySaverMode by rememberUpdatedState(isBatterySaverMode)
    val isPausedAtomic = remember { AtomicBoolean(isPaused) }
    isPausedAtomic.set(isPaused)

    val lastEmittedRef = remember { java.util.concurrent.atomic.AtomicReference<Pair<String, Long>>(Pair("", 0L)) }
    val lastAnalyzedTimeRef = remember { java.util.concurrent.atomic.AtomicLong(0L) }
    val currentDistanceStateRef = remember { AtomicBoolean(false) }
    val scanCooldownUntilRef = remember { java.util.concurrent.atomic.AtomicLong(0L) }

    val previewViewRef = remember { mutableStateOf<PreviewView?>(null) }
    val previewUseCaseRef = remember { mutableStateOf<Preview?>(null) }
    val imageAnalysisRef = remember { mutableStateOf<ImageAnalysis?>(null) }

    fun triggerAutoFocus(pointX: Float? = null, pointY: Float? = null) {
        val pv = previewViewRef.value ?: return
        val cam = cameraRef.value ?: return
        try {
            val w = pv.width.toFloat().takeIf { it > 0f } ?: return
            val h = pv.height.toFloat().takeIf { it > 0f } ?: return
            val factory = pv.meteringPointFactory
            val x = pointX ?: (w / 2f)
            val y = pointY ?: (h / 2f)
            val point = factory.createPoint(x, y)
            val action = FocusMeteringAction.Builder(
                point,
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
            ).setAutoCancelDuration(2, java.util.concurrent.TimeUnit.SECONDS).build()
            cam.cameraControl.startFocusAndMetering(action)
        } catch (_: Exception) {}
    }

    // 3. SÜREKLİ ODAK (CONTINUOUS AUTO-FOCUS)
    LaunchedEffect(isPaused) {
        if (!isPaused) {
            while (isActive) {
                delay(2600L)
                triggerAutoFocus()
            }
        }
    }

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
            camera.cameraControl.setZoomRatio(zoomRatio)
            previewViewRef.value?.post {
                triggerAutoFocus()
            }
        } catch (e: Exception) {
            android.util.Log.e("CameraXBarcodeView", "Camera rebind failed", e)
        }
    }

    LaunchedEffect(isPaused) {
        isPausedAtomic.set(isPaused)
        if (isPaused) {
            if (currentDistanceStateRef.compareAndSet(true, false)) {
                currentOnDistanceStateChanged?.invoke(false)
            }
        }
    }

    // 1. ML KIT BARKOD TARAYICI: Tüm barkod ve QR formatlarını destekleyen tek ve kalıcı istemci
    val barcodeScanner = remember {
        val builder = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        BarcodeScanning.getClient(builder.build())
    }

    DisposableEffect(barcodeScanner) {
        onDispose {
            try {
                barcodeScanner.close()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(isFlashOn) {
        try {
            cameraRef.value?.cameraControl?.enableTorch(isFlashOn)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(zoomRatio) {
        try {
            cameraRef.value?.cameraControl?.setZoomRatio(zoomRatio)
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
                mainHandler.removeCallbacksAndMessages(null)
                cameraProviderRef.value?.unbindAll()
                cameraRef.value = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                val providerFuture = ProcessCameraProvider.getInstance(context)
                if (providerFuture.isDone) {
                    providerFuture.get()?.unbindAll()
                }
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

            val scaleGestureDetector = android.view.ScaleGestureDetector(
                ctx,
                object : android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScale(detector: android.view.ScaleGestureDetector): Boolean {
                        val camera = cameraRef.value ?: return false
                        val currentZoom = camera.cameraInfo.zoomState.value?.zoomRatio ?: 1.0f
                        val delta = detector.scaleFactor
                        camera.cameraControl.setZoomRatio((currentZoom * delta).coerceIn(1.0f, 6.0f))
                        return true
                    }
                }
            )

            // Tap-to-Focus and Pinch-to-Zoom support (Dokunulan noktaya anında odaklanma)
            previewView.setOnTouchListener { v, event ->
                scaleGestureDetector.onTouchEvent(event)
                if (event.action == android.view.MotionEvent.ACTION_UP && !scaleGestureDetector.isInProgress) {
                    triggerAutoFocus(event.x, event.y)
                    v.performClick()
                }
                true
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProviderRef.value = cameraProvider

                    // 2. OPTİMAL ANALİZ ÇÖZÜNÜRLÜĞÜ VE STRATEJİ:
                    val targetResolutionSize = android.util.Size(1280, 720)

                    val resolutionSelector = ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                targetResolutionSize,
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                            )
                        )
                        .build()

                    val preview = Preview.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .build().apply {
                            setSurfaceProvider(previewView.surfaceProvider)
                        }
                    previewUseCaseRef.value = preview

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    imageAnalysisRef.value = imageAnalysis

                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        val isProxyClosed = AtomicBoolean(false)
                        fun safeClose() {
                            if (isProxyClosed.compareAndSet(false, true)) {
                                try {
                                    imageProxy.close()
                                } catch (_: Exception) {}
                            }
                        }

                        try {
                            if (isPausedAtomic.get()) {
                                safeClose()
                                return@setAnalyzer
                            }

                            val currentTime = System.currentTimeMillis()
                            // Cooldown süresince gecikmeli kareleri analiz etme
                            if (currentTime < scanCooldownUntilRef.get()) {
                                safeClose()
                                return@setAnalyzer
                            }

                            val minFrameIntervalMs = if (currentIsBatterySaverMode) 120L else 40L
                            val lastAnalyzed = lastAnalyzedTimeRef.get()
                            if (currentTime - lastAnalyzed < minFrameIntervalMs) {
                                safeClose()
                                return@setAnalyzer
                            }
                            lastAnalyzedTimeRef.set(currentTime)

                            processImageProxy(
                                barcodeScanner = barcodeScanner,
                                filterMode = currentFilterMode,
                                imageProxy = imageProxy,
                                onCloseProxy = { safeClose() },
                                onDistanceFeedback = { isTooFar ->
                                    if (isPausedAtomic.get()) return@processImageProxy
                                    if (currentDistanceStateRef.compareAndSet(!isTooFar, isTooFar)) {
                                        mainHandler.post {
                                            if (!isPausedAtomic.get()) {
                                                currentOnDistanceStateChanged?.invoke(isTooFar)
                                            }
                                        }
                                    }
                                }
                            ) { barcodes ->
                                if (currentDistanceStateRef.compareAndSet(true, false)) {
                                    mainHandler.post {
                                        currentOnDistanceStateChanged?.invoke(false)
                                    }
                                }
                                if (isPausedAtomic.get()) {
                                    return@processImageProxy
                                }
                                val raw = barcodes.firstNotNullOfOrNull { b ->
                                    (b.rawValue ?: b.displayValue)?.trim()
                                }
                                if (!raw.isNullOrBlank()) {
                                    val now = System.currentTimeMillis()
                                    val (lastEmittedCode, lastEmittedTime) = lastEmittedRef.get()

                                    // Aynı barkod için 800ms engelleme
                                    if (lastEmittedCode == raw && (now - lastEmittedTime) < 800L) {
                                        return@processImageProxy
                                    }

                                    // Farklı okumalar arası minimum 100ms
                                    if ((now - lastEmittedTime) < 100L) {
                                        return@processImageProxy
                                    }

                                    lastEmittedRef.set(Pair(raw, now))
                                    scanCooldownUntilRef.set(now + 300L)
                                    mainHandler.post {
                                        if (!isPausedAtomic.get()) {
                                            currentOnBarcodeScanned(raw)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("CameraXBarcodeView", "Analyzer execution error", e)
                            safeClose()
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
                    camera.cameraControl.setZoomRatio(zoomRatio)
                    previewView.post {
                        triggerAutoFocus()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CameraXBarcodeView", "Camera binding failed", e)
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
    onCloseProxy: () -> Unit,
    onDistanceFeedback: (isTooFar: Boolean) -> Unit,
    onSuccess: (List<Barcode>) -> Unit
) {
    var isAsyncDispatched = false

    try {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            onCloseProxy()
            return
        }

        val rotation = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)

        val task = barcodeScanner.process(inputImage)
        isAsyncDispatched = true

        task.addOnSuccessListener { barcodes ->
            val validBarcodes = barcodes.filter { b ->
                when (filterMode) {
                    ScannerFilterMode.ONLY_1D_BARCODE -> {
                        b.format != Barcode.FORMAT_QR_CODE &&
                        b.format != Barcode.FORMAT_DATA_MATRIX &&
                        b.format != Barcode.FORMAT_AZTEC &&
                        b.format != Barcode.FORMAT_PDF417
                    }
                    ScannerFilterMode.ONLY_QR_CODE -> {
                        b.format == Barcode.FORMAT_QR_CODE ||
                        b.format == Barcode.FORMAT_DATA_MATRIX ||
                        b.format == Barcode.FORMAT_AZTEC
                    }
                    ScannerFilterMode.ALL -> true
                }
            }

            if (validBarcodes.isEmpty()) {
                onDistanceFeedback(false)
                return@addOnSuccessListener
            }

            onDistanceFeedback(false)
            onSuccess(validBarcodes)
        }.addOnFailureListener {
            // Ignore transient frame analysis errors
        }.addOnCompleteListener {
            onCloseProxy()
        }
    } catch (e: Exception) {
        android.util.Log.e("CameraXBarcodeView", "processImageProxy failed", e)
    } finally {
        if (!isAsyncDispatched) {
            onCloseProxy()
        }
    }
}

@Composable
fun CornerBracketsViewfinder(
    modifier: Modifier = Modifier,
    color: Color = TurquoisePrimary,
    strokeWidth: Dp = 4.dp,
    cornerLength: Dp = 28.dp,
    cornerRadius: Dp = 14.dp,
    isGlowing: Boolean = false
) {
    Canvas(modifier = modifier) {
        val sw = strokeWidth.toPx()
        val cl = cornerLength.toPx()
        val cr = cornerRadius.toPx()
        val w = size.width
        val h = size.height

        val pathTopLeft = Path().apply {
            moveTo(0f, cl)
            lineTo(0f, cr)
            quadraticTo(0f, 0f, cr, 0f)
            lineTo(cl, 0f)
        }
        val pathTopRight = Path().apply {
            moveTo(w - cl, 0f)
            lineTo(w - cr, 0f)
            quadraticTo(w, 0f, w, cr)
            lineTo(w, cl)
        }
        val pathBottomLeft = Path().apply {
            moveTo(0f, h - cl)
            lineTo(0f, h - cr)
            quadraticTo(0f, h, cr, h)
            lineTo(cl, h)
        }
        val pathBottomRight = Path().apply {
            moveTo(w - cl, h)
            lineTo(w - cr, h)
            quadraticTo(w, h, w, h - cr)
            lineTo(w, h - cl)
        }

        // Draw soft glow aura if isGlowing is true
        if (isGlowing) {
            val glowWidth = sw * 2.2f
            val glowColor = color.copy(alpha = 0.38f)
            drawPath(pathTopLeft, color = glowColor, style = Stroke(width = glowWidth, cap = StrokeCap.Round))
            drawPath(pathTopRight, color = glowColor, style = Stroke(width = glowWidth, cap = StrokeCap.Round))
            drawPath(pathBottomLeft, color = glowColor, style = Stroke(width = glowWidth, cap = StrokeCap.Round))
            drawPath(pathBottomRight, color = glowColor, style = Stroke(width = glowWidth, cap = StrokeCap.Round))
        }

        // Crisp inner bracket lines
        drawPath(pathTopLeft, color = color, style = Stroke(width = sw, cap = StrokeCap.Round))
        drawPath(pathTopRight, color = color, style = Stroke(width = sw, cap = StrokeCap.Round))
        drawPath(pathBottomLeft, color = color, style = Stroke(width = sw, cap = StrokeCap.Round))
        drawPath(pathBottomRight, color = color, style = Stroke(width = sw, cap = StrokeCap.Round))
    }
}
