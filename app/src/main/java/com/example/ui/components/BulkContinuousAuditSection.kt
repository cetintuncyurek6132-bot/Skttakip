package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ExpiredRedBorder
import com.example.ui.theme.ExpiredRedContainer
import com.example.ui.theme.ExpiredRedDark
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import com.example.ui.viewmodel.BulkContinuousAuditInput
import java.text.DecimalFormat

private data class BulkContinuousRowSpec(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val pastelBg: Color,
    val borderColor: Color,
    val iconTint: Color
)

@Composable
fun BulkContinuousAuditSection(
    moneyFormatter: DecimalFormat,
    ratioFormatter: DecimalFormat,
    initialCategoryTargetRatios: Map<String, Double> = emptyMap(),
    onDismiss: () -> Unit,
    onSaveBulk: (
        auditPeriod: String,
        notes: String,
        entries: List<BulkContinuousAuditInput>
    ) -> Unit
) {
    val rowSpecs = remember {
        listOf(
            BulkContinuousRowSpec(
                key = "ET_TAVUK",
                label = "Et-Tavuk",
                icon = Icons.Default.SetMeal,
                pastelBg = Color(0xFFFFF1F2),
                borderColor = Color(0xFFFECDD3),
                iconTint = Color(0xFFBE123C)
            ),
            BulkContinuousRowSpec(
                key = "EKMEK",
                label = "Ekmek",
                icon = Icons.Default.BakeryDining,
                pastelBg = Color(0xFFFFFBEB),
                borderColor = Color(0xFFFDE68A),
                iconTint = Color(0xFFB45309)
            ),
            BulkContinuousRowSpec(
                key = "MEYVE_SEBZE",
                label = "Meyve-Sebze",
                icon = Icons.Default.Eco,
                pastelBg = Color(0xFFF0FDF4),
                borderColor = Color(0xFFBBF7D0),
                iconTint = Color(0xFF15803D)
            ),
            BulkContinuousRowSpec(
                key = "DONUK_MEYVE_SEBZE",
                label = "Donuk Meyve-Sebze",
                icon = Icons.Default.AcUnit,
                pastelBg = Color(0xFFF0F9FF),
                borderColor = Color(0xFFBAE6FD),
                iconTint = Color(0xFF0369A1)
            )
        )
    }

    var periodText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var showValidationError by remember { mutableStateOf(false) }
    var showMonthYearPicker by remember { mutableStateOf(false) }

    if (showMonthYearPicker) {
        CustomMonthYearPickerDialog(
            initialPeriod = periodText,
            onDismissRequest = { showMonthYearPicker = false },
            onMonthYearSelected = { chosenPeriod ->
                periodText = chosenPeriod
                showMonthYearPicker = false
            }
        )
    }

    val revenueInputs = remember {
        mutableStateMapOf(
            "ET_TAVUK" to "",
            "EKMEK" to "",
            "MEYVE_SEBZE" to "",
            "DONUK_MEYVE_SEBZE" to ""
        )
    }
    val fireInputs = remember {
        mutableStateMapOf(
            "ET_TAVUK" to "",
            "EKMEK" to "",
            "MEYVE_SEBZE" to "",
            "DONUK_MEYVE_SEBZE" to ""
        )
    }
    val targetInputs = remember(initialCategoryTargetRatios) {
        fun fmtTarget(key: String): String {
            val v = initialCategoryTargetRatios[key]?.takeIf { it > 0.0 } ?: 1.0
            return if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
        }
        mutableStateMapOf(
            "ET_TAVUK" to fmtTarget("ET_TAVUK"),
            "EKMEK" to fmtTarget("EKMEK"),
            "MEYVE_SEBZE" to fmtTarget("MEYVE_SEBZE"),
            "DONUK_MEYVE_SEBZE" to fmtTarget("DONUK_MEYVE_SEBZE")
        )
    }

    val totalLiveRevenue = rowSpecs.sumOf { spec ->
        val raw = revenueInputs[spec.key].orEmpty().replace(',', '.').trim()
        raw.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    }
    val totalLiveFire = rowSpecs.sumOf { spec ->
        val revRaw = revenueInputs[spec.key].orEmpty().replace(',', '.').trim()
        if (revRaw.isBlank()) 0.0
        else fireInputs[spec.key].orEmpty().replace(',', '.').trim().toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    }
    val liveGeneralRatio = if (totalLiveRevenue > 0.0) {
        (totalLiveFire / totalLiveRevenue) * 100.0
    } else null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .testTag("bulk_continuous_audit_form")
    ) {
        // 1. ÜST BAŞLIK VE KAPAT (SABİT)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = TurquoisePrimary.copy(alpha = 0.14f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.FactCheck,
                            contentDescription = null,
                            tint = TurquoiseDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Sürekli Envanter Girişi",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    )
                    Text(
                        text = "Dönem belirleyip 4 kategoriyi tek seferde girin",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate700
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9))
                    .clickable { onDismiss() }
                    .testTag("bulk_continuous_close_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Kapat",
                    tint = Slate700,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        HorizontalDivider(color = Slate200.copy(alpha = 0.7f), thickness = 1.dp)

        // KAYDIRILABİLİR ORTA İÇERİK (Dönem, 4 Kategori Kartı, Not)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

        // 2. DÖNEM ALANI (Ay/Yıl Takvimi Seçici)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Dönem",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Slate800
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = periodText,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = {
                        Text(
                            text = "Dönem seçin (Örn: Kasım 2026)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate500
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = TurquoiseDark,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    trailingIcon = {
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = TurquoisePrimary.copy(alpha = 0.14f),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "Dönem Seç",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TurquoiseDark,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TurquoisePrimary,
                        unfocusedBorderColor = TurquoisePrimary.copy(alpha = 0.45f),
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bulk_continuous_period_input")
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showMonthYearPicker = true }
                        .testTag("bulk_continuous_period_picker_trigger")
                )
            }
        }

        // 3. 4 KATEGORİ SATIRI (ALT ALTA CİRO VE FİRE KUTULARI)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rowSpecs.forEach { spec ->
                val revText = revenueInputs[spec.key].orEmpty()
                val fireText = fireInputs[spec.key].orEmpty()
                val parsedRev = revText.replace(',', '.').trim().toDoubleOrNull() ?: 0.0
                val parsedFire = fireText.replace(',', '.').trim().toDoubleOrNull() ?: 0.0
                val rowRatio = if (parsedRev > 0.0) (parsedFire / parsedRev) * 100.0 else null

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bulk_row_${spec.key}"),
                    shape = RoundedCornerShape(16.dp),
                    color = spec.pastelBg.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, spec.borderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    border = BorderStroke(1.dp, spec.borderColor),
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = spec.icon,
                                            contentDescription = spec.label,
                                            tint = spec.iconTint,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = spec.label,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Slate900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (rowRatio != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = BorderStroke(0.9.dp, spec.borderColor)
                                ) {
                                    Text(
                                        text = "Oran: %${ratioFormatter.format(rowRatio)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = spec.iconTint,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        val targetText = targetInputs[spec.key].orEmpty()
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Ciro (₺)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1.2f)
                                )
                                Text(
                                    text = "Fire (₺)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpiredRedDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1.1f)
                                )
                                Text(
                                    text = "Hedef (%)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TurquoiseDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(0.9f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = revText,
                                    onValueChange = { input ->
                                        revenueInputs[spec.key] = input.filter { it.isDigit() || it == '.' || it == ',' }
                                        showValidationError = false
                                    },
                                    placeholder = {
                                        Text(
                                            text = "0",
                                            fontSize = 12.5.sp,
                                            color = Slate500
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Next
                                    ),
                                    textStyle = TextStyle(
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Slate900
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = spec.iconTint,
                                        unfocusedBorderColor = spec.borderColor,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .testTag("bulk_revenue_input_${spec.key}")
                                )

                                OutlinedTextField(
                                    value = fireText,
                                    onValueChange = { input ->
                                        fireInputs[spec.key] = input.filter { it.isDigit() || it == '.' || it == ',' }
                                    },
                                    placeholder = {
                                        Text(
                                            text = "0",
                                            fontSize = 12.5.sp,
                                            color = Slate500
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Next
                                    ),
                                    textStyle = TextStyle(
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ExpiredRedDark
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ExpiredRedDark,
                                        unfocusedBorderColor = spec.borderColor,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .testTag("bulk_fire_input_${spec.key}")
                                )

                                OutlinedTextField(
                                    value = targetText,
                                    onValueChange = { input ->
                                        targetInputs[spec.key] = input.filter { it.isDigit() || it == '.' || it == ',' }
                                    },
                                    placeholder = {
                                        Text(
                                            text = "1.0",
                                            fontSize = 12.5.sp,
                                            color = Slate500
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Next
                                    ),
                                    textStyle = TextStyle(
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TurquoiseDark
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = TurquoiseDark,
                                        unfocusedBorderColor = spec.borderColor,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .testTag("bulk_target_input_${spec.key}")
                                )
                            }
                        }
                    }
                }
            }
        }

        // CANLI GENEL TOPLAM ÖZETİ
        if (liveGeneralRatio != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bulk_continuous_live_summary"),
                shape = RoundedCornerShape(14.dp),
                color = TurquoisePrimary.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Toplam Ciro: ${moneyFormatter.format(totalLiveRevenue)} ₺",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        Text(
                            text = "Toplam Fire: ${moneyFormatter.format(totalLiveFire)} ₺",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ExpiredRedDark
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Genel Oran",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate700
                        )
                        Text(
                            text = "%${ratioFormatter.format(liveGeneralRatio)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = TurquoiseDark
                        )
                    }
                }
            }
        }

        // 4. NOT (OPSİYONEL)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Not (Opsiyonel)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Slate800
            )
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                placeholder = {
                    Text(
                        text = "Örn: Kasım dönemi taze ürün envanter sonuçları...",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Notes,
                        contentDescription = null,
                        tint = TurquoiseDark,
                        modifier = Modifier.size(19.dp)
                    )
                },
                textStyle = TextStyle(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate900
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TurquoisePrimary,
                    unfocusedBorderColor = Slate200,
                    focusedContainerColor = Slate50,
                    unfocusedContainerColor = Slate50
                ),
                minLines = 2,
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_inventory_audit_notes_input")
            )
        }

        if (showValidationError) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = ExpiredRedContainer,
                border = BorderStroke(1.dp, ExpiredRedBorder)
            ) {
                Text(
                    text = "Lütfen kaydetmek için en az bir kategorinin Ciro kutusunu doldurun.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ExpiredRedDark,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
        } // Kaydırılabilir orta içerik sonu

        HorizontalDivider(color = Slate200.copy(alpha = 0.7f), thickness = 1.dp)

        // 5. SABİT ALT KAYDET BUTONU ALANI
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = {
                    val entries = rowSpecs.mapNotNull { spec ->
                        val rawRev = revenueInputs[spec.key].orEmpty().trim()
                        if (rawRev.isEmpty()) {
                            null
                        } else {
                            val parsedRev = rawRev.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0)
                            if (parsedRev == null) {
                                null
                            } else {
                                val rawFire = fireInputs[spec.key].orEmpty().trim()
                                val parsedFire = rawFire.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                                val rawTarget = targetInputs[spec.key].orEmpty().trim()
                                val parsedTarget = rawTarget.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 }
                                    ?: initialCategoryTargetRatios[spec.key]?.takeIf { it > 0.0 }
                                    ?: 1.0
                                BulkContinuousAuditInput(
                                    category = spec.key,
                                    revenue = parsedRev,
                                    fireCost = parsedFire,
                                    targetRatio = parsedTarget
                                )
                            }
                        }
                    }
                    if (entries.isEmpty()) {
                        showValidationError = true
                    } else {
                        onSaveBulk(periodText.trim(), notesText.trim(), entries)
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_bulk_continuous_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TurquoiseDark)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tümünü Kaydet",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
    }
}
