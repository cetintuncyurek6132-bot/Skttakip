package com.example.ui.screens.notification

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.ui.theme.*

@Composable
fun ActionableProductNotificationRow(
    product: Product,
    subtitle: String,
    subtitleColor: Color,
    onRemove: () -> Unit,
    onAddToAdetsel: () -> Unit,
    onDetail: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = BorderStroke(0.8.dp, Slate200),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Sol Taraf: Ürün Adı, Kodu, Kalan Gün ve Adet
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.urunAdi,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = subtitle,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = subtitleColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${product.stokAdedi} Adet",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Sağ Taraf: Hızlı Müdahale Aksiyon Butonları
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Raftan Kaldır Butonu
                Surface(
                    onClick = onRemove,
                    shape = RoundedCornerShape(6.dp),
                    color = ExpiredRed.copy(alpha = 0.12f),
                    border = BorderStroke(0.8.dp, ExpiredRed.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "Raftan Al",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = ExpiredRedDark,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }

                // Sayıma Ekle Butonu
                Surface(
                    onClick = onAddToAdetsel,
                    shape = RoundedCornerShape(6.dp),
                    color = TurquoisePrimary.copy(alpha = 0.12f),
                    border = BorderStroke(0.8.dp, TurquoisePrimary.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "+Sayım",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TurquoiseDark,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }

                // İncele / Detay Butonu
                Surface(
                    onClick = onDetail,
                    shape = RoundedCornerShape(6.dp),
                    color = Slate100,
                    border = BorderStroke(0.8.dp, Slate300)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Detay",
                        tint = Slate700,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationCategoryChip(
    title: String,
    badgeCount: Int?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) TurquoisePrimary else Slate100,
        border = BorderStroke(1.dp, if (isSelected) TurquoiseDark else Slate200)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Slate700,
                maxLines = 1,
                softWrap = false
            )
            if (badgeCount != null && badgeCount > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else ExpiredRed)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "$badgeCount",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isSelected) TurquoiseDark else Color.White
                    )
                }
            }
        }
    }
}
