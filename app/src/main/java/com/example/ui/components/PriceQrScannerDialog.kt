package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.parsePriceFromQr
import com.example.data.parseShelfQrPayload
import com.example.ui.screens.scanner.CameraXBarcodeView
import com.example.ui.screens.scanner.CornerBracketsViewfinder
import com.example.ui.screens.scanner.ScannerFilterMode
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import java.util.Locale

@Composable
fun PriceQrScannerDialog(
    expectedBarcode: String? = null,
    expectedProductCode: String? = null,
    expectedProductName: String? = null,
    onDismiss: () -> Unit,
    onPriceScanned: (Double, String) -> Unit
) {
    val context = LocalContext.current
    var isFlashOn by remember { mutableStateOf(false) }
    var hasHandledScan by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            isFlashOn = false
        }
    }

    val safeDismiss = {
        isFlashOn = false
        onDismiss()
    }

    Dialog(
        onDismissRequest = safeDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TurquoisePrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Etiket / Fiyat QR Tara",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TurquoiseDark
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { isFlashOn = !isFlashOn }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Flaş",
                                tint = if (isFlashOn) Color(0xFFFFB703) else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = safeDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Expected product target banner if targeting a specific product
                val expBar = expectedBarcode?.trim().orEmpty()
                val expCode = expectedProductCode?.trim().orEmpty()
                val expName = expectedProductName?.trim().orEmpty()

                if (expBar.isNotBlank() || expCode.isNotBlank() || expName.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TurquoisePrimary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "🎯 Hedef Ürün:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TurquoiseDark
                            )
                            if (expName.isNotBlank()) {
                                Text(
                                    text = expName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Barkod / Kod: ${if (expBar.isNotBlank()) expBar else expCode}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Text(
                        text = "Mağaza raf etiketindeki QR kodu taratarak ürün ve fiyat bilgilerini otomatik ekleyin.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    CameraXBarcodeView(
                        isFlashOn = isFlashOn,
                        filterMode = ScannerFilterMode.ONLY_QR_CODE,
                        isPaused = hasHandledScan,
                        onBarcodeScanned = { raw ->
                            if (hasHandledScan) return@CameraXBarcodeView

                            val shelfData = parseShelfQrPayload(raw)
                            val parsedPrice = shelfData.price ?: parsePriceFromQr(raw)

                            if (parsedPrice != null && parsedPrice in 0.01..9999.99) {
                                // Validate against expected product code/barcode if present
                                if (expBar.isNotBlank() || expCode.isNotBlank()) {
                                    val scBarcode = shelfData.barcode.trim()
                                    val scProductCode = shelfData.productCode?.trim().orEmpty()

                                    val barcodeMatches = scBarcode.isNotBlank() && expBar.isNotBlank() &&
                                        scBarcode.equals(expBar, ignoreCase = true)

                                    val codeMatches = scProductCode.isNotBlank() && expCode.isNotBlank() &&
                                        scProductCode.equals(expCode, ignoreCase = true)

                                    val crossMatch1 = scBarcode.isNotBlank() && expCode.isNotBlank() &&
                                        scBarcode.equals(expCode, ignoreCase = true)

                                    val crossMatch2 = scProductCode.isNotBlank() && expBar.isNotBlank() &&
                                        scProductCode.equals(expBar, ignoreCase = true)

                                    val isMatched = barcodeMatches || codeMatches || crossMatch1 || crossMatch2

                                    // If QR has identifying barcode/productCode and it DOES NOT match the expected product
                                    if (!isMatched && (scBarcode.isNotBlank() || scProductCode.isNotBlank())) {
                                        Toast.makeText(
                                            context,
                                            "Okutulan raf etiketi seçilen ürünle eşleşmediği için fiyat güncellenmedi.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@CameraXBarcodeView
                                    }
                                }

                                hasHandledScan = true
                                isFlashOn = false
                                val formatted = if (parsedPrice % 1.0 == 0.0) parsedPrice.toInt().toString() else String.format(Locale.US, "%.2f", parsedPrice)
                                Toast.makeText(context, "✅ QR Etiketinden Fiyat Alındı: $formatted ₺", Toast.LENGTH_SHORT).show()
                                onPriceScanned(parsedPrice, raw)
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Geçerli bir raf etiketi veya fiyat QR kodu tespit edilemedi.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    CornerBracketsViewfinder(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Doğrulama için lütfen ürünün altındaki raf etiketini taratın.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
