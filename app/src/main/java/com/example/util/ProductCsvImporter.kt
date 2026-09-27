package com.example.util

import com.example.data.Product
import java.util.Calendar

data class CsvParseResult(
    val productsToInsert: List<Product>,
    val skippedLineCount: Int,
    val duplicateCount: Int = 0
)

object ProductCsvImporter {

    fun isGarbageText(text: String): Boolean {
        if (text.isEmpty()) return false
        if (text.contains("\uFFFD")) return true
        if (text.contains("_rels") || text.contains("[Content_Types]") || text.contains("<xml") ||
            text.contains("PK\u0003") || text.contains("xl/workbooks") || text.contains("Root Entry")
        ) return true
        if (text.any { it.code in 0..8 || it.code in 14..31 || it.code == 127 }) return true
        return false
    }

    fun cleanField(field: String): String {
        return field.removePrefix("\uFEFF")
            .trim()
            .removeSurrounding("\"")
            .removeSurrounding("'")
            .trim()
            .filterNot { it.code in 0..8 || it.code in 14..31 || it.code == 127 || it == '\uFFFD' }
    }

    fun parseDateOrOffset(input: String): Long {
        if (input.isBlank()) return 0L
        // Can be either "+N" days offset or day timestamp or "yyyy-MM-dd" / "dd.MM.yyyy" / "dd/MM/yyyy"
        input.toIntOrNull()?.let { daysOffset ->
            return System.currentTimeMillis() + daysOffset * 24L * 60 * 60 * 1000
        }
        return try {
            if (input.contains(".")) {
                val p = input.split(".")
                if (p.size >= 3) {
                    val cal = Calendar.getInstance()
                    val rawYear = p[2].toInt()
                    val year = if (rawYear < 100) 2000 + rawYear else rawYear
                    cal.set(year, p[1].toInt() - 1, p[0].toInt(), 0, 0, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                } else 0L
            } else if (input.contains("-")) {
                val p = input.split("-")
                if (p.size >= 3) {
                    val cal = Calendar.getInstance()
                    val rawYear = p[0].toInt()
                    val year = if (rawYear < 100) 2000 + rawYear else rawYear
                    cal.set(year, p[1].toInt() - 1, p[2].toInt(), 0, 0, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                } else 0L
            } else if (input.contains("/")) {
                val p = input.split("/")
                if (p.size >= 3) {
                    val cal = Calendar.getInstance()
                    val rawYear = p[2].toInt()
                    val year = if (rawYear < 100) 2000 + rawYear else rawYear
                    cal.set(year, p[1].toInt() - 1, p[0].toInt(), 0, 0, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                } else 0L
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    fun parseLinesWithStats(lines: List<String>, existingKeys: Set<String>): CsvParseResult {
        val batchKeys = mutableSetOf<String>()
        val productsToInsert = mutableListOf<Product>()
        var skippedCount = 0
        var duplicateCount = 0

        for (rawLine in lines) {
            val line = rawLine.trim()
            val lowerLine = line.lowercase(java.util.Locale.forLanguageTag("tr-TR"))
            if (line.isEmpty() || line.startsWith("#")) continue
            if (lowerLine.contains("barkod") ||
                lowerLine.contains("ürün adı") || lowerLine.contains("urun adi") ||
                lowerLine.contains("ürün kodu") || lowerLine.contains("urun kodu")
            ) {
                // Header row - automatically skip
                continue
            }
            if (isGarbageText(line)) {
                skippedCount++
                continue
            }

            val delimiter = if (line.contains(";")) ";" else ","
            val parts = line.split(delimiter).map { it.trim().removeSurrounding("\"").trim() }
            if (parts.size < 2) {
                skippedCount++
                continue
            }

            // Standard 5-Column Format: Barkod;ÜrünKodu;ÜrünAdı;Fiyat;Kategori
            val resolvedProduct = if (parts.size >= 5) {
                val rawBarkod = parts[0]
                val urunKodu = parts[1]
                val urunAdi = parts[2]

                // Price parsing: clean "TL", "₺", change comma to dot
                val rawPrice = parts[3].replace("₺", "").replace("TL", "", ignoreCase = true).replace(',', '.').trim()
                val parsedPrice = rawPrice.toDoubleOrNull()?.takeIf { it >= 0.0 }
                val parsedCategory = parts[4].ifBlank { "Genel" }

                val rawProd = Product(
                    barkod = rawBarkod.ifEmpty { urunKodu },
                    urunKodu = urunKodu.ifEmpty { "0000" },
                    urunAdi = urunAdi.uppercase(java.util.Locale.forLanguageTag("tr-TR")),
                    kategori = parsedCategory,
                    sktTarihi = 0L,
                    stokAdedi = 0,
                    fiyat = parsedPrice
                )
                ProductDataHealer.autoHealProduct(rawProd)
            } else {
                // Try smart parsing (immune to shifted columns and extra reyon IDs)
                val smartProduct = ProductDataHealer.smartParseProductRow(parts)
                if (smartProduct != null) {
                    smartProduct
                } else if (parts.size >= 3) {
                    val rawBarkod = parts[0]
                    val urunKodu = parts[1]
                    val urunAdi = parts[2]
                    val rawPrice = if (parts.size >= 4) {
                        parts[3].replace("₺", "").replace("TL", "", ignoreCase = true).replace(',', '.').trim()
                    } else ""
                    val parsedPrice = rawPrice.toDoubleOrNull()?.takeIf { it >= 0.0 }
                    val parsedCategory = if (parts.size >= 4 && parsedPrice == null) parts[3].ifBlank { "Genel" } else "Genel"

                    val rawProd = Product(
                        barkod = rawBarkod.ifEmpty { urunKodu },
                        urunKodu = urunKodu.ifEmpty { "0000" },
                        urunAdi = urunAdi.uppercase(java.util.Locale.forLanguageTag("tr-TR")),
                        kategori = parsedCategory,
                        sktTarihi = 0L,
                        stokAdedi = 0,
                        fiyat = parsedPrice
                    )
                    ProductDataHealer.autoHealProduct(rawProd)
                } else null
            }

            if (resolvedProduct != null &&
                resolvedProduct.barkod.isNotEmpty() &&
                resolvedProduct.urunAdi.isNotEmpty() &&
                !isGarbageText(resolvedProduct.barkod) &&
                !isGarbageText(resolvedProduct.urunAdi)
            ) {
                val itemKey = "${resolvedProduct.barkod.trim().lowercase()}_${resolvedProduct.urunKodu.trim().lowercase()}_${resolvedProduct.urunAdi.trim().lowercase()}"
                if (!existingKeys.contains(itemKey) && !batchKeys.contains(itemKey)) {
                    batchKeys.add(itemKey)
                    productsToInsert.add(resolvedProduct)
                } else {
                    duplicateCount++
                }
            } else {
                skippedCount++
            }
        }
        return CsvParseResult(
            productsToInsert = productsToInsert,
            skippedLineCount = skippedCount,
            duplicateCount = duplicateCount
        )
    }

    fun parseLinesToProducts(lines: List<String>, existingKeys: Set<String>): List<Product> {
        return parseLinesWithStats(lines, existingKeys).productsToInsert
    }
}
