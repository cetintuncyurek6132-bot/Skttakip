package com.example.ui.screens.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.CustomBoxedCalendarDialog
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DateRangePickerModal(
    initialStart: Long?,
    initialEnd: Long?,
    onDismiss: () -> Unit,
    onApply: (start: Long?, end: Long?) -> Unit,
    onClear: () -> Unit
) {
    var startDateMillis by remember { mutableStateOf(initialStart) }
    var endDateMillis by remember { mutableStateOf(initialEnd) }

    var isSelectingStart by remember { mutableStateOf(false) }
    var isSelectingEnd by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("tr-TR")) }

    if (isSelectingStart) {
        CustomBoxedCalendarDialog(
            initialDateMillis = startDateMillis ?: System.currentTimeMillis(),
            onDismissRequest = { isSelectingStart = false },
            onDateSelected = { millis ->
                startDateMillis = millis
                isSelectingStart = false
            }
        )
    }

    if (isSelectingEnd) {
        CustomBoxedCalendarDialog(
            initialDateMillis = endDateMillis ?: System.currentTimeMillis(),
            onDismissRequest = { isSelectingEnd = false },
            onDateSelected = { millis ->
                endDateMillis = millis
                isSelectingEnd = false
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, TurquoisePrimary),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Modal Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = TurquoisePrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Tarih Aralığı",
                                    tint = TurquoisePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SKT TARİH ARALIĞI FİLTRESİ",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = TurquoiseDark
                            )
                            Text(
                                text = "2 tarih seçerek reyon ürünlerini süzün",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Kapat", tint = Slate500)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Date Selection Cards (Start Date & End Date)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Start Date Card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isSelectingStart = true },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (startDateMillis != null) TurquoisePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "1. BAŞLANGIÇ",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TurquoisePrimary
                                )
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = TurquoisePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = startDateMillis?.let { dateFormat.format(Date(it)) } ?: "Tarih Seçin",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (startDateMillis != null) MaterialTheme.colorScheme.onSurface else Slate500
                            )
                        }
                    }

                    // End Date Card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isSelectingEnd = true },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (endDateMillis != null) TurquoisePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "2. BİTİŞ",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TurquoisePrimary
                                )
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = TurquoisePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = endDateMillis?.let { dateFormat.format(Date(it)) } ?: "Tarih Seçin",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (endDateMillis != null) MaterialTheme.colorScheme.onSurface else Slate500
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Presets
                Text(
                    text = "HIZLI TARIH SEÇIMI:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Preset: İlk 7 Gün
                    Surface(
                        onClick = {
                            val now = System.currentTimeMillis()
                            val calEnd = Calendar.getInstance().apply {
                                timeInMillis = now
                                add(Calendar.DAY_OF_YEAR, 7)
                            }
                            startDateMillis = now
                            endDateMillis = calEnd.timeInMillis
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "İlk 7 Gün",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    // Preset: Sonraki 7 Gün (Day 7 to Day 14)
                    Surface(
                        onClick = {
                            val now = System.currentTimeMillis()
                            val calStart = Calendar.getInstance().apply {
                                timeInMillis = now
                                add(Calendar.DAY_OF_YEAR, 7)
                            }
                            val calEnd = Calendar.getInstance().apply {
                                timeInMillis = now
                                add(Calendar.DAY_OF_YEAR, 14)
                            }
                            startDateMillis = calStart.timeInMillis
                            endDateMillis = calEnd.timeInMillis
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "Sonraki 7 Gün",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    // Preset: Bu Ay
                    Surface(
                        onClick = {
                            val calStart = Calendar.getInstance().apply {
                                set(Calendar.DAY_OF_MONTH, 1)
                            }
                            val calEnd = Calendar.getInstance().apply {
                                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                            }
                            startDateMillis = calStart.timeInMillis
                            endDateMillis = calEnd.timeInMillis
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "Bu Ay",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    // Preset: Tarihleri Temizle
                    Surface(
                        onClick = {
                            startDateMillis = null
                            endDateMillis = null
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "Temizle",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onClear,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("SIFIRLA", fontWeight = FontWeight.Bold, color = Slate700, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { onApply(startDateMillis, endDateMillis) },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(46.dp)
                            .testTag("apply_date_range_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = TurquoisePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("FİLTREYİ UYGULA", fontWeight = FontWeight.Black, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
