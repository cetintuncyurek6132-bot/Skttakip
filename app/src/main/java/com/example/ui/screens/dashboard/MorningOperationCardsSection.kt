package com.example.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ProductFilter
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.CriticalOrangeBorder
import com.example.ui.theme.CriticalOrangeDark
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.ExpiredRedBorder
import com.example.ui.theme.ExpiredRedDark
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.theme.SoonYellowBorder
import com.example.ui.theme.SoonYellowDark
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary

@Composable
fun MorningOperationCardsSection(
    expiredVariety: Int,
    expiredTotalStock: Int,
    criticalVariety: Int,
    criticalTotalStock: Int,
    soonVariety: Int,
    soonTotalStock: Int,
    onFilterSelectAndNavigate: (ProductFilter) -> Unit,
    onAnalyticsClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Sabah Operasyon Görevleri",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Slate900,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // "Sabah Operasyon Görevleri" başlığının hemen altında mini "Analiz & İstatistikler" grafik rozeti/butonu
        if (onAnalyticsClick != null) {
            Surface(
                onClick = onAnalyticsClick,
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF0FDFA),
                border = BorderStroke(1.dp, Color(0xFF99F6E4)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_analytics_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(TurquoisePrimary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = TurquoiseDark,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Analiz & İstatistikler",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TurquoiseDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• SKT & Satış/Fire Grafiği",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate600,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Analiz & İstatistikler",
                        tint = TurquoiseDark,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Yan yana dizilen 3'lü eşit genişlikte kompakt kritik görev kartları
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 🔴 Kart 1: Raftan Çekilecekler (Tarihi Geçen) - Açık Kırmızı
            MorningOperationCard(
                modifier = Modifier.weight(1f),
                title = "Raftan Çekilecekler",
                varietyCount = expiredVariety,
                totalStockCount = expiredTotalStock,
                badgeColor = ExpiredRed,
                bgColor = Color(0xFFFFF5F5),
                borderColor = ExpiredRedBorder,
                textColor = ExpiredRedDark,
                icon = Icons.Default.Warning,
                testTag = "op_card_expired",
                onClick = { onFilterSelectAndNavigate(ProductFilter.EXPIRED) }
            )

            // 🟠 Kart 2: Sıcak Satış (1-2 Gün) - Açık Turuncu
            MorningOperationCard(
                modifier = Modifier.weight(1f),
                title = "Sıcak Satış (1-2 Gün)",
                varietyCount = criticalVariety,
                totalStockCount = criticalTotalStock,
                badgeColor = CriticalOrange,
                bgColor = Color(0xFFFFF9F2),
                borderColor = CriticalOrangeBorder,
                textColor = CriticalOrangeDark,
                icon = Icons.Default.NotificationsActive,
                testTag = "op_card_critical",
                onClick = { onFilterSelectAndNavigate(ProductFilter.CRITICAL) }
            )

            // 🟡 Kart 3: Yakın Takip (3-7 Gün) - Açık Sarı
            MorningOperationCard(
                modifier = Modifier.weight(1f),
                title = "Yakın Takip (3-7 Gün)",
                varietyCount = soonVariety,
                totalStockCount = soonTotalStock,
                badgeColor = SoonYellowDark,
                bgColor = Color(0xFFFEFDF0),
                borderColor = SoonYellowBorder,
                textColor = Color(0xFF92400E),
                icon = Icons.Default.CalendarToday,
                testTag = "op_card_soon",
                onClick = { onFilterSelectAndNavigate(ProductFilter.SOON) }
            )
        }
    }
}

@Composable
fun MorningOperationCard(
    title: String,
    varietyCount: Int,
    totalStockCount: Int,
    badgeColor: Color,
    bgColor: Color,
    borderColor: Color,
    textColor: Color,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 1.dp,
        modifier = modifier.testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Üst: Küçük ikon ve durum başlığı
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(12.dp)
                    )
                }

                Text(
                    text = title,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // Orta: Kalın çeşit sayısı ve altında adet sayısı
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = "$varietyCount Çeşit",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = (-0.2).sp
                )
                Text(
                    text = "$totalStockCount Adet",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
