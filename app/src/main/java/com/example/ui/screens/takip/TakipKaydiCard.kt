package com.example.ui.screens.takip

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DepoIadeKaydi
import com.example.data.DepoIadeManager
import com.example.data.IadeDurumu
import com.example.data.IadeOncelik
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.ExpiredRedContainer
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark

@Composable
fun TakipKaydiCard(
    record: DepoIadeKaydi,
    onStatusChange: (IadeDurumu) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit,
    onImageClick: (String) -> Unit = {},
    onClick: () -> Unit = {}
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
        IadeOncelik.NORMAL -> Slate500
    }

    val cardShape = RoundedCornerShape(14.dp)
    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.2.dp,
            if (record.isKritik) ExpiredRed.copy(alpha = 0.4f) else Slate200
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Üst Satır: Ürün Adı, Kodu ve Tarih / Sağda Rozetler
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = record.urunAdi,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            fontSize = 15.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Tek satırda Ürün Kodu ve İade Tarihi (+ Onay Tarihi)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!record.urunKodu.isNullOrBlank()) {
                            Text(
                                text = "🏷️ ${record.urunKodu}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TurquoiseDark
                            )
                            Text("•", color = Slate400, fontSize = 10.sp)
                        }
                        Text(
                            text = "📅 ${record.iadeTarihi}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Slate500,
                                fontSize = 11.5.sp
                            )
                        )
                        if (record.durum == IadeDurumu.ONAYLANDI) {
                            val approvedTime = record.approvedAt ?: record.guncellemeTarihiMillis
                            Text("•", color = Slate400, fontSize = 10.sp)
                            Text(
                                text = "✓ Onay: ${DepoIadeManager.formatMillisToDate(approvedTime)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF16A34A),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Durum Rozeti
                    Surface(
                        color = statusBg,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = record.durum.displayName,
                            color = statusColor,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                        )
                    }

                    // Öncelik Rozeti
                    if (record.oncelik != IadeOncelik.NORMAL) {
                        Surface(
                            color = if (record.isKritik) ExpiredRedContainer else Color(0xFFFFF7ED),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = record.oncelik.displayName,
                                color = prioColor,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            // 2. Alt Kısım: Durum butonu ile Paylaş, Düzenle, Sil ikonları
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Durum Butonu
                if (record.durum == IadeDurumu.DEVAM_EDIYOR) {
                    FilledTonalButton(
                        onClick = { onStatusChange(IadeDurumu.ONAYLANDI) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFDCFCE7),
                            contentColor = Color(0xFF16A34A)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Onayla", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    FilledTonalButton(
                        onClick = { onStatusChange(IadeDurumu.REDDEDILDI) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ExpiredRedContainer,
                            contentColor = ExpiredRed
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Reddet", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onStatusChange(IadeDurumu.DEVAM_EDIYOR) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Yeniden Aç", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // WhatsApp Butonu
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE8F5E9))
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "WhatsApp Paylaş",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Düzenle
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Slate100)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Düzenle",
                        tint = Slate700,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Sil
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ExpiredRedContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Sil",
                        tint = ExpiredRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
