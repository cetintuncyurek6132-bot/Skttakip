package com.example.util

import com.example.data.Product
import java.util.Locale

/**
 * Intelligent Retail Category Classifier for Turkish Supermarket & Grocery products.
 *
 * Classifies all products strictly into "Dolap" or "Gıda" using safe tokenization
 * without any regex ICU flags that cause crashes on Android.
 */
object CategoryClassifier {

    const val CATEGORY_DOLAP = "Dolap"
    const val CATEGORY_GIDA = "Gıda"

    // Specific meat phrases where "eti" is part of the meat description (not Eti brand)
    private val meatEtiPhrases = listOf(
        "dana eti", "kuzu eti", "sığır eti", "sigir eti", "koyun eti",
        "tavuk eti", "hindi eti", "balık eti", "balik eti", "kuşbaşı eti", "kusbasi eti"
    )

    // 1. ÖNCELİKLİ GIDA İSTİSNALARI (Tam kelime veya içerik):
    private val gidaWords = setOf(
        "eti", "yumurta", "makarna", "un", "seker", "şeker",
        "pirinc", "pirinç", "mercimek", "salca", "salça", "salep", "kahve", "cay", "çay"
    )

    private val gidaContains = listOf(
        "bisküvi", "biskuvi", "çikolata", "cikolata", "gofret", "kek", "kraker", "cips",
        "uht süt", "uzun ömürlü süt", "toz süt", "salep tozu"
    )

    // 2. DOLAP / SOĞUK ZİNCİR KELİMELERİ:
    private val dolapWords = setOf(
        "et", "kiyma", "kıyma", "kusbasi", "kuşbaşı", "tavuk", "pilic", "piliç",
        "hindi", "sucuk", "sosis", "salam", "pastirma", "pastırma", "kavurma",
        "balik", "balık", "ciger", "ciğer", "dondurma", "donuk", "milföy",
        "pizza", "nugget", "snitzel", "şnitzel", "meze"
    )

    private val dolapContains = listOf(
        "peynir", "yoğurt", "yogurt", "ayran", "kefir", "tereyağ", "tereyagi",
        "kaymak", "krema", "süzme", "suzme", "kasar", "kaşar", "lor", "labne",
        "şakşuka", "haydari", "rus salatası", "taze süt", "günlük süt"
    )

    fun classifyCategory(productName: String?): String {
        if (productName.isNullOrBlank()) return CATEGORY_GIDA

        return try {
            val trLocale = Locale("tr", "TR")
            val cleanName = productName.lowercase(trLocale)

            // Özel kontrol: "dana eti", "tavuk eti" gibi et tamlamaları (Eti markası değil)
            for (meatPhrase in meatEtiPhrases) {
                if (cleanName.contains(meatPhrase)) return CATEGORY_DOLAP
            }

            // Noktalama işaretlerini boşluğa çevirip kelimeleri ayır (Regex olmadan güvenli ve hızlı)
            val sb = StringBuilder(cleanName.length)
            for (i in 0 until cleanName.length) {
                val c = cleanName[i]
                if (c.isLetterOrDigit()) {
                    sb.append(c)
                } else {
                    sb.append(' ')
                }
            }
            val words = sb.toString().split(' ').filter { it.isNotBlank() }.toSet()

            // 1. ÖNCELİKLİ GIDA İSTİSNALARI (Tam kelime veya içerik):
            if (words.any { it in gidaWords }) return CATEGORY_GIDA
            if (gidaContains.any { cleanName.contains(it) }) return CATEGORY_GIDA

            // 2. DOLAP / SOĞUK ZİNCİR KELİMELERİ:
            if (words.any { it in dolapWords }) return CATEGORY_DOLAP
            if (dolapContains.any { cleanName.contains(it) }) return CATEGORY_DOLAP

            CATEGORY_GIDA
        } catch (t: Throwable) {
            CATEGORY_GIDA
        }
    }

    /**
     * Overload for backwards compatibility and fallback support.
     */
    fun classify(productName: String?, fallbackCategory: String? = null): String {
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
        return product.kategori.equals(CATEGORY_DOLAP, ignoreCase = true)
    }
}
