package com.example.ui.components

import android.widget.Toast
import com.example.ui.screens.DateOcrScannerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Product
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSktModal(
    product: Product,
    isEditMode: Boolean = false,
    onDismiss: () -> Unit,
    onSaveSkt: (product: Product, sktTarihi: Long, stokAdedi: Int) -> Unit
) {
    var stokAdedi by remember(product) {
        mutableStateOf(if (isEditMode && product.stokAdedi > 0) product.stokAdedi.toString() else "1")
    }
    val initialDateMillis = remember(product) {
        if (isEditMode && product.sktTarihi > 0L) product.sktTarihi else System.currentTimeMillis()
    }
    var selectedDateMillis by remember(initialDateMillis) { mutableStateOf(initialDateMillis) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showOcrScanner by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr-TR"))

    if (showOcrScanner) {
        DateOcrScannerDialog(
            onDismiss = { showOcrScanner = false },
            onDateDetected = { dateMillis, _ ->
                selectedDateMillis = dateMillis
                showOcrScanner = false
            }
        )
    }

    if (showDatePicker) {
        CustomBoxedCalendarDialog(
            initialDateMillis = selectedDateMillis,
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { selectedMillis ->
                selectedDateMillis = selectedMillis
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.35f)),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // 1. MODAL HEADER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = TurquoisePrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isEditMode) Icons.Default.Edit else Icons.Default.Add,
                                    contentDescription = null,
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isEditMode) "SKT VE ADET DÜZENLE" else "Tarih ve Adet Ekle",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TurquoiseDark
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = Slate500,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. PRODUCT INFO CARD
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = product.urunAdi,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        val codeDisplay = if (product.urunKodu.isNotBlank()) "Kod: ${product.urunKodu} | Barkod: ${product.barkod}" else "Barkod: ${product.barkod}"
                        Text(
                            text = codeDisplay,
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. NEW SKT DATE & OCR SCANNER BUTTON
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Yeni SKT Tarihi",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TurquoiseDark
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(2.dp, TurquoisePrimary),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clickable { showDatePicker = true }
                                .testTag("input_new_skt_date")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateFormat.format(Date(selectedDateMillis)),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Tarih Seç",
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showOcrScanner = true },
                            modifier = Modifier.height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, TurquoisePrimary),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = TurquoiseDark
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Kamera ile Tarih Tara",
                                tint = TurquoiseDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "OCR TARA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TurquoiseDark
                            )
                        }
                    }

                    // Hızlı SKT Tarih Kısayolları (Tek dokunuşla tarih belirleme)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                                    selectedDateMillis = dateCalc()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.4f)),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
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
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. SKT QUANTITY STEPPER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SKT Adedi (Giriş Miktarı):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Slate900
                    )

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(TurquoisePrimary.copy(alpha = 0.15f))
                                    .clickable {
                                        val current = stokAdedi.toIntOrNull() ?: 0
                                        if (current > 1) stokAdedi = (current - 1).toString()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "-",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TurquoiseDark
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .width(56.dp)
                                    .height(36.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    BasicTextField(
                                        value = stokAdedi,
                                        onValueChange = { newValue ->
                                            if (newValue.all { it.isDigit() } && newValue.length <= 4) {
                                                stokAdedi = newValue
                                            }
                                        },
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 17.sp,
                                            color = Slate900
                                        ),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        cursorBrush = SolidColor(TurquoiseDark),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(TurquoisePrimary.copy(alpha = 0.15f))
                                    .clickable {
                                        val current = stokAdedi.toIntOrNull() ?: 0
                                        stokAdedi = (current + 1).toString()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "+",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TurquoiseDark
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 5. ACTION BUTTONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val context = LocalContext.current
                    if (!isEditMode) {
                        Button(
                            onClick = {
                                val count = stokAdedi.toIntOrNull() ?: 1
                                onSaveSkt(product, selectedDateMillis, count)
                                Toast.makeText(context, "✓ SKT eklendi ($count adet)", Toast.LENGTH_SHORT).show()
                                stokAdedi = "1"
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("submit_add_skt_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Ekle", tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "EKLE",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val count = stokAdedi.toIntOrNull() ?: 1
                            onSaveSkt(product, selectedDateMillis, count)
                            val msg = if (isEditMode) "✓ SKT güncellendi ($count adet)" else "✓ Kaydedildi"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("save_and_close_skt_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isEditMode) TurquoisePrimary else Slate900),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Kaydet", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEditMode) "DÜZENLEMEYİ KAYDET" else "KAYDET",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
