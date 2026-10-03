package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
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
    val initialCategory = remember(healedProduct) {
        if (healedProduct != null) {
            com.example.util.CategoryClassifier.classify(healedProduct.urunAdi, healedProduct.kategori)
        } else {
            "Gıda"
        }
    }
    var kategori by remember { mutableStateOf(initialCategory) }
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

    // Sadece iki reyon kategorisi (Dolap ve Gıda)
    val categories = listOf(
        "Dolap",
        "Gıda"
    )
    var expandedCategoryMenu by remember { mutableStateOf(false) }

    var showFullQrScanner by remember { mutableStateOf(false) }
    var showProductNameOcrScanner by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
                .imePadding(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                // 1. ÜST BİLGİ VE BAŞLIK ALANI (HERO HEADER)
                if (product != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = (if (urunAdi.isNotBlank()) urunAdi else product.urunAdi).ifBlank { "Ürünü Düzenle" },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Kod: ${(if (urunKodu.isNotBlank()) urunKodu else product.urunKodu).ifBlank { "-" }}  •  Barkod: ${if (barkod.isNotBlank()) barkod else product.barkod.ifBlank { "-" }}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Yeni Ürün Ekle",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TurquoiseDark
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 1.dp
                )

                // 2. "RAF ETİKETİ OKU" BUTONU (Daha Kompakt: 40.dp)
                Button(
                    onClick = { showFullQrScanner = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("scan_qr_label_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoiseDark),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Raf Etiketi Oku",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
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
                                kategori = com.example.util.CategoryClassifier.classify(shelfData.productName)
                            }
                            Toast.makeText(context, "Raf etiketi okundu", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Yeni ürün ekleme modunda barkod ve kod giriş alanları
                if (product == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = barkod,
                            onValueChange = { barkod = it },
                            label = { Text("Barkod") },
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("input_barkod"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
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
                        OutlinedTextField(
                            value = urunKodu,
                            onValueChange = { urunKodu = it },
                            label = { Text("Ürün Kodu") },
                            modifier = Modifier.weight(0.9f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
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
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 3. ÜRÜN ADI ALANI (Kamera Trailing Icon'u ile)
                OutlinedTextField(
                    value = urunAdi,
                    onValueChange = {
                        val formatted = it.uppercase(java.util.Locale.forLanguageTag("tr-TR"))
                        urunAdi = formatted
                        if (formatted.isNotBlank()) {
                            kategori = com.example.util.CategoryClassifier.classifyCategory(formatted)
                        }
                    },
                    label = { Text("Ürün Adı") },
                    placeholder = { Text("Örn: Klasik Peynir 350g") },
                    trailingIcon = {
                        IconButton(
                            onClick = { showProductNameOcrScanner = true },
                            modifier = Modifier.testTag("scan_product_name_ocr_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Yazı Oku",
                                tint = TurquoisePrimary
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_urun_adi"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
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

                if (showProductNameOcrScanner) {
                    ProductNameOcrScannerDialog(
                        onDismiss = { showProductNameOcrScanner = false },
                        onProductNameDetected = { detectedName ->
                            val formatted = detectedName.uppercase(java.util.Locale.forLanguageTag("tr-TR"))
                            urunAdi = formatted
                            kategori = com.example.util.CategoryClassifier.classifyCategory(formatted)
                            showProductNameOcrScanner = false
                            Toast.makeText(context, "Yazı okundu: $formatted", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. REYON VE FİYAT (Derli toplu yan yana hizalama)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Reyon Seçimi
                    Box(modifier = Modifier.weight(1f)) {
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
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
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
                                        text = { Text(cat, fontSize = 13.sp) },
                                        onClick = {
                                            kategori = cat
                                            expandedCategoryMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Fiyat (₺) Alanı
                    OutlinedTextField(
                        value = fiyatText,
                        onValueChange = { fiyatText = it },
                        label = { Text("Fiyat (₺)") },
                        placeholder = { Text("0.00") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_fiyat"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
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
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5. ÖNEMLİ ÜRÜN SWITCH ALANI
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, if (isImportant) CriticalOrange else Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⭐", fontSize = 15.sp)
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

                Spacer(modifier = Modifier.height(14.dp))

                // 6. ALT BUTONLAR VE AKSİYONLAR: "Sil" ve "Güncelle / Kaydet"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (product != null && onDelete != null) {
                        TextButton(
                            onClick = {
                                if (isSaving) return@TextButton
                                isSaving = true
                                onDelete(product)
                            },
                            enabled = !isSaving,
                            colors = ButtonDefaults.textButtonColors(contentColor = ExpiredRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(0.7f)
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Sil",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sil", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Button(
                        onClick = {
                            if (isSaving) return@Button
                            val enteredName = urunAdi.trim()
                            val enteredBarkod = barkod.trim()
                            val enteredCode = urunKodu.trim()
                            val enteredCategory = kategori.trim()

                            // Fiyat Girişi: 2 basamağa yuvarlayarak IEEE 754 sapmalarını engelle
                            val trimmedFiyat = fiyatText.trim().replace(',', '.')
                            val validFiyat = if (trimmedFiyat.isNotBlank()) {
                                trimmedFiyat.toDoubleOrNull()?.takeIf { it > 0.0 }?.let { parsedDouble ->
                                    (Math.round(parsedDouble * 100.0) / 100.0)
                                }
                            } else {
                                null
                            }

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
                                isSaving = true
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
                        enabled = !isSaving,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp)
                            .testTag("save_product_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                        shape = RoundedCornerShape(10.dp)
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
