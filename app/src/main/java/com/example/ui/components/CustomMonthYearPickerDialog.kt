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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import java.util.Calendar
import java.util.Locale

private val TURKISH_MONTHS = listOf(
    "Ocak",
    "Şubat",
    "Mart",
    "Nisan",
    "Mayıs",
    "Haziran",
    "Temmuz",
    "Ağustos",
    "Eylül",
    "Ekim",
    "Kasım",
    "Aralık"
)

private fun parseInitialMonthAndYear(initialPeriod: String): Pair<Int, Int> {
    val nowCal = Calendar.getInstance()
    val defaultMonthIndex = nowCal.get(Calendar.MONTH).coerceIn(0, 11)
    val defaultYear = nowCal.get(Calendar.YEAR)

    val trimmed = initialPeriod.trim()
    if (trimmed.isEmpty()) {
        return defaultMonthIndex to defaultYear
    }

    val trLocale = Locale.forLanguageTag("tr-TR")
    val lower = trimmed.lowercase(trLocale)

    val matchedMonthIndex = TURKISH_MONTHS.indexOfFirst { monthName ->
        lower.contains(monthName.lowercase(trLocale))
    }.takeIf { it >= 0 } ?: run {
        // Check numeric month formats such as "11-2026", "11/2026", "2026-11"
        val numberTokens = Regex("\\d+").findAll(trimmed).mapNotNull { it.value.toIntOrNull() }.toList()
        val monthNum = numberTokens.firstOrNull { it in 1..12 }
        if (monthNum != null) monthNum - 1 else defaultMonthIndex
    }

    val yearToken = Regex("(19|20)\\d{2}").find(trimmed)?.value?.toIntOrNull()
    val resolvedYear = yearToken ?: defaultYear

    return matchedMonthIndex to resolvedYear
}

@Composable
fun CustomMonthYearPickerDialog(
    initialPeriod: String = "",
    onDismissRequest: () -> Unit,
    onMonthYearSelected: (String) -> Unit
) {
    val (initialMonthIndex, initialYear) = remember(initialPeriod) {
        parseInitialMonthAndYear(initialPeriod)
    }

    var selectedMonthIndex by remember(initialPeriod) { mutableIntStateOf(initialMonthIndex) }
    var selectedYear by remember(initialPeriod) { mutableIntStateOf(initialYear) }

    val formattedPeriod = "${TURKISH_MONTHS[selectedMonthIndex]} $selectedYear"

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .padding(vertical = 12.dp)
                .testTag("custom_month_year_picker_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Slate200),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. ÜST BAŞLIK
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = TurquoisePrimary.copy(alpha = 0.14f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = TurquoiseDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "DÖNEM SEÇİMİ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TurquoiseDark,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = formattedPeriod,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Slate900
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("month_year_picker_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = Slate500,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = Slate200, thickness = 1.dp)

                // 2. YIL SEÇİM ÇUBUĞU ([ < 2026 > ])
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Slate50,
                    border = BorderStroke(1.dp, Slate200),
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
                            onClick = { if (selectedYear > 2000) selectedYear-- },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("month_year_prev_year_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Önceki Yıl",
                                tint = TurquoiseDark,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Text(
                            text = selectedYear.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Slate900,
                            letterSpacing = 0.6.sp,
                            modifier = Modifier.testTag("month_year_selected_year_text")
                        )

                        IconButton(
                            onClick = { if (selectedYear < 2100) selectedYear++ },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("month_year_next_year_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Sonraki Yıl",
                                tint = TurquoiseDark,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // 3. 3x4 AY IZGARASI (12 AY)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TURKISH_MONTHS.chunked(3).forEachIndexed { rowIdx, rowMonths ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowMonths.forEachIndexed { colIdx, monthLabel ->
                                val monthIndex = rowIdx * 3 + colIdx
                                val isSelected = monthIndex == selectedMonthIndex

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) TurquoisePrimary else Slate50
                                        )
                                        .border(
                                            width = if (isSelected) 0.dp else 1.dp,
                                            color = if (isSelected) Color.Transparent else Slate200,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedMonthIndex = monthIndex }
                                        .testTag("month_picker_item_$monthIndex"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = monthLabel,
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                        color = if (isSelected) Color.White else Slate700,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Slate200, thickness = 1.dp)

                // 4. ALT BUTONLAR ("İptal" ve "Seç")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("month_year_picker_cancel_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Slate200),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Slate700
                        )
                    ) {
                        Text(
                            text = "İptal",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate700
                        )
                    }

                    Button(
                        onClick = {
                            onMonthYearSelected(formattedPeriod)
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .weight(1.25f)
                            .height(48.dp)
                            .testTag("month_year_picker_confirm_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TurquoiseDark
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Seç",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
