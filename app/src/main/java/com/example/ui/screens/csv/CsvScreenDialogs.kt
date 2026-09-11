package com.example.ui.screens.csv

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BuildCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.UserAccount
import com.example.auth.UserManager
import com.example.data.BackupMetadata
import com.example.data.BackupRestoreResult
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary

@Composable
fun CsvScreenResetDatabaseDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    context: Context
) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Tüm Veriler Sıfırlansın Mı?",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = "Veritabanındaki tüm ürünler ve sayım raporları silinecektir. Bu işlem geri alınamaz.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                    Toast.makeText(context, "Tüm veriler sıfırlandı.", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ExpiredRed)
            ) {
                Text("SIFIRLA", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç")
            }
        }
    )
}

@Composable
fun CsvScreenProfileEditDialog(
    show: Boolean,
    name: String,
    role: String,
    department: String,
    onNameChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    onDepartmentChange: (String) -> Unit,
    currentUser: UserAccount?,
    onDismiss: () -> Unit,
    context: Context
) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = TurquoisePrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Profil Bilgilerini Düzenle",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Ad Soyad") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = role,
                    onValueChange = onRoleChange,
                    label = { Text("Unvan / Görev (örn: Mağaza Sorumlusu)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = department,
                    onValueChange = onDepartmentChange,
                    label = { Text("Reyon / Bölüm (örn: Süt & Şarküteri Reyonu)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (currentUser != null) {
                        val updated = currentUser.copy(
                            fullName = name.trim().ifBlank { currentUser.fullName },
                            roleTitle = role.trim().ifBlank { currentUser.roleTitle },
                            department = department.trim().ifBlank { currentUser.department }
                        )
                        UserManager.updateUserAccount(updated)
                        Toast.makeText(context, "Profil bilgileri güncellendi!", Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
            ) {
                Text("KAYDET", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@Composable
fun CsvScreenRepairResultDialog(
    repairResultText: String?,
    onDismiss: () -> Unit
) {
    if (repairResultText == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BuildCircle, contentDescription = null, tint = TurquoisePrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Veritabanı Onarım Raporu",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Text(
                text = repairResultText,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
            ) {
                Text("TAMAM", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun CsvScreenBackupStatusMessageDialog(
    message: String?,
    onDismiss: () -> Unit
) {
    if (message == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = EmeraldSuccess)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Veri Güvenliği ve Yedek Durumu",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Text(
                text = message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
            ) {
                Text("TAMAM", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun CsvScreenRestoreConfirmationDialog(
    backupMetadata: BackupMetadata?,
    onDismiss: () -> Unit,
    onRestoreFromJson: (String, Boolean, (BackupRestoreResult) -> Unit) -> Unit,
    onRestoreComplete: (String) -> Unit,
    context: Context
) {
    if (backupMetadata == null) return
    val b = backupMetadata
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Restore, contentDescription = null, tint = TurquoisePrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Yedekten Geri Yükle",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Seçilen yedek dosyasından veriler yüklensin mi?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("📁 Dosya: ${b.fileName}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "📦 Ürünler: ${b.productCount} adet" + if (b.sktCount > 0) " (${b.sktCount} SKT & adet)" else "",
                            fontSize = 12.sp,
                            color = TurquoiseDark,
                            fontWeight = FontWeight.Bold
                        )
                        if (b.depoRecordCount > 0) {
                            Text("📋 Takip Sayfası: ${b.depoRecordCount} kayıt", fontSize = 12.sp, color = TurquoiseDark, fontWeight = FontWeight.Bold)
                        }
                        if (b.adetselCount > 0) {
                            Text("🔢 Adetsel Sayım: ${b.adetselCount} kayıt", fontSize = 12.sp, color = TurquoiseDark, fontWeight = FontWeight.Bold)
                        }
                        Text("🕒 Tarih: ${b.formattedDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("💾 Boyut: ${b.fileSizeFormatted}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    text = "ℹ️ Güvenli birleştirme modu devrededir. Mevcut verileriniz silinmez, eksik ve güncel kayıtlar tamamlanır.",
                    fontSize = 11.sp,
                    color = EmeraldSuccess,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val file = b.file
                    onDismiss()
                    if (file.exists()) {
                        try {
                            val jsonStr = file.readText(Charsets.UTF_8)
                            onRestoreFromJson(jsonStr, true) { res ->
                                onRestoreComplete(res.message)
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Hata: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary)
            ) {
                Text("GÜVENLİ GERİ YÜKLE", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç")
            }
        }
    )
}
