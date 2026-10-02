package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoiseLight
import com.example.ui.theme.TurquoisePrimary
import com.example.util.AppUpdateInfo

@Composable
fun AppUpdateDialog(
    updateInfo: AppUpdateInfo,
    isDownloading: Boolean,
    downloadProgress: Int,
    onConfirmUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    val filteredNotes = remember(updateInfo.releaseNotes) {
        val raw = updateInfo.releaseNotes.trim()
        val lower = raw.lowercase()
        if (raw.isBlank() ||
            lower.contains("add files via upload") ||
            lower.contains("merge") ||
            lower.contains("commit") ||
            lower.contains("github")
        ) {
            "Performans ve kararlılık iyileştirmeleri yapıldı."
        } else {
            raw
        }
    }

    val noteItems = remember(filteredNotes) {
        filteredNotes.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { line ->
                line.removePrefix("•")
                    .removePrefix("-")
                    .removePrefix("*")
                    .trim()
            }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf("Performans ve kararlılık iyileştirmeleri yapıldı.") }
    }

    val cleanVer = remember(updateInfo.latestVersionName) {
        updateInfo.latestVersionName.replace(Regex("(?i)beta|v|sürüm|\\:"), "").trim()
    }

    Dialog(
        onDismissRequest = {
            if (!isDownloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isDownloading,
            dismissOnClickOutside = !isDownloading,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Üst Alan (Hero Header)
                Surface(
                    shape = CircleShape,
                    color = TurquoisePrimary.copy(alpha = 0.12f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = null,
                            tint = TurquoiseDark,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Yeni Sürüm Hazır!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Sürüm Rozeti
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TurquoiseLight,
                    border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "Sürüm: v$cleanVer",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TurquoiseDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Kısa Açıklama Metni
                Text(
                    text = "Uygulamayı en güncel özellikler ve performans iyileştirmeleriyle kullanmak için güncelleyin.",
                    fontSize = 12.5.sp,
                    color = Slate500,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Güncelleme Notları ("Neler Yeni?" Kutusu)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NewReleases,
                                contentDescription = null,
                                tint = TurquoiseDark,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Neler Yeni?",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TurquoiseDark
                            )
                        }

                        noteItems.forEach { note ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier
                                        .size(15.dp)
                                        .padding(top = 2.dp)
                                )
                                Text(
                                    text = note,
                                    fontSize = 12.5.sp,
                                    lineHeight = 18.sp,
                                    color = Slate700,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // 3. İndirme Durumu (Progress Bar)
                if (isDownloading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "İndiriliyor... %$downloadProgress",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "%$downloadProgress",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TurquoiseDark
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress.coerceIn(0, 100) / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = TurquoisePrimary,
                            trackColor = TurquoisePrimary.copy(alpha = 0.15f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 4. Alt Aksiyon Butonları (Yan Yana Dengeli Yerleşim)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "Daha Sonra" Butonu
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isDownloading,
                        modifier = Modifier
                            .weight(0.9f)
                            .height(44.dp)
                            .testTag("app_update_dismiss_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Slate600
                        ),
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Text(
                            text = "Daha Sonra",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    // "Şimdi Güncelle" Butonu
                    Button(
                        onClick = onConfirmUpdate,
                        enabled = !isDownloading,
                        modifier = Modifier
                            .weight(1.4f)
                            .height(44.dp)
                            .testTag("app_update_confirm_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TurquoisePrimary,
                            contentColor = Color.White,
                            disabledContainerColor = TurquoisePrimary.copy(alpha = 0.6f),
                            disabledContentColor = Color.White.copy(alpha = 0.8f)
                        )
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "İndiriliyor...",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Şimdi Güncelle",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
