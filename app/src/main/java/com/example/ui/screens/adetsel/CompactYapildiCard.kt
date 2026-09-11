package com.example.ui.screens.adetsel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdetselKayit
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.ExpiredRedBorder
import com.example.ui.theme.ExpiredRedContainer
import com.example.ui.theme.ExpiredRedDark
import com.example.ui.theme.NormalGreenBorder
import com.example.ui.theme.NormalGreenContainer
import com.example.ui.theme.NormalGreenDark
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.WarningBlueBorder
import com.example.ui.theme.WarningBlueContainer
import com.example.ui.theme.WarningBlueDark

/**
 * COMPACT YAPILDI CARD
 * Belirgin kutu (card container) tasarımında, durum rozeti ve aksiyonları olan kutu.
 */
@Composable
fun CompactYapildiCard(
    kayit: AdetselKayit,
    onUndo: () -> Unit,
    onDelete: () -> Unit
) {
    val statusBg: Color
    val statusText: Color
    val statusBorder: Color
    val statusLabel: String

    val absFark = kotlin.math.abs(kayit.farkAdet)
    when (kayit.sayimSonucu) {
        "EKSIK" -> {
            statusBg = ExpiredRedContainer
            statusText = ExpiredRedDark
            statusBorder = ExpiredRedBorder
            val miktar = if (absFark == 0) 1 else absFark
            statusLabel = "EKSİK: -$miktar Adet"
        }
        "FAZLA" -> {
            statusBg = WarningBlueContainer
            statusText = WarningBlueDark
            statusBorder = WarningBlueBorder
            val miktar = if (absFark == 0) 1 else absFark
            statusLabel = "FAZLA: +$miktar Adet"
        }
        else -> {
            statusBg = NormalGreenContainer
            statusText = NormalGreenDark
            statusBorder = NormalGreenBorder
            statusLabel = "TAM (0 Fark)"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, statusBorder.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Durum Rozeti Kutusu
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusBg,
                    border = BorderStroke(1.dp, statusBorder)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        color = statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = kayit.getFormattedIslemTarihi(),
                    fontSize = 10.5.sp,
                    color = Slate500,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Ürün Adı
            Text(
                text = kayit.urunAdi.uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = Slate900,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Slate100,
                    border = BorderStroke(0.6.dp, Slate300)
                ) {
                    Text(
                        text = "KOD: ${kayit.getDisplayCode()}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (kayit.notlar.isNotBlank()) {
                    Text(
                        text = "Not: ${kayit.notlar}",
                        fontSize = 11.sp,
                        color = Slate700,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = Slate200, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(2.dp))

            // Geri Alma / Silme Butonları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onUndo,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = TurquoiseDark,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tekrar Sayıma Al",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TurquoiseDark
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Slate100,
                    border = BorderStroke(0.8.dp, Slate300),
                    modifier = Modifier.size(28.dp)
                ) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Sil",
                            tint = ExpiredRed,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
