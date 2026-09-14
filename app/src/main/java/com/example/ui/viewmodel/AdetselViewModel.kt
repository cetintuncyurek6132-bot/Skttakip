package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AdetselKayit
import com.example.data.Product
import com.example.data.ProductRepository
import kotlinx.coroutines.flow.StateFlow

class AdetselViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    private val adetselSayimManager = AdetselSayimManager(
        repository = repository,
        scope = viewModelScope
    )

    // Adetsel Sayım Akışları
    val allAdetselKayitlari: StateFlow<List<AdetselKayit>> = adetselSayimManager.allAdetselKayitlari
    val yapilacakAdetselKayitlari: StateFlow<List<AdetselKayit>> = adetselSayimManager.yapilacakAdetselKayitlari
    val yapildiAdetselKayitlari: StateFlow<List<AdetselKayit>> = adetselSayimManager.yapildiAdetselKayitlari

    // Adetsel İşlem Metotları
    fun cleanDuplicatePendingAdetsel() = adetselSayimManager.cleanDuplicatePendingAdetsel()

    fun addToAdetsel(product: Product, onComplete: ((Boolean) -> Unit)? = null) =
        adetselSayimManager.addToAdetsel(product, onComplete)

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

    class Factory(private val repository: ProductRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AdetselViewModel(repository) as T
        }
    }
}
