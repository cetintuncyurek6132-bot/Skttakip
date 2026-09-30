package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ExpiryStatus
import com.example.data.Product
import com.example.ui.DashboardState
import com.example.ui.ProductFilter
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.CriticalOrangeBorder
import com.example.ui.theme.CriticalOrangeContainer
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.ExpiredRedBorder
import com.example.ui.theme.ExpiredRedContainer
import com.example.ui.theme.NormalGreen
import com.example.ui.theme.NormalGreenContainer
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.SoonYellow
import com.example.ui.theme.SoonYellowBorder
import com.example.ui.theme.SoonYellowContainer
import com.example.ui.theme.SoonYellowDark
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary

@Composable
fun NotificationSheet(
    dashboardState: DashboardState,
    allProducts: List<Product>,
    morningReminderEnabled: Boolean = true,
    criticalAlertEnabled: Boolean = true,
    highStockAlertEnabled: Boolean = true,
    onDismiss: () -> Unit,
    onFilterSelected: (ProductFilter) -> Unit,
    onNavigate: (String) -> Unit = {},
    onMarkAllAsRead: () -> Unit = {},
    onToggleMorningReminder: () -> Unit = {},
    onToggleCriticalAlert: () -> Unit = {},
    onToggleHighStockAlert: () -> Unit = {},
    onRemoveFromShelf: (Product) -> Unit = {},
    onRemoveMultipleFromShelf: (List<Product>) -> Unit = {},
    onAddToAdetsel: (Product) -> Unit = {},
    onOpenProductDetail: (Product) -> Unit = {}
) {
    // Sayısal Özet Hesaplamaları
    val expiredCount = remember(allProducts, dashboardState) {
        val countFromList = allProducts.count {
            it.sktTarihi > 0L && it.stokAdedi > 0 && it.getExpiryStatus() == ExpiryStatus.EXPIRED
        }
        if (countFromList > 0) countFromList else dashboardState.expiredCount
    }

    val criticalCount = remember(allProducts, dashboardState) {
        val countFromList = allProducts.count {
            it.sktTarihi > 0L && it.stokAdedi > 0 && it.getExpiryStatus() == ExpiryStatus.CRITICAL
        }
        if (countFromList > 0) countFromList else dashboardState.criticalCount
    }

    val soonCount = remember(allProducts) {
        allProducts.count {
            it.sktTarihi > 0L && it.stokAdedi > 0 && it.getExpiryStatus() == ExpiryStatus.SOON
        }
    }

    val highStockCount = remember(allProducts) {
        allProducts.filter {
            it.sktTarihi > 0L && it.stokAdedi >= 10 && it.getRemainingDays() in 0..30
        }.distinctBy {
            it.barkod.ifBlank { it.urunKodu }.ifBlank { it.urunAdi }
        }.size
    }

    val totalAlertCount = expiredCount + criticalCount + soonCount + highStockCount

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .heightIn(max = 620.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // BAŞLIK ALANI (Sade ve Şık)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = TurquoisePrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Bildirim Özeti",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (totalAlertCount > 0) "$totalAlertCount aktif durum bildirimi" else "Tüm durumlar güncel",
                                fontSize = 11.5.sp,
                                color = Slate500
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("notification_sheet_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = Slate500
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // İÇERİK: SAYISAL ÖZET KARTLARI
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (totalAlertCount == 0) {
                        // HİÇBİR UYARI YOKSA TEMİZ DURUM KARTI
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = NormalGreenContainer.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, NormalGreen.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = NormalGreen.copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = NormalGreen,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Tüm ürünlerin durumu iyi, kritik SKT bulunmuyor.",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Reyonlarınız güvende ve kontroller tamamlandı.",
                                        fontSize = 11.5.sp,
                                        color = Slate700
                                    )
                                }
                            }
                        }
                    } else {
                        // 1. GÜNÜ GEÇENLER (Kırmızı Kart)
                        if (expiredCount > 0) {
                            NotificationSummaryCard(
                                title = "Günü Geçenler",
                                message = "Bugün $expiredCount adet ürünün süresi doldu.",
                                icon = Icons.Default.EventBusy,
                                badgeText = "$expiredCount Adet",
                                containerColor = ExpiredRedContainer,
                                borderColor = ExpiredRedBorder,
                                badgeBgColor = ExpiredRed,
                                badgeTextColor = Color.White,
                                iconColor = ExpiredRed,
                                titleColor = Color(0xFF991B1B),
                                onClick = {
                                    onNavigate("products")
                                    onFilterSelected(ProductFilter.EXPIRED)
                                    onDismiss()
                                },
                                testTag = "notification_summary_expired"
                            )
                        }

                        // 2. KRİTİK YAKLAŞANLAR (Turuncu Kart)
                        if (criticalCount > 0) {
                            NotificationSummaryCard(
                                title = "Kritik Yaklaşanlar",
                                message = "$criticalCount adet ürün için son 7 gün!",
                                icon = Icons.Default.WarningAmber,
                                badgeText = "$criticalCount Adet",
                                containerColor = CriticalOrangeContainer,
                                borderColor = CriticalOrangeBorder,
                                badgeBgColor = CriticalOrange,
                                badgeTextColor = Color.White,
                                iconColor = CriticalOrange,
                                titleColor = Color(0xFF9A3412),
                                onClick = {
                                    onNavigate("products")
                                    onFilterSelected(ProductFilter.CRITICAL)
                                    onDismiss()
                                },
                                testTag = "notification_summary_critical"
                            )
                        }

                        // 3. YAKLAŞANLAR / ORTA VADE (Sarı Kart)
                        if (soonCount > 0) {
                            NotificationSummaryCard(
                                title = "Yaklaşanlar",
                                message = "$soonCount adet ürünün SKT tarihi yaklaşıyor (8-30 gün).",
                                icon = Icons.Default.Schedule,
                                badgeText = "$soonCount Adet",
                                containerColor = SoonYellowContainer,
                                borderColor = SoonYellowBorder,
                                badgeBgColor = SoonYellow,
                                badgeTextColor = Color.White,
                                iconColor = SoonYellowDark,
                                titleColor = Color(0xFF854D0E),
                                onClick = {
                                    onNavigate("products")
                                    onFilterSelected(ProductFilter.SOON)
                                    onDismiss()
                                },
                                testTag = "notification_summary_soon"
                            )
                        }

                        // 4. YÜKSEK ADETLİLER (Mavi/Mor Kart)
                        if (highStockCount > 0) {
                            NotificationSummaryCard(
                                title = "Yüksek Stok Riski",
                                message = "$highStockCount kalem üründe yüksek stoklu parti bulunuyor.",
                                icon = Icons.Default.Inventory2,
                                badgeText = "$highStockCount Kalem",
                                containerColor = Color(0xFFEFF6FF),
                                borderColor = Color(0xFFBFDBFE),
                                badgeBgColor = Color(0xFF2563EB),
                                badgeTextColor = Color.White,
                                iconColor = Color(0xFF2563EB),
                                titleColor = Color(0xFF1E40AF),
                                onClick = {
                                    onNavigate("products")
                                    onFilterSelected(ProductFilter.CRITICAL)
                                    onDismiss()
                                },
                                testTag = "notification_summary_high_stock"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ALT BUTON (Sadece "Kapat")
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("notification_sheet_bottom_close_button")
                ) {
                    Text(
                        text = "Kapat",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationSummaryCard(
    title: String,
    message: String,
    icon: ImageVector,
    badgeText: String,
    containerColor: Color,
    borderColor: Color,
    badgeBgColor: Color,
    badgeTextColor: Color,
    iconColor: Color,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
    testTag: String
) {
    val cardShape = RoundedCornerShape(14.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .testTag(testTag),
        shape = cardShape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = titleColor
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeBgColor
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = badgeTextColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = message,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1E293B),
                    lineHeight = 16.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Filtrele",
                tint = Slate500,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
