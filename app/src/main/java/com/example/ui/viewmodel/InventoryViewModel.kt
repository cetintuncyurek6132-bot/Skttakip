package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ExpiryStatus
import com.example.data.Product
import com.example.data.ProductRepository
import com.example.data.getTodayMidnightMillis
import com.example.data.isDolapProduct
import com.example.data.matchesSearchQuery
import com.example.data.normalizeForSearch
import com.example.ui.ProductFilter
import com.example.ui.ProductGroupFilter
import com.example.util.ProductCsvImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

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
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
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

        val result = ArrayList<Product>(products.size)
        for (prod in products) {
            // 1. Group Filter
            if (groupFilter == ProductGroupFilter.DOLAP && !prod.isDolapProduct()) continue
            if (groupFilter == ProductGroupFilter.GIDA && prod.isDolapProduct()) continue

            // 2. Status Filter
            val matchesFilter = when (filter) {
                ProductFilter.ALL -> prod.sktTarihi > 0L && prod.stokAdedi > 0
                ProductFilter.IMPORTANT -> (prod.sktTarihi > 0L && prod.getRemainingDays(todayMidnight) in 1..30 && prod.stokAdedi >= 10) || prod.isImportant
                ProductFilter.EXPIRED -> prod.sktTarihi > 0L && prod.getExpiryStatus(todayMidnight) == ExpiryStatus.EXPIRED
                ProductFilter.CRITICAL -> prod.sktTarihi > 0L && prod.getExpiryStatus(todayMidnight) == ExpiryStatus.CRITICAL
                ProductFilter.SOON -> prod.sktTarihi > 0L && prod.getExpiryStatus(todayMidnight) == ExpiryStatus.SOON
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
            val currentEditing = _editingProduct.value

            val savedProduct: Product = if (currentEditing != null) {
                val productToSave = currentEditing.copy(
                    barkod = finalBarkod,
                    urunKodu = urunKodu.trim(),
                    urunAdi = urunAdi.uppercase().trim(),
                    kategori = kategori,
                    sktTarihi = if (sktTarihi > 0L) sktTarihi else currentEditing.sktTarihi,
                    stokAdedi = if (stokAdedi > 0) stokAdedi else currentEditing.sktTarihi.let { if (it > 0L) currentEditing.stokAdedi else 0 },
                    fiyat = fiyat,
                    isImportant = isImportant
                )
                repository.insertOrUpdateProduct(productToSave)

                // Sync sibling items of same barcode / urunKodu
                val siblings = if (currentEditing.barkod.isNotBlank()) {
                    repository.getProductsByBarcode(currentEditing.barkod)
                } else if (currentEditing.urunKodu.isNotBlank()) {
                    allProducts.value.filter { it.urunKodu == currentEditing.urunKodu }
                } else emptyList()

                siblings.filter { it.id != currentEditing.id }.forEach { sibling ->
                    val updatedSibling = sibling.copy(
                        barkod = finalBarkod,
                        urunKodu = urunKodu.trim().ifBlank { sibling.urunKodu },
                        urunAdi = urunAdi.uppercase().trim().ifBlank { sibling.urunAdi },
                        kategori = kategori.ifBlank { sibling.kategori },
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
            repository.updateProductStock(product, newStock)
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
        val existingKeys = allProducts.value.map {
            "${it.barkod.trim().lowercase()}_${it.urunKodu.trim().lowercase()}_${it.urunAdi.trim().lowercase()}"
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
