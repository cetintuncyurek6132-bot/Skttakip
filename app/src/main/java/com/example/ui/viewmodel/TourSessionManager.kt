package com.example.ui.viewmodel

import com.example.auth.UserManager
import com.example.data.InspectionReport
import com.example.data.MorningSktCheckEvaluator
import com.example.data.Product
import com.example.data.ProductRepository
import com.example.data.TurKontrolKaydi
import com.example.data.TurRaporu
import com.example.data.getDisplayName
import com.example.data.isDolapProduct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Encapsulates the Morning Tour / Control Game state and actions,
 * decoupling it from MainViewModel to reduce monolithic complexity.
 */
class TourSessionManager(
    private val repository: ProductRepository,
    private val scope: CoroutineScope,
    private val getAllProducts: () -> List<Product>
) {
    private val _gameTargetCategory = MutableStateFlow("Dolap Ürünleri")
    val gameTargetCategory: StateFlow<String> = _gameTargetCategory.asStateFlow()

    private val _gameScannedProducts = MutableStateFlow<List<Product>>(emptyList())
    val gameScannedProducts: StateFlow<List<Product>> = _gameScannedProducts.asStateFlow()

    private val _gameActive = MutableStateFlow(false)
    val gameActive: StateFlow<Boolean> = _gameActive.asStateFlow()

    private val _lastScannedGameProduct = MutableStateFlow<Product?>(null)
    val lastScannedGameProduct: StateFlow<Product?> = _lastScannedGameProduct.asStateFlow()

    private val _tourQueue = MutableStateFlow<List<Product>>(emptyList())
    val tourQueue: StateFlow<List<Product>> = _tourQueue.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(0)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    private val _tourLogs = MutableStateFlow<List<TurKontrolKaydi>>(emptyList())
    val tourLogs: StateFlow<List<TurKontrolKaydi>> = _tourLogs.asStateFlow()

    private val _tourFinished = MutableStateFlow(false)
    val tourFinished: StateFlow<Boolean> = _tourFinished.asStateFlow()

    private val _isTourPaused = MutableStateFlow(false)
    val isTourPaused: StateFlow<Boolean> = _isTourPaused.asStateFlow()

    private val _tourStartTime = MutableStateFlow(0L)
    val tourStartTime: StateFlow<Long> = _tourStartTime.asStateFlow()

    private val _tourScore = MutableStateFlow(0)
    val tourScore: StateFlow<Int> = _tourScore.asStateFlow()

    private val _tourStreak = MutableStateFlow(0)
    val tourStreak: StateFlow<Int> = _tourStreak.asStateFlow()

    private val _lastActionMessage = MutableStateFlow<String?>(null)
    val lastActionMessage: StateFlow<String?> = _lastActionMessage.asStateFlow()

    private val _lastSavedTourRaporu = MutableStateFlow<TurRaporu?>(null)
    val lastSavedTourRaporu: StateFlow<TurRaporu?> = _lastSavedTourRaporu.asStateFlow()

    val allTurRaporlari: StateFlow<List<TurRaporu>> = repository.allTurRaporlari
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getKayitlarByTurId(turId: Int): Flow<List<TurKontrolKaydi>> {
        return repository.getKayitlarByTurId(turId)
    }

    fun setGameTargetCategory(category: String) {
        _gameTargetCategory.value = category
    }

    fun startTourSession() {
        val allProds = getAllProducts()

        if (allProds.isEmpty()) {
            _tourQueue.value = emptyList()
            _currentQueueIndex.value = 0
            _tourLogs.value = emptyList()
            _tourFinished.value = false
            _isTourPaused.value = false
            _tourScore.value = 0
            _tourStreak.value = 0
            _lastActionMessage.value = null
            _lastSavedTourRaporu.value = null
            _gameActive.value = true
            _tourStartTime.value = System.currentTimeMillis()
            return
        }

        // Filter strictly for products with SKT <= 20 days
        val filteredEligible = allProds.filter { p ->
            p.sktTarihi > 0L && p.getRemainingDays() <= 20
        }

        val dolapProds = filteredEligible.filter { p ->
            p.isDolapProduct()
        }.sortedWith(
            compareBy<Product> { it.getRemainingDays() }
                .thenByDescending { it.stokAdedi }
                .thenBy { it.urunAdi }
        )

        val gidaProds = filteredEligible.filter { p ->
            !p.isDolapProduct()
        }.sortedWith(
            compareBy<Product> { it.getRemainingDays() }
                .thenByDescending { it.stokAdedi }
                .thenBy { it.urunAdi }
        )

        val fullQueue = dolapProds + gidaProds

        _gameTargetCategory.value = "Tüm Mağaza Kontrol Turu"
        _tourQueue.value = fullQueue
        _currentQueueIndex.value = 0
        _tourLogs.value = emptyList()
        _tourFinished.value = false
        _isTourPaused.value = false
        _tourScore.value = 0
        _tourStreak.value = 0
        _lastActionMessage.value = null
        _lastSavedTourRaporu.value = null
        _gameActive.value = true
        _tourStartTime.value = System.currentTimeMillis()
    }

    fun pauseTourSession() {
        _isTourPaused.value = true
    }

    fun resumeTourSession() {
        _isTourPaused.value = false
    }

    fun clearLastActionMessage() {
        _lastActionMessage.value = null
    }

    fun recordTourSold(product: Product, soldCount: Int) {
        scope.launch {
            val safeSoldCount = maxOf(1, soldCount)
            val newStock = maxOf(0, product.stokAdedi - safeSoldCount)
            repository.updateProductStock(product.id, newStock)

            val currentPersonel = UserManager.currentUser.value?.fullName ?: "Görevli Ekip"

            val log = TurKontrolKaydi(
                productId = product.id,
                urunAdiSnapshot = product.getDisplayName(),
                barkodSnapshot = product.barkod,
                kategoriSnapshot = product.kategori,
                durum = "SATILDI",
                islemAdedi = safeSoldCount,
                kontrolTarihi = System.currentTimeMillis(),
                personelSnapshot = currentPersonel
            )
            _tourLogs.value = _tourLogs.value + log

            val newStreak = _tourStreak.value + 1
            _tourStreak.value = newStreak
            val streakBonus = (newStreak / 3) * 2
            _tourScore.value += 10 + streakBonus
            _lastActionMessage.value = "+$safeSoldCount adet stoktan düşüldü"

            val nextIndex = _currentQueueIndex.value + 1
            if (nextIndex >= _tourQueue.value.size) {
                finishTourInternal(isEarlyExit = false)
            } else {
                _currentQueueIndex.value = nextIndex
            }
        }
    }

    fun recordTourFire(product: Product, fireCount: Int) {
        scope.launch {
            val safeFireCount = maxOf(1, fireCount)
            val newStock = maxOf(0, product.stokAdedi - safeFireCount)
            repository.updateProductStock(product.id, newStock)

            val currentPersonel = UserManager.currentUser.value?.fullName ?: "Görevli Ekip"

            val log = TurKontrolKaydi(
                productId = product.id,
                urunAdiSnapshot = product.getDisplayName(),
                barkodSnapshot = product.barkod,
                kategoriSnapshot = product.kategori,
                durum = "FIRE",
                islemAdedi = safeFireCount,
                kontrolTarihi = System.currentTimeMillis(),
                personelSnapshot = currentPersonel
            )
            _tourLogs.value = _tourLogs.value + log

            val newStreak = _tourStreak.value + 1
            _tourStreak.value = newStreak
            _tourScore.value += 2
            _lastActionMessage.value = "$safeFireCount adet fire kaydedildi"

            val nextIndex = _currentQueueIndex.value + 1
            if (nextIndex >= _tourQueue.value.size) {
                finishTourInternal(isEarlyExit = false)
            } else {
                _currentQueueIndex.value = nextIndex
            }
        }
    }

    fun recordTourNotr(product: Product) {
        scope.launch {
            repository.updateProductStock(product.id, product.stokAdedi)

            val currentPersonel = UserManager.currentUser.value?.fullName ?: "Görevli Ekip"

            val log = TurKontrolKaydi(
                productId = product.id,
                urunAdiSnapshot = product.getDisplayName(),
                barkodSnapshot = product.barkod,
                kategoriSnapshot = product.kategori,
                durum = "NOTR",
                islemAdedi = 0,
                kontrolTarihi = System.currentTimeMillis(),
                personelSnapshot = currentPersonel
            )
            _tourLogs.value = _tourLogs.value + log

            val newStreak = _tourStreak.value + 1
            _tourStreak.value = newStreak
            val streakBonus = (newStreak / 5) * 2
            _tourScore.value += 5 + streakBonus
            _lastActionMessage.value = "Ürün rafta duruyor"

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
            scope.launch {
                if (lastLog.durum == "SATILDI" || lastLog.durum == "FIRE") {
                    val prod = getAllProducts().find { it.id == lastLog.productId }
                    if (prod != null) {
                        val restoredStock = prod.stokAdedi + lastLog.islemAdedi
                        repository.updateProductStock(prod.id, restoredStock)
                    }
                }
                _tourLogs.value = logs.dropLast(1)
                _currentQueueIndex.value = currentIndex - 1
                _tourStreak.value = maxOf(0, _tourStreak.value - 1)
                _lastActionMessage.value = "Son işlem geri alındı"
            }
        }
    }

    fun cancelTourSession() {
        if (_tourLogs.value.isNotEmpty()) {
            finishTourInternal(isEarlyExit = true)
        } else {
            _gameActive.value = false
            _isTourPaused.value = false
            _tourFinished.value = false
        }
    }

    private fun finishTourInternal(isEarlyExit: Boolean) {
        scope.launch {
            val logs = _tourLogs.value
            val satilanLogs = logs.filter { it.durum == "SATILDI" }
            val fireLogs = logs.filter { it.durum == "FIRE" }

            val satilanUrunCount = satilanLogs.size
            val satilanTotalCount = satilanLogs.sumOf { it.islemAdedi }
            val fireUrunCount = fireLogs.size
            val fireTotalCount = fireLogs.sumOf { it.islemAdedi }
            val notrUrunCount = logs.count { it.durum == "NOTR" }

            val allProds = getAllProducts()
            var totalFireCost = 0.0
            val fireCategoryMap = mutableMapOf<String, Int>()

            fireLogs.forEach { fl ->
                val prod = allProds.find { it.id == fl.productId }
                val unitPrice = prod?.fiyat ?: 35.0
                totalFireCost += unitPrice * fl.islemAdedi
                val cat = prod?.kategori ?: fl.kategoriSnapshot.ifBlank { "Genel Gıda" }
                fireCategoryMap[cat] = (fireCategoryMap[cat] ?: 0) + fl.islemAdedi
            }

            val topFireCat = fireCategoryMap.maxByOrNull { it.value }?.key ?: if (fireLogs.isNotEmpty()) "Dolap / Şarküteri" else "Yok"

            val durationSeconds = if (_tourStartTime.value > 0L) {
                ((System.currentTimeMillis() - _tourStartTime.value) / 1000L).coerceAtLeast(1L)
            } else 0L

            val currentPersonel = UserManager.currentUser.value?.fullName ?: "Görevli Ekip"

            val tourRaporu = TurRaporu(
                turTarihi = System.currentTimeMillis(),
                hedefReyon = _gameTargetCategory.value,
                toplamUrunSayisi = logs.size,
                satilanUrunSayisi = satilanUrunCount,
                toplamSatilanAdet = satilanTotalCount,
                fireUrunSayisi = fireUrunCount,
                toplamFireAdet = fireTotalCount,
                notrUrunSayisi = notrUrunCount,
                tamamlandiMi = !isEarlyExit,
                turSuresiSaniye = durationSeconds,
                toplamPuan = _tourScore.value,
                tahminiFireMaliyeti = totalFireCost,
                enCokFireKategori = topFireCat,
                personelAdi = currentPersonel
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
                fireTutari = if (totalFireCost > 0.0) totalFireCost else (fireTotalCount * 35.0)
            )
            repository.insertReport(legacyReport)

            _gameActive.value = false
            _isTourPaused.value = false
            _tourFinished.value = true
        }
    }

    fun resetTourState() {
        _gameActive.value = false
        _isTourPaused.value = false
        _tourFinished.value = false
        _tourQueue.value = emptyList()
        _currentQueueIndex.value = 0
        _tourLogs.value = emptyList()
        _tourScore.value = 0
        _tourStreak.value = 0
        _lastActionMessage.value = null
        _lastSavedTourRaporu.value = null
    }

    fun startGameSession() {
        startTourSession()
    }

    fun cancelGameSession() {
        cancelTourSession()
    }

    fun completeGameSession(onFinished: (InspectionReport) -> Unit) {
        cancelTourSession()
    }

    fun onGameScanBarcode(barkod: String, onProductNotFound: () -> Unit) {
        scope.launch {
            val all = repository.getProductListDirect()
            val evalResult = MorningSktCheckEvaluator.evaluateScannedBarcode(
                rawCode = barkod,
                inventory = all
            )

            val prod = evalResult.product
            if (prod != null) {
                _lastScannedGameProduct.value = prod
                val currentList = _gameScannedProducts.value.toMutableList()
                if (currentList.none { it.id == prod.id }) {
                    currentList.add(0, prod)
                    _gameScannedProducts.value = currentList
                }

                val queueIdx = _tourQueue.value.indexOfFirst { it.id == prod.id || it.barkod.equals(prod.barkod, ignoreCase = true) }
                if (queueIdx != -1) {
                    _currentQueueIndex.value = queueIdx
                    _lastActionMessage.value = "${evalResult.badgeTitle} (${evalResult.daysDifferenceText})"
                } else {
                    _lastActionMessage.value = "Taranan Ürün: ${evalResult.badgeTitle}"
                }
            } else {
                onProductNotFound()
            }
        }
    }

    fun deleteTurRaporu(id: Int) {
        scope.launch {
            repository.deleteTurRaporu(id)
        }
    }

    fun clearAllTurRaporlari() {
        scope.launch {
            repository.clearAllTurRaporlari()
        }
    }
}
