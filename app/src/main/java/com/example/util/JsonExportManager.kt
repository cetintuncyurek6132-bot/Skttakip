package com.example.util

import android.content.Context
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object JsonExportManager {

    /**
     * Creates a complete unified JSON export containing all main datasets:
     * - products (Ürünler, stok adetleri ve SKT'ler)
     * - stockLogs (Stok hareketleri geçmişi)
     * - iadeTakipList / iadeList (İade & Depo red takip kayıtları)
     * - sayimList (Adetsel sayım yapılacak ve yapıldı kayıtları)
     */
    suspend fun exportFullBackup(
        context: Context,
        productDao: ProductDao,
        adetselDao: AdetselDao? = null,
        stockLogDao: StockLogDao? = null
    ): String = withContext(Dispatchers.IO) {
        val products = productDao.getAllProductsList()
        val iadeList = DepoIadeManager.loadRecords(context)
        val sayimList = adetselDao?.getAllAdetselKayitlariDirect() ?: emptyList()
        val stockLogs = stockLogDao?.getAllLogsList() ?: emptyList()
        val now = System.currentTimeMillis()

        val fullBackup = FullBackupData(
            version = 2,
            exportTimestamp = now,
            products = products,
            stockLogs = stockLogs,
            iadeList = iadeList,
            sayimList = sayimList,
            backupDate = now,
            iadeTakipList = iadeList
        )

        toJsonString(fullBackup)
    }

    fun toJsonString(data: FullBackupData): String {
        val root = JSONObject()
        root.put("version", data.version)
        root.put("appName", "SKT & Mağaza Takip")
        root.put("backupDate", data.backupDate)
        root.put("exportTimestamp", data.exportTimestamp)

        // 1. Products
        val prodArray = JSONArray()
        data.products.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("barkod", p.barkod)
            obj.put("urunKodu", p.urunKodu)
            obj.put("urunAdi", p.urunAdi)
            obj.put("kategori", p.kategori)
            obj.put("sktTarihi", p.sktTarihi)
            obj.put("stokAdedi", p.stokAdedi)
            obj.put("eklenmeTarihi", p.eklenmeTarihi)
            obj.put("isImportant", p.isImportant)
            obj.put("sonKontrolTarihi", p.sonKontrolTarihi)
            if (p.fiyat != null) obj.put("fiyat", p.fiyat)
            prodArray.put(obj)
        }
        root.put("products", prodArray)
        root.put("urunler", prodArray)

        // 2. Stock Logs
        val stockLogArray = JSONArray()
        data.stockLogs.forEach { log ->
            val obj = JSONObject()
            obj.put("id", log.id)
            obj.put("barcode", log.barcode)
            obj.put("productName", log.productName)
            obj.put("actionType", log.actionType)
            obj.put("quantity", log.quantity)
            if (log.sktDate != null) obj.put("sktDate", log.sktDate)
            if (log.daysRemaining != null) obj.put("daysRemaining", log.daysRemaining)
            obj.put("timestamp", log.timestamp)
            stockLogArray.put(obj)
        }
        root.put("stockLogs", stockLogArray)

        // 3. İade Takip
        val iadeArray = JSONArray()
        val listToExport = data.effectiveIadeList
        listToExport.forEach { i ->
            val obj = JSONObject()
            obj.put("id", i.id)
            obj.put("urunAdi", i.urunAdi)
            if (!i.urunKodu.isNullOrBlank()) obj.put("urunKodu", i.urunKodu)
            if (!i.irsaliyeGorselPath.isNullOrBlank()) obj.put("irsaliyeGorselPath", i.irsaliyeGorselPath)
            obj.put("iadeTarihi", i.iadeTarihi)
            obj.put("iadeTarihiMillis", i.iadeTarihiMillis)
            obj.put("redNedeni", i.redNedeni)
            obj.put("aciklama", i.aciklama)
            obj.put("oncelik", i.oncelik.name)
            obj.put("durum", i.durum.name)
            obj.put("hatirlatmaTarihi", i.hatirlatmaTarihi)
            if (i.hatirlatmaTarihiMillis != null) obj.put("hatirlatmaTarihiMillis", i.hatirlatmaTarihiMillis)
            obj.put("olusturmaTarihiMillis", i.olusturmaTarihiMillis)
            obj.put("guncellemeTarihiMillis", i.guncellemeTarihiMillis)
            iadeArray.put(obj)
        }
        root.put("iadeTakipList", iadeArray)
        root.put("iadeList", iadeArray)
        root.put("depoIadeKayitlari", iadeArray)

        // 4. Adetsel Sayım
        val sayimArray = JSONArray()
        data.sayimList.forEach { s ->
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("productId", s.productId)
            obj.put("urunAdi", s.urunAdi)
            obj.put("urunKodu", s.urunKodu)
            obj.put("barkod", s.barkod)
            obj.put("kategori", s.kategori)
            obj.put("eklenmeTarihi", s.eklenmeTarihi)
            obj.put("yapildiMi", s.yapildiMi)
            obj.put("sayimSonucu", s.sayimSonucu)
            obj.put("beklenenAdet", s.beklenenAdet)
            obj.put("sayilanAdet", s.sayilanAdet)
            obj.put("farkAdet", s.farkAdet)
            obj.put("notlar", s.notlar)
            obj.put("islemTarihi", s.islemTarihi)
            sayimArray.put(obj)
        }
        root.put("sayimList", sayimArray)
        root.put("adetselKayitlar", sayimArray)

        return root.toString(2)
    }

    suspend fun restoreFullBackup(
        context: Context,
        jsonString: String,
        productDao: ProductDao,
        reportDao: InspectionReportDao,
        adetselDao: AdetselDao? = null,
        stockLogDao: StockLogDao? = null,
        mergeWithExisting: Boolean = true
    ): BackupRestoreResult {
        return DataBackupManager.restoreFromJson(
            context = context,
            jsonString = jsonString,
            productDao = productDao,
            reportDao = reportDao,
            adetselDao = adetselDao,
            stockLogDao = stockLogDao,
            mergeWithExisting = mergeWithExisting
        )
    }
}
