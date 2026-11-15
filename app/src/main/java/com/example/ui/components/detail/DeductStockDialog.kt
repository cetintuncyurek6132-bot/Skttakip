package com.example.ui.components.detail

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.data.getDisplayName
import com.example.ui.components.AppBottomSheetWrapper
import com.example.ui.theme.AppFontFamily
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeductStockDialog(
    batch: Product,
    onDismiss: () -> Unit,
    onDeduct: (amount: Int, reason: String) -> Unit
) {
    val context = LocalContext.current
    val dateStr = if (batch.sktTarihi > 0L) {
        com.example.util.SafeDateFormatter.shortDate(batch.sktTarihi)
    } else {
        "SKT Girilmedi"
    }

    var deductAmountText by remember(batch) {
        mutableStateOf(if (batch.stokAdedi > 0) "1" else "0")
    }

    AppBottomSheetWrapper(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.45f)
    ) { _ ->
        // 1) BAŞLIK VE SADE KAPAT BUTONU (20.sp, ExtraBold, Koyu)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Adet Düşme İşlemi",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate900,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Kapat",
                    tint = Slate700,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // FORM İÇERİĞİ
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 2) ÜRÜN BİLGİ KARTI (Slate50 arka plan, 16.dp yuvarlak köşeler)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Slate50,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = batch.getDisplayName().uppercase(java.util.Locale.forLanguageTag("tr-TR")),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 19.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SKT: $dateStr",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (batch.stokAdedi > 0) {
                                TurquoisePrimary.copy(alpha = 0.16f)
                            } else {
                                ExpiredRed.copy(alpha = 0.15f)
                            }
                        ) {
                            Text(
                                text = "Mevcut: ${batch.stokAdedi} Adet",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (batch.stokAdedi > 0) TurquoiseDark else ExpiredRed,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // 3) ADET SEÇİCİ (STEPPER) - Tek Bir Kapsül (Pill) + Sağında Şık "Tümü" Butonu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Düşülecek Adet:",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Birleşik Kapsül (Pill) Stepper: [ - | 1 | + ]
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Slate50,
                        border = BorderStroke(1.2.dp, TurquoisePrimary.copy(alpha = 0.65f)),
                        modifier = Modifier.height(46.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Eksi (-) Butonu
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(44.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true, color = TurquoiseDark)
                                    ) {
                                        val q = deductAmountText.toIntOrNull() ?: 1
                                        if (q > 1) deductAmountText = (q - 1).toString()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "−",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TurquoiseDark
                                )
                            }

                            // İnce Dikey Ayırıcı Çizgi
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(22.dp)
                                    .background(Slate200)
                            )

                            // Ortadaki Büyük ve Tok Sayı Girişi
                            Box(
                                modifier = Modifier
                                    .width(56.dp)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value = deductAmountText,
                                    onValueChange = { input ->
                                        val filtered = input.filter { it.isDigit() }.take(4)
                                        deductAmountText = filtered
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    cursorBrush = SolidColor(TurquoiseDark),
                                    textStyle = TextStyle(
                                        fontFamily = AppFontFamily,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Slate900,
                                        textAlign = TextAlign.Center
                                    ),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("deduct_dialog_amount_field")
                                )
                            }

                            // İnce Dikey Ayırıcı Çizgi
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(22.dp)
                                    .background(Slate200)
                            )

                            // Artı (+) Butonu
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(44.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true, color = TurquoiseDark)
                                    ) {
                                        val q = deductAmountText.toIntOrNull() ?: 0
                                        val maxLimit = if (batch.stokAdedi > 0) batch.stokAdedi else 999
                                        if (q < maxLimit) deductAmountText = (q + 1).toString()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TurquoiseDark
                                )
                            }
                        }
                    }

                    // Kapsülün Hemen Sağında Küçük ve Şık "Tümü" Butonu
                    if (batch.stokAdedi > 0) {
                        Surface(
                            onClick = { deductAmountText = batch.stokAdedi.toString() },
                            shape = RoundedCornerShape(50),
                            color = TurquoisePrimary.copy(alpha = 0.14f),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tümü",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TurquoiseDark
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4) AKSİYON BUTONLARI: "Satıldı" (Zümrüt Yeşili 0xFF10B981) ve "Fire" (Yumuşak Kırmızı 0xFFEF4444)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sol Buton: Satıldı
            Button(
                onClick = {
                    val qty = deductAmountText.toIntOrNull() ?: 1
                    if (qty <= 0) {
                        Toast.makeText(context, "Lütfen geçerli bir adet girin", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (batch.stokAdedi <= 0) {
                        Toast.makeText(context, "Bu partide adet bulunmuyor", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val safeQty = minOf(qty, batch.stokAdedi)
                    onDeduct(safeQty, "Satıldı")
                },
                enabled = batch.stokAdedi > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    disabledContainerColor = Color(0xFFCBD5E1)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("dialog_action_satildi_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Satıldı",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Sağ Buton: Fire
            Button(
                onClick = {
                    val qty = deductAmountText.toIntOrNull() ?: 1
                    if (qty <= 0) {
                        Toast.makeText(context, "Lütfen geçerli bir adet girin", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (batch.stokAdedi <= 0) {
                        Toast.makeText(context, "Bu partide adet bulunmuyor", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val safeQty = minOf(qty, batch.stokAdedi)
                    onDeduct(safeQty, "Fire")
                },
                enabled = batch.stokAdedi > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444),
                    disabledContainerColor = Color(0xFFCBD5E1)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("dialog_action_fire_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Fire",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
