package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.UserAccount
import com.example.auth.UserManager
import com.example.data.Product
import com.example.ui.theme.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

enum class ExportCategoryOption(
    val id: String,
    val title: String,
    val subtitle: String
) {
    DOLAP(
        id = "dolap_sooguk_urunler",
        title = "1. Kategori: Dolap / Soğuk Ürünler",
        subtitle = "Süt, Şarküteri, Soğuk Dolap, Peynir, Yoğurt vb."
    ),
    RAF_GIDA(
        id = "raf_gida_urunleri",
        title = "2. Kategori: Raf / Gıda & Diğer Ürünler",
        subtitle = "Atıştırmalık, Temel Gıda, İçecek, Temizlik vb."
    ),
    ALL(
        id = "tum_urunler",
        title = "Tüm Ürünler (Tüm Kategoriler)",
        subtitle = "Veritabanındaki tüm aktif ürün kayıtları"
    )
}

private fun isDolapCategory(kategori: String): Boolean {
    val cat = kategori.lowercase(Locale.forLanguageTag("tr-TR"))
    return cat.contains("dolap") || cat.contains("süt") || cat.contains("sut") ||
            cat.contains("şarküteri") || cat.contains("sarkuteri") || cat.contains("soğuk") ||
            cat.contains("soguk") || cat.contains("peynir") || cat.contains("yoğurt") ||
            cat.contains("yogurt") || cat.contains("et") || cat.contains("dondurma") ||
            cat.contains("tereyağ") || cat.contains("tereyag")
}

@Composable
fun CsvScreen(
    isDarkMode: Boolean = false,
    isBatterySaverMode: Boolean = false,
    soundEffectsEnabled: Boolean = true,
    vibrationEnabled: Boolean = true,
    products: List<Product> = emptyList(),
    onToggleDarkMode: () -> Unit = {},
    onToggleBatterySaverMode: () -> Unit = {},
    onToggleSoundEffects: () -> Unit = {},
    onToggleVibration: () -> Unit = {},
    onFixAndRepairDatabase: (onResult: (Int, String) -> Unit) -> Unit = {},
    onImportLines: (List<String>) -> Int = { 0 },
    onResetDatabase: () -> Unit = {},
    onRestoreSeedData: () -> Unit = {},
    onOpenQrFixMode: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentUser by UserManager.currentUser.collectAsState()
    val usersList by UserManager.usersList.collectAsState()

    val isMsUser = currentUser?.role == "MS"
    var selectedTab by remember { mutableIntStateOf(0) }

    // CSV Sub-Category states
    var selectedCsvSubTab by remember { mutableIntStateOf(0) } // 0: 1. Kategori (İçe Aktar), 1: 2. Kategori (Dışa Aktar)
    var isPreviewExpanded by remember { mutableStateOf(false) }

    val csvExportText = remember(products) {
        buildString {
            append("Ürün Barkodu,Ürün Kodu,Ürün Adı\n")
            products.forEach { p ->
                val cleanBarkod = p.barkod.trim().replace(",", " ")
                val cleanKod = p.urunKodu.trim().replace(",", " ")
                val cleanAd = p.urunAdi.trim().replace(",", " ")
                append("$cleanBarkod,$cleanKod,$cleanAd\n")
            }
        }
    }

    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            try {
                context.contentResolver.openOutputStream(fileUri)?.use { stream ->
                    stream.write(csvExportText.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "✅ CSV dosyası başarıyla kaydedildi!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Hata: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(isMsUser) {
        if (!isMsUser && selectedTab != 0) {
            selectedTab = 0
        }
    }
    var showResetDialog by remember { mutableStateOf(false) }

    // Repair state
    var isRepairing by remember { mutableStateOf(false) }
    var repairResultText by remember { mutableStateOf<String?>(null) }

    // User editing state
    var editingUser by remember { mutableStateOf<UserAccount?>(null) }
    var editNameInput by remember { mutableStateOf("") }
    var editPasswordInput by remember { mutableStateOf("") }

    // STORAGE ACCESS FRAMEWORK FILE LAUNCHER
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            try {
                val inputStream = context.contentResolver.openInputStream(fileUri)
                val bytes = inputStream?.use { it.readBytes() }
                if (bytes == null || bytes.isEmpty()) {
                    Toast.makeText(context, "Seçilen dosya boş!", Toast.LENGTH_SHORT).show()
                    return@let
                }

                // Check ZIP / XLSX signature (PK\u0003\u0004 -> 0x50, 0x4B, 0x03, 0x04) or XLS signature (0xD0, 0xCF, 0x11, 0xE0)
                val isZipOrXlsx = bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() && bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()
                val isOldXls = bytes.size >= 4 && bytes[0] == 0xD0.toByte() && bytes[1] == 0xCF.toByte() && bytes[2] == 0x11.toByte() && bytes[3] == 0xE0.toByte()

                if (isZipOrXlsx) {
                    val xlsxLines = com.example.util.XlsxParser.parseXlsxToCsvLines(bytes)
                    if (xlsxLines.isNotEmpty()) {
                        val newCount = onImportLines(xlsxLines)
                        if (newCount > 0) {
                            Toast.makeText(
                                context,
                                "✅ Excel (.xlsx) dosyasından $newCount adet yeni ürün başarıyla eklendi!",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Excel dosyasında eklenecek yeni ürün bulunamadı (Tüm ürünler kayıtlı veya geçersiz).",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return@let
                    } else {
                        Toast.makeText(context, "⚠️ Excel dosyası okunamadı veya içerik boş.", Toast.LENGTH_LONG).show()
                        return@let
                    }
                }

                if (isOldXls) {
                    Toast.makeText(context, "⚠️ Eski (.xls) formatı desteklenmiyor. Lütfen dosyanızı .xlsx veya .csv olarak kaydedip tekrar yükleyin.", Toast.LENGTH_LONG).show()
                    return@let
                }

                // Check for null bytes / binary file
                val nullByteCount = bytes.take(1024).count { it == 0.toByte() }
                if (nullByteCount > 5) {
                    Toast.makeText(context, "⚠️ Geçersiz dosya formatı! Lütfen .xlsx veya düz metin CSV dosyası yükleyin.", Toast.LENGTH_LONG).show()
                    return@let
                }

                // Multi-encoding decode (UTF-8, UTF-8 with BOM, Windows-1254)
                val charsetTurkish = try { java.nio.charset.Charset.forName("windows-1254") } catch (e: Exception) { java.nio.charset.StandardCharsets.ISO_8859_1 }
                var textContent = ""

                if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
                    textContent = String(bytes, 3, bytes.size - 3, java.nio.charset.StandardCharsets.UTF_8)
                } else {
                    val utf8Str = String(bytes, java.nio.charset.StandardCharsets.UTF_8)
                    if (utf8Str.contains("\uFFFD")) {
                        textContent = String(bytes, charsetTurkish)
                    } else {
                        textContent = utf8Str
                    }
                }

                val lines = textContent.lines()
                val newCount = onImportLines(lines)
                if (newCount > 0) {
                    Toast.makeText(
                        context,
                        "✅ $newCount adet yeni ürün başarıyla eklendi!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "Seçilen dosyada eklenecek yeni ürün bulunamadı (Tüm ürünler kayıtlı veya geçersiz).",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Dosya okunamadı: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Tüm Veriler Sıfırlansın Mı?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Veritabanındaki tüm ürünler ve sayım raporları silinecektir. Bu işlem geri alınamaz.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetDatabase()
                        showResetDialog = false
                        Toast.makeText(context, "Tüm veriler sıfırlandı.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpiredRed)
                ) {
                    Text("SIFIRLA", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // USER EDIT DIALOG
    if (editingUser != null) {
        AlertDialog(
            onDismissRequest = { editingUser = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ManageAccounts, contentDescription = null, tint = TurquoisePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Kullanıcı Düzenle: ${editingUser?.username}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Görevi: ${editingUser?.roleTitle} (${editingUser?.role})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        label = { Text("Ad Soyad") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editPasswordInput,
                        onValueChange = { editPasswordInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Giriş Şifresi (Sadece Rakam)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val userToSave = editingUser?.copy(
                            fullName = editNameInput.trim(),
                            password = editPasswordInput.trim()
                        )
                        if (userToSave != null) {
                            UserManager.updateUserAccount(userToSave)
                            Toast.makeText(context, "Kullanıcı bilgileri güncellendi!", Toast.LENGTH_SHORT).show()
                        }
                        editingUser = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
                ) {
                    Text("KAYDET", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingUser = null }) {
                    Text("İptal")
                }
            }
        )
    }

    // REPAIR RESULT DIALOG
    if (repairResultText != null) {
        AlertDialog(
            onDismissRequest = { repairResultText = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BuildCircle, contentDescription = null, tint = TurquoisePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Veritabanı Onarım Raporu",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Text(
                    text = repairResultText ?: "",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { repairResultText = null },
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
                ) {
                    Text("TAMAM", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(TurquoisePrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = TurquoisePrimary)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isMsUser) "AYARLAR & SİSTEM TERCİHLERİ" else "GENEL AYARLAR",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isMsUser) "Uygulama Görünümü, Bildirimler ve Veri Yönetimi" else "Uygulama Görünümü ve Bildirim Tercihleri",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val csvTabIndex = if (isMsUser) 2 else 1

                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = TurquoisePrimary
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("GENEL AYARLAR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                        if (isMsUser) {
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("KULLANICILAR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            )
                        }
                        Tab(
                            selected = selectedTab == csvTabIndex,
                            onClick = { selectedTab = csvTabIndex },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("CSV İŞLEMLERİ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (selectedTab == 0) {
                // TAB 0: GENEL AYARLAR (Tüm kullanıcılarda görünür)

                // 1. TEMA AYARLARI (Karanlık / Açık Mod)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = TurquoisePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🎨 UYGULAMA TEMA MODU",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Ekran görünümünü açık veya geceye uygun karanlık moda geçirin:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ModernThemeToggleSwitch(
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = onToggleDarkMode
                        )
                    }
                }

                // 2. SES VE TİTREŞİM BİLDİRİM AYARLARI
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🔊 BİLDİRİM VE GERİ BİLDİRİM AYARLARI",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = TurquoisePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Barkod Okumada Ses Efekti (Bip)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                    Text("Ürün tarandığında sesli onay verir", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = soundEffectsEnabled,
                                onCheckedChange = { onToggleSoundEffects() }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = null,
                                    tint = TurquoisePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Titreşim Geri Bildirimi", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                    Text("İşlemlerde cihaz titreşimi sağlar", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = vibrationEnabled,
                                onCheckedChange = { onToggleVibration() }
                            )
                        }
                    }
                }

                // 3. PİL VE PERFORMANS OPTİMİZASYONU
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.BatteryChargingFull,
                                    contentDescription = null,
                                    tint = if (isBatterySaverMode) NormalGreen else TurquoisePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "🔋 PİL VE PERFORMANS OPTİMİZASYONU",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Switch(
                                checked = isBatterySaverMode,
                                onCheckedChange = { onToggleBatterySaverMode() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = NormalGreen
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isBatterySaverMode) NormalGreenContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (isBatterySaverMode) "✅ EKO MOD AKTİF (Maksimum Pil Tasarrufu)" else "⚡ Standart Performans Modu",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isBatterySaverMode) NormalGreen else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• Kamera ML Kit barkod & OCR analiz frekansı akıllı sınırlandı (CPU yükü %70 azaltıldı).\n• Arka plan periyodik kontrolleri optimize edildi.\n• OLED ekranlarda karanlık mod ile %40-60 ekran enerjisi tasarrufu sağlanır.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // 4. HATA ARAMA VE VERİ DÜZELTME (Sadece MS - Mağaza Sorumlusu için özel)
                if (currentUser?.role == "MS") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(TurquoisePrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoFixHigh,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "🔍 HATA ARAMA VE VERİ DÜZELTME",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Sadece Mağaza Sorumlusu (MS) Yetkisinde",
                                        fontSize = 11.sp,
                                        color = TurquoiseDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Veritabanındaki bozuk ürün metinlerini, eksik/hatalı barkod formatlarını, negatif stok sayılarını ve mükerrer kayıtları otomatik tarayıp düzeltir.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    isRepairing = true
                                    onFixAndRepairDatabase { count, message ->
                                        isRepairing = false
                                        repairResultText = message
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
                            ) {
                                if (isRepairing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("TARANIYOR...", fontWeight = FontWeight.Bold, color = Color.White)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "HATA ARA VE OTOMATİK DÜZELT",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. QR İLE ÜRÜN KODU / BARKOD DÜZELTME
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(TurquoisePrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "🏷️ QR ETİKET İLE BARKOD DÜZELTME",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Ürün Kodu Eşleşen Ürünlerin Barkodunu Otomatik Düzeltir",
                                        fontSize = 11.sp,
                                        color = TurquoiseDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Veritabanındaki bazı ürünlerde barkod yerine ürün kodu eklenmişse, QR raf etiketini kamerayla taratarak ürün kodu eşleşen ürünlerin gerçek ambalaj barkodlarını ve fiyatlarını anında düzeltebilirsiniz.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = onOpenQrFixMode,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "📷 QR ETİKET OKUT VE BARKODU DÜZELT",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            } else if (isMsUser && selectedTab == 1) {
                // TAB 0: KULLANICI BİLGİLERİ DÜZENLEME SAYFASI (MS ÖZEL)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "👥 Mağaza Ekip Üyeleri & Şifreler",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Ekip üyelerinin ad, unvan ve giriş şifrelerini buradan yönetebilirsiniz.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            usersList.forEach { user ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, Slate200.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when (user.role) {
                                                            "MS" -> ExpiredRed
                                                            "MSY" -> CriticalOrange
                                                            else -> TurquoisePrimary
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = user.role,
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "${user.fullName} (${user.username})",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Text(
                                                    text = "${user.roleTitle} • Şifre: ${user.password}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                editingUser = user
                                                editNameInput = user.fullName
                                                editPasswordInput = user.password
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Düzenle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = {
                                UserManager.resetToDefaults()
                                Toast.makeText(context, "Kullanıcı şifreleri varsayılana sıfırlandı (3232).", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Şifreleri Varsayılana Sıfırla (3232)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            } else if ((isMsUser && selectedTab == 2) || (!isMsUser && selectedTab == 1)) {
                // TAB CSV: CSV İŞLEMLERİ (2 KATEGORİ: İÇE AKTAR VE DIŞA AKTAR)

                // Sub-category Selector (Segmented Tabs)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        // İÇE AKTAR
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedCsvSubTab = 0 },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedCsvSubTab == 0) TurquoisePrimary else Color.Transparent,
                            shadowElevation = if (selectedCsvSubTab == 0) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.UploadFile,
                                    contentDescription = null,
                                    tint = if (selectedCsvSubTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "İçe Aktar",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (selectedCsvSubTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // DIŞA AKTAR
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedCsvSubTab = 1 },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedCsvSubTab == 1) TurquoisePrimary else Color.Transparent,
                            shadowElevation = if (selectedCsvSubTab == 1) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    tint = if (selectedCsvSubTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Dışa Aktar",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (selectedCsvSubTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedCsvSubTab == 0) {
                    // -----------------------------------------------------------------
                    // İÇE AKTARMA & SİSTEM SIFIRLAMA (CSV IMPORT)
                    // -----------------------------------------------------------------
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(TurquoisePrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.UploadFile,
                                        contentDescription = "İçe Aktar",
                                        tint = TurquoisePrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "📥 İÇE AKTAR (CSV IMPORT)",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Excel/CSV toplu ürün listesini veritabanına yükleme",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        try {
                                            filePickerLauncher.launch(arrayOf("text/csv", "text/plain", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel", "*/*"))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Dosya seçici açılamadı", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .testTag("select_csv_file_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.UploadFile,
                                        contentDescription = "Dosya Yükle",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "EXCEL / CSV YÜKLE",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }

                                Button(
                                    onClick = { showResetDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .testTag("reset_db_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ExpiredRed)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteForever,
                                        contentDescription = "Sıfırla",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "VERİLERİ SIFIRLA",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // CSV FORMATI INFO CARD
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Bilgi",
                                    tint = TurquoisePrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "CSV / Excel Yükleme Format Rehberi",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Excel tablonuzu CSV (Virgülle Ayrılmış) olarak aktarırken sütun sırası aşağıdaki gibi olmalıdır:",
                                fontSize = 12.sp,
                                color = Slate700
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Slate900)
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = "Barkod, ürün kodu, ürün adı\n" +
                                            "8690526010011, 25001234, BİSKÜVİ ÇİKOLATA KAPLI 56 G BENİMO\n" +
                                            "8690504031122, 25001402, SÜTAŞ SÜZME PEYNİR 500 G TAM YAĞLI\n" +
                                            "8690620010019, 25001560, PINAR DİLİMLİ TOST PEYNİRİ 200 G",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                } else {
                    // -----------------------------------------------------------------
                    // DIŞA AKTARMA (CSV EXPORT - TÜM ÜRÜNLER)
                    // -----------------------------------------------------------------
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(TurquoisePrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = "Dışa Aktar",
                                        tint = TurquoisePrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "📤 DIŞA AKTAR (CSV EXPORT)",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Tüm ürün listenizi Excel/CSV formatında alın",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // SUMMARY BADGE BOX
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, Slate200.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "DIŞA AKTARILACAK VERİ",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Tüm Ürünler (${products.size} Adet)",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = TurquoiseDark
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = TurquoisePrimary.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "Barkod, Kod, Ad",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TurquoiseDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // MAIN DOWNLOAD BUTTON
                            Button(
                                onClick = {
                                    val fileName = "urun_listesi_tum_urunler.csv"
                                    try {
                                        exportDocumentLauncher.launch(fileName)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Hata: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("export_csv_download_button"),
                                shape = RoundedCornerShape(14.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "İndir",
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "CSV DOSYASINI İNDİR",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // SECONDARY ACTIONS ROW (PAYLAŞ & KOPYALA)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, csvExportText)
                                                putExtra(Intent.EXTRA_SUBJECT, "Urun_Listesi_Tum_Urunler.csv")
                                                type = "text/plain"
                                            }
                                            val shareIntent = Intent.createChooser(sendIntent, "CSV Listesini Paylaş")
                                            context.startActivity(shareIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Paylaşılamadı: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Paylaş",
                                        modifier = Modifier.size(18.dp),
                                        tint = TurquoisePrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Paylaş", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TurquoisePrimary)
                                }

                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Urun_CSV_Listesi", csvExportText)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "📋 ${products.size} adet ürün panoya kopyalandı!", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Kopyalama hatası: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Kopyala",
                                        modifier = Modifier.size(18.dp),
                                        tint = TurquoisePrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Panoya Kopyala", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TurquoisePrimary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // PREVIEW CARD
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { isPreviewExpanded = !isPreviewExpanded }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isPreviewExpanded) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = TurquoisePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isPreviewExpanded) "Önizlemeyi Gizle" else "Veri Önizleme (${products.size} Ürün)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    imageVector = if (isPreviewExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = TurquoisePrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            if (isPreviewExpanded) {
                                Spacer(modifier = Modifier.height(12.dp))
                                val previewLines = csvExportText.lines().take(15)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Slate900)
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = previewLines.joinToString("\n") + if (csvExportText.lines().size > 15) "\n..." else "",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // En alt sağ: Uygulama Sürümü
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TurquoisePrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Sürüm ${com.example.BuildConfig.VERSION_NAME}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ModernThemeToggleSwitch(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, Slate200.copy(alpha = 0.3f))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            val thumbWidth = maxWidth / 2
            val animatedOffset by animateDpAsState(
                targetValue = if (isDarkMode) thumbWidth else 0.dp,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                label = "theme_switch_offset"
            )

            // Sliding Active Thumb Indicator
            Box(
                modifier = Modifier
                    .offset(x = animatedOffset)
                    .width(thumbWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        if (isDarkMode) TurquoiseDark else TurquoisePrimary
                    )
            )

            // Labels Row
            Row(modifier = Modifier.fillMaxSize()) {
                // Left: Açık Mod
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { if (isDarkMode) onToggleDarkMode() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = "Açık Mod",
                        tint = if (!isDarkMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AÇIK MOD",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = if (!isDarkMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Right: Karanlık Mod
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { if (!isDarkMode) onToggleDarkMode() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NightsStay,
                        contentDescription = "Karanlık Mod",
                        tint = if (isDarkMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "KARANLIK MOD",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = if (isDarkMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
