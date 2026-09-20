package com.example.ui.components.detail

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.CriticalOrangeBorder
import com.example.ui.theme.CriticalOrangeContainer
import com.example.ui.theme.CriticalOrangeDark
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.ExpiredRedBorder
import com.example.ui.theme.ExpiredRedContainer
import com.example.ui.theme.ExpiredRedDark
import com.example.ui.theme.NormalGreen
import com.example.ui.theme.NormalGreenBorder
import com.example.ui.theme.NormalGreenContainer
import com.example.ui.theme.NormalGreenDark
import com.example.ui.theme.Slate500
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SktRiskFilter {
    ALL,
    EXPIRED_NEAR,
    CRITICAL,
    SAFE
}

/**
 * TAB 1: SKT BATCHES & STOCK LIST WITH INTERACTIVE RISK MATRIX
 */
@Composable
fun ProductDetailSktTabContent(
    product: Product,
    matchingProducts: List<Product>,
    onAddNewSktClick: () -> Unit,
    onEditSktItem: (Product) -> Unit,
    onDeleteSkt: (Product) -> Unit,
    onBatchClick: (Product) -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr-TR"))
    val allValidSktProducts = remember(matchingProducts) {
        matchingProducts.filter { it.sktTarihi > 0L }.sortedBy { it.sktTarihi }
    }

    var selectedRiskFilter by remember { mutableStateOf(SktRiskFilter.ALL) }

    val expiredOrNearCount = remember(allValidSktProducts) {
        allValidSktProducts.filter { it.getRemainingDays() <= 3 }.sumOf { maxOf(0, it.stokAdedi) }
    }

    val criticalCount = remember(allValidSktProducts) {
        allValidSktProducts.filter { it.getRemainingDays() in 4..15 }.sumOf { maxOf(0, it.stokAdedi) }
    }

    val safeCount = remember(allValidSktProducts) {
        allValidSktProducts.filter { it.getRemainingDays() >= 16 }.sumOf { maxOf(0, it.stokAdedi) }
    }

    val displayedSktProducts = remember(allValidSktProducts, selectedRiskFilter) {
        when (selectedRiskFilter) {
            SktRiskFilter.ALL -> allValidSktProducts
            SktRiskFilter.EXPIRED_NEAR -> allValidSktProducts.filter { it.getRemainingDays() <= 3 }
            SktRiskFilter.CRITICAL -> allValidSktProducts.filter { it.getRemainingDays() in 4..15 }
            SktRiskFilter.SAFE -> allValidSktProducts.filter { it.getRemainingDays() >= 16 }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. TIKLANABİLİR SKT RİSK ÖZETİ MATRİSİ (3 Kart)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // RED: EXPIRED / NEAR (0-3 GÜN)
            ClickableRiskCard(
                modifier = Modifier.weight(1f),
                title = "0-3 Gün",
                count = expiredOrNearCount,
                accentColor = ExpiredRed,
                containerColor = ExpiredRedContainer,
                borderColor = ExpiredRedBorder,
                isSelected = selectedRiskFilter == SktRiskFilter.EXPIRED_NEAR,
                onClick = {
                    selectedRiskFilter = if (selectedRiskFilter == SktRiskFilter.EXPIRED_NEAR) SktRiskFilter.ALL else SktRiskFilter.EXPIRED_NEAR
                }
            )

            // ORANGE: CRITICAL (4-15 GÜN)
            ClickableRiskCard(
                modifier = Modifier.weight(1f),
                title = "4-15 Gün",
                count = criticalCount,
                accentColor = CriticalOrange,
                containerColor = CriticalOrangeContainer,
                borderColor = CriticalOrangeBorder,
                isSelected = selectedRiskFilter == SktRiskFilter.CRITICAL,
                onClick = {
                    selectedRiskFilter = if (selectedRiskFilter == SktRiskFilter.CRITICAL) SktRiskFilter.ALL else SktRiskFilter.CRITICAL
                }
            )

            // GREEN: SAFE (16+ GÜN)
            ClickableRiskCard(
                modifier = Modifier.weight(1f),
                title = "16+ Gün",
                count = safeCount,
                accentColor = NormalGreen,
                containerColor = NormalGreenContainer,
                borderColor = NormalGreenBorder,
                isSelected = selectedRiskFilter == SktRiskFilter.SAFE,
                onClick = {
                    selectedRiskFilter = if (selectedRiskFilter == SktRiskFilter.SAFE) SktRiskFilter.ALL else SktRiskFilter.SAFE
                }
            )
        }

        // 2. YENİ SKT EKLE BUTONU
        Button(
            onClick = onAddNewSktClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("detail_add_new_skt_button"),
            colors = ButtonDefaults.buttonColors(containerColor = TurquoiseDark),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "SKT Ekle",
                tint = Color.White,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "SKT Ekle",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 13.sp
            )
        }

        // 3. LİSTE BAŞLIĞI & AKTİF FİLTRE ROZETİ
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Kayıtlı Partiler",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (selectedRiskFilter != SktRiskFilter.ALL) {
                    Spacer(modifier = Modifier.width(6.dp))
                    val clearFilterShape = RoundedCornerShape(6.dp)
                    Box(
                        modifier = Modifier
                            .clip(clearFilterShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)), clearFilterShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true)
                            ) { selectedRiskFilter = SktRiskFilter.ALL }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (selectedRiskFilter) {
                                    SktRiskFilter.EXPIRED_NEAR -> "0-3 Gün Filtreli"
                                    SktRiskFilter.CRITICAL -> "4-15 Gün Filtreli"
                                    SktRiskFilter.SAFE -> "16+ Gün Filtreli"
                                    else -> ""
                                },
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Filtreyi Temizle",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = "${displayedSktProducts.size} / ${allValidSktProducts.size} Parti",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TurquoiseDark
            )
        }

        if (displayedSktProducts.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📅", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (selectedRiskFilter != SktRiskFilter.ALL) {
                            "Bu filtreye uygun SKT partisi bulunmuyor."
                        } else {
                            "Henüz SKT girilmemiş."
                        },
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (selectedRiskFilter != SktRiskFilter.ALL) {
                            "Tüm partileri listelemek için filtreyi kaldırın."
                        } else {
                            "Yeni SKT eklemek için butona dokunun."
                        },
                        fontSize = 11.sp,
                        color = Slate500,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                displayedSktProducts.forEach { item ->
                    val daysRemaining = item.getRemainingDays()
                    val dateStr = dateFormat.format(Date(item.sktTarihi))

                    val (badgeText, badgeBg, badgeTextColor) = when {
                        daysRemaining < 0 -> Triple("${kotlin.math.abs(daysRemaining)} gün geçti", ExpiredRedContainer, ExpiredRedDark)
                        daysRemaining == 0L -> Triple("Son gün", ExpiredRedContainer, ExpiredRedDark)
                        daysRemaining in 1..3 -> Triple("$daysRemaining gün kaldı", ExpiredRedContainer, ExpiredRedDark)
                        daysRemaining in 4..15 -> Triple("$daysRemaining gün kaldı", CriticalOrangeContainer, CriticalOrangeDark)
                        else -> Triple("$daysRemaining gün kaldı", NormalGreenContainer, NormalGreenDark)
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        shadowElevation = 1.5.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = TurquoiseDark)
                            ) { onBatchClick(item) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = TurquoisePrimary.copy(alpha = 0.12f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = TurquoiseDark,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = dateStr,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = badgeBg
                                        ) {
                                            Text(
                                                text = badgeText,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = badgeTextColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Miktar: ${item.stokAdedi} adet",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TurquoiseDark
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onEditSktItem(item) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "SKT Düzenle",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteSkt(item) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "SKT Sil",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClickableRiskCard(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    accentColor: Color,
    containerColor: Color,
    borderColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(cardShape)
            .background(if (isSelected) containerColor else containerColor.copy(alpha = 0.65f))
            .border(BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) accentColor else borderColor), cardShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = accentColor)
            ) { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 9.5.sp,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                color = accentColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$count Adet",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Black,
                color = accentColor,
                textAlign = TextAlign.Center
            )
        }
    }
}
