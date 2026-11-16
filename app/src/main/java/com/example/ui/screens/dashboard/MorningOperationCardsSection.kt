package com.example.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.WarningAmber
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
import com.example.ui.theme.CriticalOrangeDark
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.ExpiredRedDark
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate900
import com.example.ui.theme.SoonYellowDark
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary

import androidx.compose.material.icons.automirrored.outlined.FactCheck

@Composable
fun MorningOperationCardsSection(
    expiredVariety: Int,
    expiredTotalStock: Int,
    criticalVariety: Int,
    criticalTotalStock: Int,
    soonVariety: Int,
    soonTotalStock: Int,
    onFilterSelectAndNavigate: (ProductFilter) -> Unit,
    onAnalyticsClick: (() -> Unit)? = null,
    onInventoryTrackingClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Sabah Operasyon Görevleri",
            fontSize = 15.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Slate900,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // "Sabah Operasyon Görevleri" başlığının hemen altında yan yana "Analiz & İstatistikler" ve "Envanter Takibi" kartları
        if (onAnalyticsClick != null || onInventoryTrackingClick != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onAnalyticsClick != null) {
                    Surface(
                        onClick = onAnalyticsClick,
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFFF0FDFA),
                        border = BorderStroke(1.dp, Color(0xFFCCFBF1)),
                        shadowElevation = 0.5.dp,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_analytics_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(TurquoisePrimary.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Analytics,
                                        contentDescription = null,
                                        tint = TurquoiseDark,
                                        modifier = Modifier.size(15.dp)
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
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                contentDescription = "Analiz & İstatistikler",
                                tint = TurquoiseDark,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                if (onInventoryTrackingClick != null) {
                    Surface(
                        onClick = onInventoryTrackingClick,
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        shadowElevation = 0.5.dp,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_inventory_tracking_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFDBEAFE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.FactCheck,
                                        contentDescription = null,
                                        tint = Color(0xFF1D4ED8),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Envanter Takibi",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1E40AF),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                contentDescription = "Envanter Takibi",
                                tint = Color(0xFF1D4ED8),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // Yan yana dizilen 3'lü eşit genişlikte pastel kompakt kritik görev kartları (18.dp yuvarlak köşeler)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 🔴 Kart 1: Raftan Çek (Tarihi Geçen) - Pastel Kırmızı/Pembe
            MorningOperationCard(
                modifier = Modifier.weight(1f),
                title = "Raftan Çek",
                varietyCount = expiredVariety,
                totalStockCount = expiredTotalStock,
                badgeColor = ExpiredRed,
                bgColor = Color(0xFFFFF7F7),
                borderColor = Color(0xFFFECACA).copy(alpha = 0.7f),
                textColor = ExpiredRedDark,
                icon = Icons.Outlined.WarningAmber,
                testTag = "op_card_expired",
                onClick = { onFilterSelectAndNavigate(ProductFilter.EXPIRED) }
            )

            // 🟠 Kart 2: Sıcak Satış (1-2 Gün) - Pastel Şeftali/Turuncu
            MorningOperationCard(
                modifier = Modifier.weight(1f),
                title = "Sıcak Satış",
                varietyCount = criticalVariety,
                totalStockCount = criticalTotalStock,
                badgeColor = CriticalOrange,
                bgColor = Color(0xFFFFFBF5),
                borderColor = Color(0xFFFED7AA).copy(alpha = 0.75f),
                textColor = CriticalOrangeDark,
                icon = Icons.Outlined.NotificationsActive,
                testTag = "op_card_critical",
                onClick = { onFilterSelectAndNavigate(ProductFilter.CRITICAL) }
            )

            // 🟡 Kart 3: Yakın Takip (3-7 Gün) - Pastel Krem/Sarı
            MorningOperationCard(
                modifier = Modifier.weight(1f),
                title = "Yakın Takip",
                varietyCount = soonVariety,
                totalStockCount = soonTotalStock,
                badgeColor = SoonYellowDark,
                bgColor = Color(0xFFFEFEF5),
                borderColor = Color(0xFFFDE68A).copy(alpha = 0.8f),
                textColor = Color(0xFF92400E),
                icon = Icons.Outlined.CalendarToday,
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
        shape = RoundedCornerShape(18.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 1.dp,
        modifier = modifier.testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Üst: İkon üstte, başlık altta (Ortalanmış simetrik Column yapısı)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = title,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    maxLines = 2,
                    lineHeight = 14.sp,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Alt: Kalın çeşit sayısı ve altında adet sayısı (Ortalanmış)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$varietyCount Çeşit",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = (-0.2).sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "$totalStockCount Adet",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
