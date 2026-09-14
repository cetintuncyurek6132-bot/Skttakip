package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ExpiryStatus
import com.example.data.InspectionReport
import com.example.data.Product
import com.example.data.ProductRepository
import com.example.data.getTodayMidnightMillis
import com.example.sync.CloudSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ProductFilter(val label: String) {
    ALL("TÜMÜ"),
    IMPORTANT("🔥 ÖNEMLİ"),
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
    val allRegisteredCount: Int = 0,
    val expiredCount: Int = 0,
    val criticalCount: Int = 0,
    val soonCount: Int = 0,
    val importantCount: Int = 0,
    val attentionProducts: List<Product> = emptyList(),
    val removeProducts: List<Product> = emptyList(),
    val nearExpiryProducts: List<Product> = emptyList(),
    val unreadNotificationCount: Int = 3
)

class MainViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    // Loading State
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

    // Notifications Read State
    private val _notificationsRead = MutableStateFlow(false)
    val notificationsRead: StateFlow<Boolean> = _notificationsRead.asStateFlow()

    fun markNotificationsAsRead() {
        _notificationsRead.value = true
    }

    // Repository flows for calculating Dashboard state
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
        }
    }

    val dashboardState: StateFlow<DashboardState> = combine(
        allProducts,
        allReports,
        _notificationsRead
    ) { products, reports, isRead ->
        val todayMidnight = getTodayMidnightMillis()
        var expired = 0
        var critical = 0
        var soon = 0
        var important = 0
        var sktEnteredCount = 0

        val validSktProducts = ArrayList<Product>(products.size)

        for (prod in products) {
            if (prod.sktTarihi > 0L) {
                sktEnteredCount++
                validSktProducts.add(prod)
                when (prod.getExpiryStatus(todayMidnight)) {
                    ExpiryStatus.EXPIRED -> expired++
                    ExpiryStatus.CRITICAL -> critical++
                    ExpiryStatus.SOON -> soon++
                    else -> {}
                }
                if ((prod.getRemainingDays(todayMidnight) in 0..30 && prod.stokAdedi >= 10) || prod.isImportant) {
                    important++
                }
            }
        }

        // Sort by sktTarihi ascending once
        validSktProducts.sortBy { it.sktTarihi }

        // Deduplicate in a single pass using a fast composite key
        val seenKeys = HashSet<String>(validSktProducts.size)
        val deduplicated = ArrayList<Product>(validSktProducts.size)
        for (prod in validSktProducts) {
            val key = if (prod.barkod.isNotBlank()) "${prod.barkod}_${prod.sktTarihi}" else "${prod.urunAdi.trim().lowercase()}_${prod.sktTarihi}"
            if (seenKeys.add(key)) {
                deduplicated.add(prod)
            }
        }

        val attention = deduplicated.take(10)
        val removeProds = ArrayList<Product>()
        val nearExpiryProds = ArrayList<Product>()
        for (prod in deduplicated) {
            val rem = prod.getRemainingDays(todayMidnight)
            if (rem <= 0L) {
                removeProds.add(prod)
            } else if (rem in 1L..7L) {
                nearExpiryProds.add(prod)
            }
        }

        val activeAlertsCount = expired + critical
        val unreadCount = if (isRead) 0 else (if (activeAlertsCount > 0) activeAlertsCount else 1)

        DashboardState(
            totalCount = sktEnteredCount,
            allRegisteredCount = products.size,
            expiredCount = expired,
            criticalCount = critical,
            soonCount = soon,
            importantCount = important,
            attentionProducts = attention,
            removeProducts = removeProds,
            nearExpiryProducts = nearExpiryProds,
            unreadNotificationCount = unreadCount
        )
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )

    val unreadNotificationCount: StateFlow<Int> = combine(
        dashboardState,
        _notificationsRead
    ) { state, isRead ->
        if (isRead) 0 else state.unreadNotificationCount
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    class Factory(private val repository: ProductRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
