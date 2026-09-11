package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.auth.UserManager
import com.example.data.Product
import com.example.data.getDisplayName
import com.example.ui.DashboardState
import com.example.ui.ProductFilter
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoiseLight
import com.example.ui.theme.TurquoisePrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * TAM EKRAN FOTOĞRAFLI / ATMOSFERİK HERO DASHBOARD EKRANI
 * 
 * - Tam ekran market reyon/koridor atmosferik arka planı
 * - Koyu lacivert-siyah yarı saydam gradyan overlay (üstte koyu, aşağı doğru hafif açık)
 * - Sol üstte A101 turkuaz marka rozeti, sağ üstte bildirim zili
 * - Büyük, kalın "Merhaba, [İsim]" ve altında dinamik tarih
 * - Tek parça Cam Görünümlü (Glassmorphism) 2x2 "Bugünkü Durum" kartı:
 *     - Takipte (Yeşil, 124) | Yaklaşıyor (Turuncu, 8)
 *     - Kritik (Kırmızı, 3)  | İade (Mavi/Turkuaz, 2)
 * - Tam genişlikte büyük yuvarlatılmış turkuaz "Hızlı İşlemler ->" aksiyon butonu
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardState,
    onQuickActionClick: (String) -> Unit,
    onFilterSelectAndNavigate: (ProductFilter) -> Unit,
    onProductClick: (Product) -> Unit = {},
    onViewAllProductsClick: () -> Unit,
    onAvatarClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentUser by UserManager.currentUser.collectAsState()

    var showQuickActionsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentDateStr = remember {
        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("tr-TR"))
        dateFormat.format(Date())
    }

    val firstName = remember(currentUser?.fullName) {
        val name = currentUser?.fullName?.trim() ?: "Çetin"
        name.split(" ").firstOrNull()?.ifBlank { "Çetin" } ?: "Çetin"
    }

    // Gerçek Room DB verileri (boş ise kullanıcı isteğindeki varsayılan değerleri korur)
    val totalCountDisplay = state.totalCount
    val soonCountDisplay = state.soonCount
    val criticalCountDisplay = state.expiredCount + state.criticalCount
    val returnCountDisplay = state.importantCount

    // Dikkat Gerektiren Ürünler (WhatsApp Paylaşımı İçin)
    val allAttentionList = remember(state.removeProducts, state.nearExpiryProducts, state.attentionProducts) {
        (state.removeProducts + state.nearExpiryProducts + state.attentionProducts).distinctBy { "${it.id}_${it.sktTarihi}" }
    }

    fun shareProductsOnWhatsApp() {
        if (allAttentionList.isEmpty()) {
            android.widget.Toast.makeText(context, "Paylaşılacak dikkat gerektiren ürün bulunamadı.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.forLanguageTag("tr-TR"))
        val sb = StringBuilder()
        sb.append("📋 *A101 - DİKKAT GEREKTİREN SKT LİSTESİ*\n")
        sb.append("Tarih: ").append(SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR")).format(Date())).append("\n\n")

        allAttentionList.forEachIndexed { index, p ->
            val days = p.getRemainingDays()
            val dateStr = if (p.sktTarihi > 0L) dateFormat.format(Date(p.sktTarihi)) else "-"
            val status = when {
                days < 0 -> "🚫 SÜRESİ GEÇTİ ($dateStr)"
                days == 0L -> "⚠️ BUGÜN SON GÜN ($dateStr)"
                else -> "⏱️ $days GÜN KALDI ($dateStr)"
            }
            sb.append("${index + 1}. *${p.getDisplayName()}*\n")
            sb.append("   • Durum: $status\n")
            sb.append("   • Adet: ${p.stokAdedi} | Barkod: ${p.barkod}\n\n")
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            `package` = "com.whatsapp"
        }
        try {
            context.startActivity(sendIntent)
        } catch (e: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, sb.toString())
            }
            try {
                context.startActivity(Intent.createChooser(fallbackIntent, "Ürün Listesini Paylaş"))
            } catch (ex: Exception) {
                android.widget.Toast.makeText(context, "Paylaşım uygulaması açılamadı.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // =========================================================================
    // 1. TAM EKRAN FOTOĞRAFLI / ATMOSFERİK HERO ARKA PLAN & GRADYAN OVERLAY
    // =========================================================================
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070F1C))
            .testTag("dashboard_full_screen_root")
    ) {
        // Market Rafı / Koridoru Atmosferik Arka Plan Çizimi
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            // Sol ve Sağ Reyon Perspektif Rafları (Hafif odak dışı market atmosferi)
            val shelfBrushLeft = Brush.horizontalGradient(
                colors = listOf(Color(0x3538BDF8), Color(0x1000C2AB), Color.Transparent),
                startX = 0f,
                endX = w * 0.45f
            )
            val shelfBrushRight = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color(0x1000C2AB), Color(0x3538BDF8)),
                startX = w * 0.55f,
                endX = w
            )

            // Raflar ve derinlik hatları
            for (i in 1..5) {
                val y = h * (0.12f + i * 0.16f)
                val leftPath = Path().apply {
                    moveTo(0f, y)
                    lineTo(w * 0.42f, y - 28f)
                    lineTo(w * 0.42f, y - 14f)
                    lineTo(0f, y + 18f)
                    close()
                }
                drawPath(leftPath, shelfBrushLeft)

                val rightPath = Path().apply {
                    moveTo(w, y)
                    lineTo(w * 0.58f, y - 28f)
                    lineTo(w * 0.58f, y - 14f)
                    lineTo(w, y + 18f)
                    close()
                }
                drawPath(rightPath, shelfBrushRight)
            }

            // Tepe Aydınlatması (Ambient glow spot)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x3000C2AB), Color.Transparent),
                    center = Offset(w * 0.5f, 60f),
                    radius = w * 0.75f
                )
            )

            // Hafif Bokeh Işık Halkaları (Market lambası derinlik hissi)
            drawCircle(
                color = Color(0x1538BDF8),
                radius = 35f,
                center = Offset(w * 0.25f, h * 0.35f)
            )
            drawCircle(
                color = Color(0x1500C2AB),
                radius = 45f,
                center = Offset(w * 0.78f, h * 0.42f)
            )
            drawCircle(
                color = Color(0x12F59E0B),
                radius = 28f,
                center = Offset(w * 0.5f, h * 0.28f)
            )
        }

        // KOYU LACİVERT-SİYAH YARI SAYDAM GRADYAN OVERLAY
        // Üst kısımda daha KOYU (yazılar net okunur), aşağı doğru biraz daha AÇIK (market hissi belli olur)
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xF5060E1A), // Üstte çok koyu
                            0.35f to Color(0xEB0A1626),
                            0.70f to Color(0xCC0E1F35),
                            1.0f to Color(0xB8091424)   // Altta hafif açık
                        )
                    )
                )
        )

        // =========================================================================
        // 2. İÇERİK KATMANI (OVERLAY'İN ÜSTÜNDE)
        // =========================================================================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // -------------------------------------------------------------
                // 1. ÜST SATIR (MARKA LOGOSU + BİLDİRİM ZİLİ)
                // -------------------------------------------------------------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sol Üst: SKT Turkuaz Marka Rozeti
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = TurquoisePrimary,
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                            Text(
                                text = "SKT",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    // Sağ Üst: Yarı Saydam Koyu Daire İçinde Bildirim Zili İkonu
                    Surface(
                        onClick = { onNotificationClick() },
                        shape = CircleShape,
                        color = Color(0x33FFFFFF),
                        border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("dashboard_notification_bell_btn")
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Bildirimler",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            if (state.unreadNotificationCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 8.dp, end = 8.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(ExpiredRed)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // -------------------------------------------------------------
                // 2. KARŞILAMA METNİ
                // -------------------------------------------------------------
                Text(
                    text = "Merhaba, $firstName",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Mağaza #1023 • $currentDateStr",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate400
                )

                Spacer(modifier = Modifier.height(26.dp))

                // -------------------------------------------------------------
                // 3. "BUGÜNKÜ DURUM" KARTI (CAM GÖRÜNÜMLÜ - GLASSMORPHISM)
                // -------------------------------------------------------------
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .testTag("dashboard_today_status_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0x550B1728) // Yarı şeffaf koyu cam taban (~%40-50 opaklık)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = Color(0x2EFFFFFF) // İnce beyaz/açık kenarlık
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 18.dp)
                    ) {
                        // Üst Başlık
                        Text(
                            text = "Bugünkü Durum",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.3.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 2x2 GRID DÜZENİ (İnce ayırıcı çizgilerle bölünmüş)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // 1. Satır: TAKİPTE (Yeşil) | YAKLAŞIYOR (Turuncu)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(92.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Sol Üst: TAKİPTE (Yeşil)
                                GlassStatBox(
                                    modifier = Modifier.weight(1f),
                                    count = totalCountDisplay,
                                    label = "Takipte",
                                    numberColor = Color(0xFF22C55E), // Canlı Yeşil
                                    onClick = { onViewAllProductsClick() },
                                    testTag = "stat_box_takipte"
                                )

                                // Dikey İnce Ayırıcı Çizgi
                                VerticalDivider(
                                    color = Color(0x22FFFFFF),
                                    thickness = 1.dp,
                                    modifier = Modifier.fillMaxHeight(0.75f)
                                )

                                // Sağ Üst: YAKLAŞIYOR (Turuncu)
                                GlassStatBox(
                                    modifier = Modifier.weight(1f),
                                    count = soonCountDisplay,
                                    label = "Yaklaşıyor",
                                    numberColor = Color(0xFFF97316), // Canlı Turuncu
                                    onClick = { onFilterSelectAndNavigate(ProductFilter.SOON) },
                                    testTag = "stat_box_yaklasiyor"
                                )
                            }

                            // Yatay İnce Ayırıcı Çizgi
                            HorizontalDivider(
                                color = Color(0x22FFFFFF),
                                thickness = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 2. Satır: KRİTİK (Kırmızı) | İADE (Mavi/Turkuaz)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(92.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Sol Alt: KRİTİK (Kırmızı)
                                GlassStatBox(
                                    modifier = Modifier.weight(1f),
                                    count = criticalCountDisplay,
                                    label = "Kritik",
                                    numberColor = Color(0xFFEF4444), // Canlı Kırmızı
                                    onClick = {
                                        if (state.expiredCount > 0) {
                                            onFilterSelectAndNavigate(ProductFilter.EXPIRED)
                                        } else {
                                            onFilterSelectAndNavigate(ProductFilter.CRITICAL)
                                        }
                                    },
                                    testTag = "stat_box_kritik"
                                )

                                // Dikey İnce Ayırıcı Çizgi
                                VerticalDivider(
                                    color = Color(0x22FFFFFF),
                                    thickness = 1.dp,
                                    modifier = Modifier.fillMaxHeight(0.75f)
                                )

                                // Sağ Alt: İADE (Mavi/Turkuaz)
                                GlassStatBox(
                                    modifier = Modifier.weight(1f),
                                    count = returnCountDisplay,
                                    label = "İade",
                                    numberColor = Color(0xFF38BDF8), // Canlı Açık Mavi / Turkuaz
                                    onClick = { onQuickActionClick("takip") },
                                    testTag = "stat_box_iade"
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // -------------------------------------------------------------
                // 4. HIZLI ERİŞİM ARAÇLARI (4'LÜ CAM BUTON GRID'İ)
                // -------------------------------------------------------------
                Text(
                    text = "Hızlı Araçlar",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.3.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionGlassTile(
                        modifier = Modifier.weight(1f),
                        title = "Barkod Tara",
                        subtitle = "Kamera ile oku",
                        icon = Icons.Default.QrCodeScanner,
                        accentColor = TurquoisePrimary,
                        onClick = { onQuickActionClick("scan") }
                    )
                    QuickActionGlassTile(
                        modifier = Modifier.weight(1f),
                        title = "Ürün Ekle",
                        subtitle = "Yeni SKT kaydet",
                        icon = Icons.Default.AddCircleOutline,
                        accentColor = Color(0xFF22C55E),
                        onClick = { onQuickActionClick("add_product") }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionGlassTile(
                        modifier = Modifier.weight(1f),
                        title = "Adetsel Sayım",
                        subtitle = "Reyon kontrolü",
                        icon = Icons.Default.AssignmentTurnedIn,
                        accentColor = Color(0xFFF97316),
                        onClick = { onQuickActionClick("adetsel") }
                    )
                    QuickActionGlassTile(
                        modifier = Modifier.weight(1f),
                        title = "İade & Depo",
                        subtitle = "Depo iadeleri",
                        icon = Icons.Default.Checklist,
                        accentColor = Color(0xFF38BDF8),
                        onClick = { onQuickActionClick("takip") }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // -------------------------------------------------------------
                // 5. ÖNCELİKLİ SKT KONTROL LİSTESİ
                // -------------------------------------------------------------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = "Acil Kontrol Listesi",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.3.sp
                        )
                    }

                    Text(
                        text = "Tümünü Gör (${state.totalCount})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TurquoisePrimary,
                        modifier = Modifier.clickable { onViewAllProductsClick() }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val priorityList = (state.attentionProducts + state.nearExpiryProducts).distinctBy { it.id }.take(3)
                if (priorityList.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        priorityList.forEach { product ->
                            DashboardProductPreviewCard(
                                product = product,
                                onClick = { onProductClick(product) }
                            )
                        }
                    }
                } else {
                    DashboardSafeStatusCard(onViewAll = onViewAllProductsClick)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // -------------------------------------------------------------
                // 6. TÜM HIZLI İŞLEMLER PANELİ BUTONU
                // -------------------------------------------------------------
                Surface(
                    onClick = { showQuickActionsSheet = true },
                    shape = RoundedCornerShape(18.dp),
                    color = TurquoisePrimary,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("hero_quick_actions_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tüm Hızlı İşlemler Paneli",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 0.2.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // =========================================================================
    // HIZLI İŞLEMLER MODAL BOTTOM SHEET
    // =========================================================================
    if (showQuickActionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuickActionsSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF0D1B2D),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color(0x66FFFFFF))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "⚡ HIZLI İŞLEMLER",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Black,
                    color = TurquoiseLight,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "A101 Mağaza operasyon modüllerine doğrudan erişin",
                    fontSize = 12.sp,
                    color = Slate300
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 4 Ana Hızlı Aksiyon Butonu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickSheetItem(
                        modifier = Modifier.weight(1f),
                        label = "Ürün Ekle",
                        icon = Icons.Default.AddCircleOutline,
                        color = TurquoisePrimary,
                        onClick = {
                            showQuickActionsSheet = false
                            onQuickActionClick("add_product")
                        }
                    )

                    QuickSheetItem(
                        modifier = Modifier.weight(1f),
                        label = "Barkod Tara",
                        icon = Icons.Default.QrCodeScanner,
                        color = Color(0xFF38BDF8),
                        onClick = {
                            showQuickActionsSheet = false
                            onQuickActionClick("scan")
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickSheetItem(
                        modifier = Modifier.weight(1f),
                        label = "Adetsel",
                        icon = Icons.Default.Checklist,
                        color = Color(0xFFA78BFA),
                        onClick = {
                            showQuickActionsSheet = false
                            onQuickActionClick("adetsel")
                        }
                    )

                    QuickSheetItem(
                        modifier = Modifier.weight(1f),
                        label = "İade / Depo",
                        icon = Icons.Default.AssignmentTurnedIn,
                        color = Color(0xFFFB923C),
                        onClick = {
                            showQuickActionsSheet = false
                            onQuickActionClick("takip")
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0x22FFFFFF), thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // WhatsApp Paylaş & Ayarlar Aksiyonları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        onClick = {
                            showQuickActionsSheet = false
                            shareProductsOnWhatsApp()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF25D366).copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.4f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_whatsapp),
                                contentDescription = null,
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "WhatsApp Paylaş",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            showQuickActionsSheet = false
                            onQuickActionClick("csv")
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x22FFFFFF),
                        border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = Slate300,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Genel Ayarlar",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

/**
 * 2x2 Glassmorphism Kart İçi İstatistik Kutusu
 */
@Composable
private fun GlassStatBox(
    modifier: Modifier = Modifier,
    count: Int,
    label: String,
    numberColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxHeight()
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$count",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    color = numberColor,
                    letterSpacing = (-0.8).sp,
                    lineHeight = 36.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFCBD5E1), // Soluk beyaz / gri
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Hızlı İşlemler Bottom Sheet Elemanı
 */
@Composable
private fun QuickSheetItem(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF16273D),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier.height(68.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Hızlı İşlem Cam Karo Butonu
 */
@Composable
private fun QuickActionGlassTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color(0x550D1B2D),
        border = BorderStroke(1.dp, Color(0x28FFFFFF)),
        modifier = modifier.height(72.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Ana Sayfa Kritik / Yaklaşan Ürün Önizleme Kartı
 */
@Composable
private fun DashboardProductPreviewCard(
    product: Product,
    onClick: () -> Unit
) {
    val remainingDays = product.getRemainingDays()
    val isExpired = remainingDays <= 0
    val isCritical = remainingDays in 1..7

    val badgeColor = when {
        isExpired -> Color(0xFFEF4444)
        isCritical -> Color(0xFFF97316)
        else -> Color(0xFF38BDF8)
    }

    val badgeText = when {
        remainingDays < 0 -> "${-remainingDays} gün geçti"
        remainingDays == 0L -> "Bugün son gün!"
        remainingDays == 1L -> "Yarın son gün"
        else -> "$remainingDays gün kaldı"
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color(0x450A1626),
        border = BorderStroke(1.dp, Color(0x22FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 36.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(badgeColor)
                )

                Column {
                    Text(
                        text = product.getDisplayName(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${product.kategori.ifBlank { "Genel" }} • Stok: ${product.stokAdedi} adet",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = badgeColor.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
            ) {
                Text(
                    text = badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Kritik Ürün Olmadığında Gösterilen Güvenli Durum Kartı
 */
@Composable
private fun DashboardSafeStatusCard(
    onViewAll: () -> Unit
) {
    Surface(
        onClick = onViewAll,
        shape = RoundedCornerShape(14.dp),
        color = Color(0x350A1626),
        border = BorderStroke(1.dp, Color(0x22FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x2222C55E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tüm Reyonlar Güvende",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Bugün acil müdahale gerektiren kritik ürün yok.",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate400
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Slate400,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
