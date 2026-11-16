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
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.data.InventoryAuditEntity
import com.example.ui.theme.ExpiredRedBorder
import com.example.ui.theme.ExpiredRedContainer
import com.example.ui.theme.ExpiredRedDark
import com.example.ui.theme.NormalGreenBorder
import com.example.ui.theme.NormalGreenContainer
import com.example.ui.theme.NormalGreenDark
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
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ModalCategoryChipSpec(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val activeBg: Color,
    val activeText: Color,
    val activeBorder: Color
)

private fun formatEditableNumber(value: Double): String {
    if (value <= 0.0) return ""
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        value.toString()
    }
}

private fun formatContinuousCategoryHeaderName(rawCategory: String): String {
    return when (com.example.ui.viewmodel.normalizeInventoryCategoryKey(rawCategory)) {
        "ET_TAVUK" -> "Et-Tavuk"
        "EKMEK" -> "Ekmek"
        "MEYVE_SEBZE" -> "Meyve-Sebze"
        "DONUK_MEYVE_SEBZE" -> "Donuk Meyve-Sebze"
        else -> rawCategory.trim().ifBlank { "Sürekli Envanter" }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInventoryAuditModal(
    initialCategory: String = "GIDA",
    selectedCategory: String? = null,
    editingAudit: InventoryAuditEntity? = null,
    isWeeklyUpdate: Boolean = false,
    isWeeklyEntryMode: Boolean = false,
    isContinuousInventory: Boolean = false,
    isBulkContinuous: Boolean = false,
    initialCategoryTargetRatios: Map<String, Double> = emptyMap(),
    initialAuditPeriod: String = "",
    lockedAuditDateMillis: Long? = null,
    initialTargetRatio: Double? = null,
    onDismiss: () -> Unit,
    onSave: (
        category: String,
        auditDateMillis: Long,
        revenue: Double,
        fireCost: Double,
        targetRatio: Double,
        notes: String,
        auditPeriod: String
    ) -> Unit,
    onSaveBulkContinuous: (
        auditPeriod: String,
        notes: String,
        entries: List<BulkContinuousAuditInput>
    ) -> Unit = { _, _, _ -> }
) {
    val effectiveWeeklyUpdate = isWeeklyUpdate || isWeeklyEntryMode
    val isEditMode = editingAudit != null && !effectiveWeeklyUpdate
    val effectiveBulkMode = isBulkContinuous || (isContinuousInventory && selectedCategory == null)
    val isSingleLockedContinuousCategory = isContinuousInventory && !effectiveBulkMode && selectedCategory != null

    var chosenCategory by remember(initialCategory, selectedCategory, editingAudit?.id, effectiveWeeklyUpdate) {
        mutableStateOf(
            editingAudit?.category?.ifBlank { selectedCategory ?: initialCategory }
                ?: selectedCategory?.ifBlank { initialCategory }
                ?: initialCategory.ifBlank { "GIDA" }
        )
    }
    var selectedDateMillis by remember(editingAudit?.id, editingAudit?.auditDateMillis, lockedAuditDateMillis, effectiveWeeklyUpdate) {
        mutableLongStateOf(
            when {
                effectiveWeeklyUpdate && lockedAuditDateMillis != null -> lockedAuditDateMillis
                editingAudit != null -> editingAudit.auditDateMillis
                else -> System.currentTimeMillis()
            }
        )
    }
    var periodText by remember(editingAudit?.id, editingAudit?.auditPeriod, initialAuditPeriod, effectiveWeeklyUpdate, isContinuousInventory) {
        mutableStateOf(
            when {
                isEditMode && editingAudit != null -> editingAudit.auditPeriod
                isContinuousInventory && initialAuditPeriod.isNotBlank() -> initialAuditPeriod
                else -> ""
            }
        )
    }
    var revenueText by remember(editingAudit?.id, editingAudit?.revenue, effectiveWeeklyUpdate) {
        mutableStateOf(if (isEditMode && editingAudit != null) formatEditableNumber(editingAudit.revenue) else "")
    }
    var fireCostText by remember(editingAudit?.id, editingAudit?.fireCost, effectiveWeeklyUpdate) {
        mutableStateOf(if (isEditMode && editingAudit != null) formatEditableNumber(editingAudit.fireCost) else "")
    }
    var targetRatioText by remember(editingAudit?.id, editingAudit?.targetRatio, initialTargetRatio, effectiveWeeklyUpdate) {
        mutableStateOf(
            when {
                isEditMode && editingAudit != null && editingAudit.targetRatio > 0.0 ->
                    formatEditableNumber(editingAudit.targetRatio)
                initialTargetRatio != null && initialTargetRatio > 0.0 ->
                    formatEditableNumber(initialTargetRatio)
                else -> "1.0"
            }
        )
    }
    var notesText by remember(editingAudit?.id, editingAudit?.notes, effectiveWeeklyUpdate) {
        mutableStateOf(if (isEditMode && editingAudit != null) editingAudit.notes else "")
    }
    var showCalendarDialog by remember { mutableStateOf(false) }
    var showMonthYearPickerDialog by remember { mutableStateOf(false) }

    val categoryOptions = remember(isContinuousInventory) {
        if (isContinuousInventory) {
            listOf(
                ModalCategoryChipSpec(
                    key = "ET_TAVUK",
                    label = "ET-TAVUK",
                    icon = Icons.Default.SetMeal,
                    activeBg = Color(0xFFFFF1F2),
                    activeText = Color(0xFFBE123C),
                    activeBorder = Color(0xFFFB7185)
                ),
                ModalCategoryChipSpec(
                    key = "EKMEK",
                    label = "EKMEK",
                    icon = Icons.Default.BakeryDining,
                    activeBg = Color(0xFFFFFBEB),
                    activeText = Color(0xFFB45309),
                    activeBorder = Color(0xFFFBBF24)
                ),
                ModalCategoryChipSpec(
                    key = "MEYVE_SEBZE",
                    label = "MEYVE-SEBZE",
                    icon = Icons.Default.Eco,
                    activeBg = Color(0xFFF0FDF4),
                    activeText = Color(0xFF15803D),
                    activeBorder = Color(0xFF4ADE80)
                ),
                ModalCategoryChipSpec(
                    key = "DONUK_MEYVE_SEBZE",
                    label = "DONUK MEYVE-SEBZE",
                    icon = Icons.Default.AcUnit,
                    activeBg = Color(0xFFF0F9FF),
                    activeText = Color(0xFF0369A1),
                    activeBorder = Color(0xFF38BDF8)
                )
            )
        } else {
            listOf(
                ModalCategoryChipSpec(
                    key = "GIDA",
                    label = "GIDA",
                    icon = Icons.Default.Fastfood,
                    activeBg = Color(0xFFF0FDFA),
                    activeText = TurquoiseDark,
                    activeBorder = TurquoisePrimary
                ),
                ModalCategoryChipSpec(
                    key = "DOLAP",
                    label = "DOLAP",
                    icon = Icons.Default.AcUnit,
                    activeBg = Color(0xFFEFF6FF),
                    activeText = Color(0xFF1D4ED8),
                    activeBorder = Color(0xFF60A5FA)
                ),
                ModalCategoryChipSpec(
                    key = "GIDA DIŞI",
                    label = "GIDA DIŞI",
                    icon = Icons.Default.Category,
                    activeBg = Color(0xFFF5F3FF),
                    activeText = Color(0xFF6D28D9),
                    activeBorder = Color(0xFFA78BFA)
                )
            )
        }
    }

    val dateFormatter = remember {
        SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("tr-TR"))
    }
    val formattedSelectedDate = remember(selectedDateMillis) {
        dateFormatter.format(Date(selectedDateMillis))
    }

    val moneyFormatter = remember {
        val symbols = DecimalFormatSymbols(Locale.forLanguageTag("tr-TR")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        DecimalFormat("#,##0.##", symbols)
    }

    val ratioFormatter = remember {
        val symbols = DecimalFormatSymbols(Locale.forLanguageTag("tr-TR")).apply {
            decimalSeparator = ','
        }
        DecimalFormat("0.##", symbols)
    }

    val parsedRevenue = remember(revenueText) {
        revenueText.replace(',', '.').trim().toDoubleOrNull() ?: 0.0
    }
    val parsedFireCost = remember(fireCostText) {
        fireCostText.replace(',', '.').trim().toDoubleOrNull() ?: 0.0
    }
    val parsedTargetRatio = remember(targetRatioText) {
        targetRatioText.replace(',', '.').trim().toDoubleOrNull() ?: 0.0
    }

    val showLiveFinancialBox = parsedRevenue > 0.0 && (fireCostText.isNotBlank() || parsedTargetRatio > 0.0)
    val maxAllowedFire = remember(parsedRevenue, parsedTargetRatio) {
        parsedRevenue * (parsedTargetRatio / 100.0)
    }
    val actualRatioPercent = remember(parsedRevenue, parsedFireCost) {
        if (parsedRevenue > 0.0) (parsedFireCost / parsedRevenue) * 100.0 else 0.0
    }
    val isOverTarget = parsedFireCost > maxAllowedFire

    if (showCalendarDialog && !effectiveWeeklyUpdate && !isContinuousInventory && !effectiveBulkMode) {
        CustomBoxedCalendarDialog(
            initialDateMillis = selectedDateMillis,
            onDismissRequest = { showCalendarDialog = false },
            onDateSelected = { pickedMillis ->
                selectedDateMillis = pickedMillis
                showCalendarDialog = false
            }
        )
    }

    if (showMonthYearPickerDialog && isContinuousInventory && !effectiveBulkMode) {
        CustomMonthYearPickerDialog(
            initialPeriod = periodText,
            onDismissRequest = { showMonthYearPickerDialog = false },
            onMonthYearSelected = { chosenPeriod ->
                periodText = chosenPeriod
                showMonthYearPickerDialog = false
            }
        )
    }

    AppBottomSheetWrapper(
        onDismissRequest = onDismiss,
        enableVerticalScroll = false
    ) { dismissSheet ->
        if (effectiveBulkMode) {
            BulkContinuousAuditSection(
                moneyFormatter = moneyFormatter,
                ratioFormatter = ratioFormatter,
                initialCategoryTargetRatios = initialCategoryTargetRatios,
                onDismiss = { dismissSheet() },
                onSaveBulk = onSaveBulkContinuous
            )
        } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            // 1. ÜST BAŞLIK VE KAPAT BUTONU (SABİT)
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
                                imageVector = if (isEditMode) Icons.Default.Edit else Icons.AutoMirrored.Filled.FactCheck,
                                contentDescription = null,
                                tint = TurquoiseDark,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = when {
                                isSingleLockedContinuousCategory && isEditMode ->
                                    "${formatContinuousCategoryHeaderName(selectedCategory!!)} Kaydını Düzenle"
                                isSingleLockedContinuousCategory ->
                                    "${formatContinuousCategoryHeaderName(selectedCategory!!)} Veri Girişi"
                                effectiveWeeklyUpdate -> "Yeni Veri Gir"
                                isEditMode -> "Envanter Kaydını Düzenle"
                                else -> "Envanter & Finansal Kayıt"
                            },
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900
                        )
                        if (!effectiveWeeklyUpdate || isSingleLockedContinuousCategory) {
                            Text(
                                text = when {
                                    isEditMode -> "Son girilen kaydın ciro, fire ve hedef oranını düzeltin"
                                    isSingleLockedContinuousCategory -> "Dönem seçerek ciro, fire ve hedef oranı girin"
                                    else -> "Ciro, fire tutarı ve hedef oranı girin"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate700
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable { dismissSheet() }
                        .testTag("add_inventory_audit_close_button"),
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

            // KAYDIRILABİLİR ORTA İÇERİK
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

            // 2. KATEGORİ SEÇİM ÇİPLERİ (Tekli Sürekli Envanter Modunda ve Haftalık Güncellemede Gizlenir)
            if (!effectiveWeeklyUpdate && !isSingleLockedContinuousCategory) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Kategori Seçimi",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )

                    val chunkSize = if (categoryOptions.size > 3) 2 else categoryOptions.size
                    categoryOptions.chunked(chunkSize).forEach { rowOptions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowOptions.forEach { option ->
                                val isSelected = chosenCategory.equals(option.key, ignoreCase = true)
                                Surface(
                                    onClick = { chosenCategory = option.key },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) option.activeBg else Slate50,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.6.dp else 1.dp,
                                        color = if (isSelected) option.activeBorder else Slate200
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("audit_category_chip_${option.key}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 6.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = option.icon,
                                            contentDescription = option.label,
                                            tint = if (isSelected) option.activeText else Slate700,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = option.label,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                            color = if (isSelected) option.activeText else Slate700,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. ENVANTER TARİHİ ALANI (Sadece Periyodik Envanterde)
                if (!isContinuousInventory) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Envanter Tarihi",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )

                        Surface(
                            onClick = { showCalendarDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            color = Slate50,
                            border = BorderStroke(1.2.dp, TurquoisePrimary.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_inventory_audit_date_button")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = TurquoiseDark,
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = formattedSelectedDate,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Slate900
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = TurquoisePrimary.copy(alpha = 0.14f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EditCalendar,
                                            contentDescription = "Tarih Değiştir",
                                            tint = TurquoiseDark,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Tarih Seç",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TurquoiseDark
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3.B DÖNEM ALANI (Sadece Sürekli Envanterde gösterilir, Ay/Yıl seçici açılır)
            if (isContinuousInventory) {
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
                                .testTag("add_inventory_audit_period_input")
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { showMonthYearPickerDialog = true }
                                .testTag("add_inventory_audit_period_picker_trigger")
                        )
                    }
                }
            }

            // 4. FİNANSAL GİRİŞLER: CİRO (₺), FİRE TUTARI (₺), HEDEF ORAN (%)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Ciro (₺)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Ciro (₺)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )
                    OutlinedTextField(
                        value = revenueText,
                        onValueChange = { input ->
                            revenueText = input.filter { it.isDigit() || it == '.' || it == ',' }
                        },
                        placeholder = {
                            Text(
                                text = "Örn: 500000",
                                fontSize = 12.5.sp,
                                color = Slate500
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = TurquoiseDark,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TurquoisePrimary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Slate50,
                            unfocusedContainerColor = Slate50
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("audit_revenue_input")
                    )
                }

                // Fire Tutarı (₺)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Fire Tutarı (₺)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )
                    OutlinedTextField(
                        value = fireCostText,
                        onValueChange = { input ->
                            fireCostText = input.filter { it.isDigit() || it == '.' || it == ',' }
                        },
                        placeholder = {
                            Text(
                                text = "Örn: 3500",
                                fontSize = 12.5.sp,
                                color = Slate500
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = ExpiredRedDark,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TurquoisePrimary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Slate50,
                            unfocusedContainerColor = Slate50
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("audit_fire_cost_input")
                    )
                }
            }

            // Hedef Oran (%) - Hem Periyodik hem Sürekli Envanterde düzenlenebilir
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Hedef Oran (%)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800
                )
                OutlinedTextField(
                    value = targetRatioText,
                    onValueChange = { input ->
                        targetRatioText = input.filter { it.isDigit() || it == '.' || it == ',' }
                    },
                    placeholder = {
                        Text(
                            text = "Örn: 1.0",
                            fontSize = 12.5.sp,
                            color = Slate500
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Percent,
                            contentDescription = null,
                            tint = TurquoiseDark,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate900
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TurquoisePrimary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("audit_target_ratio_input")
                )
            }

            // 5. CANLI FİNANSAL ANALİZ KUTUSU
            if (showLiveFinancialBox) {
                    val boxBg = if (isOverTarget) ExpiredRedContainer else NormalGreenContainer
                    val boxBorder = if (isOverTarget) ExpiredRedBorder else NormalGreenBorder
                    val boxText = if (isOverTarget) ExpiredRedDark else NormalGreenDark
                    val remainingFireAllowance = maxOf(0.0, maxAllowedFire - parsedFireCost)
                    val excessFireAmount = maxOf(0.0, parsedFireCost - maxAllowedFire)

                    val analysisMessage = if (isOverTarget) {
                        "⚠️ Hedefi aştınız! Maksimum ${moneyFormatter.format(maxAllowedFire)} TL fire yapmalıydınız (${moneyFormatter.format(excessFireAmount)} TL aşım)."
                    } else {
                        "✅ Harika! Hedefin altındasınız, ${moneyFormatter.format(remainingFireAllowance)} TL daha fire yapabilirsiniz."
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("live_financial_analysis_box"),
                        shape = RoundedCornerShape(16.dp),
                        color = boxBg,
                        border = BorderStroke(1.2.dp, boxBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = analysisMessage,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = boxText,
                                lineHeight = 18.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Gerçekleşen Oran: %${ratioFormatter.format(actualRatioPercent)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = boxText
                                )
                                Text(
                                    text = "Hedef: %${ratioFormatter.format(parsedTargetRatio)} (${moneyFormatter.format(maxAllowedFire)} ₺)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = boxText
                                )
                            }
                        }
                    }
            }

            // 6. NOT / AÇIKLAMA ALANI
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
                            text = "Örn: Dönem sonu genel sayım farkları...",
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
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TurquoisePrimary,
                        unfocusedBorderColor = Color.Transparent,
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
            } // Kaydırılabilir orta içerik sonu

            HorizontalDivider(color = Slate200.copy(alpha = 0.7f), thickness = 1.dp)

            // 7. SABİT KAYDET BUTONU ALANI
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = {
                        val effectiveDateMillis = if (isContinuousInventory) {
                            System.currentTimeMillis()
                        } else {
                            selectedDateMillis
                        }
                        val effectivePeriod = if (isContinuousInventory) {
                            periodText.trim()
                        } else {
                            ""
                        }
                        onSave(
                            chosenCategory,
                            effectiveDateMillis,
                            parsedRevenue,
                            parsedFireCost,
                            parsedTargetRatio,
                            notesText.trim(),
                            effectivePeriod
                        )
                        dismissSheet()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_inventory_audit_button"),
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
                        text = if (isEditMode) "Değişiklikleri Kaydet" else "Kaydet",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
        }
    }
}
