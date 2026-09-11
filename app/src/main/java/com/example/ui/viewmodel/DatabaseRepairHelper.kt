package com.example.ui.viewmodel

import com.example.data.Product
import com.example.data.ProductRepository
import com.example.util.ProductDataHealer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DatabaseRepairHelper {

    private fun isCorruptedText(text: String): Boolean {
        if (text.isEmpty()) return false
        if (text.contains("\uFFFD")) return true
        if (text.contains("_rels") || text.contains("[Content_Types]") || text.contains("<xml") ||
            text.contains("PK\u0003") || text.contains("xl/workbooks") || text.contains("Root Entry")
        ) return true
        if (text.any { it.code in 0..8 || it.code in 14..31 || it.code == 127 }) return true
        return false
    }

    suspend fun repairDatabase(
        repository: ProductRepository,
        onResult: (Int, String) -> Unit
    ) {
        var fixedCount = 0
        try {
            val allProds = repository.getProductListDirect()
            val updatedProds = mutableListOf<Product>()
            val seenKeys = mutableSetOf<String>()
            val itemsToDelete = mutableListOf<Product>()

            for (p in allProds) {
                // Check if product is binary garbage / corrupted from illegal file import
                if (isCorruptedText(p.barkod) || isCorruptedText(p.urunKodu) || isCorruptedText(p.urunAdi)) {
                    itemsToDelete.add(p)
                    fixedCount++
                    continue
                }

                // 1. Run Intelligent Auto-Heal for shifted/swapped columns
                val healed = ProductDataHealer.autoHealProduct(p)
                var isModified = (healed.barkod != p.barkod || healed.urunKodu != p.urunKodu || healed.urunAdi != p.urunAdi || healed.kategori != p.kategori)
                val trimmedName = healed.urunAdi.trim().replace("\\s+".toRegex(), " ")
                val cleanBarcode = healed.barkod.trim().filter { it.isDigit() }
                val cleanUrunKodu = healed.urunKodu.trim().filter { it.isLetterOrDigit() }
                var newStok = healed.stokAdedi

                if (trimmedName != healed.urunAdi) {
                    isModified = true
                }
                if (cleanBarcode != healed.barkod && cleanBarcode.isNotEmpty()) {
                    isModified = true
                }
                if (cleanUrunKodu != healed.urunKodu) {
                    isModified = true
                }
                if (newStok < 0) {
                    newStok = 0
                    isModified = true
                }

                if (isModified) {
                    fixedCount++
                }

                val effectiveBarcode = if (cleanBarcode.isNotEmpty()) cleanBarcode else healed.barkod.trim()
                val identifier = if (effectiveBarcode.isNotEmpty()) effectiveBarcode else trimmedName.lowercase()
                val formattedSkt = healed.getFormattedSkt()

                // Deduplication key MUST include name and product code
                val dupKey = "${identifier}_${cleanUrunKodu.lowercase()}_${trimmedName.lowercase()}-$formattedSkt"
                if (seenKeys.contains(dupKey)) {
                    itemsToDelete.add(p)
                    fixedCount++
                } else {
                    seenKeys.add(dupKey)
                    if (isModified) {
                        updatedProds.add(
                            healed.copy(
                                urunAdi = trimmedName,
                                barkod = effectiveBarcode,
                                urunKodu = cleanUrunKodu,
                                stokAdedi = newStok
                            )
                        )
                    }
                }
            }

            for (item in itemsToDelete) {
                repository.deleteProduct(item)
            }
            for (mod in updatedProds) {
                repository.insertOrUpdateProduct(mod)
            }

            val summary = if (fixedCount > 0) {
                "🔍 Akıllı Tarama & Onarım Tamamlandı!\n\n• Toplam $fixedCount adet üründeki kolon kaymaları (Barkod, Ürün Kodu, Ürün Adı, Kategori), tutarsızlıklar ve mükerrer kayıtlar otomatik olarak düzeltildi.\n• Tüm ürün bilgileri ve kategorileri başarıyla optimize edildi."
            } else {
                "✅ Mükemmel! Veritabanınızdaki tüm ürün bilgileri (Barkod, Ürün Kodu, İsim, Kategori) hatasız ve tam uyumlu."
            }

            withContext(Dispatchers.Main) {
                onResult(fixedCount, summary)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult(0, "Hata taraması sırasında bir sorun oluştu: ${e.localizedMessage}")
            }
        }
    }
}
