package com.example.ui.screens.takip

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DepoIadeKaydi
import com.example.data.DepoIadeManager
import com.example.data.IadeDurumu
import com.example.data.IadeOncelik
import com.example.ui.components.AppBottomSheetWrapper
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.ExpiredRedContainer
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakipDetailSheet(
    record: DepoIadeKaydi,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onShareClick: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val statusColor = when (record.durum) {
        IadeDurumu.DEVAM_EDIYOR -> Color(0xFFD97706)
        IadeDurumu.ONAYLANDI -> Color(0xFF16A34A)
        IadeDurumu.REDDEDILDI -> ExpiredRed
    }

    val statusBg = when (record.durum) {
        IadeDurumu.DEVAM_EDIYOR -> Color(0xFFFEF3C7)
        IadeDurumu.ONAYLANDI -> Color(0xFFDCFCE7)
        IadeDurumu.REDDEDILDI -> ExpiredRedContainer
    }

    val prioColor = when (record.oncelik) {
        IadeOncelik.KRITIK -> ExpiredRed
        IadeOncelik.ONEMLI -> Color(0xFFEA580C)
        IadeOncelik.NORMAL -> Slate600
    }

    AppBottomSheetWrapper(
        onDismissRequest = onDismiss
    ) { _ ->
        // 1. ÜST BAŞLIK VE KAPAT BUTONU
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Takip Kaydı Detayı",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    fontSize = 18.sp
                )
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Slate500)
            }
        }

        HorizontalDivider(color = Slate200, modifier = Modifier.padding(horizontal = 16.dp))

        // 2. DETAY İÇERİK
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Ürün ve Tarih Bilgisi
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Slate50),
                border = BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = record.urunAdi,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900,
                                    fontSize = 17.sp
                                )
                            )
                            if (!record.urunKodu.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "🏷️ Ürün Kodu: ${record.urunKodu}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TurquoiseDark
                                )
                            }
                        }

                        // Durum ve Öncelik Rozetleri
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                color = statusBg,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = record.durum.displayName,
                                    color = statusColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            if (record.oncelik != IadeOncelik.NORMAL) {
                                Surface(
                                    color = if (record.isKritik) ExpiredRedContainer else Color(0xFFFFF7ED),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Öncelik: ${record.oncelik.displayName}",
                                        color = prioColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Slate200, thickness = 0.8.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📅 İade / İşlem Tarihi: ",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Slate500,
                                fontSize = 12.sp
                            )
                        )
                        Text(
                            text = record.iadeTarihi,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Slate800,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp
                            )
                        )
                    }

                    if (record.durum == IadeDurumu.ONAYLANDI) {
                        val approvedTime = record.approvedAt ?: record.guncellemeTarihiMillis
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✓ Onay Tarihi: ",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF16A34A),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = DepoIadeManager.formatMillisToDate(approvedTime),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF16A34A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                            )
                        }
                    }
                }
            }

            // Red / İade Nedeni
            Surface(
                color = Color(0xFFFFFBEB),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Red / İade Nedeni",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFB45309)
                        )
                        Text(
                            text = record.redNedeni,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78350F)
                        )
                    }
                }
            }

            // Açıklama Metni (Eksiksiz ve Ferah)
            if (record.aciklama.isNotBlank()) {
                Surface(
                    color = Slate50,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notes,
                                contentDescription = null,
                                tint = Slate600,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Açıklama / Notlar",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700
                                )
                            )
                        }
                        Text(
                            text = record.aciklama,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Slate800,
                                fontSize = 13.5.sp,
                                lineHeight = 19.sp
                            )
                        )
                    }
                }
            }

            // Takip Hatırlatıcısı (Varsa)
            if (record.hatirlatmaTarihi.isNotBlank()) {
                Surface(
                    color = TurquoisePrimary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = TurquoiseDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Takip Hatırlatıcısı",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TurquoiseDark
                            )
                            Text(
                                text = record.hatirlatmaTarihi,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TurquoiseDark
                            )
                        }
                    }
                }
            }

            // İrsaliye / Belge Görseli (Varsa Büyütülebilir Net Önizleme)
            if (record.hasGorsel && !record.irsaliyeGorselPath.isNullOrBlank()) {
                val file = remember(record.irsaliyeGorselPath) { File(record.irsaliyeGorselPath) }
                if (file.exists()) {
                    val bitmap = remember(record.irsaliyeGorselPath) {
                        try {
                            BitmapFactory.decodeFile(file.absolutePath)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (bitmap != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "İrsaliye / Belge Görseli",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Slate100)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true)
                                    ) {
                                        onImageClick(record.irsaliyeGorselPath)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "İrsaliye Belgesi",
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.Fit
                                )

                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ZoomIn,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Büyüt",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = Slate200, thickness = 0.8.dp)

            // 3. ALT AKSİYONLAR (DÜZENLE, PAYLAŞ, KAPAT)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Text("Kapat", fontSize = 13.sp, color = Slate700)
                }

                FilledTonalButton(
                    onClick = onShareClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFFE8F5E9),
                        contentColor = Color(0xFF2E7D32)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Paylaş", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onEditClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Düzenle", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
