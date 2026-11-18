package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.NormalGreenBorder
import com.example.ui.theme.NormalGreenContainer
import com.example.ui.theme.NormalGreenDark
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoiseLight
import com.example.ui.theme.TurquoisePrimary
import com.example.util.AppUpdateChecker
import com.example.util.AppUpdateInfo
import com.example.util.ReleaseNotesTranslator

@Composable
fun AppUpdateDialog(
    updateInfo: AppUpdateInfo,
    isDownloading: Boolean,
    downloadProgress: Int,
    downloadedBytes: Long = 0L,
    totalBytes: Long = 0L,
    onConfirmUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    val cleanVer = remember(updateInfo.latestVersionName) {
        updateInfo.latestVersionName.replace(Regex("(?i)beta|v|sürüm|\\:"), "").trim()
    }

    // Gemini API çağrısı bir suspend işlem olduğu için state ile yönetilmeli
    var noteItems by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingNotes by remember { mutableStateOf(true) }
    val sourceNotes = updateInfo.rawReleaseNotes.ifBlank { updateInfo.releaseNotes }
    
    LaunchedEffect(sourceNotes, cleanVer) {
        isLoadingNotes = true
        try {
            val translated = ReleaseNotesTranslator.translateSmart(
                rawNotes = sourceNotes,
                versionName = cleanVer
            ).filterNot { ReleaseNotesTranslator.isVersionHeaderNote(it) }
            noteItems = if (translated.isNotEmpty()) {
                translated
            } else {
                ReleaseNotesTranslator.getCorporateFallback(cleanVer)
            }
        } catch (e: Exception) {
            noteItems = ReleaseNotesTranslator.getCorporateFallback(cleanVer)
        } finally {
            isLoadingNotes = false
        }
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

                Spacer(modifier = Modifier.height(8.dp))

                // Sürüm ve Boyut Rozetleri
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                    if (updateInfo.apkSizeFormatted.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Text(
                                text = "Boyut: ${updateInfo.apkSizeFormatted}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate700,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
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
                        .heightIn(min = 50.dp, max = 240.dp)
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

                        if (isLoadingNotes) {
                            // İskelet yükleme (basit)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                repeat(3) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .height(14.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    )
                                }
                            }
                        } else {
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
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. İndirme Durumu (Animasyonlu İkon + Shimmer Progress Bar & Canlı MB Takibi)
                val showDownloadSection = isDownloading || downloadProgress >= 100
                if (showDownloadSection) {
                    Spacer(modifier = Modifier.height(16.dp))
                    UpdateDownloadProgressSection(
                        downloadProgress = downloadProgress,
                        downloadedBytes = downloadedBytes,
                        totalBytes = totalBytes,
                        fallbackSizeFormatted = updateInfo.apkSizeFormatted,
                        fallbackSizeBytes = updateInfo.apkSizeBytes
                    )
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
                            .weight(0.95f)
                            .height(44.dp)
                            .testTag("app_update_dismiss_button"),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Slate600
                        ),
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Text(
                            text = "Daha Sonra",
                            fontSize = 12.sp,
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

@Composable
private fun UpdateDownloadProgressSection(
    downloadProgress: Int,
    downloadedBytes: Long,
    totalBytes: Long,
    fallbackSizeFormatted: String,
    fallbackSizeBytes: Long
) {
    val clampedProgress = downloadProgress.coerceIn(0, 100)
    val isDownloadCompleted = clampedProgress >= 100

    val animatedFraction by animateFloatAsState(
        targetValue = clampedProgress / 100f,
        animationSpec = tween(durationMillis = 320),
        label = "update_download_progress_fraction"
    )

    val sizeText = if (downloadedBytes > 0L) {
        val totalFormatted = if (totalBytes > 0L) {
            AppUpdateChecker.formatBytes(totalBytes)
        } else {
            fallbackSizeFormatted.ifBlank { AppUpdateChecker.formatBytes(downloadedBytes) }
        }
        "${AppUpdateChecker.formatBytes(downloadedBytes)} / $totalFormatted"
    } else if (fallbackSizeFormatted.isNotBlank()) {
        val estimatedBytes = (clampedProgress * (fallbackSizeBytes.takeIf { it > 0L } ?: (40 * 1024 * 1024L))) / 100
        "${AppUpdateChecker.formatBytes(estimatedBytes)} / $fallbackSizeFormatted"
    } else {
        ""
    }

    // İndirme sürerken dönen/nabız atan animasyon ve shimmer; tamamlanınca animasyonu durdur
    val iconRotation: Float
    val pulseAlpha: Float
    val shimmerProgress: Float
    if (!isDownloadCompleted) {
        val infiniteTransition = rememberInfiniteTransition(label = "update_download_infinite")
        val rot by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "update_icon_rotation"
        )
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 850, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "update_icon_pulse_alpha"
        )
        val shimmer by infiniteTransition.animateFloat(
            initialValue = -0.35f,
            targetValue = 1.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "update_bar_shimmer"
        )
        iconRotation = rot
        pulseAlpha = alpha
        shimmerProgress = shimmer
    } else {
        iconRotation = 0f
        pulseAlpha = 1f
        shimmerProgress = 0f
    }

    val containerBg = if (isDownloadCompleted) {
        NormalGreenContainer.copy(alpha = 0.45f)
    } else {
        TurquoiseLight.copy(alpha = 0.48f)
    }
    val containerBorder = if (isDownloadCompleted) {
        NormalGreenBorder.copy(alpha = 0.75f)
    } else {
        TurquoisePrimary.copy(alpha = 0.35f)
    }
    val badgeBg = if (isDownloadCompleted) {
        NormalGreenContainer
    } else {
        TurquoisePrimary.copy(alpha = 0.16f)
    }
    val badgeTint = if (isDownloadCompleted) NormalGreenDark else TurquoiseDark
    val barColor = if (isDownloadCompleted) NormalGreenDark else TurquoisePrimary
    val barTrackColor = if (isDownloadCompleted) {
        NormalGreenBorder.copy(alpha = 0.3f)
    } else {
        TurquoisePrimary.copy(alpha = 0.16f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_update_download_progress_card"),
        shape = RoundedCornerShape(16.dp),
        color = containerBg,
        border = BorderStroke(1.dp, containerBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = badgeBg,
                    modifier = Modifier
                        .size(40.dp)
                        .graphicsLayer {
                            alpha = pulseAlpha
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isDownloadCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "İndirme tamamlandı",
                                tint = NormalGreenDark,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Güncelleme indiriliyor",
                                tint = badgeTint,
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer {
                                        rotationZ = iconRotation
                                    }
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (isDownloadCompleted) "İndirme Tamamlandı" else "Güncelleme İndiriliyor...",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDownloadCompleted) NormalGreenDark else Slate600
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDownloadCompleted) NormalGreenDark else Slate900
                                )
                            ) {
                                append("%$clampedProgress")
                            }
                            if (sizeText.isNotBlank()) {
                                withStyle(
                                    SpanStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate700
                                    )
                                ) {
                                    append("  •  $sizeText")
                                }
                            }
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 8.dp kalınlıkta, tamamen yuvarlak (RoundedCornerShape(50)) ve shimmer efektli ilerleme çubuğu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(barTrackColor)
            ) {
                val fillModifier = Modifier
                    .fillMaxWidth(fraction = animatedFraction.coerceIn(0.02f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))

                if (isDownloadCompleted) {
                    Box(
                        modifier = fillModifier.background(barColor)
                    )
                } else {
                    val shimmerBrush = Brush.linearGradient(
                        colors = listOf(
                            TurquoiseDark,
                            TurquoisePrimary,
                            Color(0xFF99F6E4),
                            TurquoisePrimary,
                            TurquoiseDark
                        ),
                        start = Offset((shimmerProgress - 0.35f) * 600f, 0f),
                        end = Offset((shimmerProgress + 0.35f) * 600f, 0f)
                    )
                    Box(
                        modifier = fillModifier.background(shimmerBrush)
                    )
                }
            }
        }
    }
}
