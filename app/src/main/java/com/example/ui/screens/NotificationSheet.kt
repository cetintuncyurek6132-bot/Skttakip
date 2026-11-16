package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ExpiryStatus
import com.example.data.Product
import com.example.ui.DashboardState
import com.example.ui.ProductFilter
import com.example.ui.theme.ExpiredRedContainer
import com.example.ui.theme.ExpiredRedDark
import com.example.ui.theme.NormalGreen
import com.example.ui.theme.NormalGreenContainer
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.SoonYellowContainer
import com.example.ui.theme.SoonYellowDark
import com.example.ui.theme.TurquoiseDark

private data class NotificationCenterItem(
    val title: String,
    val description: String,
    val badgeText: String,
    val icon: ImageVector,
    val iconBgColor: Color,
    val iconTintColor: Color,
    val badgeBgColor: Color,
    val badgeTextColor: Color,
    val testTag: String,
    val onClick: () -> Unit
)

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
    val todayMidnight by com.example.util.rememberTodayMidnight()
    var isClearedLocally by remember { mutableStateOf(false) }

    val expiredCount = remember(allProducts, dashboardState, todayMidnight) {
        val countFromList = allProducts.count {
            it.sktTarihi > 0L && it.stokAdedi > 0 && it.getExpiryStatus(todayMidnight) == ExpiryStatus.EXPIRED
        }
        if (countFromList > 0) countFromList else dashboardState.expiredCount
    }

    val criticalCount = remember(allProducts, dashboardState, todayMidnight) {
        val countFromList = allProducts.count {
            it.sktTarihi > 0L && it.stokAdedi > 0 && it.getExpiryStatus(todayMidnight) == ExpiryStatus.CRITICAL
        }
        if (countFromList > 0) countFromList else dashboardState.criticalCount
    }

    val soonCount = remember(allProducts, todayMidnight) {
        allProducts.count {
            it.sktTarihi > 0L && it.stokAdedi > 0 && it.getExpiryStatus(todayMidnight) == ExpiryStatus.SOON
        }
    }

    val highStockCount = remember(allProducts, todayMidnight) {
        allProducts.filter {
            it.sktTarihi > 0L && it.stokAdedi >= 10 && it.getRemainingDays(todayMidnight) in 0..30
        }.distinctBy {
            it.barkod.ifBlank { it.urunKodu }.ifBlank { it.urunAdi }
        }.size
    }

    val items = remember(
        expiredCount,
        criticalCount,
        soonCount,
        highStockCount,
        isClearedLocally
    ) {
        if (isClearedLocally) {
            emptyList()
        } else {
            buildList {
                if (expiredCount > 0) {
                    add(
                        NotificationCenterItem(
                            title = "Günü Geçenler",
                            description = "Bugün $expiredCount adet ürünün süresi doldu.",
                            badgeText = "$expiredCount Adet",
                            icon = Icons.Default.EventBusy,
                            iconBgColor = ExpiredRedContainer,
                            iconTintColor = ExpiredRedDark,
                            badgeBgColor = ExpiredRedContainer,
                            badgeTextColor = ExpiredRedDark,
                            testTag = "notification_summary_expired",
                            onClick = {
                                onNavigate("products")
                                onFilterSelected(ProductFilter.EXPIRED)
                                onDismiss()
                            }
                        )
                    )
                }
                if (criticalCount > 0) {
                    add(
                        NotificationCenterItem(
                            title = "Kritik Yaklaşanlar",
                            description = "$criticalCount adet ürün için son 7 gün!",
                            badgeText = "$criticalCount Adet",
                            icon = Icons.Default.WarningAmber,
                            iconBgColor = ExpiredRedContainer,
                            iconTintColor = ExpiredRedDark,
                            badgeBgColor = ExpiredRedContainer,
                            badgeTextColor = ExpiredRedDark,
                            testTag = "notification_summary_critical",
                            onClick = {
                                onNavigate("products")
                                onFilterSelected(ProductFilter.CRITICAL)
                                onDismiss()
                            }
                        )
                    )
                }
                if (soonCount > 0) {
                    add(
                        NotificationCenterItem(
                            title = "Yaklaşanlar",
                            description = "$soonCount adet ürünün SKT tarihi yaklaşıyor (8-30 gün).",
                            badgeText = "$soonCount Adet",
                            icon = Icons.Default.Schedule,
                            iconBgColor = SoonYellowContainer,
                            iconTintColor = SoonYellowDark,
                            badgeBgColor = SoonYellowContainer,
                            badgeTextColor = SoonYellowDark,
                            testTag = "notification_summary_soon",
                            onClick = {
                                onNavigate("products")
                                onFilterSelected(ProductFilter.SOON)
                                onDismiss()
                            }
                        )
                    )
                }
                if (highStockCount > 0) {
                    add(
                        NotificationCenterItem(
                            title = "Yüksek Stok Riski",
                            description = "$highStockCount kalem üründe yüksek stoklu parti bulunuyor.",
                            badgeText = "$highStockCount Kalem",
                            icon = Icons.Default.Inventory2,
                            iconBgColor = Color(0xFFEFF6FF),
                            iconTintColor = Color(0xFF1D4ED8),
                            badgeBgColor = Color(0xFFEFF6FF),
                            badgeTextColor = Color(0xFF1D4ED8),
                            testTag = "notification_summary_high_stock",
                            onClick = {
                                onNavigate("products")
                                onFilterSelected(ProductFilter.CRITICAL)
                                onDismiss()
                            }
                        )
                    )
                }
            }
        }
    }

    val activeStatusCount = if (isClearedLocally) {
        0
    } else {
        expiredCount + criticalCount + soonCount + highStockCount
    }

    LaunchedEffect(Unit) {
        onMarkAllAsRead()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 460.dp)
                .wrapContentHeight()
                .heightIn(max = 560.dp)
                .testTag("notification_center_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Slate200),
            shadowElevation = 10.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 1. BAŞLIK ALANI
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Bildirim Merkezi",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$activeStatusCount aktif durum",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate500,
                            maxLines = 1
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (items.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    isClearedLocally = true
                                    onMarkAllAsRead()
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("notification_center_clear_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Tümünü Okundu İşaretle",
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Temizle",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TurquoiseDark
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("notification_sheet_close_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Slate50),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Kapat",
                                    tint = Slate500,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color = Slate200.copy(alpha = 0.8f)
                )

                // 2. İÇERİK ALANI
                if (items.isEmpty()) {
                    // BOŞ DURUM TASARIMI
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 38.dp)
                            .testTag("notification_center_empty_state"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = NormalGreenContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = NormalGreen,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Text(
                            text = "Her şey yolunda, aktif bildirim yok.",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate800,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 4.dp)
                    ) {
                        items.forEachIndexed { index, item ->
                            NotificationCenterRow(item = item)
                            if (index < items.lastIndex) {
                                HorizontalDivider(
                                    thickness = 0.8.dp,
                                    color = Slate200.copy(alpha = 0.75f),
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCenterRow(
    item: NotificationCenterItem
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = item.onClick
            )
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag(item.testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        // Sol Pastel Yuvarlak İkon Kutusu
        Surface(
            shape = CircleShape,
            color = item.iconBgColor,
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.iconTintColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Orta: Başlık & Açıklama
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.description,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Slate500,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Sağ: Sayı Rozeti + Sağa Ok (Chevron)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = item.badgeBgColor
            ) {
                Text(
                    text = item.badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = item.badgeTextColor,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Filtreye Git",
                tint = Slate500,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
