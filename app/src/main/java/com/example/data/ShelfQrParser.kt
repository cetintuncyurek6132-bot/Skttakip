package com.example.data

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object ShelfQrParser {

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

    fun parse(rawInput: String): ShelfQrData {
        val trimmed = rawInput.trim()
            .removePrefix("\uFEFF")
            .replace("\u0000", "")
            .replace("\u001D", " ")

        // 1. JSON Payload Check
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))
        ) {
            try {
                val jsonObj = org.json.JSONObject(trimmed)
                var b = ""
                for (key in listOf("barcode", "barkod", "gtin", "ean", "ean13", "code", "b")) {
                    if (jsonObj.has(key)) {
                        b = jsonObj.optString(key, "")
                        if (b.isNotEmpty()) break
                    }
                }
                var c: String? = null
                for (key in listOf("product_code", "productCode", "urun_kodu", "urunKodu", "kod", "c", "item_code", "sku")) {
                    if (jsonObj.has(key)) {
                        c = jsonObj.optString(key, null)
                        if (!c.isNullOrEmpty()) break
                    }
                }
                var n: String? = null
                for (key in listOf("name", "urun_adi", "urunAdi", "title", "n", "desc", "description")) {
                    if (jsonObj.has(key)) {
                        n = jsonObj.optString(key, null)
                        if (!n.isNullOrEmpty()) break
                    }
                }
                var p: Double? = null
                for (key in listOf("price", "fiyat", "p", "amount")) {
                    if (jsonObj.has(key)) {
                        p = jsonObj.optDouble(key, -1.0).takeIf { it > 0 }
                        if (p != null) break
                    }
                }
                var expMillis: Long? = null
                var expStr: String? = null
                for (key in listOf("expiry_date", "expiryDate", "skt", "exp", "tett", "son_kullanma_tarihi")) {
                    if (jsonObj.has(key)) {
                        val rawExp = jsonObj.optString(key, "")
                        val digits = rawExp.filter { it.isDigit() }
                        if (digits.length == 6) {
                            parseGs1DateToMillis(digits)?.let {
                                expMillis = it.first
                                expStr = it.second
                            }
                        }
                        if (expMillis != null) break
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
            } catch (_: Exception) {
                // Fallthrough if not valid JSON
            }
        }

        // 2. GS1 Application Identifier parsing
        if (trimmed.contains("(01)") || trimmed.startsWith("01") && trimmed.length >= 16) {
            val gs1Regex = Regex("(?:\\(01\\)|01)(\\d{14})")
            val match = gs1Regex.find(trimmed)
            if (match != null) {
                val gtin14 = match.groupValues[1]
                val cleanGtin = if (gtin14.startsWith("0")) gtin14.substring(1) else gtin14
                var expMillis: Long? = null
                var expStr: String? = null
                val expMatch = Regex("(?:\\(17\\)|17)(\\d{6})").find(trimmed)
                if (expMatch != null) {
                    parseGs1DateToMillis(expMatch.groupValues[1])?.let {
                        expMillis = it.first
                        expStr = it.second
                    }
                }
                return ShelfQrData(
                    barcode = cleanGtin,
                    productCode = cleanGtin,
                    expiryDateMillis = expMillis,
                    expiryDateFormatted = expStr,
                    isShelfQr = true
                )
            }
        }

        // 3. URL Query Parameter format (e.g. ?b=869...&c=16000491&p=24.50)
        if (trimmed.contains("?") && trimmed.contains("=")) {
            val queryStr = trimmed.substringAfter("?")
            val pairs = queryStr.split("&")
            var b = ""
            var c: String? = null
            var n: String? = null
            var p: Double? = null
            var expMillis: Long? = null
            var expStr: String? = null
            for (pair in pairs) {
                val kv = pair.split("=", limit = 2)
                if (kv.size < 2) continue
                val key = kv[0].lowercase().trim()
                val value = java.net.URLDecoder.decode(kv[1].trim(), "UTF-8")
                if (key in listOf("b", "barcode", "barkod", "gtin", "ean")) {
                    b = value.filter { it.isDigit() }
                } else if (key in listOf("c", "code", "kod", "urun_kodu", "urunkodu", "sku")) {
                    c = value
                } else if (key in listOf("n", "name", "isim", "ad", "urun_adi", "urunadi", "title")) {
                    n = value
                } else if (key in listOf("p", "price", "fiyat")) {
                    val pRaw = value.replace("TL", "").replace("tl", "").replace('₺', ' ').trim()
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
                    var expMillis: Long? = null
                    var expStr: String? = null

                    for (part in parts) {
                        val digits = part.filter { it.isDigit() }
                        if (digits.length in 12..14 && b.isEmpty()) {
                            b = digits
                        } else if (digits.length in 4..11 && c == null) {
                            c = part
                        } else if (digits.length == 6 && expMillis == null) {
                            parseGs1DateToMillis(digits)?.let {
                                expMillis = it.first
                                expStr = it.second
                            }
                        } else if (part.replace('₺', ' ').replace("TL", "").replace("tl", "").trim().isNotEmpty() && p == null) {
                            val partClean = part.replace('₺', ' ').replace("TL", "").replace("tl", "").trim()
                            val hasDecimal = partClean.contains(",") || partClean.contains(".")
                            if ((hasDecimal || digits.length <= 4) && digits.length !in 5..14) {
                                val candidate = partClean.replace(',', '.').toDoubleOrNull()
                                if (candidate != null && candidate in 0.01..9999.99) {
                                    p = candidate
                                }
                            }
                        } else if (!part.all { it.isDigit() } && part.length >= 2 && n == null) {
                            n = part
                        }
                    }

                    if (b.isNotEmpty() || c != null || p != null) {
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
            }
        }

        // 5. Check for Turkish Retail QR (e.g., A101, BİM, ŞOK shelf tags with hyphen separation)
        if (trimmed.contains("-")) {
            val parts = trimmed.split("-").map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size >= 2) {
                var detectedStoreCode: String? = null
                var detectedProductCode: String? = null
                var detectedPrice: Double? = null
                var detectedName: String? = null
                val barcodePart = parts.firstOrNull { it.filter { ch -> ch.isDigit() }.length in 12..14 }
                val otherParts = parts.filter { it != barcodePart }

                for (op in otherParts) {
                    val opDigits = op.filter { it.isDigit() }
                    val parsedOpPrice = op.replace(',', '.').toDoubleOrNull()

                    // Check if it's a store prefix (e.g. D724, M101, A101, SOK)
                    if (op.matches(Regex("^[A-Za-z]{1,4}[0-9]{0,5}$")) && detectedStoreCode == null && op.length in 2..6) {
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
}
