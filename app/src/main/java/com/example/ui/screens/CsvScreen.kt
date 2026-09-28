package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.data.BackupMetadata
import com.example.data.BackupRestoreResult
import com.example.data.DataBackupManager
import com.example.data.Product
import com.example.ui.components.AppUpdateDialog
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoiseLight
import com.example.ui.theme.TurquoisePrimary
import com.example.util.AppUpdateChecker
import com.example.util.AppUpdateInfo
import com.example.util.BackupExportHelper
import com.example.util.CsvParseResult
import com.example.util.XlsxParser
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    onImportLines: (List<String>) -> CsvParseResult = { CsvParseResult(emptyList(), 0, 0) },
    onResetDatabase: () -> Unit = {},
    onRestoreSeedData: () -> Unit = {},
    onOpenQrFixMode: () -> Unit = {},
    onExportJsonBackup: ((String) -> Unit) -> Unit = {},
    onSaveLocalBackup: (String, (File?) -> Unit) -> Unit = { _, _ -> },
    onGetLocalBackups: () -> List<BackupMetadata> = { emptyList() },
    onRestoreFromJson: (String, Boolean, (BackupRestoreResult) -> Unit) -> Unit = { _, _, _ -> },
    onNavigateToReminders: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 0: Genel Ayarlar, 1: Veri Ayarları
    var selectedTab by remember { mutableIntStateOf(0) }

    // Dialog States
    var showResetDialog by remember { mutableStateOf(false) }
    var showRepairResultDialog by remember { mutableStateOf<String?>(null) }
    var restorePendingJson by remember { mutableStateOf<String?>(null) }
    var showRestoreChoiceDialog by remember { mutableStateOf(false) }

    // Update States
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfoState by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isDownloadingApk by remember { mutableStateOf(false) }
    var downloadProgressPercent by remember { mutableIntStateOf(0) }

    // Backup & Restore processing states
    var isExporting by remember { mutableStateOf(false) }
    var isSavingLocal by remember { mutableStateOf(false) }
    var showExportOptionsDialog by remember { mutableStateOf(false) }
    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    var pendingExportFileName by remember { mutableStateOf("") }

    // Storage Access Framework (SAF) CreateDocument Launcher for JSON Export
    val jsonCreateDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            val json = pendingExportJson
            if (!json.isNullOrBlank()) {
                val res = BackupExportHelper.writeJsonToUri(context, uri, json)
                if (res.isSuccess) {
                    Toast.makeText(context, "✅ Yedek seçilen klasöre başarıyla kaydedildi!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Kayıt hatası: ${res.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // JSON Import Launcher (Geri Yükle)
    val jsonImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            try {
                val inputStream = context.contentResolver.openInputStream(fileUri)
                val jsonContent = inputStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                if (jsonContent.isNullOrBlank()) {
                    Toast.makeText(context, "Seçilen JSON dosyası boş veya geçersiz!", Toast.LENGTH_SHORT).show()
                    return@let
                }
                restorePendingJson = jsonContent
                showRestoreChoiceDialog = true
            } catch (e: Exception) {
                Toast.makeText(context, "Yedek dosyası okunamadı: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // CSV / XLSX File Picker Launcher (5 Kolon ve 3 Kolon Desteği)
    val csvPickerLauncher = rememberLauncherForActivityResult(
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

                val isZipOrXlsx = bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() && bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()
                val isOldXls = bytes.size >= 4 && bytes[0] == 0xD0.toByte() && bytes[1] == 0xCF.toByte() && bytes[2] == 0x11.toByte() && bytes[3] == 0xE0.toByte()

                if (isZipOrXlsx) {
                    val xlsxLines = XlsxParser.parseXlsxToCsvLines(bytes)
                    if (xlsxLines.isNotEmpty()) {
                        val importResult = onImportLines(xlsxLines)
                        val newCount = importResult.productsToInsert.size
                        val skippedCount = importResult.skippedLineCount
                        val message = buildString {
                            if (newCount > 0) {
                                append("Excel (.xlsx) dosyasından $newCount adet ürün başarıyla aktarıldı!")
                            } else {
                                append("Excel dosyasında eklenecek yeni ürün bulunamadı.")
                            }
                            if (skippedCount > 0) {
                                append("\n$skippedCount adet satır format uyuşmazlığı nedeniyle atlandı.")
                            }
                        }
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        return@let
                    } else {
                        Toast.makeText(context, "Excel dosyası okunamadı veya boş.", Toast.LENGTH_LONG).show()
                        return@let
                    }
                }

                if (isOldXls) {
                    Toast.makeText(context, "Eski (.xls) formatı desteklenmiyor. Lütfen .xlsx veya .csv formatında yükleyin.", Toast.LENGTH_LONG).show()
                    return@let
                }

                // Text encoding handling (UTF-8, Windows-1254, BOM)
                val charsetTurkish = try {
                    java.nio.charset.Charset.forName("windows-1254")
                } catch (e: Exception) {
                    java.nio.charset.StandardCharsets.ISO_8859_1
                }

                val textContent = if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
                    String(bytes, 3, bytes.size - 3, java.nio.charset.StandardCharsets.UTF_8)
                } else {
                    val utf8Str = String(bytes, java.nio.charset.StandardCharsets.UTF_8)
                    if (utf8Str.contains("\uFFFD")) {
                        String(bytes, charsetTurkish)
                    } else {
                        utf8Str
                    }
                }

                val lines = textContent.lines()
                val importResult = onImportLines(lines)
                val newCount = importResult.productsToInsert.size
                val skippedCount = importResult.skippedLineCount
                val message = buildString {
                    if (newCount > 0) {
                        append("$newCount adet ürün başarıyla aktarıldı!")
                    } else {
                        append("Seçilen dosyada eklenecek yeni ürün bulunamadı.")
                    }
                    if (skippedCount > 0) {
                        append("\n$skippedCount adet satır hatalı format nedeniyle atlandı.")
                    }
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Dosya okunamadı: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // JSON Dışa Aktarma Başlatıcı (Diyalog Açılır)
    fun triggerJsonExport() {
        isExporting = true
        onExportJsonBackup { jsonString ->
            isExporting = false
            if (jsonString.isBlank()) {
                Toast.makeText(context, "Dışa aktarılacak veri bulunamadı!", Toast.LENGTH_SHORT).show()
                return@onExportJsonBackup
            }
            val fileName = BackupExportHelper.generateBackupFileName()
            pendingExportJson = jsonString
            pendingExportFileName = fileName
            showExportOptionsDialog = true
        }
    }

    // Güncellemeleri Denetle Aksiyonu
    fun checkAppUpdates() {
        isCheckingUpdate = true
        coroutineScope.launch {
            val result = AppUpdateChecker.checkForUpdates()
            isCheckingUpdate = false
            result.onSuccess { info ->
                if (info.hasUpdate) {
                    updateInfoState = info
                    showUpdateDialog = true
                } else {
                    Toast.makeText(context, "Uygulamanız güncel! (${BuildConfig.VERSION_NAME})", Toast.LENGTH_SHORT).show()
                }
            }.onFailure { e ->
                Toast.makeText(context, "Güncelleme kontrolü yapılamadı: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Update Dialog
    if (showUpdateDialog && updateInfoState != null) {
        AppUpdateDialog(
            updateInfo = updateInfoState!!,
            isDownloading = isDownloadingApk,
            downloadProgress = downloadProgressPercent,
            onConfirmUpdate = {
                val downloadUrl = updateInfoState?.downloadUrl.orEmpty()
                if (downloadUrl.isNotBlank()) {
                    isDownloadingApk = true
                    downloadProgressPercent = 0
                    coroutineScope.launch {
                        val downloadResult = AppUpdateChecker.downloadApk(
                            context = context,
                            downloadUrl = downloadUrl,
                            onProgress = { progress -> downloadProgressPercent = progress }
                        )
                        isDownloadingApk = false
                        downloadResult.onSuccess { apkFile ->
                            showUpdateDialog = false
                            AppUpdateChecker.installApk(context, apkFile)
                        }.onFailure { e ->
                            Toast.makeText(context, "Güncelleme indirilemedi: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            onDismiss = {
                isDownloadingApk = false
                showUpdateDialog = false
            }
        )
    }

    // Reset Confirmation Dialog (Tehlikeli Bölge)
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = ExpiredRed,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Tüm Verileri Sıfırla",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Tüm verileri silmek istediğinize emin misiniz? Kayıtlı ürünler, SKT'ler, takip ve sayım verileri kalıcı olarak silinecektir. Bu işlem geri alınamaz.",
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        showResetDialog = false
                        onResetDatabase()
                        Toast.makeText(context, "Tüm ürün ve operasyon verileri sıfırlandı.", Toast.LENGTH_SHORT).show()
                    },
                    border = BorderStroke(1.dp, ExpiredRed),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpiredRed)
                ) {
                    Text("Evet, Tümünü Sil", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false }
                ) {
                    Text("Vazgeç", color = Slate700, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // Restore Choice Dialog (Mevcutla Birleştir vs Üzerine Yaz)
    if (showRestoreChoiceDialog && restorePendingJson != null) {
        AlertDialog(
            onDismissRequest = {
                showRestoreChoiceDialog = false
                restorePendingJson = null
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = TurquoiseDark,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Yedekten Geri Yükleme Yöntemi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Yedek dosyasındaki verileri mevcut veritabanınızla birleştirmek mi yoksa mevcut verilerin üzerine yazmak mı istersiniz?",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val json = restorePendingJson ?: return@Button
                        showRestoreChoiceDialog = false
                        restorePendingJson = null
                        onRestoreFromJson(json, true) { result ->
                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Mevcutla Birleştir", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        val json = restorePendingJson ?: return@OutlinedButton
                        showRestoreChoiceDialog = false
                        restorePendingJson = null
                        onRestoreFromJson(json, false) { result ->
                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        }
                    },
                    border = BorderStroke(1.dp, TurquoisePrimary),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TurquoiseDark)
                ) {
                    Text("Üzerine Yaz", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Repair Result Dialog
    showRepairResultDialog?.let { resultMsg ->
        AlertDialog(
            onDismissRequest = { showRepairResultDialog = null },
            shape = RoundedCornerShape(16.dp),
            title = {
                Text("Veritabanı Onarım Sonucu", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Text(resultMsg, fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            confirmButton = {
                Button(
                    onClick = { showRepairResultDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Tamam", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // JSON Yedek Dışa Aktarma ve Cihaz Belleğine Kaydetme Diyaloğu
    if (showExportOptionsDialog && pendingExportJson != null) {
        AlertDialog(
            onDismissRequest = { showExportOptionsDialog = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(TurquoisePrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SaveAlt,
                        contentDescription = null,
                        tint = TurquoiseDark,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "JSON Yedeğini Dışa Aktar",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Yedek dosyasını telefon belleğinize kaydedebilir veya diğer uygulamalara aktarabilirsiniz:",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Dosya Adı Bilgi Rozeti
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate100,
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = Slate600,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = pendingExportFileName,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate800,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // SEÇENEK 1: İndirilenler Klasörüne Doğrudan Kaydet (Cihaz Hafızası)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val json = pendingExportJson
                                val fileName = pendingExportFileName
                                if (!json.isNullOrBlank()) {
                                    val res = BackupExportHelper.saveJsonToDownloads(context, json, fileName)
                                    if (res.isSuccess) {
                                        Toast.makeText(
                                            context,
                                            "✅ Yedek cihazınızın İndirilenler (Downloads) klasörüne kaydedildi:\n$fileName",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        showExportOptionsDialog = false
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "İndirilenler'e kaydetme hatası: ${res.exceptionOrNull()?.localizedMessage}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = TurquoisePrimary.copy(alpha = 0.08f)),
                        border = BorderStroke(1.5.dp, TurquoisePrimary)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(TurquoisePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "İndirilenler'e Kaydet (En Hızlı)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TurquoiseDark
                                )
                                Text(
                                    text = "Telefonun İndirilenler (Downloads) klasörüne doğrudan kaydeder.",
                                    fontSize = 11.sp,
                                    color = Slate600
                                )
                            }
                        }
                    }

                    // SEÇENEK 2: Klasör Seçerek Kaydet (Farklı Kaydet)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val fileName = pendingExportFileName
                                showExportOptionsDialog = false
                                try {
                                    jsonCreateDocLauncher.launch(fileName)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Dosya seçici açılamadı: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Slate300)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(IndigoAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreateNewFolder,
                                    contentDescription = null,
                                    tint = IndigoAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Konum / Klasör Seçerek Kaydet",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Slate900
                                )
                                Text(
                                    text = "Cihaz belleğinde istediğiniz klasörü seçin (Belgeler, SD Kart vb.).",
                                    fontSize = 11.sp,
                                    color = Slate600
                                )
                            }
                        }
                    }

                    // SEÇENEK 3: Diğer Uygulamalar ile Paylaş
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val json = pendingExportJson
                                val fileName = pendingExportFileName
                                if (!json.isNullOrBlank()) {
                                    showExportOptionsDialog = false
                                    val res = BackupExportHelper.shareJsonBackup(context, json, fileName)
                                    if (res.isFailure) {
                                        Toast.makeText(context, "Paylaşım hatası: ${res.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Slate300)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Slate200),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = Slate700,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Uygulamalar ile Paylaş",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Slate900
                                )
                                Text(
                                    text = "WhatsApp, Google Drive, E-posta vb. ile gönderin.",
                                    fontSize = 11.sp,
                                    color = Slate600
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showExportOptionsDialog = false }
                ) {
                    Text("Kapat", fontWeight = FontWeight.Bold, color = Slate700)
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp)
                ) {
                    // Top App Bar Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 8.dp, start = 4.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ayarlar ve Veri Yönetimi",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900
                        )
                    }

                    // 2-Tab Modern Segmented Control / Tab Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Tab 0: Genel Ayarlar
                        SegmentedTabButton(
                            label = "Genel Ayarlar",
                            icon = Icons.Default.Settings,
                            isSelected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            modifier = Modifier.weight(1f)
                        )

                        // Tab 1: Veri Ayarları
                        SegmentedTabButton(
                            label = "Veri Ayarları",
                            icon = Icons.Default.Storage,
                            isSelected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (selectedTab == 0) {
                // =============================================================
                // SEKME 1: GENEL AYARLAR
                // =============================================================

                // A. Sürüm ve Güncelleme Kartı
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Slate200),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(TurquoisePrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Uygulama Sürümü: ${com.example.BuildConfig.VERSION_NAME}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = "GitHub üzerinden en son yayınlanan güncellemeyi kontrol edin.",
                                    fontSize = 12.sp,
                                    color = Slate600
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { checkAppUpdates() },
                            enabled = !isCheckingUpdate,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, TurquoisePrimary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TurquoiseDark)
                        ) {
                            if (isCheckingUpdate) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = TurquoiseDark
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Kontrol Ediliyor...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Güncellemeleri Denetle", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // B. Görünüm ve Performans Tercihleri (4 Switch Kartı)
                SettingSwitchCard(
                    title = "Karanlık Tema (Dark Mode)",
                    description = "Gece ve düşük ışıklı ortamlar için koyu renk düzeni",
                    icon = Icons.Default.DarkMode,
                    isChecked = isDarkMode,
                    onCheckedChange = { onToggleDarkMode() }
                )

                SettingSwitchCard(
                    title = "Pil Tasarrufu Modu",
                    description = "Kamera FPS ve ağır görsel efektleri kısarak pil ömrünü uzatır",
                    icon = Icons.Default.BatterySaver,
                    isChecked = isBatterySaverMode,
                    onCheckedChange = { onToggleBatterySaverMode() }
                )

                SettingSwitchCard(
                    title = "Ses Efektleri",
                    description = "Barkod okuma ve işlem tamamlama sesli bildirimleri",
                    icon = Icons.Default.VolumeUp,
                    isChecked = soundEffectsEnabled,
                    onCheckedChange = { onToggleSoundEffects() }
                )

                SettingSwitchCard(
                    title = "Titreşimli Geri Bildirim",
                    description = "Başarılı tarama ve silme işlemlerinde dokunsal geri bildirim",
                    icon = Icons.Default.Vibration,
                    isChecked = vibrationEnabled,
                    onCheckedChange = { onToggleVibration() }
                )

                // C. Mağaza Notları Kısayolu
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Slate200),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(TurquoisePrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditNote,
                                    contentDescription = null,
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Hatırlatıcılar ve Mağaza Notları",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = "Vardiya teslim notları, kritik reyon uyarıları ve yapılacak görevler.",
                                    fontSize = 12.sp,
                                    color = Slate600
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onNavigateToReminders,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, TurquoisePrimary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TurquoiseDark)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Notlar ve Hatırlatıcılar Sayfasına Git", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

            } else {
                // =============================================================
                // SEKME 2: VERİ AYARLARI
                // =============================================================

                // A. Veri Yedekleme ve Dışa Aktarma (Export)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Slate200),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Veri Yedekleme (Dışa Aktar)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Tüm ürünler, SKT'ler, fiyatlar, adetsel sayımlar ve takip kayıtlarını güvenle yedekleyin.",
                            fontSize = 12.sp,
                            color = Slate600
                        )

                        // Bilgi İpucu Kutusu
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TurquoisePrimary.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Yedekler doğrudan cihazınızın İndirilenler (Downloads) klasörüne güvenle kaydedilir.",
                                    fontSize = 11.5.sp,
                                    color = TurquoiseDark,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Buton 1: Tüm Verileri Dışa Aktar (JSON) - Dolgu Turkuaz
                        Button(
                            onClick = { triggerJsonExport() },
                            enabled = !isExporting,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Yedek Hazırlanıyor...", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tüm Verileri Dışa Aktar (JSON)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Buton 2: Cihaza Hızlı Yedek Al - Outline Turkuaz
                        OutlinedButton(
                            onClick = {
                                isSavingLocal = true
                                onSaveLocalBackup("manual") { file ->
                                    isSavingLocal = false
                                    if (file != null) {
                                        try {
                                            val jsonContent = file.readText(Charsets.UTF_8)
                                            BackupExportHelper.saveJsonToDownloads(context, jsonContent, file.name)
                                        } catch (_: Exception) {}
                                        val sizeStr = DataBackupManager.formatBytes(file.length())
                                        Toast.makeText(context, "✅ Cihaza ve İndirilenler'e yedek alındı:\n${file.name} ($sizeStr)", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Yedek oluşturulamadı.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isSavingLocal,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, TurquoisePrimary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TurquoiseDark)
                        ) {
                            if (isSavingLocal) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = TurquoiseDark
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Kaydediliyor...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cihaza Hızlı Yedek Al", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // B. Veri Yükleme ve İçe Aktarma (Import)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Slate200),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Veri Yükleme (İçe Aktar)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )

                        // Buton 1: JSON Yedekten Geri Yükle
                        OutlinedButton(
                            onClick = {
                                jsonImportLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, TurquoisePrimary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TurquoiseDark)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("JSON Yedekten Geri Yükle", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        // Buton 2: CSV Ürün Verisi Yükle (5 Kolon Desteği)
                        OutlinedButton(
                            onClick = {
                                csvPickerLauncher.launch(arrayOf(
                                    "text/csv",
                                    "text/comma-separated-values",
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "application/vnd.ms-excel",
                                    "text/plain",
                                    "*/*"
                                ))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, TurquoisePrimary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TurquoiseDark)
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CSV Ürün Verisi Yükle", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        // Format Bilgi Kutusu
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF0FDFA),
                            border = BorderStroke(1.dp, Color(0xFF99F6E4)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Format: Barkod;ÜrünKodu;ÜrünAdı;Fiyat;Kategori",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TurquoiseDark
                                    )
                                    Text(
                                        text = "Örnek: 8690504011223;25002501;SÜTAŞ AYRAN 200 ML;12.50;Dolap Ürünleri",
                                        fontSize = 11.sp,
                                        color = Slate600
                                    )
                                }
                            }
                        }
                    }
                }

                // C. Bakım, Onarım ve Sıfırlama
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Slate200),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Sistem Bakımı ve Onarım",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )

                        // Buton 1: Veritabanını Onar & Düzelt
                        OutlinedButton(
                            onClick = {
                                onFixAndRepairDatabase { count, message ->
                                    showRepairResultDialog = message
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, TurquoisePrimary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TurquoiseDark)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Veritabanını Onar & Düzelt", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        // Buton 2: Verileri Sıfırla (Tehlikeli Bölge - Kırmızı Outline)
                        OutlinedButton(
                            onClick = { showResetDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ExpiredRed),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpiredRed)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = ExpiredRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verileri Sıfırla (Tehlikeli Bölge)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SegmentedTabButton(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) TurquoiseDark else Slate100,
        border = if (isSelected) null else BorderStroke(1.dp, Slate200),
        modifier = modifier.height(44.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else Slate700,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Slate700
            )
        }
    }
}

@Composable
private fun SettingSwitchCard(
    title: String,
    description: String,
    icon: ImageVector,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TurquoisePrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TurquoiseDark,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    text = description,
                    fontSize = 11.5.sp,
                    color = Slate600
                )
            }

            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = TurquoisePrimary,
                    uncheckedThumbColor = Slate500,
                    uncheckedTrackColor = Slate200
                )
            )
        }
    }
}
