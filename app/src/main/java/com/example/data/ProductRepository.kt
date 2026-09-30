package com.example.data

import android.content.Context
import com.example.sync.CloudSyncManager
import kotlinx.coroutines.flow.Flow
import java.io.File

class ProductRepository(
    val productDao: ProductDao,
    val reportDao: InspectionReportDao,
    val adetselDao: AdetselDao? = null,
    val stockMovementDao: StockMovementDao? = null,
    val stockLogDao: StockLogDao? = null
) {
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allReports: Flow<List<InspectionReport>> = reportDao.getAllReports()
    val allAdetselKayitlari: Flow<List<AdetselKayit>> = adetselDao?.getAllAdetselKayitlari() ?: kotlinx.coroutines.flow.emptyFlow()
    val yapilacakAdetselKayitlari: Flow<List<AdetselKayit>> = adetselDao?.getYapilacakKayitlar() ?: kotlinx.coroutines.flow.emptyFlow()
    val yapildiAdetselKayitlari: Flow<List<AdetselKayit>> = adetselDao?.getYapildiKayitlar() ?: kotlinx.coroutines.flow.emptyFlow()
    val allStockMovements: Flow<List<StockMovement>> = stockMovementDao?.getAllStockMovements() ?: kotlinx.coroutines.flow.emptyFlow()
    val allStockLogs: Flow<List<StockLog>> = stockLogDao?.getAllLogs() ?: kotlinx.coroutines.flow.emptyFlow()
    val totalSoldQuantity: Flow<Int> = stockLogDao?.getTotalSoldQuantityFlow() ?: kotlinx.coroutines.flow.flowOf(0)
    val totalFireQuantity: Flow<Int> = stockLogDao?.getTotalFireQuantityFlow() ?: kotlinx.coroutines.flow.flowOf(0)

    suspend fun insertStockLog(log: StockLog): Long {
        return stockLogDao?.insertLog(log) ?: 0L
    }

    suspend fun insertStockLogs(logs: List<StockLog>) {
        stockLogDao?.insertAll(logs)
    }

    suspend fun getTotalSoldQuantityDirect(): Int {
        return stockLogDao?.getTotalSoldQuantity() ?: 0
    }

    suspend fun getTotalFireQuantityDirect(): Int {
        return stockLogDao?.getTotalFireQuantity() ?: 0
    }

    suspend fun getSktEntryCountForBarcode(code: String): Int {
        return stockLogDao?.getSktEntryCountForBarcode(code) ?: 0
    }

    suspend fun getAllStockLogsDirect(): List<StockLog> {
        return stockLogDao?.getAllLogsList() ?: emptyList()
    }

    suspend fun logStockMovement(
        barkod: String,
        urunAdi: String,
        adet: Int,
        islemTuru: String,
        tarih: Long = System.currentTimeMillis()
    ): Long {
        if (stockMovementDao == null || adet <= 0) return 0L
        val movement = StockMovement(
            barkod = barkod,
            urunAdi = urunAdi,
            adet = adet,
            islemTuru = islemTuru,
            tarih = tarih
        )
        return stockMovementDao.insertMovement(movement)
    }

    suspend fun getAllStockMovementsDirect(): List<StockMovement> {
        return stockMovementDao?.getAllStockMovementsList() ?: emptyList()
    }

    suspend fun insertAdetselKayit(kayit: AdetselKayit): Long {
        return adetselDao?.insertAdetselKayit(kayit) ?: 0L
    }

    suspend fun updateAdetselKayit(kayit: AdetselKayit) {
        adetselDao?.updateAdetselKayit(kayit)
    }

    suspend fun deleteAdetselKayit(kayit: AdetselKayit) {
        adetselDao?.deleteAdetselKayit(kayit)
    }

    suspend fun deleteAdetselKayitById(id: Int) {
        adetselDao?.deleteAdetselKayitById(id)
    }

    suspend fun clearCompletedAdetselKayitlar() {
        adetselDao?.clearCompletedAdetselKayitlar()
    }

    suspend fun getPendingAdetselKayitlarList(): List<AdetselKayit> {
        return adetselDao?.getPendingKayitlarList() ?: emptyList()
    }

    suspend fun getProductByBarcode(barkod: String): Product? {
        return productDao.getProductByBarcode(barkod)
    }

    suspend fun getProductById(productId: Int): Product? {
        return productDao.getProductById(productId)
    }

    suspend fun getProductListDirect(): List<Product> {
        return productDao.getAllProductsList()
    }

    suspend fun updateProduct(product: Product): Int {
        val cleanCategory = com.example.util.CategoryClassifier.classifyCategory(product.urunAdi)
        val toUpdate = if (product.kategori != cleanCategory) product.copy(kategori = cleanCategory) else product
        val count = productDao.updateProduct(toUpdate)
        CloudSyncManager.syncProductToCloud(toUpdate)
        return count
    }

    suspend fun reclassifyAllProducts(): Int {
        val allProds = productDao.getAllProductsList()
        var updatedCount = 0
        val toUpdate = mutableListOf<Product>()
        for (prod in allProds) {
            val correctCategory = com.example.util.CategoryClassifier.classifyCategory(prod.urunAdi)
            if (prod.kategori != correctCategory) {
                val updated = prod.copy(kategori = correctCategory)
                toUpdate.add(updated)
                updatedCount++
            }
        }
        if (toUpdate.isNotEmpty()) {
            productDao.updateAllProducts(toUpdate)
            for (up in toUpdate) {
                CloudSyncManager.syncProductToCloud(up)
            }
        }
        return updatedCount
    }

    suspend fun updateProductStock(product: Product, newStok: Int) {
        val safeStok = maxOf(0, newStok)
        val updated = product.copy(stokAdedi = safeStok, sonKontrolTarihi = System.currentTimeMillis())
        productDao.updateProduct(updated)
        CloudSyncManager.syncProductToCloud(updated)
    }

    suspend fun updateProductStock(productId: Int, newStok: Int) {
        val safeStok = maxOf(0, newStok)
        val now = System.currentTimeMillis()
        val prod = productDao.getProductById(productId)
        if (prod != null) {
            val updated = prod.copy(stokAdedi = safeStok, sonKontrolTarihi = now)
            productDao.updateProduct(updated)
            CloudSyncManager.syncProductToCloud(updated)
        } else {
            productDao.updateStockAndControlDate(productId, safeStok, now)
        }
    }

    suspend fun getProductsByBarcode(barkod: String): List<Product> {
        return productDao.getProductsByBarcode(barkod)
    }

    suspend fun insertOrUpdateProduct(product: Product): Long {
        val cleanCategory = com.example.util.CategoryClassifier.classifyCategory(product.urunAdi)
        val productToSave = if (product.kategori != cleanCategory) product.copy(kategori = cleanCategory) else product
        val id = if (productToSave.id != 0) {
            productDao.updateProduct(productToSave)
            productToSave.id.toLong()
        } else {
            productDao.insertProduct(productToSave)
        }
        val insertedProd = productToSave.copy(id = id.toInt())
        CloudSyncManager.syncProductToCloud(insertedProd)
        return id
    }

    suspend fun insertNewSktForBarcode(product: Product): Long {
        val cleanCategory = com.example.util.CategoryClassifier.classifyCategory(product.urunAdi)
        val productToSave = if (product.kategori != cleanCategory) product.copy(kategori = cleanCategory) else product
        val id = productDao.insertProduct(productToSave.copy(id = 0))
        val insertedProd = productToSave.copy(id = id.toInt())
        CloudSyncManager.syncProductToCloud(insertedProd)
        return id
    }

    suspend fun insertProductsBatch(products: List<Product>) {
        if (products.isEmpty()) return
        val cleanList = products.map { p ->
            val cat = com.example.util.CategoryClassifier.classifyCategory(p.urunAdi)
            if (p.kategori != cat) p.copy(kategori = cat) else p
        }
        productDao.insertAll(cleanList)
    }

    suspend fun deleteProduct(product: Product) {
        productDao.deleteProduct(product)
        CloudSyncManager.deleteProductFromCloud(product.id)
    }

    suspend fun resetAllData() {
        productDao.deleteAllProducts()
        adetselDao?.deleteAllAdetselKayitlar()
        stockMovementDao?.deleteAllMovements()
        stockLogDao?.deleteAllLogs()
        CloudSyncManager.clearAllCloudData()
    }

    suspend fun insertReport(report: InspectionReport): Long {
        return reportDao.insertReport(report)
    }

    suspend fun createUnifiedBackupJson(context: Context): String {
        return DataBackupManager.createUnifiedBackupJson(context, productDao, reportDao, adetselDao)
    }

    suspend fun saveLocalBackup(context: Context, tag: String = "manual"): File? {
        return DataBackupManager.saveAutoBackupToStorage(context, productDao, reportDao, adetselDao, tag)
    }

    fun getLocalBackups(context: Context): List<BackupMetadata> {
        return DataBackupManager.listLocalBackups(context)
    }

    suspend fun restoreFromJson(context: Context, jsonString: String, merge: Boolean = true): BackupRestoreResult {
        return DataBackupManager.restoreFromJson(context, jsonString, productDao, reportDao, adetselDao, merge)
    }
}
