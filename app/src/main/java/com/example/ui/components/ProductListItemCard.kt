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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.text.style.TextAlign
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

private val sharedSktDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr-TR"))

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
    val hasSkt = product.sktTarihi > 0L && product.stokAdedi > 0
    val daysLeft = remember(product.sktTarihi, todayMidnight) {
        if (hasSkt) product.getRemainingDays(todayMidnight) else 9999L
    }
    val status = remember(product.sktTarihi, todayMidnight) {
        if (hasSkt) product.getExpiryStatus(todayMidnight) else ExpiryStatus.NORMAL
    }

    val squareBg: Color = remember(hasSkt, status) {
        if (!hasSkt) {
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
    }

    val squareTextColor: Color = Color.White

    val sktSummaryText = remember(product.sktTarihi) {
        if (hasSkt) {
            synchronized(sharedSktDateFormat) {
                "SKT: ${sharedSktDateFormat.format(Date(product.sktTarihi))}"
            }
        } else "SKT: Girilmedi"
    }

    val sktTextColor = remember(hasSkt, status) {
        if (!hasSkt) {
            Slate500
        } else when (status) {
            ExpiryStatus.EXPIRED -> ExpiredRedDark
            ExpiryStatus.CRITICAL -> CriticalOrangeDark
            ExpiryStatus.SOON -> SoonYellowDark
            ExpiryStatus.WARNING -> WarningBlueDark
            ExpiryStatus.NORMAL -> NormalGreenDark
        }
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // SOL ALAN: Kart boyuna sıfırlanan 66dp renkli durum bloğu
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(66.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(squareBg),
                contentAlignment = Alignment.Center
            ) {
                if (!hasSkt) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = "SKT",
                            color = squareTextColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            lineHeight = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "YOK",
                            color = squareTextColor.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            lineHeight = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else when {
                    daysLeft < 0 -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = kotlin.math.abs(daysLeft).toString(),
                                color = squareTextColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                lineHeight = 20.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "GÜN GEÇTİ",
                                color = squareTextColor.copy(alpha = 0.95f),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.5.sp,
                                letterSpacing = 0.5.sp,
                                lineHeight = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    daysLeft == 0L -> {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                            Text(
                                text = "SON GÜN",
                                color = squareTextColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    daysLeft == 1L -> {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                            Text(
                                text = "YARIN",
                                color = squareTextColor,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.5.sp,
                                letterSpacing = 0.5.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = daysLeft.toString(),
                                color = squareTextColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                lineHeight = 20.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "GÜN KALDI",
                                color = squareTextColor.copy(alpha = 0.9f),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.5.sp,
                                letterSpacing = 0.5.sp,
                                lineHeight = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // ORTA VE SAĞ ALAN: Ürün adı, kod, SKT ve Sağ Adet Alanı
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ORTA ALAN: Kompakt 2 Satırlı Bilgi (Ürün Adı ve Kod • SKT)
                Column(modifier = Modifier.weight(1f)) {
                    // 1. Satır: Ürün Adı
                    Text(
                        text = product.getDisplayName().uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 2,
                        lineHeight = 16.sp,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // 2. Satır: Kod ve SKT yan yana
                    val displayCode = if (product.urunKodu.isNotBlank()) product.urunKodu else product.barkod
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (displayCode.isNotBlank()) {
                            Text(
                                text = "Kod: $displayCode",
                                fontSize = 11.sp,
                                color = Slate500,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "•",
                                fontSize = 10.sp,
                                color = Slate500
                            )
                        }
                        Text(
                            text = sktSummaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = sktTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // SAĞ ALAN: Fiyat ve Orantılı Adet Rozeti
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
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = formattedPrice,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0369A1),
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(TurquoisePrimary.copy(alpha = 0.15f))
                            .border(1.dp, TurquoisePrimary.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${product.stokAdedi} Adet",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TurquoiseDark,
                            maxLines = 1
                        )
                    }
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
    val validBatches = remember(productList) {
        productList.filter { it.sktTarihi > 0L && it.stokAdedi > 0 }
    }
    val sortedList = remember(productList, validBatches) {
        if (validBatches.isNotEmpty()) {
            validBatches.sortedBy { it.sktTarihi }
        } else {
            productList.sortedBy { if (it.sktTarihi > 0L) it.sktTarihi else Long.MAX_VALUE }
        }
    }
    val nearestWithSkt = sortedList.firstOrNull { it.sktTarihi > 0L && it.stokAdedi > 0 }
        ?: sortedList.firstOrNull { it.sktTarihi > 0L }
        ?: sortedList.first()
    val hasSkt = nearestWithSkt.sktTarihi > 0L && nearestWithSkt.stokAdedi > 0 && validBatches.isNotEmpty()
    val daysLeft = remember(nearestWithSkt.sktTarihi, todayMidnight) {
        if (hasSkt) nearestWithSkt.getRemainingDays(todayMidnight) else 9999L
    }
    val status = remember(nearestWithSkt.sktTarihi, todayMidnight) {
        if (hasSkt) nearestWithSkt.getExpiryStatus(todayMidnight) else ExpiryStatus.NORMAL
    }
    val totalStock = remember(productList) { productList.sumOf { it.stokAdedi } }
    val partyCount = remember(validBatches, productList) {
        if (validBatches.isNotEmpty()) validBatches.size else productList.size
    }

    val squareBg: Color = remember(hasSkt, status) {
        if (!hasSkt) {
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
    }

    val squareTextColor: Color = Color.White

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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // SOL ALAN: Kartın en tepesinden en altına kadar tam boy uzanan 66dp renkli durum bloğu
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(66.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(squareBg),
                contentAlignment = Alignment.Center
            ) {
                if (partyCount >= 2) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = partyCount.toString(),
                            color = squareTextColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            lineHeight = 22.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "PARTİ",
                            color = squareTextColor.copy(alpha = 0.95f),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp,
                            lineHeight = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (!hasSkt) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = "SKT",
                            color = squareTextColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            lineHeight = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "YOK",
                            color = squareTextColor.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            lineHeight = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else when {
                    daysLeft < 0 -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = kotlin.math.abs(daysLeft).toString(),
                                color = squareTextColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                lineHeight = 20.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "GÜN GEÇTİ",
                                color = squareTextColor.copy(alpha = 0.95f),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.5.sp,
                                letterSpacing = 0.5.sp,
                                lineHeight = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    daysLeft == 0L -> {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                            Text(
                                text = "SON GÜN",
                                color = squareTextColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    daysLeft == 1L -> {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                            Text(
                                text = "YARIN",
                                color = squareTextColor,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.5.sp,
                                letterSpacing = 0.5.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = daysLeft.toString(),
                                color = squareTextColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                lineHeight = 20.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "GÜN KALDI",
                                color = squareTextColor.copy(alpha = 0.9f),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.5.sp,
                                letterSpacing = 0.5.sp,
                                lineHeight = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // SAĞ ALAN (Column): Üst satır (İsim, Kod, Fiyat, Toplam) + Alt satır (Parti çipleri)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                // ÜST SATIR: Ürün Adı, Kod, Fiyat ve Toplam Adet & Parti Rozeti
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                    // SAĞ ALAN: Fiyat ve Toplam Adet & Parti Rozeti
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
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "Toplam $totalStock Adet",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TurquoiseDark,
                                maxLines = 1
                            )
                        }
                    }
                }

                // ALT SATIR (Kaydırmalı Partiler): Kompakt, şık yatay kayan çipler
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 12.dp, end = 10.dp, top = 2.dp, bottom = 8.dp),
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
                                if (itemDaysLeft < 0) "${kotlin.math.abs(itemDaysLeft)} GÜN GEÇTİ" else "SON GÜN"
                            )
                            itemStatus == ExpiryStatus.CRITICAL -> arrayOf(
                                CriticalOrangeContainer,
                                CriticalOrangeBorder,
                                CriticalOrange,
                                Color.White,
                                when {
                                    itemDaysLeft == 0L -> "SON GÜN"
                                    itemDaysLeft == 1L -> "YARIN"
                                    else -> "$itemDaysLeft GÜN KALDI"
                                }
                            )
                            itemStatus == ExpiryStatus.SOON -> arrayOf(
                                SoonYellowContainer,
                                SoonYellowBorder,
                                SoonYellow,
                                Color.White,
                                "$itemDaysLeft GÜN KALDI"
                            )
                            itemStatus == ExpiryStatus.WARNING -> arrayOf(
                                WarningBlueContainer,
                                WarningBlueBorder,
                                WarningBlue,
                                Color.White,
                                "$itemDaysLeft GÜN KALDI"
                            )
                            else -> arrayOf(
                                NormalGreenContainer,
                                NormalGreenBorder,
                                NormalGreen,
                                Color.White,
                                "$itemDaysLeft GÜN KALDI"
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
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = badgeTextColor as Color
                                    )
                                }

                                val sktStr = if (itemHasSkt) {
                                    synchronized(sharedSktDateFormat) {
                                        sharedSktDateFormat.format(Date(item.sktTarihi))
                                    }
                                } else "SKT Girilmedi"
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
}
