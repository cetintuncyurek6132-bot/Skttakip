package com.example.ui.screens.adetsel

import android.Manifest
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.data.findMatchingProducts
import com.example.ui.screens.scanner.CameraXBarcodeView
import com.example.ui.theme.ExpiredRedDark
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import com.example.util.HapticFeedbackHelper
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

/**
 * Sayfa İçi Hızlı Sayım Penceresi (ModalBottomSheet).
 * Yalnızca stok ve adet sayımına odaklanır; SKT ve tarih içermez.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AdetselQuickCountBottomSheet(
    allProducts: List<Product>,
    onDismiss: () -> Unit,
    onSaveCount: (product: Product, countedQty: Int) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    var isFlashOn by remember { mutableStateOf(false) }
    var lastScannedBarcode by remember { mutableStateOf("") }
    var scannedProduct by remember { mutableStateOf<Product?>(null) }
    var isProductNotFound by remember { mutableStateOf(false) }
    var countQuantityText by remember { mutableStateOf("") }

    val playScanBeep: () -> Unit = {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (_: Exception) {}
        HapticFeedbackHelper.triggerSuccessHaptic(context)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Başlık Çubuğu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = TurquoiseDark,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hızlı Sayım",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = Slate500,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 1. ÜST KISIM (KAMERA - 200dp CameraX Vizörü)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .border(1.2.dp, TurquoisePrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                if (cameraPermissionState.status.isGranted) {
                    CameraXBarcodeView(
                        isFlashOn = isFlashOn,
                        zoomRatio = 1.0f,
                        onBarcodeScanned = { barcode ->
                            val clean = barcode.trim()
                            if (clean.isNotBlank() && clean != lastScannedBarcode) {
                                lastScannedBarcode = clean
                                val matched = allProducts.findMatchingProducts(clean).firstOrNull()
                                if (matched != null) {
                                    playScanBeep()
                                    scannedProduct = matched
                                    isProductNotFound = false
                                    // Veritabanındaki o anki mevcut stok adedi otomatik kutunun içine yazılır
                                    val currentStock = allProducts.filter {
                                        (matched.barkod.isNotBlank() && it.barkod == matched.barkod) ||
                                        (matched.urunKodu.isNotBlank() && it.urunKodu == matched.urunKodu)
                                    }.sumOf { it.stokAdedi }.let { if (it > 0) it else matched.stokAdedi }
                                    countQuantityText = currentStock.toString()
                                } else {
                                    HapticFeedbackHelper.triggerWarningHaptic(context)
                                    scannedProduct = null
                                    isProductNotFound = true
                                    countQuantityText = ""
                                }
                            }
                        }
                    )

                    // Flaş Butonu
                    IconButton(
                        onClick = { isFlashOn = !isFlashOn },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flaş",
                            tint = if (isFlashOn) Color(0xFFFFD54F) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Tarama Kılavuz Çerçevesi
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(0.72f)
                            .height(90.dp)
                            .border(2.dp, TurquoisePrimary.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Kamera erişim izni gerekiyor",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { cameraPermissionState.launchPermissionRequest() },
                            colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("İzin Ver", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. ORTA KISIM (ÜRÜN BİLGİSİ VE STOK ADEDİ)
            if (scannedProduct != null) {
                val prod = scannedProduct!!
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // 1. Ürün İsmi (Kalın ve okunaklı)
                        Text(
                            text = prod.urunAdi,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 2. Barkod Numarası
                            Text(
                                text = "Barkod: ${prod.barkod.ifBlank { "-" }}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate700
                            )
                            // 3. Ürün Kodu (Stok kodu)
                            Text(
                                text = "Ürün Kodu: ${prod.urunKodu.ifBlank { "-" }}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate700
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Adet Kutusu ve + / - Butonları
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Sayım Adedi:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(end = 12.dp)
                            )

                            // Eksi (-) Butonu
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable {
                                        val current = countQuantityText.toIntOrNull() ?: 0
                                        if (current > 0) {
                                            countQuantityText = (current - 1).toString()
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Azalt",
                                        tint = Slate900,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Adet Giriş Kutusu (Klavyeden yazılabilir, varsayılan mevcut stok)
                            OutlinedTextField(
                                value = countQuantityText,
                                onValueChange = { newVal ->
                                    val filtered = newVal.filter { it.isDigit() }
                                    countQuantityText = filtered
                                },
                                modifier = Modifier
                                    .width(90.dp)
                                    .height(52.dp)
                                    .testTag("quick_count_quantity_input"),
                                singleLine = true,
                                textStyle = TextStyle(
                                    textAlign = TextAlign.Center,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TurquoiseDark
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TurquoiseDark,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            // Artı (+) Butonu
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable {
                                        val current = countQuantityText.toIntOrNull() ?: 0
                                        countQuantityText = (current + 1).toString()
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Artır",
                                        tint = Slate900,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (isProductNotFound) {
                // 3. HATA DURUMU: "Ürün sistemde kayıtlı değil"
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = ExpiredRedDark.copy(alpha = 0.08f),
                    border = BorderStroke(1.2.dp, ExpiredRedDark)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = ExpiredRedDark,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Ürün sistemde kayıtlı değil",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ExpiredRedDark
                            )
                            if (lastScannedBarcode.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Okunan Barkod: $lastScannedBarcode",
                                    fontSize = 12.sp,
                                    color = Slate700
                                )
                            }
                        }
                    }
                }
            } else {
                // Henüz okutulmadıysa bekleme rehberi
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = TurquoiseDark,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Barkodu kameraya gösterin...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate700
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. ALT KISIM (BUTONLAR)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // "Vazgeç" butonu: Modalı kapatır, hiçbir şey kaydetmez
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("quick_count_cancel_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = "Vazgeç",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // "Sayıma Ekle" butonu: Kutuda yazan adedi doğrudan sayım listesine ekler/günceller, modalı kapatır.
                // Okunan barkod sistemde yoksa: Sayıma Ekle butonu pasif kalır.
                val isSaveEnabled = scannedProduct != null && !isProductNotFound && countQuantityText.toIntOrNull() != null
                Button(
                    onClick = {
                        val prod = scannedProduct ?: return@Button
                        val qty = countQuantityText.toIntOrNull() ?: 0
                        onSaveCount(prod, qty)
                        onDismiss()
                    },
                    enabled = isSaveEnabled,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("quick_count_save_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TurquoiseDark,
                        disabledContainerColor = Slate200
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (isSaveEnabled) Color.White else Slate500,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sayıma Ekle",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = if (isSaveEnabled) Color.White else Slate500
                    )
                }
            }
        }
    }
}
