package com.example.ui.components.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.SoonYellowBorder
import com.example.ui.theme.SoonYellowContainer
import com.example.ui.theme.SoonYellowDark
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import java.util.Locale

/**
 * TAB 2: PRICE, BARCODES & PRODUCT CODES
 */
@Composable
fun ProductDetailPriceInfoTabContent(
    product: Product,
    matchingProducts: List<Product>,
    onQrPriceClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. SATIŞ FİYATI & QR GÜNCELLEME KARTI
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = "Satış Fiyatı",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate500
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val priceVal = product.fiyat
                    if (priceVal != null && priceVal > 0) {
                        val formattedVal = String.format(Locale.forLanguageTag("tr-TR"), "%.2f", priceVal)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "₺$formattedVal",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Text(
                            text = "Fiyat Girilmedi",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate500
                        )
                    }

                    Button(
                        onClick = onQrPriceClick,
                        colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("detail_qr_price_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "QR Fiyat",
                            tint = TurquoiseDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (priceVal != null && priceVal > 0) "QR ile Güncelle" else "QR ile Fiyat Ekle",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TurquoiseDark
                        )
                    }
                }
            }
        }

        // 2. KOD & BARKOD KARTI
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Barkod ve Kod Bilgileri",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate500
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = TurquoiseDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Barkod:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate500
                        )
                    }
                    Text(
                        text = product.barkod.ifBlank { "-" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (product.urunKodu.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tag,
                                contentDescription = null,
                                tint = TurquoiseDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ürün Kodu:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate500
                            )
                        }
                        Text(
                            text = product.urunKodu,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (product.barkod == product.urunKodu || product.barkod.length < 8) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SoonYellowContainer,
                        border = BorderStroke(1.dp, SoonYellowBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ Barkod ile ürün kodu aynı. Raf etiketindeki QR'ı okutarak gerçek barkodu güncelleyebilirsiniz.",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = SoonYellowDark,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}
