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

fun parseShelfQrPayload(rawInput: String): ShelfQrData {
    return ShelfQrParser.parse(rawInput)
}

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
