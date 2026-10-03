package com.example.util

/**
 * GitHub Release ve Commit geçmişinden gelen metinleri dinamik olarak analiz eden,
 * yazılım terimlerini ve cümle kalıplarını akıllıca Türkçeleştiren ve gerçek
 * değişiklik maddelerini listeyen motor.
 */
object ReleaseNotesTranslator {

    /**
     * Ham notları satır satır inceler, markdown ve teknik sembolleri temizler,
     * terimleri ve cümle kalıplarını Türkçeleştirip liste döner.
     */
    fun translateToBulletPoints(rawNotes: String, versionName: String = ""): List<String> {
        val trimmed = rawNotes.trim()
        if (trimmed.isBlank() || trimmed.equals("null", ignoreCase = true)) {
            return if (versionName.isNotBlank()) {
                listOf("v$versionName sürüm güncellemesi.")
            } else {
                emptyList()
            }
        }

        // Markdown URL, hash, commit referansı ve kullanıcı etiketlerini temizle
        val lines = trimmed
            .replace(Regex("https?://\\S+"), "")
            .replace(Regex("\\b[0-9a-f]{7,40}\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("#\\d+"), "")
            .replace(Regex("@[a-zA-Z0-9_-]+"), "")
            .lines()
            .map { it.trim() }
            .filter { line ->
                line.isNotBlank() &&
                !line.startsWith("Full Changelog", ignoreCase = true) &&
                !line.startsWith("What's Changed", ignoreCase = true) &&
                !line.startsWith("See the assets", ignoreCase = true) &&
                !line.startsWith("Compare", ignoreCase = true) &&
                !line.startsWith("Assets", ignoreCase = true) &&
                !line.startsWith("```") &&
                line != "---"
            }

        val resultList = mutableListOf<String>()

        for (rawLine in lines) {
            val cleaned = rawLine
                .removePrefix("####")
                .removePrefix("###")
                .removePrefix("##")
                .removePrefix("#")
                .removePrefix("*")
                .removePrefix("-")
                .removePrefix("•")
                .removePrefix("+")
                .replace("`", "")
                .replace("\"", "")
                .replace("'", "")
                .trim()

            if (cleaned.isBlank()) continue

            val translated = translateSingleLine(cleaned)
            if (translated.isNotBlank()) {
                resultList.add(translated)
            }
        }

        val distinctList = resultList.distinct()
        if (distinctList.isNotEmpty()) {
            return distinctList
        }

        return if (versionName.isNotBlank()) {
            listOf("v$versionName sürüm güncellemesi.")
        } else {
            emptyList()
        }
    }

    fun formatAsBulletsString(rawNotes: String, versionName: String = ""): String {
        val items = translateToBulletPoints(rawNotes, versionName)
        return items.joinToString("\n") { "• $it" }
    }

    fun translateSingleLine(line: String): String {
        var text = line
            .replace("`", "")
            .replace("\"", "")
            .replace("'", "")
            .replace("**", "")
            .replace("__", "")
            .trim()

        if (text.isBlank()) return ""

        val lower = text.lowercase()

        // 1. ÖZEL CÜMLE VE KALIP EŞLEŞTİRMELERİ (Sentence-level pattern matchers)

        // "Improve release notes parsing" -> "Sürüm notları ayrıştırması iyileştirildi"
        if (lower.contains("release note") && (lower.contains("parse") || lower.contains("parsing") || lower.contains("format"))) {
            return "Sürüm notları ayrıştırması iyileştirildi."
        }

        // "Introduce ReleaseNotesTranslator for robust note formatting" / "Introduce ReleaseNotesTranslator"
        if (lower.contains("introduce") && lower.contains("releasenotestranslator")) {
            return "Akıllı sürüm notu çeviricisi entegre edildi."
        }

        // "Arayüz layout kod yapısı modernize edildi" -> "Arayüz düzeni ve kod yapısı modernize edildi"
        if (lower.contains("arayüz") && lower.contains("layout")) {
            return "Arayüz düzeni ve kod yapısı modernize edildi."
        }

        // "Move ... from X to Y" / "Move Store Notes shortcut from CsvScreen to DashboardScreen"
        val moveRegex = Regex("(?i)^move\\s+(.+?)\\s+from\\s+(.+?)\\s+to\\s+(.+)$")
        val moveMatch = moveRegex.find(text)
        if (moveMatch != null) {
            val item = translateKeywords(moveMatch.groupValues[1])
            val from = translateKeywords(moveMatch.groupValues[2])
            val to = translateKeywords(moveMatch.groupValues[3])
            if (from.contains("Ayarlar") && to.contains("Ana Sayfa")) {
                return "$item Ayarlar'dan Ana Sayfa'ya taşındı."
            }
            return "$item $from yerine $to ekranına taşındı."
        }

        // "Fix ... bug/issue"
        val fixIssueRegex = Regex("(?i)^fix(ed)?\\s+(.+?)\\s+(bug|issue|problem|error|crash)$")
        val fixIssueMatch = fixIssueRegex.find(text)
        if (fixIssueMatch != null) {
            val target = translateKeywords(fixIssueMatch.groupValues[2])
            return "$target hatası giderildi."
        }

        // "Introduce ..." -> "Yeni özellik eklendi: ..."
        val introduceRegex = Regex("(?i)^(introduce|introduced)\\s+(.+)$")
        val introduceMatch = introduceRegex.find(text)
        if (introduceMatch != null && !lower.contains("ve") && !lower.contains("için")) {
            val feature = translateKeywords(introduceMatch.groupValues[2])
            return "Yeni özellik eklendi: $feature."
        }

        // "Refactor ..." -> "Kod yapısı ve performans optimize edildi"
        if (lower.startsWith("refactor") && !lower.contains("ve") && !lower.contains("için")) {
            val target = translateKeywords(text.replace(Regex("(?i)^refactor(ed)?\\s*"), "").trim())
            return if (target.isNotBlank()) "$target için kod yapısı ve performans optimize edildi." else "Kod yapısı ve performans optimize edildi."
        }

        // "Improve ... layout" -> "Arayüz düzeni iyileştirildi"
        if (lower.startsWith("improve") && lower.contains("layout")) {
            val target = translateKeywords(text.replace(Regex("(?i)^improve\\s+"), "").replace(Regex("(?i)\\s+layout"), "").trim())
            return if (target.isNotBlank()) "$target arayüz düzeni iyileştirildi." else "Arayüz düzeni iyileştirildi."
        }

        // 2. TÜRKÇE CÜMLELERİN DOĞRUDAN KORUNMASI
        val turkishWords = listOf(
            "ve", "ile", "için", "yapıldı", "eklendi", "düzeltildi", "güncellendi",
            "düzeltme", "hata", "buton", "sayfa", "ürün", "ekranı", "tasarımı",
            "ayarlar", "bildirim", "stok", "fire", "satış", "tarihi", "skt",
            "kayıt", "arama", "tarayıcı", "yenilendi", "geliştirildi", "kaldırıldı",
            "iyileştirildi", "kod", "düzenlendi", "öngörü", "tahmin", "yeni", "modülü",
            "hatırlatıcı", "rapor", "sayım", "reyon", "raf", "kullanıcı", "taşındı",
            "kısayol", "özellik", "akıllı", "panel", "modernize"
        )
        val hasTurkishChar = text.any { it in "çğıöşüÇĞİÖŞÜ" }
        val isAlreadyTurkish = hasTurkishChar || turkishWords.any { lower.contains(it) }

        if (isAlreadyTurkish) {
            val cleanedTurkish = translateKeywords(text)
            return cleanedTurkish.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        // 3. EYLEM TÜRÜ ÖN EKLERİ (Prefix Matchers)
        var actionPrefix = ""
        val prefixRules = listOf(
            Regex("^(bug fix|bugfix|bug-fix|bug_fix|fix|fixed|resolve|resolved)\\s*[:\\-]?\\s*", RegexOption.IGNORE_CASE) to "Düzeltildi: ",
            Regex("^(add|added|feature|feat|new)\\s*[:\\-]?\\s*", RegexOption.IGNORE_CASE) to "Eklendi: ",
            Regex("^(update|updated|improve|improved|enhancement|enhance|enhanced)\\s*[:\\-]?\\s*", RegexOption.IGNORE_CASE) to "İyileştirildi: ",
            Regex("^(remove|removed|delete|deleted)\\s*[:\\-]?\\s*", RegexOption.IGNORE_CASE) to "Kaldırıldı: ",
            Regex("^(refactor|refactored|optimize|optimized|perf)\\s*[:\\-]?\\s*", RegexOption.IGNORE_CASE) to "Optimize Edildi: "
        )

        for ((regex, prefix) in prefixRules) {
            if (regex.containsMatchIn(text)) {
                actionPrefix = prefix
                text = text.replace(regex, "").trim()
                break
            }
        }

        text = translateKeywords(text)
        text = text.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        return if (actionPrefix.isNotEmpty()) {
            "$actionPrefix$text"
        } else {
            text
        }
    }

    fun translateKeywords(text: String): String {
        var t = text
        val dictionary = listOf(
            Regex("(?i)\\bstore notes\\b") to "Mağaza Notları",
            Regex("(?i)\\bstore note\\b") to "Mağaza Notu",
            Regex("(?i)\\bshortcut\\b") to "kısayolu",
            Regex("(?i)\\bshortcuts\\b") to "kısayolları",
            Regex("(?i)\\bcsvscreen\\b") to "Ayarlar Sayfası",
            Regex("(?i)\\bdashboardscreen\\b") to "Ana Sayfa",
            Regex("(?i)\\bproductdetailsmodal\\b") to "Ürün Detay Ekranı",
            Regex("(?i)\\bproductdetailmodal\\b") to "Ürün Detay Ekranı",
            Regex("(?i)\\bduplicate\\b") to "mükerrer",
            Regex("(?i)\\bbadge\\b") to "rozet",
            Regex("(?i)\\bbadges\\b") to "rozetler",
            Regex("(?i)\\bwaste\\b") to "fire",
            Regex("(?i)\\bspoilage\\b") to "fire",
            Regex("(?i)\\banalytics\\b") to "analiz ve tahminleme",
            Regex("(?i)\\bdashboard\\b") to "ana sayfa gösterge paneli",
            Regex("(?i)\\bscanner\\b") to "barkod tarayıcı",
            Regex("(?i)\\bbarcode\\b") to "barkod",
            Regex("(?i)\\bbackup\\b") to "yedekleme",
            Regex("(?i)\\brestore\\b") to "geri yükleme",
            Regex("(?i)\\breminders\\b") to "hatırlatıcılar",
            Regex("(?i)\\breminder\\b") to "hatırlatıcı",
            Regex("(?i)\\bcategory\\b") to "reyon/kategori",
            Regex("(?i)\\bcategories\\b") to "reyon/kategoriler",
            Regex("(?i)\\bcamera\\b") to "kamera",
            Regex("(?i)\\bnotification\\b") to "bildirim",
            Regex("(?i)\\bnotifications\\b") to "bildirimler",
            Regex("(?i)\\bhaptic feedback\\b") to "titreşimli geri bildirim",
            Regex("(?i)\\bdark mode\\b") to "karanlık tema",
            Regex("(?i)\\blight mode\\b") to "aydınlık tema",
            Regex("(?i)\\bsettings\\b") to "ayarlar",
            Regex("(?i)\\bproduct\\b") to "ürün",
            Regex("(?i)\\bproducts\\b") to "ürünler",
            Regex("(?i)\\bexpiry date\\b") to "son kullanma tarihi",
            Regex("(?i)\\bexpiration date\\b") to "son kullanma tarihi",
            Regex("(?i)\\bcalendar\\b") to "takvim",
            Regex("(?i)\\bdialog\\b") to "pencere",
            Regex("(?i)\\bmodal\\b") to "pencere",
            Regex("(?i)\\bui\\b") to "arayüz",
            Regex("(?i)\\blayout\\b") to "düzen",
            Regex("(?i)\\bperformance\\b") to "performans",
            Regex("(?i)\\bdatabase\\b") to "veritabanı",
            Regex("(?i)\\bcrash\\b") to "kapanma sorunu",
            Regex("(?i)\\bbug\\b") to "hata",
            Regex("(?i)\\bbutton\\b") to "buton",
            Regex("(?i)\\bhistory\\b") to "geçmiş",
            Regex("(?i)\\bexport\\b") to "dışa aktarma",
            Regex("(?i)\\bimport\\b") to "içe aktarma",
            Regex("(?i)\\bfor robust note formatting\\b") to "güvenilir not formatlama için"
        )

        for ((regex, replacement) in dictionary) {
            t = t.replace(regex, replacement)
        }

        return t.trim()
    }
}
