package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AdetselKayit
import com.example.data.BackupMetadata
import com.example.data.BackupRestoreResult
import com.example.data.DataMigrationManager
import com.example.data.ExpiryStatus
import com.example.data.InspectionReport
import com.example.data.MigrationStatus
import com.example.data.Product
import com.example.data.ProductRepository
import com.example.data.TurKontrolKaydi
import com.example.data.TurRaporu
import com.example.data.getTodayMidnightMillis
import com.example.data.isDolapProduct
import com.example.data.matchesSearchQuery
import com.example.data.normalizeForSearch
import com.example.sync.CloudSyncManager
import com.example.ui.viewmodel.AdetselSayimManager
import com.example.ui.viewmodel.BarcodeScanProcessor
import com.example.ui.viewmodel.DatabaseRepairHelper
import com.example.ui.viewmodel.TourSessionManager
import com.example.util.ProductCsvImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

enum class ProductFilter(val label: String) {
    ALL("TÜMÜ"),
    IMPORTANT("🔥 ÖNEMLİ (STOK≥10 & SKT≤30 GÜN)"),
    EXPIRED("SÜRESİ GEÇEN"),
    CRITICAL("KRİTİK"),
    SOON("YAKIN")
}

enum class ProductGroupFilter(val label: String) {
    ALL("Tümü"),
    DOLAP("Dolap"),
    GIDA("Gıda")
}

data class DashboardState(
    val totalCount: Int = 0,
    val expiredCount: Int = 0,
    val criticalCount: Int = 0,
    val soonCount: Int = 0,
    val importantCount: Int = 0,
    val attentionProducts: List<Product> = emptyList(),
    val removeProducts: List<Product> = emptyList(),
    val nearExpiryProducts: List<Product> = emptyList(),
    val isMorningTourCompletedToday: Boolean = false,
    val unreadNotificationCount: Int = 3
)

class MainViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    // Sub-Managers for Feature Decoupling
    private val tourSessionManager = TourSessionManager(
        repository = repository,
        scope = viewModelScope,
        getAllProducts = { allProducts.value }
    )

    private val adetselSayimManager = AdetselSayimManager(
        repository = repository,
        scope = viewModelScope
    )

    // Search and Filter State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadingMessage = MutableStateFlow("Veriler Yükleniyor...")
    val loadingMessage: StateFlow<String> = _loadingMessage.asStateFlow()

    fun showLoading(message: String = "Veriler Yükleniyor...") {
        _loadingMessage.value = message
        _isLoading.value = true
    }

    fun hideLoading() {
        _isLoading.value = false
    }

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

    // Morning Tour / Gamification State (Delegated to TourSessionManager)
    val gameTargetCategory: StateFlow<String> = tourSessionManager.gameTargetCategory
    val gameScannedProducts: StateFlow<List<Product>> = tourSessionManager.gameScannedProducts
    val gameActive: StateFlow<Boolean> = tourSessionManager.gameActive
    val lastScannedGameProduct: StateFlow<Product?> = tourSessionManager.lastScannedGameProduct
    val tourQueue: StateFlow<List<Product>> = tourSessionManager.tourQueue
    val currentQueueIndex: StateFlow<Int> = tourSessionManager.currentQueueIndex
    val tourLogs: StateFlow<List<TurKontrolKaydi>> = tourSessionManager.tourLogs
    val tourFinished: StateFlow<Boolean> = tourSessionManager.tourFinished
    val isTourPaused: StateFlow<Boolean> = tourSessionManager.isTourPaused
    val tourStartTime: StateFlow<Long> = tourSessionManager.tourStartTime
    val tourScore: StateFlow<Int> = tourSessionManager.tourScore
    val tourStreak: StateFlow<Int> = tourSessionManager.tourStreak
    val lastActionMessage: StateFlow<String?> = tourSessionManager.lastActionMessage
    val lastSavedTourRaporu: StateFlow<TurRaporu?> = tourSessionManager.lastSavedTourRaporu
    val allTurRaporlari: StateFlow<List<TurRaporu>> = tourSessionManager.allTurRaporlari

    // Adetsel Sayım State (Delegated to AdetselSayimManager)
    val allAdetselKayitlari: StateFlow<List<AdetselKayit>> = adetselSayimManager.allAdetselKayitlari
    val yapilacakAdetselKayitlari: StateFlow<List<AdetselKayit>> = adetselSayimManager.yapilacakAdetselKayitlari
    val yapildiAdetselKayitlari: StateFlow<List<AdetselKayit>> = adetselSayimManager.yapildiAdetselKayitlari

    // All products flow from repository
    val allProducts: StateFlow<List<Product>> = repository.allProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allReports: StateFlow<List<InspectionReport>> = repository.allReports
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            val list = repository.getProductListDirect()
            val userHasReset = CloudSyncManager.hasUserResetData()
            if (list.isEmpty() && !userHasReset) {
                repository.reSeedDefaultData()
            }
            // Automatically clean up any duplicate products from DB on launch
            fixAndRepairDatabase { _, _ -> }
            cleanDuplicatePendingAdetsel()
        }
    }

    private fun isSameCalendarDay(millis1: Long, millis2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = millis1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    // User Profile & Settings State
    private val _userName = MutableStateFlow("Ahmet Yılmaz")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userBranch = MutableStateFlow("Kadıköy Şubesi #4102")
    val userBranch: StateFlow<String> = _userBranch.asStateFlow()

    private val _userRole = MutableStateFlow("Reyon Sorumlusu & SKT Görevlisi")
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    private val _userDepartment = MutableStateFlow("Süt & Şarküteri Reyonu")
    val userDepartment: StateFlow<String> = _userDepartment.asStateFlow()

    private val _userDutyStatus = MutableStateFlow("Vardiyada (Aktif)")
    val userDutyStatus: StateFlow<String> = _userDutyStatus.asStateFlow()

    private val _morningCheckReminderEnabled = MutableStateFlow(true)
    val morningCheckReminderEnabled: StateFlow<Boolean> = _morningCheckReminderEnabled.asStateFlow()

    private val _criticalSktAlertEnabled = MutableStateFlow(true)
    val criticalSktAlertEnabled: StateFlow<Boolean> = _criticalSktAlertEnabled.asStateFlow()

    private val _highStockAlertEnabled = MutableStateFlow(true)
    val highStockAlertEnabled: StateFlow<Boolean> = _highStockAlertEnabled.asStateFlow()

    private val _soundEffectsEnabled = MutableStateFlow(true)
    val soundEffectsEnabled: StateFlow<Boolean> = _soundEffectsEnabled.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isBatterySaverMode = MutableStateFlow(false)
    val isBatterySaverMode: StateFlow<Boolean> = _isBatterySaverMode.asStateFlow()

    private val _notificationsRead = MutableStateFlow(false)
    val notificationsRead: StateFlow<Boolean> = _notificationsRead.asStateFlow()

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setDarkMode(dark: Boolean) {
        _isDarkMode.value = dark
    }

    fun toggleBatterySaverMode() {
        val nextVal = !_isBatterySaverMode.value
        _isBatterySaverMode.value = nextVal
        if (nextVal) {
            _isDarkMode.value = true
        }
    }

    fun setBatterySaverMode(enabled: Boolean) {
        _isBatterySaverMode.value = enabled
        if (enabled) {
            _isDarkMode.value = true
        }
    }

    fun fixAndRepairDatabase(onResult: (Int, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepairHelper.repairDatabase(repository, onResult)
        }
    }

    fun updateUserProfile(name: String, branch: String, role: String = "Reyon Sorumlusu & SKT Görevlisi", department: String = "Süt & Şarküteri Reyonu") {
        if (name.isNotBlank()) _userName.value = name.trim()
        if (branch.isNotBlank()) _userBranch.value = branch.trim()
        if (role.isNotBlank()) _userRole.value = role.trim()
        if (department.isNotBlank()) _userDepartment.value = department.trim()
    }

    fun updateDutyStatus(status: String) {
        _userDutyStatus.value = status
    }

    fun toggleMorningCheckReminder() {
        _morningCheckReminderEnabled.value = !_morningCheckReminderEnabled.value
    }

    fun toggleCriticalSktAlert() {
        _criticalSktAlertEnabled.value = !_criticalSktAlertEnabled.value
    }

    fun toggleHighStockAlert() {
        _highStockAlertEnabled.value = !_highStockAlertEnabled.value
    }

    fun toggleSoundEffects() {
        _soundEffectsEnabled.value = !_soundEffectsEnabled.value
    }

    fun toggleVibration() {
        _vibrationEnabled.value = !_vibrationEnabled.value
    }

    fun markNotificationsAsRead() {
        _notificationsRead.value = true
    }

    val dashboardState: StateFlow<DashboardState> = combine(
        allProducts,
        allReports,
        _notificationsRead
    ) { products, reports, isRead ->
        val todayMidnight = getTodayMidnightMillis()
        val expired = products.count { it.sktTarihi > 0L && it.getExpiryStatus(todayMidnight) == ExpiryStatus.EXPIRED }
        val critical = products.count { it.sktTarihi > 0L && it.getExpiryStatus(todayMidnight) == ExpiryStatus.CRITICAL }
        val soon = products.count { it.sktTarihi > 0L && it.getExpiryStatus(todayMidnight) == ExpiryStatus.SOON }
        val important = products.count { (it.sktTarihi > 0L && it.getRemainingDays(todayMidnight) in 1..30 && it.stokAdedi >= 10) || it.isImportant }

        val attention = products.filter { it.sktTarihi > 0L }
            .distinctBy { "${if (it.barkod.isNotBlank()) it.barkod else it.urunAdi.trim().lowercase()}-${it.getFormattedSkt()}" }
            .sortedBy { it.getRemainingDays(todayMidnight) }
            .take(10)

        val removeProds = products.filter { it.sktTarihi > 0L && it.getRemainingDays(todayMidnight) <= 0L }
            .distinctBy { "${if (it.barkod.isNotBlank()) it.barkod else it.urunAdi.trim().lowercase()}-${it.getFormattedSkt()}" }
            .sortedBy { it.getRemainingDays(todayMidnight) }

        val nearExpiryProds = products.filter { it.sktTarihi > 0L && it.getRemainingDays(todayMidnight) in 1L..7L }
            .distinctBy { "${if (it.barkod.isNotBlank()) it.barkod else it.urunAdi.trim().lowercase()}-${it.getFormattedSkt()}" }
            .sortedBy { it.getRemainingDays(todayMidnight) }

        val completedToday = reports.any { it.tarih >= todayMidnight }
        val activeAlertsCount = expired + critical
        val unreadCount = if (isRead) 0 else (if (activeAlertsCount > 0) activeAlertsCount else 1)

        DashboardState(
            totalCount = products.size,
            expiredCount = expired,
            criticalCount = critical,
            soonCount = soon,
            importantCount = important,
            attentionProducts = attention,
            removeProducts = removeProds,
            nearExpiryProducts = nearExpiryProds,
            isMorningTourCompletedToday = completedToday,
            unreadNotificationCount = unreadCount
        )
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )

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

        val baseList = products.filter { prod ->
            val matchesSearch = prod.matchesSearchQuery(rawQuery, queryTokens)
            val matchesFilter = when (filter) {
                ProductFilter.ALL -> prod.sktTarihi > 0L && prod.stokAdedi > 0
                ProductFilter.IMPORTANT -> (prod.sktTarihi > 0L && prod.getRemainingDays(todayMidnight) in 1..30 && prod.stokAdedi >= 10) || prod.isImportant
                ProductFilter.EXPIRED -> prod.sktTarihi > 0L && prod.getExpiryStatus(todayMidnight) == ExpiryStatus.EXPIRED
                ProductFilter.CRITICAL -> prod.sktTarihi > 0L && prod.getExpiryStatus(todayMidnight) == ExpiryStatus.CRITICAL
                ProductFilter.SOON -> prod.sktTarihi > 0L && prod.getExpiryStatus(todayMidnight) == ExpiryStatus.SOON
            }

            val matchesDateRange = if (calStart != null || calEnd != null) {
                if (prod.sktTarihi <= 0L) {
                    false
                } else {
                    val startValid = calStart == null || prod.sktTarihi >= calStart
                    val endValid = calEnd == null || prod.sktTarihi <= calEnd
                    startValid && endValid
                }
            } else {
                true
            }

            matchesSearch && matchesFilter && matchesDateRange
        }

        when (groupFilter) {
            ProductGroupFilter.DOLAP -> baseList.filter { it.isDolapProduct() }
            ProductGroupFilter.GIDA -> baseList.filter { !it.isDolapProduct() }
            ProductGroupFilter.ALL -> baseList
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

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
            val currentEditing = _editingProduct.value

            val savedProduct: Product = if (currentEditing != null) {
                val productToSave = currentEditing.copy(
                    barkod = finalBarkod,
                    urunKodu = urunKodu.trim(),
                    urunAdi = urunAdi.uppercase().trim(),
                    kategori = kategori,
                    sktTarihi = if (sktTarihi > 0L) sktTarihi else currentEditing.sktTarihi,
                    stokAdedi = if (stokAdedi > 0) stokAdedi else currentEditing.stokAdedi,
                    fiyat = fiyat ?: currentEditing.fiyat,
                    isImportant = isImportant
                )
                repository.insertOrUpdateProduct(productToSave)
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
                        urunAdi = urunAdi.uppercase().trim().ifBlank { sameDayMatch.urunAdi },
                        kategori = kategori.ifBlank { sameDayMatch.kategori },
                        fiyat = fiyat ?: sameDayMatch.fiyat,
                        isImportant = isImportant || sameDayMatch.isImportant
                    )
                    repository.insertOrUpdateProduct(updatedProduct)
                    updatedProduct
                } else {
                    val productToSave = Product(
                        barkod = finalBarkod,
                        urunKodu = urunKodu.trim(),
                        urunAdi = urunAdi.uppercase().trim(),
                        kategori = kategori,
                        sktTarihi = finalSkt,
                        stokAdedi = finalStok,
                        fiyat = fiyat,
                        isImportant = isImportant
                    )
                    repository.insertOrUpdateProduct(productToSave)
                    productToSave
                }
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
            repository.updateProductStock(product.id, newStock)
            val updated = product.copy(stokAdedi = newStock, sonKontrolTarihi = System.currentTimeMillis())
            if (_detailProduct.value?.id == product.id) {
                _detailProduct.value = updated
            }
            onSuccess(newStock)
        }
    }

    fun removeProductFromShelf(product: Product, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val updated = product.copy(stokAdedi = 0)
            repository.insertOrUpdateProduct(updated)
            if (_detailProduct.value?.id == product.id) {
                _detailProduct.value = updated
            }
            onSuccess()
        }
    }

    fun removeMultipleProductsFromShelf(products: List<Product>, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            products.forEach { prod ->
                val updated = prod.copy(stokAdedi = 0)
                repository.insertOrUpdateProduct(updated)
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

    fun resetAllData() {
        viewModelScope.launch {
            showLoading("Veritabanı Sıfırlanıyor...")
            try {
                CloudSyncManager.setHasUserResetData(true)
                repository.resetAllData()
            } finally {
                hideLoading()
            }
        }
    }

    fun restoreDefaultSeedData() {
        viewModelScope.launch {
            showLoading("Varsayılan Ürünler Yükleniyor...")
            try {
                CloudSyncManager.setHasUserResetData(false)
                repository.reSeedDefaultData()
            } finally {
                hideLoading()
            }
        }
    }

    // CSV / XLSX Batch Import with Smart Auto-Heal (Delegated to ProductCsvImporter)
    fun importCsvLines(lines: List<String>): Int {
        val existingKeys = allProducts.value.map {
            "${it.barkod.trim().lowercase()}_${it.urunKodu.trim().lowercase()}_${it.urunAdi.trim().lowercase()}"
        }.toSet()

        val productsToInsert = ProductCsvImporter.parseLinesToProducts(lines, existingKeys)
        if (productsToInsert.isNotEmpty()) {
            viewModelScope.launch {
                showLoading("Ürünler Aktarılıyor (${productsToInsert.size} Kalem)...")
                try {
                    repository.insertProductsBatch(productsToInsert)
                } finally {
                    hideLoading()
                }
            }
        }
        return productsToInsert.size
    }

    // Gamification / Kontrol Oyunu methods (Delegated to TourSessionManager)
    fun setGameTargetCategory(category: String) = tourSessionManager.setGameTargetCategory(category)
    fun startTourSession() = tourSessionManager.startTourSession()
    fun pauseTourSession() = tourSessionManager.pauseTourSession()
    fun resumeTourSession() = tourSessionManager.resumeTourSession()
    fun clearLastActionMessage() = tourSessionManager.clearLastActionMessage()
    fun recordTourSold(product: Product, soldCount: Int) = tourSessionManager.recordTourSold(product, soldCount)
    fun recordTourFire(product: Product, fireCount: Int) = tourSessionManager.recordTourFire(product, fireCount)
    fun recordTourNotr(product: Product) = tourSessionManager.recordTourNotr(product)
    fun undoLastTourAction() = tourSessionManager.undoLastTourAction()
    fun cancelTourSession() = tourSessionManager.cancelTourSession()
    fun resetTourState() = tourSessionManager.resetTourState()
    fun startGameSession() = tourSessionManager.startGameSession()
    fun cancelGameSession() = tourSessionManager.cancelGameSession()
    fun onGameScanBarcode(barkod: String, onProductNotFound: () -> Unit) = tourSessionManager.onGameScanBarcode(barkod, onProductNotFound)
    fun completeGameSession(onFinished: (InspectionReport) -> Unit) = tourSessionManager.completeGameSession(onFinished)
    fun deleteTurRaporu(id: Int) = tourSessionManager.deleteTurRaporu(id)
    fun clearAllTurRaporlari() = tourSessionManager.clearAllTurRaporlari()
    fun getKayitlarByTurId(turId: Int): Flow<List<TurKontrolKaydi>> = tourSessionManager.getKayitlarByTurId(turId)

    // Adetsel Sayım Methods (Delegated to AdetselSayimManager)
    fun cleanDuplicatePendingAdetsel() = adetselSayimManager.cleanDuplicatePendingAdetsel()
    fun addToAdetsel(product: Product, onComplete: ((Boolean) -> Unit)? = null) = adetselSayimManager.addToAdetsel(product, onComplete)
    fun saveAdetselSayim(
        kayit: AdetselKayit,
        sonuc: String,
        fark: Int,
        sayilanAdet: Int = kayit.beklenenAdet + fark,
        notlar: String = "",
        onComplete: (() -> Unit)? = null
    ) = adetselSayimManager.saveAdetselSayim(kayit, sonuc, fark, sayilanAdet, notlar, onComplete)
    fun undoAdetselKayit(kayit: AdetselKayit) = adetselSayimManager.undoAdetselKayit(kayit)
    fun deleteAdetselKayit(id: Int) = adetselSayimManager.deleteAdetselKayit(id)
    fun clearCompletedAdetselKayitlar() = adetselSayimManager.clearCompletedAdetselKayitlar()

    // Legacy Inspection Reports
    fun deleteReport(reportId: Int) {
        viewModelScope.launch {
            repository.deleteReport(reportId)
        }
    }

    fun clearAllReports() {
        viewModelScope.launch {
            repository.clearAllReports()
        }
    }

    // Barcode & QR code scanning (Delegated to BarcodeScanProcessor)
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

    // Data Protection & Backup / Restore System
    val migrationStatus: StateFlow<MigrationStatus?> = DataMigrationManager.migrationStatus

    fun dismissMigrationStatus() {
        DataMigrationManager.dismissStatus()
    }

    fun runStartupDataProtection(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            DataMigrationManager.performStartupDataProtectionCheck(
                context = context,
                productDao = repository.productDao,
                reportDao = repository.reportDao,
                turDao = repository.turDao,
                adetselDao = repository.adetselDao
            )
        }
    }

    fun createUnifiedBackupJson(context: Context, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val json = repository.createUnifiedBackupJson(context)
            withContext(Dispatchers.Main) {
                onResult(json)
            }
        }
    }

    fun saveLocalBackup(context: Context, tag: String = "manual", onResult: (File?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = repository.saveLocalBackup(context, tag)
            withContext(Dispatchers.Main) {
                onResult(file)
            }
        }
    }

    fun getLocalBackups(context: Context): List<BackupMetadata> {
        return repository.getLocalBackups(context)
    }

    fun restoreFromJson(
        context: Context,
        jsonString: String,
        mergeWithExisting: Boolean = true,
        onResult: (BackupRestoreResult) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.restoreFromJson(context, jsonString, mergeWithExisting)
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    class Factory(private val repository: ProductRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
