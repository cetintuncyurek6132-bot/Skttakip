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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.UserAccount
import com.example.ui.theme.*
import kotlinx.coroutines.launch

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
    labelFixSoundId: Int = 2,
    onUpdateLabelFixSound: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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
                            maxLines = 1,
                            softWrap = false,
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
                                maxLines = 1,
                                softWrap = false,
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
                                    maxLines = 1,
                                    softWrap = false,
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
                            Text(
                                text = "✓ Güncel",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                color = Color(0xFF059669),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
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
                        Text("Denetleniyor...", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false, color = Color.White)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Güncellemeleri Denetle", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false, color = Color.White)
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
        // KART 4: SES EFEKTLERİ KARTI (TERTEMİZ TEK SATIR)
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
                        Text(
                            text = "Barkod ve raf etiketi tarama bildirimleri",
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = soundEffectsEnabled,
                    onCheckedChange = {
                        onToggleSoundEffects()
                        if (!soundEffectsEnabled) {
                            com.example.util.SoundToneManager.playBarcodeBeep()
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
}
