package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Morning Check Tour SKT Warning Severity levels.
 */
enum class MorningSktWarningSeverity(
    val title: String,
    val shortLabel: String,
    val iconEmoji: String,
    val isUrgent: Boolean
) {
    CRITICAL_EXPIRED(
        title = "SÜRESİ GEÇMİŞ ÜRÜN",
        shortLabel = "SÜRESİ GEÇTİ",
        iconEmoji = "🚨",
        isUrgent = true
    ),
    EXPIRING_TODAY(
        title = "SKT BUGÜN DOLUYOR",
        shortLabel = "BUGÜN BİTİYOR",
        iconEmoji = "🔴",
        isUrgent = true
    ),
    URGENT_CRITICAL(
        title = "KRİTİK ACİLİYET (1-3 GÜN)",
        shortLabel = "KRİTİK",
        iconEmoji = "⚠️",
        isUrgent = true
    ),
    NEAR_WARNING(
        title = "YAKIN TAKİP (4-7 GÜN)",
        shortLabel = "YAKIN TAKİP",
        iconEmoji = "🟠",
        isUrgent = false
    ),
    MONITORING_SOON(
        title = "SABAH TURU İNCELEME (8-20 GÜN)",
        shortLabel = "TUR LİSTESİ",
        iconEmoji = "🟡",
        isUrgent = false
    ),
    SAFE_NORMAL(
        title = "GÜVENLİ DURUM (>20 GÜN)",
        shortLabel = "GÜVENLİ",
        iconEmoji = "🟢",
        isUrgent = false
    ),
    MISSING_SKT(
        title = "SKT TARİHİ GİRİLMEMİŞ",
        shortLabel = "SKT EKSİK",
        iconEmoji = "⚪",
        isUrgent = false
    ),
    PRODUCT_NOT_FOUND(
        title = "TANIMSIZ BARKOD",
        shortLabel = "BULUNAMADI",
        iconEmoji = "❓",
        isUrgent = false
    )
}

/**
 * Recommended next action for the store employee during morning tour.
 */
enum class MorningActionRecommendation(
    val buttonText: String,
    val actionGuide: String
) {
    FIREYE_AYIR(
        buttonText = "🗑 Fireye Ayır",
        actionGuide = "Ürün SKT'si dolmuş veya kritik durumda. Reyondan derhal çekilip fire alanına ayrılmalıdır."
    ),
    SATILDI_DUS(
        buttonText = "🟢 Satıldı / Stok Düş",
        actionGuide = "Ürün rafta bulunmuyorsa satış olarak stoktan düşürünüz."
    ),
    INDIRIM_ONE_CEK(
        buttonText = "🏷 Öne Çek / İndirim",
        actionGuide = "SKT çok yaklaştı (1-3 gün). Ön sıralara dizin veya indirimli satış etiketi uygulayın."
    ),
    RAF_KONTROL_NOTR(
        buttonText = "✓ Rafta Sağlam (Nötr)",
        actionGuide = "Ürün fiziksel olarak kontrol edildi ve reyonda sağlam duruyor."
    ),
    SKT_TANIMLA(
        buttonText = "📅 SKT Tarihi Gir",
        actionGuide = "Ürünün son kullanma tarihi sisteme işlenmemiş, lütfen ambalajdaki tarihi kaydedin."
    ),
    YENI_KAYIT(
        buttonText = "Yeni Ürün Ekle",
        actionGuide = "Taranan barkod sistemde kayıtlı değil. Yeni ürün kartı açınız."
    )
}

/**
 * Audio tone cues for rapid store scanning.
 */
enum class MorningAudioTone {
    ALERT_HIGH,      // Long continuous / double high beep for expired items
    ALERT_WARNING,   // Fast alert tone for 1-3 days critical items
    SUCCESS_CHIME,   // Clean confirmation chime for safe or valid items
    NEUTRAL_BEEP     // Standard short beep
}

/**
 * Detailed evaluation payload returned by [MorningSktCheckEvaluator].
 */
data class MorningSktEvaluationResult(
    val product: Product?,
    val scannedRaw: String,
    val todayMidnightMillis: Long,
    val sktMillis: Long,
    val remainingDays: Long,
    val severity: MorningSktWarningSeverity,
    val badgeTitle: String,
    val headerSubtitle: String,
    val detailedWarningMessage: String,
    val recommendedAction: MorningActionRecommendation,
    val audioTone: MorningAudioTone,
    val formattedSktDate: String,
    val formattedTodayDate: String,
    val daysDifferenceText: String
)

/**
 * Summary overview for morning tour metrics.
 */
data class MorningTourEvaluationOverview(
    val totalTourItems: Int,
    val expiredCount: Int,
    val expiringTodayCount: Int,
    val criticalCount: Int,
    val warningCount: Int,
    val safeCount: Int,
    val totalUrgentActionRequired: Int
)

/**
 * Core business logic layer for morning check tour SKT verification.
 * Automatically compares scanned products, dates and barcodes with today's calendar date
 * and generates actionable alerts for store employees.
 */
object MorningSktCheckEvaluator {

    private val dateFormatter = SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("tr-TR"))
    private val shortDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("tr-TR"))

    /**
     * Evaluates a known [Product] against today's date.
     */
    fun evaluateProduct(
        product: Product,
        todayMidnightMillis: Long = getTodayMidnightMillis()
    ): MorningSktEvaluationResult {
        val sktMillis = product.sktTarihi
        val todayStr = dateFormatter.format(Date(todayMidnightMillis))

        if (sktMillis <= 0L) {
            return MorningSktEvaluationResult(
                product = product,
                scannedRaw = product.barkod,
                todayMidnightMillis = todayMidnightMillis,
                sktMillis = 0L,
                remainingDays = 9999L,
                severity = MorningSktWarningSeverity.MISSING_SKT,
                badgeTitle = "⚪ SKT GİRİLMEMİŞ",
                headerSubtitle = "Tarih bilgisi sistemde eksik",
                detailedWarningMessage = "Bu ürünün (${product.getDisplayName()}) son kullanma tarihi sisteme girilmemiş. Lütfen ambalaj üzerindeki SKT'yi kontrol edip kaydedin.",
                recommendedAction = MorningActionRecommendation.SKT_TANIMLA,
                audioTone = MorningAudioTone.NEUTRAL_BEEP,
                formattedSktDate = "Belirtilmedi",
                formattedTodayDate = todayStr,
                daysDifferenceText = "SKT Belirsiz"
            )
        }

        val sktStr = dateFormatter.format(Date(sktMillis))
        val sktCal = Calendar.getInstance().apply {
            timeInMillis = sktMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayCal = Calendar.getInstance().apply {
            timeInMillis = todayMidnightMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMillis = sktCal.timeInMillis - todayCal.timeInMillis
        val remainingDays = kotlin.math.round(diffMillis.toDouble() / (1000.0 * 60 * 60 * 24)).toLong()

        return when {
            // Tarihi Geçmiş (diff < 0)
            remainingDays < 0L -> {
                val pastDays = abs(remainingDays)
                MorningSktEvaluationResult(
                    product = product,
                    scannedRaw = product.barkod,
                    todayMidnightMillis = todayMidnightMillis,
                    sktMillis = sktMillis,
                    remainingDays = remainingDays,
                    severity = MorningSktWarningSeverity.CRITICAL_EXPIRED,
                    badgeTitle = "🚨 SÜRESİ GEÇMİŞ ($pastDays GÜN ÖNCE DOLDU)",
                    headerSubtitle = "SKT: $sktStr (Bugün: $todayStr)",
                    detailedWarningMessage = "DİKKAT: Ürünün son kullanma tarihi $pastDays gün önce ($sktStr) dolmuştur! Reyonda bırakılması yasaktır. Derhal FİREYE AYIRINIZ.",
                    recommendedAction = MorningActionRecommendation.FIREYE_AYIR,
                    audioTone = MorningAudioTone.ALERT_HIGH,
                    formattedSktDate = sktStr,
                    formattedTodayDate = todayStr,
                    daysDifferenceText = "-$pastDays gün (Geçmiş)"
                )
            }

            // Bugün Bitiyor (diff == 0)
            remainingDays == 0L -> {
                MorningSktEvaluationResult(
                    product = product,
                    scannedRaw = product.barkod,
                    todayMidnightMillis = todayMidnightMillis,
                    sktMillis = sktMillis,
                    remainingDays = 0L,
                    severity = MorningSktWarningSeverity.EXPIRING_TODAY,
                    badgeTitle = "🔴 BUGÜN BİTİYOR (SON GÜN)",
                    headerSubtitle = "SKT: $sktStr (Bugün)",
                    detailedWarningMessage = "ACİL: Bu ürünün son kullanma tarihi BUGÜNDÜR! Gün sonuna kadar satılmazsa akşam fireye ayrılacaktır. İndirimli reyonuna öne çekin veya fire işlemi yapın.",
                    recommendedAction = MorningActionRecommendation.INDIRIM_ONE_CEK,
                    audioTone = MorningAudioTone.ALERT_HIGH,
                    formattedSktDate = sktStr,
                    formattedTodayDate = todayStr,
                    daysDifferenceText = "Bugün doluyor (0 gün)"
                )
            }

            // Kritik Aciliyet (1 - 3 Gün Kaldı)
            remainingDays in 1L..3L -> {
                MorningSktEvaluationResult(
                    product = product,
                    scannedRaw = product.barkod,
                    todayMidnightMillis = todayMidnightMillis,
                    sktMillis = sktMillis,
                    remainingDays = remainingDays,
                    severity = MorningSktWarningSeverity.URGENT_CRITICAL,
                    badgeTitle = "⚠️ KRİTİK RİSK (SON $remainingDays GÜN)",
                    headerSubtitle = "SKT: $sktStr (Kalan: $remainingDays gün)",
                    detailedWarningMessage = "KRİTİK UYARI: Ürünün SKT'sine sadece $remainingDays gün kaldı ($sktStr). Stok adedi: ${product.stokAdedi}. Ürünü reyonun en önüne çekin veya indirim uygulayın.",
                    recommendedAction = MorningActionRecommendation.INDIRIM_ONE_CEK,
                    audioTone = MorningAudioTone.ALERT_WARNING,
                    formattedSktDate = sktStr,
                    formattedTodayDate = todayStr,
                    daysDifferenceText = "$remainingDays gün kaldı"
                )
            }

            // Yakın Takip (4 - 7 Gün Kaldı)
            remainingDays in 4L..7L -> {
                MorningSktEvaluationResult(
                    product = product,
                    scannedRaw = product.barkod,
                    todayMidnightMillis = todayMidnightMillis,
                    sktMillis = sktMillis,
                    remainingDays = remainingDays,
                    severity = MorningSktWarningSeverity.NEAR_WARNING,
                    badgeTitle = "🟠 YAKIN TAKİP ($remainingDays GÜN KALDI)",
                    headerSubtitle = "SKT: $sktStr (Kalan: $remainingDays gün)",
                    detailedWarningMessage = "YAKIN TAKİP: Ürünün son kullanma tarihine $remainingDays gün var ($sktStr). Rafta rotasyon yapıp eski tarihli ürünlerin ön sıralarda olduğundan emin olun.",
                    recommendedAction = MorningActionRecommendation.RAF_KONTROL_NOTR,
                    audioTone = MorningAudioTone.SUCCESS_CHIME,
                    formattedSktDate = sktStr,
                    formattedTodayDate = todayStr,
                    daysDifferenceText = "$remainingDays gün kaldı"
                )
            }

            // Rutin Reyon İnceleme (8 - 20 Gün)
            remainingDays in 8L..20L -> {
                MorningSktEvaluationResult(
                    product = product,
                    scannedRaw = product.barkod,
                    todayMidnightMillis = todayMidnightMillis,
                    sktMillis = sktMillis,
                    remainingDays = remainingDays,
                    severity = MorningSktWarningSeverity.MONITORING_SOON,
                    badgeTitle = "🟡 KONTROL EDİLDİ ($remainingDays GÜN)",
                    headerSubtitle = "SKT: $sktStr (Kalan: $remainingDays gün)",
                    detailedWarningMessage = "Kontrol listesindeki ürün incelendi. $remainingDays gün geçerlilik süresi bulunmaktadır. Reyonda normal seyrinde sergilenebilir.",
                    recommendedAction = MorningActionRecommendation.RAF_KONTROL_NOTR,
                    audioTone = MorningAudioTone.SUCCESS_CHIME,
                    formattedSktDate = sktStr,
                    formattedTodayDate = todayStr,
                    daysDifferenceText = "$remainingDays gün kaldı"
                )
            }

            // Güvenli (> 20 Gün)
            else -> {
                MorningSktEvaluationResult(
                    product = product,
                    scannedRaw = product.barkod,
                    todayMidnightMillis = todayMidnightMillis,
                    sktMillis = sktMillis,
                    remainingDays = remainingDays,
                    severity = MorningSktWarningSeverity.SAFE_NORMAL,
                    badgeTitle = "🟢 GÜVENLİ ($remainingDays GÜN)",
                    headerSubtitle = "SKT: $sktStr",
                    detailedWarningMessage = "Ürünün son kullanma tarihine $remainingDays gün vardır. Reyon satışına tamamen uygundur.",
                    recommendedAction = MorningActionRecommendation.RAF_KONTROL_NOTR,
                    audioTone = MorningAudioTone.SUCCESS_CHIME,
                    formattedSktDate = sktStr,
                    formattedTodayDate = todayStr,
                    daysDifferenceText = "$remainingDays gün kaldı"
                )
            }
        }
    }

    /**
     * Evaluates a scanned barcode or shelf QR against the current store product inventory.
     */
    fun evaluateScannedBarcode(
        rawCode: String,
        inventory: List<Product>,
        todayMidnightMillis: Long = getTodayMidnightMillis()
    ): MorningSktEvaluationResult {
        val trimmedCode = rawCode.trim()
        val parsedQr = parseShelfQrPayload(trimmedCode)
        val targetBarcode = parsedQr.barcode.ifBlank { trimmedCode }
        val targetUrunKodu = parsedQr.productCode

        // Search matching product
        val matchingProduct = inventory.find { p ->
            (targetBarcode.isNotBlank() && p.barkod.equals(targetBarcode, ignoreCase = true)) ||
            (!targetUrunKodu.isNullOrBlank() && p.urunKodu.equals(targetUrunKodu, ignoreCase = true)) ||
            p.matchesSearchQuery(targetBarcode)
        }

        if (matchingProduct == null) {
            val todayStr = dateFormatter.format(Date(todayMidnightMillis))
            return MorningSktEvaluationResult(
                product = null,
                scannedRaw = trimmedCode,
                todayMidnightMillis = todayMidnightMillis,
                sktMillis = 0L,
                remainingDays = 9999L,
                severity = MorningSktWarningSeverity.PRODUCT_NOT_FOUND,
                badgeTitle = "❓ ÜRÜN BULUNAMADI",
                headerSubtitle = "Barkod: $targetBarcode",
                detailedWarningMessage = "Taranan '$targetBarcode' barkodu mağaza sisteminde kayıtlı değil. Yeni ürün olarak kaydedebilirsiniz.",
                recommendedAction = MorningActionRecommendation.YENI_KAYIT,
                audioTone = MorningAudioTone.NEUTRAL_BEEP,
                formattedSktDate = "Kayıt Yok",
                formattedTodayDate = todayStr,
                daysDifferenceText = "Tanımsız"
            )
        }

        return evaluateProduct(matchingProduct, todayMidnightMillis)
    }

    /**
     * Evaluates a standalone OCR or manually detected date timestamp against today's date.
     */
    fun evaluateDateTimestamp(
        detectedSktMillis: Long,
        associatedProduct: Product? = null,
        todayMidnightMillis: Long = getTodayMidnightMillis()
    ): MorningSktEvaluationResult {
        val todayStr = dateFormatter.format(Date(todayMidnightMillis))
        val sktStr = dateFormatter.format(Date(detectedSktMillis))
        val sktCal = Calendar.getInstance().apply {
            timeInMillis = detectedSktMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayCal = Calendar.getInstance().apply {
            timeInMillis = todayMidnightMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMillis = sktCal.timeInMillis - todayCal.timeInMillis
        val remainingDays = kotlin.math.round(diffMillis.toDouble() / (1000.0 * 60 * 60 * 24)).toLong()

        return when {
            remainingDays < 0L -> {
                val pastDays = abs(remainingDays)
                MorningSktEvaluationResult(
                    product = associatedProduct,
                    scannedRaw = associatedProduct?.barkod ?: "",
                    todayMidnightMillis = todayMidnightMillis,
                    sktMillis = detectedSktMillis,
                    remainingDays = remainingDays,
                    severity = MorningSktWarningSeverity.CRITICAL_EXPIRED,
                    badgeTitle = "🚨 SÜRESİ GEÇMİŞ ($pastDays GÜN ÖNCE DOLDU)",
                    headerSubtitle = "Okunan Tarih: $sktStr",
                    detailedWarningMessage = "UYARI: Okunan ambalaj tarihi ($sktStr) $pastDays gün önce dolmuştur. Ürün fireye ayrılmalıdır.",
                    recommendedAction = MorningActionRecommendation.FIREYE_AYIR,
                    audioTone = MorningAudioTone.ALERT_HIGH,
                    formattedSktDate = sktStr,
                    formattedTodayDate = todayStr,
                    daysDifferenceText = "-$pastDays gün"
                )
            }
            remainingDays == 0L -> {
                MorningSktEvaluationResult(
                    product = associatedProduct,
                    scannedRaw = associatedProduct?.barkod ?: "",
                    todayMidnightMillis = todayMidnightMillis,
                    sktMillis = detectedSktMillis,
                    remainingDays = 0L,
                    severity = MorningSktWarningSeverity.EXPIRING_TODAY,
                    badgeTitle = "🔴 BUGÜN BİTİYOR (SON GÜN)",
                    headerSubtitle = "Okunan Tarih: $sktStr (Bugün)",
                    detailedWarningMessage = "DİKKAT: Okunan ambalaj tarihi BUGÜN ($sktStr) doluyor.",
                    recommendedAction = MorningActionRecommendation.INDIRIM_ONE_CEK,
                    audioTone = MorningAudioTone.ALERT_HIGH,
                    formattedSktDate = sktStr,
                    formattedTodayDate = todayStr,
                    daysDifferenceText = "Bugün (0 gün)"
                )
            }
            remainingDays in 1L..3L -> {
                MorningSktEvaluationResult(
                    product = associatedProduct,
                    scannedRaw = associatedProduct?.barkod ?: "",
                    todayMidnightMillis = todayMidnightMillis,
                    sktMillis = detectedSktMillis,
                    remainingDays = remainingDays,
                    severity = MorningSktWarningSeverity.URGENT_CRITICAL,
                    badgeTitle = "⚠️ KRİTİK ($remainingDays GÜN KALDI)",
                    headerSubtitle = "Okunan Tarih: $sktStr",
                    detailedWarningMessage = "KRİTİK: Okunan ambalaj tarihine yalnızca $remainingDays gün kalmıştır ($sktStr).",
                    recommendedAction = MorningActionRecommendation.INDIRIM_ONE_CEK,
                    audioTone = MorningAudioTone.ALERT_WARNING,
                    formattedSktDate = sktStr,
                    formattedTodayDate = todayStr,
                    daysDifferenceText = "$remainingDays gün"
                )
            }
            remainingDays in 4L..7L -> {
                MorningSktEvaluationResult(
                    product = associatedProduct,
                    scannedRaw = associatedProduct?.barkod ?: "",
                    todayMidnightMillis = todayMidnightMillis,
                    sktMillis = detectedSktMillis,
                    remainingDays = remainingDays,
                    severity = MorningSktWarningSeverity.NEAR_WARNING,
                    badgeTitle = "🟠 YAKIN TAKİP ($remainingDays GÜN KALDI)",
                    headerSubtitle = "Okunan Tarih: $sktStr",
                    detailedWarningMessage = "YAKIN TAKİP: Tarihe $remainingDays gün kalmıştır ($sktStr).",
                    recommendedAction = MorningActionRecommendation.RAF_KONTROL_NOTR,
                    audioTone = MorningAudioTone.SUCCESS_CHIME,
                    formattedSktDate = sktStr,
                    formattedTodayDate = todayStr,
                    daysDifferenceText = "$remainingDays gün"
                )
            }
            else -> {
                MorningSktEvaluationResult(
                    product = associatedProduct,
                    scannedRaw = associatedProduct?.barkod ?: "",
                    todayMidnightMillis = todayMidnightMillis,
                    sktMillis = detectedSktMillis,
                    remainingDays = remainingDays,
                    severity = MorningSktWarningSeverity.SAFE_NORMAL,
                    badgeTitle = "🟢 GÜVENLİ ($remainingDays GÜN VAR)",
                    headerSubtitle = "Okunan Tarih: $sktStr",
                    detailedWarningMessage = "Okunan tarih reyon için güvenlidir ($sktStr).",
                    recommendedAction = MorningActionRecommendation.RAF_KONTROL_NOTR,
                    audioTone = MorningAudioTone.SUCCESS_CHIME,
                    formattedSktDate = sktStr,
                    formattedTodayDate = todayStr,
                    daysDifferenceText = "$remainingDays gün"
                )
            }
        }
    }

    /**
     * Aggregates an overall morning tour summary for queue analysis.
     */
    fun calculateMorningTourOverview(
        queue: List<Product>,
        todayMidnightMillis: Long = getTodayMidnightMillis()
    ): MorningTourEvaluationOverview {
        var expired = 0
        var expiringToday = 0
        var critical = 0
        var warning = 0
        var safe = 0

        for (prod in queue) {
            val eval = evaluateProduct(prod, todayMidnightMillis)
            when (eval.severity) {
                MorningSktWarningSeverity.CRITICAL_EXPIRED -> expired++
                MorningSktWarningSeverity.EXPIRING_TODAY -> expiringToday++
                MorningSktWarningSeverity.URGENT_CRITICAL -> critical++
                MorningSktWarningSeverity.NEAR_WARNING -> warning++
                else -> safe++
            }
        }

        return MorningTourEvaluationOverview(
            totalTourItems = queue.size,
            expiredCount = expired,
            expiringTodayCount = expiringToday,
            criticalCount = critical,
            warningCount = warning,
            safeCount = safe,
            totalUrgentActionRequired = expired + expiringToday + critical
        )
    }
}
