package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class IadeRedNedeni(val displayName: String) {
    DEPO_KABUL_ETMEDI("Depo kabul etmedi"),
    SARTLARA_UYGUN_DEGIL("İade şartlarına uygun değil"),
    HASARLI_URUN("Hasarlı ürün"),
    SKT_NEDENIYLE_RED("SKT sorunu"),
    IRSALIYE_EKSIK("İrsaliye / Belge eksiği"),
    DIGER("Diğer")
}

enum class IadeOncelik(val displayName: String) {
    NORMAL("Normal"),
    ONEMLI("Önemli"),
    KRITIK("Kritik")
}

enum class IadeDurumu(val displayName: String) {
    DEVAM_EDIYOR("Devam Ediyor"),
    ONAYLANDI("Onaylandı"),
    REDDEDILDI("Reddedildi")
}

data class DepoIadeKaydi(
    val id: String = UUID.randomUUID().toString(),
    val urunAdi: String,
    val irsaliyeGorselPath: String? = null,
    val iadeTarihi: String,
    val iadeTarihiMillis: Long = System.currentTimeMillis(),
    val redNedeni: String = IadeRedNedeni.DEPO_KABUL_ETMEDI.displayName,
    val aciklama: String = "",
    val oncelik: IadeOncelik = IadeOncelik.NORMAL,
    val durum: IadeDurumu = IadeDurumu.DEVAM_EDIYOR,
    val hatirlatmaTarihi: String = "",
    val hatirlatmaTarihiMillis: Long? = null,
    val olusturmaTarihiMillis: Long = System.currentTimeMillis(),
    val guncellemeTarihiMillis: Long = System.currentTimeMillis()
) {
    val isKritik: Boolean get() = oncelik == IadeOncelik.KRITIK
    val hasGorsel: Boolean get() = !irsaliyeGorselPath.isNullOrBlank() && File(irsaliyeGorselPath).exists()
}

data class TakipStats(
    val toplamKayit: Int,
    val devamEdenSayisi: Int,
    val onaylananSayisi: Int,
    val reddedilenSayisi: Int,
    val gorselliKayitSayisi: Int
)

object DepoIadeManager {
    private const val PREFS_NAME = "depo_iade_takip_prefs"
    private const val KEY_RECORDS_JSON = "depo_iade_records_v2"
    private const val NOTIFICATION_CHANNEL_ID = "depo_iade_channel"

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.forLanguageTag("tr-TR"))

    fun getTodayDateString(): String {
        return dateFormat.format(Date())
    }

    fun parseDateToMillis(dateStr: String): Long {
        return try {
            dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    fun formatMillisToDate(millis: Long): String {
        return dateFormat.format(Date(millis))
    }

    fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val dir = File(context.filesDir, "irsaliyeler")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val fileName = "irsaliye_${System.currentTimeMillis()}.jpg"
            val destFile = File(dir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Physically deletes an invoice image from internal storage to prevent disk bloating.
     */
    fun deleteImageFile(imagePath: String?) {
        if (!imagePath.isNullOrBlank()) {
            try {
                val file = File(imagePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Deletes a record and removes its physical image file from storage.
     */
    fun deleteRecord(context: Context, recordId: String): List<DepoIadeKaydi> {
        val current = loadRecords(context).toMutableList()
        val toDelete = current.find { it.id == recordId }
        if (toDelete != null) {
            deleteImageFile(toDelete.irsaliyeGorselPath)
            current.remove(toDelete)
            saveRecords(context, current)
        }
        return current
    }

    /**
     * Clears all records and deletes all invoice images in storage.
     */
    fun clearAllRecords(context: Context) {
        val current = loadRecords(context)
        for (item in current) {
            deleteImageFile(item.irsaliyeGorselPath)
        }
        try {
            val dir = File(context.filesDir, "irsaliyeler")
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        saveRecords(context, emptyList())
    }

    private fun getDefaultSeedRecords(): List<DepoIadeKaydi> {
        val now = System.currentTimeMillis()
        val day = 86400000L

        return listOf(
            DepoIadeKaydi(
                id = "seed_1",
                urunAdi = "KOKOREÇ KUZU 150 G ŞAMPİYON",
                irsaliyeGorselPath = null,
                iadeTarihi = formatMillisToDate(now - 2 * day),
                iadeTarihiMillis = now - 2 * day,
                redNedeni = "Depo kabul etmedi",
                aciklama = "İrsaliye kesildi, ambar teslim tutanağı ve onay bekleniyor.",
                oncelik = IadeOncelik.KRITIK,
                durum = IadeDurumu.DEVAM_EDIYOR,
                hatirlatmaTarihi = formatMillisToDate(now + 1 * day),
                hatirlatmaTarihiMillis = now + 1 * day,
                olusturmaTarihiMillis = now - 2 * day,
                guncellemeTarihiMillis = now - 1 * day
            ),
            DepoIadeKaydi(
                id = "seed_2",
                urunAdi = "SÜT 1 L YARIM YAĞLI BİRSEN",
                irsaliyeGorselPath = null,
                iadeTarihi = formatMillisToDate(now - 4 * day),
                iadeTarihiMillis = now - 4 * day,
                redNedeni = "İade şartlarına uygun değil",
                aciklama = "Koli içi adet uyumsuzluğu giderildi, sevkiyata onay verildi.",
                oncelik = IadeOncelik.ONEMLI,
                durum = IadeDurumu.ONAYLANDI,
                hatirlatmaTarihi = "",
                hatirlatmaTarihiMillis = null,
                olusturmaTarihiMillis = now - 4 * day,
                guncellemeTarihiMillis = now - 2 * day
            ),
            DepoIadeKaydi(
                id = "seed_3",
                urunAdi = "TEREYAĞI 500 G KEBİR",
                irsaliyeGorselPath = null,
                iadeTarihi = formatMillisToDate(now - 5 * day),
                iadeTarihiMillis = now - 5 * day,
                redNedeni = "SKT sorunu",
                aciklama = "Süre aşımı gerekçesiyle depo tarafından reddedildi.",
                oncelik = IadeOncelik.NORMAL,
                durum = IadeDurumu.REDDEDILDI,
                hatirlatmaTarihi = "",
                hatirlatmaTarihiMillis = null,
                olusturmaTarihiMillis = now - 5 * day,
                guncellemeTarihiMillis = now - 1 * day
            )
        )
    }

    fun loadRecords(context: Context): List<DepoIadeKaydi> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_RECORDS_JSON, null)
        if (jsonStr == null) {
            // Check legacy v1 records if any
            val legacyStr = prefs.getString("depo_iade_records_v1", null)
            if (legacyStr != null) {
                try {
                    val legacyArray = JSONArray(legacyStr)
                    val list = mutableListOf<DepoIadeKaydi>()
                    for (i in 0 until legacyArray.length()) {
                        val obj = legacyArray.getJSONObject(i)
                        val legacyDurum = obj.optString("durum", "TAKIPTE")
                        val mappedDurum = when (legacyDurum) {
                            "COZULDU" -> IadeDurumu.ONAYLANDI
                            "REDDEDILDI" -> IadeDurumu.REDDEDILDI
                            else -> IadeDurumu.DEVAM_EDIYOR
                        }
                        list.add(
                            DepoIadeKaydi(
                                id = obj.optString("id", "").ifBlank { UUID.randomUUID().toString() },
                                urunAdi = obj.optString("urunAdi", ""),
                                irsaliyeGorselPath = obj.optString("irsaliyeGorselPath", "").takeIf { it.isNotBlank() },
                                iadeTarihi = obj.optString("iadeTarihi", getTodayDateString()),
                                iadeTarihiMillis = obj.optLong("iadeTarihiMillis", System.currentTimeMillis()),
                                redNedeni = obj.optString("redNedeni", IadeRedNedeni.DEPO_KABUL_ETMEDI.displayName),
                                aciklama = obj.optString("aciklama", ""),
                                oncelik = try {
                                    IadeOncelik.valueOf(obj.optString("oncelik", "NORMAL"))
                                } catch (e: Exception) {
                                    IadeOncelik.NORMAL
                                },
                                durum = mappedDurum,
                                hatirlatmaTarihi = obj.optString("hatirlatmaTarihi", ""),
                                hatirlatmaTarihiMillis = if (obj.has("hatirlatmaTarihiMillis") && !obj.isNull("hatirlatmaTarihiMillis")) {
                                    obj.optLong("hatirlatmaTarihiMillis")
                                } else null,
                                olusturmaTarihiMillis = obj.optLong("olusturmaTarihiMillis", System.currentTimeMillis()),
                                guncellemeTarihiMillis = obj.optLong("guncellemeTarihiMillis", System.currentTimeMillis())
                            )
                        )
                    }
                    saveRecords(context, list)
                    return list
                } catch (e: Exception) {
                    // fallback to seed
                }
            }
            return getDefaultSeedRecords().also {
                saveRecords(context, it)
            }
        }

        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<DepoIadeKaydi>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
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

                list.add(
                    DepoIadeKaydi(
                        id = obj.optString("id", "").ifBlank { UUID.randomUUID().toString() },
                        urunAdi = obj.optString("urunAdi", ""),
                        irsaliyeGorselPath = obj.optString("irsaliyeGorselPath", "").takeIf { it.isNotBlank() },
                        iadeTarihi = obj.optString("iadeTarihi", getTodayDateString()),
                        iadeTarihiMillis = obj.optLong("iadeTarihiMillis", System.currentTimeMillis()),
                        redNedeni = obj.optString("redNedeni", IadeRedNedeni.DEPO_KABUL_ETMEDI.displayName),
                        aciklama = obj.optString("aciklama", ""),
                        oncelik = try {
                            IadeOncelik.valueOf(obj.optString("oncelik", "NORMAL"))
                        } catch (e: Exception) {
                            IadeOncelik.NORMAL
                        },
                        durum = durum,
                        hatirlatmaTarihi = obj.optString("hatirlatmaTarihi", ""),
                        hatirlatmaTarihiMillis = if (obj.has("hatirlatmaTarihiMillis") && !obj.isNull("hatirlatmaTarihiMillis")) {
                            obj.optLong("hatirlatmaTarihiMillis")
                        } else null,
                        olusturmaTarihiMillis = obj.optLong("olusturmaTarihiMillis", System.currentTimeMillis()),
                        guncellemeTarihiMillis = obj.optLong("guncellemeTarihiMillis", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            getDefaultSeedRecords()
        }
    }

    fun saveRecords(context: Context, records: List<DepoIadeKaydi>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        for (item in records) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("urunAdi", item.urunAdi)
            if (item.irsaliyeGorselPath != null) {
                obj.put("irsaliyeGorselPath", item.irsaliyeGorselPath)
            }
            obj.put("iadeTarihi", item.iadeTarihi)
            obj.put("iadeTarihiMillis", item.iadeTarihiMillis)
            obj.put("redNedeni", item.redNedeni)
            obj.put("aciklama", item.aciklama)
            obj.put("oncelik", item.oncelik.name)
            obj.put("durum", item.durum.name)
            obj.put("hatirlatmaTarihi", item.hatirlatmaTarihi)
            if (item.hatirlatmaTarihiMillis != null) {
                obj.put("hatirlatmaTarihiMillis", item.hatirlatmaTarihiMillis)
            }
            obj.put("olusturmaTarihiMillis", item.olusturmaTarihiMillis)
            obj.put("guncellemeTarihiMillis", item.guncellemeTarihiMillis)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_RECORDS_JSON, jsonArray.toString()).apply()
    }

    fun calculateStats(records: List<DepoIadeKaydi>): TakipStats {
        val toplam = records.size
        val devamEden = records.count { it.durum == IadeDurumu.DEVAM_EDIYOR }
        val onaylanan = records.count { it.durum == IadeDurumu.ONAYLANDI }
        val reddedilen = records.count { it.durum == IadeDurumu.REDDEDILDI }
        val gorselli = records.count { it.hasGorsel }

        return TakipStats(
            toplamKayit = toplam,
            devamEdenSayisi = devamEden,
            onaylananSayisi = onaylanan,
            reddedilenSayisi = reddedilen,
            gorselliKayitSayisi = gorselli
        )
    }

    fun scheduleNotification(context: Context, kayit: DepoIadeKaydi) {
        if (kayit.hatirlatmaTarihiMillis == null) return
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Takip Hatırlatıcıları",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "İade ve operasyon takip kayıtları hatırlatmaları"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "takip")
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                kayit.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("${kayit.urunAdi} için kontrol zamanı geldi (${kayit.durum.displayName}).")
                .setContentText("Durum: ${kayit.durum.displayName} (${kayit.redNedeni}) kontrol zamanı.")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Ürün: ${kayit.urunAdi}\nDurum: ${kayit.durum.displayName} | Neden: ${kayit.redNedeni}\nNot: ${kayit.aciklama.ifBlank { "İade durumunu kontrol ediniz." }}")
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            val todayMidnight = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            if (kayit.hatirlatmaTarihiMillis <= todayMidnight + 86400000L) {
                notificationManager.notify(kayit.id.hashCode(), notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
