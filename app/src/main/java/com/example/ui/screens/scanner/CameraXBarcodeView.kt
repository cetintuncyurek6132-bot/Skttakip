package com.example.ui.screens.scanner

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
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
import java.util.concurrent.Executors

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
    val isPausedAtomic = remember { java.util.concurrent.atomic.AtomicBoolean(isPaused) }
    isPausedAtomic.set(isPaused)

    val lastEmittedRef = remember { java.util.concurrent.atomic.AtomicReference<Pair<String, Long>>(Pair("", 0L)) }
    val pendingScanRef = remember { java.util.concurrent.atomic.AtomicReference<Pair<String, Long>?>(null) }
    val pendingRunnableRef = remember { java.util.concurrent.atomic.AtomicReference<Runnable?>(null) }
    val lastAnalyzedTimeRef = remember { java.util.concurrent.atomic.AtomicLong(0L) }
    val lastFarDetectedTimeRef = remember { java.util.concurrent.atomic.AtomicLong(0L) }

    LaunchedEffect(isPaused) {
        isPausedAtomic.set(isPaused)
        if (isPaused) {
            pendingRunnableRef.get()?.let { mainHandler.removeCallbacks(it) }
            pendingScanRef.set(null)
            currentOnDistanceStateChanged?.invoke(false)
        }
    }

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

    LaunchedEffect(zoomRatio) {
        try {
            cameraRef.value?.cameraControl?.setZoomRatio(zoomRatio)
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

            // Tap-to-Focus support
            previewView.setOnTouchListener { v, event ->
                if (event.action == android.view.MotionEvent.ACTION_UP) {
                    val camera = cameraRef.value
                    if (camera != null) {
                        try {
                            val factory = previewView.meteringPointFactory
                            val point = factory.createPoint(event.x, event.y)
                            val action = androidx.camera.core.FocusMeteringAction.Builder(
                                point,
                                androidx.camera.core.FocusMeteringAction.FLAG_AF or androidx.camera.core.FocusMeteringAction.FLAG_AE
                            ).setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS).build()
                            camera.cameraControl.startFocusAndMetering(action)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    v.performClick()
                }
                true
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

                    val minFrameIntervalMs = if (isBatterySaverMode) 260L else 180L

                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        if (isPausedAtomic.get()) {
                            pendingRunnableRef.get()?.let { mainHandler.removeCallbacks(it) }
                            pendingScanRef.set(null)
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

                        processImageProxy(
                            barcodeScanner = barcodeScanner,
                            filterMode = filterMode,
                            requireCloseDistance = requireCloseDistance,
                            imageProxy = imageProxy,
                            onDistanceFeedback = { isTooFar ->
                                if (isPausedAtomic.get()) return@processImageProxy
                                val now = System.currentTimeMillis()
                                if (isTooFar) {
                                    lastFarDetectedTimeRef.set(now)
                                    mainHandler.post {
                                        if (!isPausedAtomic.get()) {
                                            currentOnDistanceStateChanged?.invoke(true)
                                        }
                                    }
                                } else {
                                    if (now - lastFarDetectedTimeRef.get() > 350L) {
                                        mainHandler.post {
                                            if (!isPausedAtomic.get()) {
                                                currentOnDistanceStateChanged?.invoke(false)
                                            }
                                        }
                                    }
                                }
                            }
                        ) { barcodes ->
                            lastFarDetectedTimeRef.set(0L)
                            mainHandler.post {
                                currentOnDistanceStateChanged?.invoke(false)
                            }
                            if (isPausedAtomic.get()) {
                                return@processImageProxy
                            }
                            val raw = barcodes.firstNotNullOfOrNull { b ->
                                (b.rawValue ?: b.displayValue)?.trim()
                            }
                            if (!raw.isNullOrBlank()) {
                                if (isPausedAtomic.get()) {
                                    return@processImageProxy
                                }
                                val now = System.currentTimeMillis()
                                val (lastEmittedCode, lastEmittedTime) = lastEmittedRef.get()

                                // 1) Rate limit identical barcode within 1200ms to avoid re-trigger stutter
                                if (lastEmittedCode == raw && (now - lastEmittedTime) < 1200L) {
                                    return@processImageProxy
                                }

                                // 2) Minimal interval between any distinct scans (200ms)
                                if ((now - lastEmittedTime) < 200L) {
                                    return@processImageProxy
                                }

                                // 3) Stabilization buffer (60ms) to upgrade any partial frame scan to full length
                                val currentPending = pendingScanRef.get()
                                if (currentPending != null) {
                                    val (pCode, _) = currentPending
                                    if (raw.length > pCode.length && (raw.contains(pCode) || pCode.contains(raw))) {
                                        pendingScanRef.set(Pair(raw, now))
                                    }
                                } else {
                                    pendingScanRef.set(Pair(raw, now))
                                    val runnable = Runnable {
                                        if (isPausedAtomic.get()) {
                                            pendingScanRef.set(null)
                                            return@Runnable
                                        }
                                        val finalPending = pendingScanRef.getAndSet(null)
                                        if (finalPending != null) {
                                            if (isPausedAtomic.get()) {
                                                return@Runnable
                                            }
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
                                    mainHandler.postDelayed(runnable, 60L)
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
                    camera.cameraControl.setZoomRatio(zoomRatio)
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
    requireCloseDistance: Boolean,
    imageProxy: ImageProxy,
    onDistanceFeedback: (isTooFar: Boolean) -> Unit,
    onSuccess: (List<Barcode>) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val rotation = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotation)
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
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
                            b.format == Barcode.FORMAT_AZTEC ||
                            b.format == Barcode.FORMAT_PDF417
                        }
                        ScannerFilterMode.ALL -> true
                    }
                }

                if (validBarcodes.isEmpty()) {
                    onDistanceFeedback(false)
                    return@addOnSuccessListener
                }

                if (!requireCloseDistance) {
                    onDistanceFeedback(false)
                    onSuccess(validBarcodes)
                    return@addOnSuccessListener
                }

                // Rotated frame dimensions
                val rotatedW = if (rotation == 90 || rotation == 270) imageProxy.height else imageProxy.width
                val rotatedH = if (rotation == 90 || rotation == 270) imageProxy.width else imageProxy.height
                val minFrameDim = minOf(rotatedW, rotatedH).toFloat()

                // Filter by proximity (close distance threshold)
                val closeBarcodes = validBarcodes.filter { b ->
                    val box = b.boundingBox ?: return@filter false
                    val boxW = kotlin.math.abs(box.width()).toFloat()
                    val boxH = kotlin.math.abs(box.height()).toFloat()
                    val maxBoxDim = maxOf(boxW, boxH)
                    val minBoxDim = minOf(boxW, boxH)

                    val is2D = b.format == Barcode.FORMAT_QR_CODE ||
                               b.format == Barcode.FORMAT_DATA_MATRIX ||
                               b.format == Barcode.FORMAT_AZTEC ||
                               b.format == Barcode.FORMAT_PDF417

                    // Center region alignment check
                    val centerX = box.centerX().toFloat()
                    val centerY = box.centerY().toFloat()
                    val isCentered = (centerX in (rotatedW * 0.08f)..(rotatedW * 0.92f)) &&
                                     (centerY in (rotatedH * 0.08f)..(rotatedH * 0.92f))

                    // Proximity criteria:
                    // 1D Barcode: Length at least 22% of minFrameDim (~160px on 720p)
                    // 2D QR Code: Side length at least 16% of minFrameDim (~115px on 720p)
                    val hasRequiredSize = if (is2D) {
                        minBoxDim >= minFrameDim * 0.16f
                    } else {
                        maxBoxDim >= minFrameDim * 0.22f
                    }

                    (isCentered || maxBoxDim >= minFrameDim * 0.35f) && hasRequiredSize
                }

                if (closeBarcodes.isNotEmpty()) {
                    onDistanceFeedback(false)
                    onSuccess(closeBarcodes)
                } else {
                    // Barcode is visible in camera frame, but phone is held too far away
                    onDistanceFeedback(true)
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("CameraXBarcodeView", "Barcode analysis failure", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
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
