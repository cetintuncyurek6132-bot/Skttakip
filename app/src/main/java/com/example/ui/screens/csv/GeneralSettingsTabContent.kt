package com.example.ui.screens.csv

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.UserAccount
import com.example.ui.theme.*
import com.example.util.AppUpdateChecker
import com.example.util.AppUpdateInfo
import com.example.util.FontDownloader
import com.example.util.SoundToneManager
import com.example.util.SoundToneOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun GeneralSettingsTabContent(
    currentUser: UserAccount? = null,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    isBatterySaverMode: Boolean,
    onToggleBatterySaverMode: () -> Unit,
    soundEffectsEnabled: Boolean,
    onToggleSoundEffects: () -> Unit,
    vibrationEnabled: Boolean,
    onToggleVibration: () -> Unit,
    onEditProfileClick: () -> Unit = {},
    onOpenQrFixMode: () -> Unit = {},
    onFixAndRepairDatabase: (onResult: (Int, String) -> Unit) -> Unit = {},
    onRepairResult: (String) -> Unit = {},
    barcodeSoundId: Int = 1,
    onUpdateBarcodeSound: (Int) -> Unit = {},
    labelFixSoundId: Int = 11,
    onUpdateLabelFixSound: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentVersion = remember { com.example.BuildConfig.VERSION_NAME }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var hasUpdateAvailable by remember { mutableStateOf(false) }
    var latestVersionName by remember { mutableStateOf("") }
    var updateInfoDialog by remember { mutableStateOf<AppUpdateInfo?>(null) }
    val downloadProgressInfo by AppUpdateChecker.downloadProgressState.collectAsState()

    var showToneSelectionDialog by remember { mutableStateOf(false) }
    var currentBarcodeToneId by remember(barcodeSoundId) { mutableIntStateOf(barcodeSoundId) }
    var currentQrFixToneId by remember(labelFixSoundId) { mutableIntStateOf(labelFixSoundId) }

    var showFontPickerDialog by remember { mutableStateOf(false) }
    var selectedFontName by remember { mutableStateOf(FontDownloader.getSelectedFont(context)) }
    var isDownloadingFont by remember { mutableStateOf(false) }
    var downloadingFontTarget by remember { mutableStateOf("") }

    // Mevcut indirme durumunu arka planda hafifçe kontrol et (ağ çağrısı yapmaz, yerel DownloadManager kontrolü)
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            AppUpdateChecker.checkCurrentDownloadStatus(context)
        }
    }

    LaunchedEffect(downloadProgressInfo.isCompleted) {
        if (downloadProgressInfo.isCompleted) {
            updateInfoDialog = null
        }
    }

    // 5 Minimalist Tek Satırlık Modern Kartlar Listesi
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Üst Başlık & Dinamik Sürüm Rozeti
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(TurquoisePrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = TurquoisePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Uygulama Güncellemesi",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Sürüm: v$currentVersion",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Durum Rozeti (Güncel / Yeni Sürüm Var)
                    if (hasUpdateAvailable) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF7ED),
                            border = BorderStroke(1.dp, Color(0xFFFDBA74))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEA580C))
                                )
                                Text(
                                    text = "Yeni Sürüm Var",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC2410C)
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Güncel",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Sunucu üzerinden en son kurumsal güncellemeleri ve hata düzeltmelerini denetleyin.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                // Güncellemeleri Denetle Butonu
                Button(
                    onClick = {
                        isCheckingUpdate = true
                        coroutineScope.launch {
                            val checkRes = AppUpdateChecker.checkForUpdates()
                            isCheckingUpdate = false
                            checkRes.onSuccess { info ->
                                hasUpdateAvailable = info.hasUpdate
                                latestVersionName = info.latestVersionName
                                if (info.hasUpdate) {
                                    updateInfoDialog = info
                                } else {
                                    Toast.makeText(context, "Uygulamanız en güncel sürümde (v$currentVersion)", Toast.LENGTH_SHORT).show()
                                }
                            }.onFailure { err ->
                                Toast.makeText(context, err.localizedMessage ?: "Bağlantı hatası", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasUpdateAvailable) Color(0xFFEA580C) else TurquoisePrimary,
                        contentColor = Color.White
                    ),
                    enabled = !isCheckingUpdate
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Denetleniyor...",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = if (hasUpdateAvailable) Icons.Default.Download else Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasUpdateAvailable) "Güncellemeyi Yükle (v$latestVersionName)" else "Güncellemeleri Denetle",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Karanlık Tema (Dark Mode)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isDarkMode) "Karanlık tema aktif" else "Açık tema aktif",
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pil Tasarrufu Modu",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Kamera ve animasyon güç tüketimini optimize eder",
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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
        // KART 4: SES EFEKTLERİ KARTI (TON SEÇİMLİ VE DOKUNULABİLİR)
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
                // Sol Bölüm (Dokunulduğunda Ses Tonu Seçim Penceresini Açar)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showToneSelectionDialog = true }
                        .padding(vertical = 2.dp),
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ses Efektleri",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val currentToneName = SoundToneManager.getToneOptionById(currentBarcodeToneId).title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Seçili Ton: $currentToneName ❯",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TurquoisePrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = soundEffectsEnabled,
                    onCheckedChange = {
                        onToggleSoundEffects()
                        if (!soundEffectsEnabled) {
                            SoundToneManager.playBarcodeBeep(context)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = TurquoisePrimary
                    )
                )
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Titreşimli Geri Bildirim",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Başarılı taramalarda dokunsal titreşim iletir",
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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
        // KART 6: UYGULAMA FONTU KARTI (BİR KEZ İNDİR, ÇEVRİMDIŞI KULLAN)
        // ========================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_font_settings_card"),
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
                            .background(TurquoisePrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FontDownload,
                            contentDescription = "Uygulama Fontu",
                            tint = TurquoisePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Uygulama Fontu",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Seçili: $selectedFontName",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = TurquoisePrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { showFontPickerDialog = true },
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("change_app_font_button"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TurquoisePrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Değiştir",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // ========================================================
    // UYGULAMA FONTU SEÇİM DİYALOĞU (FontPickerDialog)
    // ========================================================
    if (showFontPickerDialog) {
        FontPickerDialog(
            currentFont = selectedFontName,
            isDownloading = isDownloadingFont,
            downloadingFontName = downloadingFontTarget,
            onSelectFont = { chosenFont ->
                if (isDownloadingFont) return@FontPickerDialog

                val alreadyCached = FontDownloader.isFontDownloaded(context, chosenFont)
                if (chosenFont == FontDownloader.FONT_DEFAULT || alreadyCached) {
                    FontDownloader.saveSelectedFont(context, chosenFont)
                    selectedFontName = chosenFont
                    showFontPickerDialog = false
                    Toast.makeText(
                        context,
                        "Font değiştirildi ($chosenFont). Uygulama yenileniyor...",
                        Toast.LENGTH_SHORT
                    ).show()
                    (context as? Activity)?.recreate()
                } else {
                    isDownloadingFont = true
                    downloadingFontTarget = chosenFont
                    coroutineScope.launch {
                        val success = FontDownloader.downloadAndSaveFont(context, chosenFont)
                        isDownloadingFont = false
                        downloadingFontTarget = ""
                        if (success) {
                            FontDownloader.saveSelectedFont(context, chosenFont)
                            selectedFontName = chosenFont
                            showFontPickerDialog = false
                            Toast.makeText(
                                context,
                                "Font indirildi ve uygulandı ($chosenFont).",
                                Toast.LENGTH_LONG
                            ).show()
                            (context as? Activity)?.recreate()
                        } else {
                            FontDownloader.saveSelectedFont(context, FontDownloader.FONT_DEFAULT)
                            selectedFontName = FontDownloader.FONT_DEFAULT
                            showFontPickerDialog = false
                            Toast.makeText(
                                context,
                                "Font indirilemedi, varsayılan sistem fontuna dönüldü.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            },
            onDismiss = {
                if (!isDownloadingFont) {
                    showFontPickerDialog = false
                }
            }
        )
    }

    // ========================================================
    // SES TONU SEÇİM DİYALOĞU
    // ========================================================
    if (showToneSelectionDialog) {
        ToneSelectionDialog(
            currentBarcodeToneId = currentBarcodeToneId,
            currentQrFixToneId = currentQrFixToneId,
            onSelectBarcodeTone = { id ->
                currentBarcodeToneId = id
                SoundToneManager.setBarcodeToneId(context, id)
                onUpdateBarcodeSound(id)
                SoundToneManager.playTone(id)
            },
            onSelectQrFixTone = { id ->
                currentQrFixToneId = id
                SoundToneManager.setQrFixToneId(context, id)
                onUpdateLabelFixSound(id)
                SoundToneManager.playTone(id)
            },
            onDismiss = { showToneSelectionDialog = false }
        )
    }

    // ========================================================
    // GÜNCELLEME İNDİRME DİYALOĞU
    // ========================================================
    if (updateInfoDialog != null) {
        com.example.ui.components.AppUpdateDialog(
            updateInfo = updateInfoDialog!!,
            isDownloading = downloadProgressInfo.isDownloading,
            downloadProgress = downloadProgressInfo.progress,
            downloadedBytes = downloadProgressInfo.downloadedBytes,
            totalBytes = downloadProgressInfo.totalBytes,
            onConfirmUpdate = {
                val downloadUrl = updateInfoDialog?.downloadUrl.orEmpty()
                if (downloadUrl.isNotBlank()) {
                    AppUpdateChecker.startDownloadWithManager(
                        context = context,
                        downloadUrl = downloadUrl,
                        versionName = updateInfoDialog?.latestVersionName.orEmpty()
                    )
                }
            },
            onDismiss = {
                updateInfoDialog = null
            }
        )
    }
}

@Composable
fun FontPickerDialog(
    currentFont: String,
    isDownloading: Boolean,
    downloadingFontName: String,
    onSelectFont: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        containerColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(TurquoisePrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FontDownload,
                        contentDescription = null,
                        tint = TurquoisePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Uygulama Fontu Seçin",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                    Text(
                        text = "Bir kez indirilir, çevrimdışı kullanılır",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FontDownloader.AVAILABLE_FONTS.forEach { fontOption ->
                    val isSelected = currentFont == fontOption
                    val isCached = remember(fontOption, isDownloading) {
                        FontDownloader.isFontDownloaded(context, fontOption)
                    }
                    val isThisDownloading = isDownloading && downloadingFontName == fontOption

                    Surface(
                        onClick = { onSelectFont(fontOption) },
                        enabled = !isDownloading,
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) TurquoisePrimary.copy(alpha = 0.12f) else Slate50,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) TurquoisePrimary else Slate200
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("font_option_$fontOption")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = fontOption,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                    color = if (isSelected) TurquoiseDark else Slate900
                                )
                                val statusText = when {
                                    isThisDownloading -> "Font İndiriliyor..."
                                    fontOption == FontDownloader.FONT_DEFAULT -> "Cihazın yerleşik sistem fontu"
                                    isCached -> "Çevrimdışı hazır (Telefonda yüklü)"
                                    else -> "İlk seçimde indirilir (Regular, Medium, Bold)"
                                }
                                Text(
                                    text = statusText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isThisDownloading) TurquoiseDark else Slate500
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            if (isThisDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = TurquoisePrimary,
                                    strokeWidth = 2.2.dp
                                )
                            } else {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSelectFont(fontOption) },
                                    enabled = !isDownloading,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = TurquoisePrimary,
                                        unselectedColor = Slate400
                                    ),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                if (isDownloading) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = TurquoisePrimary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = TurquoisePrimary,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Font İndiriliyor... ($downloadingFontName)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TurquoiseDark
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDownloading
            ) {
                Text(
                    text = "Kapat",
                    fontWeight = FontWeight.Bold,
                    color = Slate700
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToneSelectionDialog(
    currentBarcodeToneId: Int,
    currentQrFixToneId: Int,
    onSelectBarcodeTone: (Int) -> Unit,
    onSelectQrFixTone: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    androidx.compose.runtime.DisposableEffect(Unit) {
        com.example.ui.components.AppModalBlurState.onModalOpened()
        onDispose {
            com.example.ui.components.AppModalBlurState.onModalClosed()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.35f),
        dragHandle = { com.example.ui.components.PremiumSheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Başlık Satırı
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
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
                    Column {
                        Text(
                            text = "Ses Efektleri ve Bildirim Tonları",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Okuma ve doğrulama seslerini özelleştirin",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = Slate500,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = Slate200, modifier = Modifier.padding(horizontal = 20.dp))

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Kategori: Barkod Tarama Sesi
                Text(
                    text = "1. Barkod Tarama Sesi",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TurquoiseDark
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SoundToneManager.BARCODE_TONES.forEach { option ->
                        ToneOptionItem(
                            option = option,
                            isSelected = option.id == currentBarcodeToneId,
                            onClick = { onSelectBarcodeTone(option.id) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 2. Kategori: Raf Etiketi / QR Doğrulama Sesi
                Text(
                    text = "2. Raf Etiketi / QR Doğrulama Sesi",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TurquoiseDark
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SoundToneManager.QR_FIX_TONES.forEach { option ->
                        ToneOptionItem(
                            option = option,
                            isSelected = option.id == currentQrFixToneId,
                            onClick = { onSelectQrFixTone(option.id) }
                        )
                    }
                }
            }

            // Alt Aksiyon Alanı
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
                ) {
                    Text(
                        text = "Tamam",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ToneOptionItem(
    option: SoundToneOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) TurquoisePrimary.copy(alpha = 0.12f) else Color(0xFFF8FAFC),
        border = BorderStroke(
            1.dp,
            if (isSelected) TurquoisePrimary else Color(0xFFE2E8F0)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.title,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) TurquoiseDark else Slate800
                )
                Text(
                    text = option.description,
                    fontSize = 11.sp,
                    color = Slate500
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) TurquoisePrimary else Slate200,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Dinle",
                            tint = if (isSelected) Color.White else Slate700,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = TurquoisePrimary,
                        unselectedColor = Slate400
                    ),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
