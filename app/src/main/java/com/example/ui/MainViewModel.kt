package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ExpiryStatus
import com.example.data.InspectionReport
import com.example.data.Product
import com.example.data.ProductRepository
import com.example.data.TurKontrolKaydi
import com.example.data.TurRaporu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

import com.example.data.matchesSearchQuery

enum class ProductFilter(val label: String) {
    ALL("TÜMÜ"),
    IMPORTANT("🔥 ÖNEMLİ (STOK≥10 & SKT≤30 GÜN)"),
    EXPIRED("SÜRESİ GEÇEN"),
    CRITICAL("KRİTİK"),
    SOON("YAKIN")
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

    // State for Add/Edit dialog
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

    // State for Game Mode / Morning Tour
    private val _gameTargetCategory = MutableStateFlow("Dolap Ürünleri")
    val gameTargetCategory: StateFlow<String> = _gameTargetCategory.asStateFlow()

    private val _gameScannedProducts = MutableStateFlow<List<Product>>(emptyList())
    val gameScannedProducts: StateFlow<List<Product>> = _gameScannedProducts.asStateFlow()

    private val _gameActive = MutableStateFlow(false)
    val gameActive: StateFlow<Boolean> = _gameActive.asStateFlow()

    private val _lastScannedGameProduct = MutableStateFlow<Product?>(null)
    val lastScannedGameProduct: StateFlow<Product?> = _lastScannedGameProduct.asStateFlow()

    // New Morning Check Tour State
    private val _tourQueue = MutableStateFlow<List<Product>>(emptyList())
    val tourQueue: StateFlow<List<Product>> = _tourQueue.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(0)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    private val _tourLogs = MutableStateFlow<List<TurKontrolKaydi>>(emptyList())
    val tourLogs: StateFlow<List<TurKontrolKaydi>> = _tourLogs.asStateFlow()

    private val _tourFinished = MutableStateFlow(false)
    val tourFinished: StateFlow<Boolean> = _tourFinished.asStateFlow()

    private val _lastSavedTourRaporu = MutableStateFlow<TurRaporu?>(null)
    val lastSavedTourRaporu: StateFlow<TurRaporu?> = _lastSavedTourRaporu.asStateFlow()

    val allTurRaporlari: StateFlow<List<TurRaporu>> = repository.allTurRaporlari
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // All products flow from repository
    val allProducts: StateFlow<List<Product>> = repository.allProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            val list = repository.getProductListDirect()
            if (list.size < 20) {
                repository.reSeedDefaultData()
            }
        }
    }



    private fun isSameCalendarDay(millis1: Long, millis2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = millis1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    val allReports: StateFlow<List<InspectionReport>> = repository.allReports
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // User Profile & Notification State
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

    private val _notificationsRead = MutableStateFlow(false)
    val notificationsRead: StateFlow<Boolean> = _notificationsRead.asStateFlow()

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setDarkMode(dark: Boolean) {
        _isDarkMode.value = dark
    }

    fun fixAndRepairDatabase(onResult: (Int, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var fixedCount = 0
            try {
                val allProds = repository.getProductListDirect()
                val updatedProds = mutableListOf<Product>()
                val seenKeys = mutableSetOf<String>()
                val duplicatesToDelete = mutableListOf<Product>()

                for (p in allProds) {
                    val trimmedName = p.urunAdi.trim().replace("\\s+".toRegex(), " ")
                    // Clean barcode: filter non-digits and strip leading zero mismatch
                    var cleanBarcode = p.barkod.trim().filter { it.isDigit() }
                    var cleanUrunKodu = p.urunKodu.trim().filter { it.isLetterOrDigit() }
                    var newStok = p.stokAdedi
                    var isModified = false

                    if (trimmedName != p.urunAdi) {
                        isModified = true
                        fixedCount++
                    }
                    if (cleanBarcode != p.barkod && cleanBarcode.isNotEmpty()) {
                        isModified = true
                        fixedCount++
                    }
                    if (cleanUrunKodu != p.urunKodu) {
                        isModified = true
                        fixedCount++
                    }
                    if (newStok < 0) {
                        newStok = 0
                        isModified = true
                        fixedCount++
                    }

                    val effectiveBarcode = if (cleanBarcode.isNotEmpty()) cleanBarcode else p.barkod.trim()
                    val dupKey = "$effectiveBarcode-$cleanUrunKodu-${p.sktTarihi}"

                    if (seenKeys.contains(dupKey) && effectiveBarcode.isNotEmpty()) {
                        duplicatesToDelete.add(p)
                        fixedCount++
                    } else {
                        if (effectiveBarcode.isNotEmpty()) seenKeys.add(dupKey)
                        if (isModified) {
                            updatedProds.add(
                                p.copy(
                                    urunAdi = trimmedName,
                                    barkod = effectiveBarcode,
                                    urunKodu = cleanUrunKodu,
                                    stokAdedi = newStok
                                )
                            )
                        }
                    }
                }

                for (dup in duplicatesToDelete) {
                    repository.deleteProduct(dup)
                }
                for (mod in updatedProds) {
                    repository.insertOrUpdateProduct(mod)
                }

                val summary = if (fixedCount > 0) {
                    "🔍 Tarama ve Onarım Tamamlandı!\n\n• Toplam $fixedCount adet tutarsızlık/mükerrer/format hatası otomatik düzeltildi.\n• ${duplicatesToDelete.size} adet mükerrer kayıt temizlendi.\n• Ürün metinleri, barkodlar, ürün kodları ve stok sayıları optimize edildi."
                } else {
                    "✅ Mükemmel! Veritabanınızda hiçbir hata veya tutarsızlık bulunamadı. Tüm veriler tam uyumlu ve optimize edilmiş durumda."
                }

                withContext(Dispatchers.Main) {
                    onResult(fixedCount, summary)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(0, "Hata taraması sırasında bir sorun oluştu: ${e.localizedMessage}")
                }
            }
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
        val expired = products.count { it.sktTarihi > 0L && it.getExpiryStatus() == ExpiryStatus.EXPIRED }
        val critical = products.count { it.sktTarihi > 0L && it.getExpiryStatus() == ExpiryStatus.CRITICAL }
        val soon = products.count { it.sktTarihi > 0L && it.getExpiryStatus() == ExpiryStatus.SOON }
        val important = products.count { it.sktTarihi > 0L && it.getRemainingDays() in 1..30 && it.stokAdedi >= 10 }

        // Top attention items sorted by closest remaining days
        val attention = products.filter { it.sktTarihi > 0L }.sortedBy { it.getRemainingDays() }.take(10)

        // Reyondan kaldırılması gerekenler (Sağ taraf: SKT dolmuş / remainingDays <= 0)
        val removeProds = products.filter { it.sktTarihi > 0L && it.getRemainingDays() <= 0L }
            .sortedBy { it.getRemainingDays() }

        // SKT'sine son 1 ya da 2 gün kalmış ürünler (Sol taraf: 1 <= remainingDays <= 2)
        val nearExpiryProds = products.filter { it.sktTarihi > 0L && it.getRemainingDays() in 1L..2L }
            .sortedBy { it.getRemainingDays() }

        // Check if morning check completed today
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val completedToday = reports.any { it.tarih >= todayStart }

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
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )

    val filteredProducts: StateFlow<List<Product>> = combine(
        allProducts,
        _searchQuery,
        _selectedFilter
    ) { products, query, filter ->
        products.filter { prod ->
            val matchesSearch = prod.matchesSearchQuery(query)

            val matchesFilter = when (filter) {
                ProductFilter.ALL -> true
                ProductFilter.IMPORTANT -> prod.sktTarihi > 0L && prod.getRemainingDays() in 1..30 && prod.stokAdedi >= 10
                ProductFilter.EXPIRED -> prod.sktTarihi > 0L && prod.getExpiryStatus() == ExpiryStatus.EXPIRED
                ProductFilter.CRITICAL -> prod.sktTarihi > 0L && prod.getExpiryStatus() == ExpiryStatus.CRITICAL
                ProductFilter.SOON -> prod.sktTarihi > 0L && prod.getExpiryStatus() == ExpiryStatus.SOON
            }

            matchesSearch && matchesFilter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(filter: ProductFilter) {
        _selectedFilter.value = filter
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
        viewModelScope.launch {
            val currentEditing = _editingProduct.value
            val savedProduct: Product = if (currentEditing != null) {
                val productToSave = currentEditing.copy(
                    barkod = barkod,
                    urunKodu = urunKodu,
                    urunAdi = urunAdi.uppercase(),
                    kategori = kategori,
                    sktTarihi = sktTarihi,
                    stokAdedi = stokAdedi,
                    fiyat = fiyat,
                    isImportant = isImportant
                )
                repository.insertOrUpdateProduct(productToSave)
                productToSave
            } else {
                val existingList = if (barkod.isNotBlank()) {
                    repository.getProductsByBarcode(barkod)
                } else {
                    allProducts.value.filter {
                        it.urunAdi.equals(urunAdi, ignoreCase = true) ||
                        (urunKodu.isNotBlank() && it.urunKodu == urunKodu)
                    }
                }
                val sameDayMatch = existingList.find { isSameCalendarDay(it.sktTarihi, sktTarihi) }
                if (sameDayMatch != null) {
                    val updatedProduct = sameDayMatch.copy(
                        stokAdedi = sameDayMatch.stokAdedi + stokAdedi,
                        urunKodu = urunKodu.ifBlank { sameDayMatch.urunKodu },
                        urunAdi = urunAdi.uppercase().ifBlank { sameDayMatch.urunAdi },
                        kategori = kategori.ifBlank { sameDayMatch.kategori },
                        fiyat = fiyat ?: sameDayMatch.fiyat,
                        isImportant = isImportant || sameDayMatch.isImportant
                    )
                    repository.insertOrUpdateProduct(updatedProduct)
                    updatedProduct
                } else {
                    val productToSave = Product(
                        barkod = barkod,
                        urunKodu = urunKodu,
                        urunAdi = urunAdi.uppercase(),
                        kategori = kategori,
                        sktTarihi = sktTarihi,
                        stokAdedi = stokAdedi,
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
            closeAddEditModal()
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

            val savedProduct = if (sameDayMatch != null) {
                val updatedProduct = sameDayMatch.copy(
                    stokAdedi = sameDayMatch.stokAdedi + newStokAdedi
                )
                repository.insertOrUpdateProduct(updatedProduct)
                updatedProduct
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
                repository.reSeedDefaultData()
            } finally {
                hideLoading()
            }
        }
    }

    // CSV Batch Import
    fun importCsvLines(lines: List<String>): Int {
        val existingBarcodes = allProducts.value.map { it.barkod.trim() }.toSet()
        val batchBarcodes = mutableSetOf<String>()
        val productsToInsert = mutableListOf<Product>()

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#") || line.lowercase().startsWith("barkod")) continue
            val parts = line.split(",", ";", "\t").map { it.trim() }
            if (parts.size >= 3) {
                val barkod = parts[0]
                val urunKodu = parts[1]
                val urunAdi = parts[2]
                val kategori = if (parts.size >= 4 && parts[3].isNotEmpty()) parts[3] else "Gıda Ürünleri"
                val sktTarihi = if (parts.size >= 5 && parts[4].isNotEmpty()) {
                    parseDateOrOffset(parts[4])
                } else {
                    // Default to 14 days in future if not specified
                    System.currentTimeMillis() + 14L * 24 * 60 * 60 * 1000
                }
                val stokAdedi = if (parts.size >= 6) parts[5].toIntOrNull() ?: 10 else 10

                if (barkod.isNotEmpty() && urunAdi.isNotEmpty()) {
                    if (!existingBarcodes.contains(barkod) && !batchBarcodes.contains(barkod)) {
                        batchBarcodes.add(barkod)
                        productsToInsert.add(
                            Product(
                                barkod = barkod,
                                urunKodu = urunKodu.ifEmpty { "0000" },
                                urunAdi = urunAdi.uppercase(),
                                kategori = kategori,
                                sktTarihi = sktTarihi,
                                stokAdedi = stokAdedi
                            )
                        )
                    }
                }
            }
        }
        if (productsToInsert.isNotEmpty()) {
            viewModelScope.launch {
                showLoading("CSV Ürünleri Aktarılıyor (${productsToInsert.size} Kalem)...")
                try {
                    repository.insertProductsBatch(productsToInsert)
                } finally {
                    hideLoading()
                }
            }
        }
        return productsToInsert.size
    }

    private fun parseDateOrOffset(input: String): Long {
        // Can be either "+N" days offset or day timestamp or "yyyy-MM-dd" / "dd.MM.yyyy"
        input.toIntOrNull()?.let { daysOffset ->
            return System.currentTimeMillis() + daysOffset * 24L * 60 * 60 * 1000
        }
        return try {
            if (input.contains(".")) {
                val p = input.split(".")
                val cal = Calendar.getInstance()
                cal.set(p[2].toInt(), p[1].toInt() - 1, p[0].toInt(), 0, 0, 0)
                cal.timeInMillis
            } else if (input.contains("-")) {
                val p = input.split("-")
                val cal = Calendar.getInstance()
                cal.set(p[0].toInt(), p[1].toInt() - 1, p[2].toInt(), 0, 0, 0)
                cal.timeInMillis
            } else {
                System.currentTimeMillis() + 14L * 24 * 60 * 60 * 1000
            }
        } catch (e: Exception) {
            System.currentTimeMillis() + 14L * 24 * 60 * 60 * 1000
        }
    }

    // Gamification / Kontrol Oyunu methods
    fun setGameTargetCategory(category: String) {
        _gameTargetCategory.value = category
    }

    fun startTourSession() {
        val targetCat = _gameTargetCategory.value
        val allProds = allProducts.value

        // Filter by category if specific category chosen
        val catFiltered = if (targetCat != "Tüm Reyonlar" && targetCat != "Tüm Ürünler" && targetCat.isNotBlank()) {
            allProds.filter { it.kategori.equals(targetCat, ignoreCase = true) }
        } else {
            allProds
        }

        // Filter eligible products for tour:
        // 1. Critical (0-7 days remaining)
        // 2. Soon (8-30 days remaining)
        // 3. Important (isImportant == true)
        val eligible = catFiltered.filter { prod ->
            val remainingDays = prod.getRemainingDays()
            remainingDays <= 30 || prod.isImportant
        }.sortedBy { it.sktTarihi }

        _tourQueue.value = eligible
        _currentQueueIndex.value = 0
        _tourLogs.value = emptyList()
        _tourFinished.value = false
        _lastSavedTourRaporu.value = null
        _gameActive.value = true
    }

    fun recordTourSold(product: Product, soldCount: Int) {
        viewModelScope.launch {
            val newStock = maxOf(0, product.stokAdedi - soldCount)
            repository.updateProductStock(product.id, newStock)

            val log = TurKontrolKaydi(
                productId = product.id,
                urunAdiSnapshot = product.urunAdi,
                barkodSnapshot = product.barkod,
                durum = "SATILDI",
                islemAdedi = soldCount
            )
            _tourLogs.value = _tourLogs.value + log

            val nextIndex = _currentQueueIndex.value + 1
            if (nextIndex >= _tourQueue.value.size) {
                finishTourInternal(isEarlyExit = false)
            } else {
                _currentQueueIndex.value = nextIndex
            }
        }
    }

    fun recordTourFire(product: Product, fireCount: Int) {
        viewModelScope.launch {
            val newStock = maxOf(0, product.stokAdedi - fireCount)
            repository.updateProductStock(product.id, newStock)

            val log = TurKontrolKaydi(
                productId = product.id,
                urunAdiSnapshot = product.urunAdi,
                barkodSnapshot = product.barkod,
                durum = "FIRE",
                islemAdedi = fireCount
            )
            _tourLogs.value = _tourLogs.value + log

            val nextIndex = _currentQueueIndex.value + 1
            if (nextIndex >= _tourQueue.value.size) {
                finishTourInternal(isEarlyExit = false)
            } else {
                _currentQueueIndex.value = nextIndex
            }
        }
    }

    fun recordTourNotr(product: Product) {
        viewModelScope.launch {
            repository.updateProductStock(product.id, product.stokAdedi)

            val log = TurKontrolKaydi(
                productId = product.id,
                urunAdiSnapshot = product.urunAdi,
                barkodSnapshot = product.barkod,
                durum = "NOTR",
                islemAdedi = 0
            )
            _tourLogs.value = _tourLogs.value + log

            val nextIndex = _currentQueueIndex.value + 1
            if (nextIndex >= _tourQueue.value.size) {
                finishTourInternal(isEarlyExit = false)
            } else {
                _currentQueueIndex.value = nextIndex
            }
        }
    }

    fun undoLastTourAction() {
        val logs = _tourLogs.value
        val currentIndex = _currentQueueIndex.value
        if (logs.isNotEmpty() && currentIndex > 0) {
            val lastLog = logs.last()
            viewModelScope.launch {
                if (lastLog.durum == "SATILDI" || lastLog.durum == "FIRE") {
                    val prod = allProducts.value.find { it.id == lastLog.productId }
                    if (prod != null) {
                        val restoredStock = prod.stokAdedi + lastLog.islemAdedi
                        repository.updateProductStock(prod.id, restoredStock)
                    }
                }
                _tourLogs.value = logs.dropLast(1)
                _currentQueueIndex.value = currentIndex - 1
            }
        }
    }

    fun cancelTourSession() {
        if (_tourLogs.value.isNotEmpty()) {
            finishTourInternal(isEarlyExit = true)
        } else {
            _gameActive.value = false
            _tourFinished.value = false
        }
    }

    private fun finishTourInternal(isEarlyExit: Boolean) {
        viewModelScope.launch {
            val logs = _tourLogs.value
            val satilanLogs = logs.filter { it.durum == "SATILDI" }
            val fireLogs = logs.filter { it.durum == "FIRE" }

            val satilanUrunCount = satilanLogs.size
            val satilanTotalCount = satilanLogs.sumOf { it.islemAdedi }
            val fireUrunCount = fireLogs.size
            val fireTotalCount = fireLogs.sumOf { it.islemAdedi }
            val notrUrunCount = logs.count { it.durum == "NOTR" }

            val tourRaporu = TurRaporu(
                turTarihi = System.currentTimeMillis(),
                hedefReyon = _gameTargetCategory.value,
                toplamUrunSayisi = logs.size,
                satilanUrunSayisi = satilanUrunCount,
                toplamSatilanAdet = satilanTotalCount,
                fireUrunSayisi = fireUrunCount,
                toplamFireAdet = fireTotalCount,
                notrUrunSayisi = notrUrunCount,
                tamamlandiMi = !isEarlyExit
            )

            val savedId = repository.saveTourReport(tourRaporu, logs)
            _lastSavedTourRaporu.value = tourRaporu.copy(id = savedId.toInt())

            // Mirror to legacy report list
            val legacyReport = InspectionReport(
                tarih = System.currentTimeMillis(),
                reyonAdi = _gameTargetCategory.value,
                tarananUrunSayisi = logs.size,
                suresiGecenSayisi = fireUrunCount,
                kritikUrunSayisi = satilanUrunCount,
                fireTutari = fireTotalCount * 35.0
            )
            repository.insertReport(legacyReport)

            _gameActive.value = false
            _tourFinished.value = true
        }
    }

    fun resetTourState() {
        _gameActive.value = false
        _tourFinished.value = false
        _tourQueue.value = emptyList()
        _currentQueueIndex.value = 0
        _tourLogs.value = emptyList()
        _lastSavedTourRaporu.value = null
    }

    fun startGameSession() {
        startTourSession()
    }

    fun cancelGameSession() {
        cancelTourSession()
    }

    fun onGameScanBarcode(barkod: String, onProductNotFound: () -> Unit) {
        viewModelScope.launch {
            val query = barkod.trim()
            val queryDigits = query.filter { it.isDigit() }
            val queryNoLeadingZeros = queryDigits.trimStart('0')

            val all = repository.getProductListDirect()
            var prod = all.find { it.barkod.equals(query, ignoreCase = true) }

            if (prod == null && queryDigits.isNotEmpty()) {
                prod = all.find { p ->
                    val pDigits = p.barkod.trim().filter { it.isDigit() }
                    pDigits == queryDigits || (queryNoLeadingZeros.isNotEmpty() && pDigits.trimStart('0') == queryNoLeadingZeros)
                }
            }

            if (prod == null) {
                prod = all.find { it.urunKodu.equals(query, ignoreCase = true) }
            }

            if (prod != null) {
                _lastScannedGameProduct.value = prod
                val currentList = _gameScannedProducts.value.toMutableList()
                if (currentList.none { it.id == prod.id }) {
                    currentList.add(0, prod)
                    _gameScannedProducts.value = currentList
                }
            } else {
                onProductNotFound()
            }
        }
    }

    fun completeGameSession(onFinished: (InspectionReport) -> Unit) {
        cancelTourSession()
    }

    fun deleteTurRaporu(id: Int) {
        viewModelScope.launch {
            repository.deleteTurRaporu(id)
        }
    }

    fun clearAllTurRaporlari() {
        viewModelScope.launch {
            repository.clearAllTurRaporlari()
        }
    }

    fun getKayitlarByTurId(turId: Int): Flow<List<TurKontrolKaydi>> {
        return repository.getKayitlarByTurId(turId)
    }

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

    // Helper for barcode lookup in normal mode
    fun handleBarcodeScanned(
        barkod: String,
        onFound: (Product) -> Unit,
        onNotFound: (String) -> Unit
    ) {
        viewModelScope.launch {
            val query = barkod.trim()
            val queryDigits = query.filter { it.isDigit() }
            val queryNoLeadingZeros = queryDigits.trimStart('0')

            val all = repository.getProductListDirect()
            var prod = all.find { it.barkod.equals(query, ignoreCase = true) }

            if (prod == null && queryDigits.isNotEmpty()) {
                prod = all.find { p ->
                    val pDigits = p.barkod.trim().filter { it.isDigit() }
                    pDigits == queryDigits || (queryNoLeadingZeros.isNotEmpty() && pDigits.trimStart('0') == queryNoLeadingZeros)
                }
            }

            if (prod == null) {
                prod = all.find { it.urunKodu.equals(query, ignoreCase = true) }
            }

            if (prod != null) {
                onFound(prod)
            } else {
                onNotFound(barkod)
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
