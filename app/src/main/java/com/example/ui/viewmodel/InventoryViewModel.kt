package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ExpiryStatus
import com.example.data.InventoryAuditEntity
import com.example.data.Product
import com.example.data.ProductRepository
import com.example.data.StockLog
import com.example.data.computeAuditPeriod
import com.example.data.getTodayMidnightMillis
import com.example.data.isDolapProduct
import com.example.data.matchesSearchQuery
import com.example.data.normalizeForSearch
import com.example.ui.ProductFilter
import com.example.ui.ProductGroupFilter
import com.example.util.ProductCsvImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

data class WeeklyAuditTrend(
    val category: String,
    val latestAudit: InventoryAuditEntity?,
    val previousAudit: InventoryAuditEntity?,
    val currentRatio: Double,
    val previousRatio: Double?,
    val progressDiff: Double?, // Mevcut Oran - Önceki Oran (Eksi değer = iyileşme, Artı değer = kötüleşme)
    val isImproved: Boolean
)

data class MonthlyAuditSummary(
    val category: String,
    val year: Int,
    val month: Int,
    val recordCount: Int,
    val averageRevenue: Double,
    val averageFireCost: Double,
    val averageRatio: Double,
    val previousMonthAverageRatio: Double?,
    val monthlyProgressDiff: Double?
)

data class CombinedInventorySummary(
    val totalRevenue: Double = 0.0,
    val totalFireCost: Double = 0.0,
    val combinedRatio: Double? = null,
    val periodicRatio: Double? = null,
    val continuousRatio: Double? = null,
    val weightedTargetRatio: Double = 1.0,
    val isOverTarget: Boolean = false,
    val hasData: Boolean = false
)

val PERIODIC_AUDIT_CATEGORIES = listOf("GIDA", "DOLAP", "GIDA DIŞI")
val CONTINUOUS_AUDIT_CATEGORIES = listOf("ET_TAVUK", "EKMEK", "MEYVE_SEBZE", "DONUK_MEYVE_SEBZE")

fun normalizeInventoryCategoryKey(rawCategory: String): String {
    val upper = rawCategory.trim().uppercase(java.util.Locale.forLanguageTag("tr-TR"))
    return when (upper) {
        "ET-TAVUK", "ET_TAVUK" -> "ET_TAVUK"
        "EKMEK" -> "EKMEK"
        "MEYVE-SEBZE", "MEYVE_SEBZE" -> "MEYVE_SEBZE"
        "DONUK MEYVE-SEBZE", "DONUK_MEYVE_SEBZE" -> "DONUK_MEYVE_SEBZE"
        else -> upper
    }
}

fun isContinuousAuditCategory(rawCategory: String): Boolean {
    return normalizeInventoryCategoryKey(rawCategory) in CONTINUOUS_AUDIT_CATEGORIES
}

fun computeCombinedInventorySummary(
    auditsList: List<InventoryAuditEntity>
): CombinedInventorySummary {
    val latestComparator = compareByDescending<InventoryAuditEntity> { it.updatedAt }
        .thenByDescending { it.auditDateMillis }
        .thenByDescending { it.createdAt }

    val latestByNormalizedCategory = auditsList
        .groupBy { normalizeInventoryCategoryKey(it.category) }
        .mapValues { (_, items) -> items.sortedWith(latestComparator).firstOrNull() }

    val latestPeriodic = PERIODIC_AUDIT_CATEGORIES.mapNotNull { latestByNormalizedCategory[it] }
    val latestContinuous = CONTINUOUS_AUDIT_CATEGORIES.mapNotNull { latestByNormalizedCategory[it] }
    val allLatest = latestPeriodic + latestContinuous

    if (allLatest.isEmpty()) {
        return CombinedInventorySummary()
    }

    val periodicRevenue = latestPeriodic.sumOf { it.revenue.coerceAtLeast(0.0) }
    val periodicFire = latestPeriodic.sumOf { it.fireCost.coerceAtLeast(0.0) }
    val periodicRatio = if (periodicRevenue > 0.0) {
        (periodicFire / periodicRevenue) * 100.0
    } else null

    val continuousRevenue = latestContinuous.sumOf { it.revenue.coerceAtLeast(0.0) }
    val continuousFire = latestContinuous.sumOf { it.fireCost.coerceAtLeast(0.0) }
    val continuousRatio = if (continuousRevenue > 0.0) {
        (continuousFire / continuousRevenue) * 100.0
    } else null

    val totalRevenue = periodicRevenue + continuousRevenue
    val totalFire = periodicFire + continuousFire
    val combinedRatio = if (totalRevenue > 0.0) {
        (totalFire / totalRevenue) * 100.0
    } else null

    val weightedTargetRatio = if (totalRevenue > 0.0) {
        val allowedFireSum = allLatest.sumOf { item ->
            val target = if (item.targetRatio > 0.0) item.targetRatio else 1.0
            item.revenue.coerceAtLeast(0.0) * (target / 100.0)
        }
        (allowedFireSum / totalRevenue) * 100.0
    } else {
        val positiveTargets = allLatest.mapNotNull { it.targetRatio.takeIf { r -> r > 0.0 } }
        if (positiveTargets.isNotEmpty()) positiveTargets.average() else 1.0
    }

    val isOverTarget = combinedRatio != null && combinedRatio > weightedTargetRatio

    return CombinedInventorySummary(
        totalRevenue = totalRevenue,
        totalFireCost = totalFire,
        combinedRatio = combinedRatio,
        periodicRatio = periodicRatio,
        continuousRatio = continuousRatio,
        weightedTargetRatio = weightedTargetRatio,
        isOverTarget = isOverTarget,
        hasData = true
    )
}

class InventoryViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    // Search and Filter State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(ProductFilter.ALL)
    val selectedFilter: StateFlow<ProductFilter> = _selectedFilter.asStateFlow()

    private val _selectedGroupFilter = MutableStateFlow(ProductGroupFilter.ALL)
    val selectedGroupFilter: StateFlow<ProductGroupFilter> = _selectedGroupFilter.asStateFlow()

    private val _startDateFilter = MutableStateFlow<Long?>(null)
    val startDateFilter: StateFlow<Long?> = _startDateFilter.asStateFlow()

    private val _endDateFilter = MutableStateFlow<Long?>(null)
    val endDateFilter: StateFlow<Long?> = _endDateFilter.asStateFlow()

    // Dialog and Modal States
    private val _isAddEditModalOpen = MutableStateFlow(false)
    val isAddEditModalOpen: StateFlow<Boolean> = _isAddEditModalOpen.asStateFlow()

    private val _editingProduct = MutableStateFlow<Product?>(null)
    val editingProduct: StateFlow<Product?> = _editingProduct.asStateFlow()

    private val _detailProduct = MutableStateFlow<Product?>(null)
    val detailProduct: StateFlow<Product?> = _detailProduct.asStateFlow()

    private val _addSktProduct = MutableStateFlow<Product?>(null)
    val addSktProduct: StateFlow<Product?> = _addSktProduct.asStateFlow()

    private val _prefilledBarcode = MutableStateFlow("")
    val prefilledBarcode: StateFlow<String> = _prefilledBarcode.asStateFlow()

    // All products flow from repository
    val allProducts: StateFlow<List<Product>> = repository.allProducts
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allStockLogs: StateFlow<List<StockLog>> = repository.allStockLogs
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allInventoryAudits: StateFlow<List<InventoryAuditEntity>> = repository.allInventoryAudits
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val combinedInventorySummary: StateFlow<CombinedInventorySummary> = allInventoryAudits
        .map { auditsList -> computeCombinedInventorySummary(auditsList) }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CombinedInventorySummary()
        )

    fun getAuditsByCategory(category: String): Flow<List<InventoryAuditEntity>> {
        return repository.getAuditsByCategory(category.trim())
    }

    /**
     * Haftalık trend hesaplama:
     * - Aynı kategoriye ait en son iki kaydı (updatedAt sırasına göre en yeni 2 kayıt) alır.
     * - Mevcut Oran = (Son Fire / Son Ciro) * 100
     * - Önceki Oran = (Önceki Fire / Önceki Ciro) * 100
     * - İlerleme Farkı = Mevcut Oran - Önceki Oran (Eksi değer = iyileşme, Artı = kötüleşme)
     */
    fun calculateWeeklyTrend(
        category: String,
        auditsList: List<InventoryAuditEntity> = allInventoryAudits.value
    ): WeeklyAuditTrend {
        val sortedForCategory = auditsList
            .filter { it.category.equals(category.trim(), ignoreCase = true) }
            .sortedWith(
                compareByDescending<InventoryAuditEntity> { it.updatedAt }
                    .thenByDescending { it.auditDateMillis }
                    .thenByDescending { it.createdAt }
            )

        val latest = sortedForCategory.getOrNull(0)
        val previous = sortedForCategory.getOrNull(1)

        val currentRatio = if (latest != null && latest.revenue > 0.0) {
            (latest.fireCost / latest.revenue) * 100.0
        } else {
            0.0
        }

        val previousRatio = if (previous != null && previous.revenue > 0.0) {
            (previous.fireCost / previous.revenue) * 100.0
        } else {
            null
        }

        val progressDiff = if (previousRatio != null) {
            currentRatio - previousRatio
        } else {
            null
        }

        return WeeklyAuditTrend(
            category = category.trim(),
            latestAudit = latest,
            previousAudit = previous,
            currentRatio = currentRatio,
            previousRatio = previousRatio,
            progressDiff = progressDiff,
            isImproved = progressDiff != null && progressDiff < 0.0
        )
    }

    /**
     * Aylık bazda ortalama hesaplama:
     * İçinde bulunulan aya ait kayıtların ortalamasını ve bir önceki ayın ortalamasıyla farkını hesaplar.
     */
    fun calculateMonthlyAverage(
        category: String,
        auditsList: List<InventoryAuditEntity> = allInventoryAudits.value,
        referenceMillis: Long = System.currentTimeMillis()
    ): MonthlyAuditSummary {
        val refCal = Calendar.getInstance().apply { timeInMillis = referenceMillis }
        val currentYear = refCal.get(Calendar.YEAR)
        val currentMonth = refCal.get(Calendar.MONTH)

        val prevCal = (refCal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val prevYear = prevCal.get(Calendar.YEAR)
        val prevMonth = prevCal.get(Calendar.MONTH)

        val categoryAudits = auditsList.filter { it.category.equals(category.trim(), ignoreCase = true) }

        val currentMonthRecords = categoryAudits.filter { audit ->
            val ts = if (audit.updatedAt > 0L) audit.updatedAt else audit.createdAt
            val c = Calendar.getInstance().apply { timeInMillis = ts }
            c.get(Calendar.YEAR) == currentYear && c.get(Calendar.MONTH) == currentMonth
        }

        val prevMonthRecords = categoryAudits.filter { audit ->
            val ts = if (audit.updatedAt > 0L) audit.updatedAt else audit.createdAt
            val c = Calendar.getInstance().apply { timeInMillis = ts }
            c.get(Calendar.YEAR) == prevYear && c.get(Calendar.MONTH) == prevMonth
        }

        val avgRevenue = if (currentMonthRecords.isNotEmpty()) {
            currentMonthRecords.map { it.revenue }.average()
        } else 0.0

        val avgFire = if (currentMonthRecords.isNotEmpty()) {
            currentMonthRecords.map { it.fireCost }.average()
        } else 0.0

        val currentMonthRatios = currentMonthRecords.mapNotNull {
            if (it.revenue > 0.0) (it.fireCost / it.revenue) * 100.0 else null
        }
        val avgRatio = if (currentMonthRatios.isNotEmpty()) {
            currentMonthRatios.average()
        } else 0.0

        val prevMonthRatios = prevMonthRecords.mapNotNull {
            if (it.revenue > 0.0) (it.fireCost / it.revenue) * 100.0 else null
        }
        val prevAvgRatio = if (prevMonthRatios.isNotEmpty()) {
            prevMonthRatios.average()
        } else null

        val monthlyDiff = if (prevAvgRatio != null && currentMonthRatios.isNotEmpty()) {
            avgRatio - prevAvgRatio
        } else null

        return MonthlyAuditSummary(
            category = category.trim(),
            year = currentYear,
            month = currentMonth + 1,
            recordCount = currentMonthRecords.size,
            averageRevenue = avgRevenue,
            averageFireCost = avgFire,
            averageRatio = avgRatio,
            previousMonthAverageRatio = prevAvgRatio,
            monthlyProgressDiff = monthlyDiff
        )
    }

    fun recordInventoryAudit(
        category: String,
        notes: String = "",
        auditDateMillis: Long = System.currentTimeMillis(),
        revenue: Double = 0.0,
        fireCost: Double = 0.0,
        targetRatio: Double = 0.0,
        auditPeriod: String = "",
        onComplete: (() -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val audit = InventoryAuditEntity(
                category = category.trim(),
                auditDateMillis = auditDateMillis,
                revenue = revenue.coerceAtLeast(0.0),
                fireCost = fireCost.coerceAtLeast(0.0),
                targetRatio = targetRatio.coerceAtLeast(0.0),
                notes = notes.trim(),
                createdAt = now,
                updatedAt = now,
                auditPeriod = auditPeriod.trim()
            )
            repository.insertInventoryAudit(audit)
            withContext(Dispatchers.Main) {
                onComplete?.invoke()
            }
        }
    }

    fun saveInventoryAudit(
        audit: InventoryAuditEntity,
        onComplete: (() -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val auditToSave = audit.copy(
                id = 0,
                updatedAt = now,
                auditPeriod = audit.auditPeriod.trim()
            )
            repository.insertInventoryAudit(auditToSave)
            withContext(Dispatchers.Main) {
                onComplete?.invoke()
            }
        }
    }

    fun updateInventoryAudit(
        audit: InventoryAuditEntity,
        onComplete: (() -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            // Sadece en son girilen veriyi düzeltmek için mevcut kaydı günceller
            val updatedRecord = audit.copy(
                updatedAt = if (audit.updatedAt > 0L) audit.updatedAt else now,
                auditPeriod = audit.auditPeriod.trim()
            )
            repository.updateInventoryAudit(updatedRecord)
            withContext(Dispatchers.Main) {
                onComplete?.invoke()
            }
        }
    }

    fun deleteInventoryAudit(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteInventoryAuditById(id)
        }
    }

    fun deleteInventoryAudit(audit: InventoryAuditEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteInventoryAudit(audit)
        }
    }

    val filteredProducts: StateFlow<List<Product>> = combine(
        allProducts,
        _searchQuery,
        _selectedFilter,
        _startDateFilter,
        _endDateFilter,
        _selectedGroupFilter
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val products = flows[0] as List<Product>
        @Suppress("UNCHECKED_CAST")
        val query = flows[1] as String
        @Suppress("UNCHECKED_CAST")
        val filter = flows[2] as ProductFilter
        @Suppress("UNCHECKED_CAST")
        val startDate = flows[3] as Long?
        @Suppress("UNCHECKED_CAST")
        val endDate = flows[4] as Long?
        @Suppress("UNCHECKED_CAST")
        val groupFilter = flows[5] as ProductGroupFilter

        val todayMidnight = getTodayMidnightMillis()
        val rawQuery = query.trim()
        val queryTokens = if (rawQuery.isBlank()) emptyList() else {
            rawQuery.normalizeForSearch()
                .replace(',', '.')
                .split("\\s+".toRegex())
                .filter { it.isNotBlank() }
        }

        val calStart = startDate?.let {
            Calendar.getInstance().apply {
                timeInMillis = it
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        val calEnd = endDate?.let {
            Calendar.getInstance().apply {
                timeInMillis = it
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        }

        val result = ArrayList<Product>(products.size)
        for (prod in products) {
            // 1. Group Filter
            if (groupFilter == ProductGroupFilter.DOLAP && !prod.isDolapProduct()) continue
            if (groupFilter == ProductGroupFilter.GIDA && prod.isDolapProduct()) continue

            // 2. Status Filter
            val matchesFilter = when (filter) {
                ProductFilter.ALL -> prod.sktTarihi > 0L && prod.stokAdedi > 0
                ProductFilter.IMPORTANT -> prod.isImportant && prod.stokAdedi > 0
                ProductFilter.URGENT_0_TO_7 -> prod.sktTarihi > 0L && prod.stokAdedi > 0 && prod.getRemainingDays(todayMidnight) in 0L..7L
                ProductFilter.EXPIRED -> prod.sktTarihi > 0L && prod.stokAdedi > 0 && prod.getExpiryStatus(todayMidnight) == ExpiryStatus.EXPIRED
                ProductFilter.CRITICAL -> prod.sktTarihi > 0L && prod.stokAdedi > 0 && prod.getExpiryStatus(todayMidnight) == ExpiryStatus.CRITICAL
                ProductFilter.SOON -> prod.sktTarihi > 0L && prod.stokAdedi > 0 && prod.getExpiryStatus(todayMidnight) == ExpiryStatus.SOON
            }
            if (!matchesFilter) continue

            // 3. Date Range Filter
            if (calStart != null || calEnd != null) {
                if (prod.sktTarihi <= 0L) continue
                if (calStart != null && prod.sktTarihi < calStart) continue
                if (calEnd != null && prod.sktTarihi > calEnd) continue
            }

            // 4. Search Query Filter
            if (rawQuery.isNotEmpty() && !prod.matchesSearchQuery(rawQuery, queryTokens)) {
                continue
            }

            result.add(prod)
        }
        result
    }
    .flowOn(Dispatchers.Default)
    .distinctUntilChanged()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun isSameCalendarDay(millis1: Long, millis2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = millis1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun onGroupFilterSelected(group: ProductGroupFilter) {
        if (_selectedGroupFilter.value == group) {
            _selectedGroupFilter.value = ProductGroupFilter.ALL
        } else {
            _selectedGroupFilter.value = group
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(filter: ProductFilter) {
        _selectedFilter.value = filter
    }

    fun setDateRangeFilter(startMillis: Long?, endMillis: Long?) {
        _startDateFilter.value = startMillis
        _endDateFilter.value = endMillis
    }

    fun clearDateRangeFilter() {
        _startDateFilter.value = null
        _endDateFilter.value = null
    }

    fun openAddProductModal(prefilledBarcode: String = "") {
        _editingProduct.value = null
        _prefilledBarcode.value = prefilledBarcode
        _isAddEditModalOpen.value = true
    }

    fun openEditProductModal(product: Product) {
        _editingProduct.value = product
        _prefilledBarcode.value = product.barkod
        _isAddEditModalOpen.value = true
    }

    fun closeAddEditModal() {
        _isAddEditModalOpen.value = false
        _editingProduct.value = null
        _prefilledBarcode.value = ""
    }

    fun openProductDetailModal(product: Product) {
        _detailProduct.value = product
    }

    fun closeProductDetailModal() {
        _detailProduct.value = null
    }

    fun openAddSktModal(product: Product) {
        _addSktProduct.value = product
    }

    fun closeAddSktModal() {
        _addSktProduct.value = null
    }

    fun saveProduct(
        barkod: String,
        urunKodu: String,
        urunAdi: String,
        kategori: String,
        sktTarihi: Long,
        stokAdedi: Int,
        fiyat: Double? = null,
        isImportant: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val finalBarkod = barkod.trim().ifBlank {
                if (urunKodu.isNotBlank()) "2500$urunKodu" else "869${(100000000..999999999).random()}"
            }
            val finalSkt = if (sktTarihi > 0L) sktTarihi else 0L
            val finalStok = if (sktTarihi > 0L) (if (stokAdedi > 0) stokAdedi else 1) else 0
            val userCategory = kategori.trim()
            val cleanCategory = if (userCategory.equals("Dolap", ignoreCase = true) ||
                                    userCategory.equals("Gıda", ignoreCase = true)) {
                userCategory  // Kullanıcı seçimi
            } else {
                com.example.util.CategoryClassifier.classifyCategory(urunAdi)
            }
            val currentEditing = _editingProduct.value

            val trLocale = java.util.Locale.forLanguageTag("tr-TR")
            val savedProduct: Product = if (currentEditing != null) {
                val productToSave = currentEditing.copy(
                    barkod = finalBarkod,
                    urunKodu = urunKodu.trim(),
                    urunAdi = urunAdi.uppercase(trLocale).trim(),
                    kategori = cleanCategory,
                    sktTarihi = if (sktTarihi > 0L) sktTarihi else currentEditing.sktTarihi,
                    stokAdedi = if (stokAdedi > 0) stokAdedi else currentEditing.sktTarihi.let { if (it > 0L) currentEditing.stokAdedi else 0 },
                    fiyat = fiyat,
                    isImportant = isImportant
                )
                repository.insertOrUpdateProduct(productToSave)

                // Sync sibling items of same barcode / urunKodu / product name
                val oldBarkod = currentEditing.barkod.trim()
                val oldUrunKodu = currentEditing.urunKodu.trim()
                val oldUrunAdi = currentEditing.urunAdi.trim()

                val allCurrentProducts = repository.getProductListDirect()
                val siblings = allCurrentProducts.filter { prod ->
                    prod.id != currentEditing.id && (
                        (oldBarkod.isNotBlank() && prod.barkod.equals(oldBarkod, ignoreCase = true)) ||
                        (oldUrunKodu.isNotBlank() && prod.urunKodu.equals(oldUrunKodu, ignoreCase = true)) ||
                        (oldUrunAdi.isNotBlank() && prod.urunAdi.equals(oldUrunAdi, ignoreCase = true))
                    )
                }

                siblings.forEach { sibling ->
                    val siblingCat = cleanCategory  // Kullanıcının seçtiği kategori tüm kardeşlere uygulanır
                    val updatedSibling = sibling.copy(
                        barkod = finalBarkod,
                        urunKodu = urunKodu.trim().ifBlank { sibling.urunKodu },
                        urunAdi = urunAdi.uppercase(trLocale).trim().ifBlank { sibling.urunAdi },
                        kategori = siblingCat,
                        fiyat = fiyat ?: sibling.fiyat,
                        isImportant = isImportant
                    )
                    repository.insertOrUpdateProduct(updatedSibling)
                }

                productToSave
            } else {
                val existingList = if (finalBarkod.isNotBlank()) {
                    repository.getProductsByBarcode(finalBarkod)
                } else {
                    allProducts.value.filter {
                        it.urunAdi.equals(urunAdi.trim(), ignoreCase = true) ||
                        (urunKodu.isNotBlank() && it.urunKodu == urunKodu.trim())
                    }
                }
                val sameDayMatch = existingList.find { isSameCalendarDay(it.sktTarihi, finalSkt) }
                if (sameDayMatch != null) {
                    val updatedProduct = sameDayMatch.copy(
                        stokAdedi = sameDayMatch.stokAdedi + finalStok,
                        urunKodu = urunKodu.trim().ifBlank { sameDayMatch.urunKodu },
                        urunAdi = urunAdi.uppercase(trLocale).trim().ifBlank { sameDayMatch.urunAdi },
                        kategori = cleanCategory,
                        fiyat = fiyat ?: sameDayMatch.fiyat,
                        isImportant = isImportant || sameDayMatch.isImportant
                    )
                    repository.insertOrUpdateProduct(updatedProduct)
                    updatedProduct
                } else {
                    val productToSave = Product(
                        barkod = finalBarkod,
                        urunKodu = urunKodu.trim(),
                        urunAdi = urunAdi.uppercase(trLocale).trim(),
                        kategori = cleanCategory,
                        sktTarihi = finalSkt,
                        stokAdedi = finalStok,
                        fiyat = fiyat,
                        isImportant = isImportant
                    )
                    repository.insertOrUpdateProduct(productToSave)
                    productToSave
                }
            }

            if (finalSkt > 0L) {
                val daysRemaining = if (finalSkt > 0L) {
                    val tempProduct = Product(
                        barkod = finalBarkod,
                        urunKodu = urunKodu,
                        urunAdi = urunAdi,
                        kategori = cleanCategory,
                        sktTarihi = finalSkt,
                        stokAdedi = finalStok
                    )
                    tempProduct.getRemainingDays(getTodayMidnightMillis()).toInt()
                } else 0
                repository.insertStockLog(
                    StockLog(
                        barcode = finalBarkod,
                        productName = urunAdi.uppercase(trLocale).trim(),
                        actionType = "SKT_GIRIS",
                        quantity = finalStok,
                        sktDate = finalSkt,
                        daysRemaining = daysRemaining,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }

            if (_detailProduct.value != null) {
                _detailProduct.value = savedProduct
            }
            withContext(Dispatchers.Main) {
                closeAddEditModal()
            }
        }
    }

    fun updateProductPrice(product: Product, newPrice: Double?) {
        viewModelScope.launch {
            val matches = if (product.barkod.isNotBlank()) {
                repository.getProductsByBarcode(product.barkod)
            } else {
                allProducts.value.filter { it.urunAdi.equals(product.urunAdi, ignoreCase = true) }
            }
            if (matches.isNotEmpty()) {
                matches.forEach { p ->
                    repository.insertOrUpdateProduct(p.copy(fiyat = newPrice))
                }
            } else {
                repository.insertOrUpdateProduct(product.copy(fiyat = newPrice))
            }
            if (_detailProduct.value?.id == product.id || (_detailProduct.value != null && _detailProduct.value?.barkod == product.barkod && product.barkod.isNotBlank())) {
                _detailProduct.value = _detailProduct.value?.copy(fiyat = newPrice)
            }
        }
    }

    fun toggleProductImportant(product: Product) {
        viewModelScope.launch {
            val updated = product.copy(isImportant = !product.isImportant)
            repository.insertOrUpdateProduct(updated)
            if (_detailProduct.value?.id == product.id) {
                _detailProduct.value = updated
            }
        }
    }

    fun addSktToExistingProduct(
        existingProduct: Product,
        newSktTarihi: Long,
        newStokAdedi: Int,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val existingList = if (existingProduct.barkod.isNotBlank()) {
                repository.getProductsByBarcode(existingProduct.barkod)
            } else {
                allProducts.value.filter {
                    it.urunAdi.equals(existingProduct.urunAdi, ignoreCase = true) ||
                    (existingProduct.urunKodu.isNotBlank() && it.urunKodu == existingProduct.urunKodu)
                }
            }

            val sameDayMatch = existingList.find { isSameCalendarDay(it.sktTarihi, newSktTarihi) }
            val placeholder = existingList.find { it.sktTarihi <= 0L }

            val savedProduct = if (sameDayMatch != null) {
                val updatedProduct = sameDayMatch.copy(
                    stokAdedi = sameDayMatch.stokAdedi + newStokAdedi
                )
                repository.insertOrUpdateProduct(updatedProduct)
                if (placeholder != null && placeholder.id != sameDayMatch.id) {
                    repository.deleteProduct(placeholder)
                }
                updatedProduct
            } else if (placeholder != null) {
                val updatedPlaceholder = placeholder.copy(
                    sktTarihi = newSktTarihi,
                    stokAdedi = newStokAdedi
                )
                repository.insertOrUpdateProduct(updatedPlaceholder)
                updatedPlaceholder
            } else {
                val newProd = existingProduct.copy(
                    id = 0,
                    sktTarihi = newSktTarihi,
                    stokAdedi = newStokAdedi
                )
                repository.insertOrUpdateProduct(newProd)
                newProd
            }

            if (newSktTarihi > 0L) {
                val daysRemaining = if (newSktTarihi > 0L) {
                    existingProduct.copy(sktTarihi = newSktTarihi)
                        .getRemainingDays(getTodayMidnightMillis()).toInt()
                } else 0
                repository.insertStockLog(
                    StockLog(
                        barcode = existingProduct.barkod,
                        productName = existingProduct.urunAdi,
                        actionType = "SKT_GIRIS",
                        quantity = newStokAdedi,
                        sktDate = newSktTarihi,
                        daysRemaining = daysRemaining,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }

            if (_detailProduct.value?.barkod == existingProduct.barkod) {
                _detailProduct.value = savedProduct
            }
            onSuccess()
        }
    }

    fun updateSktItem(
        item: Product,
        newSktTarihi: Long,
        newStokAdedi: Int,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val updatedProduct = item.copy(
                sktTarihi = newSktTarihi,
                stokAdedi = newStokAdedi
            )
            repository.insertOrUpdateProduct(updatedProduct)

            if (newSktTarihi > 0L) {
                val daysRemaining = if (newSktTarihi > 0L) {
                    item.copy(sktTarihi = newSktTarihi)
                        .getRemainingDays(getTodayMidnightMillis()).toInt()
                } else 0
                repository.insertStockLog(
                    StockLog(
                        barcode = item.barkod,
                        productName = item.urunAdi,
                        actionType = "SKT_GIRIS",
                        quantity = newStokAdedi,
                        sktDate = newSktTarihi,
                        daysRemaining = daysRemaining,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }

            if (_detailProduct.value?.barkod == item.barkod) {
                _detailProduct.value = updatedProduct
            }
            onSuccess()
        }
    }

    fun deductProductStock(
        product: Product,
        amount: Int,
        reason: String,
        onSuccess: (remaining: Int) -> Unit = {}
    ) {
        viewModelScope.launch {
            val safeAmount = if (amount <= 0) 1 else amount
            val newStock = maxOf(0, product.stokAdedi - safeAmount)

            val normalizedType = when {
                reason.contains("SAT", ignoreCase = true) -> "SATIS"
                reason.contains("FİR", ignoreCase = true) || reason.contains("FIR", ignoreCase = true) || reason.contains("İMHA", ignoreCase = true) || reason.contains("IMHA", ignoreCase = true) -> "FIRE"
                else -> if (reason.equals("Fire", ignoreCase = true)) "FIRE" else "SATIS"
            }

            val daysRemaining = if (product.sktTarihi > 0L) {
                product.getRemainingDays(getTodayMidnightMillis()).toInt()
            } else null

            repository.insertStockLog(
                StockLog(
                    barcode = product.barkod,
                    productName = product.urunAdi,
                    actionType = normalizedType,
                    quantity = safeAmount,
                    sktDate = if (product.sktTarihi > 0L) product.sktTarihi else null,
                    daysRemaining = daysRemaining,
                    timestamp = System.currentTimeMillis()
                )
            )

            val existingBatches = if (product.barkod.isNotBlank() && !product.barkod.startsWith("NO_BARCODE_")) {
                repository.getProductsByBarcode(product.barkod).ifEmpty {
                    allProducts.value.filter {
                        it.barkod == product.barkod ||
                        (product.urunKodu.isNotBlank() && it.urunKodu == product.urunKodu) ||
                        it.urunAdi.equals(product.urunAdi, ignoreCase = true)
                    }
                }
            } else {
                allProducts.value.filter {
                    (product.urunKodu.isNotBlank() && it.urunKodu == product.urunKodu) ||
                    it.urunAdi.equals(product.urunAdi, ignoreCase = true)
                }
            }
            val otherBatches = existingBatches.filter { it.id != product.id }
            val otherActiveBatches = otherBatches.filter { it.stokAdedi > 0 && it.sktTarihi > 0L }

            if (newStock == 0) {
                if (otherActiveBatches.isNotEmpty()) {
                    // Depleted batch is completely removed
                    repository.deleteProduct(product)
                    // Also clean up any stale 0-stock batches that may linger
                    otherBatches.filter { it.stokAdedi <= 0 && it.sktTarihi > 0L }.forEach { stale ->
                        repository.deleteProduct(stale)
                    }
                    if (_detailProduct.value?.id == product.id) {
                        _detailProduct.value = otherActiveBatches.first()
                    }
                } else {
                    // All active batches for this product are finished. Clean up extra batch rows
                    otherBatches.forEach { other ->
                        repository.deleteProduct(other)
                    }
                    // Reset to single base product with NO active SKT (sktTarihi = 0L) and 0 stock
                    val resetProduct = product.copy(
                        sktTarihi = 0L,
                        stokAdedi = 0,
                        sonKontrolTarihi = System.currentTimeMillis()
                    )
                    repository.insertOrUpdateProduct(resetProduct)
                    if (_detailProduct.value?.id == product.id) {
                        _detailProduct.value = resetProduct
                    }
                }
            } else {
                repository.updateProductStock(product, newStock)
                val updated = product.copy(stokAdedi = newStock, sonKontrolTarihi = System.currentTimeMillis())
                if (_detailProduct.value?.id == product.id) {
                    _detailProduct.value = updated
                }
            }
            onSuccess(newStock)
        }
    }

    fun removeProductFromShelf(product: Product, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val existingBatches = if (product.barkod.isNotBlank() && !product.barkod.startsWith("NO_BARCODE_")) {
                repository.getProductsByBarcode(product.barkod).ifEmpty {
                    allProducts.value.filter {
                        it.barkod == product.barkod ||
                        (product.urunKodu.isNotBlank() && it.urunKodu == product.urunKodu) ||
                        it.urunAdi.equals(product.urunAdi, ignoreCase = true)
                    }
                }
            } else {
                allProducts.value.filter {
                    (product.urunKodu.isNotBlank() && it.urunKodu == product.urunKodu) ||
                    it.urunAdi.equals(product.urunAdi, ignoreCase = true)
                }
            }
            val otherBatches = existingBatches.filter { it.id != product.id }
            val otherActiveBatches = otherBatches.filter { it.stokAdedi > 0 && it.sktTarihi > 0L }

            if (otherActiveBatches.isNotEmpty()) {
                repository.deleteProduct(product)
                otherBatches.filter { it.stokAdedi <= 0 && it.sktTarihi > 0L }.forEach { stale ->
                    repository.deleteProduct(stale)
                }
                if (_detailProduct.value?.id == product.id) {
                    _detailProduct.value = otherActiveBatches.first()
                }
            } else {
                otherBatches.forEach { other ->
                    repository.deleteProduct(other)
                }
                val resetProduct = product.copy(
                    sktTarihi = 0L,
                    stokAdedi = 0,
                    sonKontrolTarihi = System.currentTimeMillis()
                )
                repository.insertOrUpdateProduct(resetProduct)
                if (_detailProduct.value?.id == product.id) {
                    _detailProduct.value = resetProduct
                }
            }
            onSuccess()
        }
    }

    fun removeMultipleProductsFromShelf(products: List<Product>, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            products.forEach { prod ->
                val existingBatches = if (prod.barkod.isNotBlank() && !prod.barkod.startsWith("NO_BARCODE_")) {
                    repository.getProductsByBarcode(prod.barkod).ifEmpty {
                        allProducts.value.filter {
                            it.barkod == prod.barkod ||
                            (prod.urunKodu.isNotBlank() && it.urunKodu == prod.urunKodu) ||
                            it.urunAdi.equals(prod.urunAdi, ignoreCase = true)
                        }
                    }
                } else {
                    allProducts.value.filter {
                        (prod.urunKodu.isNotBlank() && it.urunKodu == prod.urunKodu) ||
                        it.urunAdi.equals(prod.urunAdi, ignoreCase = true)
                    }
                }
                val otherBatches = existingBatches.filter { it.id != prod.id }
                val otherActiveBatches = otherBatches.filter { it.stokAdedi > 0 && it.sktTarihi > 0L }

                if (otherActiveBatches.isNotEmpty()) {
                    repository.deleteProduct(prod)
                    otherBatches.filter { it.stokAdedi <= 0 && it.sktTarihi > 0L }.forEach { stale ->
                        repository.deleteProduct(stale)
                    }
                } else {
                    otherBatches.forEach { other ->
                        repository.deleteProduct(other)
                    }
                    val resetProduct = prod.copy(
                        sktTarihi = 0L,
                        stokAdedi = 0,
                        sonKontrolTarihi = System.currentTimeMillis()
                    )
                    repository.insertOrUpdateProduct(resetProduct)
                }
            }
            onSuccess()
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            if (_editingProduct.value?.id == product.id) {
                closeAddEditModal()
            }
            if (_detailProduct.value?.id == product.id) {
                val remaining = allProducts.value.filter {
                    it.barkod.equals(product.barkod, ignoreCase = true) && it.id != product.id
                }
                if (remaining.isNotEmpty()) {
                    _detailProduct.value = remaining.first()
                } else {
                    closeProductDetailModal()
                }
            }
        }
    }

    fun deleteProductWithAllBatches(product: Product) {
        viewModelScope.launch {
            val allMatches = if (product.barkod.isNotBlank()) {
                repository.getProductsByBarcode(product.barkod)
            } else {
                allProducts.value.filter { it.urunAdi.equals(product.urunAdi, ignoreCase = true) }
            }
            if (allMatches.isNotEmpty()) {
                allMatches.forEach { repository.deleteProduct(it) }
            } else {
                repository.deleteProduct(product)
            }
            closeAddEditModal()
            closeProductDetailModal()
        }
    }

    fun importCsvLines(lines: List<String>, onLoadingChange: ((Boolean, String) -> Unit)? = null): com.example.util.CsvParseResult {
        val trLocale = java.util.Locale.forLanguageTag("tr-TR")
        val existingKeys = allProducts.value.map {
            "${it.barkod.trim().lowercase(trLocale)}_${it.urunKodu.trim().lowercase(trLocale)}_${it.urunAdi.trim().lowercase(trLocale)}"
        }.toSet()

        val parseResult = ProductCsvImporter.parseLinesWithStats(lines, existingKeys)
        val productsToInsert = parseResult.productsToInsert
        if (productsToInsert.isNotEmpty()) {
            viewModelScope.launch {
                onLoadingChange?.invoke(true, "Ürünler Aktarılıyor (${productsToInsert.size} Kalem)...")
                try {
                    repository.insertProductsBatch(productsToInsert)
                } finally {
                    onLoadingChange?.invoke(false, "")
                }
            }
        }
        return parseResult
    }

    fun fixProductBarcodeAndPriceFromQr(
        rawInput: String,
        onResult: (message: String, isSuccess: Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            BarcodeScanProcessor.fixProductBarcodeAndPriceFromQr(repository, rawInput, onResult)
        }
    }

    fun handleBarcodeScanned(
        barkod: String,
        onFound: (Product) -> Unit,
        onNotFound: (String) -> Unit
    ) {
        viewModelScope.launch {
            BarcodeScanProcessor.handleBarcodeScanned(
                repository = repository,
                barkod = barkod,
                onUpdatePrice = { prod, price -> updateProductPrice(prod, price) },
                onFound = onFound,
                onNotFound = onNotFound
            )
        }
    }

    class Factory(private val repository: ProductRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InventoryViewModel(repository) as T
        }
    }
}
