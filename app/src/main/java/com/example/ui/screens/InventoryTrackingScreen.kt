package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InventoryAuditEntity
import com.example.data.InventoryTargetPreferences
import com.example.ui.components.AddInventoryAuditModal
import com.example.ui.components.InventoryCategoryDetailModal
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.TurquoiseDark
import com.example.ui.theme.TurquoisePrimary
import com.example.ui.viewmodel.BulkContinuousAuditInput
import com.example.ui.viewmodel.CombinedInventorySummary
import com.example.ui.viewmodel.computeCombinedInventorySummary
import com.example.ui.viewmodel.isContinuousAuditCategory
import com.example.ui.viewmodel.normalizeInventoryCategoryKey
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@Composable
fun InventoryTrackingScreen(
    audits: List<InventoryAuditEntity>,
    combinedSummary: CombinedInventorySummary = remember(audits) {
        computeCombinedInventorySummary(audits)
    },
    onRecordAudit: (
        category: String,
        auditDateMillis: Long,
        revenue: Double,
        fireCost: Double,
        targetRatio: Double,
        notes: String,
        auditPeriod: String
    ) -> Unit,
    onRecordBulkContinuousAudits: (
        auditPeriod: String,
        notes: String,
        entries: List<BulkContinuousAuditInput>
    ) -> Unit = { _, _, _ -> },
    onUpdateAudit: (InventoryAuditEntity) -> Unit = {},
    onDeleteAudit: (id: Int) -> Unit = {},
    onBackClick: () -> Unit
) {
    BackHandler {
        onBackClick()
    }

    val context = LocalContext.current
    val todayMidnight by com.example.util.rememberTodayMidnight()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedDetailCategory by remember { mutableStateOf<String?>(null) }
    var activeCategoryForAddModal by remember { mutableStateOf<String?>(null) }
    var editingAuditForModal by remember { mutableStateOf<InventoryAuditEntity?>(null) }
    var isWeeklyEntryModalMode by remember { mutableStateOf(false) }
    var isContinuousModalMode by remember { mutableStateOf(false) }
    var isBulkContinuousModalMode by remember { mutableStateOf(false) }
    var weeklyLockedDateMillis by remember { mutableStateOf<Long?>(null) }
    var weeklyInitialTargetRatio by remember { mutableStateOf<Double?>(null) }
    var singleAuditInitialPeriod by remember { mutableStateOf("") }
    var selectedContinuousCategoryForModal by remember { mutableStateOf<String?>(null) }
    var storeCombinedTargetRatio by remember {
        mutableDoubleStateOf(InventoryTargetPreferences.getStoreCombinedTargetRatio(context))
    }
    var continuousTotalTargetRatio by remember {
        mutableDoubleStateOf(InventoryTargetPreferences.getContinuousTotalTargetRatio(context))
    }
    var editingTargetDialogType by remember { mutableStateOf<String?>(null) }

    val periodicCategories = remember {
        listOf(
            AuditCategorySpec(
                key = "GIDA",
                title = "GIDA",
                icon = Icons.Default.Fastfood,
                pastelBg = Color(0xFFF0FDFA),
                iconTint = TurquoiseDark
            ),
            AuditCategorySpec(
                key = "DOLAP",
                title = "DOLAP",
                icon = Icons.Default.AcUnit,
                pastelBg = Color(0xFFEFF6FF),
                iconTint = Color(0xFF1D4ED8)
            ),
            AuditCategorySpec(
                key = "GIDA DIŞI",
                title = "GIDA DIŞI",
                icon = Icons.Default.Category,
                pastelBg = Color(0xFFF5F3FF),
                iconTint = Color(0xFF6D28D9)
            )
        )
    }

    val continuousCategories = remember {
        listOf(
            AuditCategorySpec(
                key = "ET_TAVUK",
                title = "ET-TAVUK",
                icon = Icons.Default.SetMeal,
                pastelBg = Color(0xFFFFF1F2),
                iconTint = Color(0xFFBE123C)
            ),
            AuditCategorySpec(
                key = "EKMEK",
                title = "EKMEK",
                icon = Icons.Default.BakeryDining,
                pastelBg = Color(0xFFFFFBEB),
                iconTint = Color(0xFFB45309)
            ),
            AuditCategorySpec(
                key = "MEYVE_SEBZE",
                title = "MEYVE-SEBZE",
                icon = Icons.Default.Eco,
                pastelBg = Color(0xFFF0FDF4),
                iconTint = Color(0xFF15803D)
            ),
            AuditCategorySpec(
                key = "DONUK_MEYVE_SEBZE",
                title = "DONUK MEYVE-SEBZE",
                icon = Icons.Default.AcUnit,
                pastelBg = Color(0xFFF0F9FF),
                iconTint = Color(0xFF0369A1)
            )
        )
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

    // Kategori kartına tıklandığında açılan Periyodik Envanter Detay Alt Sayfası
    if (selectedDetailCategory != null && activeCategoryForAddModal == null && editingAuditForModal == null) {
        val currentCat = selectedDetailCategory!!
        val sortedForCategory = remember(audits, currentCat) {
            audits.filter {
                normalizeInventoryCategoryKey(it.category) == normalizeInventoryCategoryKey(currentCat)
            }.sortedWith(
                compareByDescending<InventoryAuditEntity> { it.updatedAt }
                    .thenByDescending { it.auditDateMillis }
                    .thenByDescending { it.createdAt }
            )
        }
        val latestForCategory = sortedForCategory.firstOrNull()
        val initialAuditDateForCategory = sortedForCategory.lastOrNull()?.auditDateMillis
            ?: latestForCategory?.auditDateMillis
        InventoryCategoryDetailModal(
            category = formatAuditCategoryLabel(currentCat),
            latestAudit = latestForCategory,
            categoryHistory = sortedForCategory,
            targetRatio = latestForCategory?.targetRatio?.takeIf { it > 0.0 } ?: 1.0,
            onDismiss = { selectedDetailCategory = null },
            onEditClick = { auditToEdit ->
                isWeeklyEntryModalMode = false
                isContinuousModalMode = isContinuousAuditCategory(currentCat)
                isBulkContinuousModalMode = false
                selectedContinuousCategoryForModal = if (isContinuousAuditCategory(currentCat)) currentCat else null
                weeklyLockedDateMillis = null
                weeklyInitialTargetRatio = null
                singleAuditInitialPeriod = if (isContinuousAuditCategory(currentCat)) {
                    latestForCategory?.auditPeriod.orEmpty()
                } else {
                    ""
                }
                if (auditToEdit != null) {
                    editingAuditForModal = auditToEdit
                } else {
                    activeCategoryForAddModal = currentCat
                }
            },
            onAddNewAuditClick = {
                editingAuditForModal = null
                activeCategoryForAddModal = currentCat
                isWeeklyEntryModalMode = true
                isContinuousModalMode = isContinuousAuditCategory(currentCat)
                isBulkContinuousModalMode = false
                selectedContinuousCategoryForModal = if (isContinuousAuditCategory(currentCat)) currentCat else null
                weeklyLockedDateMillis = initialAuditDateForCategory ?: System.currentTimeMillis()
                weeklyInitialTargetRatio = latestForCategory?.targetRatio?.takeIf { it > 0.0 } ?: 1.0
                singleAuditInitialPeriod = if (isContinuousAuditCategory(currentCat)) {
                    latestForCategory?.auditPeriod.orEmpty()
                } else {
                    ""
                }
            }
        )
    }

    // Yeni Sayım Ekleme, Toplu Sürekli Envanter, Haftalık Veri Girme veya Mevcut Sayımı Düzenleme Modalı
    if (activeCategoryForAddModal != null || editingAuditForModal != null) {
        val targetEdit = editingAuditForModal
        val initialCat = targetEdit?.category ?: activeCategoryForAddModal ?: "GIDA"
        val resolvedContinuous = isContinuousModalMode || isContinuousAuditCategory(initialCat)
        val continuousTargetRatios = remember(audits) {
            val comparator = compareByDescending<InventoryAuditEntity> { it.updatedAt }
                .thenByDescending { it.auditDateMillis }
                .thenByDescending { it.createdAt }
            continuousCategories.associate { spec ->
                val normalizedSpecKey = normalizeInventoryCategoryKey(spec.key)
                val latest = audits
                    .filter { normalizeInventoryCategoryKey(it.category) == normalizedSpecKey }
                    .sortedWith(comparator)
                    .firstOrNull()
                spec.key to (latest?.targetRatio?.takeIf { it > 0.0 } ?: 1.0)
            }
        }
        AddInventoryAuditModal(
            initialCategory = initialCat,
            selectedCategory = if (resolvedContinuous && !isBulkContinuousModalMode) {
                selectedContinuousCategoryForModal
            } else {
                null
            },
            editingAudit = targetEdit,
            isWeeklyUpdate = isWeeklyEntryModalMode,
            isContinuousInventory = resolvedContinuous,
            isBulkContinuous = isBulkContinuousModalMode,
            initialCategoryTargetRatios = continuousTargetRatios,
            initialAuditPeriod = singleAuditInitialPeriod,
            lockedAuditDateMillis = if (resolvedContinuous) null else weeklyLockedDateMillis,
            initialTargetRatio = weeklyInitialTargetRatio,
            onDismiss = {
                activeCategoryForAddModal = null
                editingAuditForModal = null
                isWeeklyEntryModalMode = false
                isContinuousModalMode = false
                isBulkContinuousModalMode = false
                selectedContinuousCategoryForModal = null
                weeklyLockedDateMillis = null
                weeklyInitialTargetRatio = null
                singleAuditInitialPeriod = ""
            },
                    onSave = { chosenCategory, pickedDateMillis, revenue, fireCost, targetRatio, notes, auditPeriod ->
                val catLabel = formatAuditCategoryLabel(chosenCategory)
                if (targetEdit != null && !isWeeklyEntryModalMode) {
                    val updated = targetEdit.copy(
                        category = chosenCategory,
                        auditDateMillis = pickedDateMillis,
                        revenue = revenue,
                        fireCost = fireCost,
                        targetRatio = targetRatio,
                        notes = notes,
                        auditPeriod = auditPeriod
                    )
                    onUpdateAudit(updated)
                    selectedDetailCategory = chosenCategory
                    editingAuditForModal = null
                    isBulkContinuousModalMode = false
                    selectedContinuousCategoryForModal = null
                    singleAuditInitialPeriod = ""
                    Toast.makeText(
                        context,
                        "$catLabel kaydı düzeltildi",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val isContinuousSave = isContinuousModalMode || isContinuousAuditCategory(chosenCategory)
                    val effectiveAuditDate = if (!isContinuousSave && isWeeklyEntryModalMode && weeklyLockedDateMillis != null) {
                        weeklyLockedDateMillis!!
                    } else {
                        pickedDateMillis
                    }
                    onRecordAudit(
                        chosenCategory,
                        effectiveAuditDate,
                        revenue,
                        fireCost,
                        targetRatio,
                        notes,
                        auditPeriod
                    )
                    if (!isContinuousSave) {
                        selectedDetailCategory = chosenCategory
                    }
                    activeCategoryForAddModal = null
                    isWeeklyEntryModalMode = false
                    isContinuousModalMode = false
                    isBulkContinuousModalMode = false
                    selectedContinuousCategoryForModal = null
                    weeklyLockedDateMillis = null
                    weeklyInitialTargetRatio = null
                    singleAuditInitialPeriod = ""
                    Toast.makeText(
                        context,
                        if (isContinuousSave) "$catLabel sürekli envanter verisi kaydedildi" else "$catLabel haftalık verisi kaydedildi",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onSaveBulkContinuous = { auditPeriod, notes, entries ->
                onRecordBulkContinuousAudits(auditPeriod, notes, entries)
                activeCategoryForAddModal = null
                editingAuditForModal = null
                isWeeklyEntryModalMode = false
                isContinuousModalMode = false
                isBulkContinuousModalMode = false
                selectedContinuousCategoryForModal = null
                weeklyLockedDateMillis = null
                weeklyInitialTargetRatio = null
                singleAuditInitialPeriod = ""
                val periodLabel = auditPeriod.trim().ifBlank { "Güncel" }
                Toast.makeText(
                    context,
                    "$periodLabel dönemi verileri başarıyla kaydedildi",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    if (editingTargetDialogType != null) {
        val isCombinedDialog = editingTargetDialogType == "COMBINED"
        TargetRatioEditDialog(
            title = if (isCombinedDialog) "Mağaza Genel Hedef Oranı" else "Sürekli Envanter Toplam Hedefi",
            subtitle = if (isCombinedDialog) {
                "Birleşik Mağaza Envanteri (T) toplam oranı için hedef yüzdeyi belirleyin."
            } else {
                "Et-Tavuk, Ekmek ve Meyve-Sebze toplam oranı için hedef yüzdeyi belirleyin."
            },
            initialValue = if (isCombinedDialog) storeCombinedTargetRatio else continuousTotalTargetRatio,
            onDismiss = { editingTargetDialogType = null },
            onSave = { newTarget ->
                if (isCombinedDialog) {
                    InventoryTargetPreferences.setStoreCombinedTargetRatio(context, newTarget)
                    storeCombinedTargetRatio = newTarget
                } else {
                    InventoryTargetPreferences.setContinuousTotalTargetRatio(context, newTarget)
                    continuousTotalTargetRatio = newTarget
                }
                editingTargetDialogType = null
                Toast.makeText(
                    context,
                    "Hedef oran %${ratioFormatter.format(newTarget)} olarak güncellendi",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .testTag("inventory_tracking_screen")
    ) {
        // 1. ÜST BAR: "Envanter Takibi" başlığı + Yeni Ekle butonu
        Surface(
            color = Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Slate50,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { onBackClick() }
                            .testTag("inventory_tracking_back_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Geri",
                                tint = Slate900,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Envanter Takibi",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Parçalı (90g) & Sürekli (Haftalık) analiz",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate700,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    onClick = {
                        editingAuditForModal = null
                        isWeeklyEntryModalMode = false
                        val isContinuousTabSelected = selectedTabIndex == 1
                        isContinuousModalMode = isContinuousTabSelected
                        isBulkContinuousModalMode = isContinuousTabSelected
                        selectedContinuousCategoryForModal = null
                        weeklyLockedDateMillis = null
                        weeklyInitialTargetRatio = null
                        singleAuditInitialPeriod = ""
                        activeCategoryForAddModal = if (isContinuousTabSelected) "ET_TAVUK" else "GIDA"
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = TurquoisePrimary.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, TurquoisePrimary.copy(alpha = 0.35f)),
                    modifier = Modifier.testTag("inventory_tracking_add_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Envanter Ekle",
                            tint = TurquoiseDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ekle",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TurquoiseDark
                        )
                    }
                }
            }
        }

        // 2. EN ÜST PANEL: MAĞAZA BİRLEŞİK ENVANTER ÖZETİ KARTI (T, P, S)
        CombinedStoreInventoryCard(
            summary = combinedSummary,
            combinedTargetRatio = storeCombinedTargetRatio,
            continuousTargetRatio = continuousTotalTargetRatio,
            onEditCombinedTargetClick = { editingTargetDialogType = "COMBINED" },
            moneyFormatter = moneyFormatter,
            ratioFormatter = ratioFormatter,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )

        // 3. SEKME YAPISI: "Periyodik Envanter" / "Sürekli Envanter"
        InventoryTrackingTabRow(
            selectedTabIndex = selectedTabIndex,
            onTabSelected = { selectedTabIndex = it },
            modifier = Modifier.padding(horizontal = 14.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 4. SEKMELER ARASI YUMUŞAK GEÇİŞLİ İÇERİK (AnimatedContent)
        AnimatedContent(
            targetState = selectedTabIndex,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 250)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = 250))
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            label = "inventory_tracking_tab_transition"
        ) { currentTab ->
            val isContinuousTab = currentTab == 1
            val tabCategories = if (isContinuousTab) continuousCategories else periodicCategories
            val latestByCategory = remember(audits, tabCategories) {
                val comparator = compareByDescending<InventoryAuditEntity> { it.updatedAt }
                    .thenByDescending { it.auditDateMillis }
                    .thenByDescending { it.createdAt }
                tabCategories.associate { spec ->
                    val normalizedSpecKey = normalizeInventoryCategoryKey(spec.key)
                    val latest = audits
                        .filter { normalizeInventoryCategoryKey(it.category) == normalizedSpecKey }
                        .sortedWith(comparator)
                        .firstOrNull()
                    spec.key to latest
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // KATEGORİ KARTLARI
                if (isContinuousTab) {
                    ContinuousTotalTargetStrip(
                        continuousTargetRatio = continuousTotalTargetRatio,
                        ratioFormatter = ratioFormatter,
                        onEditClick = { editingTargetDialogType = "CONTINUOUS" }
                    )

                    ContinuousCategoryCardsRow(
                        categories = continuousCategories,
                        audits = audits,
                        moneyFormatter = moneyFormatter,
                        ratioFormatter = ratioFormatter,
                        onCategoryClick = { spec, latestAudit ->
                            editingAuditForModal = null
                            activeCategoryForAddModal = spec.key
                            selectedContinuousCategoryForModal = spec.key
                            isWeeklyEntryModalMode = false
                            isContinuousModalMode = true
                            isBulkContinuousModalMode = false
                            weeklyLockedDateMillis = null
                            weeklyInitialTargetRatio = latestAudit?.targetRatio?.takeIf { it > 0.0 } ?: 1.0
                            singleAuditInitialPeriod = latestAudit?.auditPeriod.orEmpty()
                        }
                    )

                    ContinuousInventoryTotalCard(
                        latestAudits = continuousCategories.mapNotNull { latestByCategory[it.key] },
                        continuousTargetRatio = continuousTotalTargetRatio,
                        moneyFormatter = moneyFormatter,
                        ratioFormatter = ratioFormatter
                    )
                } else {
                    PeriodicCategoryCardsRow(
                        categories = periodicCategories,
                        audits = audits,
                        todayMidnight = todayMidnight,
                        moneyFormatter = moneyFormatter,
                        ratioFormatter = ratioFormatter,
                        onCategoryClick = { spec ->
                            selectedDetailCategory = spec.key
                        }
                    )
                }

                // AKILLI FİNANSAL ANALİZ KARTLARI (Her kategori için alt alta)
                tabCategories.forEach { spec ->
                    SmartCategoryAnalysisCard(
                        spec = spec,
                        latestAudit = latestByCategory[spec.key],
                        moneyFormatter = moneyFormatter,
                        ratioFormatter = ratioFormatter,
                        onClick = { selectedDetailCategory = spec.key }
                    )
                }
            }
        }
    }
}
