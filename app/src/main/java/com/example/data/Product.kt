package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

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
    return this.lowercase(Locale.forLanguageTag("tr-TR"))
        .replace('ı', 'i')
        .replace('İ', 'i')
        .replace('ö', 'o')
        .replace('ü', 'u')
        .replace('ç', 'c')
        .replace('ş', 's')
        .replace('ğ', 'g')
}

fun Product.matchesSearchQuery(rawQuery: String, queryTokens: List<String> = emptyList()): Boolean {
    val q = rawQuery.trim()
    if (q.isEmpty()) return true

    val hasRealBarcode = !this.barkod.startsWith("NO_BARCODE_")

    // Direct substring match in name, barcode, product code or category
    if (this.urunAdi.contains(q, ignoreCase = true) ||
        (hasRealBarcode && this.barkod.contains(q, ignoreCase = true)) ||
        this.urunKodu.contains(q, ignoreCase = true) ||
        this.kategori.contains(q, ignoreCase = true)) {
        return true
    }

    val qNorm = q.normalizeForSearch()
    val barcodePart = if (hasRealBarcode) this.barkod else ""
    val targetText = "${this.urunAdi} $barcodePart ${this.urunKodu} ${this.kategori}".normalizeForSearch()

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
        if (sktTarihi <= 0L) return "SKT Girilmedi"
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr-TR"))
        return sdf.format(java.util.Date(sktTarihi))
    }
    fun getRemainingDays(todayMidnight: Long = getTodayMidnightMillis()): Long {
        if (sktTarihi <= 0L) return 9999L
        val sktCal = Calendar.getInstance().apply {
            timeInMillis = sktTarihi
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayCal = Calendar.getInstance().apply {
            timeInMillis = todayMidnight
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMillis = sktCal.timeInMillis - todayCal.timeInMillis
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
    val catTrim = kategori.trim()
    val barTrim = barkod.trim()

    // If urunAdi is pure numbers or short code and kategori has descriptive name
    if ((nameTrim.isBlank() || nameTrim.all { it.isDigit() || it == '-' || it == '_' || it == ' ' }) && catTrim.count { it.isLetter() } >= 3 && !catTrim.equals("Dolap Ürünleri", ignoreCase = true) && !catTrim.equals("Gıda Ürünleri", ignoreCase = true) && !catTrim.equals("Genel", ignoreCase = true)) {
        return catTrim.uppercase(Locale.forLanguageTag("tr-TR"))
    }
    // If name is numeric and code has letters
    if (nameTrim.isNotBlank() && nameTrim.all { it.isDigit() || it == '-' || it == '_' || it == ' ' } && codeTrim.any { it.isLetter() }) {
        return codeTrim
    }
    // If name is numeric and barkod has letters
    if (nameTrim.isNotBlank() && nameTrim.all { it.isDigit() || it == '-' || it == '_' || it == ' ' } && barTrim.any { it.isLetter() }) {
        return barTrim
    }
    if (nameTrim.isBlank() && codeTrim.isNotBlank()) {
        return codeTrim
    }
    return if (nameTrim.isNotBlank()) nameTrim else "İSİMSİZ ÜRÜN"
}

fun Product.getDisplayCode(): String {
    val nameTrim = urunAdi.trim()
    val codeTrim = urunKodu.trim()
    val barTrim = barkod.trim()

    // If urunAdi is 6-8 digit code and urunKodu has 13-digit barcode, code is actually nameTrim
    if (nameTrim.all { it.isDigit() } && nameTrim.length in 4..9 && codeTrim.length == 13) {
        return nameTrim
    }
    if (nameTrim.isNotBlank() && nameTrim.all { it.isDigit() || it == '-' || it == '_' || it == ' ' } && codeTrim.any { it.isLetter() }) {
        return nameTrim
    }
    return codeTrim
}

fun Product.getDisplayBarcode(): String {
    val barTrim = barkod.trim()
    val codeTrim = urunKodu.trim()
    // If barkod is short reyon number and urunKodu has 13-digit EAN, barcode is urunKodu
    if (barTrim.length in 1..3 && barTrim.all { it.isDigit() } && codeTrim.length == 13 && codeTrim.all { it.isDigit() }) {
        return codeTrim
    }
    return barTrim
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
    val expiryDateMillis: Long? = null,
    val expiryDateFormatted: String? = null,
    val isScaleBarcode: Boolean = false,
    val isShelfQr: Boolean = false
)

private fun parseGs1DateToMillis(rawDate6: String): Pair<Long, String>? {
    if (rawDate6.length != 6 || !rawDate6.all { it.isDigit() }) return null
    val yy = rawDate6.substring(0, 2).toIntOrNull() ?: return null
    val mm = rawDate6.substring(2, 4).toIntOrNull() ?: return null
    val dd = rawDate6.substring(4, 6).toIntOrNull() ?: return null

    val year = 2000 + yy
    if (mm in 1..12 && dd in 1..31) {
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, mm - 1)
            set(Calendar.DAY_OF_MONTH, dd)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val formatted = String.format(Locale.getDefault(), "%02d/%02d/%04d", dd, mm, year)
        return Pair(cal.timeInMillis, formatted)
    }
    return null
}

fun parseShelfQrPayload(rawInput: String): ShelfQrData {
    val trimmed = rawInput.trim()
        .removePrefix("\uFEFF")
        .replace("\u0000", "")
        .replace("\u001D", " ") // GS separator in GS1 barcodes
        .replace("\u001E", " ")
        .replace("\u001F", " ")
        .filterNot { (it.code in 0..8) || (it.code in 14..31 && it != '\t' && it != '\n' && it != '\r') }
        
    if (trimmed.isBlank()) {
        return ShelfQrData(barcode = "")
    }

    // Special Rule: Standard Store Shelf QR format: [MağazaKodu]-[13 Haneli EAN Barkod]-[Fiyat]-[Boşluk/Tire]
    // Example: "D724-8690574117291-275,00-", "D724-8690574117291-275,00", "8690574117291-275,00-"
    val cleanHyphenStr = trimmed.trim().trim('-').trim()
    if (cleanHyphenStr.contains("-")) {
        val rawHyphenParts = cleanHyphenStr.split("-").map { it.trim() }.filter { it.isNotEmpty() }
        val eanIndex = rawHyphenParts.indexOfFirst { part ->
            val digits = part.filter { it.isDigit() }
            digits.length in 8..14 && !part.contains(",") && !part.contains(".")
        }

        if (eanIndex != -1) {
            val cleanBarcode = rawHyphenParts[eanIndex].filter { it.isDigit() }
            var foundPrice: Double? = null
            var foundStoreCode: String? = null
            var foundProductCode: String? = null
            var foundName: String? = null

            for (i in rawHyphenParts.indices) {
                if (i == eanIndex) continue
                val part = rawHyphenParts[i]
                val partDigits = part.filter { it.isDigit() }
                val partClean = part.replace("₺", "").replace("TL", "").replace("tl", "").trim()
                val hasDecimal = partClean.contains(",") || partClean.contains(".")

                if (foundPrice == null && (hasDecimal || partDigits.length <= 4)) {
                    val candidate = partClean.replace(",", ".").toDoubleOrNull()
                    if (candidate != null && candidate in 0.01..9999.99) {
                        foundPrice = candidate
                        continue
                    }
                }

                if (part.length in 2..8 && part.first().isLetter() && partDigits.isNotEmpty() && foundStoreCode == null) {
                    foundStoreCode = part
                } else if (partDigits.length in 4..11 && foundProductCode == null && !hasDecimal) {
                    foundProductCode = part
                } else if (part.length >= 2 && !part.all { it.isDigit() } && foundName == null) {
                    foundName = part
                }
            }

            if (cleanBarcode.isNotEmpty()) {
                return ShelfQrData(
                    storeCode = foundStoreCode,
                    barcode = cleanBarcode,
                    price = foundPrice,
                    productCode = foundProductCode,
                    productName = foundName,
                    isShelfQr = true
                )
            }
        }
    }

    // 0. JSON Formats (e.g. {"barcode":"8690504005100","code":"16000491","price":28.0,"name":"DOST SUT"})
    if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
        try {
            var b = ""
            var c: String? = null
            var n: String? = null
            var p: Double? = null
            var expMillis: Long? = null
            var expStr: String? = null

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
                        value.replace(',', '.').replace("₺", "").replace("TL", "").replace("tl", "").trim().toDoubleOrNull()?.let { p = it }
                    } else if (key == "skt" || key == "exp" || key == "tarih" || key == "expiry") {
                        val digits = value.filter { it.isDigit() }
                        if (digits.length == 6) {
                            parseGs1DateToMillis(digits)?.let {
                                expMillis = it.first
                                expStr = it.second
                            }
                        }
                    }
                }
            }
            if (b.isNotBlank() || c != null || n != null || p != null) {
                return ShelfQrData(
                    barcode = b.ifBlank { c ?: "" },
                    productCode = c,
                    productName = n,
                    price = p,
                    expiryDateMillis = expMillis,
                    expiryDateFormatted = expStr,
                    isShelfQr = true
                )
            }
        } catch (e: Exception) {
            // Ignore JSON parsing fallback
        }
    }

    // 1. GS1 DataMatrix / GS1 QR / GS1-128 format (e.g. (01)08690504005100(17)260815(10)12345 or 010869...17260815)
    if (trimmed.contains("(01)") || trimmed.startsWith("010869") || trimmed.startsWith("]C10869") || trimmed.startsWith("]d20869") || trimmed.startsWith("]Q30869") || trimmed.contains("id.gs1.org/01/")) {
        var barcodeCandidate: String? = null
        var expMillis: Long? = null
        var expStr: String? = null

        val gtinMatch = Regex("(?:\\(01\\)|01|^](?:C1|d2|Q3)01|/01/)?([0-9]{13,14})").find(trimmed)
        if (gtinMatch != null) {
            val rawGtin = gtinMatch.groupValues[1]
            barcodeCandidate = if (rawGtin.length == 14 && rawGtin.startsWith("0")) rawGtin.substring(1) else rawGtin
        }

        // GS1 Expiration date AI (17) YYMMDD - Sıkılaştırılmış ve güvenli eşleşme
        val expMatch = Regex("""(?:\(17\)|/17/|(?<=[\u001D\s])17|^(?:\](?:C1|d2|Q3))?01[0-9]{14}17)([0-9]{6})""").find(trimmed)
        if (expMatch != null) {
            parseGs1DateToMillis(expMatch.groupValues[1])?.let {
                expMillis = it.first
                expStr = it.second
            }
        }

        if (barcodeCandidate != null) {
            return ShelfQrData(
                barcode = barcodeCandidate,
                expiryDateMillis = expMillis,
                expiryDateFormatted = expStr,
                isShelfQr = true
            )
        }
    }

    // 2. URL Formats (e.g. https://.../product?barcode=8690504005100&kod=16000491&price=28.50)
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("www.")) {
        var b = ""
        var c: String? = null
        var p: Double? = null
        val uriQueryMatch = Regex("[?&](?:barcode|barkod|bar|b|ean|gtin)=([0-9]+)").find(trimmed)
        val uriCodeMatch = Regex("[?&](?:kod|code|urunkodu|k|item)=([a-zA-Z0-9_-]+)").find(trimmed)
        val uriPriceMatch = Regex("[?&](?:price|fiyat|f|p)=([0-9.,]+)").find(trimmed)

        if (uriQueryMatch != null) b = uriQueryMatch.groupValues[1]
        if (uriCodeMatch != null) c = uriCodeMatch.groupValues[1]
        if (uriPriceMatch != null) {
            val rawP = uriPriceMatch.groupValues[1].trim()
            val hasDecimal = rawP.contains(",") || rawP.contains(".")
            val digitsOnly = rawP.filter { it.isDigit() }
            if (hasDecimal || digitsOnly.length <= 4) {
                val candidateP = rawP.replace(',', '.').toDoubleOrNull()
                if (candidateP != null && candidateP in 0.01..9999.99) {
                    p = candidateP
                }
            }
        }

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
                price = p,
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
        var expMillis: Long? = null
        var expStr: String? = null

        val pairs = trimmed.split(Regex("[;\n\r,|&]")).map { it.trim() }
        for (pair in pairs) {
            val kv = pair.split(Regex("[:=]"), limit = 2).map { it.trim() }
            if (kv.size == 2) {
                val key = kv[0].lowercase(Locale.ROOT)
                val value = kv[1]
                if (key.contains("barkod") || key.contains("bar") || key == "b" || key == "ean" || key == "gtin") {
                    b = value.filter { it.isDigit() }
                } else if (key.contains("kod") || key == "k" || key.contains("code") || key == "item") {
                    c = value.trim()
                } else if (key.contains("adi") || key.contains("isim") || key.contains("name") || key == "a" || key == "urun") {
                    n = value.trim()
                } else if (key.contains("fiyat") || key.contains("price") || key == "f" || key == "p" || key.contains("tl") || key.contains("₺")) {
                    val pRaw = value.replace("₺", "").replace("TL", "").replace("tl", "").trim()
                    val hasDecimal = pRaw.contains(",") || pRaw.contains(".")
                    val digitsOnly = pRaw.filter { it.isDigit() }
                    if (hasDecimal || digitsOnly.length <= 4) {
                        val candidate = pRaw.replace(',', '.').toDoubleOrNull()
                        if (candidate != null && candidate in 0.01..9999.99) {
                            p = candidate
                        }
                    }
                } else if (key.contains("skt") || key.contains("exp") || key.contains("tarih")) {
                    val digits = value.filter { it.isDigit() }
                    if (digits.length == 6) {
                        parseGs1DateToMillis(digits)?.let {
                            expMillis = it.first
                            expStr = it.second
                        }
                    }
                }
            }
        }
        if (b.isNotBlank() || c != null || n != null || p != null) {
            return ShelfQrData(
                barcode = b.ifBlank { c ?: "" },
                productCode = c,
                productName = n,
                price = p,
                expiryDateMillis = expMillis,
                expiryDateFormatted = expStr,
                isShelfQr = true
            )
        }
    }

    // 4. Delimited formats (Pipe, Semicolon, Tab, Asterisk, Slash, Comma)
    val delimiters = listOf("|", ";", "\t", "*", "/", ",")
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
                    } else if (part.replace('₺', ' ').replace("TL", "").replace("tl", "").trim().isNotEmpty() && p == null) {
                        val pClean = part.replace('₺', ' ').replace("TL", "").replace("tl", "").trim()
                        val hasDecimal = pClean.contains(",") || pClean.contains(".")
                        val digitsOnly = pClean.filter { it.isDigit() }
                        if (hasDecimal || digitsOnly.length <= 4) {
                            val candidate = pClean.replace(',', '.').toDoubleOrNull()
                            if (candidate != null && candidate in 0.01..9999.99) {
                                p = candidate
                            }
                        }
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

    // 5. Hyphenated & Underscore Formats (e.g. D724-8683130143209-79, D724-8690504005100-28,00-16000491, 16000491-8690504005100-28,00)
    if (trimmed.contains("-") || trimmed.contains("_")) {
        val splitDelim = if (trimmed.contains("-")) "-" else "_"
        val rawHyphenParts = trimmed.split(splitDelim).map { it.trim() }.filter { it.isNotEmpty() }

        if (rawHyphenParts.size >= 2) {
            val eanIndex = rawHyphenParts.indexOfFirst { part ->
                val digits = part.filter { it.isDigit() }
                digits.length in 12..14 && !part.contains(",") && !part.contains(".")
            }.let { if (it != -1) it else rawHyphenParts.indexOfFirst { p -> p.filter { c -> c.isDigit() }.length in 8..14 && !p.contains(",") && !p.contains(".") } }

            val barcodePart = if (eanIndex != -1) rawHyphenParts[eanIndex] else null

            var detectedStoreCode: String? = null
            var detectedProductCode: String? = null
            var detectedPrice: Double? = null
            var detectedName: String? = null

            val otherParts = if (eanIndex != -1) rawHyphenParts.filterIndexed { idx, _ -> idx != eanIndex } else rawHyphenParts
            for (op in otherParts) {
                val opDigits = op.filter { it.isDigit() }
                val parsedOpPrice = op.replace("₺", "").replace("TL", "").replace("tl", "").replace(",", ".").trim().toDoubleOrNull()

                // Check if it's a store/branch code (e.g. D724, M102, S10, B99, or short alphanumeric starting with letter)
                if (op.length in 2..8 && op.first().isLetter() && opDigits.isNotEmpty() && detectedStoreCode == null) {
                    detectedStoreCode = op
                }
                // Check if it's a valid price (e.g. 79, 79.90, 79,90, 129.50) - max 4 digits if integer, 1..9999 TL
                else if (parsedOpPrice != null && parsedOpPrice in 0.01..9999.99 && detectedPrice == null && (op.contains(",") || op.contains(".") || opDigits.length <= 4)) {
                    detectedPrice = parsedOpPrice
                }
                // Check if it's a product/item code (e.g. 16000491, 25001234, 4-10 digits)
                else if (opDigits.length in 4..10 && detectedProductCode == null) {
                    detectedProductCode = op
                }
                // Otherwise treat as product title/description
                else if (op.length >= 2 && !op.all { it.isDigit() } && detectedName == null) {
                    detectedName = op
                }
            }

            val cleanBarcode = barcodePart?.filter { it.isDigit() } ?: ""
            if (cleanBarcode.isNotEmpty() || detectedProductCode != null || detectedStoreCode != null || detectedPrice != null) {
                return ShelfQrData(
                    storeCode = detectedStoreCode,
                    barcode = cleanBarcode.ifEmpty { detectedProductCode ?: "" },
                    price = detectedPrice,
                    productCode = detectedProductCode,
                    productName = detectedName,
                    isShelfQr = true
                )
            }
        }
    }

    // 6. Space-separated format (e.g. 8690504005100 16000491 28,50 TL DOST SUT)
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
            } else if (tok.replace('₺', ' ').replace("TL", "").replace("tl", "").trim().isNotEmpty() && p == null) {
                val tokClean = tok.replace('₺', ' ').replace("TL", "").replace("tl", "").trim()
                val hasDecimal = tokClean.contains(",") || tokClean.contains(".")
                if ((hasDecimal || tokDigits.length <= 4) && tokDigits.length !in 5..14) {
                    val candidate = tokClean.replace(',', '.').toDoubleOrNull()
                    if (candidate != null && candidate in 0.01..9999.99) {
                        p = candidate
                    }
                }
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

    // 7. Variable-Weight / Scale Barcode Detection (EAN-13 starting with 20, 21, 22, 23, 24, 27, 28, 29)
    val digitsOnly = trimmed.filter { it.isDigit() }
    val isScaleCode = digitsOnly.length == 13 && (digitsOnly.startsWith("20") || digitsOnly.startsWith("21") ||
            digitsOnly.startsWith("22") || digitsOnly.startsWith("23") || digitsOnly.startsWith("24") ||
            digitsOnly.startsWith("27") || digitsOnly.startsWith("28") || digitsOnly.startsWith("29"))

    if (isScaleCode) {
        val baseCode = digitsOnly.substring(0, 7) // e.g. 2800456
        val subCode = digitsOnly.substring(2, 7)  // e.g. 00456
        return ShelfQrData(
            barcode = digitsOnly,
            productCode = baseCode,
            isScaleBarcode = true,
            isShelfQr = false
        )
    }

    // 8. Plain single barcode or product code string
    return ShelfQrData(
        barcode = if (digitsOnly.length in 8..14) digitsOnly else trimmed,
        productCode = if (digitsOnly.length in 4..11) digitsOnly else trimmed,
        isShelfQr = false
    )
}

/**
 * Universal high-performance product matching engine for 1D barcodes, 2D QR codes, and Shelf tags.
 * Matches exact barcodes, product codes, normalized digits, leading zeros, scale barcodes, and product titles.
 */
fun List<Product>.findMatchingProducts(rawQuery: String): List<Product> {
    val raw = rawQuery.trim()
        .removePrefix("\uFEFF")
        .replace("\u0000", "")
        .replace("\u001D", " ")
        .filterNot { it.code in 0..8 || it.code in 14..31 }
        
    if (raw.isBlank()) return emptyList()

    val qrData = parseShelfQrPayload(raw)
    val queryBarcode = qrData.barcode.trim()
    val queryProductCode = qrData.productCode?.trim()
    val queryProductCodeDigits = queryProductCode?.filter { it.isDigit() } ?: ""
    val queryDigits = queryBarcode.filter { it.isDigit() }
    val queryNoLeadingZeros = queryDigits.trimStart('0')
    val rawDigits = raw.filter { it.isDigit() }
    val rawNoLeadingZeros = rawDigits.trimStart('0')

    // 0. VARIABLE-WEIGHT / SCALE BARCODE MATCH (Tartılı ürün eşleşmesi - En yüksek öncelik)
    if (qrData.isScaleBarcode || (queryDigits.length == 13 && (queryDigits.startsWith("20") || queryDigits.startsWith("21") ||
            queryDigits.startsWith("22") || queryDigits.startsWith("23") || queryDigits.startsWith("24") ||
            queryDigits.startsWith("27") || queryDigits.startsWith("28") || queryDigits.startsWith("29")))) {
        val scaleDigits = if (queryDigits.length == 13) queryDigits else rawDigits
        if (scaleDigits.length == 13) {
            val base7 = scaleDigits.substring(0, 7) // e.g. 2800456
            val base5 = scaleDigits.substring(2, 7) // e.g. 00456

            val scaleMatches = this.filter { p ->
                val pBarcodeDigits = p.barkod.trim().filter { it.isDigit() }
                val pCodeDigits = p.urunKodu.trim().filter { it.isDigit() }

                pBarcodeDigits.startsWith(base7) ||
                pBarcodeDigits == base7 ||
                pBarcodeDigits == base5 ||
                pBarcodeDigits == "${base7}000000" ||
                pCodeDigits == base7 ||
                pCodeDigits == base5 ||
                pCodeDigits.startsWith(base7)
            }
            if (scaleMatches.isNotEmpty()) {
                return scaleMatches.sortedBy { it.sktTarihi }
            }
        }
    }

    // 1. SHELF QR PRODUCT CODE MATCH (Top Priority when Shelf QR or Product Code is detected)
    if (!queryProductCode.isNullOrBlank()) {
        val codeMatches = this.filter { p ->
            val pCode = p.urunKodu.trim()
            val pCodeDigits = pCode.filter { it.isDigit() }
            val pBarcode = p.barkod.trim()
            val pBarcodeDigits = pBarcode.filter { it.isDigit() }

            (pCode.isNotEmpty() && pCode.equals(queryProductCode, ignoreCase = true)) ||
            (queryProductCodeDigits.isNotEmpty() && pCodeDigits.isNotEmpty() && pCodeDigits == queryProductCodeDigits) ||
            (pCode.isNotEmpty() && pCode.equals(queryBarcode, ignoreCase = true)) ||
            (pBarcode.isNotEmpty() && pBarcode.equals(queryProductCode, ignoreCase = true)) ||
            (queryProductCodeDigits.isNotEmpty() && pBarcodeDigits.isNotEmpty() && pBarcodeDigits == queryProductCodeDigits)
        }
        if (codeMatches.isNotEmpty()) {
            return codeMatches.sortedBy { it.sktTarihi }
        }
    }

    // 2. EXACT BARCODE MATCH
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

    // 3. EXACT PRODUCT CODE MATCH (Fallback)
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

    // 4. EMBEDDED DIGIT SEQUENCE MATCH (For shelf labels / QR codes containing barcode or code)
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

    // 5. FALLBACK TEXT SEARCH QUERY MATCH
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
    if (shelfData.price != null && shelfData.price in 0.01..9999.99) {
        return shelfData.price
    }

    val cleaned = trimmed
        .replace("₺", "")
        .replace("TL", "")
        .replace("tl", "")
        .replace(" ", "")
        .trim()

    val hasDecimal = cleaned.contains(",") || cleaned.contains(".")
    val digitsOnly = cleaned.filter { it.isDigit() }
    if (hasDecimal || digitsOnly.length <= 4) {
        val directValue = cleaned.replace(",", ".").toDoubleOrNull()
        if (directValue != null && directValue in 0.01..9999.99) {
            return directValue
        }
    }

    val parts = trimmed.split("-", ";", " ", "\n", "\r", "\t", "*", "/", "|")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    for (part in parts) {
        val partClean = part.replace("₺", "").replace("TL", "").replace("tl", "").trim()
        val partHasDecimal = partClean.contains(",") || partClean.contains(".")
        val partDigits = partClean.filter { it.isDigit() }

        // Ignore parts that have no comma/dot and > 4 digits (product codes, barcodes)
        if (!partHasDecimal && partDigits.length > 4) {
            continue
        }
        if (partClean.startsWith("869") || partClean.startsWith("0869")) {
            continue
        }

        val candidate = partClean.replace(",", ".").toDoubleOrNull()
        if (candidate != null && candidate in 0.01..9999.99) {
            return candidate
        }
    }
    return null
}
