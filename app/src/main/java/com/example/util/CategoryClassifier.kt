package com.example.util

import com.example.data.Product
import java.util.Locale

/**
 * Intelligent Retail Category Classifier for Turkish Supermarket & Grocery products.
 *
 * Classifies all products strictly into "Dolap" or "Gıda" using regex boundaries
 * and priority food exception rules to prevent word collision.
 */
object CategoryClassifier {

    const val CATEGORY_DOLAP = "Dolap"
    const val CATEGORY_GIDA = "Gıda"

    private val trLocale = Locale.forLanguageTag("tr-TR")

    // Specific meat phrases where "eti" is part of the meat description (not Eti brand)
    private val meatEtiPhrases = listOf(
        "dana eti", "kuzu eti", "sığır eti", "sigir eti", "koyun eti",
        "tavuk eti", "hindi eti", "balık eti", "balik eti", "kuşbaşı eti", "kusbasi eti"
    )

    // 1. ÖNCELİKLİ GIDA İSTİSNALARI (İçinde süt, et, krema geçse bile doğrudan GIDA):
    // İki ve üç harfli kelimeler ("eti", "un", "ot", "kek") tam kelime kontrolüyle (\bkelime\b) aranır.
    private val gidaExceptionPatterns = listOf(
        Regex("(?U)(?i)\\beti\\b"),
        Regex("(?U)(?i)\\byumurta\\b"),
        Regex("(?U)(?i)yumurta"),
        Regex("(?U)(?i)bisküvi"),
        Regex("(?U)(?i)biskuvi"),
        Regex("(?U)(?i)çikolata"),
        Regex("(?U)(?i)cikolata"),
        Regex("(?U)(?i)gofret"),
        Regex("(?U)(?i)\\bkek\\b"),
        Regex("(?U)(?i)kraker"),
        Regex("(?U)(?i)cips"),
        Regex("(?U)(?i)patates cipsi"),
        Regex("(?U)(?i)makarna"),
        Regex("(?U)(?i)\\bun\\b"),
        Regex("(?U)(?i)şeker"),
        Regex("(?U)(?i)seker"),
        Regex("(?U)(?i)bakliyat"),
        Regex("(?U)(?i)pirinç"),
        Regex("(?U)(?i)pirinc"),
        Regex("(?U)(?i)mercimek"),
        Regex("(?U)(?i)salça"),
        Regex("(?U)(?i)salca"),
        Regex("(?U)(?i)konserve"),
        Regex("(?U)(?i)sıvı yağ"),
        Regex("(?U)(?i)sivi yag"),
        Regex("(?U)(?i)zeytinyağ"),
        Regex("(?U)(?i)zeytinyag"),
        Regex("(?U)(?i)ayçiçek yağ"),
        Regex("(?U)(?i)aycicek yag"),
        Regex("(?U)(?i)uht süt"),
        Regex("(?U)(?i)uht sut"),
        Regex("(?U)(?i)uzun ömürlü süt"),
        Regex("(?U)(?i)uzun omurlu sut"),
        Regex("(?U)(?i)toz süt"),
        Regex("(?U)(?i)toz sut"),
        Regex("(?U)(?i)salep tozu"),
        Regex("(?U)(?i)salep"),
        Regex("(?U)(?i)kahve"),
        Regex("(?U)(?i)\\bçay\\b"),
        Regex("(?U)(?i)\\bcay\\b"),
        Regex("(?U)(?i)\\bot\\b")
    )

    // 2. DOLAP (SOĞUK ZİNCİR / ŞARKÜTERİ) KURALLARI:
    // Sadece tek başına geçen 'et' kelimesi \bet\b ile aranır (böylece 'paket' veya 'market' eşleşmez).
    private val dolapPatterns = listOf(
        // Süt Ürünleri
        Regex("(?U)(?i)peynir"),
        Regex("(?U)(?i)yoğurt"),
        Regex("(?U)(?i)yogurt"),
        Regex("(?U)(?i)ayran"),
        Regex("(?U)(?i)kefir"),
        Regex("(?U)(?i)tereyağ"),
        Regex("(?U)(?i)tereyag"),
        Regex("(?U)(?i)kaymak"),
        Regex("(?U)(?i)krema"),
        Regex("(?U)(?i)süzme"),
        Regex("(?U)(?i)suzme"),
        Regex("(?U)(?i)kaşar"),
        Regex("(?U)(?i)kasar"),
        Regex("(?U)(?i)\\blor\\b"),
        Regex("(?U)(?i)labne"),
        Regex("(?U)(?i)taze süt"),
        Regex("(?U)(?i)taze sut"),
        Regex("(?U)(?i)günlük süt"),
        Regex("(?U)(?i)gunluk sut"),

        // Et & Şarküteri (Tek başına geçen 'et' tam kelime sınırıyla)
        Regex("(?U)(?i)\\bet\\b"),
        Regex("(?U)(?i)kıyma"),
        Regex("(?U)(?i)kiyma"),
        Regex("(?U)(?i)kuşbaşı"),
        Regex("(?U)(?i)kusbasi"),
        Regex("(?U)(?i)tavuk"),
        Regex("(?U)(?i)piliç"),
        Regex("(?U)(?i)pilic"),
        Regex("(?U)(?i)hindi"),
        Regex("(?U)(?i)sucuk"),
        Regex("(?U)(?i)sosis"),
        Regex("(?U)(?i)salam"),
        Regex("(?U)(?i)pastırma"),
        Regex("(?U)(?i)pastirma"),
        Regex("(?U)(?i)kavurma"),
        Regex("(?U)(?i)balık"),
        Regex("(?U)(?i)balik"),
        Regex("(?U)(?i)ciğer"),
        Regex("(?U)(?i)ciger"),

        // Dondurucu / Donuk
        Regex("(?U)(?i)dondurma"),
        Regex("(?U)(?i)donuk"),
        Regex("(?U)(?i)dondurulmuş"),
        Regex("(?U)(?i)dondurulmus"),
        Regex("(?U)(?i)milföy"),
        Regex("(?U)(?i)milfoy"),
        Regex("(?U)(?i)pizza"),
        Regex("(?U)(?i)nugget"),
        Regex("(?U)(?i)şnitzel"),
        Regex("(?U)(?i)snitzel"),

        // Meze
        Regex("(?U)(?i)şakşuka"),
        Regex("(?U)(?i)saksuka"),
        Regex("(?U)(?i)haydari"),
        Regex("(?U)(?i)rus salatası"),
        Regex("(?U)(?i)rus salatasi"),
        Regex("(?U)(?i)meze")
    )

    /**
     * Primary classification function.
     * Evaluates product name in Turkish lowercase and classifies as "Dolap" or "Gıda".
     */
    fun classifyCategory(productName: String): String {
        val cleanName = productName.lowercase(trLocale)

        // Özel kontrol: "dana eti", "tavuk eti" gibi et tamlamaları (Eti markası değil)
        for (meatPhrase in meatEtiPhrases) {
            if (cleanName.contains(meatPhrase)) return CATEGORY_DOLAP
        }

        // 1. ÖNCELİKLİ GIDA İSTİSNALARI:
        for (pattern in gidaExceptionPatterns) {
            if (pattern.containsMatchIn(cleanName)) return CATEGORY_GIDA
        }

        // 2. DOLAP / SOĞUK ZİNCİR KURALLARI:
        for (pattern in dolapPatterns) {
            if (pattern.containsMatchIn(cleanName)) return CATEGORY_DOLAP
        }

        // 3. Eşleşmeyen her şey varsayılan Gıda:
        return CATEGORY_GIDA
    }

    /**
     * Overload for backwards compatibility and fallback support.
     */
    fun classify(productName: String, fallbackCategory: String? = null): String {
        return classifyCategory(productName)
    }

    /**
     * Helper to classify a Product entity directly.
     */
    fun classifyProduct(product: Product): String {
        return classifyCategory(product.urunAdi)
    }

    /**
     * Checks if a Product belongs to the "Dolap" category.
     */
    fun isDolap(product: Product): Boolean {
        return classifyCategory(product.urunAdi) == CATEGORY_DOLAP
    }
}
