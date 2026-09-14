package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.UserManager
import com.example.data.BackupMetadata
import com.example.data.BackupRestoreResult
import com.example.data.Product
import com.example.ui.screens.csv.*
import com.example.ui.theme.*

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
    onImportLines: (List<String>) -> com.example.util.CsvParseResult = { com.example.util.CsvParseResult(emptyList(), 0, 0) },
    onResetDatabase: () -> Unit = {},
    onRestoreSeedData: () -> Unit = {},
    onOpenQrFixMode: () -> Unit = {},
    onExportJsonBackup: ((String) -> Unit) -> Unit = {},
    onSaveLocalBackup: (String, (java.io.File?) -> Unit) -> Unit = { _, _ -> },
    onGetLocalBackups: () -> List<BackupMetadata> = { emptyList() },
    onRestoreFromJson: (String, Boolean, (BackupRestoreResult) -> Unit) -> Unit = { _, _, _ -> },
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentUser by UserManager.currentUser.collectAsState()

    val isMsUser = currentUser?.role == "MS"
    var selectedTab by remember { mutableIntStateOf(0) }

    // Dialog & Process states
    var showResetDialog by remember { mutableStateOf(false) }
    var isEditingProfile by remember { mutableStateOf(false) }
    var tempProfileName by remember { mutableStateOf("") }
    var tempProfileRole by remember { mutableStateOf("") }
    var tempProfileDepartment by remember { mutableStateOf("") }

    var repairResultText by remember { mutableStateOf<String?>(null) }
    var backupStatusMessage by remember { mutableStateOf<String?>(null) }
    var restoreConfirmBackupFile by remember { mutableStateOf<BackupMetadata?>(null) }

    // Backup & Restore states
    var localBackups by remember { mutableStateOf<List<BackupMetadata>>(emptyList()) }
    var isTakingBackup by remember { mutableStateOf(false) }
    var isRestoringBackup by remember { mutableStateOf(false) }

    fun refreshLocalBackups() {
        localBackups = onGetLocalBackups()
    }

    LaunchedEffect(Unit) {
        refreshLocalBackups()
    }

    // JSON FULL EXPORT LAUNCHER
    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            onExportJsonBackup { jsonString ->
                try {
                    context.contentResolver.openOutputStream(fileUri)?.use { stream ->
                        stream.write(jsonString.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, "✅ Ürünler, SKT/Adetler, Takip ve Adetsel yedeği başarıyla kaydedildi!", Toast.LENGTH_LONG).show()
                    refreshLocalBackups()
                } catch (e: Exception) {
                    Toast.makeText(context, "Kayıt hatası: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // JSON FULL IMPORT LAUNCHER
    val jsonImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            try {
                val inputStream = context.contentResolver.openInputStream(fileUri)
                val jsonContent = inputStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                if (jsonContent.isNullOrBlank()) {
                    Toast.makeText(context, "Seçilen JSON dosyası boş!", Toast.LENGTH_SHORT).show()
                    return@let
                }
                isRestoringBackup = true
                onRestoreFromJson(jsonContent, true) { result ->
                    isRestoringBackup = false
                    backupStatusMessage = result.message
                    refreshLocalBackups()
                }
            } catch (e: Exception) {
                isRestoringBackup = false
                Toast.makeText(context, "Yedek dosyası okunamadı: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // CSV Sub-Category states
    var selectedCsvSubTab by remember { mutableIntStateOf(0) } // 0: İçe Aktar, 1: Dışa Aktar

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

    // Restrict MS only tabs safely on initial render
    LaunchedEffect(isMsUser) {
        if (!isMsUser && selectedTab != 0) {
            selectedTab = 0
        }
    }

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

                val isZipOrXlsx = bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() && bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()
                val isOldXls = bytes.size >= 4 && bytes[0] == 0xD0.toByte() && bytes[1] == 0xCF.toByte() && bytes[2] == 0x11.toByte() && bytes[3] == 0xE0.toByte()

                if (isZipOrXlsx) {
                    val xlsxLines = com.example.util.XlsxParser.parseXlsxToCsvLines(bytes)
                    if (xlsxLines.isNotEmpty()) {
                        val importResult = onImportLines(xlsxLines)
                        val newCount = importResult.productsToInsert.size
                        val skippedCount = importResult.skippedLineCount
                        val message = buildString {
                            if (newCount > 0) {
                                append("✅ Excel (.xlsx) dosyasından $newCount adet yeni ürün başarıyla eklendi!")
                            } else {
                                append("Excel dosyasında eklenecek yeni ürün bulunamadı.")
                            }
                            if (skippedCount > 0) {
                                append("\n⚠️ $skippedCount adet satır hatalı format nedeniyle atlandı.")
                            }
                        }
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
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

                val nullByteCount = bytes.take(1024).count { it == 0.toByte() }
                if (nullByteCount > 5) {
                    Toast.makeText(context, "⚠️ Geçersiz dosya formatı! Lütfen .xlsx veya düz metin CSV dosyası yükleyin.", Toast.LENGTH_LONG).show()
                    return@let
                }

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
                val importResult = onImportLines(lines)
                val newCount = importResult.productsToInsert.size
                val skippedCount = importResult.skippedLineCount
                val message = buildString {
                    if (newCount > 0) {
                        append("✅ $newCount adet yeni ürün başarıyla eklendi!")
                    } else {
                        append("Seçilen dosyada eklenecek yeni ürün bulunamadı.")
                    }
                    if (skippedCount > 0) {
                        append("\n⚠️ $skippedCount adet satır hatalı format nedeniyle atlandı.")
                    }
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Dosya okunamadı: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // DIALOGS
    CsvScreenResetDatabaseDialog(
        show = showResetDialog,
        onDismiss = { showResetDialog = false },
        onConfirm = onResetDatabase,
        context = context
    )

    CsvScreenProfileEditDialog(
        show = isEditingProfile,
        name = tempProfileName,
        role = tempProfileRole,
        department = tempProfileDepartment,
        onNameChange = { tempProfileName = it },
        onRoleChange = { tempProfileRole = it },
        onDepartmentChange = { tempProfileDepartment = it },
        currentUser = currentUser,
        onDismiss = { isEditingProfile = false },
        context = context
    )

    CsvScreenRepairResultDialog(
        repairResultText = repairResultText,
        onDismiss = { repairResultText = null }
    )

    CsvScreenBackupStatusMessageDialog(
        message = backupStatusMessage,
        onDismiss = { backupStatusMessage = null }
    )

    CsvScreenRestoreConfirmationDialog(
        backupMetadata = restoreConfirmBackupFile,
        onDismiss = { restoreConfirmBackupFile = null },
        onRestoreFromJson = onRestoreFromJson,
        onRestoreComplete = { msg ->
            isRestoringBackup = false
            backupStatusMessage = msg
            refreshLocalBackups()
        },
        context = context
    )

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
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
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Slate100, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Geri",
                                tint = Slate700,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
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
                                text = if (isMsUser) "AYARLAR & VERİ YÖNETİMİ" else "GENEL AYARLAR",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isMsUser) "Görünüm, Bildirimler, Güvenli Yedekleme ve CSV" else "Uygulama Görünümü ve Bildirim Tercihleri",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

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
                                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("AYARLAR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                if (!isMsUser) {
                                    Toast.makeText(context, "🔒 Bu sekmeye erişmek için 'Mağaza Sorumlusu (MS)' yetkisi gereklidir.", Toast.LENGTH_LONG).show()
                                } else {
                                    selectedTab = 1
                                }
                            },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (!isMsUser) Icons.Default.Lock else Icons.Default.Security,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = if (!isMsUser) Color.Gray else if (selectedTab == 1) TurquoisePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "VERİLER & YEDEK",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isMsUser) Color.Gray else Color.Unspecified
                                    )
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = {
                                if (!isMsUser) {
                                    Toast.makeText(context, "🔒 Bu sekmeye erişmek için 'Mağaza Sorumlusu (MS)' yetkisi gereklidir.", Toast.LENGTH_LONG).show()
                                } else {
                                    selectedTab = 2
                                }
                            },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (!isMsUser) Icons.Default.Lock else Icons.Default.UploadFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = if (!isMsUser) Color.Gray else if (selectedTab == 2) TurquoisePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "CSV İŞLEMLERİ",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isMsUser) Color.Gray else Color.Unspecified
                                    )
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
                GeneralSettingsTabContent(
                    currentUser = currentUser,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode,
                    isBatterySaverMode = isBatterySaverMode,
                    onToggleBatterySaverMode = onToggleBatterySaverMode,
                    soundEffectsEnabled = soundEffectsEnabled,
                    onToggleSoundEffects = onToggleSoundEffects,
                    vibrationEnabled = vibrationEnabled,
                    onToggleVibration = onToggleVibration,
                    onEditProfileClick = {
                        tempProfileName = currentUser?.fullName ?: ""
                        tempProfileRole = currentUser?.roleTitle ?: ""
                        tempProfileDepartment = currentUser?.department ?: "Süt & Şarküteri Reyonu"
                        isEditingProfile = true
                    },
                    onOpenQrFixMode = onOpenQrFixMode,
                    onFixAndRepairDatabase = onFixAndRepairDatabase,
                    onRepairResult = { msg -> repairResultText = msg }
                )
            } else if (selectedTab == 1) {
                BackupSettingsTabContent(
                    localBackups = localBackups,
                    onTriggerLocalBackup = {
                        isTakingBackup = true
                        onSaveLocalBackup("manual") { file ->
                            isTakingBackup = false
                            if (file != null) {
                                backupStatusMessage = "✅ Güvenli yedek başarıyla alındı ve cihaz hafızasına kaydedildi!\n\nDosya: ${file.name}\nBoyut: ${com.example.data.DataBackupManager.formatBytes(file.length())}\n\nYedeklenen Veriler:\n• Ürünler ve SKT / stok adetleri\n• Takip sayfası kayıtları\n• Adetsel sayım kayıtları"
                                refreshLocalBackups()
                            } else {
                                backupStatusMessage = "⚠️ Yedek oluşturulamadı."
                            }
                        }
                    },
                    onRefreshLocalBackups = { refreshLocalBackups() },
                    isTakingBackup = isTakingBackup,
                    onTriggerJsonExport = {
                        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                        val suggestedName = "SKT_TAKIP_YEDEK_$dateStr.json"
                        jsonExportLauncher.launch(suggestedName)
                    },
                    onTriggerJsonImport = {
                        jsonImportLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    },
                    isRestoringBackup = isRestoringBackup,
                    onSelectBackupToRestore = { b ->
                        restoreConfirmBackupFile = b
                    },
                    onFixAndRepairDatabase = onFixAndRepairDatabase,
                    onRepairResult = { msg -> repairResultText = msg }
                )
            } else if (selectedTab == 2) {
                // TAB CSV: CSV İŞLEMLERİ (2 KATEGORİ: İÇE AKTAR VE DIŞA AKTAR)
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
                    CsvImportSection(
                        filePickerLauncher = filePickerLauncher,
                        onResetDatabaseClick = { showResetDialog = true },
                        context = context
                    )
                } else {
                    CsvExportSection(
                        products = products,
                        csvExportText = csvExportText,
                        exportDocumentLauncher = exportDocumentLauncher,
                        context = context
                    )
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
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
