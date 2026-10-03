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

    // 5 Minimalist Tek Satırlık Modern Kartlar Listesi
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ========================================================
        // KART 1: GÜNCELLEME KARTI
        // ========================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, if (isDarkMode) Slate700 else Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Üst Başlık & Dinamik Sürüm Rozeti
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                        Text(
                            text = "Sürüm: v$currentVersion",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Dinamik Durum Rozeti
                    if (isCheckingUpdate) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDarkMode) Slate800 else Slate100
                        ) {
                            Text(
                                text = "Denetleniyor...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate500,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else if (hasUpdateAvailable) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFFF7ED),
                            border = BorderStroke(1.dp, Color(0xFFEA580C).copy(alpha = 0.4f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEA580C))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (latestVersionName.isNotBlank()) "Yeni Sürüm Var (v$latestVersionName)" else "Yeni Sürüm Var",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEA580C)
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
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "✓ Güncel",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Sunucu üzerinden en son kurumsal güncellemeyi denetleyin.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

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
                                    Toast.makeText(context, "✅ Uygulamanız en güncel sürümde (v$currentVersion).", Toast.LENGTH_SHORT).show()
                                }
                            }.onFailure { err ->
                                Toast.makeText(context, err.localizedMessage ?: "Güncelleme denetlenemedi.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Denetleniyor...", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Güncellemeleri Denetle", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                    }
                }
            }
        }

        // ========================================================
        // KART 2: KARANLIK TEMA KARTI
        // ========================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, if (isDarkMode) Slate700 else Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDarkMode) Color(0xFF334155) else Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = if (isDarkMode) Color(0xFFFCD34D) else TurquoisePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Karanlık Tema (Dark Mode)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isDarkMode) "Karanlık tema aktif" else "Açık tema aktif",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { onToggleDarkMode() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = TurquoisePrimary
                    )
                )
            }
        }

        // ========================================================
        // KART 3: PİL TASARRUFU MODU KARTI
        // ========================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, if (isDarkMode) Slate700 else Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isBatterySaverMode) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBatterySaverMode) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                            contentDescription = null,
                            tint = if (isBatterySaverMode) Color(0xFF16A34A) else Slate600,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Pil Tasarrufu Modu",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Kamera ve animasyon güç tüketimini optimize eder",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isBatterySaverMode,
                    onCheckedChange = { onToggleBatterySaverMode() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = TurquoisePrimary
                    )
                )
            }
        }

        // ========================================================
        // KART 4: SES EFEKTLERİ & TON SEÇİMİ KARTI
        // ========================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, if (isDarkMode) Slate700 else Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(TurquoisePrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = TurquoisePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Ses Efektleri",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Barkod ve etiket okuma sesli bildirimleri",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = soundEffectsEnabled,
                        onCheckedChange = { onToggleSoundEffects() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = TurquoisePrimary
                        )
                    )
                }

                // Ton Özelleştirme Link Butonu (Switch üzerine ASLA binmez, temiz alt satırda yer alır)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDarkMode) Slate800 else TurquoisePrimary.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSoundCustomizerDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = TurquoiseDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val barcodeTone = com.example.util.SoundToneManager.getOptionById(currentBarcodeSoundId)
                            val labelTone = com.example.util.SoundToneManager.getOptionById(currentLabelFixSoundId)
                            Text(
                                text = "Ses Tonları: ${barcodeTone.title} / ${labelTone.title}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TurquoiseDark
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TurquoisePrimary
                        ) {
                            Text(
                                text = "Değiştir ⚙️",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // ========================================================
        // KART 5: TİTREŞİMLİ GERİ BİLDİRİM KARTI
        // ========================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, if (isDarkMode) Slate700 else Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (vibrationEnabled) Color(0xFFEDE9FE) else Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = if (vibrationEnabled) Color(0xFF7C3AED) else Slate600,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Titreşimli Geri Bildirim",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Başarılı taramalarda dokunsal titreşim iletir",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = vibrationEnabled,
                    onCheckedChange = { onToggleVibration() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = TurquoisePrimary
                    )
                )
            }
        }

        // ========================================================
        // KART 6: QR RAF ETİKETİ İLE DÜZELTME KARTI
        // ========================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, if (isDarkMode) Slate700 else Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(TurquoisePrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = TurquoiseDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "QR Etiket ile Barkod Düzeltme",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Ürün kodu eşleşen ürünlerin ambalaj barkodunu düzeltir",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onOpenQrFixMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "📷 QR Etiket Okut ve Düzelt",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // ========================================================
    // GÜNCELLEME İNDİRME DİYALOĞU
    // ========================================================
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

    // ========================================================
    // 5 TONLUK SES ÖZELLEŞTİRME DİYALOĞU
    // ========================================================
    if (showSoundCustomizerDialog) {
        var selectedTab by remember { mutableIntStateOf(0) } // 0: Barkod Tarama, 1: Etiket Düzeltme

        Dialog(
            onDismissRequest = { showSoundCustomizerDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .wrapContentHeight()
                    .padding(vertical = 16.dp)
                    .clip(RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Başlık
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(TurquoisePrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Tarama Ses Tonları",
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

                    // Sekmeler: [ 🔍 Barkod Tarama Sesi ] | [ 🏷️ Etiket Düzeltme Sesi ]
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
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
