package com.example.data

typealias ProductEntity = Product
typealias StockLogEntity = StockLog
typealias DepoIadeEntity = DepoIadeKaydi
typealias AdetselSayimEntity = AdetselKayit
typealias DepoIadeItem = DepoIadeKaydi
typealias AdetselSayimItem = AdetselKayit
typealias InventoryAuditBackupEntity = InventoryAuditEntity

/**
 * Tüm uygulama modüllerini ve tablolarını kapsayan tek ve tam yedek modeli.
 * - products: Kayıtlı ürünler, fiyatlar, stok adetleri ve SKT kayıtları
 * - stockLogs: Satıldı, Fire, İade ve SKT Giriş stok hareketleri geçmişi
 * - iadeList: İade ve Depo Takip kayıtları
 * - sayimList: Adetsel sayım sonuçları ve stok farkları
 * - inventoryAudits: Envanter Takip kayıtları (Ciro, Fire Tutarı, Hedef Oran, Tarih ve Notlar)
 */
data class AppFullBackup(
    val version: Int = 3,
    val exportTimestamp: Long = System.currentTimeMillis(),
    val products: List<ProductEntity> = emptyList(),
    val stockLogs: List<StockLogEntity> = emptyList(),     // Satıldı / Fire / Analiz hareketleri
    val iadeList: List<DepoIadeEntity> = emptyList(),      // İade ve Depo Takip kayıtları
    val sayimList: List<AdetselSayimEntity> = emptyList(), // Sayım sonuçları ve stok farkları
    val inventoryAudits: List<InventoryAuditEntity> = emptyList(), // Envanter takip kayıtları
    val backupDate: Long = exportTimestamp,
    val iadeTakipList: List<DepoIadeEntity> = iadeList
) {
    val effectiveIadeList: List<DepoIadeEntity>
        get() = if (iadeList.isNotEmpty()) iadeList else iadeTakipList
}

typealias AppBackupData = AppFullBackup
typealias FullBackupData = AppFullBackup
