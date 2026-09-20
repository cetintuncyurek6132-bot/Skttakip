package com.example.ui.screens

import com.example.ui.screens.scanner.CameraXProductNameOcrView

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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
                                    val sim = com.example.util.ProductNameOcrParser.calculateTextSimilarity(lastCandidateText, bestCandidate)
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
