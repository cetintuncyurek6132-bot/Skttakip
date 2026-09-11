package com.example.ui.screens.takip

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
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
    var urunAdi by remember { mutableStateOf(existingRecord?.urunAdi ?: "") }
    var selectedRedNedeni by remember {
        mutableStateOf(existingRecord?.redNedeni ?: IadeRedNedeni.DEPO_KABUL_ETMEDI.displayName)
    }
    var oncelik by remember { mutableStateOf(existingRecord?.oncelik ?: IadeOncelik.NORMAL) }
    var durum by remember { mutableStateOf(existingRecord?.durum ?: IadeDurumu.DEVAM_EDIYOR) }
    var aciklama by remember { mutableStateOf(existingRecord?.aciklama ?: "") }
    var iadeTarihi by remember { mutableStateOf(existingRecord?.iadeTarihi ?: DepoIadeManager.getTodayDateString()) }
    var hatirlatmaTarihi by remember { mutableStateOf(existingRecord?.hatirlatmaTarihi ?: "") }
    var gorselPath by remember { mutableStateOf<String?>(existingRecord?.irsaliyeGorselPath) }

    var showDatePickerForIade by remember { mutableStateOf(false) }
    var showDatePickerForHatirlatma by remember { mutableStateOf(false) }
    var showRedNedeniDropdown by remember { mutableStateOf(false) }

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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Başlık
                Row(
                    modifier = Modifier.fillMaxWidth(),
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

                HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. ÜRÜN ADI
                    item {
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
                            if (urunAdi.isNotEmpty() && urunAdi.length >= 2) {
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
                                                onClick = { urunAdi = prod.urunAdi },
                                                label = { Text(prod.urunAdi, fontSize = 11.sp, maxLines = 1) },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. RED / İADE NEDENİ
                    item {
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
                        }
                    }

                    // 3. ÖNCELİK VE DURUM
                    item {
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
                    }

                    item {
                        // Durum
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
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { durum = d },
                                        label = { Text(d.displayName, fontSize = 12.sp) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = chipColor,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // 4. İADE TARİHİ VE HATIRLATMA
                    item {
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
                    }

                    // 5. AÇIKLAMA / NOT
                    item {
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
                    }

                    // 6. İRSALİYE GÖRSELİ EKLEME
                    item {
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
                                            contentScale = ContentScale.Crop
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
                }

                HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 10.dp))

                // Kaydet & İptal Butonları
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                            if (urunAdi.isBlank()) {
                                Toast.makeText(context, "Lütfen ürün adını giriniz.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val record = DepoIadeKaydi(
                                id = existingRecord?.id ?: System.currentTimeMillis().toString(),
                                urunAdi = urunAdi.trim(),
                                irsaliyeGorselPath = gorselPath,
                                iadeTarihi = iadeTarihi.ifBlank { DepoIadeManager.getTodayDateString() },
                                iadeTarihiMillis = DepoIadeManager.parseDateToMillis(iadeTarihi),
                                redNedeni = selectedRedNedeni,
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
