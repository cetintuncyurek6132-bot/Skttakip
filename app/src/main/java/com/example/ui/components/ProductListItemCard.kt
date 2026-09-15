package com.example.ui.components

import com.example.data.getDisplayName
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExpiryStatus
import com.example.data.Product
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.CriticalOrangeBorder
import com.example.ui.theme.CriticalOrangeContainer
import com.example.ui.theme.CriticalOrangeDark
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.ExpiredRedBorder
import com.example.ui.theme.ExpiredRedContainer
import com.example.ui.theme.ExpiredRedDark
import com.example.ui.theme.NormalGreen
import com.example.ui.theme.NormalGreenBorder
import com.example.ui.theme.NormalGreenContainer
import com.example.ui.theme.NormalGreenDark
import com.example.ui.theme.Slate500
import com.example.ui.theme.SoonYellow
import com.example.ui.theme.SoonYellowBorder
import com.example.ui.theme.SoonYellowContainer
import com.example.ui.theme.SoonYellowDark
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import com.example.ui.theme.WarningBlue
import com.example.ui.theme.WarningBlueBorder
import com.example.ui.theme.WarningBlueContainer
import com.example.ui.theme.WarningBlueDark
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ProductListItemCard(
    product: Product,
    onClick: () -> Unit,
    onQuickAddSkt: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    todayMidnight: Long = remember {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }
) {
    val hasSkt = product.sktTarihi > 0L
    val daysLeft = if (hasSkt) product.getRemainingDays(todayMidnight) else 9999L
    val status = if (hasSkt) product.getExpiryStatus(todayMidnight) else ExpiryStatus.NORMAL

    val squareBg: Color = if (!hasSkt) {
        Slate500
    } else {
        when (status) {
            ExpiryStatus.EXPIRED -> ExpiredRed
            ExpiryStatus.CRITICAL -> CriticalOrange
            ExpiryStatus.SOON -> SoonYellow
            ExpiryStatus.WARNING -> WarningBlue
            ExpiryStatus.NORMAL -> NormalGreen
        }
    }

    val squareTextColor: Color = if (!hasSkt) {
        Color.White
    } else {
        when (status) {
            ExpiryStatus.SOON -> Color.Black
            else -> Color.White
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr-TR")) }
    val sktSummaryText = if (hasSkt) "SKT: ${dateFormat.format(Date(product.sktTarihi))}" else "SKT: Belirtilmedi"

    val sktTextColor = if (!hasSkt) {
        Slate500
    } else when (status) {
        ExpiryStatus.EXPIRED -> ExpiredRedDark
        ExpiryStatus.CRITICAL -> CriticalOrangeDark
        ExpiryStatus.SOON -> SoonYellowDark
        ExpiryStatus.WARNING -> WarningBlueDark
        ExpiryStatus.NORMAL -> NormalGreenDark
    }

    val cardShape = RoundedCornerShape(12.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) { onClick() }
            .testTag("product_item_card"),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // SOL ALAN: 56x56 dp Durum Rozeti
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(squareBg),
                contentAlignment = Alignment.Center
            ) {
                if (!hasSkt) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SKT",
                            color = squareTextColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "YOK",
                            color = squareTextColor.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                } else when {
                    daysLeft < 0 -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = daysLeft.toString(),
                                color = squareTextColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "GÜN",
                                color = squareTextColor.copy(alpha = 0.95f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp
                            )
                        }
                    }
                    daysLeft == 0L -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "BUGÜN",
                                color = squareTextColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.5.sp
                            )
                            Text(
                                text = "SON GÜN",
                                color = squareTextColor.copy(alpha = 0.95f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                        }
                    }
                    else -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = daysLeft.toString(),
                                color = squareTextColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "GÜN",
                                color = squareTextColor.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // ORTA ALAN: 3 Satırlı Bilgi (Ürün Adı, Kod, SKT)
            Column(modifier = Modifier.weight(1f)) {
                // 1. Satır: Ürün Adı
                Text(
                    text = product.getDisplayName().uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    lineHeight = 18.sp,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 2. Satır: Kod / Barkod bilgisi
                val code = if (product.urunKodu.isNotBlank()) product.urunKodu else product.barkod
                if (code.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Kod: $code",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 3. Satır: SKT Bilgisi
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "📅",
                        fontSize = 11.sp
                    )
                    Text(
                        text = sktSummaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = sktTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // SAĞ ALAN: Fiyat ve Büyük Ferah Adet Rozeti
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                product.getFormattedPrice()?.let { formattedPrice ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE0F2FE))
                            .border(0.5.dp, Color(0xFF0284C7), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formattedPrice,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0369A1),
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(TurquoisePrimary.copy(alpha = 0.15f))
                        .border(1.2.dp, TurquoisePrimary.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${product.stokAdedi} Adet",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TurquoiseDark,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun GroupedProductListItemCard(
    productList: List<Product>,
    onClick: (Product) -> Unit,
    onQuickAddSkt: ((Product) -> Unit)? = null,
    onDeleteProduct: ((Product) -> Unit)? = null,
    todayMidnight: Long = remember {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }
) {
    if (productList.isEmpty()) return
    val mainProduct = productList.first()
    val sortedList = remember(productList) {
        productList.sortedBy { if (it.sktTarihi > 0L) it.sktTarihi else Long.MAX_VALUE }
    }
    val nearestWithSkt = sortedList.firstOrNull { it.sktTarihi > 0L } ?: sortedList.first()
    val hasSkt = nearestWithSkt.sktTarihi > 0L
    val daysLeft = if (hasSkt) nearestWithSkt.getRemainingDays(todayMidnight) else 9999L
    val status = if (hasSkt) nearestWithSkt.getExpiryStatus(todayMidnight) else ExpiryStatus.NORMAL
    val totalStock = productList.sumOf { it.stokAdedi }
    val partyCount = productList.size

    val squareBg: Color = if (!hasSkt) {
        Slate500
    } else {
        when (status) {
            ExpiryStatus.EXPIRED -> ExpiredRed
            ExpiryStatus.CRITICAL -> CriticalOrange
            ExpiryStatus.SOON -> SoonYellow
            ExpiryStatus.WARNING -> WarningBlue
            ExpiryStatus.NORMAL -> NormalGreen
        }
    }

    val squareTextColor: Color = if (!hasSkt) {
        Color.White
    } else {
        when (status) {
            ExpiryStatus.SOON -> Color.Black
            else -> Color.White
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr-TR")) }

    val cardShape = RoundedCornerShape(12.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) { onClick(nearestWithSkt) }
            .testTag("grouped_product_item_card"),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // ÜST KISIM: Sol rozet + Orta İsim & Kod + Sağ Toplam Adet ve Parti Sayısı
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SOL ALAN: 56x56 dp Durum Rozeti (En Yakın/En Kritik SKT'ye göre)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(squareBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (!hasSkt) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "SKT",
                                color = squareTextColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "YOK",
                                color = squareTextColor.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    } else when {
                        daysLeft < 0 -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = daysLeft.toString(),
                                    color = squareTextColor,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "GÜN",
                                    color = squareTextColor.copy(alpha = 0.95f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.5.sp
                                )
                            }
                        }
                        daysLeft == 0L -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "BUGÜN",
                                    color = squareTextColor,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.5.sp
                                )
                                Text(
                                    text = "SON GÜN",
                                    color = squareTextColor.copy(alpha = 0.95f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp
                                )
                            }
                        }
                        else -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = daysLeft.toString(),
                                    color = squareTextColor,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = "GÜN",
                                    color = squareTextColor.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // ORTA ALAN: Ürün Adı ve Kod
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mainProduct.getDisplayName().uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 2,
                        lineHeight = 18.sp,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val groupCode = if (mainProduct.urunKodu.isNotBlank()) mainProduct.urunKodu else mainProduct.barkod
                    if (groupCode.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Kod: $groupCode",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // SAĞ ALAN: Fiyat ve Toplam Stok & Parti Rozeti
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    mainProduct.getFormattedPrice()?.let { formattedPrice ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE0F2FE))
                                .border(0.5.dp, Color(0xFF0284C7), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = formattedPrice,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0369A1),
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(TurquoisePrimary.copy(alpha = 0.15f))
                            .border(1.2.dp, TurquoisePrimary.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$totalStock Adet",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TurquoiseDark,
                                maxLines = 1
                            )
                            Text(
                                text = "$partyCount Parti",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate500,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // ALT KISIM (Kaydırmalı Partiler): Kompakt, şık yatay kayan çipler
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                sortedList.forEach { item ->
                    val itemHasSkt = item.sktTarihi > 0L
                    val itemDaysLeft = if (itemHasSkt) item.getRemainingDays(todayMidnight) else 9999L
                    val itemStatus = if (itemHasSkt) item.getExpiryStatus(todayMidnight) else ExpiryStatus.NORMAL

                    val (chipBgColor, chipBorderColor, badgeBg, badgeTextColor, badgeLabel) = when {
                        !itemHasSkt -> arrayOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                            Slate500,
                            Color.White,
                            "SKT YOK"
                        )
                        itemStatus == ExpiryStatus.EXPIRED -> arrayOf(
                            ExpiredRedContainer,
                            ExpiredRedBorder,
                            ExpiredRed,
                            Color.White,
                            if (itemDaysLeft < 0) "$itemDaysLeft GÜN" else "BUGÜN"
                        )
                        itemStatus == ExpiryStatus.CRITICAL -> arrayOf(
                            CriticalOrangeContainer,
                            CriticalOrangeBorder,
                            CriticalOrange,
                            Color.White,
                            "$itemDaysLeft GÜN"
                        )
                        itemStatus == ExpiryStatus.SOON -> arrayOf(
                            SoonYellowContainer,
                            SoonYellowBorder,
                            SoonYellow,
                            Color.Black,
                            "$itemDaysLeft GÜN"
                        )
                        itemStatus == ExpiryStatus.WARNING -> arrayOf(
                            WarningBlueContainer,
                            WarningBlueBorder,
                            WarningBlue,
                            Color.White,
                            "$itemDaysLeft GÜN"
                        )
                        else -> arrayOf(
                            NormalGreenContainer,
                            NormalGreenBorder,
                            NormalGreen,
                            Color.White,
                            "$itemDaysLeft GÜN"
                        )
                    }

                    val chipShape = RoundedCornerShape(8.dp)
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(chipShape)
                            .background(chipBgColor as Color)
                            .border(BorderStroke(1.dp, chipBorderColor as Color), chipShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true)
                            ) { onClick(item) }
                            .padding(horizontal = 8.dp)
                            .testTag("skt_chip_${item.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(badgeBg as Color)
                                    .padding(horizontal = 5.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = badgeLabel as String,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = badgeTextColor as Color
                                )
                            }

                            val sktStr = if (itemHasSkt) dateFormat.format(Date(item.sktTarihi)) else "Tarihsiz"
                            Text(
                                text = "📅 $sktStr",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "${item.stokAdedi} Adet",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TurquoiseDark
                            )
                        }
                    }
                }
            }
        }
    }
}
