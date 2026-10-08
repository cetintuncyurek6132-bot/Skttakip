package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.UserManager
import com.example.data.BackupMetadata
import com.example.data.BackupRestoreResult
import com.example.data.Product
import com.example.ui.screens.csv.BackupSettingsTabContent
import com.example.ui.screens.csv.CsvScreenBackupStatusMessageDialog
import com.example.ui.screens.csv.CsvScreenProfileEditDialog
import com.example.ui.screens.csv.CsvScreenRepairResultDialog
import com.example.ui.screens.csv.CsvScreenResetDatabaseDialog
import com.example.ui.screens.csv.CsvScreenRestoreConfirmationDialog
import com.example.ui.screens.csv.GeneralSettingsTabContent
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import com.example.util.BackupExportHelper
import com.example.util.CsvParseResult
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CsvScreen(
    isDarkMode: Boolean = false,
    isBatterySaverMode: Boolean = false,
    soundEffectsEnabled: Boolean = true,
    vibrationEnabled: Boolean = true,
    barcodeSoundId: Int = 1,
    onUpdateBarcodeSound: (Int) -> Unit = {},
    labelFixSoundId: Int = 2,
    onUpdateLabelFixSound: (Int) -> Unit = {},
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
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by UserManager.currentUser.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var localBackups by remember { mutableStateOf<List<BackupMetadata>>(emptyList()) }
    var isTakingBackup by remember { mutableStateOf(false) }
    var isRestoringBackup by remember { mutableStateOf(false) }

    fun reloadLocalBackups() {
        coroutineScope.launch(Dispatchers.IO) {
            val list = onGetLocalBackups()
            withContext(Dispatchers.Main) {
                localBackups = list
            }
        }
    }

    // Dialog States
    var showResetDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editRole by remember { mutableStateOf("") }
    var editDepartment by remember { mutableStateOf("") }
    var repairResultText by remember { mutableStateOf<String?>(null) }
    var backupStatusMessage by remember { mutableStateOf<String?>(null) }
    var selectedBackupToRestore by remember { mutableStateOf<BackupMetadata?>(null) }
    var restorePendingJson by remember { mutableStateOf<String?>(null) }
    var pendingExportJson by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        reloadLocalBackups()
    }

    // Storage Access Framework: JSON Export Launcher
    val jsonCreateDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && !pendingExportJson.isNullOrBlank()) {
            val res = BackupExportHelper.writeJsonToUri(context, uri, pendingExportJson!!)
            if (res.isSuccess) {
                Toast.makeText(context, "✅ Yedek başarıyla kaydedildi!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Kayıt hatası: ${res.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Storage Access Framework: JSON Import Launcher
    val jsonImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { fileUri ->
            try {
                val json = context.contentResolver.openInputStream(fileUri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                if (!json.isNullOrBlank()) {
                    restorePendingJson = json
                } else {
                    Toast.makeText(context, "Seçilen dosya boş veya geçersiz!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Yedek okunamadı: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Storage Access Framework: CSV Import Launcher
    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { fileUri ->
            try {
                val lines = context.contentResolver.openInputStream(fileUri)?.bufferedReader(Charsets.UTF_8)?.useLines { it.toList() } ?: emptyList()
                if (lines.isNotEmpty()) {
                    val result = onImportLines(lines)
                    Toast.makeText(context, "✅ CSV İçe Aktarıldı: ${result.productsToInsert.size} ürün işlendi.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Seçilen dosya boş!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "CSV aktarım hatası: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = if (isDarkMode) Color(0xFF0F172A) else MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = if (isDarkMode) Color(0xFF0F172A) else MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("csv_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Geri",
                                tint = if (isDarkMode) Color(0xFFF8FAFC) else Slate900
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ayarlar ve Veri Yönetimi",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkMode) Color(0xFFF8FAFC) else Slate900
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SegmentedTabButton(
                            label = "Genel Ayarlar",
                            icon = Icons.Default.Settings,
                            isSelected = selectedTab == 0,
                            isDarkMode = isDarkMode,
                            onClick = { selectedTab = 0 },
                            modifier = Modifier.weight(1f)
                        )
                        SegmentedTabButton(
                            label = "Veri Ayarları",
                            icon = Icons.Default.Storage,
                            isSelected = selectedTab == 1,
                            isDarkMode = isDarkMode,
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.weight(1f)
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        editName = currentUser?.fullName.orEmpty()
                        editRole = currentUser?.roleTitle.orEmpty()
                        editDepartment = currentUser?.department.orEmpty()
                        showProfileDialog = true
                    },
                    onOpenQrFixMode = onOpenQrFixMode,
                    onFixAndRepairDatabase = onFixAndRepairDatabase,
                    onRepairResult = { msg -> repairResultText = msg },
                    barcodeSoundId = barcodeSoundId,
                    onUpdateBarcodeSound = onUpdateBarcodeSound,
                    labelFixSoundId = labelFixSoundId,
                    onUpdateLabelFixSound = onUpdateLabelFixSound
                )
            } else {
                BackupSettingsTabContent(
                    localBackups = localBackups,
                    onTriggerLocalBackup = {
                        isTakingBackup = true
                        onSaveLocalBackup("MANUEL") { file ->
                            isTakingBackup = false
                            reloadLocalBackups()
                            if (file != null) {
                                Toast.makeText(context, "✅ Güvenli yedek alındı: ${file.name}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onRefreshLocalBackups = {
                        reloadLocalBackups()
                    },
                    isTakingBackup = isTakingBackup,
                    onTriggerJsonExport = {
                        onExportJsonBackup { json ->
                            pendingExportJson = json
                            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                            jsonCreateDocLauncher.launch("SKT_Tam_Yedek_$timeStamp.json")
                        }
                    },
                    onTriggerJsonImport = {
                        jsonImportLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                    },
                    onTriggerJsonShare = {
                        onExportJsonBackup { json ->
                            val fileName = BackupExportHelper.generateBackupFileName("SKT_Tam_Yedek", "json")
                            BackupExportHelper.shareJsonBackup(context, json, fileName)
                        }
                    },
                    onTriggerCsvExport = {
                        if (products.isEmpty()) {
                            Toast.makeText(context, "Dışa aktarılacak ürün bulunamadı.", Toast.LENGTH_SHORT).show()
                        } else {
                            val csvContent = BackupExportHelper.exportProductsToCsv(products)
                            val fileName = BackupExportHelper.generateBackupFileName("SKT_Urun_Stok_Raporu", "csv")
                            val res = BackupExportHelper.saveFileToDownloads(context, csvContent, fileName, "text/csv")
                            if (res.isSuccess) {
                                Toast.makeText(context, "✅ CSV Raporu İndirilenler klasörüne kaydedildi:\n$fileName", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Kayıt hatası: ${res.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onTriggerCsvImport = {
                        csvImportLauncher.launch(arrayOf("text/comma-separated-values", "text/csv", "text/plain", "*/*"))
                    },
                    isRestoringBackup = isRestoringBackup,
                    onSelectBackupToRestore = { backup ->
                        selectedBackupToRestore = backup
                    },
                    onFixAndRepairDatabase = onFixAndRepairDatabase,
                    onRepairResult = { msg -> repairResultText = msg }
                )
            }
        }
    }

    // Delegated Dialogs
    CsvScreenResetDatabaseDialog(
        show = showResetDialog,
        onDismiss = { showResetDialog = false },
        onConfirm = onResetDatabase,
        context = context
    )

    CsvScreenProfileEditDialog(
        show = showProfileDialog,
        name = editName,
        role = editRole,
        department = editDepartment,
        onNameChange = { editName = it },
        onRoleChange = { editRole = it },
        onDepartmentChange = { editDepartment = it },
        currentUser = currentUser,
        onDismiss = { showProfileDialog = false },
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
        backupMetadata = selectedBackupToRestore,
        onDismiss = { selectedBackupToRestore = null },
        onRestoreFromJson = onRestoreFromJson,
        onRestoreComplete = { msg ->
            backupStatusMessage = msg
            reloadLocalBackups()
        },
        context = context
    )

    // Manual JSON Import Confirmation Dialog
    if (restorePendingJson != null) {
        AlertDialog(
            onDismissRequest = { restorePendingJson = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Restore, contentDescription = null, tint = TurquoisePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Yedekten Geri Yükle", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text("Seçtiğiniz JSON yedek dosyası mevcut verilerinizle birleştirilerek güvenle geri yüklenecektir. Devam edilsin mi?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val json = restorePendingJson ?: return@Button
                        restorePendingJson = null
                        isRestoringBackup = true
                        onRestoreFromJson(json, true) { res ->
                            isRestoringBackup = false
                            backupStatusMessage = res.message
                            reloadLocalBackups()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
                ) {
                    Text("GERİ YÜKLE", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { restorePendingJson = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@Composable
private fun SegmentedTabButton(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    isDarkMode: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) {
        if (isDarkMode) Color(0xFF0D9488) else TurquoiseDark
    } else {
        if (isDarkMode) Color(0xFF1E293B) else Slate100
    }
    val contentColor = if (isSelected) {
        Color.White
    } else {
        if (isDarkMode) Color(0xFF94A3B8) else Slate700
    }
    val border = if (isSelected) null else BorderStroke(1.dp, if (isDarkMode) Color(0xFF334155) else Slate200)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = border,
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
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                color = contentColor
            )
        }
    }
}
