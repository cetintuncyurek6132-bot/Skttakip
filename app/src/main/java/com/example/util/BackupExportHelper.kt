package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.AdetselKayit
import com.example.data.DepoIadeKaydi
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupExportHelper {

    fun generateBackupFileName(prefix: String = "Skttakip_Yedek", extension: String = "json"): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        return "${prefix}_$timeStamp.$extension"
    }

    /**
     * Saves text/json/csv content directly to device's public Downloads directory.
     * Uses MediaStore on Android 10+ (API 29+) and direct File in Environment.DIRECTORY_DOWNLOADS on older devices.
     */
    fun saveFileToDownloads(
        context: Context,
        content: String,
        fileName: String,
        mimeType: String = "text/plain"
    ): Result<String> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return Result.failure(Exception("Downloads klasöründe dosya oluşturulamadı."))

                resolver.openOutputStream(uri, "wt")?.use { os ->
                    os.write(content.toByteArray(Charsets.UTF_8))
                    os.flush()
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                Result.success("İndirilenler (Downloads)/$fileName")
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val targetFile = File(downloadsDir, fileName)
                targetFile.writeText(content, Charsets.UTF_8)
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf(mimeType),
                    null
                )
                Result.success("İndirilenler/$fileName")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun saveJsonToDownloads(context: Context, jsonString: String, fileName: String): Result<String> {
        return saveFileToDownloads(context, jsonString, fileName, "application/json")
    }

    /**
     * Writes string content to a Uri obtained from ActivityResultContracts.CreateDocument.
     */
    fun writeJsonToUri(context: Context, uri: Uri, jsonString: String): Result<Unit> {
        return try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { os ->
                os.write(jsonString.toByteArray(Charsets.UTF_8))
                os.flush()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Shares a file via Android Share Chooser (FileProvider).
     */
    fun shareFile(
        context: Context,
        content: String,
        fileName: String,
        mimeType: String = "text/plain",
        subject: String = "Veri Dışa Aktarımı",
        bodyText: String = ""
    ): Result<Unit> {
        return try {
            val cacheFile = File(context.cacheDir, fileName)
            cacheFile.writeText(content, Charsets.UTF_8)

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                if (bodyText.isNotBlank()) {
                    putExtra(Intent.EXTRA_TEXT, bodyText)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Dosyayı Paylaş / Dışa Aktar")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun shareJsonBackup(context: Context, jsonString: String, fileName: String): Result<Unit> {
        return shareFile(
            context = context,
            content = jsonString,
            fileName = fileName,
            mimeType = "application/json",
            subject = "SKT Takip Tam Sistem Yedeği ($fileName)",
            bodyText = "SKT Takip uygulaması tam veri yedeği (Ürünler, SKT'ler, Fiyatlar, Takip ve Sayımlar)."
        )
    }

    // ==========================================
    // TÜM ÜRÜNLER & STOK EXPORT METOTLARI
    // ==========================================

    fun exportProductsToJson(products: List<com.example.data.Product>): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("module", "products_only")
        root.put("appName", "SKT & Mağaza Takip")
        root.put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))
        root.put("totalProducts", products.size)

        val array = JSONArray()
        products.forEach { p ->
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
            array.put(obj)
        }
        root.put("products", array)
        root.put("urunler", array)
        return root.toString(2)
    }

    fun exportProductsToCsv(products: List<com.example.data.Product>): String {
        val sb = StringBuilder()
        // UTF-8 BOM for Excel compatibility with Turkish characters
        sb.append('\uFEFF')
        sb.append("Sıra;Barkod;Ürün Kodu;Ürün Adı;Kategori;Fiyat;SKT Tarihi;Stok Adet\n")

        products.forEachIndexed { index, p ->
            val barkod = p.barkod.replace("\"", "\"\"")
            val kod = p.urunKodu.replace("\"", "\"\"")
            val ad = p.urunAdi.replace("\"", "\"\"")
            val kat = p.kategori.replace("\"", "\"\"")
            val fiyat = p.fiyat ?: 0.0
            val skt = p.getFormattedSkt().replace("\"", "\"\"")
            val stok = p.stokAdedi

            sb.append("${index + 1};")
            sb.append("\"$barkod\";")
            sb.append("\"$kod\";")
            sb.append("\"$ad\";")
            sb.append("\"$kat\";")
            sb.append("$fiyat;")
            sb.append("\"$skt\";")
            sb.append("$stok\n")
        }
        return sb.toString()
    }

    // ==========================================
    // İADE VE DEPO TAKİP EXPORT METOTLARI
    // ==========================================

    fun exportIadeTakipToCsv(records: List<DepoIadeKaydi>): String {
        val sb = StringBuilder()
        // UTF-8 BOM for Excel compatibility with Turkish characters
        sb.append('\uFEFF')
        sb.append("Sıra;Ürün Adı;Ürün Kodu;Durum;Öncelik;İade Tarihi;Red / İade Nedeni;Açıklama;Takip Tarihi;İrsaliye Görseli\n")

        records.forEachIndexed { index, r ->
            val urunAdiEscaped = r.urunAdi.replace("\"", "\"\"")
            val urunKoduEscaped = (r.urunKodu ?: "").replace("\"", "\"\"")
            val durumEscaped = r.durum.displayName.replace("\"", "\"\"")
            val oncelikEscaped = r.oncelik.displayName.replace("\"", "\"\"")
            val iadeTarihiEscaped = r.iadeTarihi.replace("\"", "\"\"")
            val redNedeniEscaped = r.redNedeni.replace("\"", "\"\"")
            val aciklamaEscaped = r.aciklama.replace("\"", "\"\"")
            val takipTarihiEscaped = r.hatirlatmaTarihi.replace("\"", "\"\"")
            val hasGorsel = if (!r.irsaliyeGorselPath.isNullOrBlank()) "Mevcut" else "Yok"

            sb.append("${index + 1};")
            sb.append("\"$urunAdiEscaped\";")
            sb.append("\"$urunKoduEscaped\";")
            sb.append("\"$durumEscaped\";")
            sb.append("\"$oncelikEscaped\";")
            sb.append("\"$iadeTarihiEscaped\";")
            sb.append("\"$redNedeniEscaped\";")
            sb.append("\"$aciklamaEscaped\";")
            sb.append("\"$takipTarihiEscaped\";")
            sb.append("\"$hasGorsel\"\n")
        }
        return sb.toString()
    }

    fun exportIadeTakipToJson(records: List<DepoIadeKaydi>): String {
        val root = JSONObject()
        root.put("module", "iade_depo_takip")
        root.put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))
        root.put("totalRecords", records.size)

        val array = JSONArray()
        records.forEach { r ->
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("urunAdi", r.urunAdi)
            if (!r.urunKodu.isNullOrBlank()) obj.put("urunKodu", r.urunKodu)
            obj.put("durum", r.durum.name)
            obj.put("durumText", r.durum.displayName)
            obj.put("oncelik", r.oncelik.name)
            obj.put("oncelikText", r.oncelik.displayName)
            obj.put("iadeTarihi", r.iadeTarihi)
            obj.put("iadeTarihiMillis", r.iadeTarihiMillis)
            obj.put("redNedeni", r.redNedeni)
            obj.put("aciklama", r.aciklama)
            obj.put("hatirlatmaTarihi", r.hatirlatmaTarihi)
            if (r.hatirlatmaTarihiMillis != null) obj.put("hatirlatmaTarihiMillis", r.hatirlatmaTarihiMillis)
            if (!r.irsaliyeGorselPath.isNullOrBlank()) obj.put("irsaliyeGorselPath", r.irsaliyeGorselPath)
            obj.put("olusturmaTarihiMillis", r.olusturmaTarihiMillis)
            obj.put("guncellemeTarihiMillis", r.guncellemeTarihiMillis)
            array.put(obj)
        }
        root.put("iadeTakipList", array)
        return root.toString(2)
    }

    // ==========================================
    // ADETSEL SAYIM EXPORT METOTLARI
    // ==========================================

    fun exportCountAndIadeToJson(sayimList: List<AdetselKayit>, iadeList: List<DepoIadeKaydi>): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("module", "count_and_iade_only")
        root.put("appName", "SKT & Mağaza Takip")
        root.put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))
        root.put("totalSayimRecords", sayimList.size)
        root.put("totalIadeRecords", iadeList.size)

        val sayimArray = JSONArray()
        sayimList.forEach { s ->
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

        val iadeArray = JSONArray()
        iadeList.forEach { i ->
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

        return root.toString(2)
    }

    fun exportAdetselSayimToCsv(records: List<AdetselKayit>): String {
        val sb = StringBuilder()
        // UTF-8 BOM
        sb.append('\uFEFF')
        sb.append("Sıra;Barkod;Ürün Kodu;Ürün Adı;Sistem Stoğu;Sayılan;Fark;Durum;Sayım Sonucu;Notlar;İşlem Tarihi\n")

        val dateFmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR"))

        records.forEachIndexed { index, r ->
            val durumText = if (r.yapildiMi) "TAMAMLANDI" else "YAPILACAK"
            val barkodEsc = r.barkod.replace("\"", "\"\"")
            val kodEsc = r.urunKodu.replace("\"", "\"\"")
            val adEsc = r.urunAdi.replace("\"", "\"\"")
            val sonucEsc = r.sayimSonucu.replace("\"", "\"\"")
            val notEsc = r.notlar.replace("\"", "\"\"")
            val islemDateStr = if (r.islemTarihi > 0) dateFmt.format(Date(r.islemTarihi)) else "-"

            sb.append("${index + 1};")
            sb.append("\"$barkodEsc\";")
            sb.append("\"$kodEsc\";")
            sb.append("\"$adEsc\";")
            sb.append("${r.beklenenAdet};")
            sb.append("${r.sayilanAdet};")
            sb.append("${r.farkAdet};")
            sb.append("\"$durumText\";")
            sb.append("\"$sonucEsc\";")
            sb.append("\"$notEsc\";")
            sb.append("\"$islemDateStr\"\n")
        }
        return sb.toString()
    }

    fun exportAdetselSayimToTxt(records: List<AdetselKayit>): String {
        val sb = StringBuilder()
        val timeStamp = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR")).format(Date())
        sb.append("=================================================================\n")
        sb.append("                     MAĞAZA ADETSEL SAYIM RAPORU                 \n")
        sb.append("Tarih: $timeStamp\n")
        sb.append("Toplam: ${records.size} Ürün | Yapılan: ${records.count { it.yapildiMi }} | Bekleyen: ${records.count { !it.yapildiMi }}\n")
        sb.append("=================================================================\n")
        sb.append(String.format(Locale.getDefault(), "%-4s | %-14s | %-24s | %-7s | %-7s | %-6s\n", "NO", "BARKOD", "ÜRÜN ADI", "SİSTEM", "SAYILAN", "FARK"))
        sb.append("-----------------------------------------------------------------\n")

        records.forEachIndexed { index, r ->
            val shortName = if (r.urunAdi.length > 24) r.urunAdi.take(21) + "..." else r.urunAdi
            val barcode = if (r.barkod.isNotBlank()) r.barkod else r.urunKodu.ifBlank { "-" }
            val shortBarcode = if (barcode.length > 14) barcode.take(14) else barcode
            sb.append(
                String.format(
                    Locale.getDefault(),
                    "%-4d | %-14s | %-24s | %-7d | %-7d | %-+6d\n",
                    index + 1,
                    shortBarcode,
                    shortName,
                    r.beklenenAdet,
                    r.sayilanAdet,
                    r.farkAdet
                )
            )
            if (r.notlar.isNotBlank()) {
                sb.append("     Not: ${r.notlar}\n")
            }
        }
        sb.append("=================================================================\n")
        return sb.toString()
    }

    fun exportAdetselSayimToJson(records: List<AdetselKayit>): String {
        val root = JSONObject()
        root.put("module", "adetsel_sayim")
        root.put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))
        root.put("totalRecords", records.size)
        root.put("completedCount", records.count { it.yapildiMi })
        root.put("pendingCount", records.count { !it.yapildiMi })

        val array = JSONArray()
        records.forEach { r ->
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("productId", r.productId)
            obj.put("barkod", r.barkod)
            obj.put("urunKodu", r.urunKodu)
            obj.put("urunAdi", r.urunAdi)
            obj.put("kategori", r.kategori)
            obj.put("yapildiMi", r.yapildiMi)
            obj.put("beklenenAdet", r.beklenenAdet)
            obj.put("sayilanAdet", r.sayilanAdet)
            obj.put("farkAdet", r.farkAdet)
            obj.put("sayimSonucu", r.sayimSonucu)
            obj.put("notlar", r.notlar)
            obj.put("eklenmeTarihi", r.eklenmeTarihi)
            obj.put("islemTarihi", r.islemTarihi)
            array.put(obj)
        }
        root.put("sayimList", array)
        return root.toString(2)
    }
}
