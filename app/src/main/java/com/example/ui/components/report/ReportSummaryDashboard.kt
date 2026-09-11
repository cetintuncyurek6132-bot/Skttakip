package com.example.ui.components.report

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TealPrimary = Color(0xFF0D9488)
private val TealLightBg = Color(0xFFF0FDFA)
private val RedCritical = Color(0xFFDC2626)
private val RedLightBg = Color(0xFFFEF2F2)
private val OrangeWarning = Color(0xFFEA580C)
private val OrangeLightBg = Color(0xFFFFF7ED)
private val BlueDate = Color(0xFF2563EB)
private val BlueLightBg = Color(0xFFEFF6FF)
private val TextPrimary = Color(0xFF0F172A)
private val TextSecondary = Color(0xFF475569)
private val TextMuted = Color(0xFF64748B)

@Composable
fun CleanSummaryMiniCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconTint: Color,
    label: String,
    value: String,
    unit: String,
    valueColor: Color,
    isDate: Boolean = false
) {
    Surface(
        modifier = modifier.height(68.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, iconTint.copy(alpha = 0.25f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // İkon Kutucuğu
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg)
                    .border(1.dp, iconTint.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(19.dp)
                )
            }

            // Metin Alanı
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = value,
                        fontSize = if (isDate) 12.sp else 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = valueColor,
                        maxLines = 1
                    )
                    Text(
                        text = unit,
                        fontSize = if (isDate) 10.5.sp else 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun ReportSummaryDashboard(
    selectedCount: Int,
    criticalStockCount: Int,
    criticalVariantCount: Int,
    dateStr: String,
    timeStr: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Satır: Seçilen Ürün ve Kritik Stok
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. SEÇİLEN ÜRÜN
            CleanSummaryMiniCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Inventory2,
                iconBg = TealLightBg,
                iconTint = TealPrimary,
                label = "SEÇİLEN ÜRÜN",
                value = "$selectedCount",
                unit = "Çeşit",
                valueColor = TealPrimary
            )

            // 2. KRİTİK STOK ADETİ
            CleanSummaryMiniCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Layers,
                iconBg = RedLightBg,
                iconTint = RedCritical,
                label = "KRİTİK STOK",
                value = "$criticalStockCount",
                unit = "Adet",
                valueColor = RedCritical
            )
        }

        // 2. Satır: Kritik Çeşit ve Takip Tarihi
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 3. KRİTİK ÇEŞİT
            CleanSummaryMiniCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Warning,
                iconBg = OrangeLightBg,
                iconTint = OrangeWarning,
                label = "KRİTİK ÇEŞİT",
                value = "$criticalVariantCount",
                unit = "Ürün",
                valueColor = if (criticalVariantCount > 0) OrangeWarning else TextPrimary
            )

            // 4. TAKİP TARİHİ
            CleanSummaryMiniCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Event,
                iconBg = BlueLightBg,
                iconTint = BlueDate,
                label = "TAKİP TARİHİ",
                value = dateStr,
                unit = timeStr,
                valueColor = TextPrimary,
                isDate = true
            )
        }
    }
}
