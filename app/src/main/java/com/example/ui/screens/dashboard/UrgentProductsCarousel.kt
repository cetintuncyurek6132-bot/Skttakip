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
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.VerifiedUser
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
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
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
    onViewAllClick: (() -> Unit)? = null,
    onAddProductClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Vitrin Başlık Çubuğu (Her durumda görünür, sağda Stadium "+ Ekle" butonu ile)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sol: Pastel gösterge noktası + "Acil Müdahale Vitrini" + varsa adet sayacı
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (urgentProducts.isNotEmpty()) CriticalOrange else EmeraldSuccess)
                )
                Text(
                    text = "Acil Müdahale Vitrini",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (urgentProducts.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = if (onViewAllClick != null) {
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onViewAllClick() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        } else {
                            Modifier
                        }
                    ) {
                        Text(
                            text = "(${urgentProducts.size})",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = CriticalOrange,
                            maxLines = 1,
                            softWrap = false
                        )
                        if (onViewAllClick != null) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                contentDescription = "Tümünü Gör",
                                tint = CriticalOrange,
                                modifier = Modifier.size(9.dp)
                            )
                        }
                    }
                }
            }

            // Sağ: Şık, Stadium (RoundedCornerShape(50)) mini "Ekle" butonu ve Outlined ikon
            if (onAddProductClick != null) {
                Surface(
                    onClick = onAddProductClick,
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFF0FDFA),
                    border = BorderStroke(1.dp, Color(0xFFCCFBF1)),
                    shadowElevation = 0.5.dp,
                    modifier = Modifier.testTag("dashboard_header_add_product_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddCircleOutline,
                            contentDescription = "Ürün / SKT Ekle",
                            tint = TurquoiseDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Ekle",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TurquoiseDark,
                            maxLines = 1
                        )
                    }
                }
            }
        }

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

            // Yatay Akış Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                pageSpacing = 12.dp
            ) { page ->
                val product = urgentProducts[page]
                val daysLeft = product.getRemainingDays(todayMidnight)

                val badgeInfo = when {
                    daysLeft < 0 -> {
                        val absDays = kotlin.math.abs(daysLeft)
                        UrgentBadgeInfo(
                            bgColor = Color(0xFFFEE2E2),
                            textColor = Color(0xFFB91C1C),
                            numberText = if (absDays > 99) "99+" else absDays.toString(),
                            labelText = "GÜN GEÇTİ"
                        )
                    }
                    daysLeft == 0L -> {
                        UrgentBadgeInfo(
                            bgColor = Color(0xFFFEE2E2),
                            textColor = Color(0xFFB91C1C),
                            numberText = "!",
                            labelText = "BUGÜN"
                        )
                    }
                    daysLeft in 1L..2L -> {
                        UrgentBadgeInfo(
                            bgColor = Color(0xFFFFEDD5),
                            textColor = Color(0xFFC2410C),
                            numberText = daysLeft.toString(),
                            labelText = "GÜN KALDI"
                        )
                    }
                    else -> {
                        UrgentBadgeInfo(
                            bgColor = Color(0xFFFEF3C7),
                            textColor = Color(0xFFB45309),
                            numberText = daysLeft.toString(),
                            labelText = "GÜN KALDI"
                        )
                    }
                }

                val cardPastelBg = when {
                    daysLeft <= 0L -> Color(0xFFFFFBFB)
                    daysLeft in 1L..2L -> Color(0xFFFFFCFA)
                    else -> Color(0xFFFFFEFA)
                }

                val cardBorderColor = when {
                    daysLeft <= 0L -> Color(0xFFFECACA).copy(alpha = 0.7f)
                    daysLeft in 1L..2L -> Color(0xFFFED7AA).copy(alpha = 0.75f)
                    else -> Color(0xFFFDE68A).copy(alpha = 0.8f)
                }

                Surface(
                    onClick = { onProductClick(product) },
                    shape = RoundedCornerShape(20.dp),
                    color = cardPastelBg,
                    border = BorderStroke(1.dp, cardBorderColor),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("urgent_product_carousel_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Sol Pastel Aciliyet Rozeti
                        Box(
                            modifier = Modifier
                                .size(width = 62.dp, height = 58.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(badgeInfo.bgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = badgeInfo.numberText,
                                    fontSize = if (badgeInfo.numberText == "!") 22.sp else 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = badgeInfo.textColor,
                                    lineHeight = 20.sp
                                )
                                Text(
                                    text = badgeInfo.labelText,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeInfo.textColor,
                                    letterSpacing = 0.2.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        // Orta: Ürün Adı, SKT Tarihi ve Kod
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = product.getDisplayName(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "SKT: ${product.getFormattedSkt()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        daysLeft <= 0L -> ExpiredRed
                                        daysLeft in 1L..2L -> CriticalOrange
                                        else -> Color(0xFFB45309)
                                    },
                                    maxLines = 1,
                                    softWrap = false
                                )
                                if (product.urunKodu.isNotBlank()) {
                                    Text(
                                        text = "•",
                                        fontSize = 11.sp,
                                        color = Slate500
                                    )
                                    Text(
                                        text = "Kod: ${product.urunKodu}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Slate800,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Sağ: Stadium Adet Rozeti
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Slate200)
                        ) {
                            Text(
                                text = "${product.stokAdedi} Adet",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
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
        } else {
            // EĞER ACİL ÜRÜN YOKSA (PASTEL TAZELİK GÜVENCESİ KARTI)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFF3FAF6),
                border = BorderStroke(1.dp, Color(0xFFD1FAE5)),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD1FAE5).copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VerifiedUser,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Tüm Reyonlar Güvende",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF065F46)
                        )
                        Text(
                            text = "Acil müdahale gerektiren (0-7 gün) ürün bulunmuyor.",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF047857)
                        )
                    }
                }
            }
        }
    }
}
