package com.example.ui.screens.dashboard

import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
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
import com.example.data.getDisplayName
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.CriticalOrangeBorder
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.ExpiredRedBorder
import com.example.ui.theme.ExpiredRedDark
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.SoonYellowBorder
import com.example.ui.theme.TurquoisePrimary
import kotlinx.coroutines.delay

private data class UrgentBadgeInfo(
    val bgColor: Color,
    val textColor: Color,
    val numberText: String,
    val labelText: String
)

@Composable
fun UrgentProductsCarousel(
    urgentProducts: List<Product>,
    todayMidnight: Long,
    onProductClick: (Product) -> Unit,
    onViewAllClick: (() -> Unit)? = null
) {
    if (urgentProducts.isNotEmpty()) {
        val pagerState = rememberPagerState(pageCount = { urgentProducts.size })

        LaunchedEffect(urgentProducts.size) {
            if (urgentProducts.size > 1) {
                while (true) {
                    delay(3500L)
                    if (!pagerState.isScrollInProgress) {
                        val nextPage = (pagerState.currentPage + 1) % urgentProducts.size
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

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Vitrin Başlık Çubuğu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sol: Turuncu nokta + "Acil Müdahale Vitrini"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(CriticalOrange)
                    )
                    Text(
                        text = "Acil Müdahale Vitrini",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Sağ: "$count Adet" sade sayaç (asla alt satıra kırılmaz)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = if (onViewAllClick != null) {
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onViewAllClick() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    } else {
                        Modifier
                    }
                ) {
                    Text(
                        text = "${urgentProducts.size} Adet",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CriticalOrange,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (onViewAllClick != null) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "Tümünü Gör",
                            tint = CriticalOrange,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            // Yatay Akış Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                pageSpacing = 10.dp
            ) { page ->
                val product = urgentProducts[page]
                val daysLeft = product.getRemainingDays(todayMidnight)

                val badgeInfo = when {
                    daysLeft < 0 -> {
                        val absDays = kotlin.math.abs(daysLeft)
                        UrgentBadgeInfo(
                            bgColor = ExpiredRed,
                            textColor = Color.White,
                            numberText = if (absDays > 99) "99+" else absDays.toString(),
                            labelText = "GÜN GEÇTİ"
                        )
                    }
                    daysLeft == 0L -> {
                        UrgentBadgeInfo(
                            bgColor = ExpiredRed,
                            textColor = Color.White,
                            numberText = "!",
                            labelText = "BUGÜN"
                        )
                    }
                    daysLeft in 1L..2L -> {
                        UrgentBadgeInfo(
                            bgColor = CriticalOrange,
                            textColor = Color.White,
                            numberText = daysLeft.toString(),
                            labelText = "GÜN KALDI"
                        )
                    }
                    else -> {
                        UrgentBadgeInfo(
                            bgColor = Color(0xFFD97706),
                            textColor = Color.White,
                            numberText = daysLeft.toString(),
                            labelText = "GÜN KALDI"
                        )
                    }
                }

                Surface(
                    onClick = { onProductClick(product) },
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(
                        1.dp,
                        when {
                            daysLeft < 0 -> ExpiredRedBorder
                            daysLeft == 0L -> ExpiredRedBorder
                            daysLeft in 1L..2L -> CriticalOrangeBorder
                            else -> SoonYellowBorder
                        }
                    ),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("urgent_product_carousel_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Sol Aciliyet Rozeti
                        Box(
                            modifier = Modifier
                                .size(width = 62.dp, height = 58.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(badgeInfo.bgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = badgeInfo.numberText,
                                    fontSize = if (badgeInfo.numberText == "!") 23.sp else 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = badgeInfo.textColor,
                                    lineHeight = 20.sp
                                )
                                Text(
                                    text = badgeInfo.labelText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.95f),
                                    letterSpacing = 0.3.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        // Orta: Ürün Adı, SKT Tarihi ve Kod
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            // 1. Satır: Ürün Adı
                            Text(
                                text = product.getDisplayName(),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // 2. Satır: SKT Tarihi (Öncelikli & Renkli) • Kod
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = "SKT: ${product.getFormattedSkt()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        daysLeft <= 0L -> ExpiredRed
                                        daysLeft in 1L..2L -> CriticalOrange
                                        else -> Color(0xFFD97706)
                                    },
                                    maxLines = 1,
                                    softWrap = false
                                )
                                if (product.urunKodu.isNotBlank()) {
                                    Text(
                                        text = "•",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Text(
                                        text = "Kod: ${product.urunKodu}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Slate500,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Sağ: Sabit Adet Rozeti
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Slate100,
                            border = BorderStroke(1.dp, Slate200)
                        ) {
                            Text(
                                text = "${product.stokAdedi} Adet",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate800,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Sayfa Noktacıkları (Pager Indicators)
            if (urgentProducts.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(urgentProducts.size.coerceAtMost(10)) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(
                                    width = if (isSelected) 16.dp else 6.dp,
                                    height = 6.dp
                                )
                                .clip(CircleShape)
                                .background(if (isSelected) TurquoisePrimary else Slate200)
                        )
                    }
                }
            }
        }
    } else {
        // EĞER ACİL ÜRÜN YOKSA (BOŞ DURUM)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF0FDF4),
            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
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

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Tüm Reyonlar Güvende",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF166534)
                    )
                    Text(
                        text = "Acil müdahale gerektiren (0-7 gün) ürün bulunmuyor.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF15803D)
                    )
                }
            }
        }
    }
}
