package com.example.util

import com.example.data.Product
import java.util.Calendar
import java.util.Locale

/**
 * Intelligent Data Disambiguation & Self-Healing Engine for Retail Products (A101 / BİM / General Retail).
 *
 * Automatically detects and fixes column shifts, field swaps, category anomalies, and data corruption
 * during Excel/CSV imports, QR scanning, or database loading.
 */
object ProductDataHealer {

    private val trLocale = Locale.forLanguageTag("tr-TR")

    // Common standard Turkish retail barcode prefixes
    private val standardBarcodePrefixes = listOf("869", "868", "978", "979", "762", "590", "400", "0")

    /**
     * Checks if a string represents an EAN-13, EAN-8, UPC, or standard package barcode.
     */
    fun isEanOrBarcode(raw: String): Boolean {
        val s = raw.trim()
        if (s.isEmpty()) return false
        val digitsOnly = s.filter { it.isDigit() }
        
        // 13-digit EAN (very common in Turkish retail starting with 869/868)
        if (digitsOnly.length == 13) return true
        // 8, 12, 14 digit barcodes
        if (digitsOnly.length == 8 && standardBarcodePrefixes.any { s.startsWith(it) }) return true
        if (digitsOnly.length in 12..14 && digitsOnly == s) return true
        
        // Standard numeric barcode with length >= 8
        if (s.length in 8..14 && s.all { it.isDigit() } && standardBarcodePrefixes.any { s.startsWith(it) }) {
            return true
        }
        return false
    }

    /**
     * Specifically checks for 13-digit EAN barcode (e.g. 8690158124264).
     */
    fun is13DigitEan(raw: String): Boolean {
        val s = raw.trim()
        return s.length == 13 && s.all { it.isDigit() }
    }

    /**
     * Checks if a string is a retail product code (Malzeme / Ürün Kodu, typically 4 to 8 digits, e.g. 12003631, 25001234).
     */
    fun isProductCode(raw: String): Boolean {
        val s = raw.trim()
        if (s.isEmpty()) return false
        if (is13DigitEan(s)) return false
        // 4 to 9 digit pure number
        if (s.all { it.isDigit() } && s.length in 4..9) return true
        // Alphanumeric short code e.g. "PRD-1024"
        if (s.length in 4..10 && s.any { it.isDigit() } && !s.contains(" ") && !isProductName(s)) return true
        return false
    }

    /**
     * Checks if a string is a reyon/group/row number (e.g. "53", "1", "12").
     */
    fun isReyonOrRowNumber(raw: String): Boolean {
        val s = raw.trim()
        if (s.isEmpty()) return false
        return s.all { it.isDigit() } && s.length in 1..3
    }

    /**
     * Checks if a string is a descriptive Product Name (contains letters, words, retail units, brands).
     */
    fun isProductName(raw: String): Boolean {
        val s = raw.trim()
        if (s.isEmpty()) return false
        val letterCount = s.count { it.isLetter() }
        if (letterCount < 3) return false

        // Exclude generic broad category names when standalone
        val lower = s.lowercase(trLocale)
        if (lower == "dolap ürünleri" || lower == "gıda ürünleri" || lower == "genel" || lower == "temizlik" || lower == "manav") {
            return false
        }
        return true
    }

    /**
     * Automatically classifies the product into 'Dolap Ürünleri' vs 'Gıda Ürünleri' based on product name and keywords.
     */
    fun detectCategory(productName: String, fallbackCategory: String = "Gıda Ürünleri"): String {
        val lower = "${productName.lowercase(trLocale)} ${fallbackCategory.lowercase(trLocale)}"
        
        val isDolap = lower.contains("peynir") || lower.contains("süt") || lower.contains("sut") ||
                lower.contains("yoğurt") || lower.contains("yogurt") || lower.contains("tereyağ") ||
                lower.contains("tereyag") || lower.contains("kaymak") || lower.contains("şarküteri") ||
                lower.contains("sarkuteri") || lower.contains("salam") || lower.contains("sosis") ||
                lower.contains("sucuk") || lower.contains("pastırma") || lower.contains("pastirma") ||
                lower.contains("kavurma") || lower.contains("tavuk") || lower.contains("piliç") ||
                lower.contains("pilic") || lower.contains("et ") || lower.contains("kıyma") ||
                lower.contains("kiyma") || lower.contains("dondurma") || lower.contains("krema") ||
                lower.contains("ayran") || lower.contains("kefir") || lower.contains("lor") ||
                lower.contains("kaşar") || lower.contains("kasar") || lower.contains("labne") ||
                lower.contains("tulum") || lower.contains("çökelek") || lower.contains("süzme") ||
                lower.contains("soğuk") || lower.contains("dolap") || lower.contains("yumurta") ||
                lower.contains("helva") || lower.contains("meze")

        return if (isDolap) "Dolap Ürünleri" else "Gıda Ürünleri"
    }

    /**
     * Inspects a single Product entity and returns a healed, corrected version if any column shift or swap is detected.
     */
    fun autoHealProduct(product: Product): Product {
        val rawBarkod = product.barkod.trim()
        val rawUrunKodu = product.urunKodu.trim()
        val rawUrunAdi = product.urunAdi.trim()
        val rawKategori = product.kategori.trim()

        var healedBarkod = rawBarkod
        var healedUrunKodu = rawUrunKodu
        var healedUrunAdi = rawUrunAdi
        var healedKategori = rawKategori

        // Case 1: Standard 1-Step Right Shift (Reyon in Barkod, 13-digit EAN in UrunKodu, 8-digit Code in UrunAdi, Name in Kategori)
        // Example: Barkod="53", UrunKodu="8690158124264", UrunAdi="12003631", Kategori="BEYAZ PEYNİR 900 G TEKSÜT"
        if (isReyonOrRowNumber(rawBarkod) && is13DigitEan(rawUrunKodu) && (isProductCode(rawUrunAdi) || rawUrunAdi.all { it.isDigit() }) && isProductName(rawKategori)) {
            healedBarkod = rawUrunKodu
            healedUrunKodu = rawUrunAdi
            healedUrunAdi = rawKategori.uppercase(trLocale)
            healedKategori = detectCategory(healedUrunAdi, "Dolap Ürünleri")
            return product.copy(
                barkod = healedBarkod,
                urunKodu = healedUrunKodu,
                urunAdi = healedUrunAdi,
                kategori = healedKategori
            )
        }

        // Case 2: 13-digit EAN is in UrunKodu and Barkod has short code or reyon number, and Name is in UrunAdi
        if (is13DigitEan(rawUrunKodu) && (isReyonOrRowNumber(rawBarkod) || isProductCode(rawBarkod))) {
            healedBarkod = rawUrunKodu
            healedUrunKodu = if (isProductCode(rawBarkod)) rawBarkod else (if (isProductCode(rawUrunAdi)) rawUrunAdi else "25001000")
            if (isProductName(rawKategori) && !isProductName(rawUrunAdi)) {
                healedUrunAdi = rawKategori.uppercase(trLocale)
            } else if (isProductName(rawUrunAdi)) {
                healedUrunAdi = rawUrunAdi.uppercase(trLocale)
            }
            healedKategori = detectCategory(healedUrunAdi, rawKategori)
            return product.copy(
                barkod = healedBarkod,
                urunKodu = healedUrunKodu,
                urunAdi = healedUrunAdi,
                kategori = healedKategori
            )
        }

        // Case 3: UrunAdi is numeric code, and UrunKodu has the descriptive text name (Swapped Name & Code)
        if ((rawUrunAdi.all { it.isDigit() } || isProductCode(rawUrunAdi)) && isProductName(rawUrunKodu)) {
            healedUrunAdi = rawUrunKodu.uppercase(trLocale)
            healedUrunKodu = rawUrunAdi
            healedKategori = detectCategory(healedUrunAdi, rawKategori)
            return product.copy(
                barkod = healedBarkod,
                urunKodu = healedUrunKodu,
                urunAdi = healedUrunAdi,
                kategori = healedKategori
            )
        }

        // Case 4: Barkod and UrunKodu are swapped (13-digit EAN in UrunKodu, 6-8 digit code in Barkod)
        if (is13DigitEan(rawUrunKodu) && isProductCode(rawBarkod) && isProductName(rawUrunAdi)) {
            healedBarkod = rawUrunKodu
            healedUrunKodu = rawBarkod
            healedUrunAdi = rawUrunAdi.uppercase(trLocale)
            healedKategori = detectCategory(healedUrunAdi, rawKategori)
            return product.copy(
                barkod = healedBarkod,
                urunKodu = healedUrunKodu,
                urunAdi = healedUrunAdi,
                kategori = healedKategori
            )
        }

        // Case 5: Barkod contains full name and UrunAdi contains 13-digit EAN
        if (isProductName(rawBarkod) && is13DigitEan(rawUrunAdi)) {
            healedBarkod = rawUrunAdi
            healedUrunAdi = rawBarkod.uppercase(trLocale)
            healedUrunKodu = if (isProductCode(rawUrunKodu)) rawUrunKodu else "25001000"
            healedKategori = detectCategory(healedUrunAdi, rawKategori)
            return product.copy(
                barkod = healedBarkod,
                urunKodu = healedUrunKodu,
                urunAdi = healedUrunAdi,
                kategori = healedKategori
            )
        }

        // Case 6: Category field contains the product name and UrunAdi is a numeric code
        if (rawUrunAdi.all { it.isDigit() } && isProductName(rawKategori)) {
            healedUrunAdi = rawKategori.uppercase(trLocale)
            healedUrunKodu = rawUrunAdi
            healedKategori = detectCategory(healedUrunAdi, "Dolap Ürünleri")
            return product.copy(
                barkod = healedBarkod,
                urunKodu = healedUrunKodu,
                urunAdi = healedUrunAdi,
                kategori = healedKategori
            )
        }

        // If category is a raw number (e.g. "53"), replace with real category
        if (rawKategori.all { it.isDigit() } || rawKategori.isBlank()) {
            healedKategori = detectCategory(healedUrunAdi, "Gıda Ürünleri")
            return product.copy(
                kategori = healedKategori,
                urunAdi = healedUrunAdi.uppercase(trLocale)
            )
        }

        return product
    }

    /**
     * Intelligently parses an imported raw line (CSV, XLSX, TSV) by inspecting cell contents and types,
     * immune to column shifting or extra reyon/sequence columns.
     */
    fun smartParseProductRow(rawTokens: List<String>): Product? {
        val tokens = rawTokens.map { token ->
            token.removePrefix("\uFEFF")
                .trim()
                .removeSurrounding("\"")
                .removeSurrounding("'")
                .trim()
                .filterNot { it.code in 0..8 || it.code in 14..31 || it.code == 127 || it == '\uFFFD' }
        }.filter { it.isNotEmpty() }

        if (tokens.size < 2) return null

        // Check if row is header
        val headerKeywords = listOf("barkod", "barcode", "ürün kodu", "urun kodu", "ürün adı", "urun adi", "malzeme", "reyon", "kategori", "fiyat", "skt")
        if (tokens.any { t -> headerKeywords.any { kw -> t.lowercase(trLocale) == kw } }) {
            return null
        }

        var foundBarcode: String? = null
        var foundProductCode: String? = null
        var foundProductName: String? = null
        var foundCategory: String? = null
        var foundPrice: Double? = null
        var foundSkt: Long = 0L
        var foundStock: Int = 0

        val remainingTokens = mutableListOf<String>()

        for (token in tokens) {
            val lower = token.lowercase(trLocale)

            // 1. Check Date (SKT)
            if (foundSkt == 0L && (token.contains(".") || token.contains("-") || token.contains("/"))) {
                val parsedDate = parseDateString(token)
                if (parsedDate > 0L) {
                    foundSkt = parsedDate
                    continue
                }
            }

            // 2. Check Price (e.g. "129.90", "129,90", "129.90 TL")
            if (foundPrice == null && (lower.contains("tl") || lower.contains("₺") || ((token.contains(".") || token.contains(",")) && token.replace(",", ".").replace("tl", "").trim().toDoubleOrNull() != null))) {
                val cleanPriceStr = token.replace("tl", "", ignoreCase = true).replace("₺", "").replace(",", ".").trim()
                cleanPriceStr.toDoubleOrNull()?.let { pVal ->
                    if (pVal > 0.0 && pVal < 100000.0) {
                        foundPrice = pVal
                        continue
                    }
                }
            }

            // 3. 13-digit EAN Barcode
            if (foundBarcode == null && is13DigitEan(token)) {
                foundBarcode = token
                continue
            }

            remainingTokens.add(token)
        }

        // Search for Product Name in remaining tokens (most letters / longest descriptive text)
        val nameCandidate = remainingTokens.filter { isProductName(it) }
            .maxByOrNull { it.count { c -> c.isLetter() } }

        if (nameCandidate != null) {
            foundProductName = nameCandidate
            remainingTokens.remove(nameCandidate)
        }

        // If barcode not found yet, check remaining tokens for barcode
        if (foundBarcode == null) {
            val barCandidate = remainingTokens.firstOrNull { isEanOrBarcode(it) }
            if (barCandidate != null) {
                foundBarcode = barCandidate
                remainingTokens.remove(barCandidate)
            }
        }

        // Search for Product Code in remaining tokens
        val codeCandidate = remainingTokens.firstOrNull { isProductCode(it) || (it.all { c -> c.isDigit() } && it.length in 4..9) }
        if (codeCandidate != null) {
            foundProductCode = codeCandidate
            remainingTokens.remove(codeCandidate)
        }

        // Search for Category or Stock in remaining tokens
        for (token in remainingTokens) {
            if (foundStock == 0 && token.all { it.isDigit() } && token.length <= 4) {
                foundStock = token.toIntOrNull() ?: 0
            } else if (foundCategory == null && !token.all { it.isDigit() }) {
                foundCategory = token
            }
        }

        // Fallbacks
        val finalProductName = (foundProductName ?: "İSİMSİZ ÜRÜN").uppercase(trLocale)
        val finalBarcode = foundBarcode ?: (foundProductCode ?: "869${(1000000000..9999999999).random()}")
        val finalProductCode = foundProductCode ?: (if (foundBarcode != null && foundBarcode.length <= 9) foundBarcode else "2500${(1000..9999).random()}")
        val finalCategory = detectCategory(finalProductName, foundCategory ?: "Gıda Ürünleri")

        val rawProduct = Product(
            barkod = finalBarcode,
            urunKodu = finalProductCode,
            urunAdi = finalProductName,
            kategori = finalCategory,
            sktTarihi = foundSkt,
            stokAdedi = if (foundSkt > 0L && foundStock == 0) 1 else foundStock,
            fiyat = foundPrice
        )

        return autoHealProduct(rawProduct)
    }

    private fun parseDateString(input: String): Long {
        if (input.isBlank()) return 0L
        return try {
            if (input.contains(".")) {
                val p = input.split(".")
                if (p.size >= 3) {
                    val cal = Calendar.getInstance()
                    val year = p[2].toInt().let { if (it < 100) 2000 + it else it }
                    cal.set(year, p[1].toInt() - 1, p[0].toInt(), 0, 0, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                } else 0L
            } else if (input.contains("-")) {
                val p = input.split("-")
                if (p.size >= 3) {
                    val cal = Calendar.getInstance()
                    val year = p[0].toInt().let { if (it < 100) 2000 + it else it }
                    cal.set(year, p[1].toInt() - 1, p[2].toInt(), 0, 0, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                } else 0L
            } else if (input.contains("/")) {
                val p = input.split("/")
                if (p.size >= 3) {
                    val cal = Calendar.getInstance()
                    val year = p[2].toInt().let { if (it < 100) 2000 + it else it }
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
}
