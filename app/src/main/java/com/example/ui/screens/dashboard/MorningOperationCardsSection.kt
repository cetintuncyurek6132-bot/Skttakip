package com.example.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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

@Composable
fun MorningOperationCardsSection(
    expiredVariety: Int,
    expiredTotalStock: Int,
    criticalVariety: Int,
    criticalTotalStock: Int,
    soonVariety: Int,
    soonTotalStock: Int,
    onFilterSelectAndNavigate: (ProductFilter) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Sabah Operasyon Görevleri",
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Bold,
            color = Slate900
        )

        // 🔴 Kart 1: Raftan Çekilecekler (Tarihi Geçen)
        MorningOperationCard(
            title = "Raftan Çekilecekler (Tarihi Geçen)",
            varietyCount = expiredVariety,
            totalStockCount = expiredTotalStock,
            description = "Raftan toplayıp iade veya fireye ayırın.",
            badgeColor = ExpiredRed,
            bgColor = Color(0xFFFFF5F5),
            borderColor = ExpiredRedBorder,
            textColor = ExpiredRedDark,
            icon = Icons.Default.Warning,
            testTag = "op_card_expired",
            onClick = { onFilterSelectAndNavigate(ProductFilter.EXPIRED) }
        )

        // 🟠 Kart 2: Sıcak Satışa Al (Sarı Etiket / Son 1-2 Gün)
        MorningOperationCard(
            title = "Sıcak Satışa Al (Sarı Etiket / Son 1-2 Gün)",
            varietyCount = criticalVariety,
            totalStockCount = criticalTotalStock,
            description = "Kasa önüne veya reyon önüne çekip indirim uygulayın.",
            badgeColor = CriticalOrange,
            bgColor = Color(0xFFFFF9F2),
            borderColor = CriticalOrangeBorder,
            textColor = CriticalOrangeDark,
            icon = Icons.Default.NotificationsActive,
            testTag = "op_card_critical",
            onClick = { onFilterSelectAndNavigate(ProductFilter.CRITICAL) }
        )

        // 🟡 Kart 3: Yakın Takip (3 - 7 Gün Kalanlar)
        MorningOperationCard(
            title = "Yakın Takip (3 - 7 Gün Kalanlar)",
            varietyCount = soonVariety,
            totalStockCount = soonTotalStock,
            description = "Haftalık tüketim ve sipariş planlamasına alın.",
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

@Composable
fun MorningOperationCard(
    title: String,
    varietyCount: Int,
    totalStockCount: Int,
    description: String,
    badgeColor: Color,
    bgColor: Color,
    borderColor: Color,
    textColor: Color,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    text = "$varietyCount Çeşit • $totalStockCount Adet",
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = description,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = Slate600,
                    lineHeight = 14.5.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.85f))
                    .border(1.dp, borderColor.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "İncele",
                    tint = textColor,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
