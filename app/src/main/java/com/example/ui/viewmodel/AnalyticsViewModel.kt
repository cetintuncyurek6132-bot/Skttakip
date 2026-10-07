package com.example.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ProductRepository
import com.example.data.StockLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

@Immutable
data class PerformanceMetrics(
    val totalSold: Int = 0,
    val totalFire: Int = 0,
    val totalProcessed: Int = 0,
    val recoverySuccessRate: Int = 0,
    val fireRate: Int = 0,
    val hasRealLogs: Boolean = false
)

class AnalyticsViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    val allStockLogs: StateFlow<List<StockLog>> = repository.allStockLogs
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalSoldQuantity: StateFlow<Int> = repository.totalSoldQuantity
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val totalFireQuantity: StateFlow<Int> = repository.totalFireQuantity
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val performanceMetrics: StateFlow<PerformanceMetrics> = combine(
        repository.totalSoldQuantity,
        repository.totalFireQuantity
    ) { sold, fire ->
        val total = sold + fire
        if (total > 0) {
            val rate = ((sold.toDouble() / total.toDouble()) * 100.0).toInt().coerceIn(0, 100)
            val fRate = 100 - rate
            PerformanceMetrics(
                totalSold = sold,
                totalFire = fire,
                totalProcessed = total,
                recoverySuccessRate = rate,
                fireRate = fRate,
                hasRealLogs = true
            )
        } else {
            PerformanceMetrics(
                totalSold = 0,
                totalFire = 0,
                totalProcessed = 0,
                recoverySuccessRate = 0,
                fireRate = 0,
                hasRealLogs = false
            )
        }
    }
        .distinctUntilChanged()
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PerformanceMetrics()
    )

    suspend fun getSktEntryCountForBarcode(barcode: String): Int = withContext(Dispatchers.IO) {
        repository.getSktEntryCountForBarcode(barcode)
    }

    class Factory(private val repository: ProductRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AnalyticsViewModel(repository) as T
        }
    }
}
