package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CriticalOrangeBorder
import com.example.ui.theme.CriticalOrangeContainer
import com.example.ui.theme.CriticalOrangeDark
import com.example.ui.theme.ExpiredRedBorder
import com.example.ui.theme.ExpiredRedContainer
import com.example.ui.theme.ExpiredRedDark
import com.example.ui.theme.NormalGreenBorder
import com.example.ui.theme.NormalGreenContainer
import com.example.ui.theme.NormalGreenDark
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import com.example.ui.theme.WarningBlueBorder
import com.example.ui.theme.WarningBlueContainer
import com.example.ui.theme.WarningBlueDark
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CustomBoxedCalendarDialog(
    initialDateMillis: Long,
    onDismissRequest: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    var selectedMillis by remember {
        mutableStateOf(if (initialDateMillis > 0L) initialDateMillis else System.currentTimeMillis())
    }

    var calendarView by remember {
        mutableStateOf(Calendar.getInstance().apply {
            timeInMillis = if (initialDateMillis > 0L) initialDateMillis else System.currentTimeMillis()
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        })
    }

    val todayMidnight = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val currentYear = calendarView.get(Calendar.YEAR)
    val currentMonth = calendarView.get(Calendar.MONTH)

    val trLocale = remember { Locale.forLanguageTag("tr-TR") }
    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", trLocale) }
    val fullDateFormat = remember { SimpleDateFormat("dd MMMM yyyy, EEEE", trLocale) }
    val shortDateFormat = remember { SimpleDateFormat("dd MMMM yyyy", trLocale) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. ÜST BAŞLIK & SEÇİLİ TARİH
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SON KULLANMA TARİHİ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TurquoiseDark,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (selectedMillis > 0L) fullDateFormat.format(Date(selectedMillis)) else "Tarih Seçiniz",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp)

                // 2. AY VE YIL GEÇİŞ ÇUBUĞU (‹  AĞUSTOS 2026  ›)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val nextCal = calendarView.clone() as Calendar
                                nextCal.set(Calendar.DAY_OF_MONTH, 1)
                                nextCal.add(Calendar.MONTH, -1)
                                calendarView = nextCal
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Önceki Ay",
                                tint = TurquoiseDark,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Text(
                            text = monthYearFormat.format(calendarView.time).uppercase(trLocale),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.5.sp
                        )

                        IconButton(
                            onClick = {
                                val nextCal = calendarView.clone() as Calendar
                                nextCal.set(Calendar.DAY_OF_MONTH, 1)
                                nextCal.add(Calendar.MONTH, 1)
                                calendarView = nextCal
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Sonraki Ay",
                                tint = TurquoiseDark,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // 3. HAFTANIN GÜNLERİ (PZT - SAL - ÇAR - PER - CUM - CMT - PAZ)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val daysOfWeek = listOf("PZT", "SAL", "ÇAR", "PER", "CUM", "CMT", "PAZ")
                    daysOfWeek.forEach { dayName ->
                        Text(
                            text = dayName,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 4. TAKVİM GÜN IZGARASI
                val tempCal = calendarView.clone() as Calendar
                tempCal.set(Calendar.DAY_OF_MONTH, 1)
                val dayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
                val emptySlotsBefore = (dayOfWeek + 5) % 7
                val maxDaysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

                val totalGridCells = emptySlotsBefore + maxDaysInMonth
                val totalRows = (totalGridCells + 6) / 7

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    for (rowIndex in 0 until totalRows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (colIndex in 0 until 7) {
                                val cellIndex = rowIndex * 7 + colIndex
                                val dayNumber = cellIndex - emptySlotsBefore + 1

                                if (dayNumber in 1..maxDaysInMonth) {
                                    val cellCal = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, currentYear)
                                        set(Calendar.MONTH, currentMonth)
                                        set(Calendar.DAY_OF_MONTH, dayNumber)
                                        set(Calendar.HOUR_OF_DAY, 12)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    val cellMillis = cellCal.timeInMillis

                                    val cellMidnight = cellCal.clone() as Calendar
                                    cellMidnight.set(Calendar.HOUR_OF_DAY, 0)
                                    val isPast = cellMidnight.timeInMillis < todayMidnight

                                    val isSelected = isSameDay(selectedMillis, cellMillis)
                                    val isToday = isSameDay(todayMidnight, cellMillis)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .padding(1.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when {
                                                    isSelected -> TurquoiseDark
                                                    isToday -> TurquoisePrimary.copy(alpha = 0.15f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .border(
                                                width = when {
                                                    isSelected -> 0.dp
                                                    isToday -> 1.5.dp
                                                    else -> 0.dp
                                                },
                                                color = if (isToday) TurquoiseDark else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                selectedMillis = cellMillis
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNumber.toString(),
                                            fontWeight = if (isSelected || isToday) FontWeight.Black else FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = when {
                                                isSelected -> Color.White
                                                isToday -> TurquoiseDark
                                                isPast -> Slate500
                                                else -> Slate900
                                            }
                                        )
                                    }
                                } else {
                                    Spacer(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .padding(1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp)

                // 5. SEÇİLEN TARİH BİLGİSİ & KALAN GÜN
                val daysDiff = remember(selectedMillis, todayMidnight) {
                    if (selectedMillis <= 0L) null else {
                        val selCal = Calendar.getInstance().apply {
                            timeInMillis = selectedMillis
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val diffMs = selCal.timeInMillis - todayMidnight
                        (diffMs / (1000L * 60 * 60 * 24)).toInt()
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Seçilen Tarih:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (selectedMillis > 0L) shortDateFormat.format(Date(selectedMillis)) else "-",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Slate900
                            )

                            if (daysDiff != null) {
                                val bgCol: Color
                                val txtCol: Color
                                val borderCol: Color
                                val label: String

                                when {
                                    daysDiff < 0 -> {
                                        bgCol = ExpiredRedContainer
                                        txtCol = ExpiredRedDark
                                        borderCol = ExpiredRedBorder
                                        label = "$daysDiff gün"
                                    }
                                    daysDiff == 0 -> {
                                        bgCol = CriticalOrangeContainer
                                        txtCol = CriticalOrangeDark
                                        borderCol = CriticalOrangeBorder
                                        label = "Bugün bitiyor"
                                    }
                                    daysDiff == 1 -> {
                                        bgCol = CriticalOrangeContainer
                                        txtCol = CriticalOrangeDark
                                        borderCol = CriticalOrangeBorder
                                        label = "1 gün kaldı"
                                    }
                                    daysDiff in 2..7 -> {
                                        bgCol = CriticalOrangeContainer
                                        txtCol = CriticalOrangeDark
                                        borderCol = CriticalOrangeBorder
                                        label = "$daysDiff gün kaldı"
                                    }
                                    daysDiff in 8..30 -> {
                                        bgCol = WarningBlueContainer
                                        txtCol = WarningBlueDark
                                        borderCol = WarningBlueBorder
                                        label = "$daysDiff gün kaldı"
                                    }
                                    else -> {
                                        bgCol = NormalGreenContainer
                                        txtCol = NormalGreenDark
                                        borderCol = NormalGreenBorder
                                        label = "$daysDiff gün kaldı"
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = bgCol,
                                    border = BorderStroke(0.8.dp, borderCol)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = txtCol,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("cancel_date_picker_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            text = "VAZGEÇ",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (selectedMillis > 0L) {
                                onDateSelected(selectedMillis)
                                onDismissRequest()
                            }
                        },
                        enabled = selectedMillis > 0L,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(46.dp)
                            .testTag("confirm_select_date_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TurquoiseDark,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Tarihi Seç",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TARİHİ SEÇ",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

private fun isSameDay(millis1: Long, millis2: Long): Boolean {
    if (millis1 <= 0 || millis2 <= 0) return false
    val cal1 = Calendar.getInstance().apply { timeInMillis = millis1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
