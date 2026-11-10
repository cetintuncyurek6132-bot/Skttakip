package com.example.ui.viewmodel

import com.example.data.AdetselKayit
import com.example.data.Product
import com.example.data.ProductRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Encapsulates Adetsel Sayım (Piece-based inventory counting) logic,
 * decoupling it from MainViewModel.
 */
class AdetselSayimManager(
    private val repository: ProductRepository,
    private val scope: CoroutineScope
) {
    val allAdetselKayitlari: StateFlow<List<AdetselKayit>> = repository.allAdetselKayitlari
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val yapilacakAdetselKayitlari: StateFlow<List<AdetselKayit>> = repository.yapilacakAdetselKayitlari
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val yapildiAdetselKayitlari: StateFlow<List<AdetselKayit>> = repository.yapildiAdetselKayitlari
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun cleanDuplicatePendingAdetsel() {
        scope.launch(Dispatchers.IO) {
            val pending = repository.getPendingAdetselKayitlarList()
            if (pending.size <= 1) return@launch

            val seenKeys = mutableSetOf<String>()
            for (item in pending) {
                val key = when {
                    item.barkod.isNotBlank() -> "B:${item.barkod.trim()}"
                    item.urunKodu.isNotBlank() -> "K:${item.urunKodu.trim()}"
                    item.productId > 0 -> "P:${item.productId}"
                    else -> "N:${item.urunAdi.trim().lowercase()}"
                }
                if (seenKeys.contains(key)) {
                    repository.deleteAdetselKayitById(item.id)
                } else {
                    seenKeys.add(key)
                }
            }
        }
    }

    fun addToAdetsel(product: Product, onComplete: ((Boolean) -> Unit)? = null) {
        scope.launch(Dispatchers.IO) {
            val pendingList = repository.getPendingAdetselKayitlarList()

            // Check if already in pending adetsel list to prevent duplicates
            val isAlreadyPending = pendingList.any { existing ->
                (product.id > 0 && existing.productId == product.id) ||
                (product.barkod.isNotBlank() && existing.barkod.equals(product.barkod, ignoreCase = true)) ||
                (product.urunKodu.isNotBlank() && existing.urunKodu.equals(product.urunKodu, ignoreCase = true)) ||
                (product.urunAdi.isNotBlank() && existing.urunAdi.trim().equals(product.urunAdi.trim(), ignoreCase = true))
            }

            if (isAlreadyPending) {
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false)
                }
                return@launch
            }

            val code = if (product.urunKodu.isNotBlank()) product.urunKodu else product.barkod
            val kayit = AdetselKayit(
                productId = product.id,
                urunAdi = product.urunAdi,
                urunKodu = code,
                barkod = product.barkod,
                kategori = product.kategori,
                eklenmeTarihi = System.currentTimeMillis(),
                yapildiMi = false,
                sayimSonucu = "",
                sayilanAdet = 0,
                farkAdet = 0,
                beklenenAdet = 0
            )
            val id = repository.insertAdetselKayit(kayit)
            withContext(Dispatchers.Main) {
                onComplete?.invoke(id > 0)
            }
        }
    }

    fun saveAdetselSayim(
        kayit: AdetselKayit,
        sonuc: String,
        fark: Int,
        sayilanAdet: Int = if (kayit.sayilanAdet > 0) kayit.sayilanAdet else maxOf(0, fark),
        notlar: String = "",
        onComplete: (() -> Unit)? = null
    ) {
        scope.launch(Dispatchers.IO) {
            val finalSayilan = if (kayit.sayilanAdet > 0) kayit.sayilanAdet else if (sayilanAdet > 0) sayilanAdet else 0
            val updated = kayit.copy(
                yapildiMi = true,
                sayimSonucu = sonuc,
                sayilanAdet = finalSayilan,
                farkAdet = fark,
                notlar = notlar,
                islemTarihi = System.currentTimeMillis()
            )
            repository.updateAdetselKayit(updated)
            withContext(Dispatchers.Main) {
                onComplete?.invoke()
            }
        }
    }

    fun undoAdetselKayit(kayit: AdetselKayit) {
        scope.launch(Dispatchers.IO) {
            val undone = kayit.copy(
                yapildiMi = false,
                sayimSonucu = "",
                sayilanAdet = 0,
                farkAdet = 0,
                islemTarihi = 0L
            )
            repository.updateAdetselKayit(undone)
        }
    }

    fun deleteAdetselKayit(id: Int) {
        scope.launch(Dispatchers.IO) {
            repository.deleteAdetselKayitById(id)
        }
    }

    fun clearCompletedAdetselKayitlar() {
        scope.launch(Dispatchers.IO) {
            repository.clearCompletedAdetselKayitlar()
        }
    }

    /**
     * Directly adds or updates a product count with the specified shelf quantity in the YAPILACAK list.
     * No DB stock comparison is performed; beklenenAdet and farkAdet are 0, sayimSonucu is "", and yapildiMi is false.
     */
    fun saveDirectCount(
        product: Product,
        countedQty: Int,
        notlar: String = "",
        onComplete: (() -> Unit)? = null
    ) {
        scope.launch(Dispatchers.IO) {
            val allKayitlar = repository.getAllAdetselKayitlarList()
            val existingKayit = allKayitlar.find { existing ->
                (product.id > 0 && existing.productId == product.id) ||
                (product.barkod.isNotBlank() && existing.barkod.equals(product.barkod, ignoreCase = true)) ||
                (product.urunKodu.isNotBlank() && existing.urunKodu.equals(product.urunKodu, ignoreCase = true)) ||
                (product.urunAdi.isNotBlank() && existing.urunAdi.trim().equals(product.urunAdi.trim(), ignoreCase = true))
            }

            if (existingKayit != null) {
                val updated = existingKayit.copy(
                    yapildiMi = false,
                    sayimSonucu = "",
                    sayilanAdet = countedQty,
                    farkAdet = 0,
                    beklenenAdet = 0,
                    notlar = if (notlar.isNotBlank()) notlar else existingKayit.notlar,
                    islemTarihi = System.currentTimeMillis()
                )
                repository.updateAdetselKayit(updated)
            } else {
                val code = if (product.urunKodu.isNotBlank()) product.urunKodu else product.barkod
                val newKayit = AdetselKayit(
                    productId = product.id,
                    urunAdi = product.urunAdi,
                    urunKodu = code,
                    barkod = product.barkod,
                    kategori = product.kategori,
                    eklenmeTarihi = System.currentTimeMillis(),
                    yapildiMi = false,
                    sayimSonucu = "",
                    sayilanAdet = countedQty,
                    farkAdet = 0,
                    beklenenAdet = 0,
                    notlar = notlar,
                    islemTarihi = System.currentTimeMillis()
                )
                repository.insertAdetselKayit(newKayit)
            }

            withContext(Dispatchers.Main) {
                onComplete?.invoke()
            }
        }
    }
}
