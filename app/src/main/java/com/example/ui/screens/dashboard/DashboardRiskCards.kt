package com.example.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.CriticalOrangeDark
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.ExpiredRedDark
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900

/**
 * 4-CARD COMPACT INTERACTIVE RISK MATRIX
 * Serves as direct primary risk filter tabs.
 */
@Composable
fun DashboardRiskCards(
    expiredCount: Int,
    criticalCount: Int,
    soonCount: Int,
    importantCount: Int,
    selectedFilterTag: String,
    onCardClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1. REYONDAN KALDIR
        RiskSummaryTile(
            modifier = Modifier.weight(1f),
            title = "Kaldır",
            count = expiredCount,
            accentColor = ExpiredRed,
            accentDarkColor = ExpiredRedDark,
            containerColor = if (expiredCount > 0) Color(0xFFFEE2E2) else Color(0xFFF8FAFC),
            borderColor = if (selectedFilterTag == "EXPIRED") ExpiredRed else if (expiredCount > 0) ExpiredRed.copy(alpha = 0.35f) else Slate200,
            isSelected = selectedFilterTag == "EXPIRED",
            iconEmoji = "🚫",
            testTag = "dashboard_tile_expired",
            onClick = { onCardClick("EXPIRED") }
        )

        // 2. KRİTİK (1-3 GÜN)
        RiskSummaryTile(
            modifier = Modifier.weight(1f),
            title = "Kritik",
            count = criticalCount,
            accentColor = CriticalOrange,
            accentDarkColor = CriticalOrangeDark,
            containerColor = if (criticalCount > 0) Color(0xFFFFEDD5) else Color(0xFFF8FAFC),
            borderColor = if (selectedFilterTag == "CRITICAL") CriticalOrange else if (criticalCount > 0) CriticalOrange.copy(alpha = 0.35f) else Slate200,
            isSelected = selectedFilterTag == "CRITICAL",
            iconEmoji = "⚠️",
            testTag = "dashboard_tile_critical",
            onClick = { onCardClick("CRITICAL") }
        )

        // 3. YAKLAŞAN (4-7 GÜN)
        RiskSummaryTile(
            modifier = Modifier.weight(1f),
            title = "Yaklaşan",
            count = soonCount,
            accentColor = AmberWarning,
            accentDarkColor = Color(0xFFB45309),
            containerColor = if (soonCount > 0) Color(0xFFFEF9C3) else Color(0xFFF8FAFC),
            borderColor = if (selectedFilterTag == "SOON") AmberWarning else if (soonCount > 0) AmberWarning.copy(alpha = 0.35f) else Slate200,
            isSelected = selectedFilterTag == "SOON",
            iconEmoji = "⏱️",
            testTag = "dashboard_tile_soon",
            onClick = { onCardClick("SOON") }
        )

        // 4. ÖNEMLİ YÜKSEK ADET (>=10 ADET)
        RiskSummaryTile(
            modifier = Modifier.weight(1f),
            title = "Adet ≥10",
            count = importantCount,
            accentColor = IndigoAccent,
            accentDarkColor = Color(0xFF4338CA),
            containerColor = if (importantCount > 0) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
            borderColor = if (selectedFilterTag == "IMPORTANT") IndigoAccent else if (importantCount > 0) IndigoAccent.copy(alpha = 0.35f) else Slate200,
            isSelected = selectedFilterTag == "IMPORTANT",
            iconEmoji = "🔥",
            testTag = "dashboard_tile_important",
            onClick = { onCardClick("IMPORTANT") }
        )
    }
}

@Composable
private fun RiskSummaryTile(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    accentColor: Color,
    accentDarkColor: Color,
    containerColor: Color,
    borderColor: Color,
    isSelected: Boolean,
    iconEmoji: String,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) containerColor else Color.White
        ),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = iconEmoji, fontSize = 10.sp)
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                    color = if (isSelected) accentDarkColor else if (count > 0) Slate700 else Slate500
                )
            }

            Spacer(modifier = Modifier.height(1.dp))

            Text(
                text = "$count",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = if (count > 0) accentDarkColor else Slate500
            )
        }
    }
}
