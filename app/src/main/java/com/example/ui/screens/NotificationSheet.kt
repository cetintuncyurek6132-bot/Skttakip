package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ExpiryStatus
import com.example.data.Product
import com.example.ui.DashboardState
import com.example.ui.ProductFilter
import com.example.ui.screens.notification.*
import com.example.ui.theme.*

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
    onToggleHighStockAlert: () -> Unit,
    onRemoveFromShelf: (Product) -> Unit = {},
    onRemoveMultipleFromShelf: (List<Product>) -> Unit = {},
    onAddToAdetsel: (Product) -> Unit = {},
    onOpenProductDetail: (Product) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf(NotificationCategoryFilter.ALL) }
    var showSettingsSection by remember { mutableStateOf(false) }
    var showBulkRemoveDialog by remember { mutableStateOf<List<Product>?>(null) }

    val overdue2DaysProducts = remember(allProducts) {
        allProducts.filter { it.sktTarihi > 0L && it.stokAdedi > 0 && it.getRemainingDays() <= -2 }
    }
    val expiredProducts = remember(allProducts) {
        allProducts.filter { it.sktTarihi > 0L && it.stokAdedi > 0 && it.getExpiryStatus() == ExpiryStatus.EXPIRED }
    }
    val criticalProducts = remember(allProducts) {
        allProducts.filter { it.sktTarihi > 0L && it.stokAdedi > 0 && it.getExpiryStatus() == ExpiryStatus.CRITICAL }
    }
    val highStockNearExpiry = remember(allProducts) {
        allProducts.filter { it.sktTarihi > 0L && it.getRemainingDays() in 1..30 && it.stokAdedi >= 10 }
    }

    // Toplu Raftan Kaldırma Onay Penceresi
    if (showBulkRemoveDialog != null) {
        val targets = showBulkRemoveDialog!!
        AlertDialog(
            onDismissRequest = { showBulkRemoveDialog = null },
            title = {
                Text("Toplu Raftan Kaldır", fontWeight = FontWeight.Bold, color = Slate900)
            },
            text = {
                Text(
                    text = "${targets.size} adet ürünün reyon stoğu sıfırlanacak ve raftan kaldırıldı olarak işaretlenecektir. Onaylıyor musunuz?",
                    fontSize = 13.sp,
                    color = Slate700
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveMultipleFromShelf(targets)
                        showBulkRemoveDialog = null
                        Toast.makeText(context, "${targets.size} ürün başarıyla raftan kaldırıldı", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpiredRed)
                ) {
                    Text("Evet, Raftan Kaldır", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkRemoveDialog = null }) {
                    Text("İptal", color = Slate700)
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(TurquoisePrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Bildirimler",
                                tint = TurquoiseDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Bildirim & Hızlı Aksiyon",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
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
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Kritik SKT ve reyon uyarıları",
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

                Spacer(modifier = Modifier.height(8.dp))

                // AYARLAR & OKUNDU AKSİYON ÇUBUĞU
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showSettingsSection = !showSettingsSection },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Slate700,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showSettingsSection) "Ayarları Gizle" else "Uyarı Ayarları",
                            fontSize = 11.sp,
                            color = Slate700,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        com.example.worker.MorningCheckWorker.triggerTestNotificationDirectly(context)
                                        Toast.makeText(context, "🔔 Test bildirimi ve uyarı sesi gönderildi", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Bildirim hatası: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Sesi Test Et",
                                tint = Slate500,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        TextButton(
                            onClick = {
                                onMarkAllAsRead()
                                Toast.makeText(context, "Tüm bildirimler okundu", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Tümünü Oku",
                                fontSize = 10.5.sp,
                                color = TurquoiseDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // COLLAPSIBLE SETTINGS SECTION
                AnimatedVisibility(visible = showSettingsSection) {
                    NotificationSettingsCard(
                        morningReminderEnabled = morningReminderEnabled,
                        criticalAlertEnabled = criticalAlertEnabled,
                        highStockAlertEnabled = highStockAlertEnabled,
                        onToggleMorningReminder = onToggleMorningReminder,
                        onToggleCriticalAlert = onToggleCriticalAlert,
                        onToggleHighStockAlert = onToggleHighStockAlert
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                        title = "🔥 Adet ≥10",
                        badgeCount = dashboardState.importantCount,
                        isSelected = selectedCategory == NotificationCategoryFilter.STOCK_DISCOUNT,
                        onClick = { selectedCategory = NotificationCategoryFilter.STOCK_DISCOUNT }
                    )
                    NotificationCategoryChip(
                        title = "📋 Sistem & Bilgi",
                        badgeCount = null,
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
                    notificationCardsList(
                        selectedCategory = selectedCategory,
                        overdue2DaysProducts = overdue2DaysProducts,
                        expiredProducts = expiredProducts,
                        criticalProducts = criticalProducts,
                        highStockNearExpiry = highStockNearExpiry,
                        dashboardState = dashboardState,
                        onShowBulkRemoveDialog = { showBulkRemoveDialog = it },
                        onRemoveFromShelf = onRemoveFromShelf,
                        onAddToAdetsel = onAddToAdetsel,
                        onOpenProductDetail = onOpenProductDetail,
                        onFilterSelected = onFilterSelected,
                        onNavigate = onNavigate,
                        onDismiss = onDismiss
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

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
