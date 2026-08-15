package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Calendar
import java.util.Locale

fun getTodayMidnightMillis(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

fun String.normalizeForSearch(): String {
    if (this.isBlank()) return ""

    var normalized = this.lowercase(Locale.forLanguageTag("tr-TR"))
        .replace('ı', 'i')
        .replace('ö', 'o')
        .replace('ü', 'u')
        .replace('ç', 'c')
        .replace('ş', 's')
        .replace('ğ', 'g')

    if (normalized.any { it.isDigit() || it == 'l' || it == 'g' || it == 'k' || it == 'm' }) {
        normalized = normalized
            .replace("1 litre", "1l 1lt 1litre 1000ml")
            .replace("1 lt", "1l 1lt 1000ml")
            .replace("1.5 litre", "1.5l 1.5lt 1.5litre 1500ml")
            .replace("2.5 litre", "2.5l 2.5lt 2.5litre 2500ml")
            .replace("2 litre", "2l 2lt 2litre 2000ml")
            .replace("500 ml", "500ml 0.5l")
            .replace("330 ml", "330ml 0.33l")
            .replace("250 ml", "250ml 0.25l")
            .replace("200 ml", "200ml 0.2l")
            .replace("1 kg", "1kg 1000g 1000gr")
            .replace("500 gr", "500gr 500g")
            .replace("250 gr", "250gr 250g")
    }

    return normalized
}

fun Product.matchesSearchQuery(rawQuery: String, queryTokens: List<String> = emptyList()): Boolean {
    val q = rawQuery.trim()
    if (q.isEmpty()) return true

    // Direct substring match in name, barcode, product code or category
    if (this.urunAdi.contains(q, ignoreCase = true) ||
        this.barkod.contains(q, ignoreCase = true) ||
        this.urunKodu.contains(q, ignoreCase = true) ||
        this.kategori.contains(q, ignoreCase = true)) {
        return true
    }

    val qNorm = q.normalizeForSearch()
    val targetText = "${this.urunAdi} ${this.barkod} ${this.urunKodu} ${this.kategori}".normalizeForSearch()

    if (qNorm.isNotEmpty() && targetText.contains(qNorm)) {
        return true
    }

    val tokens = if (queryTokens.isNotEmpty()) queryTokens else {
        qNorm.replace(',', '.').split("\\s+".toRegex()).filter { it.isNotBlank() }
    }

    if (tokens.isEmpty()) return true

    return tokens.all { token ->
        targetText.contains(token) ||
        (token.contains('.') && targetText.contains(token.replace('.', ','))) ||
        (token.contains(',') && targetText.contains(token.replace(',', '.')))
    }
}

@Entity(
    tableName = "products",
    indices = [Index(value = ["barkod"], unique = false)]
)
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val barkod: String,
    val urunKodu: String,
    val urunAdi: String,
    val kategori: String,
    val sktTarihi: Long, // Timestamp in milliseconds
    val stokAdedi: Int,
    val eklenmeTarihi: Long = System.currentTimeMillis(),
    val isImportant: Boolean = false,
    val sonKontrolTarihi: Long = 0L,
    val fiyat: Double? = null
) {
    fun getFormattedPrice(): String? {
        val f = fiyat ?: return null
        if (f <= 0.0) return null
        return "₺${String.format(Locale.forLanguageTag("tr-TR"), "%.2f", f)}"
    }
    fun getFormattedSkt(): String {
        if (sktTarihi <= 0L) return "Belirtilmedi"
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr-TR"))
        return sdf.format(java.util.Date(sktTarihi))
    }
    fun getRemainingDays(todayMidnight: Long = getTodayMidnightMillis()): Long {
        if (sktTarihi <= 0L) return 9999L
        val diffMillis = sktTarihi - todayMidnight
        return diffMillis / (1000L * 60 * 60 * 24)
    }

    fun getExpiryStatus(todayMidnight: Long = getTodayMidnightMillis()): ExpiryStatus {
        if (sktTarihi <= 0L) return ExpiryStatus.NORMAL
        val days = getRemainingDays(todayMidnight)
        return when {
            days <= 0 -> ExpiryStatus.EXPIRED
            days <= 7 -> ExpiryStatus.CRITICAL
            days <= 15 -> ExpiryStatus.SOON
            days <= 30 -> ExpiryStatus.WARNING
            else -> ExpiryStatus.NORMAL
        }
    }
}

fun Product.getDisplayName(): String {
    val nameTrim = urunAdi.trim()
    val codeTrim = urunKodu.trim()
    if (nameTrim.isNotBlank() && nameTrim.all { it.isDigit() || it == '-' || it == '_' || it == ' ' } && codeTrim.any { it.isLetter() }) {
        return codeTrim
    }
    if (nameTrim.isBlank() && codeTrim.isNotBlank()) {
        return codeTrim
    }
    return if (nameTrim.isNotBlank()) nameTrim else "İSİMSİZ ÜRÜN"
}

fun Product.getDisplayCode(): String {
    val nameTrim = urunAdi.trim()
    val codeTrim = urunKodu.trim()
    if (nameTrim.isNotBlank() && nameTrim.all { it.isDigit() || it == '-' || it == '_' || it == ' ' } && codeTrim.any { it.isLetter() }) {
        return nameTrim
    }
    return codeTrim
}

fun Product.isDolapProduct(): Boolean {
    val cat = this.kategori.lowercase(Locale.forLanguageTag("tr-TR"))
    return cat.contains("dolap") || cat.contains("süt") || cat.contains("sut") ||
           cat.contains("şarküteri") || cat.contains("sarkuteri") || cat.contains("soğuk") ||
           cat.contains("soguk") || cat.contains("peynir") || cat.contains("yoğurt") ||
           cat.contains("yogurt") || cat.contains("dondurma") || cat.contains("et") ||
           cat.contains("tavuk") || cat.contains("tatlı") || cat.contains("tatli")
}

enum class ExpiryStatus(val label: String) {
    EXPIRED("Süresi Geçen"),
    CRITICAL("Kritik"),
    SOON("Yakın (8-15 Gün)"),
    WARNING("Orta Vadeli (16-30 Gün)"),
    NORMAL("Normal (>30 Gün)")
}

data class ShelfQrData(
    val storeCode: String? = null,
    val barcode: String,
    val price: Double? = null,
    val productCode: String? = null,
    val productName: String? = null,
    val isShelfQr: Boolean = false
)

fun parseShelfQrPayload(rawInput: String): ShelfQrData {
    val trimmed = rawInput.trim().removePrefix("\uFEFF").filterNot { it.code in 0..8 || it.code in 14..31 }
    if (trimmed.isBlank()) {
        return ShelfQrData(barcode = "")
    }

    // 0. JSON Formats (e.g. {"barcode":"8690504005100","code":"16000491","price":28.0,"name":"DOST SUT"})
    if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
        try {
            var b = ""
            var c: String? = null
            var n: String? = null
            var p: Double? = null

            val jsonClean = trimmed.removeSurrounding("{", "}").trim()
            val entries = jsonClean.split(Regex(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"))
            for (entry in entries) {
                val kv = entry.split(Regex(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"), limit = 2)
                if (kv.size == 2) {
                    val key = kv[0].replace("\"", "").trim().lowercase(Locale.ROOT)
                    val value = kv[1].replace("\"", "").trim()
                    if (key == "barkod" || key == "barcode" || key == "bar" || key == "b" || key == "ean" || key == "gtin") {
                        b = value.filter { it.isDigit() }
                    } else if (key == "urunkodu" || key == "urun_kodu" || key == "kod" || key == "code" || key == "k" || key == "itemcode") {
                        c = value.trim()
                    } else if (key == "urunadi" || key == "urun_adi" || key == "adi" || key == "name" || key == "n" || key == "title") {
                        n = value.trim()
                    } else if (key == "fiyat" || key == "price" || key == "p" || key == "tutar") {
                        value.replace(',', '.').replace("₺", "").replace("TL", "").trim().toDoubleOrNull()?.let { p = it }
                    }
                }
            }
            if (b.isNotBlank() || c != null || n != null || p != null) {
                return ShelfQrData(
                    barcode = b.ifBlank { c ?: "" },
                    productCode = c,
                    productName = n,
                    price = p,
                    isShelfQr = true
                )
            }
        } catch (e: Exception) {
            // Ignore JSON parsing fallback
        }
    }

    // 1. GS1 DataMatrix / GS1 QR format (e.g. (01)08690504005100(17)260815(10)12345 or ]C10869...)
    if (trimmed.contains("(01)") || trimmed.startsWith("010869") || trimmed.startsWith("]C10869") || trimmed.startsWith("]d20869")) {
        val gtinMatch = Regex("(?:\\(01\\)|01|^](?:C1|d2)01)?([0-9]{13,14})").find(trimmed)
        if (gtinMatch != null) {
            val rawGtin = gtinMatch.groupValues[1]
            val barcodeCandidate = if (rawGtin.length == 14 && rawGtin.startsWith("0")) rawGtin.substring(1) else rawGtin
            return ShelfQrData(
                barcode = barcodeCandidate,
                isShelfQr = true
            )
        }
    }

    // 2. URL Formats (e.g. https://.../product?barcode=8690504005100&kod=16000491 or https://.../8690504005100)
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("www.")) {
        var b = ""
        var c: String? = null
        val uriQueryMatch = Regex("[?&](?:barcode|barkod|bar|b|ean)=([0-9]+)").find(trimmed)
        val uriCodeMatch = Regex("[?&](?:kod|code|urunkodu|k)=([a-zA-Z0-9_-]+)").find(trimmed)
        if (uriQueryMatch != null) b = uriQueryMatch.groupValues[1]
        if (uriCodeMatch != null) c = uriCodeMatch.groupValues[1]

        if (b.isEmpty()) {
            val lastPathSegment = trimmed.substringAfterLast("/").substringBefore("?").filter { it.isDigit() }
            if (lastPathSegment.length in 8..14) {
                b = lastPathSegment
            }
        }
        if (b.isNotEmpty() || c != null) {
            return ShelfQrData(
                barcode = b,
                productCode = c,
                isShelfQr = true
            )
        }
    }

    // 3. Key-Value Formats (BARKOD:..., KOD:..., ADI:..., FIYAT:...)
    if (trimmed.contains(":") || (trimmed.contains("=") && !trimmed.startsWith("http"))) {
        var b = ""
        var c: String? = null
        var n: String? = null
        var p: Double? = null
        val pairs = trimmed.split(Regex("[;\n\r,|]")).map { it.trim() }
        for (pair in pairs) {
            val kv = pair.split(Regex("[:=]"), limit = 2).map { it.trim() }
            if (kv.size == 2) {
                val key = kv[0].lowercase(Locale.ROOT)
                val value = kv[1]
                if (key.contains("barkod") || key.contains("bar") || key == "b" || key == "ean") {
                    b = value.filter { it.isDigit() }
                } else if (key.contains("kod") || key == "k" || key.contains("code")) {
                    c = value.trim()
                } else if (key.contains("adi") || key.contains("isim") || key.contains("name") || key == "a") {
                    n = value.trim()
                } else if (key.contains("fiyat") || key.contains("price") || key == "f" || key.contains("tl") || key.contains("₺")) {
                    val pStr = value.replace("₺", "").replace("TL", "").replace("tl", "").replace(',', '.').trim()
                    pStr.toDoubleOrNull()?.let { p = it }
                }
            }
        }
        if (b.isNotBlank() || c != null || n != null || p != null) {
            return ShelfQrData(
                barcode = b.ifBlank { c ?: "" },
                productCode = c,
                productName = n,
                price = p,
                isShelfQr = true
            )
        }
    }

    // 4. Delimited formats (Pipe, Semicolon, Tab, Comma, Newline)
    val delimiters = listOf("|", ";", "\t", ",")
    for (delim in delimiters) {
        if (trimmed.contains(delim)) {
            val parts = trimmed.split(delim).map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size >= 2) {
                var b = ""
                var c: String? = null
                var n: String? = null
                var p: Double? = null
                for (part in parts) {
                    val partDigits = part.filter { it.isDigit() }
                    if (partDigits.length in 12..14 && b.isEmpty()) {
                        b = partDigits
                    } else if (partDigits.length in 4..11 && c == null && !part.contains(",") && !part.contains(".")) {
                        c = part
                    } else if (part.replace('₺', ' ').replace("TL", "").replace("tl", "").replace(',', '.').trim().toDoubleOrNull() != null && p == null) {
                        p = part.replace('₺', ' ').replace("TL", "").replace("tl", "").replace(',', '.').trim().toDoubleOrNull()
                    } else if (part.length >= 2 && n == null && !part.startsWith("http")) {
                        n = part
                    }
                }
                if (b.isNotEmpty() || c != null || n != null || p != null) {
                    return ShelfQrData(
                        barcode = b.ifBlank { c ?: "" },
                        productCode = c,
                        productName = n,
                        price = p,
                        isShelfQr = true
                    )
                }
            }
        }
    }

    // 5. Hyphenated Formats (e.g. D724-8690504005100-28,00-16000491 or 16000491-8690504005100-28,00)
    if (trimmed.contains("-")) {
        val parts = trimmed.split("-").map { it.trim() }

        // Format 1: StoreCode - Barcode - Price - ProductCode [- ProductName]
        if (parts.size >= 4) {
            val storeCode = parts[0]
            val barcodeCandidate = parts[1]
            val priceCandidate = parts[2].replace(",", ".")
            val productCodeCandidate = parts[3]
            val nameCandidate = if (parts.size >= 5) parts.subList(4, parts.size).joinToString("-") else null

            val parsedPrice = priceCandidate.toDoubleOrNull()
            if (parsedPrice != null && barcodeCandidate.isNotEmpty()) {
                return ShelfQrData(
                    storeCode = storeCode,
                    barcode = barcodeCandidate,
                    price = parsedPrice,
                    productCode = if (productCodeCandidate.isNotEmpty()) productCodeCandidate else null,
                    productName = nameCandidate,
                    isShelfQr = true
                )
            }
        }

        // Format 2: Barcode - Price - ProductCode OR ProductCode - Barcode - Price
        if (parts.size == 3) {
            val p0Digits = parts[0].filter { it.isDigit() }
            val p1Digits = parts[1].filter { it.isDigit() }
            val p2Digits = parts[2].filter { it.isDigit() }

            val parsedP1Price = parts[1].replace(",", ".").toDoubleOrNull()
            val parsedP2Price = parts[2].replace(",", ".").toDoubleOrNull()

            if (p0Digits.length in 12..14 && parsedP1Price != null) {
                return ShelfQrData(
                    barcode = p0Digits,
                    price = parsedP1Price,
                    productCode = parts[2].ifBlank { null },
                    isShelfQr = true
                )
            } else if (p1Digits.length in 12..14 && parsedP2Price != null) {
                return ShelfQrData(
                    barcode = p1Digits,
                    price = parsedP2Price,
                    productCode = parts[0].ifBlank { null },
                    isShelfQr = true
                )
            }
        }

        // Format 3: Hyphenated label (e.g. 16000491-8690504005100 or D724-8690504005100)
        if (parts.size >= 2) {
            val longDigitPart = parts.firstOrNull { it.filter { c -> c.isDigit() }.length in 12..14 }
            val shortDigitPart = parts.firstOrNull { it != longDigitPart && it.filter { c -> c.isDigit() }.length in 4..11 }
            if (longDigitPart != null) {
                return ShelfQrData(
                    barcode = longDigitPart.filter { it.isDigit() },
                    price = null,
                    productCode = shortDigitPart,
                    isShelfQr = true
                )
            }
        }
    }

    // 6. Space-separated format
    val tokens = trimmed.split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (tokens.size >= 3) {
        var b = ""
        var c: String? = null
        var p: Double? = null
        val nameWords = mutableListOf<String>()
        for (tok in tokens) {
            val tokDigits = tok.filter { it.isDigit() }
            if (tokDigits.length in 12..14 && b.isEmpty()) {
                b = tokDigits
            } else if (tokDigits.length in 4..11 && c == null) {
                c = tok
            } else if (tok.replace(',', '.').toDoubleOrNull() != null && p == null && tokDigits.length !in 5..14) {
                p = tok.replace(',', '.').toDoubleOrNull()
            } else if (!tok.equals("TL", ignoreCase = true) && tok != "₺") {
                nameWords.add(tok)
            }
        }
        if (b.isNotEmpty() || c != null || p != null) {
            return ShelfQrData(
                barcode = b.ifBlank { c ?: "" },
                productCode = c,
                productName = if (nameWords.isNotEmpty()) nameWords.joinToString(" ") else null,
                price = p,
                isShelfQr = true
            )
        }
    }

    // 7. Plain single barcode or product code string
    val digitsOnly = trimmed.filter { it.isDigit() }
    return ShelfQrData(
        barcode = if (digitsOnly.length in 8..14) digitsOnly else trimmed,
        productCode = if (digitsOnly.length in 4..11) digitsOnly else trimmed,
        isShelfQr = false
    )
}

/**
 * Universal high-performance product matching engine for 1D barcodes and QR codes.
 * Matches exact barcodes, product codes, normalized digits, leading zeros, and product titles.
 */
fun List<Product>.findMatchingProducts(rawQuery: String): List<Product> {
    val raw = rawQuery.trim().removePrefix("\uFEFF").filterNot { it.code in 0..8 || it.code in 14..31 }
    if (raw.isBlank()) return emptyList()

    val qrData = parseShelfQrPayload(raw)
    val queryBarcode = qrData.barcode.trim()
    val queryProductCode = qrData.productCode?.trim()
    val queryDigits = queryBarcode.filter { it.isDigit() }
    val queryNoLeadingZeros = queryDigits.trimStart('0')
    val rawDigits = raw.filter { it.isDigit() }
    val rawNoLeadingZeros = rawDigits.trimStart('0')

    // 1. EXACT BARCODE MATCH (Highest Priority)
    val exactBarcodeMatches = this.filter { p ->
        val pBarcode = p.barkod.trim()
        val pBarcodeDigits = pBarcode.filter { it.isDigit() }
        val pBarcodeNoZeros = pBarcodeDigits.trimStart('0')

        pBarcode.equals(queryBarcode, ignoreCase = true) ||
        pBarcode.equals(raw, ignoreCase = true) ||
        (queryDigits.isNotEmpty() && pBarcodeDigits == queryDigits) ||
        (rawDigits.isNotEmpty() && pBarcodeDigits == rawDigits) ||
        (queryNoLeadingZeros.isNotEmpty() && pBarcodeNoZeros == queryNoLeadingZeros) ||
        (rawNoLeadingZeros.isNotEmpty() && pBarcodeNoZeros == rawNoLeadingZeros) ||
        // 12 vs 13 digit UPC/EAN leading zero conversion
        (queryDigits.length == 12 && pBarcodeDigits == "0$queryDigits") ||
        (queryDigits.length == 13 && queryDigits.startsWith("0") && pBarcodeDigits == queryDigits.drop(1)) ||
        (pBarcodeDigits.length == 12 && queryDigits == "0$pBarcodeDigits") ||
        (pBarcodeDigits.length == 13 && pBarcodeDigits.startsWith("0") && queryDigits == pBarcodeDigits.drop(1))
    }
    if (exactBarcodeMatches.isNotEmpty()) {
        return exactBarcodeMatches.sortedBy { it.sktTarihi }
    }

    // 2. EXACT PRODUCT CODE MATCH
    val exactCodeMatches = this.filter { p ->
        val pCode = p.urunKodu.trim()
        val pCodeDigits = pCode.filter { it.isDigit() }
        val pBarcode = p.barkod.trim()

        (queryProductCode != null && queryProductCode.isNotBlank() && pCode.equals(queryProductCode, ignoreCase = true)) ||
        pCode.equals(queryBarcode, ignoreCase = true) ||
        pCode.equals(raw, ignoreCase = true) ||
        (queryDigits.isNotEmpty() && pCodeDigits.isNotEmpty() && pCodeDigits == queryDigits) ||
        (rawDigits.isNotEmpty() && pCodeDigits.isNotEmpty() && pCodeDigits == rawDigits) ||
        // Swapped column check (where DB has barcode in urunKodu or vice versa)
        (queryProductCode != null && queryProductCode.isNotBlank() && pBarcode.equals(queryProductCode, ignoreCase = true)) ||
        pBarcode.equals(queryProductCode, ignoreCase = true)
    }
    if (exactCodeMatches.isNotEmpty()) {
        return exactCodeMatches.sortedBy { it.sktTarihi }
    }

    // 3. EMBEDDED DIGIT SEQUENCE MATCH (For shelf labels / QR codes containing barcode or code)
    if (queryDigits.length >= 8 || rawDigits.length >= 8) {
        val embeddedMatches = this.filter { p ->
            val pBarcodeDigits = p.barkod.trim().filter { it.isDigit() }
            val pCodeDigits = p.urunKodu.trim().filter { it.isDigit() }

            (pBarcodeDigits.length in 8..14 && (queryDigits.contains(pBarcodeDigits) || rawDigits.contains(pBarcodeDigits))) ||
            (pCodeDigits.length in 5..10 && (queryDigits.contains(pCodeDigits) || rawDigits.contains(pCodeDigits)))
        }
        if (embeddedMatches.isNotEmpty()) {
            return embeddedMatches.sortedBy { it.sktTarihi }
        }
    }

    // 4. FALLBACK TEXT SEARCH QUERY MATCH
    val searchMatches = this.filter {
        it.matchesSearchQuery(queryBarcode) ||
        it.matchesSearchQuery(raw) ||
        (queryProductCode != null && it.matchesSearchQuery(queryProductCode))
    }
    return searchMatches.sortedBy { it.sktTarihi }
}

fun parsePriceFromQr(rawInput: String): Double? {
    val trimmed = rawInput.trim()
    if (trimmed.isBlank()) return null

    val shelfData = parseShelfQrPayload(trimmed)
    if (shelfData.price != null && shelfData.price > 0.0) {
        return shelfData.price
    }

    val cleaned = trimmed.replace("₺", "").replace("TL", "").replace("tl", "").replace(",", ".").trim()
    val directValue = cleaned.toDoubleOrNull()
    if (directValue != null && directValue > 0.0) {
        return directValue
    }

    val parts = trimmed.split("-", ";", " ", "\n", "\t").map { it.trim().replace(",", ".") }
    for (part in parts) {
        val candidate = part.toDoubleOrNull()
        if (candidate != null && candidate > 0.0 && candidate < 100000.0 && !part.startsWith("869") && part.length <= 7) {
            return candidate
        }
    }
    return null
}
