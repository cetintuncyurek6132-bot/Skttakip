package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun ModuleExportDialog(
    title: String,
    subtitle: String,
    recordCountText: String,
    onExportCsvDownload: () -> Unit,
    onExportCsvShare: () -> Unit,
    onExportJsonDownload: () -> Unit,
    onExportJsonShare: () -> Unit,
    onExportTxtDownload: (() -> Unit)? = null,
    onExportTxtShare: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Icon & Title
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(TurquoisePrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = TurquoiseDark,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                )

                // Record count badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate100,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = recordCountText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // EXCEL / CSV OPTIONS SECTION
                Text(
                    text = "EXCEL / CSV FORMATI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TurquoiseDark,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))

                ExportActionCard(
                    icon = Icons.Default.TableChart,
                    iconBg = Color(0xFFE8F5E9),
                    iconTint = Color(0xFF2E7D32),
                    title = "Excel / CSV Olarak İndir (.csv)",
                    description = "Cihazın İndirilenler klasörüne kaydeder",
                    onClick = onExportCsvDownload
                )

                Spacer(modifier = Modifier.height(6.dp))

                ExportActionCard(
                    icon = Icons.Default.Share,
                    iconBg = Color(0xFFE8F5E9),
                    iconTint = Color(0xFF2E7D32),
                    title = "Excel / CSV Olarak Paylaş",
                    description = "WhatsApp, Mail veya başka bir uygulamaya gönder",
                    onClick = onExportCsvShare
                )

                // OPTIONAL TXT REPORT SECTION
                if (onExportTxtDownload != null || onExportTxtShare != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "METİN RAPORU FORMATI (.TXT)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TurquoiseDark,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (onExportTxtDownload != null) {
                        ExportActionCard(
                            icon = Icons.Default.Description,
                            iconBg = Color(0xFFFFF3E0),
                            iconTint = Color(0xFFE65100),
                            title = "Metin Raporu İndir (.txt)",
                            description = "Tablo formatında salt metin olarak kaydeder",
                            onClick = onExportTxtDownload
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (onExportTxtShare != null) {
                        ExportActionCard(
                            icon = Icons.Default.Send,
                            iconBg = Color(0xFFFFF3E0),
                            iconTint = Color(0xFFE65100),
                            title = "Metin Raporu Paylaş",
                            description = "WhatsApp veya e-posta ile metin olarak gönder",
                            onClick = onExportTxtShare
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // JSON BACKUP OPTIONS SECTION
                Text(
                    text = "JSON VERİ YEDEĞİ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TurquoiseDark,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))

                ExportActionCard(
                    icon = Icons.Default.DataObject,
                    iconBg = TurquoisePrimary.copy(alpha = 0.12f),
                    iconTint = TurquoiseDark,
                    title = "JSON Yedeği Olarak İndir (.json)",
                    description = "Tam veri yapısıyla cihaz hafızasına kaydeder",
                    onClick = onExportJsonDownload
                )

                Spacer(modifier = Modifier.height(6.dp))

                ExportActionCard(
                    icon = Icons.Default.CloudUpload,
                    iconBg = TurquoisePrimary.copy(alpha = 0.12f),
                    iconTint = TurquoiseDark,
                    title = "JSON Yedeği Olarak Paylaş",
                    description = "Yedek dosyasını doğrudan paylaş",
                    onClick = onExportJsonShare
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Close Button
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Text(
                        text = "Kapat",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportActionCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Slate400,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
