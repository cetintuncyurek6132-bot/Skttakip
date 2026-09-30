package com.example.util.image

import com.example.data.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Mobil veri tasarrufu amacıyla internet üzerinden görsel arama ve indirme işlemleri
 * tamamen devre dışı bırakılmıştır.
 */
object ProductImageManager {

    /**
     * İnternet üzerinden görsel arama kapalıdır.
     * Mobil veri harcanmaması için doğrudan null döner.
     */
    suspend fun getOrFetchProductImageUrl(
        barcode: String,
        existingResimUrl: String? = null,
        repository: ProductRepository? = null
    ): String? = withContext(Dispatchers.IO) {
        // Ağ isteği atılmaz, doğrudan null döner
        null
    }
}

