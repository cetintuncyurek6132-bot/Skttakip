package com.example.ui.components

import com.example.data.getDisplayName
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import java.util.Date
import java.util.Locale

private data class SktChipStyle(
    val sktBgColor: Color,
    val sktBorderColor: Color,
    val sktTextColor: Color,
    val badgeBgColor: Color,
    val badgeTextColor: Color,
    val statusLabel: String
)

@Composable
fun ProductListItemCard(
    product: Product,
    onClick: () -> Unit,
    onQuickAddSkt: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val hasSkt = product.sktTarihi > 0L
    val daysLeft = if (hasSkt) product.getRemainingDays() else 9999L
    val status = if (hasSkt) product.getExpiryStatus() else ExpiryStatus.NORMAL

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

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr-TR"))
    val sktString = if (hasSkt) dateFormat.format(Date(product.sktTarihi)) else "SKT Girilmedi"

    val (sktContainerBg, sktContainerBorder, sktContainerText) = if (!hasSkt) {
        Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), MaterialTheme.colorScheme.onSurfaceVariant)
    } else when (status) {
        ExpiryStatus.EXPIRED -> Triple(ExpiredRedContainer, ExpiredRedBorder, ExpiredRedDark)
        ExpiryStatus.CRITICAL -> Triple(CriticalOrangeContainer, CriticalOrangeBorder, CriticalOrangeDark)
        ExpiryStatus.SOON -> Triple(SoonYellowContainer, SoonYellowBorder, SoonYellowDark)
        ExpiryStatus.WARNING -> Triple(WarningBlueContainer, WarningBlueBorder, WarningBlueDark)
        ExpiryStatus.NORMAL -> Triple(NormalGreenContainer, NormalGreenBorder, NormalGreenDark)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("product_item_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Square badge
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
                                text = kotlin.math.abs(daysLeft).toString(),
                                color = squareTextColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "GEÇTİ",
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

            // Center info
            Column(modifier = Modifier.weight(1f)) {
                // 1. Ürün Adı
                Text(
                    text = product.getDisplayName().uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                // 2. Üst Satır: Adet (BÜYÜK) ve varsa Fiyat / Önemli etiketleri
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Adet Kutusu (Büyük ve belirgin)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(TurquoisePrimary.copy(alpha = 0.16f))
                            .border(1.5.dp, TurquoisePrimary.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${product.stokAdedi} Adet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TurquoiseDark,
                            maxLines = 1
                        )
                    }

                    product.getFormattedPrice()?.let { formattedPrice ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE0F2FE))
                                .border(0.5.dp, Color(0xFF0284C7), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 3.5.dp)
                        ) {
                            Text(
                                text = formattedPrice,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0369A1),
                                maxLines = 1
                            )
                        }
                    }

                    if ((hasSkt && daysLeft in 1..30 && product.stokAdedi >= 10) || product.isImportant) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CriticalOrangeContainer)
                                .border(0.5.dp, CriticalOrange, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🔥 ÖNEMLİ",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = CriticalOrange,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 3. Ürün Kodu / Barkod (Kategori komple kaldırıldı)
                val code = if (product.urunKodu.isNotBlank()) product.urunKodu else product.barkod
                if (code.isNotBlank()) {
                    Text(
                        text = "Kod: $code",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                }

                // 4. Alt Satır: SKT Yazısı Altta (Göze net çarpan, kontrastlı)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(sktContainerBg)
                        .border(1.2.dp, sktContainerBorder, RoundedCornerShape(7.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "📅",
                            fontSize = 11.sp
                        )
                        Text(
                            text = "SKT: $sktString",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            color = sktContainerText,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Hızlı SKT Ekle Butonu (Modal içinde modal döngüsünü bitiren 1 tık aksiyonu)
            if (onQuickAddSkt != null) {
                Surface(
                    onClick = onQuickAddSkt,
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("quick_add_skt_button_${product.id}"),
                    shape = RoundedCornerShape(8.dp),
                    color = TurquoisePrimary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Hızlı SKT Ekle",
                            tint = TurquoiseDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "SKT EKLE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TurquoiseDark
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Right arrow or delete
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = ExpiredRed
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Detay",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun GroupedProductListItemCard(
    productList: List<Product>,
    onClick: (Product) -> Unit,
    onQuickAddSkt: ((Product) -> Unit)? = null,
    onDeleteProduct: ((Product) -> Unit)? = null
) {
    if (productList.isEmpty()) return
    val mainProduct = productList.first()
    val sortedList = remember(productList) {
        productList.sortedBy { if (it.sktTarihi > 0L) it.sktTarihi else Long.MAX_VALUE }
    }
    val totalStock = productList.sumOf { it.stokAdedi }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr-TR")) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(sortedList.first()) }
            .testTag("grouped_product_item_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(TurquoisePrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = "Çoklu SKT",
                        tint = TurquoiseDark,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mainProduct.getDisplayName().uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val groupCode = if (mainProduct.urunKodu.isNotBlank()) mainProduct.urunKodu else mainProduct.barkod
                    if (groupCode.isNotBlank()) {
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
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = formattedPrice,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0369A1)
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(TurquoisePrimary.copy(alpha = 0.16f))
                            .border(1.5.dp, TurquoisePrimary.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$totalStock Adet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TurquoiseDark,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${sortedList.size} SKT Tarihi",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-header text & Quick SKT Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "SKT VE ADETLERİ (${sortedList.size}):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Slate500
                    )

                    if (onQuickAddSkt != null) {
                        Surface(
                            onClick = { onQuickAddSkt(sortedList.first()) },
                            shape = RoundedCornerShape(6.dp),
                            color = TurquoisePrimary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .height(24.dp)
                                .testTag("group_quick_add_skt_button_${mainProduct.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Hızlı SKT Ekle",
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "SKT EKLE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TurquoiseDark
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Kaydır →",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TurquoiseDark
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Horizontal Scrollable Cards/Chips for SKT
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sortedList.forEach { item ->
                    val hasSkt = item.sktTarihi > 0L
                    val daysLeft = if (hasSkt) item.getRemainingDays() else 9999L
                    val status = if (hasSkt) item.getExpiryStatus() else ExpiryStatus.NORMAL

                    val (sktBgColor, sktBorderColor, sktTextColor, badgeBgColor, badgeTextColor, statusLabel) = when {
                        !hasSkt -> SktChipStyle(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.onSurfaceVariant,
                            Slate500,
                            Color.White,
                            "SKT YOK"
                        )
                        status == ExpiryStatus.EXPIRED -> SktChipStyle(
                            ExpiredRedContainer,
                            ExpiredRedBorder,
                            ExpiredRedDark,
                            ExpiredRed,
                            Color.White,
                            when {
                                daysLeft < 0 -> "${kotlin.math.abs(daysLeft)} GÜN GEÇTİ"
                                daysLeft == 0L -> "BUGÜN SON GÜN"
                                else -> "SÜRESİ GEÇTİ"
                            }
                        )
                        status == ExpiryStatus.CRITICAL -> SktChipStyle(
                            CriticalOrangeContainer,
                            CriticalOrangeBorder,
                            CriticalOrangeDark,
                            CriticalOrange,
                            Color.White,
                            if (daysLeft == 1L) "1 GÜN KALDI" else "$daysLeft GÜN"
                        )
                        status == ExpiryStatus.SOON -> SktChipStyle(
                            SoonYellowContainer,
                            SoonYellowBorder,
                            SoonYellowDark,
                            SoonYellow,
                            Color.Black,
                            "$daysLeft GÜN"
                        )
                        status == ExpiryStatus.WARNING -> SktChipStyle(
                            WarningBlueContainer,
                            WarningBlueBorder,
                            WarningBlueDark,
                            WarningBlue,
                            Color.White,
                            "$daysLeft GÜN"
                        )
                        else -> SktChipStyle(
                            NormalGreenContainer,
                            NormalGreenBorder,
                            NormalGreenDark,
                            NormalGreen,
                            Color.White,
                            "$daysLeft GÜN"
                        )
                    }

                    Surface(
                        onClick = { onClick(item) },
                        shape = RoundedCornerShape(10.dp),
                        color = sktBgColor,
                        border = BorderStroke(1.dp, sktBorderColor),
                        modifier = Modifier.testTag("skt_chip_${item.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(badgeBgColor)
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = statusLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = badgeTextColor
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                val sktStr = if (hasSkt) dateFormat.format(Date(item.sktTarihi)) else "Tarihsiz"
                                Text(
                                    text = "📅 SKT: $sktStr",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${item.stokAdedi} Adet",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = sktTextColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
