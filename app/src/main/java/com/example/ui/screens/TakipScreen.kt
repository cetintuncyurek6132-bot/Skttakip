package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import com.example.ui.components.CustomBoxedCalendarDialog
import com.example.ui.screens.takip.AddEditTakipModal
import com.example.ui.screens.takip.TakipKaydiCard
import com.example.ui.screens.takip.TakipStatCard
import com.example.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakipScreen(
    products: List<Product> = emptyList(),
    onBackClick: () -> Unit = {},
    onOpenScanner: () -> Unit = {}
) {
    val context = LocalContext.current
    var records by remember { mutableStateOf(DepoIadeManager.loadRecords(context)) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDurumFilter by remember { mutableStateOf<IadeDurumu?>(null) }
    var selectedOncelikFilter by remember { mutableStateOf<IadeOncelik?>(null) }

    // Dialog & Modal State
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<DepoIadeKaydi?>(null) }
    var previewImagePath by remember { mutableStateOf<String?>(null) }
    var recordToDelete by remember { mutableStateOf<DepoIadeKaydi?>(null) }

    fun refreshRecords() {
        records = DepoIadeManager.loadRecords(context)
    }

    fun saveAndRefresh(newRecords: List<DepoIadeKaydi>) {
        DepoIadeManager.saveRecords(context, newRecords)
        records = newRecords
    }

    val stats = remember(records) {
        DepoIadeManager.calculateStats(records)
    }

    fun normalizeSearch(text: String): String {
        return text.lowercase(Locale.forLanguageTag("tr-TR"))
            .replace("ı", "i")
            .replace("ü", "u")
            .replace("ö", "o")
            .replace("ş", "s")
            .replace("ğ", "g")
            .replace("ç", "c")
            .trim()
    }

    val filteredRecords = remember(records, searchQuery, selectedDurumFilter, selectedOncelikFilter) {
        val normQuery = normalizeSearch(searchQuery)
        records.filter { record ->
            val matchesQuery = normQuery.isBlank() ||
                    normalizeSearch(record.urunAdi).contains(normQuery) ||
                    normalizeSearch(record.aciklama).contains(normQuery) ||
                    normalizeSearch(record.redNedeni).contains(normQuery)

            val matchesDurum = selectedDurumFilter == null || record.durum == selectedDurumFilter
            val matchesOncelik = selectedOncelikFilter == null || record.oncelik == selectedOncelikFilter

            matchesQuery && matchesDurum && matchesOncelik
        }.sortedWith(
            compareByDescending<DepoIadeKaydi> { it.isKritik }
                .thenByDescending { it.guncellemeTarihiMillis }
        )
    }

    fun shareRecordOnWhatsApp(record: DepoIadeKaydi) {
        val message = buildString {
            append("📦 *DEPO / İADE RED TAKİP BİLDİRİMİ*\n\n")
            append("• *Ürün:* ${record.urunAdi}\n")
            append("• *Durum:* ${record.durum.displayName}\n")
            append("• *Öncelik:* ${record.oncelik.displayName}\n")
            append("• *İade Tarihi:* ${record.iadeTarihi}\n")
            append("• *Red/İade Nedeni:* ${record.redNedeni}\n")
            if (record.aciklama.isNotBlank()) {
                append("• *Açıklama / Not:* ${record.aciklama}\n")
            }
            if (record.hatirlatmaTarihi.isNotBlank()) {
                append("• *Takip Tarihi:* ${record.hatirlatmaTarihi}\n")
            }
            if (record.hasGorsel) {
                append("• *İrsaliye Görseli:* Mevcut\n")
            }
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            `package` = "com.whatsapp"
        }
        try {
            context.startActivity(sendIntent)
        } catch (e: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            try {
                context.startActivity(Intent.createChooser(fallbackIntent, "Takip Kaydını Paylaş"))
            } catch (ex: Exception) {
                Toast.makeText(context, "Paylaşım uygulaması açılamadı.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareAllRecordsOnWhatsApp() {
        if (records.isEmpty()) {
            Toast.makeText(context, "Paylaşılacak takip kaydı bulunamadı.", Toast.LENGTH_SHORT).show()
            return
        }
        val message = buildString {
            append("📋 *İADE VE DEPO RED TAKİP LİSTESİ*\n")
            append("Tarih: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR")).format(Date())}\n")
            append("Toplam: ${records.size} Kayıt | Devam Eden: ${stats.devamEdenSayisi} | Onaylanan: ${stats.onaylananSayisi} | Red: ${stats.reddedilenSayisi}\n\n")

            records.forEachIndexed { index, r ->
                val emoji = when (r.durum) {
                    IadeDurumu.DEVAM_EDIYOR -> "⏳"
                    IadeDurumu.ONAYLANDI -> "✅"
                    IadeDurumu.REDDEDILDI -> "❌"
                }
                val prio = if (r.isKritik) " [🚨 KRİTİK]" else ""
                append("${index + 1}. $emoji *${r.urunAdi}*$prio\n")
                append("   • Durum: ${r.durum.displayName} (${r.redNedeni})\n")
                append("   • İade Tarihi: ${r.iadeTarihi}")
                if (r.aciklama.isNotBlank()) {
                    append(" | Not: ${r.aciklama}")
                }
                append("\n\n")
            }
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            `package` = "com.whatsapp"
        }
        try {
            context.startActivity(sendIntent)
        } catch (e: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            try {
                context.startActivity(Intent.createChooser(fallbackIntent, "Takip Listesini Paylaş"))
            } catch (ex: Exception) {
                Toast.makeText(context, "Paylaşım uygulaması açılamadı.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
    ) {
        // KOMPAKT SAYFA BAŞLIĞI VE İŞLEMLER
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "İade & Depo Takibi",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            fontSize = 17.sp
                        )
                    )
                    Text(
                        text = "${filteredRecords.size} aktif takip kaydı",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Slate500,
                            fontSize = 11.5.sp
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { shareAllRecordsOnWhatsApp() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFE8F5E9), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "WhatsApp ile Paylaş",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            editingRecord = null
                            showAddEditDialog = true
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = TurquoisePrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Yeni Kayıt",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. İSTATİSTİK KARTLARI
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TakipStatCard(
                        title = "Toplam",
                        count = stats.toplamKayit,
                        color = Slate700,
                        bgColor = Slate100,
                        modifier = Modifier.weight(1f)
                    )
                    TakipStatCard(
                        title = "Devam Eden",
                        count = stats.devamEdenSayisi,
                        color = Color(0xFFD97706),
                        bgColor = Color(0xFFFEF3C7),
                        modifier = Modifier.weight(1f)
                    )
                    TakipStatCard(
                        title = "Onaylanan",
                        count = stats.onaylananSayisi,
                        color = Color(0xFF16A34A),
                        bgColor = Color(0xFFDCFCE7),
                        modifier = Modifier.weight(1f)
                    )
                    TakipStatCard(
                        title = "Reddedilen",
                        count = stats.reddedilenSayisi,
                        color = ExpiredRed,
                        bgColor = ExpiredRedContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. ARAMA VE FİLTRELEME
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Arama Girişi
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it.uppercase(java.util.Locale.forLanguageTag("tr-TR")) },
                            placeholder = { Text("Ürün adı veya açıklama ile ara...", fontSize = 14.sp, color = Slate500) },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters
                            ),
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Slate500, modifier = Modifier.size(20.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Temizle", tint = Slate500, modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TurquoisePrimary,
                                unfocusedBorderColor = Slate200,
                                focusedContainerColor = Slate50,
                                unfocusedContainerColor = Slate50
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        )

                        // Durum Filtresi Çipleri
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedDurumFilter == null,
                                    onClick = { selectedDurumFilter = null },
                                    label = { Text("Tüm Durumlar (${records.size})", fontSize = 12.sp) },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Slate700,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedDurumFilter == IadeDurumu.DEVAM_EDIYOR,
                                    onClick = {
                                        selectedDurumFilter = if (selectedDurumFilter == IadeDurumu.DEVAM_EDIYOR) null else IadeDurumu.DEVAM_EDIYOR
                                    },
                                    label = { Text("Devam Eden ⏳ (${stats.devamEdenSayisi})", fontSize = 12.sp) },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFD97706),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedDurumFilter == IadeDurumu.ONAYLANDI,
                                    onClick = {
                                        selectedDurumFilter = if (selectedDurumFilter == IadeDurumu.ONAYLANDI) null else IadeDurumu.ONAYLANDI
                                    },
                                    label = { Text("Onaylanan ✅ (${stats.onaylananSayisi})", fontSize = 12.sp) },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF16A34A),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedDurumFilter == IadeDurumu.REDDEDILDI,
                                    onClick = {
                                        selectedDurumFilter = if (selectedDurumFilter == IadeDurumu.REDDEDILDI) null else IadeDurumu.REDDEDILDI
                                    },
                                    label = { Text("Reddedilen ❌ (${stats.reddedilenSayisi})", fontSize = 12.sp) },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ExpiredRed,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 3. KAYITLAR LİSTESİ
            if (filteredRecords.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Slate100, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.FactCheck,
                                    contentDescription = null,
                                    tint = Slate500,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty() || selectedDurumFilter != null)
                                    "Filtrelere uygun takip kaydı bulunamadı."
                                else
                                    "Henüz kayıtlı bir iade / depo red kaydı yok.",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate700
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Depodan dönen veya iade edilen ürünleri ekleyerek takip sürecini başlatabilirsiniz.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate500),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    editingRecord = null
                                    showAddEditDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Yeni Takip Kaydı Ekle")
                            }
                        }
                    }
                }
            } else {
                items(filteredRecords, key = { it.id }) { record ->
                    TakipKaydiCard(
                        record = record,
                        onStatusChange = { newStatus ->
                            val updated = records.map {
                                if (it.id == record.id) it.copy(
                                    durum = newStatus,
                                    guncellemeTarihiMillis = System.currentTimeMillis()
                                ) else it
                            }
                            saveAndRefresh(updated)
                            Toast.makeText(context, "Durum '${newStatus.displayName}' olarak güncellendi.", Toast.LENGTH_SHORT).show()
                        },
                        onEditClick = {
                            editingRecord = record
                            showAddEditDialog = true
                        },
                        onDeleteClick = {
                            recordToDelete = record
                        },
                        onShareClick = {
                            shareRecordOnWhatsApp(record)
                        },
                        onImageClick = { imgPath ->
                            previewImagePath = imgPath
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // YENİ EKLE / DÜZENLE MODAL
    if (showAddEditDialog) {
        AddEditTakipModal(
            existingRecord = editingRecord,
            availableProducts = products,
            onDismiss = {
                showAddEditDialog = false
                editingRecord = null
            },
            onSave = { updatedOrNewRecord ->
                val updatedList = if (editingRecord != null) {
                    records.map { if (it.id == updatedOrNewRecord.id) updatedOrNewRecord else it }
                } else {
                    listOf(updatedOrNewRecord) + records
                }
                saveAndRefresh(updatedList)
                showAddEditDialog = false
                editingRecord = null
                Toast.makeText(context, "Takip kaydı kaydedildi.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // SİLME ONAY DİYALOĞU
    if (recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("Takip Kaydını Sil", fontWeight = FontWeight.Bold) },
            text = {
                Text("'${recordToDelete?.urunAdi}' için oluşturulan takip kaydı silinecektir. Bu işlem geri alınamaz.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toRemove = recordToDelete!!
                        DepoIadeManager.deleteImageFile(toRemove.irsaliyeGorselPath)
                        val updated = records.filter { it.id != toRemove.id }
                        saveAndRefresh(updated)
                        recordToDelete = null
                        Toast.makeText(context, "Kayıt ve görseli silindi.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpiredRed)
                ) {
                    Text("Sil", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { recordToDelete = null }) {
                    Text("İptal")
                }
            }
        )
    }

    // GÖRSEL TAM EKRAN ÖNİZLEME DİYALOĞU
    if (previewImagePath != null) {
        Dialog(
            onDismissRequest = { previewImagePath = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val file = File(previewImagePath!!)
                if (file.exists()) {
                    val bitmap = remember(previewImagePath) {
                        try {
                            BitmapFactory.decodeFile(file.absolutePath)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "İrsaliye Belgesi",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                IconButton(
                    onClick = { previewImagePath = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 24.dp, end = 8.dp)
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
                }
            }
        }
    }
}

