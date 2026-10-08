package com.example.ui.components

import android.Manifest
import android.media.AudioManager
import android.media.ToneGenerator
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.data.findMatchingProducts
import com.example.ui.screens.scanner.CameraXBarcodeView
import com.example.ui.screens.scanner.ScannerFilterMode
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.ExpiredRedContainer
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import com.example.util.HapticFeedbackHelper
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun QuickSearchScannerSheet(
    allProducts: List<Product>,
    onDismiss: () -> Unit,
    onProductClick: (Product) -> Unit,
    onAddNewProductWithBarcode: (String) -> Unit
) {
    val context = LocalContext.current
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

    val playScanBeep = remember {
        {
            try {
                val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
                tg.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
            } catch (_: Exception) {}
            HapticFeedbackHelper.triggerSuccessHaptic(context)
        }
    }

    AppBottomSheetWrapper(
        onDismissRequest = onDismiss
    ) { _ ->
        // 1. ÜST BAŞLIK VE KAPAT BUTONU
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = TurquoisePrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Hızlı Ürün Ara",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        fontSize = 18.sp
                    )
                )
            }

            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Slate500)
            }
        }

        HorizontalDivider(color = Slate200, modifier = Modifier.padding(horizontal = 16.dp))

        // 2. İÇERİK: KAMERA VE SONUÇ KARTI
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Kamera Vizörü (Kompakt 190 dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .border(1.2.dp, TurquoisePrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                if (cameraPermissionState.status.isGranted) {
                    CameraXBarcodeView(
                        isFlashOn = isFlashOn,
                        zoomRatio = 1.0f,
                        filterMode = ScannerFilterMode.ALL,
                        isPaused = false,
                        requireCloseDistance = false,
                        onBarcodeScanned = { barcode ->
                            val clean = barcode.trim()
                            if (clean.isNotBlank() && clean != lastScannedBarcode) {
                                lastScannedBarcode = clean
                                val matched = allProducts.findMatchingProducts(clean).firstOrNull()
                                if (matched != null) {
                                    scannedProduct = matched
                                    isProductNotFound = false
                                    playScanBeep()
                                } else {
                                    scannedProduct = null
                                    isProductNotFound = true
                                    HapticFeedbackHelper.triggerWarningHaptic(context)
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

                    // Odaklama Çerçevesi
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(0.72f)
                            .height(85.dp)
                            .border(2.dp, TurquoisePrimary.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Kamera İzni Gerekli",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { cameraPermissionState.launchPermissionRequest() },
                            colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("İzin Ver", fontSize = 12.sp)
                        }
                    }
                }
            }

            // 3. ALT KISIM: SONUÇ KARTI VEYA YÖNLENDİRME METNİ
            if (scannedProduct != null) {
                val prod = scannedProduct!!
                val totalStock = allProducts
                    .filter { it.barkod.isNotBlank() && it.barkod == prod.barkod }
                    .sumOf { it.stokAdedi }
                    .let { if (it > 0) it else prod.stokAdedi }

                val earliestSkt = allProducts
                    .filter { it.barkod.isNotBlank() && it.barkod == prod.barkod && it.sktTarihi > 0 }
                    .minByOrNull { it.sktTarihi }
                    ?.getFormattedSkt() ?: prod.getFormattedSkt()

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.4.dp, TurquoisePrimary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true)
                        ) {
                            onProductClick(prod)
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = prod.urunAdi,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900,
                                        fontSize = 15.5.sp
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (prod.urunKodu.isNotBlank()) {
                                        Text(
                                            text = "🏷️ ${prod.urunKodu}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TurquoiseDark
                                        )
                                        Text("•", color = Slate400, fontSize = 10.sp)
                                    }
                                    Text(
                                        text = prod.barkod,
                                        fontSize = 11.5.sp,
                                        color = Slate600
                                    )
                                }
                            }

                            // Satış Fiyatı (Varsa)
                            prod.getFormattedPrice()?.let { formattedPrice ->
                                Surface(
                                    color = TurquoisePrimary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = formattedPrice,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.5.sp,
                                        color = TurquoiseDark,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Slate100, thickness = 1.dp)

                        // En Yakın SKT ve Toplam Adet Rozetleri
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "📅 $earliestSkt",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFB45309),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }

                                Surface(
                                    color = Color(0xFFDCFCE7),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "📦 $totalStock Adet",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF16A34A),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Detay",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TurquoiseDark
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            } else if (isProductNotFound) {
                // Ürün Bulunamadı Kartı
                Surface(
                    color = ExpiredRedContainer,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ExpiredRed.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = ExpiredRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Ürün Bulunamadı",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ExpiredRed
                                )
                                Text(
                                    text = "Barkod: $lastScannedBarcode",
                                    fontSize = 12.sp,
                                    color = Slate700
                                )
                            }
                        }

                        Button(
                            onClick = {
                                onAddNewProductWithBarcode(lastScannedBarcode)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Yeni Ürün Olarak Ekle", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                // Henüz Okutulmadı - Rehber Metin
                Surface(
                    color = Slate50,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Barkodu vizöre hizalayın",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate600
                        )
                    }
                }
            }
        }
    }
}
