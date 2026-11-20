package com.example.ui.screens.scanner

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
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
import com.example.ui.theme.TurquoisePrimary
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun ProductDetailSktInputSection(
    product: Product,
    selectedSktMillis: Long,
    sktAdediStr: String,
    dateFormat: SimpleDateFormat,
    onDateClick: () -> Unit,
    onSktAdediChange: (String) -> Unit,
    onStartEditing: () -> Unit = {},
    onAddSkt: (Product, Long, Int) -> Unit,
    onResetForm: () -> Unit = {}
) {
    val context = LocalContext.current
    var lastActionTimestamp by remember { mutableLongStateOf(0L) }

    val handleAdetChange: (String) -> Unit = { newValue ->
        onStartEditing()
        onSktAdediChange(newValue)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: Date Picker (Left, weight 1.15f) + Quantity Stepper [- | 1 | +] (Right, weight 0.85f)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sol: Takvim ikonlu tarih seçici kutu (48.dp)
            Surface(
                onClick = {
                    onStartEditing()
                    onDateClick()
                },
                modifier = Modifier
                    .weight(1.15f)
                    .height(48.dp)
                    .testTag("skt_date_picker_button"),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Son Kullanma Tarihi Seç",
                        tint = TurquoisePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateFormat.format(Date(selectedSktMillis)),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Sağ: Kompakt [ - | 1 | + ] adet sayacı (48.dp)
            Surface(
                modifier = Modifier
                    .weight(0.85f)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentCount = sktAdediStr.toIntOrNull() ?: 1
                    val canDecrement = currentCount > 1

                    Box(
                        modifier = Modifier
                            .width(38.dp)
                            .fillMaxHeight()
                            .clickable(enabled = canDecrement) {
                                if (currentCount > 1) {
                                    handleAdetChange((currentCount - 1).toString())
                                }
                            }
                            .testTag("skt_quantity_decrement"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Adet Azalt",
                            tint = if (canDecrement) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicTextField(
                            value = sktAdediStr,
                            onValueChange = { newValue ->
                                if (newValue.all { it.isDigit() } && newValue.length <= 4) {
                                    handleAdetChange(newValue)
                                }
                            },
                            singleLine = true,
                            textStyle = TextStyle(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            cursorBrush = SolidColor(TurquoisePrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("skt_quantity_input")
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        onStartEditing()
                                    }
                                }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(38.dp)
                            .fillMaxHeight()
                            .clickable {
                                val next = (sktAdediStr.toIntOrNull() ?: 0) + 1
                                handleAdetChange(next.toString())
                            }
                            .testTag("skt_quantity_increment"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Adet Artır",
                            tint = TurquoisePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Row 2: Tek Tam Genişlikte Birincil Buton: "✓ Kaydet ve Devam Et" (48.dp)
        Button(
            onClick = {
                val now = System.currentTimeMillis()
                if (now - lastActionTimestamp < 350L) return@Button
                lastActionTimestamp = now
                val count = sktAdediStr.toIntOrNull()?.coerceAtLeast(1) ?: 1
                onAddSkt(product, selectedSktMillis, count)
                Toast.makeText(
                    context,
                    "✓ Kaydedildi ($count adet). Yeni SKT girebilirsiniz.",
                    Toast.LENGTH_SHORT
                ).show()
                onResetForm()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_finished_skt_button"),
            colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
            shape = RoundedCornerShape(10.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Kaydet ve Devam Et",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
