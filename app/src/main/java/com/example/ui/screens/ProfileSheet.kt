package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.DashboardState
import com.example.ui.theme.*
import com.example.worker.MorningCheckWorker

import com.example.sync.CloudSyncManager
import com.example.sync.SyncState
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileSheet(
    userName: String,
    userBranch: String,
    userRole: String,
    userDepartment: String,
    userDutyStatus: String,
    soundEffectsEnabled: Boolean,
    vibrationEnabled: Boolean,
    isBatterySaverMode: Boolean = false,
    dashboardState: DashboardState,
    onDismiss: () -> Unit,
    onUpdateProfile: (name: String, branch: String, role: String, department: String) -> Unit,
    onUpdateDutyStatus: (status: String) -> Unit,
    onToggleSoundEffects: () -> Unit,
    onToggleVibration: () -> Unit,
    onToggleBatterySaverMode: () -> Unit = {},
    onNavigateToCsv: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by com.example.auth.UserManager.currentUser.collectAsState()
    var isEditing by remember { mutableStateOf(false) }

    var tempName by remember(userName) { mutableStateOf(userName) }
    var tempBranch by remember(userBranch) { mutableStateOf(userBranch) }
    var tempRole by remember(userRole) { mutableStateOf(userRole) }
    var tempDepartment by remember(userDepartment) { mutableStateOf(userDepartment) }

    val departmentsList = remember {
        listOf("Süt & Şarküteri Reyonu", "Et & Tavuk Reyonu", "Unlu Mamuller", "Temizlik & Hijyen", "Meyve & Sebze", "Tüm Reyonlar")
    }

    val dutyStatusList = remember {
        listOf("Vardiyada (Aktif)", "Molada", "Saha Sayımında", "Vardiya Bitti")
    }

    var showResetDataDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // HEADER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(TurquoisePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = "Profil",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Personel Profili",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ExpiredRed.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, ExpiredRed.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        com.example.auth.UserManager.logout()
                                        onDismiss()
                                        Toast.makeText(context, "Oturum kapatıldı.", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Çıkış Yap",
                                        tint = ExpiredRed,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Çıkış Yap",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpiredRed
                                    )
                                }
                            }
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // STAFF CARD & DUTY STATUS
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(1.dp, Slate200.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                if (!isEditing) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = TurquoiseDark,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = userName,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Storefront,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = userBranch,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Category,
                                                    contentDescription = null,
                                                    tint = TurquoisePrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = userDepartment,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TurquoiseDark
                                                )
                                            }
                                        }

                                        IconButton(onClick = { isEditing = true }) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Düzenle",
                                                tint = TurquoisePrimary
                                            )
                                        }
                                    }


                                } else {
                                    // EDIT MODE FORM
                                    Text(
                                        text = "PROFİL BİLGİLERİNİ DÜZENLE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TurquoiseDark
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = tempName,
                                        onValueChange = { tempName = it },
                                        label = { Text("Ad Soyad") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = tempBranch,
                                        onValueChange = { tempBranch = it },
                                        label = { Text("Şube Bilgisi / Kodu") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = tempRole,
                                        onValueChange = { tempRole = it },
                                        label = { Text("Unvan / Görev") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = "Varsayılan Sorumlu Reyon:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate700
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        departmentsList.forEach { dept ->
                                            val isSel = tempDepartment == dept
                                            FilterChip(
                                                selected = isSel,
                                                onClick = { tempDepartment = dept },
                                                label = { Text(dept, fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = TurquoisePrimary,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { isEditing = false }) {
                                            Text("İptal", color = Slate500)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                onUpdateProfile(tempName, tempBranch, tempRole, tempDepartment)
                                                isEditing = false
                                                Toast.makeText(context, "Profil güncellendi", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
                                        ) {
                                            Text("KAYDET", fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // GAMIFICATION & PERFORMANCE STATS
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE2FAFB)),
                            border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🏆 REYON SKT BAŞARI SKORU",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TurquoiseDark
                                    )
                                    Text(
                                        text = "🔥 5 Gün Seri",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CriticalOrange
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { 0.98f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = NormalGreen,
                                    trackColor = Color.White
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(horizontalAlignment = Alignment.Start) {
                                        Text("📦 Yönetilen Stok", fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.Bold)
                                        Text("${dashboardState.totalCount} Kalem", fontSize = 13.sp, color = Slate900, fontWeight = FontWeight.Black)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🚨 Geçen SKT", fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.Bold)
                                        Text("${dashboardState.expiredCount} Adet", fontSize = 13.sp, color = ExpiredRed, fontWeight = FontWeight.Black)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("⚠️ Kritik SKT", fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.Bold)
                                        Text("${dashboardState.criticalCount} Adet", fontSize = 13.sp, color = CriticalOrange, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }

                    // BATTERY & ENERGY OPTIMIZATION CARD
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(1.dp, Slate200.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
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
                                            text = "🔋 PİL VE ENERJİ OPTİMİZASYONU",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
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

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = if (isBatterySaverMode)
                                        "⚡ Eko Pil Tasarrufu AKTİF: Kamera ML Kit işlemci yükü %70 düşürüldü, OLED karanlık mod optimizasyonu devrede."
                                    else
                                        "Kamera analiz kare hızını optimize ederek ve ekran güç tüketimini azaltarak batarya ömrünü uzatın.",
                                    fontSize = 11.sp,
                                    color = if (isBatterySaverMode) NormalGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // QUICK SYSTEM ACTIONS (CSV Export / Settings & Update Check)
                    item {
                        var isProfileCheckingUpdate by remember { mutableStateOf(false) }
                        val coroutineScope = rememberCoroutineScope()
                        var profileUpdateInfoDialog by remember { mutableStateOf<com.example.util.AppUpdateInfo?>(null) }
                        var isProfileDownloading by remember { mutableStateOf(false) }
                        var profileDownloadPercent by remember { mutableIntStateOf(0) }

                        if (profileUpdateInfoDialog != null) {
                            com.example.ui.components.AppUpdateDialog(
                                updateInfo = profileUpdateInfoDialog!!,
                                isDownloading = isProfileDownloading,
                                downloadProgress = profileDownloadPercent,
                                onConfirmUpdate = {
                                    val downloadUrl = profileUpdateInfoDialog?.downloadUrl.orEmpty()
                                    if (downloadUrl.isNotBlank()) {
                                        isProfileDownloading = true
                                        profileDownloadPercent = 0
                                        coroutineScope.launch {
                                            val downloadResult = com.example.util.AppUpdateChecker.downloadApk(
                                                context = context,
                                                downloadUrl = downloadUrl,
                                                onProgress = { progress ->
                                                    profileDownloadPercent = progress
                                                }
                                            )
                                            isProfileDownloading = false
                                            downloadResult.onSuccess { apkFile ->
                                                profileUpdateInfoDialog = null
                                                com.example.util.AppUpdateChecker.installApk(context, apkFile)
                                            }.onFailure { e ->
                                                Toast.makeText(context, "Güncelleme indirilemedi: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                onDismiss = {
                                    isProfileDownloading = false
                                    profileUpdateInfoDialog = null
                                }
                            )
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(1.dp, Slate200.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "🛠️ SİSTEM VE GÜNCELLEME",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                if (currentUser?.canAccessSettings == true) {
                                    Button(
                                        onClick = {
                                            onDismiss()
                                            onNavigateToCsv()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val settingsBtnText = if (currentUser?.role == "MS") "AYARLAR & CSV VERİ YÖNETİMİ" else "GENEL AYARLAR"
                                        Text(settingsBtnText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                OutlinedButton(
                                    onClick = {
                                        isProfileCheckingUpdate = true
                                        coroutineScope.launch {
                                            val result = com.example.util.AppUpdateChecker.checkForUpdates()
                                            isProfileCheckingUpdate = false
                                            result.onSuccess { info ->
                                                if (info.hasUpdate) {
                                                    profileUpdateInfoDialog = info
                                                } else {
                                                    Toast.makeText(context, "✅ Uygulamanız güncel (v${com.example.BuildConfig.VERSION_NAME})", Toast.LENGTH_SHORT).show()
                                                }
                                            }.onFailure { err ->
                                                Toast.makeText(context, "Güncelleme kontrolü başarısız: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TurquoiseDark)
                                ) {
                                    if (isProfileCheckingUpdate) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = TurquoisePrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("KONTROL EDİLİYOR...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    } else {
                                        Icon(imageVector = Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("GÜNCELLEMELERİ DENETLE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("TAMAM / BİTİR", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color.White)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sürüm ${com.example.BuildConfig.VERSION_NAME}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
