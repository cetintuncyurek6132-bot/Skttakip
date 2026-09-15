package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.UserManager
import com.example.data.ExpiryStatus
import com.example.data.Product
import com.example.data.getDisplayName
import com.example.ui.DashboardState
import com.example.ui.ProductFilter
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.CriticalOrangeContainer
import com.example.ui.theme.CriticalOrangeDark
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.ExpiredRedContainer
import com.example.ui.theme.NormalGreenContainer
import com.example.ui.theme.NormalGreenDark
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoiseLight
import com.example.ui.theme.TurquoisePrimary
import com.example.ui.theme.WarningBlueContainer
import com.example.ui.theme.WarningBlueDark
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DashboardStatTab {
    TOTAL,
    SOON,
    CRITICAL,
    RETURNS
}

/**
 * Modern, Aydınlık ve Hızlı Okunabilir Ana Sayfa (Dashboard)
 *
 * 1. Sade Üst Karşılama: "Merhaba, [İsim]", Mağaza & Dinamik Tarih, Bildirim Zili & Profil
 * 2. 4'lü Yatay Özet İstatistik Şeridi: Takipte, Yaklaşıyor, Kritik, İade (Filtreli sayfalara doğrudan yönlendirir)
 * 3. 2 Sütunlu Hızlı İşlemler Grid'i: Barkod Tara, Ürün Ekle, Takip Listesi, Adetsel Sayım
 * 4. Dikkat Gerektiren Ürünler Listesi (En yakın SKT'li 3-5 ürün, renkli kalan gün rozetiyle)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardState,
    products: List<Product> = emptyList(),
    onQuickActionClick: (String) -> Unit,
    onFilterSelectAndNavigate: (ProductFilter) -> Unit,
    onProductClick: (Product) -> Unit = {},
    onViewAllProductsClick: () -> Unit,
    onAvatarClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentUser by UserManager.currentUser.collectAsState()

    var selectedTab by remember { mutableStateOf(DashboardStatTab.CRITICAL) }

    val todayMidnight = remember {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    val displayedProducts = remember(products, state, selectedTab) {
        when (selectedTab) {
            DashboardStatTab.TOTAL -> {
                products.filter { it.sktTarihi > 0L }
                    .sortedBy { it.sktTarihi }
            }
            DashboardStatTab.SOON -> {
                products.filter { it.sktTarihi > 0L && it.getExpiryStatus(todayMidnight) == ExpiryStatus.SOON }
                    .sortedBy { it.sktTarihi }
            }
            DashboardStatTab.CRITICAL -> {
                products.filter { it.sktTarihi > 0L && (it.getExpiryStatus(todayMidnight) == ExpiryStatus.CRITICAL || it.getExpiryStatus(todayMidnight) == ExpiryStatus.EXPIRED) }
                    .sortedBy { it.sktTarihi }
            }
            DashboardStatTab.RETURNS -> {
                products.filter { (it.sktTarihi > 0L && it.getRemainingDays(todayMidnight) in 1..30 && it.stokAdedi >= 10) || it.isImportant }
                    .sortedBy { it.sktTarihi }
            }
        }
    }

    val (sectionTitle, badgeColor, targetFilter) = when (selectedTab) {
        DashboardStatTab.TOTAL -> Triple("SKT Girilmiş Tüm Ürünler", Slate900, ProductFilter.ALL)
        DashboardStatTab.SOON -> Triple("Yaklaşan Ürünler", Color(0xFFF59E0B), ProductFilter.SOON)
        DashboardStatTab.CRITICAL -> Triple("Kritik ve Süresi Geçen Ürünler", ExpiredRed, ProductFilter.CRITICAL)
        DashboardStatTab.RETURNS -> Triple("İade / Takip Ürünleri", TurquoiseDark, ProductFilter.IMPORTANT)
    }

    fun shareListOnWhatsApp() {
        if (displayedProducts.isEmpty()) {
            android.widget.Toast.makeText(context, "Paylaşılacak ürün bulunmuyor.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.forLanguageTag("tr-TR"))
        val sb = StringBuilder()
        val listHeaderName = when (selectedTab) {
            DashboardStatTab.TOTAL -> "TÜM SKT'Lİ ÜRÜNLER"
            DashboardStatTab.SOON -> "YAKLAŞAN ÜRÜNLER"
            DashboardStatTab.CRITICAL -> "GÜNLÜK KRİTİK VE SÜRESİ GEÇEN ÜRÜNLER"
            DashboardStatTab.RETURNS -> "İADE / TAKİP LİSTESİ"
        }
        sb.append("📋 *SKT TAKİP - $listHeaderName*\n")
        sb.append("Tarih: ").append(SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR")).format(Date())).append("\n")
        sb.append("Sorumlu: ").append(currentUser?.fullName ?: "Personel").append("\n\n")

        displayedProducts.take(30).forEachIndexed { index, p ->
            val days = p.getRemainingDays(todayMidnight)
            val dateStr = if (p.sktTarihi > 0L) dateFormat.format(Date(p.sktTarihi)) else "-"
            val status = when {
                days < 0 -> "🔴 SÜRESİ GEÇTİ (${-days} gün)"
                days == 0L -> "⚠️ BUGÜN SON GÜN"
                else -> "🟠 $days GÜN KALDI"
            }
            sb.append("${index + 1}. *${p.getDisplayName()}*\n")
            sb.append("   • SKT: $dateStr | $status\n")
            sb.append("   • Stok: ${p.stokAdedi} adet | Barkod: ${p.barkod}\n\n")
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
            context.startActivity(Intent.createChooser(shareIntent, "Listeyi Paylaş"))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("dashboard_screen_root"),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // =====================================================================
        // 1. ÖZET İSTATİSTİK ŞERİDİ (4'LÜ YATAY KART DİZİLİMİ - TIKLANABİLİR / BASILABİLİR)
        // =====================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. SKT GİRİLEN TOPLAM ÜRÜN
            StatSummaryCard(
                modifier = Modifier.weight(1f),
                count = state.totalCount,
                label = "SKT Girilen",
                numberColor = Slate900,
                isSelected = selectedTab == DashboardStatTab.TOTAL,
                selectedBorderColor = Slate900,
                selectedBgColor = Slate900.copy(alpha = 0.08f),
                testTag = "stat_card_total",
                onClick = { selectedTab = DashboardStatTab.TOTAL }
            )

            // 2. YAKLAŞIYOR (Yaklaşanlar Filtresi)
            StatSummaryCard(
                modifier = Modifier.weight(1f),
                count = state.soonCount,
                label = "Yaklaşıyor",
                numberColor = Color(0xFFF59E0B),
                isSelected = selectedTab == DashboardStatTab.SOON,
                selectedBorderColor = Color(0xFFF59E0B),
                selectedBgColor = Color(0xFFF59E0B).copy(alpha = 0.08f),
                testTag = "stat_card_soon",
                onClick = { selectedTab = DashboardStatTab.SOON }
            )

            // 3. KRİTİK (Kritik & Geçenler Filtresi)
            StatSummaryCard(
                modifier = Modifier.weight(1f),
                count = state.expiredCount + state.criticalCount,
                label = "Kritik",
                numberColor = ExpiredRed,
                isSelected = selectedTab == DashboardStatTab.CRITICAL,
                selectedBorderColor = ExpiredRed,
                selectedBgColor = ExpiredRed.copy(alpha = 0.08f),
                testTag = "stat_card_critical",
                onClick = { selectedTab = DashboardStatTab.CRITICAL }
            )

            // 4. İADE / BEKLEYEN (Takip Listesi)
            StatSummaryCard(
                modifier = Modifier.weight(1f),
                count = state.importantCount,
                label = "İade",
                numberColor = TurquoiseDark,
                isSelected = selectedTab == DashboardStatTab.RETURNS,
                selectedBorderColor = TurquoiseDark,
                selectedBgColor = TurquoiseDark.copy(alpha = 0.08f),
                testTag = "stat_card_returns",
                onClick = { selectedTab = DashboardStatTab.RETURNS }
            )
        }

        // =====================================================================
        // 3. HIZLI İŞLEMLER (2 SÜTUNLU KART GRID'İ)
        // =====================================================================
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Hızlı İşlemler",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Yeni Ürün / SKT Ekle
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    title = "Ürün / SKT Ekle",
                    subtitle = "Yeni SKT kaydı aç",
                    icon = Icons.Default.AddCircleOutline,
                    iconBg = NormalGreenContainer,
                    iconTint = NormalGreenDark,
                    testTag = "quick_action_add",
                    onClick = { onQuickActionClick("add_product") }
                )

                // 2. Barkod Tara
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    title = "Barkod Tara",
                    subtitle = "Kamera ile anında bul",
                    icon = Icons.Default.QrCodeScanner,
                    iconBg = TurquoiseLight,
                    iconTint = TurquoiseDark,
                    testTag = "quick_action_scan",
                    onClick = { onQuickActionClick("scan") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 3. Hatırlatıcılar & Mağaza Notları
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    title = "Hatırlatıcılar",
                    subtitle = "Görev ve mağaza notları",
                    icon = Icons.Default.Notifications,
                    iconBg = WarningBlueContainer,
                    iconTint = WarningBlueDark,
                    testTag = "quick_action_reminders",
                    onClick = { onQuickActionClick("reminders") }
                )

                // 4. Yedekleme & Excel / CSV
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    title = "Yedek & Excel",
                    subtitle = "Veri aktarımı ve ayarlar",
                    icon = Icons.Default.Share,
                    iconBg = CriticalOrangeContainer,
                    iconTint = CriticalOrangeDark,
                    testTag = "quick_action_csv",
                    onClick = { onQuickActionClick("csv") }
                )
            }
        }

        // =====================================================================
        // 4. SEÇİLEN KATEGORİDEKİ ÜRÜNLER LİSTESİ
        // =====================================================================
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (displayedProducts.isNotEmpty()) badgeColor else EmeraldSuccess)
                    )
                    Text(
                        text = sectionTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val seeAllShape = RoundedCornerShape(6.dp)
                Text(
                    text = "Tümünü Gör (${displayedProducts.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TurquoiseDark,
                    modifier = Modifier
                        .clip(seeAllShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = TurquoiseDark)
                        ) { onFilterSelectAndNavigate(targetFilter) }
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                )
            }

            if (displayedProducts.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val itemsToShow = displayedProducts.take(30)
                    itemsToShow.forEach { product ->
                        UrgentProductCard(
                            product = product,
                            todayMidnight = todayMidnight,
                            onClick = { onProductClick(product) }
                        )
                    }

                    if (displayedProducts.size > 30) {
                        val moreShape = RoundedCornerShape(10.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(moreShape)
                                .background(Slate100)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true)
                                ) { onFilterSelectAndNavigate(targetFilter) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+${displayedProducts.size - 30} ürünü daha görüntülemek için tıklayın",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate700,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // WhatsApp Liste Paylaşım Butonu
                    val waShape = RoundedCornerShape(12.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .clip(waShape)
                            .background(Color(0xFFF0FDF4))
                            .border(1.dp, Color(0xFFBBF7D0), waShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = Color(0xFF16A34A))
                            ) { shareListOnWhatsApp() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Bu Listeyi WhatsApp'tan Paylaş",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                }
            } else {
                SafeStateCard(
                    title = "Bu Grupta Ürün Bulunmuyor",
                    subtitle = "Seçilen filtreye ait kayıtlı ürün bulunmuyor.",
                    onViewAll = { onFilterSelectAndNavigate(ProductFilter.ALL) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * 4'lü Şerit İçin Tıklanabilir / Basılabilir İstatistik Kartı
 */
@Composable
private fun StatSummaryCard(
    modifier: Modifier = Modifier,
    count: Int,
    label: String,
    numberColor: Color,
    isSelected: Boolean = false,
    selectedBorderColor: Color = numberColor,
    selectedBgColor: Color = numberColor.copy(alpha = 0.08f),
    testTag: String,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .height(84.dp)
            .clip(cardShape)
            .background(if (isSelected) selectedBgColor else Color.White)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) selectedBorderColor else Slate200,
                shape = cardShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = numberColor)
            ) { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$count",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = numberColor,
                lineHeight = 26.sp,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(selectedBorderColor)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) selectedBorderColor else Slate500,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    lineHeight = 13.sp,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 2 Sütunlu Hızlı İşlem Kartı
 */
@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .height(76.dp)
            .clip(cardShape)
            .background(Color.White)
            .border(1.dp, Slate200, cardShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = iconTint)
            ) { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = Slate500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Dikkat Gerektiren Ürün Satır Kartı
 */
@Composable
private fun UrgentProductCard(
    product: Product,
    todayMidnight: Long,
    onClick: () -> Unit
) {
    val remainingDays = product.getRemainingDays(todayMidnight)
    val status = product.getExpiryStatus(todayMidnight)

    val (badgeBg, badgeTextColor, badgeText) = when (status) {
        ExpiryStatus.EXPIRED -> {
            val text = if (remainingDays < 0) "${remainingDays}g" else "Bugün son!"
            Triple(ExpiredRedContainer, ExpiredRed, text)
        }
        ExpiryStatus.CRITICAL -> {
            val text = if (remainingDays == 1L) "Yarın son" else "${remainingDays}g kaldı"
            Triple(CriticalOrangeContainer, CriticalOrange, text)
        }
        ExpiryStatus.SOON -> {
            Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "${remainingDays}g kaldı")
        }
        else -> {
            Triple(NormalGreenContainer, NormalGreenDark, "${remainingDays}g kaldı")
        }
    }

    val cardShape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Color.White)
            .border(1.dp, Slate200, cardShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = badgeTextColor)
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Sol: Kalan gün hap rozeti
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = badgeBg,
                modifier = Modifier.width(82.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeTextColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Orta: Ürün Adı & Kategori & Stok
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.getDisplayName(),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate900,
                    maxLines = 2,
                    lineHeight = 17.sp,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${product.kategori.ifBlank { "Genel" }} • Stok: ${product.stokAdedi} adet",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = Slate500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Sağ: Ok ikonu
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Slate400,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Kritik Ürün Yokken Gösterilen Güvenli Durum Kartı
 */
@Composable
private fun SafeStateCard(
    title: String = "Tüm Reyonlar Güvende",
    subtitle: String = "Bugün acil müdahale gerektiren kritik ürün yok.",
    onViewAll: () -> Unit
) {
    val cardShape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Color.White)
            .border(1.dp, Slate200, cardShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = EmeraldSuccess)
            ) { onViewAll() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDCFCE7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldSuccess,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Slate500
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Slate400,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
