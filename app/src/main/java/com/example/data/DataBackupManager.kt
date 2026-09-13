package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupMetadata(
    val fileName: String,
    val filePath: String,
    val timestamp: Long,
    val formattedDate: String,
    val productCount: Int,
    val sktCount: Int = 0,
    val depoRecordCount: Int,
    val tourReportCount: Int,
    val adetselCount: Int = 0,
    val fileSizeFormatted: String,
    val isAutoBackup: Boolean,
    val tag: String = if (isAutoBackup) "Oto" else "Manuel"
) {
    val file: File get() = File(filePath)
}

data class BackupRestoreResult(
    val success: Boolean,
    val message: String,
    val productsRestored: Int = 0,
    val sktEntriesRestored: Int = 0,
    val depoRecordsRestored: Int = 0,
    val tourReportsRestored: Int = 0,
    val adetselRecordsRestored: Int = 0,
    val inspectionReportsRestored: Int = 0,
    val remindersRestored: Boolean = false
)

object DataBackupManager {
    const val CURRENT_DATA_SCHEMA_VERSION = 3
    private const val BACKUPS_DIR = "app_data_backups"

    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.forLanguageTag("tr-TR"))
    private val exportDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.forLanguageTag("tr-TR"))
    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("tr-TR"))

    fun getBackupDirectory(context: Context): File {
        val dir = File(context.filesDir, BACKUPS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getSettingsPrefs(context: Context): SharedPreferences {
        val sp = context.getSharedPreferences("skt_settings_prefs", Context.MODE_PRIVATE)
        val legacy = context.getSharedPreferences("a101_settings_prefs", Context.MODE_PRIVATE)
        if (sp.all.isEmpty() && legacy.all.isNotEmpty()) {
            val ed = sp.edit()
            legacy.all.forEach { (k, v) ->
                if (v is String) ed.putString(k, v)
            }
            ed.apply()
        }
        return sp
    }

    fun generateDefaultExportFileName(): String {
        val dateStr = exportDateFormat.format(Date())
        return "SKT_TAKIP_YEDEK_$dateStr.json"
    }

    /**
     * Creates a complete unified JSON backup of all application data:
     * - Ürünler ve eklenen SKT ve stok adetleri
     * - Takip sayfası kayıtları (İade & operasyon takibi)
     * - Adetsel sayfası kayıtları (Yapılacak & Yapıldı sayımlar)
     * - Tur ve denetim raporları
     */
    suspend fun createUnifiedBackupJson(
        context: Context,
        productDao: ProductDao,
        reportDao: InspectionReportDao,
        turDao: TurDao?,
        adetselDao: AdetselDao? = null
    ): String {
        val root = JSONObject()
        val now = System.currentTimeMillis()

        // 1. Metadata
        root.put("version", CURRENT_DATA_SCHEMA_VERSION)
        root.put("appName", "SKT & Mağaza Takip")
        root.put("exportDate", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date(now)))
        root.put("exportTimestamp", now)

        // 2. Products (Ürünler, SKT tarihleri ve stok adetleri)
        val products = productDao.getAllProductsList()
        val productsArray = JSONArray()
        for (p in products) {
            val pObj = JSONObject()
            pObj.put("id", p.id)
            pObj.put("barkod", p.barkod)
            pObj.put("urunKodu", p.urunKodu)
            pObj.put("urunAdi", p.urunAdi)
            pObj.put("kategori", p.kategori)
            pObj.put("sktTarihi", p.sktTarihi)
            pObj.put("stokAdedi", p.stokAdedi)
            pObj.put("eklenmeTarihi", p.eklenmeTarihi)
            pObj.put("isImportant", p.isImportant)
            pObj.put("sonKontrolTarihi", p.sonKontrolTarihi)
            if (p.fiyat != null) {
                pObj.put("fiyat", p.fiyat)
            }
            productsArray.put(pObj)
        }
        root.put("products", productsArray)

        // 3. Takip Sayfası Kayıtları (Depo İade / Operasyon Takip)
        val depoRecords = DepoIadeManager.loadRecords(context)
        val depoArray = JSONArray()
        for (d in depoRecords) {
            val dObj = JSONObject()
            dObj.put("id", d.id)
            dObj.put("urunAdi", d.urunAdi)
            if (d.irsaliyeGorselPath != null) dObj.put("irsaliyeGorselPath", d.irsaliyeGorselPath)
            dObj.put("iadeTarihi", d.iadeTarihi)
            dObj.put("iadeTarihiMillis", d.iadeTarihiMillis)
            dObj.put("redNedeni", d.redNedeni)
            dObj.put("aciklama", d.aciklama)
            dObj.put("oncelik", d.oncelik.name)
            dObj.put("durum", d.durum.name)
            dObj.put("hatirlatmaTarihi", d.hatirlatmaTarihi)
            if (d.hatirlatmaTarihiMillis != null) dObj.put("hatirlatmaTarihiMillis", d.hatirlatmaTarihiMillis)
            dObj.put("olusturmaTarihiMillis", d.olusturmaTarihiMillis)
            dObj.put("guncellemeTarihiMillis", d.guncellemeTarihiMillis)
            depoArray.put(dObj)
        }
        root.put("depoIadeKayitlari", depoArray)

        // 4. Adetsel Sayfası Kayıtları (Yapılacak ve Yapıldı Adetsel Sayım Kayıtları)
        val adetselList = adetselDao?.getAllAdetselKayitlariDirect() ?: emptyList()
        val adetselArray = JSONArray()
        for (a in adetselList) {
            val aObj = JSONObject()
            aObj.put("id", a.id)
            aObj.put("productId", a.productId)
            aObj.put("urunAdi", a.urunAdi)
            aObj.put("urunKodu", a.urunKodu)
            aObj.put("barkod", a.barkod)
            aObj.put("kategori", a.kategori)
            aObj.put("eklenmeTarihi", a.eklenmeTarihi)
            aObj.put("yapildiMi", a.yapildiMi)
            aObj.put("sayimSonucu", a.sayimSonucu)
            aObj.put("beklenenAdet", a.beklenenAdet)
            aObj.put("sayilanAdet", a.sayilanAdet)
            aObj.put("farkAdet", a.farkAdet)
            aObj.put("notlar", a.notlar)
            aObj.put("islemTarihi", a.islemTarihi)
            adetselArray.put(aObj)
        }
        root.put("adetselKayitlar", adetselArray)

        // 5. Tur Raporları
        val turRaporlari = turDao?.getAllTurRaporlariDirect() ?: emptyList()
        val turRaporlariArray = JSONArray()
        for (tr in turRaporlari) {
            val trObj = JSONObject()
            trObj.put("id", tr.id)
            trObj.put("turTarihi", tr.turTarihi)
            trObj.put("hedefReyon", tr.hedefReyon)
            trObj.put("toplamUrunSayisi", tr.toplamUrunSayisi)
            trObj.put("satilanUrunSayisi", tr.satilanUrunSayisi)
            trObj.put("toplamSatilanAdet", tr.toplamSatilanAdet)
            trObj.put("fireUrunSayisi", tr.fireUrunSayisi)
            trObj.put("toplamFireAdet", tr.toplamFireAdet)
            trObj.put("notrUrunSayisi", tr.notrUrunSayisi)
            trObj.put("tamamlandiMi", tr.tamamlandiMi)
            trObj.put("turSuresiSaniye", tr.turSuresiSaniye)
            trObj.put("toplamPuan", tr.toplamPuan)
            trObj.put("tahminiFireMaliyeti", tr.tahminiFireMaliyeti)
            trObj.put("enCokFireKategori", tr.enCokFireKategori)
            trObj.put("personelAdi", tr.personelAdi)
            turRaporlariArray.put(trObj)
        }
        root.put("turRaporlari", turRaporlariArray)

        // 6. Tur Kontrol Kayıtları
        val kontrolKayitlari = turDao?.getAllKontrolKayitlariDirect() ?: emptyList()
        val kontrolKayitlariArray = JSONArray()
        for (k in kontrolKayitlari) {
            val kObj = JSONObject()
            kObj.put("id", k.id)
            kObj.put("turId", k.turId)
            kObj.put("productId", k.productId)
            kObj.put("urunAdiSnapshot", k.urunAdiSnapshot)
            kObj.put("barkodSnapshot", k.barkodSnapshot)
            kObj.put("kategoriSnapshot", k.kategoriSnapshot)
            kObj.put("durum", k.durum)
            kObj.put("islemAdedi", k.islemAdedi)
            kObj.put("kontrolTarihi", k.kontrolTarihi)
            kObj.put("personelSnapshot", k.personelSnapshot)
            kontrolKayitlariArray.put(kObj)
        }
        root.put("turKontrolKayitlari", kontrolKayitlariArray)

        // 7. Reminders Notes & Settings
        val reminderPrefs = getSettingsPrefs(context)
        val reminderNotes = reminderPrefs.getString("store_reminders_notes", "") ?: ""
        root.put("remindersNotes", reminderNotes)

        return root.toString(2)
    }

    /**
     * Creates an automated or manual backup file in internal storage.
     */
    suspend fun saveAutoBackupToStorage(
        context: Context,
        productDao: ProductDao,
        reportDao: InspectionReportDao,
        turDao: TurDao?,
        adetselDao: AdetselDao? = null,
        tag: String = "auto"
    ): File? {
        return try {
            val jsonContent = createUnifiedBackupJson(context, productDao, reportDao, turDao, adetselDao)
            val dir = getBackupDirectory(context)
            val timestampStr = fileDateFormat.format(Date())
            val fileName = "backup_${tag}_$timestampStr.json"
            val file = File(dir, fileName)
            file.writeText(jsonContent, Charsets.UTF_8)

            // Rotate backups (keep latest 15 files)
            rotateBackups(dir, 15)

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Lists all existing backup files saved locally.
     */
    fun listLocalBackups(context: Context): List<BackupMetadata> {
        val dir = getBackupDirectory(context)
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".json") } ?: return emptyList()

        return files.sortedByDescending { it.lastModified() }.mapNotNull { file ->
            try {
                val content = file.readText(Charsets.UTF_8)
                val json = JSONObject(content)
                val timestamp = json.optLong("exportTimestamp", file.lastModified())
                val productsArr = json.optJSONArray("products") ?: json.optJSONArray("urunler")
                val depoArr = json.optJSONArray("depoIadeKayitlari") ?: json.optJSONArray("depoKayitlari")
                val turArr = json.optJSONArray("turRaporlari")
                val adetselArr = json.optJSONArray("adetselKayitlar") ?: json.optJSONArray("adetsel")

                var sktCount = 0
                if (productsArr != null) {
                    for (i in 0 until productsArr.length()) {
                        val pObj = productsArr.optJSONObject(i)
                        if (pObj != null && pObj.optLong("sktTarihi", 0L) > 0L) {
                            sktCount++
                        }
                    }
                }

                val sizeBytes = file.length()
                val sizeStr = when {
                    sizeBytes < 1024 -> "$sizeBytes B"
                    sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
                    else -> String.format(Locale.getDefault(), "%.1f MB", sizeBytes.toDouble() / (1024 * 1024))
                }

                BackupMetadata(
                    fileName = file.name,
                    filePath = file.absolutePath,
                    timestamp = timestamp,
                    formattedDate = displayDateFormat.format(Date(timestamp)),
                    productCount = productsArr?.length() ?: 0,
                    sktCount = sktCount,
                    depoRecordCount = depoArr?.length() ?: 0,
                    tourReportCount = turArr?.length() ?: 0,
                    adetselCount = adetselArr?.length() ?: 0,
                    fileSizeFormatted = sizeStr,
                    isAutoBackup = file.name.contains("auto") || file.name.contains("migration")
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Restores application data from a JSON string, supporting both legacy formats and current schema.
     */
    suspend fun restoreFromJson(
        context: Context,
        jsonString: String,
        productDao: ProductDao,
        reportDao: InspectionReportDao,
        turDao: TurDao?,
        adetselDao: AdetselDao? = null,
        mergeWithExisting: Boolean = true
    ): BackupRestoreResult {
        return try {
            val trimmed = jsonString.trim()
            if (trimmed.isEmpty()) {
                return BackupRestoreResult(false, "Yedek dosyası boş!")
            }

            var productsRestored = 0
            var depoRestored = 0
            var adetselRestored = 0
            var turReportsRestored = 0
            var inspectionRestored = 0
            var remindersRestored = false

            val parsedProducts = mutableListOf<Product>()
            val parsedDepoRecords = mutableListOf<DepoIadeKaydi>()
            val parsedAdetselKayitlar = mutableListOf<AdetselKayit>()
            val parsedTurRaporlari = mutableListOf<TurRaporu>()
            val parsedTurKayitlari = mutableListOf<TurKontrolKaydi>()
            val parsedReports = mutableListOf<InspectionReport>()

            if (trimmed.startsWith("[")) {
                // Legacy / Direct array of products
                val arr = JSONArray(trimmed)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    parseProductFromFlexibleJson(obj)?.let { parsedProducts.add(it) }
                }
            } else {
                val root = JSONObject(trimmed)

                // 1. Products array (handles "products", "urunler", "items", "data")
                val prodArray = root.optJSONArray("products")
                    ?: root.optJSONArray("urunler")
                    ?: root.optJSONArray("items")
                    ?: root.optJSONArray("data")

                if (prodArray != null) {
                    for (i in 0 until prodArray.length()) {
                        val pObj = prodArray.getJSONObject(i)
                        parseProductFromFlexibleJson(pObj)?.let { parsedProducts.add(it) }
                    }
                }

                // 2. Takip Sayfası Kayıtları (handles "depoIadeKayitlari", "depoKayitlari", "iadeKayitlari", "takipKayitlari")
                val depoArr = root.optJSONArray("depoIadeKayitlari")
                    ?: root.optJSONArray("depoKayitlari")
                    ?: root.optJSONArray("iadeKayitlari")
                    ?: root.optJSONArray("takipKayitlari")

                if (depoArr != null) {
                    for (i in 0 until depoArr.length()) {
                        val dObj = depoArr.getJSONObject(i)
                        parseDepoRecordFromFlexibleJson(dObj)?.let { parsedDepoRecords.add(it) }
                    }
                }

                // 3. Adetsel Sayfası Kayıtları (handles "adetselKayitlar", "adetsel", "adetselSayimlar")
                val adetselArr = root.optJSONArray("adetselKayitlar")
                    ?: root.optJSONArray("adetsel")
                    ?: root.optJSONArray("adetselSayimlar")

                if (adetselArr != null) {
                    for (i in 0 until adetselArr.length()) {
                        val aObj = adetselArr.getJSONObject(i)
                        parseAdetselKayitFromJson(aObj)?.let { parsedAdetselKayitlar.add(it) }
                    }
                }

                // 4. Tur Raporları
                val turArr = root.optJSONArray("turRaporlari")
                if (turArr != null) {
                    for (i in 0 until turArr.length()) {
                        val tObj = turArr.getJSONObject(i)
                        parseTurRaporuFromJson(tObj)?.let { parsedTurRaporlari.add(it) }
                    }
                }

                // 5. Tur Kontrol Kayıtları
                val kontrolArr = root.optJSONArray("turKontrolKayitlari")
                if (kontrolArr != null) {
                    for (i in 0 until kontrolArr.length()) {
                        val kObj = kontrolArr.getJSONObject(i)
                        parseTurKontrolKaydiFromJson(kObj)?.let { parsedTurKayitlari.add(it) }
                    }
                }

                // 6. Inspection Reports
                val repArr = root.optJSONArray("inspectionReports") ?: root.optJSONArray("reports")
                if (repArr != null) {
                    for (i in 0 until repArr.length()) {
                        val rObj = repArr.getJSONObject(i)
                        parseInspectionReportFromJson(rObj)?.let { parsedReports.add(it) }
                    }
                }

                // 7. Reminders Notes
                val notes = root.optString("remindersNotes", "")
                if (notes.isNotBlank()) {
                    val reminderPrefs = getSettingsPrefs(context)
                    reminderPrefs.edit().putString("store_reminders_notes", notes).apply()
                    remindersRestored = true
                }
            }

            // Perform Save / Merge
            if (!mergeWithExisting) {
                productDao.deleteAllProducts()
                turDao?.deleteAllTurRaporlari()
                turDao?.deleteAllKontrolKayitlari()
                reportDao.deleteAllReports()
                adetselDao?.deleteAllAdetselKayitlar()
                DepoIadeManager.saveRecords(context, emptyList())
            }

            // 1. Ürünler ve SKT / Adetlerini Yükle
            if (parsedProducts.isNotEmpty()) {
                if (!mergeWithExisting) {
                    productDao.insertAll(parsedProducts.map { it.copy(id = 0) })
                    productsRestored = parsedProducts.size
                } else {
                    val existing = productDao.getAllProductsList()
                    val existingMap = existing.associateBy {
                        when {
                            it.barkod.isNotBlank() -> "B:${it.barkod.trim()}_${it.sktTarihi}"
                            it.urunKodu.isNotBlank() -> "K:${it.urunKodu.trim()}_${it.sktTarihi}"
                            else -> "N:${it.urunAdi.trim().lowercase(Locale.forLanguageTag("tr-TR"))}_${it.sktTarihi}"
                        }
                    }

                    for (prod in parsedProducts) {
                        val key = when {
                            prod.barkod.isNotBlank() -> "B:${prod.barkod.trim()}_${prod.sktTarihi}"
                            prod.urunKodu.isNotBlank() -> "K:${prod.urunKodu.trim()}_${prod.sktTarihi}"
                            else -> "N:${prod.urunAdi.trim().lowercase(Locale.forLanguageTag("tr-TR"))}_${prod.sktTarihi}"
                        }
                        val match = existingMap[key]
                        if (match != null) {
                            productDao.updateProduct(prod.copy(id = match.id))
                        } else {
                            productDao.insertProduct(prod.copy(id = 0))
                        }
                        productsRestored++
                    }
                }
            }

            // 2. Takip Sayfası Kayıtlarını Yükle
            if (parsedDepoRecords.isNotEmpty()) {
                if (!mergeWithExisting) {
                    DepoIadeManager.saveRecords(context, parsedDepoRecords)
                    depoRestored = parsedDepoRecords.size
                } else {
                    val existingDepo = DepoIadeManager.loadRecords(context).toMutableList()
                    val existingIds = existingDepo.map { it.id }.toSet()
                    for (rec in parsedDepoRecords) {
                        if (existingIds.contains(rec.id)) {
                            val idx = existingDepo.indexOfFirst { it.id == rec.id }
                            if (idx >= 0) existingDepo[idx] = rec
                        } else {
                            existingDepo.add(rec)
                        }
                        depoRestored++
                    }
                    DepoIadeManager.saveRecords(context, existingDepo)
                }
            }

            // 3. Adetsel Sayfası Kayıtlarını Yükle
            if (adetselDao != null && parsedAdetselKayitlar.isNotEmpty()) {
                if (!mergeWithExisting) {
                    adetselDao.insertAdetselKayitlarBatch(parsedAdetselKayitlar.map { it.copy(id = 0) })
                    adetselRestored = parsedAdetselKayitlar.size
                } else {
                    val existingAdetsel = adetselDao.getAllAdetselKayitlariDirect()
                    val existingKeys = existingAdetsel.map {
                        "${it.barkod.trim()}_${it.urunKodu.trim()}_${it.yapildiMi}_${it.islemTarihi}"
                    }.toSet()

                    val toInsert = mutableListOf<AdetselKayit>()
                    for (ak in parsedAdetselKayitlar) {
                        val key = "${ak.barkod.trim()}_${ak.urunKodu.trim()}_${ak.yapildiMi}_${ak.islemTarihi}"
                        if (!existingKeys.contains(key)) {
                            toInsert.add(ak.copy(id = 0))
                            adetselRestored++
                        }
                    }
                    if (toInsert.isNotEmpty()) {
                        adetselDao.insertAdetselKayitlarBatch(toInsert)
                    }
                }
            }

            // 4. Tur Raporları & Kontrol Kayıtları
            if (turDao != null) {
                for (tr in parsedTurRaporlari) {
                    turDao.insertTurRaporu(tr.copy(id = 0))
                    turReportsRestored++
                }
                if (parsedTurKayitlari.isNotEmpty()) {
                    turDao.insertKontrolKayitlari(parsedTurKayitlari.map { it.copy(id = 0) })
                }
            }

            // 5. Inspection Reports
            for (ir in parsedReports) {
                reportDao.insertReport(ir.copy(id = 0))
                inspectionRestored++
            }

            val sktWithDateCount = parsedProducts.count { it.sktTarihi > 0L }
            val message = buildString {
                append("Yedek başarıyla geri yüklendi!\n")
                append("• $productsRestored ürün")
                if (sktWithDateCount > 0) {
                    append(" ($sktWithDateCount SKT & adet kaydı)")
                }
                append("\n")
                append("• $depoRestored takip kaydı\n")
                append("• $adetselRestored adetsel sayım kaydı aktarıldı.")
                if (turReportsRestored > 0) {
                    append("\n• $turReportsRestored tur raporu")
                }
            }

            BackupRestoreResult(
                success = true,
                message = message,
                productsRestored = productsRestored,
                sktEntriesRestored = sktWithDateCount,
                depoRecordsRestored = depoRestored,
                tourReportsRestored = turReportsRestored,
                adetselRecordsRestored = adetselRestored,
                inspectionReportsRestored = inspectionRestored,
                remindersRestored = remindersRestored
            )
        } catch (e: Exception) {
            e.printStackTrace()
            BackupRestoreResult(
                success = false,
                message = "Yedek geri yüklenirken hata oluştu: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Safe schema evolution parser: extracts product from diverse key names & missing fields.
     */
    private fun parseProductFromFlexibleJson(obj: JSONObject): Product? {
        val barkod = obj.optString("barkod", "")
            .ifBlank { obj.optString("barcode", "") }
            .ifBlank { obj.optString("ean", "") }
            .ifBlank { obj.optString("gtin", "") }
            .trim()

        val urunAdi = obj.optString("urunAdi", "")
            .ifBlank { obj.optString("urun_adi", "") }
            .ifBlank { obj.optString("name", "") }
            .ifBlank { obj.optString("productName", "") }
            .ifBlank { obj.optString("title", "") }
            .trim()

        if (barkod.isBlank() && urunAdi.isBlank()) return null

        val urunKodu = obj.optString("urunKodu", "")
            .ifBlank { obj.optString("urun_kodu", "") }
            .ifBlank { obj.optString("code", "") }
            .ifBlank { obj.optString("itemCode", "") }
            .trim()
            .ifBlank { "0000" }

        val kategori = obj.optString("kategori", "")
            .ifBlank { obj.optString("category", "") }
            .ifBlank { "Genel" }
            .trim()

        val rawSkt = obj.optLong("sktTarihi", 0L)
        val sktTarihi = if (rawSkt > 0L) rawSkt else {
            val exp = obj.optLong("expiryDate", 0L)
            if (exp > 0L) exp else {
                val expStr = obj.optString("expiryDate", "").ifBlank { obj.optString("skt", "") }
                parseDateStringToMillis(expStr)
            }
        }

        val stokAdedi = obj.optInt("stokAdedi", -1).let {
            if (it >= 0) it else obj.optInt("quantity", -1).let { q ->
                if (q >= 0) q else obj.optInt("stok", 1)
            }
        }

        val eklenmeTarihi = obj.optLong("eklenmeTarihi", System.currentTimeMillis())
        val isImportant = obj.optBoolean("isImportant", false)
        val sonKontrolTarihi = obj.optLong("sonKontrolTarihi", 0L)
        val fiyat = if (obj.has("fiyat") && !obj.isNull("fiyat")) {
            obj.optDouble("fiyat")
        } else if (obj.has("price") && !obj.isNull("price")) {
            obj.optDouble("price")
        } else null

        val rawProduct = Product(
            id = 0,
            barkod = barkod.ifBlank { urunKodu },
            urunKodu = urunKodu,
            urunAdi = urunAdi.uppercase(Locale.forLanguageTag("tr-TR")).ifBlank { "İSİMSİZ ÜRÜN" },
            kategori = kategori,
            sktTarihi = sktTarihi,
            stokAdedi = stokAdedi,
            eklenmeTarihi = eklenmeTarihi,
            isImportant = isImportant,
            sonKontrolTarihi = sonKontrolTarihi,
            fiyat = fiyat
        )

        return com.example.util.ProductDataHealer.autoHealProduct(rawProduct)
    }

    private fun parseDepoRecordFromFlexibleJson(obj: JSONObject): DepoIadeKaydi? {
        val urunAdi = obj.optString("urunAdi", "").ifBlank { obj.optString("name", "") }.trim()
        if (urunAdi.isBlank()) return null

        val id = obj.optString("id", System.currentTimeMillis().toString())
        val irsaliyeGorselPath = obj.optString("irsaliyeGorselPath", "").takeIf { it.isNotBlank() }
        val iadeTarihi = obj.optString("iadeTarihi", DepoIadeManager.getTodayDateString())
        val iadeTarihiMillis = obj.optLong("iadeTarihiMillis", System.currentTimeMillis())
        val redNedeni = obj.optString("redNedeni", IadeRedNedeni.DEPO_KABUL_ETMEDI.displayName)
        val aciklama = obj.optString("aciklama", "").ifBlank { obj.optString("notes", "") }

        val oncelik = try {
            IadeOncelik.valueOf(obj.optString("oncelik", "NORMAL"))
        } catch (e: Exception) {
            IadeOncelik.NORMAL
        }

        val rawDurum = obj.optString("durum", "DEVAM_EDIYOR")
        val durum = try {
            IadeDurumu.valueOf(rawDurum)
        } catch (e: Exception) {
            when (rawDurum) {
                "COZULDU" -> IadeDurumu.ONAYLANDI
                "REDDEDILDI" -> IadeDurumu.REDDEDILDI
                else -> IadeDurumu.DEVAM_EDIYOR
            }
        }

        val hatirlatmaTarihi = obj.optString("hatirlatmaTarihi", "")
        val hatirlatmaTarihiMillis = if (obj.has("hatirlatmaTarihiMillis") && !obj.isNull("hatirlatmaTarihiMillis")) {
            obj.optLong("hatirlatmaTarihiMillis")
        } else null

        val olusturmaTarihiMillis = obj.optLong("olusturmaTarihiMillis", System.currentTimeMillis())
        val guncellemeTarihiMillis = obj.optLong("guncellemeTarihiMillis", System.currentTimeMillis())

        return DepoIadeKaydi(
            id = id,
            urunAdi = urunAdi,
            irsaliyeGorselPath = irsaliyeGorselPath,
            iadeTarihi = iadeTarihi,
            iadeTarihiMillis = iadeTarihiMillis,
            redNedeni = redNedeni,
            aciklama = aciklama,
            oncelik = oncelik,
            durum = durum,
            hatirlatmaTarihi = hatirlatmaTarihi,
            hatirlatmaTarihiMillis = hatirlatmaTarihiMillis,
            olusturmaTarihiMillis = olusturmaTarihiMillis,
            guncellemeTarihiMillis = guncellemeTarihiMillis
        )
    }

    private fun parseTurRaporuFromJson(obj: JSONObject): TurRaporu? {
        val hedefReyon = obj.optString("hedefReyon", "").ifBlank { "Dolap Ürünleri" }
        return TurRaporu(
            id = 0,
            turTarihi = obj.optLong("turTarihi", System.currentTimeMillis()),
            hedefReyon = hedefReyon,
            toplamUrunSayisi = obj.optInt("toplamUrunSayisi", 0),
            satilanUrunSayisi = obj.optInt("satilanUrunSayisi", 0),
            toplamSatilanAdet = obj.optInt("toplamSatilanAdet", 0),
            fireUrunSayisi = obj.optInt("fireUrunSayisi", 0),
            toplamFireAdet = obj.optInt("toplamFireAdet", 0),
            notrUrunSayisi = obj.optInt("notrUrunSayisi", 0),
            tamamlandiMi = obj.optBoolean("tamamlandiMi", true),
            turSuresiSaniye = obj.optLong("turSuresiSaniye", 0L),
            toplamPuan = obj.optInt("toplamPuan", 0),
            tahminiFireMaliyeti = obj.optDouble("tahminiFireMaliyeti", 0.0),
            enCokFireKategori = obj.optString("enCokFireKategori", ""),
            personelAdi = obj.optString("personelAdi", "")
        )
    }

    private fun parseTurKontrolKaydiFromJson(obj: JSONObject): TurKontrolKaydi? {
        val urunAdi = obj.optString("urunAdiSnapshot", "").trim()
        if (urunAdi.isBlank()) return null
        return TurKontrolKaydi(
            id = 0,
            turId = obj.optInt("turId", 0),
            productId = obj.optInt("productId", 0),
            urunAdiSnapshot = urunAdi,
            barkodSnapshot = obj.optString("barkodSnapshot", ""),
            kategoriSnapshot = obj.optString("kategoriSnapshot", ""),
            durum = obj.optString("durum", "NOTR"),
            islemAdedi = obj.optInt("islemAdedi", 0),
            kontrolTarihi = obj.optLong("kontrolTarihi", System.currentTimeMillis()),
            personelSnapshot = obj.optString("personelSnapshot", "")
        )
    }

    private fun parseInspectionReportFromJson(obj: JSONObject): InspectionReport? {
        val reyon = obj.optString("reyonAdi", "").ifBlank { "Dolap Ürünleri" }
        return InspectionReport(
            id = 0,
            tarih = obj.optLong("tarih", System.currentTimeMillis()),
            reyonAdi = reyon,
            tarananUrunSayisi = obj.optInt("tarananUrunSayisi", 0),
            suresiGecenSayisi = obj.optInt("suresiGecenSayisi", 0),
            kritikUrunSayisi = obj.optInt("kritikUrunSayisi", 0),
            fireTutari = obj.optDouble("fireTutari", 0.0)
        )
    }

    private fun parseAdetselKayitFromJson(obj: JSONObject): AdetselKayit? {
        val urunAdi = obj.optString("urunAdi", "")
            .ifBlank { obj.optString("name", "") }
            .ifBlank { obj.optString("urun_adi", "") }
            .trim()
        val barkod = obj.optString("barkod", "")
            .ifBlank { obj.optString("barcode", "") }
            .trim()
        val urunKodu = obj.optString("urunKodu", "")
            .ifBlank { obj.optString("code", "") }
            .ifBlank { obj.optString("urun_kodu", "") }
            .trim()

        if (urunAdi.isBlank() && barkod.isBlank() && urunKodu.isBlank()) return null

        return AdetselKayit(
            id = 0,
            productId = obj.optInt("productId", 0),
            urunAdi = urunAdi.ifBlank { "İSİMSİZ ÜRÜN" },
            urunKodu = urunKodu,
            barkod = barkod,
            kategori = obj.optString("kategori", "Genel").ifBlank { "Genel" },
            eklenmeTarihi = obj.optLong("eklenmeTarihi", System.currentTimeMillis()),
            yapildiMi = obj.optBoolean("yapildiMi", false),
            sayimSonucu = obj.optString("sayimSonucu", ""),
            beklenenAdet = obj.optInt("beklenenAdet", 0),
            sayilanAdet = obj.optInt("sayilanAdet", 0),
            farkAdet = obj.optInt("farkAdet", 0),
            notlar = obj.optString("notlar", ""),
            islemTarihi = obj.optLong("islemTarihi", 0L)
        )
    }

    private fun parseDateStringToMillis(dateStr: String): Long {
        if (dateStr.isBlank()) return 0L
        val formats = listOf("dd/MM/yyyy", "dd.MM.yyyy", "yyyy-MM-dd", "dd-MM-yyyy")
        for (f in formats) {
            try {
                val sdf = SimpleDateFormat(f, Locale.getDefault())
                val d = sdf.parse(dateStr)
                if (d != null) return d.time
            } catch (e: Exception) {
                // Try next format
            }
        }
        return 0L
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
        val pre = "KMGTPE"[exp - 1]
        return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
    }

    private fun rotateBackups(dir: File, maxFiles: Int) {
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".json") } ?: return
        if (files.size > maxFiles) {
            val sorted = files.sortedBy { it.lastModified() }
            val toDeleteCount = files.size - maxFiles
            for (i in 0 until toDeleteCount) {
                sorted[i].delete()
            }
        }
    }
}
