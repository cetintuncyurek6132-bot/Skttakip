package com.example.ui.screens.csv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

@Composable
fun BackupSettingsTabContent(
    localBackups: List<BackupMetadata>,
    onTriggerLocalBackup: () -> Unit,
    onRefreshLocalBackups: () -> Unit = {},
    isTakingBackup: Boolean,
    onTriggerJsonExport: () -> Unit,
    onTriggerJsonImport: () -> Unit,
    isRestoringBackup: Boolean,
    onSelectBackupToRestore: (BackupMetadata) -> Unit,
    onFixAndRepairDatabase: (onResult: (Int, String) -> Unit) -> Unit,
    onRepairResult: (String) -> Unit
) {
    val context = LocalContext.current
    var isRepairing by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. DATA PROTECTION & HEALTH BANNER
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = EmeraldSuccess.copy(alpha = 0.1f)),
            border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(EmeraldSuccess),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🛡️ Aktif Veri Koruma Katmanı Devrede",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = EmeraldSuccess
                    )
                    Text(
                        text = "Uygulama güncellendiğinde tüm ürünleriniz, SKT tarihleri, stok adetleri, fire ve depo takip kayıtlarınız sıfırlanmadan güvenle korunmaktadır.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 2. VERİLER ANA BÖLÜMÜ
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(TurquoisePrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = TurquoisePrimary)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "VERİLER & YEDEKLEME YÖNETİMİ",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Yedekle, Geri Yükle, Dışa Aktar ve İçe Aktar",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                // 2.1 VERİ YEDEKLEME
                Text(
                    text = "1. VERİ YEDEKLEME",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TurquoiseDark
                )
                Text(
                    text = "Tüm ürün ve sayım verilerini cihaz hafızasına yedekleyin.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        onTriggerLocalBackup()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
                ) {
                    if (isTakingBackup) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("YEDEK ALINIYOR...", fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("💾 CİHAZ HAFIZASINA GÜVENLİ YEDEK AL", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.White)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                // 2.2 VERİLERİ DIŞA AKTAR (JSON)
                Text(
                    text = "2. VERİLERİ DIŞA AKTAR (JSON)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TurquoiseDark
                )
                Text(
                    text = "Ürünler, ürünlere eklenen SKT ve stok adetleri, Takip sayfası ve Adetsel sayım verilerini tek bir '.json' dosyasında dışa aktarır ve telefonunuza indirir.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = {
                                    onTriggerJsonExport()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, TurquoisePrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TurquoisePrimary)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("📤 VERİLERİ DIŞA AKTAR (JSON YEDEK)", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                // 2.3 VERİLERİ İÇE AKTAR (JSON)
                Text(
                    text = "3. VERİLERİ İÇE AKTAR (JSON)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TurquoiseDark
                )
                Text(
                    text = "Daha önce dışa aktardığınız yedek dosyasını seçerek ürünlerinizi, SKT ve adetlerini, Takip ve Adetsel sayfası verilerini eksiksiz geri yükleyin.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                                    onTriggerJsonImport()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent)
                ) {
                    if (isRestoringBackup) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("YÜKLENİYOR...", fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📥 JSON YEDEK DOSYASINI SEÇ VE YÜKLE", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }

        // 3. YEDEKTEKİ VERİDEN GERİ YÜKLE (CİHAZDAKİ YEREL YEDEKLER LİSTESİ)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = TurquoisePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "4. YEDEKTEKİ VERİDEN GERİ YÜKLE",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onRefreshLocalBackups,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yenile", tint = TurquoisePrimary, modifier = Modifier.size(18.dp))
                    }
                }

                Text(
                    text = "Cihaz hafızasında saklanan son otomatik ve manuel yedekler:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (localBackups.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.FolderZip, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
                            Text("Henüz yerel yedek bulunmuyor.", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Yukarıdaki 'Cihaz Hafızasına Güvenli Yedek Al' butonuyla anında yedek oluşturabilirsiniz.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        localBackups.forEach { backup ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (backup.tag == "startup_migration") EmeraldSuccess.copy(alpha = 0.15f) else TurquoisePrimary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (backup.tag == "startup_migration" || backup.isAutoBackup) Icons.Default.Security else Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = if (backup.tag == "startup_migration" || backup.isAutoBackup) EmeraldSuccess else TurquoisePrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (backup.tag == "startup_migration") "🛡️ Güncelleme Öncesi Otomatik Yedek" else "💾 ${backup.fileName}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val backupSummary = buildList {
                                            add("${backup.productCount} Ürün")
                                            if (backup.sktCount > 0) add("${backup.sktCount} SKT/Adet")
                                            if (backup.depoRecordCount > 0) add("${backup.depoRecordCount} Takip")
                                            if (backup.adetselCount > 0) add("${backup.adetselCount} Adetsel")
                                        }.joinToString(" • ")
                                        Text(
                                            text = "📦 $backupSummary",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TurquoiseDark
                                        )
                                        Text(
                                            text = "🕒 ${backup.formattedDate} • 💾 ${backup.fileSizeFormatted}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = {
                                                onSelectBackupToRestore(backup)
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
                                    ) {
                                        Text("Geri Yükle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. VERİTABANI AKILLI ONARIM VE QR DÜZELTME
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(AmberWarning.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = AmberWarning)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AKILLI SÜTUN & VERİ ONARIMI",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Veritabanındaki format ve kayma problemlerini onarır",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Ürün adı veya barkod alanlarında oluşabilecek kaymaları, boşlukları ve veri tipi uyuşmazlıklarını hiçbir veriyi silmeden otomatik tespit edip onarır.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

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
                            text = "⚡ KAYMALARI VE HATALARI OTOMATİK ONAR",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
