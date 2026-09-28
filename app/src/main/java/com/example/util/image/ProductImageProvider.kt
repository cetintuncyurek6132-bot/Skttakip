package com.example.util.image

/**
 * Ürün görseli sağlayıcıları için genişletilebilir arayüz.
 * İleride yeni bir servis veya lokal veritabanı kaynağı eklenmek istendiğinde
 * bu arayüzü uygulayan yeni bir sınıf tanımlamak yeterlidir.
 */
interface ProductImageProvider {
    val providerName: String

    /**
     * Verilen barkoda ait ürün fotoğraf URL'sini arar.
     * Bulunamazsa veya hata oluşursa null döner.
     */
    suspend fun findProductImageUrl(barcode: String): String?
}
