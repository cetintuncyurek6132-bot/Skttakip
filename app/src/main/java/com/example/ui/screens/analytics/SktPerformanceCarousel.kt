package com.example.ui.screens.analytics

import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import kotlinx.coroutines.delay

private val PurpleAccent = Color(0xFF8B5CF6)
private val PurpleDark = Color(0xFF6D28D9)
private val PurpleBg = Color(0xFFF5F3FF)
private val PurpleBorder = Color(0xFFDDD6FE)

@Composable
fun SktPerformanceCarousel(
    items: List<SktPerformanceItem>,
    onProductClick: (Product) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // =====================================================================
        // 1. BAŞLIK ALANI (Mor/Yeşil Rozet + Çeşit Sayacı)
        // =====================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(PurpleAccent)
                )
                Text(
                    text = "Son 30 Gün SKT Akıbeti",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    maxLines = 1,
                    softWrap = false
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PurpleBg,
                    border = BorderStroke(0.8.dp, PurpleBorder)
                ) {
                    Text(
                        text = "Performans & Fire",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleDark,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "${items.size} Çeşit",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (items.isNotEmpty()) PurpleDark else Slate500,
                maxLines = 1,
                softWrap = false
            )
        }

        // =====================================================================
        // 2. KAYAN KARTLAR VEYA BOŞ DURUM
        // =====================================================================
        if (items.isEmpty()) {
            // ŞIK BOŞ DURUM KARTI
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF0FDF4),
                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kusursuz SKT Yönetimi",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Son 30 günde süresi dolmuş veya fireye ayrılmış ürün kaydı bulunmuyor. Tüm reyonlar güvende.",
                            fontSize = 12.sp,
                            color = Slate600
                        )
                    }
                }
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { items.size })

            LaunchedEffect(items.size) {
                if (items.size > 1) {
                    while (true) {
                        delay(4000L)
                        if (!pagerState.isScrollInProgress) {
                            val nextPage = (pagerState.currentPage + 1) % items.size
                            try {
                                pagerState.animateScrollToPage(
                                    page = nextPage,
                                    animationSpec = tween(durationMillis = 650)
                                )
                            } catch (_: Exception) {}
                        }
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                pageSpacing = 10.dp
            ) { page ->
                val item = items[page]

                Surface(
                    onClick = {
                        item.product?.let { onProductClick(it) }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("skt_performance_carousel_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        // 1. SATIR: Ürün Adı & Kod
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.productName,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            if (item.productCode.isNotBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Slate100,
                                    border = BorderStroke(0.8.dp, Slate200)
                                ) {
                                    Text(
                                        text = "Kod: ${item.productCode}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Slate600,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // 2. SATIR: Biten SKT Bilgisi
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventBusy,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Biten SKT: ${item.formattedSkt}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                        }

                        // 3. SATIR: PERFORMANS ROZETLERİ (Yan Yana 2 Kutu)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Yeşil Kutu: Satıldı / Kurtarıldı
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Satıldı / Kurtarıldı",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF166534)
                                        )
                                        Text(
                                            text = "${item.soldCount} Adet",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF15803D)
                                        )
                                    }
                                }
                            }

                            // Kırmızı Kutu: Fire / İmha Edildi
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFEF2F2),
                                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HighlightOff,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Fire / İmha",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF991B1B)
                                        )
                                        Text(
                                            text = "${item.fireCount} Adet",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFB91C1C)
                                        )
                                    }
                                }
                            }
                        }

                        // 4. SATIR: KURTARMA ORANI VE İLERLEME ÇUBUĞU
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Kurtarma Başarısı",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate600
                                )
                                Text(
                                    text = "%${item.recoveryRate}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when {
                                        item.recoveryRate >= 75 -> Color(0xFF16A34A)
                                        item.recoveryRate >= 50 -> Color(0xFFEA580C)
                                        else -> Color(0xFFDC2626)
                                    }
                                )
                            }

                            LinearProgressIndicator(
                                progress = { (item.recoveryRate / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = when {
                                    item.recoveryRate >= 75 -> Color(0xFF16A34A)
                                    item.recoveryRate >= 50 -> Color(0xFFEA580C)
                                    else -> Color(0xFFDC2626)
                                },
                                trackColor = Color(0xFFF1F5F9)
                            )
                        }
                    }
                }
            }

            // Sayfa Gösterge Noktaları
            if (items.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(items.size.coerceAtMost(10)) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(
                                    width = if (isSelected) 16.dp else 6.dp,
                                    height = 6.dp
                                )
                                .clip(CircleShape)
                                .background(if (isSelected) PurpleAccent else Slate200)
                        )
                    }
                }
            }
        }
    }
}
