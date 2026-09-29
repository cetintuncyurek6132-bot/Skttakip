package com.example.util.image

/**
 * Ürün görseli sağlayıcıları için arayüz.
 * Barkod, ürün adı veya kategori bilgisine göre görsel arar.
 */
interface ProductImageProvider {
    val providerName: String

    /**
     * Verilen barkoda ve/veya ürün adı/kategorisine ait görsel URL'sini arar.
     */
    suspend fun findProductImageUrl(
        barcode: String,
        productName: String? = null,
        category: String? = null
    ): String?
}
