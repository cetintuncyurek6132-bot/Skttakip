package com.example.util

import java.util.Locale
import java.util.regex.Pattern

object ProductNameOcrParser {

    /**
     * Filter list of keywords that commonly appear on Turkish supermarket price tags & shelf labels
     * but are NOT part of the product name.
     */
    val EXCLUDED_LABEL_KEYWORDS = listOf(
        "FIYAT", "FİYAT", "FIYATI", "FİYATI",
        "GECERLILIK", "GEÇERLİLİK", "GECERLILIK TARIHI", "GEÇERLİLİK TARİHİ",
        "TARIHI", "TARİHİ", "TARIH", "TARİH",
        "KDV", "DAHIL", "DAHİL", "DAHILDIR", "DAHİLDİR", "HARIC", "HARİÇ",
        "MENSEI", "MENŞEİ", "MENSE", "MENŞE",
        "TURKIYE", "TÜRKİYE", "TURKİYE", "TURKIYE'DE", "TÜRKİYE'DE",
        "URETIM", "ÜRETİM", "URETIM YERI", "ÜRETİM YERİ",
        "YERLI", "YERLİ", "YERLI URETIM", "YERLİ ÜRETİM",
        "PARTI", "PARTİ", "SERI", "SERİ", "LOT", "NO",
        "ICINDEKILER", "İÇİNDEKİLER", "ALERJEN",
        "TAVSIYE", "TAVSİYE", "TETT", "SKT", "SON TUKETIM", "SON TÜKETİM",
        "URUN KODU", "ÜRÜN KODU", "BARKOD", "BARKOD NO",
        "ISLETME", "İŞLETME", "KAYIT", "ONAY",
        "1 KG =", "1 LT =", "1 ADET =", "100 G =", "100 ML =", "BIRIM FIYAT", "BİRİM FİYAT", "BIRIM FIYATI", "BİRİM FİYATI",
        "TL", "KRŞ", "KRS", "KURUS", "KURUŞ", "₺",
        "INDIRIM", "İNDİRİM", "KAMPANYA", "ETIKET", "ETİKET",
        "RAF FIYATI", "RAF FİYATI", "SATIS FIYATI", "SATIŞ FİYATI",
        "MAGAZA", "MAĞAZA", "SUBE", "ŞUBE", "REYON",
        "D724", "D-724", "M101", "A101", "BIM", "SOK", "ŞOK", "CARREFOUR", "MIGROS", "MİGROS"
    )

    private val GRAMAJ_PATTERN = Pattern.compile(
        "\\b(\\d+([.,]\\d+)?\\s*(?:KG|GR|GRAM|G|L|LT|LITRE|LİTRE|ML|CL|ADET|PK|PAKET|'L[IUÜİ]|X\\s*\\d+\\s*(?:ML|G|L|GR)?))\\b",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Extracts the cleanest product name and gramaj from ML Kit VisionText
     */
    fun extractProductNameAndGramaj(visionText: com.google.mlkit.vision.text.Text): Pair<String, List<String>> {
        val rawLines = mutableListOf<String>()
        val suggestions = mutableListOf<String>()

        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val lineWords = line.elements.map { it.text.trim() }.filter { it.isNotBlank() }
                val lineText = if (lineWords.isNotEmpty()) {
                    lineWords.joinToString(" ")
                } else {
                    line.text.trim()
                }.replace(Regex("\\s+"), " ")

                if (lineText.isNotBlank() && lineText.length >= 2) {
                    rawLines.add(lineText)
                }
            }
        }

        if (rawLines.isEmpty() && visionText.text.isNotBlank()) {
            rawLines.addAll(
                visionText.text
                    .split("\n", "\r")
                    .map { it.trim().replace(Regex("\\s+"), " ") }
                    .filter { it.length >= 2 }
            )
        }

        var extractedGramaj: String? = null
        val filteredProductLines = mutableListOf<String>()

        for (rawLine in rawLines) {
            val upperTr = rawLine.uppercase(Locale.forLanguageTag("tr-TR"))

            // 1) Filter shelf codes & barcodes (e.g., D724-8690574117291-275,00-)
            if (upperTr.contains(Regex("\\b[A-Z0-9]{3,8}-[0-9]{8,14}")) ||
                upperTr.matches(Regex("^[0-9\\-/.]{6,}$"))
            ) {
                continue
            }

            // 2) Filter date stamps like 01.01.2025 or 12/2026
            if (upperTr.matches(Regex(".*\\b\\d{1,2}[./\\-]\\d{1,2}[./\\-]\\d{2,4}\\b.*")) &&
                !upperTr.contains(Regex("[A-ZĞÜŞİÖÇ]{4,}"))
            ) {
                continue
            }

            // 3) Filter standalone price stamps like "275,00 TL" or "275.00"
            if (rawLine.matches(Regex("^[0-9.,\\s]+(TL|₺|kr|krş)?$", RegexOption.IGNORE_CASE))) {
                continue
            }

            // 4) Check if line is predominantly junk label keywords
            val words = upperTr.split(" ").filter { it.isNotBlank() }
            val junkWordCount = words.count { word ->
                EXCLUDED_LABEL_KEYWORDS.any { kw -> word == kw || word.startsWith(kw) || kw.startsWith(word) }
            }
            if (words.isNotEmpty() && (junkWordCount.toDouble() / words.size.toDouble()) >= 0.5) {
                continue
            }

            // 5) Search for gramaj pattern
            val mat = GRAMAJ_PATTERN.matcher(rawLine)
            if (mat.find()) {
                val foundGramaj = mat.group(1)?.trim()
                if (!foundGramaj.isNullOrBlank() && extractedGramaj == null) {
                    extractedGramaj = foundGramaj.uppercase(Locale.forLanguageTag("tr-TR"))
                }
            }

            // 6) Clean isolated junk words from the line itself
            val cleanedWords = words.filterNot { word ->
                val w = word.trim(',', '.', ':', ';', '-', '!', '?', '(', ')')
                EXCLUDED_LABEL_KEYWORDS.contains(w)
            }
            val cleanedLine = cleanedWords.joinToString(" ").trim()

            if (cleanedLine.isNotBlank() && cleanedLine.length >= 2 && !cleanedLine.all { it.isDigit() }) {
                filteredProductLines.add(cleanedLine)
                suggestions.add(cleanedLine)
            }
        }

        // Build best candidate
        val combinedCandidate = when {
            filteredProductLines.isEmpty() -> ""
            filteredProductLines.size == 1 -> filteredProductLines[0]
            else -> {
                filteredProductLines.take(3).joinToString(" ").replace(Regex("\\s+"), " ").trim()
            }
        }

        // Ensure gramaj is included if found and not yet part of candidate
        val finalBestCandidate = if (!extractedGramaj.isNullOrBlank() && combinedCandidate.isNotBlank()) {
            val cleanGramajNorm = extractedGramaj.replace(" ", "")
            val upperCombinedNorm = combinedCandidate.uppercase(Locale.forLanguageTag("tr-TR")).replace(" ", "")

            if (!upperCombinedNorm.contains(cleanGramajNorm)) {
                "$combinedCandidate $extractedGramaj"
            } else {
                combinedCandidate
            }
        } else {
            combinedCandidate
        }

        if (finalBestCandidate.isNotBlank() && !suggestions.contains(finalBestCandidate)) {
            suggestions.add(0, finalBestCandidate)
        }

        return Pair(finalBestCandidate, suggestions.distinct())
    }

    /**
     * Calculates word-based Jaccard similarity between two strings
     */
    fun calculateTextSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isBlank() || s2.isBlank()) return 0.0
        val words1 = s1.uppercase(Locale.forLanguageTag("tr-TR")).split(" ").filter { it.isNotBlank() }.toSet()
        val words2 = s2.uppercase(Locale.forLanguageTag("tr-TR")).split(" ").filter { it.isNotBlank() }.toSet()
        if (words1.isEmpty() || words2.isEmpty()) return 0.0
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        return intersection.toDouble() / union.toDouble()
    }
}
