package com.example.ui.screens.scanner

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.ui.theme.Slate700
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun ProductDetailSktInputSection(
    product: Product,
    selectedSktMillis: Long,
    sktAdediStr: String,
    dateFormat: SimpleDateFormat,
    onDateClick: () -> Unit,
    onOcrClick: () -> Unit,
    onPresetDateSelected: (Long) -> Unit,
    onSktAdediChange: (String) -> Unit,
    onAddSkt: (Product, Long, Int) -> Unit,
    onClearDetail: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1: Date Picker & OCR Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onDateClick,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .testTag("skt_date_picker_button"),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, TurquoisePrimary),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Tarih",
                        tint = TurquoisePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateFormat.format(Date(selectedSktMillis)),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Button(
                onClick = onOcrClick,
                modifier = Modifier
                    .height(42.dp)
                    .testTag("open_skt_ocr_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "SKT Tara",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Tarih Oku",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Row 1.5: Hızlı Tarih Kısayolları (Ayaküstü tek tıkla SKT belirleme)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val presets = listOf(
                "+15 Gün" to {
                    val c = Calendar.getInstance()
                    c.add(Calendar.DAY_OF_YEAR, 15)
                    c.timeInMillis
                },
                "+1 Ay" to {
                    val c = Calendar.getInstance()
                    c.add(Calendar.MONTH, 1)
                    c.timeInMillis
                },
                "+2 Ay" to {
                    val c = Calendar.getInstance()
                    c.add(Calendar.MONTH, 2)
                    c.timeInMillis
                },
                "+3 Ay" to {
                    val c = Calendar.getInstance()
                    c.add(Calendar.MONTH, 3)
                    c.timeInMillis
                },
                "+6 Ay" to {
                    val c = Calendar.getInstance()
                    c.add(Calendar.MONTH, 6)
                    c.timeInMillis
                },
                "+1 Yıl" to {
                    val c = Calendar.getInstance()
                    c.add(Calendar.YEAR, 1)
                    c.timeInMillis
                },
                "Yıl Sonu" to {
                    val c = Calendar.getInstance()
                    c.set(Calendar.MONTH, Calendar.DECEMBER)
                    c.set(Calendar.DAY_OF_MONTH, 31)
                    c.timeInMillis
                }
            )
            presets.forEach { (label, dateCalc) ->
                Surface(
                    onClick = {
                        onPresetDateSelected(dateCalc())
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.45f)),
                    modifier = Modifier.height(28.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TurquoiseDark
                        )
                    }
                }
            }
        }

        // Row 2: Adet Arttırma / Azaltma & Manuel Giriş (BÜYÜTÜLMÜŞ, GENİŞ VE ERGONOMİK ALAN)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .border(1.2.dp, TurquoisePrimary.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Adet Başlığı & Hızlı Miktar Kısayolları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📦 Adet / Miktar",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(1, 2, 5, 10, 24).forEach { addCount ->
                        Surface(
                            onClick = {
                                val current = sktAdediStr.toIntOrNull() ?: 0
                                onSktAdediChange((current + addCount).toString())
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+$addCount",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TurquoiseDark
                                )
                            }
                        }
                    }
                }
            }

            // Büyük Stepper [-] [ Sayı ] [+] ve "+ Farklı Tarih" Butonu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .border(1.5.dp, TurquoisePrimary, RoundedCornerShape(12.dp))
                        .padding(3.dp)
                ) {
                    Button(
                        onClick = {
                            val current = sktAdediStr.toIntOrNull() ?: 1
                            if (current > 1) {
                                onSktAdediChange((current - 1).toString())
                            }
                        },
                        modifier = Modifier
                            .size(width = 50.dp, height = 46.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("-", fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Box(
                        modifier = Modifier
                            .width(76.dp)
                            .height(46.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = sktAdediStr,
                            onValueChange = { newValue ->
                                if (newValue.all { it.isDigit() } && newValue.length <= 4) {
                                    onSktAdediChange(newValue)
                                }
                            },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(TurquoisePrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                                .testTag("skt_quantity_input")
                        )
                    }

                    Button(
                        onClick = {
                            val current = sktAdediStr.toIntOrNull() ?: 0
                            onSktAdediChange((current + 1).toString())
                        },
                        modifier = Modifier
                            .size(width = 50.dp, height = 46.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }

                OutlinedButton(
                    onClick = {
                        val count = sktAdediStr.toIntOrNull() ?: 1
                        onAddSkt(product, selectedSktMillis, count)
                        Toast.makeText(context, "✓ SKT eklendi ($count adet), yeni SKT girebilirsiniz", Toast.LENGTH_SHORT).show()
                        onSktAdediChange("1")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("save_skt_button"),
                    border = BorderStroke(1.5.dp, TurquoisePrimary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Farklı Tarih Ekle",
                                tint = TurquoiseDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "+ Tarih Ekle",
                                fontWeight = FontWeight.ExtraBold,
                                color = TurquoiseDark,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Row 3: Tek Adımlı Ana Buton: "Kaydet ve Devam Et"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    val count = sktAdediStr.toIntOrNull() ?: 1
                    onAddSkt(product, selectedSktMillis, count)
                    Toast.makeText(context, "✓ Kaydedildi ($count adet). Sıradaki taranıyor...", Toast.LENGTH_SHORT).show()
                    onClearDetail()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("save_finished_skt_button"),
                colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                shape = RoundedCornerShape(10.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Kaydet ve Devam Et",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Kaydet ve Devam Et",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = onClearDetail,
                modifier = Modifier
                    .height(46.dp)
                    .testTag("close_preview_detail_button"),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Text(
                    text = "Kapat",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
