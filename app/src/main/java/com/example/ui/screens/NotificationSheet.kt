package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ExpiryStatus
import com.example.data.Product
import com.example.ui.DashboardState
import com.example.ui.ProductFilter
import com.example.ui.theme.*
import com.example.worker.MorningCheckWorker

enum class NotificationCategoryFilter {
    ALL,
    CRITICAL_SKT,
    STOCK_DISCOUNT,
    SYSTEM_TASKS
}

@Composable
fun NotificationSheet(
    dashboardState: DashboardState,
    allProducts: List<Product>,
    morningReminderEnabled: Boolean,
    criticalAlertEnabled: Boolean,
    highStockAlertEnabled: Boolean,
    onDismiss: () -> Unit,
    onFilterSelected: (ProductFilter) -> Unit,
    onNavigate: (String) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onToggleMorningReminder: () -> Unit,
    onToggleCriticalAlert: () -> Unit,
    onToggleHighStockAlert: () -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(NotificationCategoryFilter.ALL) }
    var showSettingsSection by remember { mutableStateOf(false) }

    val expiredProducts = remember(allProducts) {
        allProducts.filter { it.sktTarihi > 0L && it.getExpiryStatus() == ExpiryStatus.EXPIRED }
    }
    val criticalProducts = remember(allProducts) {
        allProducts.filter { it.sktTarihi > 0L && it.getExpiryStatus() == ExpiryStatus.CRITICAL }
    }
    val highStockNearExpiry = remember(allProducts) {
        allProducts.filter { it.sktTarihi > 0L && it.getRemainingDays() in 1..30 && it.stokAdedi >= 10 }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .heightIn(max = 660.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // HEADER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(TurquoisePrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Bildirimler",
                                tint = TurquoiseDark,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Bildirim & Aksiyon Merkezi",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = Slate900
                                )
                                if (dashboardState.unreadNotificationCount > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(ExpiredRed)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${dashboardState.unreadNotificationCount} Yeni",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Reyon canlı SKT, stok riskleri ve görev uyarıları",
                                fontSize = 11.sp,
                                color = Slate500
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = Slate500
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // TOP ACTION TOOLBAR (Test Notification + Settings Toggle + Mark Read)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate100)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Test Push Notification Trigger
                    Surface(
                        onClick = {
                            try {
                                MorningCheckWorker.triggerTestNotificationNow(context)
                                Toast.makeText(context, "🔔 Test bildirimi cihaza gönderildi!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Bildirim gönderilemedi", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Test",
                                tint = TurquoiseDark,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Test Push Bildirimi",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            onClick = { showSettingsSection = !showSettingsSection },
                            shape = CircleShape,
                            color = if (showSettingsSection) TurquoisePrimary.copy(alpha = 0.2f) else Color.Transparent
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Bildirim Ayarları",
                                tint = TurquoiseDark,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(18.dp)
                            )
                        }

                        TextButton(
                            onClick = {
                                onMarkAllAsRead()
                                Toast.makeText(context, "Tüm bildirimler okundu işaretlendi", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Tümünü Okundu İşaretle",
                                fontSize = 11.sp,
                                color = TurquoiseDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // COLLAPSIBLE SETTINGS SECTION
                AnimatedVisibility(visible = showSettingsSection) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate50),
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "⚙️ BİLDİRİM VE UYARI AYARLARI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Slate700
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Sabah 08:30 Kontrol Turu Uyarısı", fontSize = 12.sp, color = Slate900)
                                Switch(
                                    checked = morningReminderEnabled,
                                    onCheckedChange = { onToggleMorningReminder() },
                                    modifier = Modifier.scale(0.8f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Kritik SKT Anlık Sesli Bildirimi", fontSize = 12.sp, color = Slate900)
                                Switch(
                                    checked = criticalAlertEnabled,
                                    onCheckedChange = { onToggleCriticalAlert() },
                                    modifier = Modifier.scale(0.8f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Yüksek Stok Risk Uyarısı", fontSize = 12.sp, color = Slate900)
                                Switch(
                                    checked = highStockAlertEnabled,
                                    onCheckedChange = { onToggleHighStockAlert() },
                                    modifier = Modifier.scale(0.8f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // CATEGORY FILTER CHIPS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NotificationCategoryChip(
                        title = "Tümü",
                        badgeCount = null,
                        isSelected = selectedCategory == NotificationCategoryFilter.ALL,
                        onClick = { selectedCategory = NotificationCategoryFilter.ALL }
                    )
                    NotificationCategoryChip(
                        title = "🚨 Acil SKT",
                        badgeCount = dashboardState.expiredCount + dashboardState.criticalCount,
                        isSelected = selectedCategory == NotificationCategoryFilter.CRITICAL_SKT,
                        onClick = { selectedCategory = NotificationCategoryFilter.CRITICAL_SKT }
                    )
                    NotificationCategoryChip(
                        title = "📦 Stok & İndirim",
                        badgeCount = dashboardState.importantCount,
                        isSelected = selectedCategory == NotificationCategoryFilter.STOCK_DISCOUNT,
                        onClick = { selectedCategory = NotificationCategoryFilter.STOCK_DISCOUNT }
                    )
                    NotificationCategoryChip(
                        title = "📋 Tur / Görev",
                        badgeCount = if (!dashboardState.isMorningTourCompletedToday) 1 else 0,
                        isSelected = selectedCategory == NotificationCategoryFilter.SYSTEM_TASKS,
                        onClick = { selectedCategory = NotificationCategoryFilter.SYSTEM_TASKS }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // NOTIFICATION CARDS LIST
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // EXPIRED ITEMS NOTIFICATION
                    if ((selectedCategory == NotificationCategoryFilter.ALL || selectedCategory == NotificationCategoryFilter.CRITICAL_SKT) && dashboardState.expiredCount > 0) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                border = BorderStroke(1.dp, ExpiredRed.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.ErrorOutline,
                                                contentDescription = "Acil",
                                                tint = ExpiredRed,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "🚨 ACİL: SKT'si Geçmiş ${dashboardState.expiredCount} Ürün Bulundu!",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp,
                                                color = ExpiredRed
                                            )
                                        }
                                        Text(
                                            text = "BUGÜN",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ExpiredRed.copy(alpha = 0.8f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Bu ürünlerin müşteriye sunulmaması için derhal raftan toplanıp imha/iade kaydı açılması gerekmektedir.",
                                        fontSize = 11.sp,
                                        color = Slate700,
                                        lineHeight = 15.sp
                                    )
                                    if (expiredProducts.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Örnek: ${expiredProducts.take(2).joinToString { it.urunAdi }}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Slate900,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Button(
                                            onClick = {
                                                onDismiss()
                                                onFilterSelected(ProductFilter.EXPIRED)
                                                onNavigate("products")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = ExpiredRed),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("ÜRÜNLERİ LİSTELE & RAFTAN AL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // CRITICAL ITEMS NOTIFICATION
                    if ((selectedCategory == NotificationCategoryFilter.ALL || selectedCategory == NotificationCategoryFilter.CRITICAL_SKT) && dashboardState.criticalCount > 0) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                                border = BorderStroke(1.dp, CriticalOrange.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = "Kritik",
                                                tint = CriticalOrange,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "⚠️ KRİTİK: ${dashboardState.criticalCount} Ürünün SKT'sine ≤ 7 Gün Kaldı!",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp,
                                                color = CriticalOrange
                                            )
                                        }
                                        Text(
                                            text = "BUGÜN",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CriticalOrange.copy(alpha = 0.8f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Erken satış için ön sıraya alma (FIFO), %30-%50 etiket indirimi veya reyon önü alanlara çıkarma önerilir.",
                                        fontSize = 11.sp,
                                        color = Slate700,
                                        lineHeight = 15.sp
                                    )
                                    if (criticalProducts.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Örnek: ${criticalProducts.take(2).joinToString { "${it.urunAdi} (${it.getRemainingDays()} gün)" }}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Slate900,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Button(
                                            onClick = {
                                                onDismiss()
                                                onFilterSelected(ProductFilter.CRITICAL)
                                                onNavigate("products")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = CriticalOrange),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("KRİTİK ÜRÜNLERİ İNCELE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // HIGH STOCK NEAR EXPIRY NOTIFICATION
                    if ((selectedCategory == NotificationCategoryFilter.ALL || selectedCategory == NotificationCategoryFilter.STOCK_DISCOUNT) && dashboardState.importantCount > 0) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                                border = BorderStroke(1.dp, Color(0xFFFFB300))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🔥 YÜKSEK STOK & SKT RİSKİ (${dashboardState.importantCount} Kalem)",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            color = Color(0xFFE65100)
                                        )
                                        Text(
                                            text = "BİLDİRİM",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE65100)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Yüksek adette stoğa sahip (≥10 adet) ve 30 günden az SKT süresi kalan ürünler için toplu indirim etiketi önerilir.",
                                        fontSize = 11.sp,
                                        color = Slate700,
                                        lineHeight = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Button(
                                            onClick = {
                                                onDismiss()
                                                onFilterSelected(ProductFilter.IMPORTANT)
                                                onNavigate("products")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("RİSKLİ STOKLARI FİLTRELE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // MORNING TOUR TASK NOTIFICATION
                    if (selectedCategory == NotificationCategoryFilter.ALL || selectedCategory == NotificationCategoryFilter.SYSTEM_TASKS) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (dashboardState.isMorningTourCompletedToday) Color(0xFFE8F5E9) else Color(0xFFE2FAFB)
                                ),
                                border = BorderStroke(1.dp, if (dashboardState.isMorningTourCompletedToday) NormalGreen.copy(alpha = 0.4f) else TurquoisePrimary.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (dashboardState.isMorningTourCompletedToday) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = if (dashboardState.isMorningTourCompletedToday) NormalGreen else TurquoisePrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (dashboardState.isMorningTourCompletedToday) "✅ GÜNLÜK SABAH TURU TAMAMLANDI" else "⚡ BUGÜNKÜ SABAH SAYIM TURU BEKLİYOR",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.sp,
                                                color = if (dashboardState.isMorningTourCompletedToday) NormalGreen else TurquoiseDark
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (dashboardState.isMorningTourCompletedToday)
                                            "Tebrikler! Bugünkü sabah reyon kontrol raporunuz başarıyla sisteme kaydedildi."
                                        else
                                            "Kamera, OCR tarih okuyucu ve barkod ile 2 dakikada reyon kontrolünü yapıp gününüzü tamamlayın.",
                                        fontSize = 11.sp,
                                        color = Slate700
                                    )
                                    if (!dashboardState.isMorningTourCompletedToday) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Button(
                                                onClick = {
                                                    onDismiss()
                                                    onNavigate("game")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("SAYIM TURUNA BAŞLA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // SYSTEM BACKUP & ROOM DB INFO
                    if (selectedCategory == NotificationCategoryFilter.ALL || selectedCategory == NotificationCategoryFilter.SYSTEM_TASKS) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Slate50),
                                border = BorderStroke(1.dp, Slate200)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = Slate700,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "💾 Veritabanı Durumu: Çevrimdışı Room DB Aktif",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900
                                        )
                                        Text(
                                            text = "Toplam ${dashboardState.totalCount} ürün yerel bellekte güvenle saklanıyor.",
                                            fontSize = 11.sp,
                                            color = Slate500
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // FOOTER
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "TAMAM / ANLADIM",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCategoryChip(
    title: String,
    badgeCount: Int?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) TurquoisePrimary else Slate100,
        border = BorderStroke(1.dp, if (isSelected) TurquoiseDark else Slate200)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Slate700,
                maxLines = 1,
                softWrap = false
            )
            if (badgeCount != null && badgeCount > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else ExpiredRed)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "$badgeCount",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isSelected) TurquoiseDark else Color.White
                    )
                }
            }
        }
    }
}
