package com.example.ui.viewmodel

import com.example.data.Product
import com.example.data.ProductRepository
import com.example.data.findMatchingProducts
import com.example.data.parseShelfQrPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BarcodeScanProcessor {

    suspend fun fixProductBarcodeAndPriceFromQr(
        repository: ProductRepository,
        rawInput: String,
        onResult: (message: String, isSuccess: Boolean) -> Unit
    ) {
        val qrData = parseShelfQrPayload(rawInput)
        val realBarcode = qrData.barcode.trim()
        val productCode = qrData.productCode?.trim()
        val newPrice = qrData.price

        val all = repository.getProductListDirect()
        var targetProducts = all.filter { p ->
            (productCode != null && productCode.isNotBlank() && p.urunKodu.equals(productCode, ignoreCase = true)) ||
            (productCode != null && productCode.isNotBlank() && p.barkod.equals(productCode, ignoreCase = true)) ||
            p.urunKodu.equals(realBarcode, ignoreCase = true) ||
            p.urunKodu.equals(rawInput.trim(), ignoreCase = true)
        }

        if (targetProducts.isEmpty()) {
            targetProducts = all.findMatchingProducts(rawInput)
        }

        if (targetProducts.isEmpty()) {
            withContext(Dispatchers.Main) {
                onResult("⚠️ Ürün kodu '${productCode ?: realBarcode}' ile eşleşen ürün bulunamadı.", false)
            }
            return
        }

        var updatedCount = 0
        var sampleName = ""

        targetProducts.forEach { prod ->
            val finalBarcode = if (realBarcode.length >= 8 && realBarcode != prod.urunKodu) realBarcode else prod.barkod
            val finalPrice = newPrice ?: prod.fiyat
            if (finalBarcode != prod.barkod || finalPrice != prod.fiyat) {
                val updated = prod.copy(
                    barkod = finalBarcode,
                    fiyat = finalPrice
                )
                repository.insertOrUpdateProduct(updated)
                updatedCount++
                if (sampleName.isBlank()) sampleName = prod.urunAdi
            }
        }

        withContext(Dispatchers.Main) {
            if (updatedCount > 0) {
                val priceInfo = if (newPrice != null) " • Fiyat: $newPrice TL" else ""
                onResult("✅ '$sampleName' ($updatedCount kayıt) barkodu: $realBarcode$priceInfo olarak güncellendi!", true)
            } else {
                onResult("ℹ️ '$sampleName' ürünü zaten güncel barkoda ($realBarcode) sahip.", true)
            }
        }
    }

    suspend fun handleBarcodeScanned(
        repository: ProductRepository,
        barkod: String,
        onUpdatePrice: suspend (Product, Double?) -> Unit,
        onFound: (Product) -> Unit,
        onNotFound: (String) -> Unit
    ) {
        val qrData = parseShelfQrPayload(barkod)
        val queryBarcode = qrData.barcode.trim()
        val queryProductCode = qrData.productCode?.trim()
        val scannedPrice = qrData.price

        val all = repository.getProductListDirect()
        val matchingList = all.findMatchingProducts(barkod)
        var prod: Product? = matchingList.firstOrNull()

        if (prod != null) {
            val isProductCodeMatch = queryProductCode.isNullOrBlank() ||
                prod.urunKodu.isBlank() ||
                prod.urunKodu.equals(queryProductCode, ignoreCase = true)

            var updatedProd = prod

            // Auto-fix barcode in database if queryBarcode is a valid EAN/package barcode
            val queryDigits = queryBarcode.filter { it.isDigit() }
            if (queryDigits.length >= 8 && queryBarcode != prod.barkod && isProductCodeMatch) {
                val corrected = prod.copy(barkod = queryBarcode)
                repository.insertOrUpdateProduct(corrected)
                updatedProd = corrected
            }

            if (scannedPrice != null && scannedPrice > 0.0 && isProductCodeMatch) {
                onUpdatePrice(updatedProd, scannedPrice)
                updatedProd = updatedProd.copy(fiyat = scannedPrice)
            }

            withContext(Dispatchers.Main) {
                onFound(updatedProd)
            }
        } else {
            withContext(Dispatchers.Main) {
                onNotFound(barkod)
            }
        }
    }
}
