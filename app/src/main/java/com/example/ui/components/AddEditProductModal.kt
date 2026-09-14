package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Product
import com.example.data.isDolapProduct
import com.example.data.parseShelfQrPayload
import com.example.ui.screens.ProductNameOcrScannerDialog
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import com.example.util.ProductDataHealer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductModal(
    product: Product?,
    prefilledBarcode: String,
    onDismiss: () -> Unit,
    onSave: (
        barkod: String,
        urunKodu: String,
        urunAdi: String,
        kategori: String,
        sktTarihi: Long,
        stokAdedi: Int,
        isNewSkt: Boolean,
        fiyat: Double?,
        isImportant: Boolean
    ) -> Unit,
    onDelete: ((Product) -> Unit)? = null
) {
    val context = LocalContext.current
    val parsedPrefill = remember(prefilledBarcode) {
        if (prefilledBarcode.isNotBlank()) parseShelfQrPayload(prefilledBarcode) else null
    }

    val healedProduct = remember(product) {
        product?.let { ProductDataHealer.autoHealProduct(it) }
    }

    var barkod by remember {
        mutableStateOf(
            healedProduct?.barkod ?: parsedPrefill?.barcode?.ifEmpty { null } ?: prefilledBarcode
        )
    }
    var urunKodu by remember {
        mutableStateOf(
            healedProduct?.urunKodu ?: parsedPrefill?.productCode ?: ""
        )
    }
    var urunAdi by remember { mutableStateOf(healedProduct?.urunAdi ?: "") }
    var kategori by remember { mutableStateOf(healedProduct?.kategori ?: "Genel") }
    var fiyatText by remember {
        mutableStateOf(
            healedProduct?.fiyat?.let { f ->
                if (f % 1.0 == 0.0) f.toInt().toString() else f.toString()
            } ?: parsedPrefill?.price?.let { f ->
                if (f % 1.0 == 0.0) f.toInt().toString() else f.toString()
            } ?: ""
        )
    }
    var isImportant by remember { mutableStateOf(healedProduct?.isImportant ?: false) }

    val categories = listOf(
        "Dolap Ürünleri",
        "Gıda Ürünleri"
    )
    var expandedCategoryMenu by remember { mutableStateOf(false) }

    var showFullQrScanner by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (product == null) "Yeni Ürün" else "Ürünü Düzenle",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TurquoiseDark
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Kapat")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // QR Etiket Oku Button (Auto-fill barcode, product code, price, name)
                Button(
                    onClick = { showFullQrScanner = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("scan_qr_label_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoiseDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Raf Etiketi Oku",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Raf Etiketi Oku",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                if (showFullQrScanner) {
                    PriceQrScannerDialog(
                        expectedBarcode = if (product != null) (product.barkod.ifBlank { barkod.trim() }) else "",
                        expectedProductCode = if (product != null) (product.urunKodu.ifBlank { urunKodu.trim() }) else "",
                        expectedProductName = product?.urunAdi ?: urunAdi.trim(),
                        onDismiss = { showFullQrScanner = false },
                        onPriceScanned = { scannedPrice, rawQr ->
                            val shelfData = parseShelfQrPayload(rawQr)
                            if (shelfData.barcode.isNotBlank()) {
                                barkod = shelfData.barcode
                            }
                            if (!shelfData.productCode.isNullOrBlank()) {
                                urunKodu = shelfData.productCode
                            }
                            val finalPrice = shelfData.price ?: scannedPrice
                            if (finalPrice > 0.0) {
                                fiyatText = if (finalPrice % 1.0 == 0.0) finalPrice.toInt().toString() else finalPrice.toString()
                            }
                            if (!shelfData.productName.isNullOrBlank()) {
                                urunAdi = shelfData.productName
                                val testProduct = Product(
                                    barkod = barkod,
                                    urunKodu = urunKodu,
                                    urunAdi = shelfData.productName,
                                    kategori = shelfData.productName,
                                    sktTarihi = 0L,
                                    stokAdedi = 0
                                )
                                if (testProduct.isDolapProduct()) {
                                    kategori = "Dolap Ürünleri"
                                }
                            }
                            Toast.makeText(context, "Raf etiketi okundu", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Barcode input
                OutlinedTextField(
                    value = barkod,
                    onValueChange = { barkod = it },
                    label = { Text("Barkod") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_barkod"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = TurquoisePrimary,
                        focusedLabelColor = TurquoiseDark,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Product Code
                OutlinedTextField(
                    value = urunKodu,
                    onValueChange = { urunKodu = it },
                    label = { Text("Ürün Kodu") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = TurquoisePrimary,
                        focusedLabelColor = TurquoiseDark,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Product Name with Camera OCR Addon
                var showProductNameOcrScanner by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = urunAdi,
                        onValueChange = { urunAdi = it.uppercase(java.util.Locale.forLanguageTag("tr-TR")) },
                        label = { Text("Ürün Adı") },
                        placeholder = { Text("Örn: Klasik Peynir 350g") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_urun_adi"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = TurquoisePrimary,
                            focusedLabelColor = TurquoiseDark,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Tek Kamera Butonu (Yazı Oku)
                    Button(
                        onClick = { showProductNameOcrScanner = true },
                        modifier = Modifier
                            .height(54.dp)
                            .padding(top = 4.dp)
                            .testTag("scan_product_name_ocr_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Yazı Oku",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Yazı Oku",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                if (showProductNameOcrScanner) {
                    ProductNameOcrScannerDialog(
                        onDismiss = { showProductNameOcrScanner = false },
                        onProductNameDetected = { detectedName ->
                            urunAdi = detectedName
                            showProductNameOcrScanner = false
                            Toast.makeText(context, "Yazı okundu: $detectedName", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Category / Reyon Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedCategoryMenu,
                    onExpandedChange = { expandedCategoryMenu = !expandedCategoryMenu },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = kategori,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Reyon") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoryMenu) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = TurquoisePrimary,
                            focusedLabelColor = TurquoiseDark,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategoryMenu,
                        onDismissRequest = { expandedCategoryMenu = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    kategori = cat
                                    expandedCategoryMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Price Input (Optional)
                var showPriceQrScanner by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = fiyatText,
                        onValueChange = { fiyatText = it },
                        label = { Text("Fiyat (₺)") },
                        placeholder = { Text("Örn: 45.50") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_fiyat"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = TurquoisePrimary,
                            focusedLabelColor = TurquoiseDark,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Button(
                        onClick = { showPriceQrScanner = true },
                        modifier = Modifier
                            .height(54.dp)
                            .padding(top = 6.dp)
                            .testTag("scan_price_qr_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "QR Fiyat",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "QR Fiyat",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                if (showPriceQrScanner) {
                    PriceQrScannerDialog(
                        expectedBarcode = product?.barkod ?: barkod.trim(),
                        expectedProductCode = product?.urunKodu ?: urunKodu.trim(),
                        expectedProductName = product?.urunAdi ?: urunAdi.trim(),
                        onDismiss = { showPriceQrScanner = false },
                        onPriceScanned = { scannedPrice, rawQr ->
                            fiyatText = if (scannedPrice % 1.0 == 0.0) scannedPrice.toInt().toString() else scannedPrice.toString()
                            val shelfData = parseShelfQrPayload(rawQr)
                            if (barkod.isBlank() && shelfData.barcode.isNotBlank()) {
                                barkod = shelfData.barcode
                            }
                            if (urunKodu.isBlank() && shelfData.productCode != null) {
                                urunKodu = shelfData.productCode
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Önemli Ürün Switch
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, if (isImportant) CriticalOrange else Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⭐", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Önemli Ürün",
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = isImportant,
                            onCheckedChange = { isImportant = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CriticalOrange
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (product != null && onDelete != null) {
                        TextButton(
                            onClick = { onDelete(product) },
                            colors = ButtonDefaults.textButtonColors(contentColor = ExpiredRed),
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Sil")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sil", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            val enteredName = urunAdi.trim()
                            val enteredBarkod = barkod.trim()
                            val enteredCode = urunKodu.trim()
                            val enteredCategory = kategori.trim()

                            val parsedFiyat = fiyatText.trim().replace(',', '.').toDoubleOrNull()
                            val validFiyat = if (parsedFiyat != null && parsedFiyat > 0.0) parsedFiyat else null

                            val candidate = Product(
                                id = product?.id ?: 0,
                                barkod = enteredBarkod.ifBlank { if (enteredCode.isNotBlank()) "2500$enteredCode" else "NO_BARCODE_${System.currentTimeMillis()}" },
                                urunKodu = enteredCode.ifBlank { "0000" },
                                urunAdi = enteredName,
                                kategori = enteredCategory,
                                sktTarihi = product?.sktTarihi ?: 0L,
                                stokAdedi = product?.stokAdedi ?: 0,
                                fiyat = validFiyat,
                                isImportant = isImportant
                            )
                            val finalHealed = ProductDataHealer.autoHealProduct(candidate)

                            if (finalHealed.urunAdi.isNotBlank()) {
                                onSave(
                                    finalHealed.barkod,
                                    finalHealed.urunKodu,
                                    finalHealed.urunAdi,
                                    finalHealed.kategori,
                                    finalHealed.sktTarihi,
                                    finalHealed.stokAdedi,
                                    false,
                                    finalHealed.fiyat,
                                    finalHealed.isImportant
                                )
                            } else {
                                Toast.makeText(context, "Lütfen ürün adını girin", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("save_product_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (product != null) "Güncelle" else "Kaydet",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
