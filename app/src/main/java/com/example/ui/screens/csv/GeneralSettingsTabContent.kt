package com.example.ui.screens.csv

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.auth.UserAccount
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun GeneralSettingsTabContent(
    currentUser: UserAccount?,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    isBatterySaverMode: Boolean,
    onToggleBatterySaverMode: () -> Unit,
    soundEffectsEnabled: Boolean,
    onToggleSoundEffects: () -> Unit,
    vibrationEnabled: Boolean,
    onToggleVibration: () -> Unit,
    onEditProfileClick: () -> Unit,
    onOpenQrFixMode: () -> Unit,
    onFixAndRepairDatabase: (onResult: (Int, String) -> Unit) -> Unit,
    onRepairResult: (String) -> Unit,
    barcodeSoundId: Int = 1,
    onUpdateBarcodeSound: (Int) -> Unit = {},
    labelFixSoundId: Int = 2,
    onUpdateLabelFixSound: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE) }

    var currentBarcodeSoundId by remember(barcodeSoundId) {
        mutableIntStateOf(if (barcodeSoundId in 1..5) barcodeSoundId else prefs.getInt("scanner_sound_barcode", 1))
    }
    var currentLabelFixSoundId by remember(labelFixSoundId) {
        mutableIntStateOf(if (labelFixSoundId in 1..5) labelFixSoundId else prefs.getInt("scanner_sound_label_fix", 2))
    }
    var showSoundCustomizerDialog by remember { mutableStateOf(false) }

    val currentVersion = remember { com.example.BuildConfig.VERSION_NAME }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var hasUpdateAvailable by remember { mutableStateOf(false) }
    var latestVersionName by remember { mutableStateOf("") }
    var updateInfoDialog by remember { mutableStateOf<com.example.util.AppUpdateInfo?>(null) }
    var isManualDownloading by remember { mutableStateOf(false) }
    var manualDownloadPercent by remember { mutableIntStateOf(0) }
    var manualDownloadedBytes by remember { mutableLongStateOf(0L) }
    var manualTotalBytes by remember { mutableLongStateOf(0L) }

    // Otomatik Sessiz Kontrol: Ekran ilk açıldığında arka planda çalışsın
    LaunchedEffect(Unit) {
        isCheckingUpdate = true
        val result = com.example.util.AppUpdateChecker.checkForUpdates()
        isCheckingUpdate = false
        result.onSuccess { info ->
            hasUpdateAvailable = info.hasUpdate
            latestVersionName = info.latestVersionName
        }.onFailure {
            hasUpdateAvailable = false
        }
    }

    var isRepairing by remember { mutableStateOf(false) }
    // 1. KULLANICI PROFİL KARTI (EN ÜSTTE)
    val user = currentUser
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
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(TurquoisePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = user?.fullName ?: "Mağaza Yetkilisi",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = user?.roleTitle ?: "Mağaza Sorumlusu",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = {
                        onEditProfileClick()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Profili Düzenle",
                        tint = TurquoisePrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = TurquoisePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Reyon: ${user?.department ?: "Süt & Şarküteri Reyonu"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TurquoisePrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "AKTİF KULLANICI",
                            color = TurquoisePrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }


    // 3. TEMA AYARLARI (Karanlık / Açık Mod)
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
                text = "Uygulama temasını seçin:",
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showSoundCustomizerDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = TurquoisePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Barkod Okumada Ses Efekti", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = TurquoisePrimary.copy(alpha = 0.12f),
                                modifier = Modifier.clickable { showSoundCustomizerDialog = true }
                            ) {
                                Text(
                                    text = "Tonu Değiştir ⚙️",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TurquoiseDark,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Barkod: ${com.example.util.SoundToneManager.getToneOptionById(currentBarcodeSoundId).title} • Etiket: ${com.example.util.SoundToneManager.getToneOptionById(currentLabelFixSoundId).title}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
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
                            text = "⚡ AKILLI KAYMA VE VERİ ONARIMI",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Kolon Kaymaları, Barkod/İsim ve Kategori Onarımı",
                            fontSize = 11.sp,
                            color = TurquoiseDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Excel ve CSV aktarımlarında oluşan tüm kolon kaymalarını (Barkod, Ürün Kodu, Ürün Adı, Kategori yer değiştirmelerini), hatalı reyon numaralarını ve mükerrer kayıtları otomatik analiz edip tam doğruluğa kavuşturur.",
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
                            onRepairResult(message)
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
                        Text("ONARILIYOR...", fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "KAYMALARI VE HATALARI OTOMATİK ONAR",
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

        Spacer(modifier = Modifier.height(14.dp))

        // 5. UYGULAMA SÜRÜMÜ & GÜNCELLEME KONTROLÜ
        if (updateInfoDialog != null) {
            com.example.ui.components.AppUpdateDialog(
                updateInfo = updateInfoDialog!!,
                isDownloading = isManualDownloading,
                downloadProgress = manualDownloadPercent,
                downloadedBytes = manualDownloadedBytes,
                totalBytes = manualTotalBytes,
                onConfirmUpdate = {
                    val downloadUrl = updateInfoDialog?.downloadUrl.orEmpty()
                    if (downloadUrl.isNotBlank()) {
                        isManualDownloading = true
                        manualDownloadPercent = 0
                        manualDownloadedBytes = 0L
                        manualTotalBytes = 0L
                        coroutineScope.launch {
                            val downloadResult = com.example.util.AppUpdateChecker.downloadApk(
                                context = context,
                                downloadUrl = downloadUrl,
                                onProgress = { progress, downloaded, total ->
                                    manualDownloadPercent = progress
                                    manualDownloadedBytes = downloaded
                                    manualTotalBytes = total
                                }
                            )
                            isManualDownloading = false
                            downloadResult.onSuccess { apkFile ->
                                updateInfoDialog = null
                                com.example.util.AppUpdateChecker.installApk(context, apkFile)
                            }.onFailure { e ->
                                Toast.makeText(context, "Güncelleme indirilemedi: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                onDismiss = {
                    isManualDownloading = false
                    updateInfoDialog = null
                }
            )
        }

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
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(TurquoisePrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = TurquoiseDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "🚀 UYGULAMA GÜNCELLEME",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Sürüm: v$currentVersion",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // Dinamik Durum Rozeti:
                                if (isCheckingUpdate) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFE2E8F0)
                                    ) {
                                        Text(
                                            text = "Denetleniyor...",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate600,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else if (hasUpdateAvailable) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFEF2F2),
                                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(Color(0xFFEF4444), CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Yeni Sürüm Var (v$latestVersionName)",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFFB91C1C)
                                            )
                                        }
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFECFDF5),
                                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF059669),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Güncel",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF059669)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "GitHub Releases üzerinden yeni sürüm kontrolü yapabilir, tek tıkla en son APK sürümünü indirip güncelleyebilirsiniz.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TurquoisePrimary.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "📝 Güncel Sürüm Notları:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TurquoiseDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Bulunan hatalar düzeltildi.\n• Ana sayfa adetsel ve takip sayfaları düzeltildi.\n• Ürün hataları giderildi.\n• Optimizasyonu yapıldı",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        isCheckingUpdate = true
                        coroutineScope.launch {
                            val result = com.example.util.AppUpdateChecker.checkForUpdates()
                            isCheckingUpdate = false
                            result.onSuccess { info ->
                                hasUpdateAvailable = info.hasUpdate
                                latestVersionName = info.latestVersionName
                                if (info.hasUpdate) {
                                    updateInfoDialog = info
                                } else {
                                    Toast.makeText(context, "✅ Uygulamanız güncel (v$currentVersion)", Toast.LENGTH_SHORT).show()
                                }
                            }.onFailure { err ->
                                Toast.makeText(context, "Güncelleme kontrolü başarısız: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("KONTROL EDİLİYOR...", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GÜNCELLEMELERİ KONTROL ET",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 6. TARAMA SESLERİ ÖZELLEŞTİRME DİYALOĞU
        if (showSoundCustomizerDialog) {
            var selectedTab by remember { mutableIntStateOf(0) } // 0: Barkod, 1: Etiket Düzeltme
            Dialog(
                onDismissRequest = { showSoundCustomizerDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .wrapContentHeight()
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 10.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = TurquoisePrimary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = TurquoiseDark,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Tarama Sesleri",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "İşlem geri bildirim tonlarını özelleştirin",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showSoundCustomizerDialog = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Kapat",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Üst Sekmeler: [ 🔍 Barkod Tarama Sesi ] | [ 🏷️ Etiket Düzeltme Sesi ]
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selectedTab == 0) TurquoisePrimary else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedTab = 0 }
                                ) {
                                    Text(
                                        text = "🔍 Barkod Tarama",
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTab == 0) Color.White else MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selectedTab == 1) TurquoisePrimary else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedTab = 1 }
                                ) {
                                    Text(
                                        text = "🏷️ Etiket Düzeltme",
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTab == 1) Color.White else MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 5 Seçilebilir Ses Tonu Listesi
                        val activeToneId = if (selectedTab == 0) currentBarcodeSoundId else currentLabelFixSoundId
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            com.example.util.SoundToneManager.TONES.forEach { option ->
                                val isSelected = option.id == activeToneId
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) TurquoisePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(1.dp, if (isSelected) TurquoisePrimary else Color.Transparent),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (selectedTab == 0) {
                                                currentBarcodeSoundId = option.id
                                                onUpdateBarcodeSound(option.id)
                                                prefs.edit().putInt("scanner_sound_barcode", option.id).apply()
                                            } else {
                                                currentLabelFixSoundId = option.id
                                                onUpdateLabelFixSound(option.id)
                                                prefs.edit().putInt("scanner_sound_label_fix", option.id).apply()
                                            }
                                            com.example.util.SoundToneManager.playTonePreview(context, option.id)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = {
                                                    if (selectedTab == 0) {
                                                        currentBarcodeSoundId = option.id
                                                        onUpdateBarcodeSound(option.id)
                                                        prefs.edit().putInt("scanner_sound_barcode", option.id).apply()
                                                    } else {
                                                        currentLabelFixSoundId = option.id
                                                        onUpdateLabelFixSound(option.id)
                                                        prefs.edit().putInt("scanner_sound_label_fix", option.id).apply()
                                                    }
                                                    com.example.util.SoundToneManager.playTonePreview(context, option.id)
                                                },
                                                colors = RadioButtonDefaults.colors(selectedColor = TurquoiseDark)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = option.title,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${option.durationMs}ms süre",
                                                    fontSize = 10.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                com.example.util.SoundToneManager.playTonePreview(context, option.id)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = "Dinle",
                                                tint = TurquoiseDark,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showSoundCustomizerDialog = false },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
                        ) {
                            Text("Tamam", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
