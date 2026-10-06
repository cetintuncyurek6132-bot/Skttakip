package com.example.ui.screens.csv

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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
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
import com.example.data.BackupMetadata
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsTabContent(
    localBackups: List<BackupMetadata> = emptyList(),
    onTriggerLocalBackup: () -> Unit = {},
    onRefreshLocalBackups: () -> Unit = {},
    isTakingBackup: Boolean = false,
    onTriggerJsonExport: () -> Unit = {},
    onTriggerJsonImport: () -> Unit = {},
    onTriggerJsonShare: () -> Unit = {},
    onTriggerProductsExport: () -> Unit = {},
    onTriggerCountAndIadeExport: () -> Unit = {},
    onTriggerCsvExport: () -> Unit = {},
    onTriggerCsvImport: () -> Unit = {},
    isRestoringBackup: Boolean = false,
    onSelectBackupToRestore: (BackupMetadata) -> Unit = {},
    onFixAndRepairDatabase: (onResult: (Int, String) -> Unit) -> Unit = {},
    onRepairResult: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var showImportSheet by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ========================================================
        // KART 1: [ 📥 İÇE AKTARMA ]
        // ========================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showImportSheet = true },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.2.dp, Color(0xFF93C5FD)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "İçe Aktarma",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Yedek dosyasından veya Excel/CSV'den veri yükleyin",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Aç",
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // ========================================================
        // KART 2: [ 📤 DIŞA AKTARMA / YEDEKLEME ]
        // ========================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showExportSheet = true },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.2.dp, Color(0xFFA7F3D0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFECFDF5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dışa Aktarma / Yedekleme",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Cihaz verilerini, ürünleri ve sayımları yedekleyin",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Aç",
                    tint = Color(0xFF059669),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    // ========================================================
    // 3. DIŞA AKTARMA MODAL BOTTOM SHEET
    // ========================================================
    if (showExportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExportSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFECFDF5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Dışa Aktarma ve Yedekleme",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Yedeklemek istediğiniz veri kapsamını seçin",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = Slate200)

                // BUTON 1: [ 💾 Tüm Verileri Yedek Al ]
                ExportOptionCard(
                    title = "Tüm Verileri Yedek Al (Tam Yedek)",
                    description = "Ürünler, ürünlere girilen tüm SKT parti adetleri, birim fiyatlar, sayım ve iade takip verilerinin tamamını eksiksiz tek bir .json yedeği olarak dışa aktarır ve paylaşır.",
                    icon = Icons.Default.Save,
                    iconBgColor = Color(0xFFECFDF5),
                    iconColor = Color(0xFF059669),
                    badgeText = "Önerilen",
                    badgeColor = Color(0xFF059669),
                    onClick = {
                        showExportSheet = false
                        onTriggerJsonShare()
                    }
                )

                // BUTON 2: [ 📦 Ürünleri Yedek Al ]
                ExportOptionCard(
                    title = "Ürünleri Yedek Al",
                    description = "Kayıtlı ürünleri, barkodları, reyonları, fiyatları ve girilmiş tüm SKT parti adetlerini yedekler.",
                    icon = Icons.Default.Inventory2,
                    iconBgColor = Color(0xFFEFF6FF),
                    iconColor = Color(0xFF2563EB),
                    onClick = {
                        showExportSheet = false
                        onTriggerProductsExport()
                    }
                )

                // BUTON 3: [ 📋 Sayım ve İade Takip Verilerini Yedek Al ]
                ExportOptionCard(
                    title = "Sayım ve İade Takip Verilerini Yedek Al",
                    description = "Adetsel sayım sonuçlarını ve iade & depo takip kayıtlarını yedekler.",
                    icon = Icons.Default.AssignmentTurnedIn,
                    iconBgColor = Color(0xFFFAF5FF),
                    iconColor = Color(0xFF7C3AED),
                    onClick = {
                        showExportSheet = false
                        onTriggerCountAndIadeExport()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    // ========================================================
    // 4. İÇE AKTARMA MODAL BOTTOM SHEET
    // ========================================================
    if (showImportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showImportSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "İçe Aktarma",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Yüklemek istediğiniz dosya türünü seçin",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = Slate200)

                // BUTON 1: [ 📄 JSON Yedek Dosyasından Yükle ]
                ExportOptionCard(
                    title = "JSON Yedek Dosyasından Yükle",
                    description = "Tam veya parça yedek dosyasını içeri aktarır. Mevcut verileriniz korunarak akıllıca birleştirilir.",
                    icon = Icons.Default.DataObject,
                    iconBgColor = Color(0xFFEFF6FF),
                    iconColor = Color(0xFF2563EB),
                    onClick = {
                        showImportSheet = false
                        onTriggerJsonImport()
                    }
                )

                // BUTON 2: [ 📊 Excel / CSV'den Ürün Aktar ]
                ExportOptionCard(
                    title = "Excel / CSV'den Ürün Aktar",
                    description = "Toplu ürün listesi yükler. Barkod, ürün kodu, reyon ve fiyatları otomatik içeri aktarır.",
                    icon = Icons.Default.TableChart,
                    iconBgColor = Color(0xFFECFDF5),
                    iconColor = Color(0xFF059669),
                    onClick = {
                        showImportSheet = false
                        onTriggerCsvImport()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ExportOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBgColor: Color,
    iconColor: Color,
    badgeText: String? = null,
    badgeColor: Color = TurquoisePrimary,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Slate900
                        )
                        if (badgeText != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = badgeColor.copy(alpha = 0.12f),
                                border = BorderStroke(0.8.dp, badgeColor.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = badgeText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = description,
                        fontSize = 11.5.sp,
                        color = Slate600,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Slate400,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
