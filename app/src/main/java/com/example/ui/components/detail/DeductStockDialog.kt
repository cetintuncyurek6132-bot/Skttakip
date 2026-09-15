package com.example.ui.components.detail

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Product
import com.example.data.getDisplayName
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeductStockDialog(
    batch: Product,
    onDismiss: () -> Unit,
    onDeduct: (amount: Int, reason: String) -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr-TR")) }
    val dateStr = if (batch.sktTarihi > 0L) dateFormat.format(Date(batch.sktTarihi)) else "SKT Girilmedi"

    var deductAmountText by remember {
        mutableStateOf(if (batch.stokAdedi > 0) "1" else "0")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .padding(vertical = 16.dp)
                .imePadding()
                .clip(RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, TurquoisePrimary),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // 1. BAŞLIK VE KAPAT BUTONU
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = TurquoisePrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Stok Düşme İşlemi",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TurquoiseDark
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = Slate500,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. SEÇİLEN PARTİ BİLGİ KARTI
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = batch.getDisplayName().uppercase(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📅 SKT: $dateStr",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TurquoiseDark
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (batch.stokAdedi > 0) TurquoisePrimary.copy(alpha = 0.15f) else ExpiredRed.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Mevcut: ${batch.stokAdedi} Adet",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (batch.stokAdedi > 0) TurquoiseDark else ExpiredRed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. MİKTAR SEÇİMİ (- / + / "Tümü" / Doğrudan Giriş)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Düşülecek Adet:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // - butonu
                        val counterBtnShape = RoundedCornerShape(8.dp)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(counterBtnShape)
                                .background(Color(0xFFE2E8F0))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true)
                                ) {
                                    val q = deductAmountText.toIntOrNull() ?: 1
                                    if (q > 1) deductAmountText = (q - 1).toString()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Slate700)
                        }

                        // Sayı kutusu
                        BasicTextField(
                            value = deductAmountText,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() }.take(4)
                                deductAmountText = filtered
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                textAlign = TextAlign.Center
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .width(60.dp)
                                .height(38.dp)
                                .clip(counterBtnShape)
                                .background(Color.White, counterBtnShape)
                                .border(1.2.dp, Color(0xFFCBD5E1), counterBtnShape)
                                .padding(horizontal = 4.dp, vertical = 7.dp)
                                .testTag("deduct_dialog_amount_field")
                        )

                        // + butonu
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(counterBtnShape)
                                .background(Color(0xFFE2E8F0))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true)
                                ) {
                                    val q = deductAmountText.toIntOrNull() ?: 0
                                    val maxLimit = if (batch.stokAdedi > 0) batch.stokAdedi else 999
                                    if (q < maxLimit) deductAmountText = (q + 1).toString()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Slate700)
                        }

                        // Tümü Butonu
                        if (batch.stokAdedi > 0) {
                            Box(
                                modifier = Modifier
                                    .height(38.dp)
                                    .clip(counterBtnShape)
                                    .background(TurquoisePrimary.copy(alpha = 0.15f))
                                    .border(BorderStroke(1.2.dp, TurquoiseDark), counterBtnShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true, color = TurquoiseDark)
                                    ) { deductAmountText = batch.stokAdedi.toString() }
                                    .padding(horizontal = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tümü",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TurquoiseDark
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 4. EYLEM BUTONLARI: "SATILDI" (Yeşil) ve "FİRE" (Kırmızı)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                Toast.makeText(context, "Bu partide stok bulunmuyor", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val safeQty = minOf(qty, batch.stokAdedi)
                            onDeduct(safeQty, "Satıldı")
                        },
                        enabled = batch.stokAdedi > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF16A34A),
                            disabledContainerColor = Color(0xFFCBD5E1)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("dialog_action_satildi_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Satıldı",
                            fontSize = 14.sp,
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
                                Toast.makeText(context, "Bu partide stok bulunmuyor", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val safeQty = minOf(qty, batch.stokAdedi)
                            onDeduct(safeQty, "Fire")
                        },
                        enabled = batch.stokAdedi > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            disabledContainerColor = Color(0xFFCBD5E1)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("dialog_action_fire_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Fire",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
