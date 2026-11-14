package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.data.parseShelfQrPayload
import com.example.ui.screens.ProductNameOcrScannerDialog
import com.example.ui.theme.AppFontFamily
import com.example.ui.theme.ExpiredRedDark
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import com.example.util.ProductDataHealer
import java.util.Locale

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

    var barkod by remember(product, prefilledBarcode) {
        mutableStateOf(
            healedProduct?.barkod ?: parsedPrefill?.barcode?.ifEmpty { null } ?: prefilledBarcode
        )
    }
    var urunKodu by remember(product, prefilledBarcode) {
        mutableStateOf(
            healedProduct?.urunKodu ?: (
                if (parsedPrefill?.isShelfQr == true &&
                    !parsedPrefill.productCode.isNullOrBlank() &&
                    parsedPrefill.productCode != parsedPrefill.barcode
                ) {
                    parsedPrefill.productCode
                } else {
                    ""
                }
            )
        )
    }
    var urunAdi by remember(product, prefilledBarcode) {
        mutableStateOf(healedProduct?.urunAdi ?: parsedPrefill?.productName ?: "")
    }
    val initialCategory = remember(healedProduct, parsedPrefill) {
        if (healedProduct != null) {
            com.example.util.CategoryClassifier.classify(healedProduct.urunAdi, healedProduct.kategori)
        } else if (!parsedPrefill?.productName.isNullOrBlank()) {
            com.example.util.CategoryClassifier.classify(parsedPrefill!!.productName)
        } else {
            "Gıda"
        }
    }
    var kategori by remember(product, prefilledBarcode) { mutableStateOf(initialCategory) }
    var userOverrodeCategory by remember(product, prefilledBarcode) { mutableStateOf(false) }
    var fiyatText by remember(product, prefilledBarcode) {
        mutableStateOf(
            healedProduct?.fiyat?.let { f ->
                if (f % 1.0 == 0.0) f.toInt().toString() else f.toString()
            } ?: parsedPrefill?.price?.let { f ->
                if (f % 1.0 == 0.0) f.toInt().toString() else f.toString()
            } ?: ""
        )
    }
    var isImportant by remember(product, prefilledBarcode) {
        mutableStateOf(healedProduct?.isImportant ?: false)
    }

    // Sadece iki reyon kategorisi (Dolap ve Gıda)
    val categories = listOf("Dolap", "Gıda")
    var expandedCategoryMenu by remember { mutableStateOf(false) }

    var showFullQrScanner by remember { mutableStateOf(false) }
    var showProductNameOcrScanner by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(product, prefilledBarcode) {
        if (product != null) {
            val healed = ProductDataHealer.autoHealProduct(product)
            barkod = healed.barkod
            urunKodu = healed.urunKodu
            urunAdi = healed.urunAdi
            kategori = if (healed.kategori.isNotBlank()) {
                healed.kategori
            } else {
                com.example.util.CategoryClassifier.classifyCategory(healed.urunAdi)
            }
            userOverrodeCategory = false
            fiyatText = healed.fiyat?.let { f ->
                if (f % 1.0 == 0.0) f.toInt().toString() else f.toString()
            } ?: ""
            isImportant = healed.isImportant
        } else if (prefilledBarcode.isNotBlank()) {
            val parsed = parseShelfQrPayload(prefilledBarcode)
            if (parsed.barcode.isNotBlank()) barkod = parsed.barcode
            urunKodu = if (parsed.isShelfQr && !parsed.productCode.isNullOrBlank() && parsed.productCode != parsed.barcode) {
                parsed.productCode
            } else {
                ""
            }
            if (!parsed.productName.isNullOrBlank()) {
                urunAdi = parsed.productName
                kategori = com.example.util.CategoryClassifier.classify(parsed.productName)
                userOverrodeCategory = false
            }
            if (parsed.price != null && parsed.price > 0.0) {
                fiyatText = if (parsed.price % 1.0 == 0.0) parsed.price.toInt().toString() else parsed.price.toString()
            }
        } else {
            barkod = ""
            urunKodu = ""
            urunAdi = ""
            kategori = "Gıda"
            userOverrodeCategory = false
            fiyatText = ""
            isImportant = false
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
                    userOverrodeCategory = false
                }
                Toast.makeText(context, "Raf etiketi okundu", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showProductNameOcrScanner) {
        ProductNameOcrScannerDialog(
            onDismiss = { showProductNameOcrScanner = false },
            onProductNameDetected = { detectedName ->
                val formatted = detectedName.uppercase(Locale.forLanguageTag("tr-TR"))
                urunAdi = formatted
                kategori = com.example.util.CategoryClassifier.classifyCategory(formatted)
                userOverrodeCategory = false
                showProductNameOcrScanner = false
                Toast.makeText(context, "Yazı okundu: $formatted", Toast.LENGTH_SHORT).show()
            }
        )
    }

    AppBottomSheetWrapper(
        onDismissRequest = onDismiss
    ) { _ ->
        // 1) ÜST BAŞLIK (22.sp, ExtraBold, Koyu Renk, Sağ Üstte İnce Kapat X İkonu)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (product != null) "Ürünü Düzenle" else "Yeni Ürün Ekle",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Kapat",
                    tint = Slate700,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // KAYDIRILABİLİR FORM İÇERİĞİ
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // Kompakt "Raf Etiketi Oku" Butonu (48.dp, Açık Turkuaz Arka Plan, Turkuaz Yazı)
            Surface(
                onClick = { showFullQrScanner = true },
                shape = RoundedCornerShape(16.dp),
                color = TurquoisePrimary.copy(alpha = 0.14f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("scan_qr_label_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QrCodeScanner,
                        contentDescription = "Raf Etiketi Oku",
                        tint = TurquoiseDark,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Raf Etiketi Oku",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TurquoiseDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2 & 3) BARKOD VE ÜRÜN KODU (Yan yana, weight(1f), 12.dp boşluk)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernProductInputField(
                    label = "Barkod",
                    value = barkod,
                    onValueChange = { barkod = it },
                    placeholder = "869...",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    inputTestTag = "input_barkod",
                    modifier = Modifier.weight(1f)
                )

                ModernProductInputField(
                    label = "Ürün Kodu",
                    value = urunKodu,
                    onValueChange = { urunKodu = it },
                    placeholder = "Örn: 1300...",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3) ÜRÜN ADI (Tam genişlikte, sağında düzgün hizalı kamera ikonu)
            ModernProductInputField(
                label = "Ürün Adı",
                value = urunAdi,
                onValueChange = {
                    val formatted = it.uppercase(Locale.forLanguageTag("tr-TR"))
                    urunAdi = formatted
                    if (!userOverrodeCategory && formatted.isNotBlank()) {
                        kategori = com.example.util.CategoryClassifier.classifyCategory(formatted)
                    }
                },
                placeholder = "Örn: KLASİK PEYNİR 350G",
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters
                ),
                inputTestTag = "input_urun_adi",
                trailingContent = {
                    IconButton(
                        onClick = { showProductNameOcrScanner = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("scan_product_name_ocr_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = "Yazı Oku",
                            tint = TurquoiseDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3) REYON (Dropdown) VE FİYAT (Yan yana, weight(1f), 12.dp boşluk)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Reyon Seçici
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reyon",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate600,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val fieldShape = RoundedCornerShape(16.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .clip(fieldShape)
                                .background(Slate50)
                                .then(
                                    if (expandedCategoryMenu) {
                                        Modifier.border(1.5.dp, TurquoisePrimary, fieldShape)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = TurquoisePrimary)
                                ) { expandedCategoryMenu = true }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = kategori,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = "Reyon Seç",
                                tint = Slate700,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = expandedCategoryMenu,
                            onDismissRequest = { expandedCategoryMenu = false },
                            containerColor = Color.White,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = cat,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900
                                        )
                                    },
                                    onClick = {
                                        kategori = cat
                                        userOverrodeCategory = true
                                        expandedCategoryMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Fiyat Alanı
                ModernProductInputField(
                    label = "Fiyat (₺)",
                    value = fiyatText,
                    onValueChange = { fiyatText = it },
                    placeholder = "0.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    inputTestTag = "input_fiyat",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4) ÖNEMLİ ÜRÜN SWITCH'İ (Kutusu olmayan temiz Row, Turkuaz Switch)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⭐",
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Önemli Ürün",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }

                Switch(
                    checked = isImportant,
                    onCheckedChange = { isImportant = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = TurquoisePrimary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Slate200,
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5) ALT BUTONLAR (52.dp yükseklik, 16.dp yuvarlak köşe, Düzenleme modunda solda 0.3f Sil, sağda 0.7f Güncelle/Kaydet)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (product != null && onDelete != null) {
                    Button(
                        onClick = {
                            if (isSaving) return@Button
                            isSaving = true
                            onDelete(product)
                        },
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFEE2E2),
                            contentColor = ExpiredRedDark
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(0.3f)
                            .height(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Sil",
                            tint = ExpiredRedDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Sil",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = ExpiredRedDark,
                            maxLines = 1
                        )
                    }
                }

                Button(
                    onClick = {
                        if (isSaving) return@Button
                        val enteredName = urunAdi.trim()
                        val enteredBarkod = barkod.trim()
                        val enteredCode = urunKodu.trim()
                        val enteredCategory = kategori.trim()

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
                            barkod = enteredBarkod.ifBlank {
                                if (enteredCode.isNotBlank()) "2500$enteredCode"
                                else "NO_BARCODE_${(enteredName + enteredCode).hashCode().toString().replace("-", "N")}"
                            },
                            urunKodu = enteredCode,
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
                        .weight(if (product != null && onDelete != null) 0.7f else 1f)
                        .height(52.dp)
                        .testTag("save_product_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (product != null) "Güncelle" else "Kaydet",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernProductInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    inputTestTag: String? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val boxShape = RoundedCornerShape(16.dp)

    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isFocused) TurquoiseDark else Slate600,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(boxShape)
                .background(Slate50)
                .then(
                    if (isFocused) {
                        Modifier.border(BorderStroke(1.5.dp, TurquoisePrimary), boxShape)
                    } else {
                        Modifier
                    }
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusRequester.requestFocus()
                }
                .padding(start = 14.dp, end = if (trailingContent != null) 6.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    keyboardOptions = keyboardOptions,
                    cursorBrush = SolidColor(TurquoisePrimary),
                    textStyle = TextStyle(
                        fontFamily = AppFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused = it.isFocused }
                        .then(
                            if (inputTestTag != null) Modifier.testTag(inputTestTag) else Modifier
                        )
                )
            }

            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(4.dp))
                trailingContent()
            }
        }
    }
}
