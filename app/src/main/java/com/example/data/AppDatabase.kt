package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [Product::class, InspectionReport::class, TurRaporu::class, TurKontrolKaydi::class, AdetselKayit::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun inspectionReportDao(): InspectionReportDao
    abstract fun turDao(): TurDao
    abstract fun adetselDao(): AdetselDao
    fun reportDao(): InspectionReportDao = inspectionReportDao()

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // SAFE ROOM MIGRATIONS (Preserves all existing user records, products, and reports)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE products ADD COLUMN isImportant INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    android.util.Log.w("AppDatabase", "Migration 1->2 column already exists: ${e.message}")
                }
                try {
                    db.execSQL("ALTER TABLE products ADD COLUMN sonKontrolTarihi INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    android.util.Log.w("AppDatabase", "Migration 1->2 column already exists: ${e.message}")
                }
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE products ADD COLUMN fiyat REAL")
                } catch (e: Exception) {
                    android.util.Log.w("AppDatabase", "Migration 2->3 column already exists: ${e.message}")
                }
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS inspection_reports (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tarih INTEGER NOT NULL,
                        reyonAdi TEXT NOT NULL,
                        tarananUrunSayisi INTEGER NOT NULL,
                        suresiGecenSayisi INTEGER NOT NULL,
                        kritikUrunSayisi INTEGER NOT NULL,
                        fireTutari REAL NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tur_raporlari (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        turTarihi INTEGER NOT NULL,
                        hedefReyon TEXT NOT NULL,
                        toplamUrunSayisi INTEGER NOT NULL,
                        satilanUrunSayisi INTEGER NOT NULL,
                        toplamSatilanAdet INTEGER NOT NULL,
                        fireUrunSayisi INTEGER NOT NULL,
                        toplamFireAdet INTEGER NOT NULL,
                        notrUrunSayisi INTEGER NOT NULL,
                        tamamlandiMi INTEGER NOT NULL,
                        turSuresiSaniye INTEGER NOT NULL,
                        toplamPuan INTEGER NOT NULL,
                        tahminiFireMaliyeti REAL NOT NULL,
                        enCokFireKategori TEXT NOT NULL DEFAULT '',
                        personelAdi TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tur_kontrol_kayitlari (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        turId INTEGER NOT NULL,
                        productId INTEGER NOT NULL,
                        urunAdiSnapshot TEXT NOT NULL,
                        barkodSnapshot TEXT NOT NULL,
                        kategoriSnapshot TEXT NOT NULL,
                        durum TEXT NOT NULL,
                        islemAdedi INTEGER NOT NULL,
                        kontrolTarihi INTEGER NOT NULL,
                        personelSnapshot TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE tur_raporlari ADD COLUMN enCokFireKategori TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {
                    android.util.Log.w("AppDatabase", "Migration 5->6 column already exists: ${e.message}")
                }
                try {
                    db.execSQL("ALTER TABLE tur_raporlari ADD COLUMN personelAdi TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {
                    android.util.Log.w("AppDatabase", "Migration 5->6 column already exists: ${e.message}")
                }
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS adetsel_kayitlar (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        productId INTEGER NOT NULL,
                        urunAdi TEXT NOT NULL,
                        urunKodu TEXT NOT NULL,
                        barkod TEXT NOT NULL DEFAULT '',
                        kategori TEXT NOT NULL DEFAULT '',
                        eklenmeTarihi INTEGER NOT NULL,
                        yapildiMi INTEGER NOT NULL DEFAULT 0,
                        sayimSonucu TEXT NOT NULL DEFAULT '',
                        beklenenAdet INTEGER NOT NULL DEFAULT 0,
                        sayilanAdet INTEGER NOT NULL DEFAULT 0,
                        farkAdet INTEGER NOT NULL DEFAULT 0,
                        notlar TEXT NOT NULL DEFAULT '',
                        islemTarihi INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(
            context: Context,
            scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
        ): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "skt_takip_database"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.productDao(), database.inspectionReportDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(productDao: ProductDao, reportDao: InspectionReportDao) {
            val existing = productDao.getAllProductsList()
            if (existing.isNotEmpty()) {
                return
            }
            val now = System.currentTimeMillis()
            val oneDayMs = 24L * 60 * 60 * 1000

            // Helper for generating future/past SKT dates
            fun sktOffset(days: Int): Long {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, days)
                return cal.timeInMillis
            }

            val seedProducts = listOf(
                // 1. Expired items (Süresi Geçen)
                Product(
                    barkod = "8690526010011",
                    urunKodu = "25001234",
                    urunAdi = "BİSKÜVİ ÇİKOLATA KAPLI 56 G BENİMO KOMBO",
                    kategori = "Atıştırmalık",
                    sktTarihi = sktOffset(-2), // 2 days ago (Expired)
                    stokAdedi = 12
                ),
                Product(
                    barkod = "8690632020455",
                    urunKodu = "25001288",
                    urunAdi = "DANONE BİTTER ÇİKOLATALI PUDİNG 100 G",
                    kategori = "Süt & Tatlı",
                    sktTarihi = sktOffset(-1), // Yesterday (Expired) - Batch 1
                    stokAdedi = 5
                ),
                Product(
                    barkod = "8690789055667",
                    urunKodu = "25002506",
                    urunAdi = "DORİTOS NACHO ACI BİBERLİ CİPS 117 G",
                    kategori = "Atıştırmalık",
                    sktTarihi = sktOffset(-5), // 5 days ago (Expired)
                    stokAdedi = 4
                ),
                Product(
                    barkod = "8690620010019",
                    urunKodu = "25001560",
                    urunAdi = "PINAR DİLİMLİ TOST PEYNİRİ 200 G",
                    kategori = "Süt & Şarküteri",
                    sktTarihi = sktOffset(-3), // 3 days ago (Expired) - Batch 1
                    stokAdedi = 2
                ),

                // 2. Critical items (Kritik - 0..7 days)
                Product(
                    barkod = "8690504031122",
                    urunKodu = "25001402",
                    urunAdi = "SÜTAŞ SÜZME PEYNİR 500 G TAM YAĞLI",
                    kategori = "Süt & Şarküteri",
                    sktTarihi = sktOffset(3), // 3 days left - Batch 1
                    stokAdedi = 18
                ),
                Product(
                    barkod = "8690504099881",
                    urunKodu = "25001415",
                    urunAdi = "BİRŞAH GÜNLÜK PASTORİZE SÜT 1 L",
                    kategori = "Süt & Şarküteri",
                    sktTarihi = sktOffset(1), // Tomorrow - Batch 1
                    stokAdedi = 24
                ),
                Product(
                    barkod = "8690620010019",
                    urunKodu = "25001560",
                    urunAdi = "PINAR DİLİMLİ TOST PEYNİRİ 200 G",
                    kategori = "Süt & Şarküteri",
                    sktTarihi = sktOffset(5), // 5 days left - Batch 2
                    stokAdedi = 9
                ),
                Product(
                    barkod = "8690504011223",
                    urunKodu = "25002501",
                    urunAdi = "SÜTAŞ AYRAN 200 ML BARDAN",
                    kategori = "İçecek",
                    sktTarihi = sktOffset(2), // 2 days left
                    stokAdedi = 40
                ),
                Product(
                    barkod = "8690555544556",
                    urunKodu = "25002515",
                    urunAdi = "KNORR ŞEHRİYELİ TAVUK ÇORBASI 58 G",
                    kategori = "Hazır Gıda",
                    sktTarihi = sktOffset(6), // 6 days left
                    stokAdedi = 19
                ),
                Product(
                    barkod = "8690620066778",
                    urunKodu = "25002517",
                    urunAdi = "PINAR LABNE PEYNİR 200 G",
                    kategori = "Süt & Şarküteri",
                    sktTarihi = sktOffset(2), // 2 days left - Batch 1
                    stokAdedi = 16
                ),

                // 3. Soon items (Yakın - 8..30 days)
                Product(
                    barkod = "8690123456789",
                    urunKodu = "25001890",
                    urunAdi = "TORKU BANADA KAKAOLU FINDIK KREMASI 400 G",
                    kategori = "Kahvaltılık",
                    sktTarihi = sktOffset(14), // 14 days left
                    stokAdedi = 30
                ),
                Product(
                    barkod = "8690526090907",
                    urunKodu = "25001912",
                    urunAdi = "ÜLKER ÇİKOLATALI GOFRET 36 G (5'Lİ PAKET)",
                    kategori = "Atıştırmalık",
                    sktTarihi = sktOffset(22), // 22 days left - Batch 1
                    stokAdedi = 45
                ),
                Product(
                    barkod = "8690700112233",
                    urunKodu = "25002011",
                    urunAdi = "TAT DOMATES SALÇASI 830 G TENEKE KUTU",
                    kategori = "Temel Gıda",
                    sktTarihi = sktOffset(28), // 28 days left
                    stokAdedi = 16
                ),
                Product(
                    barkod = "8690504099881",
                    urunKodu = "25001415",
                    urunAdi = "BİRŞAH GÜNLÜK PASTORİZE SÜT 1 L",
                    kategori = "Süt & Şarküteri",
                    sktTarihi = sktOffset(10), // 10 days left - Batch 2
                    stokAdedi = 15
                ),
                Product(
                    barkod = "8690504031122",
                    urunKodu = "25001402",
                    urunAdi = "SÜTAŞ SÜZME PEYNİR 500 G TAM YAĞLI",
                    kategori = "Süt & Şarküteri",
                    sktTarihi = sktOffset(20), // 20 days left - Batch 2
                    stokAdedi = 12
                ),
                Product(
                    barkod = "8690632020455",
                    urunKodu = "25001288",
                    urunAdi = "DANONE BİTTER ÇİKOLATALI PUDİNG 100 G",
                    kategori = "Süt & Tatlı",
                    sktTarihi = sktOffset(12), // 12 days left - Batch 2
                    stokAdedi = 14
                ),
                Product(
                    barkod = "8690533033445",
                    urunKodu = "25002503",
                    urunAdi = "ETİ HOŞBEŞ FINDIK KREMALI GOFRET 142 G",
                    kategori = "Atıştırmalık",
                    sktTarihi = sktOffset(18), // 18 days left
                    stokAdedi = 15
                ),
                Product(
                    barkod = "8690123451122",
                    urunKodu = "25002504",
                    urunAdi = "TORKU SÜT 1 L %3.1 YAĞLI",
                    kategori = "Süt & Şarküteri",
                    sktTarihi = sktOffset(11), // 11 days left
                    stokAdedi = 22
                ),
                Product(
                    barkod = "8690580088990",
                    urunKodu = "25002509",
                    urunAdi = "LİPTON İCE TEA ŞEFTALİ 500 ML",
                    kategori = "İçecek",
                    sktTarihi = sktOffset(15), // 15 days left
                    stokAdedi = 18
                ),
                Product(
                    barkod = "8690620066778",
                    urunKodu = "25002517",
                    urunAdi = "PINAR LABNE PEYNİR 200 G",
                    kategori = "Süt & Şarküteri",
                    sktTarihi = sktOffset(25), // 25 days left - Batch 2
                    stokAdedi = 20
                ),

                // 4. Normal items (30+ days)
                Product(
                    barkod = "8690504099881",
                    urunKodu = "25001415",
                    urunAdi = "BİRŞAH GÜNLÜK PASTORİZE SÜT 1 L",
                    kategori = "Süt & Şarküteri",
                    sktTarihi = sktOffset(32), // 32 days left - Batch 3
                    stokAdedi = 30
                ),
                Product(
                    barkod = "8690620010019",
                    urunKodu = "25001560",
                    urunAdi = "PINAR DİLİMLİ TOST PEYNİRİ 200 G",
                    kategori = "Süt & Şarküteri",
                    sktTarihi = sktOffset(35), // 35 days left - Batch 3
                    stokAdedi = 20
                ),
                Product(
                    barkod = "8690526090907",
                    urunKodu = "25001912",
                    urunAdi = "ÜLKER ÇİKOLATALI GOFRET 36 G (5'Lİ PAKET)",
                    kategori = "Atıştırmalık",
                    sktTarihi = sktOffset(60), // 60 days left - Batch 2
                    stokAdedi = 50
                ),
                Product(
                    barkod = "8690526022334",
                    urunKodu = "25002502",
                    urunAdi = "ÜLKER SÜTLÜ ÇİKOLATA 60 G",
                    kategori = "Atıştırmalık",
                    sktTarihi = sktOffset(45), // 45 days left
                    stokAdedi = 25
                ),
                Product(
                    barkod = "8690789044556",
                    urunKodu = "25002505",
                    urunAdi = "LAYS KLASİK PATATES CİPSİ SÜPER BOY 107 G",
                    kategori = "Atıştırmalık",
                    sktTarihi = sktOffset(60), // 60 days left
                    stokAdedi = 10
                ),
                Product(
                    barkod = "8690580066778",
                    urunKodu = "25002507",
                    urunAdi = "COCA-COLA ORİJİNAL TAT 1 L",
                    kategori = "İçecek",
                    sktTarihi = sktOffset(90), // 90 days left
                    stokAdedi = 36
                ),
                Product(
                    barkod = "8690800334455",
                    urunKodu = "25002155",
                    urunAdi = "DOĞUŞ GURME DEMLİK POŞET ÇAY 100'LÜ",
                    kategori = "Sıcak İçecek",
                    sktTarihi = sktOffset(180), // 180 days left
                    stokAdedi = 50
                ),
                Product(
                    barkod = "8690900556677",
                    urunKodu = "25002340",
                    urunAdi = "FAİRY BULAŞIK SIVISI LİMON 1500 ML",
                    kategori = "Temizlik",
                    sktTarihi = sktOffset(365), // 365 days left
                    stokAdedi = 20
                )
            )

            productDao.insertAll(seedProducts)

            // Seed 1 past inspection report for example
            val seedReport = InspectionReport(
                tarih = now - oneDayMs,
                reyonAdi = "Dolap Ürünleri",
                tarananUrunSayisi = 14,
                suresiGecenSayisi = 1,
                kritikUrunSayisi = 3,
                fireTutari = 84.50
            )
            reportDao.insertReport(seedReport)
        }
    }
}
