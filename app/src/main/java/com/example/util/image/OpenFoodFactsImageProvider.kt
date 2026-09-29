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
        private const val TIMEOUT_MS = 4000
    }

    override suspend fun findProductImageUrl(
        barcode: String,
        productName: String?,
        category: String?
    ): String? = withContext(Dispatchers.IO) {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isBlank() || cleanBarcode.startsWith("NO_BARCODE_")) {
            return@withContext null
        }

        // 1. Önce Türkiye API uç noktasını dene, sonra Dünya API uç noktasını dene
        val endpoints = listOf(
            "https://tr.openfoodfacts.org/api/v2/product/$cleanBarcode.json?fields=code,product_name,image_front_small_url,image_front_url,image_small_url,image_url",
            "https://world.openfoodfacts.org/api/v2/product/$cleanBarcode.json?fields=code,product_name,image_front_small_url,image_front_url,image_small_url,image_url"
        )

        for (urlString in endpoints) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(urlString)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", USER_AGENT)
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    instanceFollowRedirects = true
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                    if (responseBody.isNotBlank()) {
                        val json = JSONObject(responseBody)
                        if (json.optInt("status", 0) == 1) {
                            val productObj = json.optJSONObject("product")
                            if (productObj != null) {
                                val frontSmall = productObj.optString("image_front_small_url", "").trim()
                                if (frontSmall.isNotBlank() && frontSmall.startsWith("http")) return@withContext frontSmall

                                val small = productObj.optString("image_small_url", "").trim()
                                if (small.isNotBlank() && small.startsWith("http")) return@withContext small

                                val front = productObj.optString("image_front_url", "").trim()
                                if (front.isNotBlank() && front.startsWith("http")) return@withContext front

                                val general = productObj.optString("image_url", "").trim()
                                if (general.isNotBlank() && general.startsWith("http")) return@withContext general
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "OFF sorgu hatası ($cleanBarcode): ${e.message}")
            } finally {
                connection?.disconnect()
            }
        }
        null
    }
}
