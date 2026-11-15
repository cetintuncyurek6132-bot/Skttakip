package com.example.util

import java.util.Locale

/**
 * GitHub Release ve Commit geçmişinden gelen metinleri akıllıca analiz eden,
 * önce Gemini API ile mağaza çalışanının anlayacağı akıcı Türkçeye çeviren,
 * çevrimdışı durumda ise agresif İngilizce filtresiyle yarım İngilizce kelimeleri
 * tamamen engelleyen sürüm notu çeviri motoru.
 */
object ReleaseNotesTranslator {

    const val SINGLE_FALLBACK_NOTE = "Sistem altyapısı ve performans iyileştirmeleri yapıldı."

    val DEFAULT_CORPORATE_NOTES = listOf(
        SINGLE_FALLBACK_NOTE
    )

    fun getCorporateFallback(versionName: String = ""): List<String> {
        return listOf(SINGLE_FALLBACK_NOTE)
    }

    // Kesinlikle ekranda görünmemesi gereken İngilizce kelimeler ve teknik terimler
    private val FORBIDDEN_ENGLISH_WORDS = setOf(
        "fields", "field", "updated", "updatedat", "period", "auditperiod",
        "switched", "switch", "audit", "audits", "entity", "entities",
        "room", "migration", "migrations", "lazycolumn", "compose",
        "view", "views", "viewmodel", "data", "user", "users",
        "inventoryauditentity", "inventoryauditdao", "appdatabase",
        "to", "from", "with", "for", "in", "on", "at", "by", "of", "the", "and", "or",
        "into", "onto", "when", "while", "after", "before", "between", "through", "during",
        "add", "added", "adding", "update", "updating", "fix", "fixed", "fixing",
        "remove", "removed", "removing", "delete", "deleted", "implement", "implemented",
        "introduce", "introduced", "refactor", "refactored", "resolve", "resolved",
        "create", "created", "improve", "improved", "optimize", "optimized",
        "screen", "screens", "modal", "modals", "dialog", "dialogs", "panel", "panels",
        "button", "buttons", "card", "cards", "column", "row", "box", "surface",
        "padding", "margin", "scroll", "verticalscroll", "overscroll", "weight",
        "state", "flow", "dao", "database", "table", "schema", "backup", "restore",
        "feature", "features", "support", "supports", "supported", "enable", "enabled",
        "disable", "disabled", "replace", "replaced", "change", "changed", "changes",
        "layout", "ui", "ux", "icon", "icons", "color", "colors", "theme", "font", "fonts",
        "locale", "string", "strings", "number", "numbers", "list", "lists", "item", "items",
        "detail", "details", "category", "categories", "history", "chart", "charts",
        "trend", "trends", "weekly", "daily", "monthly", "target", "ratio", "revenue",
        "cost", "firecost", "targetratio", "createdat", "millis", "auditdatemillis",
        "readonly", "locked", "initial", "current", "latest", "previous", "next",
        "handle", "handler", "handling", "manager", "helper", "util", "utils",
        "repository", "model", "component", "components", "section", "header", "footer",
        "topbar", "bottombar", "sheet", "bottomsheet", "wrapper", "bug", "bugs",
        "issue", "issues", "crash", "crashes", "error", "errors", "warning", "warnings",
        "potential", "reductions", "tagging", "metrics", "summary", "tracking", "inventory",
        "scanner", "camera", "barcode", "stock", "product", "products", "expiry", "expiration",
        "notification", "notifications", "dark", "light", "jdk", "desugaring", "kotlin",
        "android", "firebase", "gradle", "build", "compile", "lint", "test", "tests",
        "null", "true", "false", "fun", "val", "var", "class", "object", "interface", "suspend"
    )

    // CamelCase veya PascalCase kod terimlerini (örn: updatedAt, auditPeriod, InventoryAuditEntity, LazyColumn) yakalar
    private val CAMEL_CASE_CODE_REGEX = Regex("\\b([a-z]+[A-Z][a-zA-Z0-9]*|[A-Z][a-z0-9]+[A-Z][a-zA-Z0-9]*)\\b")

    // Dosya uzantılarını yakalar (örn: DashboardScreen.kt, strings.xml)
    private val CODE_FILE_EXTENSION_REGEX = Regex("(?i)\\b\\w+\\.(kt|kts|xml|json|gradle|java|toml)\\b")

    private val TURKISH_KEYWORDS = listOf(
        "ve", "ile", "için", "yapıldı", "eklendi", "düzeltildi", "güncellendi",
        "düzeltme", "hata", "buton", "sayfa", "ürün", "ekranı", "tasarımı",
        "ayarlar", "bildirim", "stok", "fire", "satış", "tarihi", "skt",
        "kayıt", "arama", "tarayıcı", "yenilendi", "geliştirildi", "kaldırıldı",
        "iyileştirildi", "kod", "düzenlendi", "öngörü", "tahmin", "yeni", "modülü",
        "hatırlatıcı", "rapor", "sayım", "reyon", "raf", "kullanıcı", "taşındı",
        "kısayol", "özellik", "akıllı", "panel", "modernize", "entegre", "sağlandı",
        "çıkarıldı", "giderildi", "optimize", "vitrin", "kurtarma", "ciro", "maliyet",
        "etiket", "indirim", "zincir", "analiz", "denetim", "raporu", "envanter",
        "haftalık", "performans", "altyapı"
    )

    /**
     * Bir cümlenin içinde yasaklı İngilizce kelime, CamelCase kod adı veya dosya adı olup olmadığını denetler.
     * Varsa true döner ve o cümle ASLA ekrana yazdırılmaz.
     */
    fun containsForbiddenEnglish(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return true

        // 1. CamelCase / PascalCase kod terimi kontrolü (updatedAt, InventoryAuditEntity, LazyColumn vb.)
        if (CAMEL_CASE_CODE_REGEX.containsMatchIn(trimmed)) {
            return true
        }

        // 2. Kod dosyası uzantısı kontrolü (.kt, .xml vb.)
        if (CODE_FILE_EXTENSION_REGEX.containsMatchIn(trimmed)) {
            return true
        }

        // 3. Kelime bazlı agresif İngilizce kontrolü
        val trLocale = Locale.forLanguageTag("tr-TR")
        val enLocale = Locale.US
        val words = trimmed
            .replace(Regex("[^\\p{L}\\p{N}_]+"), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        for (word in words) {
            val lowerEn = word.lowercase(enLocale)
            val lowerTr = word.lowercase(trLocale)
            if (lowerEn in FORBIDDEN_ENGLISH_WORDS || lowerTr in FORBIDDEN_ENGLISH_WORDS) {
                return true
            }
        }

        return false
    }

    /**
     * Ham notları satır satır inceler, markdown ve teknik sembolleri temizler,
     * terimleri ve cümle kalıplarını Türkçeleştirip liste döner.
     * İçinde İngilizce kelime kalan hiçbir satırı listeye almaz.
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
            if (translated.isNotBlank() && !containsForbiddenEnglish(translated)) {
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
        val line = raw
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
        val lower = raw.lowercase(Locale.US).replace("'", "").replace("`", "").trim()
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
     * İçinde İngilizce kelime kalırsa boş string döner (asla yarım İngilizce döndürmez).
     */
    fun translateSingleLine(line: String): String {
        var text = cleanLineSymbols(line)
        if (text.isBlank()) return ""

        // 1. ADIM: Conventional Commit Prefix'lerini ve Action Ön Eklerini Temizle
        val conventionalRegex = Regex(
            "^(feat|fix|perf|refactor|style|docs|chore)(\\([^)]+\\))?\\s*:\\s*",
            RegexOption.IGNORE_CASE
        )
        text = text.replace(conventionalRegex, "").trim()

        val actionPrefixRegex = Regex(
            "^(add|added|implement|implemented|introduce|introduced|create|created|fix|fixed|resolve|resolved|update|updated|improve|improved|optimize|optimized|refactor|refactored|remove|removed|delete|deleted)\\s*[:\\-]?\\s+",
            RegexOption.IGNORE_CASE
        )
        val hadActionPrefix = actionPrefixRegex.containsMatchIn(text)
        text = text.replace(actionPrefixRegex, "").trim()

        val lower = text.lowercase(Locale.US)

        // 2. ADIM: ZATEN DOĞAL VE TAMAMEN TÜRKÇE OLAN CÜMLELERİN KORUNMASI
        if (isNaturalTurkish(text)) {
            val formatted = formatFinalSentence(text)
            if (!containsForbiddenEnglish(formatted)) {
                return formatted
            }
        }

        // 3. ADIM: PERAKENDE VE MARKET ALANINA ÖZEL SEMANTİK KALIPLAR

        // Envanter Takip, Haftalık Veri ve Finansal Oran Kalıpları
        if (lower.contains("inventoryaudit") || lower.contains("auditperiod") || lower.contains("updatedat") ||
            (lower.contains("inventory") && (lower.contains("audit") || lower.contains("weekly") || lower.contains("trend") || lower.contains("category")))
        ) {
            if (lower.contains("weekly") || lower.contains("trend") || lower.contains("auditperiod") || lower.contains("updatedat")) {
                return "Envanter takip modülüne haftalık ilerleme ve trend analizi altyapısı eklendi."
            }
            if (lower.contains("detail") || lower.contains("modal") || lower.contains("chart")) {
                return "Envanter kategori detay ekranı ve haftalık gelişim grafiği eklendi."
            }
            return "Envanter takip modülü ve finansal fire oranı hesaplamaları güncellendi."
        }

        // Font ve Türkçe Karakter Desteği Kalıpları
        if (lower.contains("font") || lower.contains("locale") || lower.contains("turkish") || lower.contains("uppercase")) {
            return "Türkçe karakter ve yazı tipi görünümü tüm ekranlarda iyileştirildi."
        }

        // Kaydırma ve Ana Sayfa Düzeni Kalıpları
        if (lower.contains("lazycolumn") || lower.contains("overscroll") || lower.contains("verticalscroll")) {
            return "Sayfa kaydırma performansı ve ekran yerleşimi iyileştirildi."
        }

        // Kalıp 1: Finansal Analiz & KPI Metrikleri & Reyon Risk Özeti
        if (lower.contains("financial") || lower.contains("kpi") || lower.contains("finansal") || lower.contains("revenue") || lower.contains("firecost")) {
            if ((lower.contains("implement") || lower.contains("component") || lower.contains("panel")) &&
                (lower.contains("analiz") || lower.contains("analytics") || lower.contains("dashboard") || lower.contains("gösterge"))
            ) {
                return "Analiz ekranına finansal performans göstergeleri ve reyon risk özeti eklendi."
            }
            if (lower.contains("risk summary") || (lower.contains("risk") && lower.contains("summary")) || lower.contains("reyon")) {
                return "Finansal analiz ve reyon risk özeti özellikleri eklendi."
            }
            if (lower.contains("recovery") || lower.contains("kurtarma") || lower.contains("ciro") || lower.contains("fire") || lower.contains("revenue")) {
                return "Ciro, fire tutarı ve hedef oran analiz kartları güncellendi."
            }
            return "Finansal analiz ve performans göstergeleri geliştirildi."
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
                return "Acil vitrin için tümünü gör yönlendirmesi eklendi."
            }
            if (lower.contains("7 day") || lower.contains("7 gün") || lower.contains("window") || lower.contains("extend")) {
                return "Acil müdahale vitrini 7 güne çıkarıldı."
            }
            return "Acil müdahale vitrini ve kritik ürün takibi geliştirildi."
        }

        // Kalıp 6: SKT Dağılım Grafiği
        if (lower.contains("chart") || lower.contains("grafik") || lower.contains("distribution") || lower.contains("dağılım")) {
            if (lower.contains("today") || lower.contains("bugün") || lower.contains("indicator") || lower.contains("tooltip") || lower.contains("card")) {
                return "SKT dağılım grafiğine bugün referans çizgisi ve etkileşim kartı eklendi."
            }
            if (lower.contains("axis") || lower.contains("align") || lower.contains("hiza") || lower.contains("label") || lower.contains("etiket")) {
                return "SKT dağılım grafiği tarih etiketlerinin hizalaması düzeltildi."
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
                return "Mağaza Notları kısayolu Ana Sayfa ekranına taşındı."
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
            return "Denetim raporları için dışa aktarma özelliği eklendi."
        }

        // Kalıp 11: Yedekleme & Geri Yükleme
        if (lower.contains("backup") || lower.contains("restore") || lower.contains("yedek")) {
            return "Veritabanı yedekleme ve geri yükleme işlemleri daha kararlı hâle getirildi."
        }

        // Kalıp 12: Karanlık Tema & Arayüz Düzeni
        if (lower.contains("dark mode") || lower.contains("karanlık tema")) {
            return "Karanlık tema uyumluluğu ve görsel kontrast iyileştirildi."
        }
        if ((lower.contains("layout") || lower.contains("ui") || lower.contains("design") || lower.contains("arayüz") || lower.contains("morningoperation") || lower.contains("addedittakip")) &&
            (lower.contains("modern") || lower.contains("refactor") || lower.contains("clean") || lower.contains("align") || lower.contains("scroll") || lower.contains("card") || lower.contains("modal"))
        ) {
            return "Arayüz düzeni ve kart hizalamaları modernize edildi."
        }

        // Kalıp 13: Depo İade & Adetsel Sayım
        if (lower.contains("depo") || lower.contains("iade") || lower.contains("adetsel") || lower.contains("sayım") || lower.contains("takip")) {
            return "Depo iade ve adetsel sayım işlemleri optimize edildi."
        }

        // Kalıp 14: Release Notes Translator
        if (lower.contains("releasenotestranslator") || (lower.contains("release note") && lower.contains("translat"))) {
            return "Akıllı sürüm notu çeviricisi güncellendi."
        }

        // 4. ADIM: GENEL CÜMLE ÇEVİRİ VE KELİME DÖNÜŞTÜRÜCÜ
        val constructed = constructTurkishSentence(text, hadActionPrefix)
        if (constructed.isNotBlank() && !containsForbiddenEnglish(constructed)) {
            return formatFinalSentence(constructed)
        }

        // 5. ADIM: EĞER HÂLÂ İNGİLİZCE KELİME KALIYORSA ASLA YARIM CÜMLE GÖSTERME
        val fallback = resolveFallbackForUnmatched(lower)
        return if (fallback.isNotBlank() && !containsForbiddenEnglish(fallback)) fallback else ""
    }

    private fun isNaturalTurkish(text: String): Boolean {
        val lower = text.lowercase(Locale.forLanguageTag("tr-TR")).trim()
        if (lower.isBlank()) return false
        if (containsForbiddenEnglish(text)) return false

        val hasTurkishChar = text.any { it in "çğıöşüÇĞİÖŞÜ" }
        val hasTurkishWord = TURKISH_KEYWORDS.any { lower.contains(it) }

        return hasTurkishChar || hasTurkishWord
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
            Regex("(?i)\\brisk summary panel\\b") to "risk özeti paneli",
            Regex("(?i)\\brisk summary\\b") to "reyon risk özeti",
            Regex("(?i)\\bfinancial kpi metrics\\b") to "finansal performans göstergeleri",
            Regex("(?i)\\bfinancial analysis\\b") to "finansal analiz",
            Regex("(?i)\\bfinancial\\b") to "finansal",
            Regex("(?i)\\bprediction features\\b") to "tahminleme özellikleri",
            Regex("(?i)\\breyon risk summary components\\b") to "reyon bazlı risk analiz bileşenleri",
            Regex("(?i)\\binventory reductions\\b") to "stok azalışları",
            Regex("(?i)\\bstock loss\\b") to "stok kaybı",
            Regex("(?i)\\bbarcode scanner\\b") to "barkod tarayıcı",
            Regex("(?i)\\buser experience\\b") to "kullanıcı deneyimi",
            Regex("(?i)\\bdata management\\b") to "veri yönetimi",
            Regex("(?i)\\brelease notes\\b") to "sürüm notları",
            Regex("(?i)\\bfeatures?\\b") to "özellikleri",
            Regex("(?i)\\bcomponents?\\b") to "bileşenleri",
            Regex("(?i)\\bpotential\\b") to "olası",
            Regex("(?i)\\btracking\\b") to "takibi",
            Regex("(?i)\\bmetrics\\b") to "göstergeleri"
        )

        for ((pattern, replacement) in dict) {
            t = t.replace(pattern, replacement)
        }

        t = t.trim()
        if (t.isBlank()) return ""

        val lowerT = t.lowercase(Locale.forLanguageTag("tr-TR"))
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
        return ""
    }

    /**
     * AKILLI ÇEVİRİ: Önce Gemini API, erişilemezse Agresif Filtreli Yerel Sözlük.
     *
     * 1. Önce GitHub'dan gelen ham metin Gemini API'ye gönderilir.
     * 2. Gemini'den dönen maddeler agresif İngilizce filtresinden geçirilir.
     * 3. Gemini'ye erişilemezse yerel sözlük devreye girer ve içinde İngilizce kelime kalan
     *    tüm satırlar elenir.
     * 4. Eğer geçerli hiçbir Türkçe cümle kalmazsa tek bir genel madde döner:
     *    "Sistem altyapısı ve performans iyileştirmeleri yapıldı."
     */
    suspend fun translateSmart(rawNotes: String, versionName: String = ""): List<String> {
        val trimmed = rawNotes.trim()
        if (trimmed.isBlank() || trimmed.equals("null", ignoreCase = true)) {
            return listOf(SINGLE_FALLBACK_NOTE)
        }

        // Clean boilerplate release lines (Full Changelog, commit hashes, URLs) before sending to Gemini
        val cleanedInputLines = trimmed
            .lines()
            .map { cleanLineSymbols(it) }
            .filter { it.isNotBlank() && !shouldSkipLine(it) }

        if (cleanedInputLines.isEmpty()) {
            return listOf(SINGLE_FALLBACK_NOTE)
        }

        val cleanedInputText = cleanedInputLines.joinToString("\n")

        // 1. ADIM: ÖNCE GEMİNİ API'Yİ ÇAĞIR
        val geminiResult = try {
            GeminiTranslator.translateToTurkish(cleanedInputText)
        } catch (e: Exception) {
            null
        }

        if (!geminiResult.isNullOrEmpty()) {
            val cleanGeminiLines = geminiResult
                .map { formatFinalSentence(cleanLineSymbols(it)) }
                .filter { it.isNotBlank() && !containsForbiddenEnglish(it) }
                .distinct()

            if (cleanGeminiLines.isNotEmpty()) {
                return cleanGeminiLines
            }
        }

        // 2. ADIM: GEMİNİ'YE ERİŞİLEMEZSE AGRESİF FİLTRELİ YEREL SÖZLÜK
        val localResult = translateToBulletPoints(cleanedInputText, versionName)
            .filter { it.isNotBlank() && !containsForbiddenEnglish(it) }
            .distinct()

        return if (localResult.isNotEmpty()) {
            localResult
        } else {
            listOf(SINGLE_FALLBACK_NOTE)
        }
    }

    private fun formatFinalSentence(text: String): String {
        var result = text.trim()
        if (result.isEmpty()) return ""

        val trLocale = Locale.forLanguageTag("tr-TR")
        result = result.replaceFirstChar { if (it.isLowerCase()) it.titlecase(trLocale) else it.toString() }
        if (!result.endsWith(".") && !result.endsWith("!") && !result.endsWith("?")) {
            result += "."
        }
        return result
    }
}
