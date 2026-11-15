package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE products ADD COLUMN resimUrl TEXT DEFAULT NULL")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS index_products_barkod ON products(barkod)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_products_urunKodu ON products(urunKodu)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_products_sktTarihi ON products(sktTarihi)")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS stock_movements (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                barkod TEXT NOT NULL,
                urunAdi TEXT NOT NULL,
                adet INTEGER NOT NULL,
                islemTuru TEXT NOT NULL,
                tarih INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS stock_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                barcode TEXT NOT NULL,
                productName TEXT NOT NULL,
                actionType TEXT NOT NULL,
                quantity INTEGER NOT NULL,
                sktDate INTEGER,
                daysRemaining INTEGER,
                timestamp INTEGER NOT NULL
            )
        """.trimIndent())
        try {
            db.execSQL("""
                INSERT INTO stock_logs (barcode, productName, actionType, quantity, sktDate, daysRemaining, timestamp)
                SELECT barkod, urunAdi, islemTuru, adet, NULL, NULL, tarih FROM stock_movements
            """.trimIndent())
        } catch (_: Exception) {}
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE products ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS inventory_audits (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                category TEXT NOT NULL,
                auditDateMillis INTEGER NOT NULL,
                notes TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try { db.execSQL("ALTER TABLE inventory_audits ADD COLUMN revenue REAL NOT NULL DEFAULT 0.0") } catch (_: Exception) {}
        try { db.execSQL("ALTER TABLE inventory_audits ADD COLUMN fireCost REAL NOT NULL DEFAULT 0.0") } catch (_: Exception) {}
        try { db.execSQL("ALTER TABLE inventory_audits ADD COLUMN targetRatio REAL NOT NULL DEFAULT 0.0") } catch (_: Exception) {}
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS inventory_audits (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                category TEXT NOT NULL,
                auditDateMillis INTEGER NOT NULL,
                revenue REAL NOT NULL DEFAULT 0.0,
                fireCost REAL NOT NULL DEFAULT 0.0,
                targetRatio REAL NOT NULL DEFAULT 0.0,
                notes TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
        try { db.execSQL("ALTER TABLE inventory_audits ADD COLUMN revenue REAL NOT NULL DEFAULT 0.0") } catch (_: Exception) {}
        try { db.execSQL("ALTER TABLE inventory_audits ADD COLUMN fireCost REAL NOT NULL DEFAULT 0.0") } catch (_: Exception) {}
        try { db.execSQL("ALTER TABLE inventory_audits ADD COLUMN targetRatio REAL NOT NULL DEFAULT 0.0") } catch (_: Exception) {}
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try { db.execSQL("ALTER TABLE inventory_audits ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0") } catch (_: Exception) {}
        try { db.execSQL("ALTER TABLE inventory_audits ADD COLUMN auditPeriod TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
        try { db.execSQL("UPDATE inventory_audits SET updatedAt = createdAt WHERE updatedAt = 0") } catch (_: Exception) {}
    }
}

@Database(
    entities = [
        Product::class,
        InspectionReport::class,
        AdetselKayit::class,
        StockMovement::class,
        StockLog::class,
        InventoryAuditEntity::class
    ],
    version = 10,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun inspectionReportDao(): InspectionReportDao
    abstract fun adetselDao(): AdetselDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun stockLogDao(): StockLogDao
    abstract fun inventoryAuditDao(): InventoryAuditDao
    fun reportDao(): InspectionReportDao = inspectionReportDao()

    /**
     * Executes the given block in an atomic database transaction.
     * Automatically rolls back all changes if any exception occurs.
     */
    suspend fun <R> runInTransaction(block: suspend () -> R): R {
        return withTransaction(block)
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

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
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10
                    )
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .addCallback(AppDatabaseCallback(scope))
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
            }
        }
    }
}
