package com.example.ui.screens
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdetselKayit
import com.example.data.Product
import com.example.data.findMatchingProducts
import com.example.data.getDisplayName
import com.example.ui.screens.BarcodeScannerSheet
import com.example.ui.screens.adetsel.AdetselQuickCountBottomSheet
import com.example.ui.viewmodel.AdetselViewModel
import com.example.util.HapticFeedbackHelper
import com.example.ui.screens.adetsel.AdetselFilterChip
import com.example.ui.screens.adetsel.AdetselSayimDialog
import com.example.ui.screens.adetsel.CompactYapilacakCard
import com.example.ui.screens.adetsel.CompactYapildiCard
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.ExpiredRedDark
import com.example.ui.theme.NormalGreen
import com.example.ui.theme.NormalGreenDark
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import com.example.ui.theme.WarningBlueDark

import com.example.ui.screens.scanner.ScannerOpenMode

enum class AdetselTab {
    YAPILACAK,
    YAPILDI
}

@Composable
fun AdetselScreen(
    yapilacakList: List<AdetselKayit>,
    yapildiList: List<AdetselKayit>,
    allProducts: List<Product> = emptyList(),
    adetselViewModel: AdetselViewModel? = null,
    onAddToAdetsel: ((Product, ((Boolean) -> Unit)?) -> Unit)? = null,
    onSaveSayim: (kayit: AdetselKayit, sonuc: String, fark: Int, notlar: String) -> Unit,
    onUndoSayim: (AdetselKayit) -> Unit,
    onDeleteKayit: (Int) -> Unit,
    onClearCompleted: () -> Unit,
    onOpenScanner: ((ScannerOpenMode) -> Unit)? = null,
    onNavigateToProducts: () -> Unit,
    onBackClick: () -> Unit = onNavigateToProducts
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var selectedTab by remember { mutableStateOf(AdetselTab.YAPILACAK) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedResultFilter by remember { mutableStateOf("ALL") } // ALL, EKSIK, FAZLA, TAM
    var showBarcodeScanner by remember { mutableStateOf(false) }

    val searchFocusRequester = remember { FocusRequester() }
    var notFoundWarning by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(notFoundWarning) {
        if (notFoundWarning != null) {
            kotlinx.coroutines.delay(3500L)
            notFoundWarning = null
        }
    }

    val playScanBeep: () -> Unit = {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (_: Exception) {}
        HapticFeedbackHelper.triggerSuccessHaptic(context)
    }

    var candidateProductToAdd by remember { mutableStateOf<Product?>(null) }
    var countingKayit by remember { mutableStateOf<AdetselKayit?>(null) }
    var initialModeForDialog by remember { mutableStateOf("TAM") } // "TAM", "EKSIK", "FAZLA"
    var itemToDelete by remember { mutableStateOf<AdetselKayit?>(null) }
    var showClearCompletedConfirm by remember { mutableStateOf(false) }
    var showExportModal by remember { mutableStateOf(false) }

    val distinctYapilacakList = remember(yapilacakList) {
        yapilacakList.distinctBy { item ->
            when {
                item.barkod.isNotBlank() -> "B:${item.barkod.trim()}_${item.productId}"
                item.urunKodu.isNotBlank() -> "K:${item.urunKodu.trim()}_${item.productId}"
                item.productId > 0 -> "P:${item.productId}"
                else -> "N:${item.urunAdi.trim().lowercase()}_${item.id}"
            }
        }
    }

    val filteredYapilacak = remember(distinctYapilacakList, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) distinctYapilacakList else {
            distinctYapilacakList.filter {
                it.urunAdi.lowercase().contains(q) ||
                it.urunKodu.lowercase().contains(q) ||
                it.barkod.lowercase().contains(q)
            }
        }
    }

    val filteredYapildi = remember(yapildiList, searchQuery, selectedResultFilter) {
        val q = searchQuery.trim().lowercase()
        yapildiList.filter { item ->
            val matchQuery = q.isEmpty() ||
                item.urunAdi.lowercase().contains(q) ||
                item.urunKodu.lowercase().contains(q) ||
                item.barkod.lowercase().contains(q)

            val matchFilter = when (selectedResultFilter) {
                "EKSIK" -> item.sayimSonucu == "EKSIK"
                "FAZLA" -> item.sayimSonucu == "FAZLA"
                "TAM" -> item.sayimSonucu == "TAM"
                else -> true
            }
            matchQuery && matchFilter
        }
    }

    // HIZLI SAYIM MODAL BOTTOM SHEET (SAYFA İÇİ)
    if (showBarcodeScanner) {
        AdetselQuickCountBottomSheet(
            allProducts = allProducts,
            onDismiss = { showBarcodeScanner = false },
            onSaveCount = { product, countedQty ->
                if (adetselViewModel != null) {
                    adetselViewModel.saveDirectCount(product, countedQty) {
                        Toast.makeText(context, "${product.urunAdi} sayıma eklendi (Adet: $countedQty)", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val existing = distinctYapilacakList.find { it.productId == product.id || (it.barkod.isNotBlank() && it.barkod == product.barkod) }
                        ?: yapildiList.find { it.productId == product.id || (it.barkod.isNotBlank() && it.barkod == product.barkod) }
                    if (existing != null) {
                        val fark = countedQty - existing.beklenenAdet
                        val sonuc = when {
                            fark == 0 -> "TAM"
                            fark < 0 -> "EKSIK"
                            else -> "FAZLA"
                        }
                        onSaveSayim(existing, sonuc, fark, "")
                    } else {
                        onAddToAdetsel?.invoke(product) { _ -> }
                    }
                }
            }
        )
    }

    // SAYIMA ÜRÜN EKLE ONAY DİYALOĞU
    candidateProductToAdd?.let { product ->
        val systemStock = remember(product, allProducts) {
            val matchingStock = allProducts.filter {
                (it.barkod.isNotBlank() && it.barkod == product.barkod) ||
                (it.urunKodu.isNotBlank() && it.urunKodu == product.urunKodu)
            }.sumOf { it.stokAdedi }
            if (matchingStock > 0) matchingStock else product.stokAdedi
        }

        AlertDialog(
            onDismissRequest = { candidateProductToAdd = null },
            shape = RoundedCornerShape(16.dp),
            icon = {
                Surface(
                    shape = CircleShape,
                    color = TurquoisePrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = TurquoiseDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Sayıma Ürün Ekle",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Ürün Detay Kartı
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Ürün Baş Harf Rozeti
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = TurquoisePrimary.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.3f))
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = product.getDisplayName().take(1).uppercase(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = TurquoiseDark
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.getDisplayName(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                if (product.urunKodu.isNotBlank()) {
                                    Text(
                                        text = "Kod: ${product.urunKodu}",
                                        fontSize = 11.5.sp,
                                        color = Slate500,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (product.barkod.isNotBlank() && !product.barkod.startsWith("NO_BARCODE_")) {
                                    Text(
                                        text = "Barkod: ${product.barkod}",
                                        fontSize = 11.sp,
                                        color = Slate500
                                    )
                                }
                            }
                        }
                    }

                    // Sistem Stoğu Bilgi Alanı
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = TurquoisePrimary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mevcut Sistem Stoğu:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate700
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = TurquoiseDark
                            ) {
                                Text(
                                    text = "$systemStock Adet",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = candidateProductToAdd ?: return@Button
                        if (onAddToAdetsel != null) {
                            onAddToAdetsel(p) { isSuccess ->
                                if (isSuccess) {
                                    Toast.makeText(context, "${p.urunAdi} sayım listesine eklendi", Toast.LENGTH_SHORT).show()
                                    selectedTab = AdetselTab.YAPILACAK
                                    searchQuery = ""
                                } else {
                                    Toast.makeText(context, "Bu ürün zaten sayım listesinde ekli!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "${p.urunAdi} sayıma eklenemedi.", Toast.LENGTH_SHORT).show()
                        }
                        candidateProductToAdd = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoiseDark),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("adetsel_confirm_add_product_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sayıma Ekle", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { candidateProductToAdd = null },
                    modifier = Modifier.testTag("adetsel_cancel_add_product_button")
                ) {
                    Text("Vazgeç", color = Slate700, fontSize = 13.sp)
                }
            }
        )
    }

    // SAYIM DIALOGU
    countingKayit?.let { kayit ->
        AdetselSayimDialog(
            kayit = kayit,
            initialMode = initialModeForDialog,
            onDismiss = { countingKayit = null },
            onSave = { sonuc, fark, notlar ->
                onSaveSayim(kayit, sonuc, fark, notlar)
                countingKayit = null
            }
        )
    }

    // SİLME ONAY DIALOGU
    itemToDelete?.let { kayit ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            icon = {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = ExpiredRed)
            },
            title = {
                Text(text = "Adetsel Kaydını Sil", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            },
            text = {
                Text(
                    text = "\"${kayit.urunAdi}\" kaydını listeden silmek istediğinize emin misiniz?",
                    fontSize = 13.sp,
                    color = Slate700
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteKayit(kayit.id)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpiredRed)
                ) {
                    Text("Sil", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Vazgeç", color = Slate700)
                }
            }
        )
    }

    // TAMAMLANANLARI TEMİZLE ONAY DIALOGU
    if (showClearCompletedConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCompletedConfirm = false },
            icon = {
                Icon(imageVector = Icons.Default.ClearAll, contentDescription = null, tint = ExpiredRed)
            },
            title = {
                Text(text = "Yapılan Sayımları Temizle", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            },
            text = {
                Text(
                    text = "Tamamlanmış tüm adetsel sayım geçmişi silinecektir.",
                    fontSize = 13.sp,
                    color = Slate700
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearCompleted()
                        showClearCompletedConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpiredRed)
                ) {
                    Text("Temizle", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCompletedConfirm = false }) {
                    Text("Vazgeç", color = Slate700)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP HEADER - KOMPAKT VE ŞIK
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = TurquoisePrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.FactCheck,
                                    contentDescription = null,
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Adetsel Sayım",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { showExportModal = true },
                            modifier = Modifier
                                .size(30.dp)
                                .background(TurquoisePrimary.copy(alpha = 0.12f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Sayım Sonucunu Dışa Aktar (Excel/CSV)",
                                tint = TurquoiseDark,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (selectedTab == AdetselTab.YAPILDI && yapildiList.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { showClearCompletedConfirm = true },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, ExpiredRed.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpiredRed),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Temizle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // TAB ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // TAB 1: ADETSEL YAPILACAK
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedTab = AdetselTab.YAPILACAK },
                        color = if (selectedTab == AdetselTab.YAPILACAK) TurquoiseDark else Slate100,
                        shape = RoundedCornerShape(8.dp),
                        border = if (selectedTab == AdetselTab.YAPILACAK) null else BorderStroke(1.dp, Slate200)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "YAPILACAK",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == AdetselTab.YAPILACAK) Color.White else Slate700
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (selectedTab == AdetselTab.YAPILACAK) Color.White.copy(alpha = 0.25f) else TurquoisePrimary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "${distinctYapilacakList.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (selectedTab == AdetselTab.YAPILACAK) Color.White else TurquoiseDark,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    // TAB 2: ADETSEL YAPILDI
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedTab = AdetselTab.YAPILDI },
                        color = if (selectedTab == AdetselTab.YAPILDI) TurquoiseDark else Slate100,
                        shape = RoundedCornerShape(8.dp),
                        border = if (selectedTab == AdetselTab.YAPILDI) null else BorderStroke(1.dp, Slate200)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "YAPILDI",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == AdetselTab.YAPILDI) Color.White else Slate700
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (selectedTab == AdetselTab.YAPILDI) Color.White.copy(alpha = 0.25f) else NormalGreen.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "${yapildiList.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (selectedTab == AdetselTab.YAPILDI) Color.White else NormalGreenDark,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // SEARCH & FILTER BAR
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // SAYIM İÇİ ARAMA VE LİSTE FİLTRELEME ALANI (OutlinedTextField)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { input ->
                    searchQuery = input.replace("\n", "").replace("\r", "")
                },
                placeholder = {
                    Text(
                        text = "Sayım listesinde ara (isim veya barkod)...",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Ara",
                        tint = TurquoiseDark,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Temizle",
                                    tint = Slate500,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                // Kullanıcıyı harici sayfaya yönlendirme, sayfa içi kamera aç
                                showBarcodeScanner = true
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("adetsel_barcode_scanner_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Kamera ile Barkod Oku",
                                tint = TurquoiseDark,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search,
                    keyboardType = KeyboardType.Text
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    },
                    onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TurquoiseDark,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(searchFocusRequester)
                    .testTag("adetsel_search_input")
            )

            // 3. ÜRÜN BULUNAMAZSA KIRMIZI UYARI ROZETİ
            AnimatedVisibility(
                visible = notFoundWarning != null,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it }
            ) {
                notFoundWarning?.let { warningText ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = ExpiredRedDark,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = warningText,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { notFoundWarning = null },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Kapat",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // YAPILDI TAB FILTER PILLS (TÜMÜ, EKSİK, FAZLA, TAM)
            if (selectedTab == AdetselTab.YAPILDI && yapildiList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val eksikCount = remember(yapildiList) { yapildiList.count { it.sayimSonucu == "EKSIK" } }
                    val fazlaCount = remember(yapildiList) { yapildiList.count { it.sayimSonucu == "FAZLA" } }
                    val tamCount = remember(yapildiList) { yapildiList.count { it.sayimSonucu == "TAM" } }

                    AdetselFilterChip(
                        label = "Tümü (${yapildiList.size})",
                        isSelected = selectedResultFilter == "ALL",
                        onClick = { selectedResultFilter = "ALL" },
                        color = TurquoiseDark
                    )
                    AdetselFilterChip(
                        label = "Eksik ($eksikCount)",
                        isSelected = selectedResultFilter == "EKSIK",
                        onClick = { selectedResultFilter = "EKSIK" },
                        color = ExpiredRedDark
                    )
                    AdetselFilterChip(
                        label = "Fazla ($fazlaCount)",
                        isSelected = selectedResultFilter == "FAZLA",
                        onClick = { selectedResultFilter = "FAZLA" },
                        color = WarningBlueDark
                    )
                    AdetselFilterChip(
                        label = "Tam ($tamCount)",
                        isSelected = selectedResultFilter == "TAM",
                        onClick = { selectedResultFilter = "TAM" },
                        color = NormalGreenDark
                    )
                }
            }
        }

        // CONTENT BODY
        when (selectedTab) {
            AdetselTab.YAPILACAK -> {
                if (filteredYapilacak.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(TurquoisePrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.FactCheck,
                                    contentDescription = null,
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Ürün bulunamadı" else "Sayım Listesi Boş",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Arama terimini kontrol edin." else "Ürün detayından 'Sayıma Ekle' butonuna dokunarak listeye ürün aktarabilirsiniz.",
                                fontSize = 12.sp,
                                color = Slate500,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            if (searchQuery.isEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = onNavigateToProducts,
                                    colors = ButtonDefaults.buttonColors(containerColor = TurquoiseDark),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ürünler Sayfasına Git", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp, top = 8.dp)
                    ) {
                        items(filteredYapilacak, key = { it.id }) { kayit ->
                            CompactYapilacakCard(
                                kayit = kayit,
                                onClick = {
                                    initialModeForDialog = "TAM"
                                    countingKayit = kayit
                                },
                                onDelete = { itemToDelete = kayit }
                            )
                        }
                    }
                }
            }

            AdetselTab.YAPILDI -> {
                if (filteredYapildi.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(NormalGreen.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = NormalGreenDark,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty() || selectedResultFilter != "ALL") "Filtreye uygun kayıt yok" else "Henüz Sayılan Ürün Yok",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tamamlanan adetsel sayımlar burada listelenir.",
                                fontSize = 12.sp,
                                color = Slate500,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp, top = 8.dp)
                    ) {
                        items(filteredYapildi, key = { it.id }) { kayit ->
                            CompactYapildiCard(
                                kayit = kayit,
                                onUndo = { onUndoSayim(kayit) },
                                onDelete = { itemToDelete = kayit }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showExportModal) {
        val allSayimRecords = distinctYapilacakList + yapildiList
        com.example.ui.components.ModuleExportDialog(
            title = "Sayım Sonucunu Dışa Aktar",
            subtitle = "Mağaza sayım sonuçlarını (Barkod, Ürün Adı, Sistem Stoğu, Sayılan, Fark) dışa aktarın.",
            recordCountText = "${allSayimRecords.size} Sayım Kaydı (${distinctYapilacakList.size} Yapılacak, ${yapildiList.size} Yapıldı)",
            onExportCsvDownload = {
                if (allSayimRecords.isEmpty()) {
                    Toast.makeText(context, "Dışa aktarılacak sayım kaydı bulunamadı.", Toast.LENGTH_SHORT).show()
                } else {
                    val csv = com.example.util.BackupExportHelper.exportAdetselSayimToCsv(allSayimRecords)
                    val fileName = com.example.util.BackupExportHelper.generateBackupFileName("adetsel_sayim_listesi", "csv")
                    val res = com.example.util.BackupExportHelper.saveFileToDownloads(context, csv, fileName, "text/csv")
                    if (res.isSuccess) {
                        Toast.makeText(context, "✅ CSV dosyası İndirilenler klasörüne kaydedildi:\n$fileName", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Kayıt hatası: ${res.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                    showExportModal = false
                }
            },
            onExportCsvShare = {
                if (allSayimRecords.isEmpty()) {
                    Toast.makeText(context, "Dışa aktarılacak sayım kaydı bulunamadı.", Toast.LENGTH_SHORT).show()
                } else {
                    val csv = com.example.util.BackupExportHelper.exportAdetselSayimToCsv(allSayimRecords)
                    val fileName = com.example.util.BackupExportHelper.generateBackupFileName("adetsel_sayim_listesi", "csv")
                    com.example.util.BackupExportHelper.shareFile(
                        context = context,
                        content = csv,
                        fileName = fileName,
                        mimeType = "text/csv",
                        subject = "Adetsel Sayım Listesi (CSV)",
                        bodyText = "Ekli CSV dosyasında ${allSayimRecords.size} adet sayım kaydı bulunmaktadır."
                    )
                    showExportModal = false
                }
            },
            onExportTxtDownload = {
                if (allSayimRecords.isEmpty()) {
                    Toast.makeText(context, "Dışa aktarılacak sayım kaydı bulunamadı.", Toast.LENGTH_SHORT).show()
                } else {
                    val txt = com.example.util.BackupExportHelper.exportAdetselSayimToTxt(allSayimRecords)
                    val fileName = com.example.util.BackupExportHelper.generateBackupFileName("adetsel_sayim_raporu", "txt")
                    val res = com.example.util.BackupExportHelper.saveFileToDownloads(context, txt, fileName, "text/plain")
                    if (res.isSuccess) {
                        Toast.makeText(context, "✅ Metin raporu İndirilenler klasörüne kaydedildi:\n$fileName", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Kayıt hatası: ${res.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                    showExportModal = false
                }
            },
            onExportTxtShare = {
                if (allSayimRecords.isEmpty()) {
                    Toast.makeText(context, "Dışa aktarılacak sayım kaydı bulunamadı.", Toast.LENGTH_SHORT).show()
                } else {
                    val txt = com.example.util.BackupExportHelper.exportAdetselSayimToTxt(allSayimRecords)
                    val fileName = com.example.util.BackupExportHelper.generateBackupFileName("adetsel_sayim_raporu", "txt")
                    com.example.util.BackupExportHelper.shareFile(
                        context = context,
                        content = txt,
                        fileName = fileName,
                        mimeType = "text/plain",
                        subject = "Adetsel Sayım Raporu (.txt)",
                        bodyText = "Ekli metin raporunda mağaza sayım sonuçları (Barkod, Ürün Adı, Sistem Stoğu, Sayılan, Fark) yer almaktadır."
                    )
                    showExportModal = false
                }
            },
            onExportJsonDownload = {
                if (allSayimRecords.isEmpty()) {
                    Toast.makeText(context, "Dışa aktarılacak sayım kaydı bulunamadı.", Toast.LENGTH_SHORT).show()
                } else {
                    val json = com.example.util.BackupExportHelper.exportAdetselSayimToJson(allSayimRecords)
                    val fileName = com.example.util.BackupExportHelper.generateBackupFileName("adetsel_sayim_listesi", "json")
                    val res = com.example.util.BackupExportHelper.saveFileToDownloads(context, json, fileName, "application/json")
                    if (res.isSuccess) {
                        Toast.makeText(context, "✅ JSON yedeği İndirilenler klasörüne kaydedildi:\n$fileName", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Kayıt hatası: ${res.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                    showExportModal = false
                }
            },
            onExportJsonShare = {
                if (allSayimRecords.isEmpty()) {
                    Toast.makeText(context, "Dışa aktarılacak sayım kaydı bulunamadı.", Toast.LENGTH_SHORT).show()
                } else {
                    val json = com.example.util.BackupExportHelper.exportAdetselSayimToJson(allSayimRecords)
                    val fileName = com.example.util.BackupExportHelper.generateBackupFileName("adetsel_sayim_listesi", "json")
                    com.example.util.BackupExportHelper.shareFile(
                        context = context,
                        content = json,
                        fileName = fileName,
                        mimeType = "application/json",
                        subject = "Adetsel Sayım Listesi JSON Yedeği",
                        bodyText = "Ekli JSON dosyasında ${allSayimRecords.size} adet sayım kaydı bulunmaktadır."
                    )
                    showExportModal = false
                }
            },
            onDismiss = { showExportModal = false }
        )
    }
}
