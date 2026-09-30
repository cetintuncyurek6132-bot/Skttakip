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
        val productCode = qrData.productCode?.trim()?.takeIf { it.isNotBlank() }
        val newPrice = qrData.price

        val all = repository.getProductListDirect()

        // SADECE KATI BİREBİR EŞLEŞME (Exact Match)
        // Barkod veya Ürün Kodu veritabanında kesin olarak eşleşmelidir
        val targetProducts = all.filter { p ->
            val pBarcode = p.barkod.trim()
            val pCode = p.urunKodu.trim()

            // 1. Barkod ile birebir tam eşleşme (EAN-13, EAN-8 vb.)
            (realBarcode.isNotBlank() && !realBarcode.startsWith("NO_BARCODE_") && pBarcode.equals(realBarcode, ignoreCase = true)) ||
            // 2. Ürün kodu ile birebir tam eşleşme (örn: 16000491)
            (productCode != null && pCode.isNotBlank() && pCode.equals(productCode, ignoreCase = true)) ||
            // 3. Sütunların yerel veritabanında yer değiştirmiş olması ihtimaline karşı çapraz tam eşleşme
            (realBarcode.isNotBlank() && !realBarcode.startsWith("NO_BARCODE_") && pCode.equals(realBarcode, ignoreCase = true)) ||
            (productCode != null && pBarcode.isNotBlank() && pBarcode.equals(productCode, ignoreCase = true))
        }

        // Birebir eşleşen ürün yoksa ASLA gevşek/benzer arama yapma ve işlemi doğrudan HATALI olarak bildir
        if (targetProducts.isEmpty()) {
            val identifier = if (realBarcode.isNotBlank()) realBarcode else (productCode ?: rawInput.take(15))
            withContext(Dispatchers.Main) {
                onResult("⚠️ Barkod / Kod '$identifier' ile eşleşen ürün bulunamadı.", false)
            }
            return
        }

        var updatedCount = 0
        var sampleName = ""

        targetProducts.forEach { prod ->
            val finalBarcode = if (realBarcode.length >= 8 && !realBarcode.startsWith("NO_BARCODE_") && realBarcode != prod.urunKodu) realBarcode else prod.barkod
            val finalPrice = newPrice ?: prod.fiyat
            if (finalBarcode != prod.barkod || finalPrice != prod.fiyat) {
                val updated = prod.copy(
                    barkod = finalBarcode,
                    fiyat = finalPrice
                )
                repository.insertOrUpdateProduct(updated)
                updatedCount++
                if (sampleName.isBlank()) sampleName = prod.urunAdi
            } else {
                if (sampleName.isBlank()) sampleName = prod.urunAdi
            }
        }

        withContext(Dispatchers.Main) {
            val priceStr = if (newPrice != null) {
                val formatted = if (newPrice % 1.0 == 0.0) newPrice.toInt().toString() else String.format(java.util.Locale.US, "%.2f", newPrice)
                " -> $formatted TL"
            } else ""

            if (updatedCount > 0) {
                onResult("Son İşlem: $sampleName$priceStr güncellendi", true)
            } else {
                onResult("Son İşlem: $sampleName zaten güncel ($realBarcode$priceStr)", true)
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
        val matchingList = if (queryBarcode.isNotBlank()) {
            val byClean = all.findMatchingProducts(queryBarcode)
            if (byClean.isNotEmpty()) byClean else all.findMatchingProducts(barkod)
        } else {
            all.findMatchingProducts(barkod)
        }
        var prod: Product? = matchingList.firstOrNull { it.urunAdi.isNotBlank() && !it.urunAdi.equals("İSİMSİZ ÜRÜN", ignoreCase = true) }
            ?: matchingList.firstOrNull()

        if (prod != null && prod.urunAdi.isNotBlank() && !prod.urunAdi.equals("İSİMSİZ ÜRÜN", ignoreCase = true)) {
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
