package com.example.util.image

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Mobil veri tasarrufu ve offline-first performans için internet görsel aramaları kapatılmıştır.
 */
class OpenFoodFactsImageProvider : ProductImageProvider {

    override val providerName: String = "OpenFoodFacts"

    override suspend fun findProductImageUrl(
        barcode: String,
        productName: String?,
        category: String?
    ): String? = withContext(Dispatchers.IO) {
        // İnternet üzerinden görsel sorgulamaları mobil veri tasarrufu amacıyla devre dışı bırakılmıştır
        null
    }
}

