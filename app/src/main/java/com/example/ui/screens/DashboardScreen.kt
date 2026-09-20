package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AssignmentLate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.UserManager
import com.example.data.DepoIadeManager
import com.example.data.IadeDurumu
import com.example.data.Product
import com.example.data.getDisplayName
import com.example.ui.DashboardState
import com.example.ui.ProductFilter
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.CriticalOrangeBorder
import com.example.ui.theme.CriticalOrangeDark
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.ExpiredRedBorder
import com.example.ui.theme.ExpiredRedContainer
import com.example.ui.theme.ExpiredRedDark
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.SoonYellowBorder
import com.example.ui.theme.SoonYellowDark
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardState,
    products: List<Product> = emptyList(),
    pendingAdetselCount: Int = 0,
    onQuickActionClick: (String) -> Unit,
    onFilterSelectAndNavigate: (ProductFilter) -> Unit,
    onProductClick: (Product) -> Unit = {},
    onViewAllProductsClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val currentUser by UserManager.currentUser.collectAsState()

    val todayMidnight = remember {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    // 1. Acil Ürünler (daysLeft <= 2) - En acilden (küçük daysLeft) en uzağa sıralı
    val urgentProducts = remember(products, todayMidnight) {
        products
            .filter { it.sktTarihi > 0L && it.getRemainingDays(todayMidnight) <= 2 }
            .sortedWith(
                compareBy<Product> { it.getRemainingDays(todayMidnight) }
                    .thenBy { it.sktTarihi }
                    .thenBy { it.urunAdi }
            )
    }

    // 2. Sabah Operasyon Ürün Gruplamaları
    // 🔴 Kart 1: Raftan Çekilecekler (Tarihi Geçenler: daysLeft < 0)
    val expiredProducts = remember(products, todayMidnight) {
        products.filter { it.sktTarihi > 0L && it.getRemainingDays(todayMidnight) < 0 }
    }
    val expiredVariety = expiredProducts.size
    val expiredTotalStock = expiredProducts.sumOf { it.stokAdedi }

    // 🟠 Kart 2: Sıcak Satışa Al (Sarı Etiket / Son 1-2 Gün: daysLeft in 0..1)
    val criticalProducts = remember(products, todayMidnight) {
        products.filter { it.sktTarihi > 0L && it.getRemainingDays(todayMidnight) in 0..1 }
    }
    val criticalVariety = criticalProducts.size
    val criticalTotalStock = criticalProducts.sumOf { it.stokAdedi }

    // 🟡 Kart 3: Yakın Takip (3 - 7 Gün Kalanlar: daysLeft in 2..7)
    val soonProducts = remember(products, todayMidnight) {
        products.filter { it.sktTarihi > 0L && it.getRemainingDays(todayMidnight) in 2..7 }
    }
    val soonVariety = soonProducts.size
    val soonTotalStock = soonProducts.sumOf { it.stokAdedi }

    val hasExpired = expiredVariety > 0

    // 4. Depo İade & Eksik İrsaliye Durumu
    val depoRecords = remember(context) { DepoIadeManager.loadRecords(context) }
    val pendingDepoRecords = remember(depoRecords) {
        depoRecords.filter {
            it.durum == IadeDurumu.DEVAM_EDIYOR ||
            it.durum == IadeDurumu.REDDEDILDI ||
            it.irsaliyeGorselPath.isNullOrBlank()
        }
    }
    val pendingDepoCount = pendingDepoRecords.size

    // 5. Sabah Açılış Rutini (SharedPreferences tabanlı günlük 4 maddelik checklist)
    val routineItems = remember {
        listOf(
            "Süt & Şarküteri dolap SKT kontrolü yapıldı",
            "Sıcak satış / sarı etiketli ürünler öne çekildi (FIFO)",
            "Dünkü ambar teslim ve irsaliye tutanakları kontrol edildi",
            "Ekmek ve taze ürün iadeleri ayrıldı"
        )
    }

    val routinePrefs = remember(context) {
        context.getSharedPreferences("morning_routine_prefs", Context.MODE_PRIVATE)
    }

    val todayKey = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    var checkedStates by remember {
        val savedDate = routinePrefs.getString("last_routine_date", null)
        if (savedDate != todayKey) {
            routinePrefs.edit()
                .putString("last_routine_date", todayKey)
                .putBoolean("item_0", false)
                .putBoolean("item_1", false)
                .putBoolean("item_2", false)
                .putBoolean("item_3", false)
                .apply()
            mutableStateOf(listOf(false, false, false, false))
        } else {
            mutableStateOf(
                listOf(
                    routinePrefs.getBoolean("item_0", false),
                    routinePrefs.getBoolean("item_1", false),
                    routinePrefs.getBoolean("item_2", false),
                    routinePrefs.getBoolean("item_3", false)
                )
            )
        }
    }

    fun toggleRoutineItem(index: Int) {
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Exception) {}
        val updated = checkedStates.toMutableList()
        updated[index] = !updated[index]
        checkedStates = updated
        routinePrefs.edit().putBoolean("item_$index", updated[index]).apply()
    }

    // WhatsApp Sabah Raporu Paylaşımı
    fun shareMorningCockpitOnWhatsApp() {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR"))
        val sb = StringBuilder()
        sb.append("📋 *GÜNLÜK SABAH MAĞAZA AÇILIŞ VE KONTROL RAPORU*\n")
        sb.append("🗓 *Tarih:* ").append(dateFormat.format(Date())).append("\n")
        sb.append("👤 *Sorumlu:* ").append(currentUser?.fullName ?: "Mağaza Sorumlusu").append(" (").append(currentUser?.roleTitle ?: "MS").append(")\n\n")

        sb.append("🚨 *OPERASYONEL DURUM:*\n")
        if (hasExpired) {
            sb.append("🔴 *Raftan Çekilecekler (Tarihi Geçen):* $expiredVariety Çeşit • $expiredTotalStock Adet (Acil Müdahale!)\n")
        } else {
            sb.append("✅ *Raftan Çekilecek Ürün Yok* (Tarihi geçen ürün bulunmuyor)\n")
        }
        sb.append("🟠 *Sıcak Satış (Sarı Etiket / Son 1-2 Gün):* $criticalVariety Çeşit • $criticalTotalStock Adet\n")
        sb.append("🟡 *Yakın Takip (3-7 Gün Kalanlar):* $soonVariety Çeşit • $soonTotalStock Adet\n\n")

        sb.append("📦 *DEVREDEN & BEKLEYEN İŞLER:*\n")
        if (pendingAdetselCount > 0) {
            sb.append("⚠️ *Adetsel Sayım:* Dünden kalan $pendingAdetselCount kalem sayım tamamlanmayı bekliyor.\n")
        } else {
            sb.append("✅ *Adetsel Sayım:* Bekleyen sayım görevi yok, liste güncel.\n")
        }

        if (pendingDepoCount > 0) {
            sb.append("📦 *Depo İade / İrsaliye:* $pendingDepoCount adet iade işlemi onay bekliyor.\n")
        } else {
            sb.append("✅ *Depo İade:* Açık iade evrakı bulunmuyor.\n")
        }

        val completedRoutine = checkedStates.count { it }
        sb.append("\n📝 *AÇILIŞ RUTİNİ TAMAMLANMA:* $completedRoutine/4\n")
        routineItems.forEachIndexed { i, item ->
            val mark = if (checkedStates[i]) "✅" else "⬜"
            sb.append("$mark $item\n")
        }

        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, sb.toString())
                setPackage("com.whatsapp")
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, sb.toString())
            }
            context.startActivity(Intent.createChooser(shareIntent, "Sabah Raporunu Paylaş"))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("dashboard_screen_root"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // =====================================================================
        // 1. DİNAMİK VE OTOMATİK SOLA AKAN ACİL ÜRÜN VİTRİNİ (HORIZONTAL PAGER)
        // =====================================================================
        com.example.ui.screens.dashboard.UrgentProductsCarousel(
            urgentProducts = urgentProducts,
            todayMidnight = todayMidnight,
            onProductClick = onProductClick
        )

        // =====================================================================
        // 2. 3'LÜ SABAH OPERASYON KARTLARI (Büyük, Dokunmatik ve Net Rakamlar)
        // =====================================================================
        com.example.ui.screens.dashboard.MorningOperationCardsSection(
            expiredVariety = expiredVariety,
            expiredTotalStock = expiredTotalStock,
            criticalVariety = criticalVariety,
            criticalTotalStock = criticalTotalStock,
            soonVariety = soonVariety,
            soonTotalStock = soonTotalStock,
            onFilterSelectAndNavigate = onFilterSelectAndNavigate
        )

        // =====================================================================
        // 3. DÜNDEN DEVREDENLER & BEKLEYEN İŞLER (Adetsel Sayım)
        // =====================================================================
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, if (pendingAdetselCount > 0) Color(0xFFFDBA74) else Slate200),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
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
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (pendingAdetselCount > 0) Color(0xFFFFEDD5) else Color(0xFFDCFCE7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (pendingAdetselCount > 0) Icons.Default.AssignmentLate else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (pendingAdetselCount > 0) Color(0xFFEA580C) else EmeraldSuccess,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Adetsel Sayım Kontrolü",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = if (pendingAdetselCount > 0)
                            "Dünden kalan $pendingAdetselCount kalem sayım tamamlanmadı"
                        else
                            "Bekleyen sayım görevi yok, liste güncel.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (pendingAdetselCount > 0) Color(0xFFC2410C) else Slate500
                    )
                }

                if (pendingAdetselCount > 0) {
                    Button(
                        onClick = { onQuickActionClick("adetsel") },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Sayıma Git", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // =====================================================================
        // 4. DEPO İADE & EKSİK İRSALİYE ALARMI KARTI
        // =====================================================================
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, if (pendingDepoCount > 0) ExpiredRedBorder else Slate200),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
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
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (pendingDepoCount > 0) ExpiredRedContainer else Color(0xFFDCFCE7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (pendingDepoCount > 0) Icons.Default.LocalShipping else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (pendingDepoCount > 0) ExpiredRed else EmeraldSuccess,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Depo İade & Eksik İrsaliye Alarmı",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = if (pendingDepoCount > 0)
                            "$pendingDepoCount adet iade işlemi onay bekliyor (Depo Reddi / Eksik Belge)"
                        else
                            "Depoya sevk edilecek açık iade evrakı bulunmuyor.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (pendingDepoCount > 0) ExpiredRedDark else Slate500
                    )
                }

                if (pendingDepoCount > 0) {
                    Button(
                        onClick = { onQuickActionClick("takip") },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ExpiredRed),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Takip Sayfasına Git", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // =====================================================================
        // 5. SABAH AÇILIŞ RUTİNİ (GÜNLÜK HIZLI CHECKLIST)
        // =====================================================================
        com.example.ui.screens.dashboard.MorningOpeningRoutineCard(
            routineItems = routineItems,
            checkedStates = checkedStates,
            onToggleRoutineItem = { toggleRoutineItem(it) }
        )

        // =====================================================================
        // 6. WHATSAPP SABAH RAPORU & HIZLI KISAYOLLAR
        // =====================================================================
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // WhatsApp Vardiya Raporu Paylaş Butonu
            Surface(
                onClick = { shareMorningCockpitOnWhatsApp() },
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF0FDF4),
                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sabah Vardiya Raporunu WhatsApp'tan Paylaş",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D)
                    )
                }
            }

            // Hızlı Kısayol Butonları (Barkod Tara, Yeni Ürün)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = { onQuickActionClick("scan") },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 11.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = TurquoiseDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Barkod Tara",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate800
                        )
                    }
                }

                Surface(
                    onClick = { onQuickActionClick("add_product") },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 11.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = null,
                            tint = TurquoiseDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ürün / SKT Ekle",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate800
                        )
                    }
                }
            }
        }

        // 6. Tasarım ve Ergonomi: Yüzen alt bar için geniş boşluk
        Spacer(modifier = Modifier.height(100.dp))
    }
}
