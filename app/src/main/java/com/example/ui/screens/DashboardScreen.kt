package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentLate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.UserManager
import com.example.data.DepoIadeManager
import com.example.data.IadeDurumu
import com.example.data.Product
import com.example.ui.DashboardState
import com.example.ui.ProductFilter
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.ExpiredRedBorder
import com.example.ui.theme.ExpiredRedContainer
import com.example.ui.theme.ExpiredRedDark
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate900
import java.text.SimpleDateFormat
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

    val todayMidnight by com.example.util.rememberTodayMidnight()

    // 1. Acil Ürünler (daysLeft in 0..7) - En acilden (küçük daysLeft) en uzağa sıralı
    val urgentProducts = remember(products, todayMidnight) {
        products
            .filter { it.sktTarihi > 0L && it.stokAdedi > 0 && it.getRemainingDays(todayMidnight) in 0..7 }
            .sortedWith(
                compareBy<Product> { it.getRemainingDays(todayMidnight) }
                    .thenBy { it.sktTarihi }
                    .thenBy { it.urunAdi }
            )
    }

    // 2. Sabah Operasyon Ürün Gruplamaları
    // 🔴 Kart 1: Raftan Çekilecekler (Tarihi Geçenler: daysLeft < 0)
    val expiredProducts = remember(products, todayMidnight) {
        products.filter { it.sktTarihi > 0L && it.stokAdedi > 0 && it.getRemainingDays(todayMidnight) < 0 }
    }
    val expiredVariety = expiredProducts.size
    val expiredTotalStock = expiredProducts.sumOf { it.stokAdedi }

    // 🟠 Kart 2: Sıcak Satışa Al (Sarı Etiket / Son 1-2 Gün: daysLeft in 0..2)
    val criticalProducts = remember(products, todayMidnight) {
        products.filter { it.sktTarihi > 0L && it.stokAdedi > 0 && it.getRemainingDays(todayMidnight) in 0..2 }
    }
    val criticalVariety = criticalProducts.size
    val criticalTotalStock = criticalProducts.sumOf { it.stokAdedi }

    // 🟡 Kart 3: Yakın Takip (3 - 7 Gün Kalanlar: daysLeft in 3..7)
    val soonProducts = remember(products, todayMidnight) {
        products.filter { it.sktTarihi > 0L && it.stokAdedi > 0 && it.getRemainingDays(todayMidnight) in 3..7 }
    }
    val soonVariety = soonProducts.size
    val soonTotalStock = soonProducts.sumOf { it.stokAdedi }

    val hasExpired = expiredVariety > 0

    // 4. Depo İade & Eksik İrsaliye Durumu
    val depoRecords = remember(context) { DepoIadeManager.loadRecords(context) }
    val rejectedDepoRecords = remember(depoRecords) {
        depoRecords.filter { it.durum == IadeDurumu.REDDEDILDI }
    }
    val missingDocDepoRecords = remember(depoRecords) {
        depoRecords.filter {
            it.durum == IadeDurumu.DEVAM_EDIYOR && !it.hasGorsel &&
                (it.redNedeni.contains("eksik", ignoreCase = true) ||
                 it.redNedeni.contains("belge", ignoreCase = true) ||
                 it.redNedeni.contains("irsaliye", ignoreCase = true) ||
                 it.aciklama.contains("eksik", ignoreCase = true))
        }
    }
    val ongoingDepoRecords = remember(depoRecords) {
        depoRecords.filter { it.durum == IadeDurumu.DEVAM_EDIYOR }
    }

    // 5. Sabah Açılış Rutini (SharedPreferences tabanlı günlük 4 maddelik checklist)
    val routineItems = remember {
        listOf(
            "Dolap ve şarküteri reyonu sıcaklık/tarih kontrolü yapıldı",
            "Günü geçen ve kritik ürünler raftan toplandı",
            "Son 1-2 günü kalan ürünlere indirim/sarı etiket uygulandı",
            "Reyon önü teşhir ve kasa önü sepetleri düzenlendi"
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

        when {
            rejectedDepoRecords.isNotEmpty() -> {
                sb.append("🚨 *Depo İade Alarmı:* ${rejectedDepoRecords.size} kayıt onaylanmadı (Depo Reddi).\n")
            }
            missingDocDepoRecords.isNotEmpty() -> {
                sb.append("⚠️ *Depo İade Alarmı:* ${missingDocDepoRecords.size} kayıtta eksik irsaliye belgesi var.\n")
            }
            ongoingDepoRecords.isNotEmpty() -> {
                sb.append("📦 *Depo İade Takibi:* ${ongoingDepoRecords.size} kayıt süreçte (${ongoingDepoRecords.first().redNedeni}).\n")
            }
            else -> {
                sb.append("✅ *Depo İade:* Açık iade evrakı bulunmuyor.\n")
            }
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // =====================================================================
        // 1. DİNAMİK VE OTOMATİK SOLA AKAN ACİL ÜRÜN VİTRİNİ (HORIZONTAL PAGER)
        //    + Sağ Üstte Mini "Ekle" Butonu
        // =====================================================================
        com.example.ui.screens.dashboard.UrgentProductsCarousel(
            urgentProducts = urgentProducts,
            todayMidnight = todayMidnight,
            onProductClick = onProductClick,
            onViewAllClick = { onFilterSelectAndNavigate(ProductFilter.URGENT_0_TO_7) },
            onAddProductClick = { onQuickActionClick("add_product") }
        )

        // =====================================================================
        // 2. SABAH OPERASYON GÖREVLERİ + MİNİ ANALİZ & İSTATİSTİKLER ROZETİ
        //    + 3'LÜ YATAY KOMPAKT KARTLAR
        // =====================================================================
        com.example.ui.screens.dashboard.MorningOperationCardsSection(
            expiredVariety = expiredVariety,
            expiredTotalStock = expiredTotalStock,
            criticalVariety = criticalVariety,
            criticalTotalStock = criticalTotalStock,
            soonVariety = soonVariety,
            soonTotalStock = soonTotalStock,
            onFilterSelectAndNavigate = onFilterSelectAndNavigate,
            onAnalyticsClick = { onQuickActionClick("analytics") }
        )

        // =====================================================================
        // 3. OPERASYONEL DURUM (TEK SATIRDA: Adetsel Sayım Kontrolü & Depo İade Takibi)
        // =====================================================================
        val isDepoRedAlert = rejectedDepoRecords.isNotEmpty() || missingDocDepoRecords.isNotEmpty()
        val hasOngoingDepo = ongoingDepoRecords.isNotEmpty()
        val hasAnyDepoAction = isDepoRedAlert || hasOngoingDepo

        val depoStatusText = when {
            rejectedDepoRecords.isNotEmpty() -> "${rejectedDepoRecords.size} Reddedildi"
            missingDocDepoRecords.isNotEmpty() -> "${missingDocDepoRecords.size} Eksik Belge"
            ongoingDepoRecords.isNotEmpty() -> "${ongoingDepoRecords.size} Süreçte"
            else -> "Açık Evrak Yok"
        }

        val depoBorderColor = when {
            isDepoRedAlert -> ExpiredRedBorder
            hasOngoingDepo -> Color(0xFFFDBA74)
            else -> Slate200
        }
        val depoIconBg = when {
            isDepoRedAlert -> ExpiredRedContainer
            hasOngoingDepo -> Color(0xFFFFEDD5)
            else -> Color(0xFFDCFCE7)
        }
        val depoIconTint = when {
            isDepoRedAlert -> ExpiredRed
            hasOngoingDepo -> Color(0xFFEA580C)
            else -> EmeraldSuccess
        }
        val depoTextColor = when {
            isDepoRedAlert -> ExpiredRedDark
            hasOngoingDepo -> Color(0xFFC2410C)
            else -> Color(0xFF15803D)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Sol Kutu: Adetsel Sayım Kontrolü
            Surface(
                onClick = { onQuickActionClick("adetsel") },
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, if (pendingAdetselCount > 0) Color(0xFFFDBA74) else Slate200),
                shadowElevation = 1.dp,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (pendingAdetselCount > 0) Color(0xFFFFEDD5) else Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (pendingAdetselCount > 0) Icons.Default.AssignmentLate else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (pendingAdetselCount > 0) Color(0xFFEA580C) else EmeraldSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = "Adetsel Sayım",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (pendingAdetselCount > 0)
                                "$pendingAdetselCount Bekleyen"
                            else
                                "Sayım: Güncel",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (pendingAdetselCount > 0) Color(0xFFC2410C) else Color(0xFF15803D),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Sağ Kutu: Depo İade Takibi
            Surface(
                onClick = { onQuickActionClick("takip") },
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, depoBorderColor),
                shadowElevation = 1.dp,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(depoIconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hasAnyDepoAction) Icons.Default.LocalShipping else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = depoIconTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = "Depo İade Takibi",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = depoStatusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = depoTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // =====================================================================
        // 4. SABAH AÇILIŞ RUTİNİ (GÜNLÜK HIZLI CHECKLIST)
        // =====================================================================
        com.example.ui.screens.dashboard.MorningOpeningRoutineCard(
            routineItems = routineItems,
            checkedStates = checkedStates,
            onToggleRoutineItem = { toggleRoutineItem(it) }
        )

        // =====================================================================
        // 5. WHATSAPP SABAH RAPORU PAYLAŞ BUTONU
        // =====================================================================
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

        // 6. Alt gezinme çubuğunun (BottomBar) içeriğin üstünü kapatmaması için 80.dp boşluk
        Spacer(modifier = Modifier.height(80.dp))
    }
}
