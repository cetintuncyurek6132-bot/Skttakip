package com.example.ui.screens.takip

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.DepoIadeKaydi
import com.example.data.DepoIadeManager
import com.example.data.IadeDurumu
import com.example.data.IadeOncelik
import com.example.data.IadeRedNedeni
import com.example.data.Product
import com.example.ui.components.CustomBoxedCalendarDialog
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
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTakipModal(
    existingRecord: DepoIadeKaydi? = null,
    availableProducts: List<Product> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (DepoIadeKaydi) -> Unit
) {
    val context = LocalContext.current
    var urunKoduInput by remember(existingRecord) { mutableStateOf(existingRecord?.urunKodu ?: "") }
    var urunAdi by remember(existingRecord) { mutableStateOf(existingRecord?.urunAdi ?: "") }
    var matchedProduct by remember { mutableStateOf<Product?>(null) }
    var hasSearchedCode by remember { mutableStateOf(false) }
    var isNotFoundByCode by remember { mutableStateOf(false) }
    var showManualNameInput by remember { mutableStateOf(true) }

    var selectedRedNedeni by remember(existingRecord) {
        mutableStateOf(
            if (existingRecord != null && IadeRedNedeni.values().none { it.displayName == existingRecord.redNedeni }) {
                IadeRedNedeni.DIGER.displayName
            } else {
                existingRecord?.redNedeni ?: IadeRedNedeni.DEPO_KABUL_ETMEDI.displayName
            }
        )
    }
    var customRedNedeni by remember(existingRecord) {
        mutableStateOf(
            if (existingRecord != null && IadeRedNedeni.values().none { it.displayName == existingRecord.redNedeni }) {
                existingRecord.redNedeni
            } else ""
        )
    }
    var oncelik by remember(existingRecord) { mutableStateOf(existingRecord?.oncelik ?: IadeOncelik.NORMAL) }
    var durum by remember(existingRecord) { mutableStateOf(existingRecord?.durum ?: IadeDurumu.DEVAM_EDIYOR) }
    var aciklama by remember(existingRecord) { mutableStateOf(existingRecord?.aciklama ?: "") }
    var iadeTarihi by remember(existingRecord) { mutableStateOf(existingRecord?.iadeTarihi ?: DepoIadeManager.getTodayDateString()) }
    var hatirlatmaTarihi by remember(existingRecord) { mutableStateOf(existingRecord?.hatirlatmaTarihi ?: "") }
    var gorselPath by remember(existingRecord) { mutableStateOf<String?>(existingRecord?.irsaliyeGorselPath) }

    var showDatePickerForIade by remember { mutableStateOf(false) }
    var showDatePickerForHatirlatma by remember { mutableStateOf(false) }
    var showRedNedeniDropdown by remember { mutableStateOf(false) }

    // Düzenleme modunda mevcut kaydı güvenli şekilde state'e aktar
    LaunchedEffect(existingRecord) {
        if (existingRecord != null) {
            urunKoduInput = existingRecord.urunKodu ?: ""
            urunAdi = existingRecord.urunAdi
            selectedRedNedeni = if (IadeRedNedeni.values().none { it.displayName == existingRecord.redNedeni }) {
                IadeRedNedeni.DIGER.displayName
            } else {
                existingRecord.redNedeni
            }
            customRedNedeni = if (IadeRedNedeni.values().none { it.displayName == existingRecord.redNedeni }) {
                existingRecord.redNedeni
            } else ""
            oncelik = existingRecord.oncelik
            durum = existingRecord.durum
            aciklama = existingRecord.aciklama
            iadeTarihi = existingRecord.iadeTarihi.ifBlank { DepoIadeManager.getTodayDateString() }
            hatirlatmaTarihi = existingRecord.hatirlatmaTarihi
            gorselPath = existingRecord.irsaliyeGorselPath
            showManualNameInput = true

            val found = availableProducts.firstOrNull { prod ->
                (!existingRecord.urunKodu.isNullOrBlank() && prod.urunKodu.equals(existingRecord.urunKodu, ignoreCase = true)) ||
                (!existingRecord.urunKodu.isNullOrBlank() && prod.barkod == existingRecord.urunKodu) ||
                prod.urunAdi.equals(existingRecord.urunAdi, ignoreCase = true)
            }
            if (found != null) {
                matchedProduct = found
                if (urunKoduInput.isBlank() && found.urunKodu.isNotBlank()) {
                    urunKoduInput = found.urunKodu
                }
            }
        }
    }

    fun performProductCodeLookup(code: String) {
        val trimmed = code.trim()
        if (trimmed.isBlank()) {
            isNotFoundByCode = false
            hasSearchedCode = false
            matchedProduct = null
            return
        }

        val found = availableProducts.firstOrNull { prod ->
            (prod.urunKodu.isNotBlank() && prod.urunKodu.equals(trimmed, ignoreCase = true)) ||
            prod.barkod == trimmed ||
            (trimmed.length >= 4 && prod.barkod.endsWith(trimmed)) ||
            (trimmed.length >= 3 && prod.urunKodu.contains(trimmed, ignoreCase = true))
        }

        hasSearchedCode = true
        if (found != null) {
            matchedProduct = found
            urunAdi = found.urunAdi
            urunKoduInput = if (found.urunKodu.isNotBlank()) found.urunKodu else found.barkod
            isNotFoundByCode = false
            showManualNameInput = true
            Toast.makeText(context, "Eşleşen ürün bulundu: ${found.urunAdi}", Toast.LENGTH_SHORT).show()
        } else {
            matchedProduct = null
            isNotFoundByCode = true
            showManualNameInput = true // Ürün kodu ile kayıtlı ürün yoksa hemen altına ürün adı yeri açılsın
            Toast.makeText(context, "Kayıtlı ürün bulunamadı. Lütfen ürün adını giriniz.", Toast.LENGTH_SHORT).show()
        }
    }

    // Görsel Seçici Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = DepoIadeManager.saveImageToInternalStorage(context, uri)
            if (savedPath != null) {
                gorselPath = savedPath
                Toast.makeText(context, "İrsaliye görseli eklendi.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    com.example.ui.components.AppBottomSheetWrapper(
        onDismissRequest = onDismiss
    ) { dismissSheet ->
        // Başlık
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (existingRecord == null) "Yeni Takip Kaydı" else "Kaydı Düzenle",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            fontSize = 18.sp
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Slate500)
                    }
                }

                HorizontalDivider(color = Slate200, modifier = Modifier.padding(horizontal = 16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. ÜRÜN KODU & ÜRÜN ADI (KOD YAZIP ENTERLEYİNCE BİLGİLER GELİR)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Ürün Kodu Giriş Alanı
                            Text(
                                text = "Ürün Kodu *",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate700)
                            )
                            OutlinedTextField(
                                value = urunKoduInput,
                                onValueChange = {
                                    urunKoduInput = it.trim()
                                    if (hasSearchedCode && it != (matchedProduct?.urunKodu ?: "")) {
                                        hasSearchedCode = false
                                        isNotFoundByCode = false
                                        matchedProduct = null
                                    }
                                },
                                placeholder = { Text("Ürün kodu veya barkod girip Enter'a basın...", color = Slate400, fontSize = 13.5.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Search
                                ),
                                keyboardActions = KeyboardActions(
                                    onSearch = { performProductCodeLookup(urunKoduInput) },
                                    onDone = { performProductCodeLookup(urunKoduInput) }
                                ),
                                trailingIcon = {
                                    IconButton(onClick = { performProductCodeLookup(urunKoduInput) }) {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = "Ürün Kodunu Ara / Getir",
                                            tint = TurquoiseDark
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Ürün Kodu Eşleşen Ürün Otomatik Bilgileri
                            if (matchedProduct != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = TurquoisePrimary.copy(alpha = 0.08f),
                                    border = BorderStroke(1.2.dp, TurquoisePrimary.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = TurquoiseDark,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Eşleşen Ürün: ${matchedProduct!!.urunAdi}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = Slate900
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Kod: ${matchedProduct!!.urunKodu.ifBlank { "-" }} • Barkod: ${matchedProduct!!.barkod} • Kat: ${matchedProduct!!.kategori}",
                                                fontSize = 11.5.sp,
                                                color = Slate600
                                            )
                                        }
                                    }
                                }
                            }

                            // Eğer ürün kodu ile kayıtlı ürün yoksa uyarı rozeti
                            if (isNotFoundByCode) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = ExpiredRedContainer.copy(alpha = 0.45f),
                                    border = BorderStroke(1.dp, ExpiredRed.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            tint = ExpiredRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Bu kod ile kayıtlı ürün bulunamadı. Lütfen ürün adını aşağıya yazınız.",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = ExpiredRed
                                        )
                                    }
                                }
                            }

                            // Ürün Adı Giriş Alanı (Daima eksiksiz render edilir)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Ürün Adı *",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate700)
                                )
                                OutlinedTextField(
                                    value = urunAdi,
                                    onValueChange = { urunAdi = it.uppercase(java.util.Locale.forLanguageTag("tr-TR")) },
                                    placeholder = { Text("Örn: Süt 1L Yarım Yağlı...", color = Slate400, fontSize = 14.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Characters
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Hızlı Öneri Çipleri (Varsa)
                                if (urunAdi.isNotEmpty() && urunAdi.length >= 2 && matchedProduct == null) {
                                    val suggestions = remember(urunAdi, availableProducts) {
                                        availableProducts.filter {
                                            it.urunAdi.contains(urunAdi, ignoreCase = true)
                                        }.take(4)
                                    }
                                    if (suggestions.isNotEmpty()) {
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            items(suggestions) { prod ->
                                                SuggestionChip(
                                                    onClick = {
                                                        urunAdi = prod.urunAdi
                                                        if (urunKoduInput.isBlank() && prod.urunKodu.isNotBlank()) {
                                                            urunKoduInput = prod.urunKodu
                                                        }
                                                        matchedProduct = prod
                                                    },
                                                    label = { Text(prod.urunAdi, fontSize = 11.sp, maxLines = 1) },
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    // 2. RED / İADE NEDENİ (DİĞER DİYİNCE YAZMA KUTUSU GELİR)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Red / İade Nedeni *",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate700)
                            )

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedCard(
                                    onClick = { showRedNedeniDropdown = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.outlinedCardColors(containerColor = Slate50),
                                    border = BorderStroke(1.dp, Slate200),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedRedNedeni,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Slate900
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Slate600)
                                    }
                                }

                                DropdownMenu(
                                    expanded = showRedNedeniDropdown,
                                    onDismissRequest = { showRedNedeniDropdown = false }
                                ) {
                                    IadeRedNedeni.values().forEach { neden ->
                                        DropdownMenuItem(
                                            text = { Text(neden.displayName) },
                                            onClick = {
                                                selectedRedNedeni = neden.displayName
                                                showRedNedeniDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Red iade nedeni "Diğer" seçilirse yazma kutusu açılsın
                            if (selectedRedNedeni == IadeRedNedeni.DIGER.displayName || selectedRedNedeni == "Diğer") {
                                OutlinedTextField(
                                    value = customRedNedeni,
                                    onValueChange = { customRedNedeni = it },
                                    placeholder = { Text("Red / iade nedenini detaylı belirtin...", color = Slate400, fontSize = 13.sp) },
                                    label = { Text("Red İade Nedeni Yazın *", fontSize = 11.5.sp, color = TurquoiseDark) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    textStyle = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                )
                            }
                        }

                    // 3. ÖNCELİK VE DURUM
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Öncelik
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Öncelik",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate700)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IadeOncelik.values().forEach { p ->
                                        val isSelected = oncelik == p
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { oncelik = p },
                                            label = { Text(p.displayName, fontSize = 11.sp) },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = if (p == IadeOncelik.KRITIK) ExpiredRed else TurquoisePrimary,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }

                    // Süreç Durumu (Yazının aşağı kaymasını önleyen tek satır ortalı butonlar)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Süreç Durumu",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate700)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IadeDurumu.values().forEach { d ->
                                    val isSelected = durum == d
                                    val chipColor = when (d) {
                                        IadeDurumu.DEVAM_EDIYOR -> Color(0xFFD97706)
                                        IadeDurumu.ONAYLANDI -> Color(0xFF16A34A)
                                        IadeDurumu.REDDEDILDI -> ExpiredRed
                                    }
                                    val unselectedBg = when (d) {
                                        IadeDurumu.DEVAM_EDIYOR -> Color(0xFFFFFBEB)
                                        IadeDurumu.ONAYLANDI -> Color(0xFFF0FDF4)
                                        IadeDurumu.REDDEDILDI -> Color(0xFFFEF2F2)
                                    }
                                    val unselectedBorder = when (d) {
                                        IadeDurumu.DEVAM_EDIYOR -> Color(0xFFFDE68A)
                                        IadeDurumu.ONAYLANDI -> Color(0xFFBBF7D0)
                                        IadeDurumu.REDDEDILDI -> Color(0xFFFECACA)
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { durum = d },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) chipColor else unselectedBg,
                                        border = BorderStroke(1.dp, if (isSelected) chipColor else unselectedBorder)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = d.displayName,
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isSelected) Color.White else Slate700,
                                                maxLines = 1,
                                                softWrap = false,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                    // 4. İADE TARİHİ VE HATIRLATMA
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // İade Tarihi
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "İade Tarihi",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate700)
                                )
                                OutlinedCard(
                                    onClick = { showDatePickerForIade = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.outlinedCardColors(containerColor = Slate50),
                                    border = BorderStroke(1.dp, Slate200),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(iadeTarihi.ifBlank { "Tarih Seç" }, fontSize = 13.sp, color = Slate900)
                                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TurquoisePrimary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            // Hatırlatma Tarihi
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Takip Hatırlatıcısı",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate700)
                                )
                                OutlinedCard(
                                    onClick = { showDatePickerForHatirlatma = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.outlinedCardColors(containerColor = Slate50),
                                    border = BorderStroke(1.dp, Slate200),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(hatirlatmaTarihi.ifBlank { "İsteğe bağlı" }, fontSize = 13.sp, color = if (hatirlatmaTarihi.isBlank()) Slate400 else Slate900)
                                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                    // 5. AÇIKLAMA / NOT
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Açıklama & Takip Notu",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate700)
                        )
                        OutlinedTextField(
                            value = aciklama,
                            onValueChange = { aciklama = it },
                            placeholder = { Text("Örn: İrsaliye numarası, ambar teslim tutanağı, şoför adı vb...", color = Slate400, fontSize = 13.sp) },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 6. İRSALİYE GÖRSELİ EKLEME
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "İrsaliye / Belge Fotoğrafı",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate700)
                            )

                            if (gorselPath != null && File(gorselPath!!).exists()) {
                                val bitmap = remember(gorselPath) {
                                    try {
                                        BitmapFactory.decodeFile(gorselPath!!)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Slate100, RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Görsel",
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("İrsaliye Belgesi Yüklendi", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("Değiştirmek veya kaldırmak için butonları kullanın.", fontSize = 11.sp, color = Slate500)
                                    }
                                    IconButton(onClick = { gorselPath = null }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Kaldır", tint = ExpiredRed)
                                    }
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { photoPickerLauncher.launch("image/*") },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, TurquoisePrimary),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TurquoiseDark),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("İrsaliye Fotoğrafı / Belge Ekle")
                                }
                            }
                        }
                }

                HorizontalDivider(color = Slate200, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))

                // Kaydet & İptal Butonları
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("İptal")
                    }

                    Button(
                        onClick = {
                            val finalUrunAdi = urunAdi.trim()
                            if (finalUrunAdi.isBlank()) {
                                Toast.makeText(context, "Lütfen ürün adını giriniz.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val isDiger = selectedRedNedeni == IadeRedNedeni.DIGER.displayName || selectedRedNedeni == "Diğer"
                            if (isDiger && customRedNedeni.isBlank()) {
                                Toast.makeText(context, "Lütfen red / iade nedenini yazınız.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val finalRedNedeni = if (isDiger && customRedNedeni.isNotBlank()) {
                                customRedNedeni.trim()
                            } else {
                                selectedRedNedeni
                            }

                            val record = DepoIadeKaydi(
                                id = existingRecord?.id ?: System.currentTimeMillis().toString(),
                                urunAdi = finalUrunAdi,
                                urunKodu = urunKoduInput.trim().takeIf { it.isNotBlank() },
                                irsaliyeGorselPath = gorselPath,
                                iadeTarihi = iadeTarihi.ifBlank { DepoIadeManager.getTodayDateString() },
                                iadeTarihiMillis = DepoIadeManager.parseDateToMillis(iadeTarihi),
                                redNedeni = finalRedNedeni,
                                aciklama = aciklama.trim(),
                                oncelik = oncelik,
                                durum = durum,
                                hatirlatmaTarihi = hatirlatmaTarihi,
                                hatirlatmaTarihiMillis = if (hatirlatmaTarihi.isNotBlank()) DepoIadeManager.parseDateToMillis(hatirlatmaTarihi) else null,
                                olusturmaTarihiMillis = existingRecord?.olusturmaTarihiMillis ?: System.currentTimeMillis(),
                                guncellemeTarihiMillis = System.currentTimeMillis()
                            )
                            if (record.hatirlatmaTarihiMillis != null) {
                                DepoIadeManager.scheduleNotification(context, record)
                            }
                            onSave(record)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kaydet", fontWeight = FontWeight.Bold)
                    }
                }
    }

    // TARİH SEÇİCİ MODALLAR
    if (showDatePickerForIade) {
        CustomBoxedCalendarDialog(
            initialDateMillis = DepoIadeManager.parseDateToMillis(iadeTarihi),
            onDismissRequest = { showDatePickerForIade = false },
            onDateSelected = { selectedMillis ->
                iadeTarihi = DepoIadeManager.formatMillisToDate(selectedMillis)
                showDatePickerForIade = false
            }
        )
    }

    if (showDatePickerForHatirlatma) {
        CustomBoxedCalendarDialog(
            initialDateMillis = if (hatirlatmaTarihi.isNotBlank()) DepoIadeManager.parseDateToMillis(hatirlatmaTarihi) else System.currentTimeMillis(),
            onDismissRequest = { showDatePickerForHatirlatma = false },
            onDateSelected = { selectedMillis ->
                hatirlatmaTarihi = DepoIadeManager.formatMillisToDate(selectedMillis)
                showDatePickerForHatirlatma = false
            }
        )
    }
}
