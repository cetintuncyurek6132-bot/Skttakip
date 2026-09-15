package com.example.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.data.getDisplayName
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.CriticalOrangeDark
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.ExpiredRedDark
import com.example.ui.theme.NormalGreen
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary

/**
 * HIGH-CONTRAST DASHBOARD PRODUCT CARD
 * Clear remaining days pill, product name, barcode/code badge, and stock count.
 */
@Composable
fun DashboardProductItemCard(
    product: Product,
    onClick: () -> Unit
) {
    val daysLeft = product.getRemainingDays()

    val (badgeBg, badgeText, badgeLabel) = when {
        daysLeft < 0 -> Triple(ExpiredRed, Color.White, "$daysLeft GÜN")
        daysLeft == 0L -> Triple(ExpiredRed, Color.White, "BUGÜN SON GÜN")
        daysLeft == 1L -> Triple(CriticalOrangeDark, Color.White, "1 GÜN KALDI")
        daysLeft in 2L..3L -> Triple(CriticalOrange, Color.White, "$daysLeft GÜN KALDI")
        daysLeft in 4L..7L -> Triple(Color(0xFFD97706), Color.White, "$daysLeft GÜN KALDI")
        else -> Triple(NormalGreen, Color.White, "$daysLeft GÜN KALDI")
    }

    val cardBorderColor = when {
        daysLeft <= 0L -> ExpiredRed.copy(alpha = 0.4f)
        daysLeft in 1L..3L -> CriticalOrange.copy(alpha = 0.35f)
        else -> Slate200
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("dashboard_product_${product.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = BorderStroke(1.dp, cardBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Header Row: Days Left Badge + SKT Date + Stock Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeBg)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badgeLabel,
                            color = badgeText,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    if (product.sktTarihi > 0L) {
                        Text(
                            text = product.getFormattedSkt(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate600
                        )
                    }
                }

                // Adet Rozeti (Büyük ve belirgin)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (product.stokAdedi >= 10) Color(0xFFEEF2FF) else TurquoisePrimary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, if (product.stokAdedi >= 10) Color(0xFF818CF8) else TurquoisePrimary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (product.stokAdedi >= 10) {
                            Text("🔥 ", fontSize = 10.sp)
                        }
                        Text(
                            text = "${product.stokAdedi} Adet",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (product.stokAdedi >= 10) Color(0xFF4338CA) else TurquoiseDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Ürün Adı
            Text(
                text = product.getDisplayName().uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(5.dp))

            // Alt Satır: Barkod/Kod Bilgisi ve Fiyat (Kategori komple kaldırıldı)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val codeInfo = when {
                    product.urunKodu.isNotBlank() -> "Kod: ${product.urunKodu}"
                    product.barkod.isNotBlank() -> "Barkod: ${product.barkod}"
                    else -> ""
                }
                if (codeInfo.isNotBlank()) {
                    Text(
                        text = codeInfo,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (product.fiyat != null && product.fiyat > 0) {
                    Text(
                        text = "₺%.2f".format(product.fiyat),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        color = TurquoisePrimary
                    )
                }
            }
        }
    }
}
