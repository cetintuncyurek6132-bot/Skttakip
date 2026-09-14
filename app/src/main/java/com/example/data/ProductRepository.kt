package com.example.data

import android.content.Context
import com.example.sync.CloudSyncManager
import kotlinx.coroutines.flow.Flow
import java.io.File

class ProductRepository(
    val productDao: ProductDao,
    val reportDao: InspectionReportDao,
    val adetselDao: AdetselDao? = null
) {
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allReports: Flow<List<InspectionReport>> = reportDao.getAllReports()
    val allAdetselKayitlari: Flow<List<AdetselKayit>> = adetselDao?.getAllAdetselKayitlari() ?: kotlinx.coroutines.flow.emptyFlow()
    val yapilacakAdetselKayitlari: Flow<List<AdetselKayit>> = adetselDao?.getYapilacakKayitlar() ?: kotlinx.coroutines.flow.emptyFlow()
    val yapildiAdetselKayitlari: Flow<List<AdetselKayit>> = adetselDao?.getYapildiKayitlar() ?: kotlinx.coroutines.flow.emptyFlow()

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
        val count = productDao.updateProduct(product)
        CloudSyncManager.syncProductToCloud(product)
        return count
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

    private suspend fun scopeSyncProduct(productId: Int) {
        val prod = productDao.getProductById(productId)
        if (prod != null) {
            CloudSyncManager.syncProductToCloud(prod)
        }
    }

    suspend fun getProductsByBarcode(barkod: String): List<Product> {
        return productDao.getProductsByBarcode(barkod)
    }

    suspend fun insertOrUpdateProduct(product: Product): Long {
        val id = if (product.id != 0) {
            productDao.updateProduct(product)
            product.id.toLong()
        } else {
            productDao.insertProduct(product)
        }
        val insertedProd = product.copy(id = id.toInt())
        CloudSyncManager.syncProductToCloud(insertedProd)
        return id
    }

    suspend fun insertNewSktForBarcode(product: Product): Long {
        val id = productDao.insertProduct(product.copy(id = 0))
        val insertedProd = product.copy(id = id.toInt())
        CloudSyncManager.syncProductToCloud(insertedProd)
        return id
    }

    suspend fun insertProductsBatch(products: List<Product>) {
        products.forEach { prod ->
            val id = if (prod.id != 0) {
                productDao.updateProduct(prod)
                prod.id.toLong()
            } else {
                productDao.insertProduct(prod)
            }
            CloudSyncManager.syncProductToCloud(prod.copy(id = id.toInt()))
        }
    }

    suspend fun deleteProduct(product: Product) {
        productDao.deleteProduct(product)
        CloudSyncManager.deleteProductFromCloud(product.id)
    }

    suspend fun resetAllData() {
        productDao.deleteAllProducts()
        reportDao.deleteAllReports()
        adetselDao?.deleteAllAdetselKayitlar()
        CloudSyncManager.clearAllCloudData()
    }

    suspend fun insertReport(report: InspectionReport): Long {
        return reportDao.insertReport(report)
    }

    suspend fun deleteReport(reportId: Int) {
        reportDao.deleteReportById(reportId)
    }

    suspend fun clearAllReports() {
        reportDao.deleteAllReports()
    }

    suspend fun reSeedDefaultData() {
        AppDatabase.populateInitialData(productDao, reportDao)
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
