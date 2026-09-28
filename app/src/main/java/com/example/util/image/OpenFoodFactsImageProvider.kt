package com.example.util.image

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class OpenFoodFactsImageProvider : ProductImageProvider {

    override val providerName: String = "OpenFoodFacts"

    companion object {
        private const val TAG = "OpenFoodFactsProvider"
        private const val USER_AGENT = "SKT-Takip-App/2.0 (Android; Market-Stok-Yonetimi; https://github.com/cetintuncyurek6132-bot/Skttakip)"
        private const val TIMEOUT_MS = 5000
    }

    override suspend fun findProductImageUrl(barcode: String): String? = withContext(Dispatchers.IO) {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isBlank() || cleanBarcode.startsWith("NO_BARCODE_")) {
            return@withContext null
        }

        var connection: HttpURLConnection? = null
        try {
            val urlString = "https://world.openfoodfacts.org/api/v2/product/$cleanBarcode.json?fields=code,product_name,image_front_small_url,image_front_url,image_small_url,image_url"
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                instanceFollowRedirects = true
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "Barkod $cleanBarcode için HTTP yanıt kodu: $responseCode")
                return@withContext null
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            if (responseBody.isBlank()) return@withContext null

            val json = JSONObject(responseBody)
            val status = json.optInt("status", 0)
            if (status != 1) {
                // Ürün bulunamadı (status == 0)
                return@withContext null
            }

            val productObj = json.optJSONObject("product") ?: return@withContext null

            // Öncelik Sıralaması (Hafif ve hızlı küçük thumbnail öncelikli):
            // 1. image_front_small_url
            // 2. image_small_url
            // 3. image_front_url
            // 4. image_url
            val frontSmall = productObj.optString("image_front_small_url", "").trim()
            if (frontSmall.isNotBlank() && frontSmall.startsWith("http")) return@withContext frontSmall

            val small = productObj.optString("image_small_url", "").trim()
            if (small.isNotBlank() && small.startsWith("http")) return@withContext small

            val front = productObj.optString("image_front_url", "").trim()
            if (front.isNotBlank() && front.startsWith("http")) return@withContext front

            val general = productObj.optString("image_url", "").trim()
            if (general.isNotBlank() && general.startsWith("http")) return@withContext general

            null
        } catch (e: Exception) {
            Log.d(TAG, "Barkod $cleanBarcode için resim sorgulama hatası: ${e.message}")
            null
        } finally {
            connection?.disconnect()
        }
    }
}
