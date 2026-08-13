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
    val isShelfQr: Boolean = false
)

fun parseShelfQrPayload(rawInput: String): ShelfQrData {
    val trimmed = rawInput.trim()
    if (trimmed.isBlank()) {
        return ShelfQrData(barcode = "")
    }

    if (trimmed.contains("-")) {
        val parts = trimmed.split("-").map { it.trim() }

        // Format 1: StoreCode - Barcode - Price - ProductCode
        // Example: D724-8690504005100-28,00-16000491
        if (parts.size >= 4) {
            val storeCode = parts[0]
            val barcodeCandidate = parts[1]
            val priceCandidate = parts[2].replace(",", ".")
            val productCodeCandidate = parts[3]

            val parsedPrice = priceCandidate.toDoubleOrNull()
            if (parsedPrice != null && barcodeCandidate.isNotEmpty()) {
                return ShelfQrData(
                    storeCode = storeCode,
                    barcode = barcodeCandidate,
                    price = parsedPrice,
                    productCode = if (productCodeCandidate.isNotEmpty()) productCodeCandidate else null,
                    isShelfQr = true
                )
            }
        }

        // Format 2: Barcode - Price - ProductCode
        // Example: 8690504005100-28,00-16000491
        if (parts.size == 3) {
            val barcodeCandidate = parts[0]
            val priceCandidate = parts[1].replace(",", ".")
            val productCodeCandidate = parts[2]

            val parsedPrice = priceCandidate.toDoubleOrNull()
            if (parsedPrice != null && barcodeCandidate.isNotEmpty()) {
                return ShelfQrData(
                    storeCode = null,
                    barcode = barcodeCandidate,
                    price = parsedPrice,
                    productCode = if (productCodeCandidate.isNotEmpty()) productCodeCandidate else null,
                    isShelfQr = true
                )
            }
        }

        // Format 3: Hyphenated label (e.g. 16000491-8690504005100-RE-7)
        if (parts.size >= 2) {
            val longDigitPart = parts.firstOrNull { it.filter { c -> c.isDigit() }.length >= 10 }
            val shortDigitPart = parts.firstOrNull { it != longDigitPart && it.filter { c -> c.isDigit() }.length in 4..9 }
            if (longDigitPart != null) {
                return ShelfQrData(
                    storeCode = null,
                    barcode = longDigitPart,
                    price = null,
                    productCode = shortDigitPart,
                    isShelfQr = false
                )
            }
        }
    }

    return ShelfQrData(barcode = trimmed, isShelfQr = false)
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
