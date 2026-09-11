package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoiseLight
import com.example.ui.theme.TurquoisePrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val scope = rememberCoroutineScope()

    var showQuickActionsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentDateStr = remember {
        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("tr-TR"))
        dateFormat.format(Date())
    }

    val firstName = remember(currentUser?.fullName) {
        (currentUser?.fullName ?: "Çetin").trim().split(" ").firstOrNull() ?: "Çetin"
    }

    // Dikkat Gerektiren Ürünler Listesi (WhatsApp Paylaşım için)
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

    // ==========================================
    // 10. TAM EKRAN TASARIMI (GÖRSELLE BİREBİR)
    // ==========================================
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF091422), // Çok koyu derin lacivert
                        Color(0xFF0F1E33), // Atmosferik market tonu
                        Color(0xFF0B1728)
                    )
                )
            )
            .testTag("dashboard_full_screen_root")
    ) {
        // Profesyonel Market Reyon & Mağaza Derinliği Çizimi (Aisle Canvas Background)
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            // Sol ve Sağ Reyon Perspektif Rafları (Çok hafif transparan mavi-gri hatlar)
            val shelfBrushLeft = Brush.horizontalGradient(
                colors = listOf(Color(0x2238BDF8), Color.Transparent),
                startX = 0f,
                endX = w * 0.45f
            )
            val shelfBrushRight = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color(0x2238BDF8)),
                startX = w * 0.55f,
                endX = w
            )

            // Raflar
            for (i in 1..5) {
                val y = h * (0.12f + i * 0.16f)
                val leftPath = Path().apply {
                    moveTo(0f, y)
                    lineTo(w * 0.40f, y - 25f)
                    lineTo(w * 0.40f, y - 12f)
                    lineTo(0f, y + 15f)
                    close()
                }
                drawPath(leftPath, shelfBrushLeft)

                val rightPath = Path().apply {
                    moveTo(w, y)
                    lineTo(w * 0.60f, y - 25f)
                    lineTo(w * 0.60f, y - 12f)
                    lineTo(w, y + 15f)
                    close()
                }
                drawPath(rightPath, shelfBrushRight)
            }

            // Tepe Aydınlatma Vurgusu (Ceiling ambient glow)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x2500C2AB), Color.Transparent),
                    center = Offset(w * 0.5f, 50f),
                    radius = w * 0.7f
                )
            )
        }

        // Karartma & Vignette Overlay Katmanı
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x55000000),
                            Color(0x22000000),
                            Color(0x88050B14)
                        )
                    )
                )
        )

        // Ana İçerik Kolonu (Tam Ekran)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 1. ÜST SATIR: A101 Logosu ve Sağ Avatar Butonu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // A101 Turkuaz Logo Kutusu (Görseldeki Gibi)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = TurquoisePrimary,
                        shadowElevation = 6.dp
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "A101",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Sağ Üst Avatar / Profil Butonu
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x26FFFFFF))
                            .border(1.5.dp, TurquoisePrimary, CircleShape)
                            .clickable { onAvatarClick() }
                            .testTag("dashboard_profile_avatar_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = firstName.take(1).uppercase(Locale.forLanguageTag("tr-TR")),
                            color = TurquoiseLight,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // 2. KULLANICI VE MAĞAZA BİLGİSİ
                Text(
                    text = "Merhaba, $firstName",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-0.4).sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Mağaza #1023 • $currentDateStr",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate300
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 3. BUGÜNKÜ DURUM BAŞLIĞI
                Text(
                    text = "Bugünkü Durum",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 0.4.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. 2x2 KOYU CAM EFEKTLİ DURUM KARTLARI
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Satır: TAKİPTE (Yeşil/Turkuaz) & YAKLAŞIYOR (Turuncu)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // TAKİPTE
                        HeroStatusCard(
                            modifier = Modifier.weight(1f),
                            count = state.totalCount,
                            label = "Takipte",
                            numberColor = TurquoisePrimary,
                            onClick = { onViewAllProductsClick() },
                            testTag = "card_status_takipte"
                        )

                        // YAKLAŞIYOR
                        HeroStatusCard(
                            modifier = Modifier.weight(1f),
                            count = state.soonCount,
                            label = "Yaklaşıyor",
                            numberColor = AmberWarning,
                            onClick = { onFilterSelectAndNavigate(ProductFilter.SOON) },
                            testTag = "card_status_yaklasiyor"
                        )
                    }

                    // 2. Satır: KRİTİK (Kırmızı) & İADE (Açık Mavi/Beyaz)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // KRİTİK
                        val totalCritical = state.expiredCount + state.criticalCount
                        HeroStatusCard(
                            modifier = Modifier.weight(1f),
                            count = totalCritical,
                            label = "Kritik",
                            numberColor = ExpiredRed,
                            onClick = {
                                if (state.expiredCount > 0) {
                                    onFilterSelectAndNavigate(ProductFilter.EXPIRED)
                                } else {
                                    onFilterSelectAndNavigate(ProductFilter.CRITICAL)
                                }
                            },
                            testTag = "card_status_kritik"
                        )

                        // İADE
                        HeroStatusCard(
                            modifier = Modifier.weight(1f),
                            count = state.importantCount,
                            label = "İade",
                            numberColor = Color(0xFF93C5FD),
                            onClick = { onQuickActionClick("takip") },
                            testTag = "card_status_iade"
                        )
                    }
                }
            }

            // 5. ALT BÖLÜM: BÜYÜK TURKUAZ "HIZLI İŞLEMLER ->" BUTONU
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Surface(
                    onClick = { showQuickActionsSheet = true },
                    shape = RoundedCornerShape(16.dp),
                    color = TurquoisePrimary,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("hero_quick_actions_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hızlı İşlemler",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // ==========================================
    // HIZLI İŞLEMLER MODAL BOTTOM SHEET
    // ==========================================
    if (showQuickActionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuickActionsSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF0F1E33),
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
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = TurquoiseLight,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "A101 Mağaza operasyon modüllerine doğrudan erişin",
                    fontSize = 11.5.sp,
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
                            .height(46.dp)
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
                            .height(46.dp)
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
 * 10. Tasarım 2x2 Grid Durum Kartı
 */
@Composable
private fun HeroStatusCard(
    modifier: Modifier = Modifier,
    count: Int,
    label: String,
    numberColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF132238) // Koyu cam zemin
        ),
        border = BorderStroke(
            width = 1.dp,
            color = Color(0x2BFFFFFF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$count",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = numberColor,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
        color = Color(0xFF192B45),
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
                    .size(36.dp)
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
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
