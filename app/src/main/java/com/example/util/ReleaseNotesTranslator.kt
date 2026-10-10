package com.example.util

/**
 * GitHub Release ve Commit geçmişinden gelen metinleri akıllıca analiz eden,
 * teknik developer terimlerini, İngilizce kalıpları ve commit başlıklarını
 * son kullanıcı odaklı, temiz ve kurumsal Türkçeye dönüştüren çeviri motoru.
 */
object ReleaseNotesTranslator {

    val DEFAULT_CORPORATE_NOTES = listOf(
        "Sistem performansı ve kararlılık iyileştirmeleri yapıldı.",
        "Arayüz ve kullanıcı deneyimi geliştirmeleri uygulandı.",
        "Veri güvenliği ve hızlı işlem optimizasyonları sağlandı."
    )

    fun getCorporateFallback(versionName: String = ""): List<String> {
        val list = mutableListOf<String>()
        if (versionName.isNotBlank()) {
            list.add("v$versionName sürüm güncellemesi.")
        }
        list.addAll(DEFAULT_CORPORATE_NOTES)
        return list
    }

    private val ENGLISH_RESIDUAL_REGEX = Regex(
        "\\b(and|the|with|features|feature|support for|for|from|when|while|after|before|between|through|during|potential|reductions|tagging|components|metrics|summary|tracking|inventory|screen|dialog|modal|panel|cards|issue|issues|crash|crashes|error|errors|add|added|implement|implemented|introduce|introduced|fixing|fixed|bug|bugs)\\b",
        RegexOption.IGNORE_CASE
    )

    private val TURKISH_KEYWORDS = listOf(
        "ve", "ile", "için", "yapıldı", "eklendi", "düzeltildi", "güncellendi",
        "düzeltme", "hata", "buton", "sayfa", "ürün", "ekranı", "tasarımı",
        "ayarlar", "bildirim", "stok", "fire", "satış", "tarihi", "skt",
        "kayıt", "arama", "tarayıcı", "yenilendi", "geliştirildi", "kaldırıldı",
        "iyileştirildi", "kod", "düzenlendi", "öngörü", "tahmin", "yeni", "modülü",
        "hatırlatıcı", "rapor", "sayım", "reyon", "raf", "kullanıcı", "taşındı",
        "kısayol", "özellik", "akıllı", "panel", "modernize", "entegre", "sağlandı",
        "çıkarıldı", "giderildi", "optimize", "vitrin", "kurtarma", "ciro", "maliyet",
        "etiket", "indirim", "zincir", "analiz", "denetim", "raporu"
    )

    /**
     * Ham notları satır satır inceler, markdown ve teknik sembolleri temizler,
     * terimleri ve cümle kalıplarını Türkçeleştirip liste döner.
     */
    fun translateToBulletPoints(rawNotes: String, versionName: String = ""): List<String> {
        val trimmed = rawNotes.trim()
        if (trimmed.isBlank() || trimmed.equals("null", ignoreCase = true)) {
            return getCorporateFallback(versionName)
        }

        val lines = trimmed
            .lines()
            .map { it.trim() }
            .filter { line -> line.isNotBlank() && !shouldSkipLine(line) }

        val resultList = mutableListOf<String>()

        for (rawLine in lines) {
            val cleaned = cleanLineSymbols(rawLine)
            if (cleaned.isBlank() || shouldSkipLine(cleaned)) continue

            val translated = translateSingleLine(cleaned)
            if (translated.isNotBlank()) {
                resultList.add(translated)
            }
        }

        val distinctList = resultList.distinct()
        if (distinctList.isNotEmpty()) {
            return distinctList
        }

        return getCorporateFallback(versionName)
    }

    fun formatAsBulletsString(rawNotes: String, versionName: String = ""): String {
        val items = translateToBulletPoints(rawNotes, versionName)
        return items.joinToString("\n") { "• $it" }
    }

    private fun cleanLineSymbols(raw: String): String {
        var line = raw
            // Markdown linklerini temizle: [text](url) -> text
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
            // Git trailer, link ve hashleri satır bazında temizle
            .replace(Regex("(?i)\\b(by|in|via)\\s+@[a-zA-Z0-9_-]+.*"), "")
            .replace(Regex("https?://\\S+"), "")
            .replace(Regex("\\b[0-9a-f]{7,40}\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("#\\d+"), "")
            .replace(Regex("@[a-zA-Z0-9_-]+"), "")
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
            .replace("**", "")
            .replace("__", "")
            .trim()

        return line
    }

    private fun shouldSkipLine(raw: String): Boolean {
        val lower = raw.lowercase().replace("'", "").replace("`", "").trim()
        if (lower.isBlank()) return true
        if (lower == "---" || lower.startsWith("```") || lower.startsWith("***")) return true

        // Release template headers
        if (lower.startsWith("whats changed") ||
            lower.startsWith("what is changed") ||
            lower.startsWith("full changelog") ||
            lower.startsWith("see the assets") ||
            lower.startsWith("compare") ||
            lower.startsWith("assets") ||
            lower.startsWith("release notes") ||
            lower.startsWith("changelog")
        ) return true

        // Git merge commits
        if (lower.startsWith("merge pull request") ||
            lower.startsWith("merge branch") ||
            lower.startsWith("merge remote") ||
            lower.startsWith("merge commit")
        ) return true

        // Version bumps
        if (lower.startsWith("bump version") ||
            lower.startsWith("version bump") ||
            lower.startsWith("prepare release") ||
            lower.startsWith("release v") ||
            lower.matches(Regex("^(v?\\d+\\.\\d+(\\.\\d+)?).*"))
        ) return true

        // Developer chore / CI commits
        if (lower.startsWith("chore:") || lower.startsWith("chore(") ||
            lower.startsWith("ci:") || lower.startsWith("ci(") ||
            lower.startsWith("test:") || lower.startsWith("test(") ||
            lower.startsWith("build:") || lower.startsWith("build(") ||
            lower.contains("bump dependencies") ||
            lower.contains("update readme") ||
            lower.contains("update gitignore")
        ) return true

        return false
    }

    /**
     * Tek bir satırı analiz edip doğal ve kurumsal Türkçe cümleye dönüştürür.
     */
    fun translateSingleLine(line: String): String {
        var text = cleanLineSymbols(line)
        if (text.isBlank()) return ""

        // 1. ADIM: Conventional Commit Prefix'lerini ve Action Ön Eklerini Temizle
        // Örn: feat(analytics): ..., fix(scanner): ..., add: ..., Added: ...
        val conventionalRegex = Regex(
            "^(feat|fix|perf|refactor|style|docs|chore)(\\([^)]+\\))?\\s*:\\s*",
            RegexOption.IGNORE_CASE
        )
        text = text.replace(conventionalRegex, "").trim()

        val actionPrefixRegex = Regex(
            "^(add|added|implement|implemented|introduce|introduced|create|created|fix|fixed|resolve|resolved|update|updated|improve|improved|optimize|optimized|refactor|refactored|remove|removed|delete|deleted)\\s*[:\\-]?\\s+",
            RegexOption.IGNORE_CASE
        )
        // Eğer satır başında fiil varsa temizle (mükerrer 'Eklendi: Add...' olmaması için)
        val hadActionPrefix = actionPrefixRegex.containsMatchIn(text)
        text = text.replace(actionPrefixRegex, "").trim()

        var lower = text.lowercase()

        // 2. ADIM: ZATEN DOĞAL TÜRKÇE OLAN CÜMLELERİN KORUNMASI
        if (isNaturalTurkish(text)) {
            return formatFinalSentence(text)
        }

        // 3. ADIM: PERAKENDE VE MARKET ALANINA ÖZEL SEMANTİK KALIPLAR (High-Priority Domain Rules)

        // Kalıp 1: Finansal Analiz & KPI Metrikleri & Reyon Risk Özeti
        if (lower.contains("financial") || lower.contains("kpi") || lower.contains("finansal")) {
            if ((lower.contains("implement") || lower.contains("component") || lower.contains("panel")) &&
                (lower.contains("analiz") || lower.contains("analytics") || lower.contains("dashboard") || lower.contains("gösterge"))
            ) {
                return "Analiz ekranına finansal KPI metrikleri ve reyon risk özeti panelleri entegre edildi."
            }
            if (lower.contains("risk summary") || (lower.contains("risk") && lower.contains("summary")) || lower.contains("reyon")) {
                return "Finansal analiz, KPI göstergeleri ve reyon risk özeti özellikleri eklendi."
            }
            if (lower.contains("recovery") || lower.contains("kurtarma") || lower.contains("ciro") || lower.contains("fire")) {
                return "Kurtarma oranı, kurtarılan ciro ve fire risk maliyeti KPI kartları eklendi."
            }
            return "Finansal analiz ve risk metrikleri geliştirildi."
        }

        // Kalıp 2: Sarı Etiket & İndirim Takip Desteği
        if (lower.contains("yellow label") || lower.contains("sarı etiket") ||
            (lower.contains("tagging") && (lower.contains("yellow") || lower.contains("label") || lower.contains("etiket")))
        ) {
            return "Yüksek riskli ürünler için sarı etiket ve indirimli satış desteği eklendi."
        }

        // Kalıp 3: Barkod Tarayıcı & Kamera İzni Çökmesi / Hata Giderimi
        if ((lower.contains("barcode") || lower.contains("barkod") || lower.contains("scanner") || lower.contains("tarayıcı")) &&
            (lower.contains("crash") || lower.contains("fix") || lower.contains("error") || lower.contains("denied") || lower.contains("permission"))
        ) {
            if (lower.contains("permission") || lower.contains("camera") || lower.contains("izin") || lower.contains("kamera")) {
                return "Kamera izni verilmediğinde barkod tarayıcıda oluşan çökme sorunu giderildi."
            }
            return "Barkod tarayıcı kararlılığı artırıldı ve tarama hataları giderildi."
        }

        // Kalıp 4: Barkod Tarayıcı & Titreşimli Geri Bildirim
        if ((lower.contains("barcode") || lower.contains("barkod") || lower.contains("scanner")) &&
            (lower.contains("haptic") || lower.contains("feedback") || lower.contains("titreşim"))
        ) {
            return "Barkod tarayıcı performansı ve titreşimli geri bildirim iyileştirildi."
        }

        // Kalıp 5: Acil Müdahale Vitrini & SKT Filtre Aralığı (7 Gün)
        if (lower.contains("urgent") || lower.contains("acil")) {
            if (lower.contains("filter") || lower.contains("range") || lower.contains("aralık")) {
                return "Kritik SKT filtre aralığı 0-7 gün olarak güncellendi."
            }
            if (lower.contains("view all") || lower.contains("tümünü gör") || lower.contains("navigation")) {
                return "Acil vitrin için 'Tümünü Gör' yönlendirmesi eklendi."
            }
            if (lower.contains("7 day") || lower.contains("7 gün") || lower.contains("window") || lower.contains("extend")) {
                return "Acil müdahale vitrini 7 güne çıkarıldı."
            }
            return "Acil müdahale vitrini ve kritik ürün takibi geliştirildi."
        }

        // Kalıp 6: SKT Dağılım Grafiği (Bugün Göstergesi / X Ekseni Hizalama)
        if (lower.contains("chart") || lower.contains("grafik") || lower.contains("distribution") || lower.contains("dağılım")) {
            if (lower.contains("today") || lower.contains("bugün") || lower.contains("indicator") || lower.contains("tooltip") || lower.contains("card")) {
                return "SKT dağılım grafiğine 'Bugün' referans çizgisi ve etkileşim kartı eklendi."
            }
            if (lower.contains("axis") || lower.contains("align") || lower.contains("hiza") || lower.contains("label") || lower.contains("etiket")) {
                return "SKT dağılım grafiği X ekseni tarih etiketlerinin hizalaması düzeltildi."
            }
            return "SKT dağılım grafiği ve görselleştirmeler optimize edildi."
        }

        // Kalıp 7: Reyon / Kategori Risk Özeti
        if ((lower.contains("reyon") || lower.contains("category") || lower.contains("department")) &&
            (lower.contains("risk") || lower.contains("summary") || lower.contains("özet"))
        ) {
            return "Soğuk zincir ve kuru gıda reyonları için risk özeti panelleri eklendi."
        }

        // Kalıp 8: Mağaza Notları Kısayolu ve Konumu
        if (lower.contains("store note") || lower.contains("mağaza not")) {
            if (lower.contains("dashboardscreen") || lower.contains("ana sayfa") || lower.contains("move") || lower.contains("taşın")) {
                return "Mağaza Notları kısayolu Ana Sayfa'ya taşındı."
            }
            return "Mağaza Notları modülü iyileştirildi."
        }

        // Kalıp 9: Ayarlar Sekme Bileşenleri
        if ((lower.contains("default parameter") || lower.contains("parameter")) &&
            (lower.contains("ayarlar") || lower.contains("settings"))
        ) {
            return "Ayarlar ekranı sekme bileşenleri optimize edildi."
        }

        // Kalıp 10: Excel / Rapor Dışa Aktarma
        if (lower.contains("excel") || lower.contains("xlsx") ||
            (lower.contains("export") && (lower.contains("report") || lower.contains("inspection") || lower.contains("denetim")))
        ) {
            return "Denetim raporları için Excel (XLSX) dışa aktarma özelliği eklendi."
        }

        // Kalıp 11: Yedekleme & Geri Yükleme
        if (lower.contains("backup") || lower.contains("restore") || lower.contains("yedek")) {
            return "Veritabanı yedekleme ve geri yükleme işlemleri daha kararlı hâle getirildi."
        }

        // Kalıp 12: Karanlık Tema & Arayüz Düzeni
        if (lower.contains("dark mode") || lower.contains("karanlık tema")) {
            return "Karanlık tema uyumluluğu ve görsel kontrast iyileştirildi."
        }
        if ((lower.contains("layout") || lower.contains("ui") || lower.contains("design") || lower.contains("arayüz")) &&
            (lower.contains("modern") || lower.contains("refactor") || lower.contains("clean"))
        ) {
            return "Arayüz düzeni ve görsel tasarım modernize edildi."
        }

        // Kalıp 13: Depo İade & Adetsel Sayım
        if (lower.contains("depo") || lower.contains("iade") || lower.contains("adetsel") || lower.contains("sayım")) {
            return "Depo iade ve adetsel sayım işlemleri optimize edildi."
        }

        // Kalıp 14: Release Notes Translator
        if (lower.contains("releasenotestranslator") || (lower.contains("release note") && lower.contains("translat"))) {
            return "Akıllı sürüm notu çeviricisi entegre edildi."
        }

        // 4. ADIM: GENEL CÜMLE ÇEVİRİ VE KELİME DÖNÜŞTÜRÜCÜ (Syntactic sentence construction)
        val constructed = constructTurkishSentence(text, hadActionPrefix)
        if (constructed.isNotBlank() && !hasEnglishResiduals(constructed)) {
            return formatFinalSentence(constructed)
        }

        // 5. ADIM: GÜVENLİK KALİTE KAPISI (Strict Quality Gate)
        // Eğer hâlâ İngilizce artıklar içeriyorsa veya anlamsızsa, güvenli kurumsal karşılık üret
        return resolveFallbackForUnmatched(lower)
    }

    private fun isNaturalTurkish(text: String): Boolean {
        val lower = text.lowercase().trim()
        if (lower.isBlank()) return false
        if (hasEnglishResiduals(lower)) return false

        val hasTurkishChar = text.any { it in "çğıöşüÇĞİÖŞÜ" }
        val hasTurkishWord = TURKISH_KEYWORDS.any { lower.contains(it) }

        return hasTurkishChar || hasTurkishWord
    }

    private fun hasEnglishResiduals(text: String): Boolean {
        return ENGLISH_RESIDUAL_REGEX.containsMatchIn(text)
    }

    private fun constructTurkishSentence(text: String, hadActionPrefix: Boolean): String {
        var t = text

        // İngilizce bağlaç ve edat kalıplarını temizle / dönüştür
        t = t.replace(Regex("(?i)\\bfor\\s+tagging\\s+"), "")
            .replace(Regex("(?i)\\bto\\s+track\\s+"), "takip etmek için ")
            .replace(Regex("(?i)\\bto\\s+the\\s+"), "")
            .replace(Regex("(?i)\\bwith\\s+"), "ile ")
            .replace(Regex("(?i)\\bfor\\s+"), "için ")
            .replace(Regex("(?i)\\band\\s+"), "ve ")

        // Sözlük eşleştirmeleri
        val dict = listOf(
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
            Regex("(?i)\\bdashboard\\b") to "ana sayfa",
            Regex("(?i)\\bscanner\\b") to "barkod tarayıcı",
            Regex("(?i)\\bbarcode\\b") to "barkod",
            Regex("(?i)\\bbackup\\b") to "yedekleme",
            Regex("(?i)\\brestore\\b") to "geri yükleme",
            Regex("(?i)\\breminders\\b") to "hatırlatıcılar",
            Regex("(?i)\\breminder\\b") to "hatırlatıcı",
            Regex("(?i)\\bcategories\\b") to "reyonlar",
            Regex("(?i)\\bcategory\\b") to "reyon",
            Regex("(?i)\\bcamera\\b") to "kamera",
            Regex("(?i)\\bnotifications\\b") to "bildirimler",
            Regex("(?i)\\bnotification\\b") to "bildirim",
            Regex("(?i)\\bhaptic feedback\\b") to "titreşimli geri bildirim",
            Regex("(?i)\\bdark mode\\b") to "karanlık tema",
            Regex("(?i)\\blight mode\\b") to "aydınlık tema",
            Regex("(?i)\\bsettings\\b") to "ayarlar",
            Regex("(?i)\\bproducts\\b") to "ürünler",
            Regex("(?i)\\bproduct\\b") to "ürün",
            Regex("(?i)\\bexpiry date\\b") to "son kullanma tarihi",
            Regex("(?i)\\bexpiration date\\b") to "son kullanma tarihi",
            Regex("(?i)\\bcalendar\\b") to "takvim",
            Regex("(?i)\\bdialog\\b") to "penceresi",
            Regex("(?i)\\bmodal\\b") to "penceresi",
            Regex("(?i)\\bui\\b") to "arayüz",
            Regex("(?i)\\blayout\\b") to "düzen",
            Regex("(?i)\\bperformance\\b") to "performans",
            Regex("(?i)\\bdatabase\\b") to "veritabanı",
            Regex("(?i)\\bcrash\\b") to "çökme sorunu",
            Regex("(?i)\\bbug\\b") to "hata",
            Regex("(?i)\\bbutton\\b") to "buton",
            Regex("(?i)\\bbuttons\\b") to "butonlar",
            Regex("(?i)\\bhistory\\b") to "geçmiş",
            Regex("(?i)\\bexport\\b") to "dışa aktarma",
            Regex("(?i)\\bimport\\b") to "içe aktarma",
            Regex("(?i)\\bcarousel\\b") to "vitrin",
            Regex("(?i)\\bwindow\\b") to "aralık",
            Regex("(?i)\\bfilter range\\b") to "filtre aralığı",
            Regex("(?i)\\btab components\\b") to "sekme bileşenleri",
            Regex("(?i)\\byellow labels?\\b") to "sarı etiket",
            Regex("(?i)\\brisk summary\\b") to "reyon risk özeti",
            Regex("(?i)\\bfinancial\\b") to "finansal",
            Regex("(?i)\\bfeatures?\\b") to "özellikleri",
            Regex("(?i)\\bcomponents?\\b") to "bileşenleri"
        )

        for ((pattern, replacement) in dict) {
            t = t.replace(pattern, replacement)
        }

        t = t.trim()
        if (t.isBlank()) return ""

        // Eğer son eylem belirtilmemişse mantıklı bir yüklem ekle
        val lowerT = t.lowercase()
        if (!lowerT.endsWith("eklendi") && !lowerT.endsWith("düzeltildi") &&
            !lowerT.endsWith("iyileştirildi") && !lowerT.endsWith("giderildi") &&
            !lowerT.endsWith("sağlandı") && !lowerT.endsWith("yapıldı") &&
            !lowerT.endsWith("entegre edildi") && !lowerT.endsWith("kaldırıldı")
        ) {
            t = if (lowerT.contains("hata") || lowerT.contains("sorun") || lowerT.contains("çökme")) {
                "$t giderildi."
            } else if (hadActionPrefix) {
                "$t eklendi."
            } else {
                "$t iyileştirildi."
            }
        }

        return t
    }

    private fun resolveFallbackForUnmatched(lower: String): String {
        return if (lower.contains("analiz") || lower.contains("analytics") || lower.contains("kpi") || lower.contains("financial") || lower.contains("tahmin")) {
            "Finansal analiz ve risk tahminleme modülü güncellendi."
        } else if (lower.contains("skt") || lower.contains("expiry") || lower.contains("tarih")) {
            "Son kullanma tarihi (SKT) takip sistemi ve kontrolleri iyileştirildi."
        } else if (lower.contains("stok") || lower.contains("ürün") || lower.contains("product") || lower.contains("sayım")) {
            "Stok yönetimi ve ürün işlem akışları optimize edildi."
        } else if (lower.contains("barkod") || lower.contains("scanner") || lower.contains("kamera")) {
            "Barkod tarama ve kamera okuma kararlılığı artırıldı."
        } else if (lower.contains("rapor") || lower.contains("export") || lower.contains("excel") || lower.contains("iade")) {
            "Raporlama ve veri aktarım özellikleri güncellendi."
        } else if (lower.contains("hata") || lower.contains("crash") || lower.contains("bug") || lower.contains("fix")) {
            "Sistem kararlılığını artıran hata düzeltmeleri yapıldı."
        } else if (lower.contains("arayüz") || lower.contains("ui") || lower.contains("tasarım") || lower.contains("tema")) {
            "Arayüz düzeni ve kullanıcı deneyimi geliştirmeleri uygulandı."
        } else {
            ""
        }
    }

    private fun formatFinalSentence(text: String): String {
        var result = text.trim()
        if (result.isEmpty()) return ""

        result = result.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        if (!result.endsWith(".") && !result.endsWith("!") && !result.endsWith("?")) {
            result += "."
        }
        return result
    }
}
