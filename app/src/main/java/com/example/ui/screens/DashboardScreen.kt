package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExpiryStatus
import com.example.data.Product
import com.example.data.getDisplayName
import com.example.ui.DashboardState
import com.example.ui.ProductFilter
import com.example.ui.screens.dashboard.DashboardAlertBanner
import com.example.ui.screens.dashboard.DashboardProductItemCard
import com.example.ui.screens.dashboard.DashboardRiskCards
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoisePrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DashboardTabFilter(val label: String, val emoji: String) {
    REMOVE("Kaldır", "🚫"),
    CRITICAL("Kritik (1-3g)", "⚠️"),
    SOON("Yaklaşan (4-7g)", "⏱️"),
    IMPORTANT("Adet ≥10", "🔥"),
    ALL_ATTENTION("Tümü", "📋")
}

@Composable
fun DashboardScreen(
    state: DashboardState,
    onQuickActionClick: (String) -> Unit,
    onFilterSelectAndNavigate: (ProductFilter) -> Unit,
    onProductClick: (Product) -> Unit,
    onViewAllProductsClick: () -> Unit
) {
    val context = LocalContext.current

    val allAttentionList = remember(state.removeProducts, state.nearExpiryProducts, state.attentionProducts) {
        (state.removeProducts + state.nearExpiryProducts + state.attentionProducts).distinctBy { "${it.id}_${it.sktTarihi}" }
    }

    val criticalProducts = remember(allAttentionList) {
        allAttentionList.filter { it.sktTarihi > 0L && it.stokAdedi > 0 && it.getRemainingDays() in 1L..3L }
    }

    val upcomingProducts = remember(allAttentionList) {
        allAttentionList.filter { it.sktTarihi > 0L && it.stokAdedi > 0 && it.getRemainingDays() in 4L..7L }
    }

    val importantProducts = remember(allAttentionList) {
        allAttentionList.filter { it.stokAdedi >= 10 || it.isImportant }
    }

    var selectedTab by remember(state.expiredCount, state.criticalCount, state.soonCount) {
        mutableStateOf(
            when {
                state.expiredCount > 0 -> DashboardTabFilter.REMOVE
                state.criticalCount > 0 -> DashboardTabFilter.CRITICAL
                state.soonCount > 0 -> DashboardTabFilter.SOON
                state.importantCount > 0 -> DashboardTabFilter.IMPORTANT
                else -> DashboardTabFilter.ALL_ATTENTION
            }
        )
    }

    fun shareProductsOnWhatsApp(products: List<Product>) {
        if (products.isEmpty()) {
            android.widget.Toast.makeText(context, "Paylaşılacak dikkat gerektiren ürün bulunamadı.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.forLanguageTag("tr-TR"))
        val sb = StringBuilder()
        sb.append("📋 *A101 - DİKKAT GEREKTİREN SKT LİSTESİ*\n")
        sb.append("Tarih: ").append(SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR")).format(Date())).append("\n\n")

        products.forEachIndexed { index, p ->
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

    val displayedProducts = when (selectedTab) {
        DashboardTabFilter.REMOVE -> state.removeProducts
        DashboardTabFilter.CRITICAL -> criticalProducts
        DashboardTabFilter.SOON -> upcomingProducts
        DashboardTabFilter.IMPORTANT -> importantProducts
        DashboardTabFilter.ALL_ATTENTION -> allAttentionList
    }

    val activeRiskTag = when (selectedTab) {
        DashboardTabFilter.REMOVE -> "EXPIRED"
        DashboardTabFilter.CRITICAL -> "CRITICAL"
        DashboardTabFilter.SOON -> "SOON"
        DashboardTabFilter.IMPORTANT -> "IMPORTANT"
        DashboardTabFilter.ALL_ATTENTION -> "ALL"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)
    ) {
        // 1. ULTRA KOMPAKT AKILLI ACİL DURUM ŞERİDİ
        DashboardAlertBanner(
            expiredCount = state.expiredCount,
            nearCount = state.soonCount + state.criticalCount,
            onShareWhatsApp = { shareProductsOnWhatsApp(allAttentionList) },
            onReviewClick = {
                selectedTab = if (state.expiredCount > 0) DashboardTabFilter.REMOVE else DashboardTabFilter.CRITICAL
            }
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 2. 4'LÜ DOĞRUDAN FİLTRELEYEN KOMPAKT RİSK MATRİSİ
        DashboardRiskCards(
            expiredCount = state.expiredCount,
            criticalCount = state.criticalCount,
            soonCount = state.soonCount,
            importantCount = state.importantCount,
            selectedFilterTag = activeRiskTag,
            onCardClick = { tag ->
                selectedTab = when (tag) {
                    "EXPIRED" -> DashboardTabFilter.REMOVE
                    "CRITICAL" -> DashboardTabFilter.CRITICAL
                    "SOON" -> DashboardTabFilter.SOON
                    "IMPORTANT" -> DashboardTabFilter.IMPORTANT
                    else -> DashboardTabFilter.ALL_ATTENTION
                }
            }
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 3. AKTİF LİSTE BİLGİ & HIZLI KONTROL SATIRI (Tek Satırda!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sol: Aktif listenin başlığı ve sayısı
            val activeTitle = when (selectedTab) {
                DashboardTabFilter.REMOVE -> "🚫 Kaldırılacaklar (${displayedProducts.size})"
                DashboardTabFilter.CRITICAL -> "⚠️ Kritik SKT: 1-3 Gün (${displayedProducts.size})"
                DashboardTabFilter.SOON -> "⏱️ Yaklaşan: 4-7 Gün (${displayedProducts.size})"
                DashboardTabFilter.IMPORTANT -> "🔥 Adet ≥10 Riski (${displayedProducts.size})"
                DashboardTabFilter.ALL_ATTENTION -> "📋 Tüm Dikkat Gerektirenler (${displayedProducts.size})"
            }
            val activeColor = when (selectedTab) {
                DashboardTabFilter.REMOVE -> ExpiredRed
                DashboardTabFilter.CRITICAL -> CriticalOrange
                DashboardTabFilter.SOON -> AmberWarning
                DashboardTabFilter.IMPORTANT -> IndigoAccent
                DashboardTabFilter.ALL_ATTENTION -> TurquoisePrimary
            }

            Text(
                text = activeTitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = activeColor
            )

            // Sağ: "Tümü (XX)" çipi ve "Katalog →" linki
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Tümü çipi
                Surface(
                    onClick = { selectedTab = DashboardTabFilter.ALL_ATTENTION },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedTab == DashboardTabFilter.ALL_ATTENTION) TurquoisePrimary else Slate100,
                    border = BorderStroke(1.dp, if (selectedTab == DashboardTabFilter.ALL_ATTENTION) TurquoisePrimary else Slate200)
                ) {
                    Text(
                        text = "Tümü (${allAttentionList.size})",
                        fontSize = 10.5.sp,
                        fontWeight = if (selectedTab == DashboardTabFilter.ALL_ATTENTION) FontWeight.Black else FontWeight.Bold,
                        color = if (selectedTab == DashboardTabFilter.ALL_ATTENTION) Color.White else Slate700,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                // Tüm Ürünler Katalog Butonu
                Surface(
                    onClick = onViewAllProductsClick,
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent,
                    modifier = Modifier.testTag("see_all_products_link")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Katalog",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate700
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Slate700,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 4. SEÇİLİ RİSK GRUBU LİSTESİ (Ferah, Genişletilmiş ve Hızlı)
        AnimatedContent(
            targetState = displayedProducts,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = "dashboard_products_list_anim"
        ) { currentList ->
            if (currentList.isEmpty()) {
                val emptyMsg = when (selectedTab) {
                    DashboardTabFilter.REMOVE -> "Reyondan kaldırılması gereken ürün bulunmuyor 🎉"
                    DashboardTabFilter.CRITICAL -> "1-3 gün içinde süresi dolacak kritik ürün yok 👍"
                    DashboardTabFilter.SOON -> "4-7 gün içinde süresi dolacak ürün yok ✨"
                    DashboardTabFilter.IMPORTANT -> "Yüksek adetli (≥10) riskli ürün bulunmuyor ✨"
                    DashboardTabFilter.ALL_ATTENTION -> "Dikkat gerektiren herhangi bir ürün bulunmuyor 👏"
                }
                DashboardEmptyState(message = emptyMsg)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 12.dp)
                ) {
                    items(
                        items = currentList,
                        key = { "${it.id}_${it.sktTarihi}_${it.barkod}" }
                    ) { product ->
                        DashboardProductItemCard(
                            product = product,
                            onClick = { onProductClick(product) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardEmptyState(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Slate100),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "✅", fontSize = 28.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate500,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )
        }
    }
}
