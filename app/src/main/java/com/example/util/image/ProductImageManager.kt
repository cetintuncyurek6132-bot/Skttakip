package com.example.util.image

import android.content.Context
import android.util.Log
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Ürün fotoğraflarını arka planda arayan, önbelleğe alan ve
 * gereksiz ağ isteklerini engelleyen merkezi görsel yöneticisi.
 */
object ProductImageManager {

    private const val TAG = "ProductImageManager"

    // Sağlayıcılar zinciri (Genişletilebilir)
    private val providers: List<ProductImageProvider> = listOf(
        OpenFoodFactsImageProvider()
    )

    // Başarılı URL önbelleği (barkod -> resimUrl)
    private val memoryUrlCache = ConcurrentHashMap<String, String>()

    // Başarısız arama zaman damgaları (barkod -> sonDenemeMillis)
    // Aynı barkod için 30 dakika boyunca tekrar tekrar istek atılmasını önler
    private val failedLookupTimestamps = ConcurrentHashMap<String, Long>()
    private const val FAILED_LOOKUP_COOLDOWN_MS = 30 * 60 * 1000L // 30 dakika

    // Eşzamanlı aramalarda aynı barkod için birden fazla istek yapılmasını engelleyen kilitler
    private val inFlightRequests = ConcurrentHashMap<String, Mutex>()

    @Volatile
    private var customImageLoader: ImageLoader? = null

    /**
     * Coil için disk ve bellek sınırları optimize edilmiş ImageLoader döndürür.
     * Disk önbelleği: ~100 MB
     * Bellek önbelleği: %20 RAM
     */
    fun getImageLoader(context: Context): ImageLoader {
        return customImageLoader ?: synchronized(this) {
            customImageLoader ?: ImageLoader.Builder(context.applicationContext)
                .memoryCache {
                    MemoryCache.Builder(context.applicationContext)
                        .maxSizePercent(0.20)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(File(context.applicationContext.cacheDir, "product_images_cache"))
                        .maxSizeBytes(100L * 1024 * 1024) // 100 MB disk limiti
                        .build()
                }
                .crossfade(true)
                .respectCacheHeaders(false) // Offline-first hızlı önbellek gösterimi
                .build()
                .also { customImageLoader = it }
        }
    }

    /**
     * Verilen barkoda ait görseli bulur veya önbellekten döner.
     * Eğer görsel bulunursa veritabanını günceller.
     */
    suspend fun getOrFetchProductImageUrl(
        barcode: String,
        existingResimUrl: String? = null,
        repository: ProductRepository? = null
    ): String? = withContext(Dispatchers.IO) {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isBlank() || cleanBarcode.startsWith("NO_BARCODE_")) {
            return@withContext null
        }

        // 1. Zaten mevcut geçerli bir URL varsa doğrudan kullan ve önbelleğe al
        if (!existingResimUrl.isNullOrBlank()) {
            memoryUrlCache[cleanBarcode] = existingResimUrl
            return@withContext existingResimUrl
        }

        // 2. Bellek önbelleğinde varsa anında dön
        memoryUrlCache[cleanBarcode]?.let { cached ->
            return@withContext cached
        }

        // 3. Yakın zamanda başarısız olduysa tekrar API'yi yorma (Cooldown kontrolü)
        val now = System.currentTimeMillis()
        val lastFailed = failedLookupTimestamps[cleanBarcode]
        if (lastFailed != null && (now - lastFailed) < FAILED_LOOKUP_COOLDOWN_MS) {
            return@withContext null
        }

        // 4. Eşzamanlı mükerrer istekleri engelle
        val mutex = inFlightRequests.computeIfAbsent(cleanBarcode) { Mutex() }
        mutex.withLock {
            // Kilidi aldıktan sonra önbelleği tekrar kontrol et
            memoryUrlCache[cleanBarcode]?.let { return@withContext it }

            var foundUrl: String? = null
            for (provider in providers) {
                try {
                    val url = provider.findProductImageUrl(cleanBarcode)
                    if (!url.isNullOrBlank()) {
                        foundUrl = url
                        Log.d(TAG, "Görsel bulundu ($cleanBarcode) -> $url [Sağlayıcı: ${provider.providerName}]")
                        break
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Sağlayıcı ${provider.providerName} hatası: ${e.message}")
                }
            }

            if (!foundUrl.isNullOrBlank()) {
                memoryUrlCache[cleanBarcode] = foundUrl
                failedLookupTimestamps.remove(cleanBarcode)

                // Veritabanındaki ürün kayıtlarına bu URL'yi kaydet
                repository?.updateProductImage(cleanBarcode, foundUrl)
                return@withContext foundUrl
            } else {
                // Başarısızlığı işaretle
                failedLookupTimestamps[cleanBarcode] = now
                return@withContext null
            }
        }
    }
}
