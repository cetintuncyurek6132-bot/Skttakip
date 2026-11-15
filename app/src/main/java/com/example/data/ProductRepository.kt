package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

class ProductRepository(
    val productDao: ProductDao,
    val adetselDao: AdetselDao? = null,
    val stockLogDao: StockLogDao? = null,
    val inventoryAuditDao: InventoryAuditDao? = null
) {
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
        .catch { emit(emptyList()) }
        .flowOn(Dispatchers.IO)
    val allAdetselKayitlari: Flow<List<AdetselKayit>> = (adetselDao?.getAllAdetselKayitlari() ?: kotlinx.coroutines.flow.emptyFlow())
        .catch { emit(emptyList()) }
        .flowOn(Dispatchers.IO)
    val yapilacakAdetselKayitlari: Flow<List<AdetselKayit>> = (adetselDao?.getYapilacakKayitlar() ?: kotlinx.coroutines.flow.emptyFlow())
        .catch { emit(emptyList()) }
        .flowOn(Dispatchers.IO)
    val yapildiAdetselKayitlari: Flow<List<AdetselKayit>> = (adetselDao?.getYapildiKayitlar() ?: kotlinx.coroutines.flow.emptyFlow())
        .catch { emit(emptyList()) }
        .flowOn(Dispatchers.IO)
    val allStockLogs: Flow<List<StockLog>> = (stockLogDao?.getAllLogs() ?: kotlinx.coroutines.flow.emptyFlow())
        .catch { emit(emptyList()) }
        .flowOn(Dispatchers.IO)
    val allInventoryAudits: Flow<List<InventoryAuditEntity>> = (inventoryAuditDao?.getAllAuditsFlow() ?: kotlinx.coroutines.flow.emptyFlow())
        .catch { emit(emptyList()) }
        .flowOn(Dispatchers.IO)
    val totalSoldQuantity: Flow<Int> = (stockLogDao?.getTotalSoldQuantityFlow() ?: kotlinx.coroutines.flow.flowOf(0))
        .catch { emit(0) }
        .flowOn(Dispatchers.IO)
    val totalFireQuantity: Flow<Int> = (stockLogDao?.getTotalFireQuantityFlow() ?: kotlinx.coroutines.flow.flowOf(0))
        .catch { emit(0) }
        .flowOn(Dispatchers.IO)

    suspend fun insertInventoryAudit(audit: InventoryAuditEntity): Long = withContext(Dispatchers.IO) {
        inventoryAuditDao?.insertAudit(audit) ?: 0L
    }

    suspend fun insertInventoryAuditsBatch(audits: List<InventoryAuditEntity>) = withContext(Dispatchers.IO) {
        inventoryAuditDao?.insertAllAudits(audits)
    }

    suspend fun updateInventoryAudit(audit: InventoryAuditEntity) = withContext(Dispatchers.IO) {
        inventoryAuditDao?.updateAudit(audit)
    }

    suspend fun getAllInventoryAuditsDirect(): List<InventoryAuditEntity> = withContext(Dispatchers.IO) {
        inventoryAuditDao?.getAllAuditsList() ?: emptyList()
    }

    fun getAuditsByCategory(category: String): Flow<List<InventoryAuditEntity>> {
        return (inventoryAuditDao?.getAuditsByCategory(category) ?: kotlinx.coroutines.flow.emptyFlow()).flowOn(Dispatchers.IO)
    }

    suspend fun getLatestAuditForCategory(category: String): InventoryAuditEntity? = withContext(Dispatchers.IO) {
        inventoryAuditDao?.getLatestAuditForCategory(category)
    }

    suspend fun deleteInventoryAudit(audit: InventoryAuditEntity) = withContext(Dispatchers.IO) {
        inventoryAuditDao?.deleteAudit(audit)
    }

    suspend fun deleteInventoryAuditById(id: Int) = withContext(Dispatchers.IO) {
        inventoryAuditDao?.deleteAuditById(id)
    }

    suspend fun deleteAllInventoryAudits() = withContext(Dispatchers.IO) {
        inventoryAuditDao?.deleteAllAudits()
    }

    suspend fun insertStockLog(log: StockLog): Long = withContext(Dispatchers.IO) {
        stockLogDao?.insertLog(log) ?: 0L
    }

    suspend fun insertStockLogs(logs: List<StockLog>) = withContext(Dispatchers.IO) {
        stockLogDao?.insertAll(logs)
    }

    suspend fun getTotalSoldQuantityDirect(): Int = withContext(Dispatchers.IO) {
        stockLogDao?.getTotalSoldQuantity() ?: 0
    }

    suspend fun getTotalFireQuantityDirect(): Int = withContext(Dispatchers.IO) {
        stockLogDao?.getTotalFireQuantity() ?: 0
    }

    suspend fun getSktEntryCountForBarcode(code: String): Int = withContext(Dispatchers.IO) {
        stockLogDao?.getSktEntryCountForBarcode(code) ?: 0
    }

    suspend fun getAllStockLogsDirect(): List<StockLog> = withContext(Dispatchers.IO) {
        stockLogDao?.getAllLogsList() ?: emptyList()
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

    suspend fun getAllAdetselKayitlarList(): List<AdetselKayit> {
        return adetselDao?.getAllAdetselKayitlariDirect() ?: emptyList()
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
        val cleaned = if (product.kategori.isBlank()) {
            product.copy(kategori = com.example.util.CategoryClassifier.classifyCategory(product.urunAdi))
        } else {
            product
        }
        return productDao.updateProduct(cleaned)
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
        }
        return updatedCount
    }

    suspend fun updateProductStock(product: Product, newStok: Int) {
        val safeStok = maxOf(0, newStok)
        val updated = product.copy(stokAdedi = safeStok, sonKontrolTarihi = System.currentTimeMillis())
        productDao.updateProduct(updated)
    }

    suspend fun updateProductStock(productId: Int, newStok: Int) {
        val safeStok = maxOf(0, newStok)
        val now = System.currentTimeMillis()
        val prod = productDao.getProductById(productId)
        if (prod != null) {
            val updated = prod.copy(stokAdedi = safeStok, sonKontrolTarihi = now)
            productDao.updateProduct(updated)
        } else {
            productDao.updateStockAndControlDate(productId, safeStok, now)
        }
    }

    suspend fun getProductsByBarcode(barkod: String): List<Product> {
        return productDao.getProductsByBarcode(barkod)
    }

    suspend fun insertOrUpdateProduct(product: Product): Long {
        val cleaned = if (product.kategori.isBlank()) {
            product.copy(kategori = com.example.util.CategoryClassifier.classifyCategory(product.urunAdi))
        } else {
            product
        }
        val id = if (cleaned.id != 0) {
            productDao.updateProduct(cleaned)
            cleaned.id.toLong()
        } else {
            productDao.insertProduct(cleaned)
        }
        return id
    }

    suspend fun insertNewSktForBarcode(product: Product): Long {
        val cleaned = if (product.kategori.isBlank()) {
            product.copy(kategori = com.example.util.CategoryClassifier.classifyCategory(product.urunAdi))
        } else {
            product
        }
        return productDao.insertProduct(cleaned.copy(id = 0))
    }

    suspend fun insertProductsBatch(products: List<Product>) {
        if (products.isEmpty()) return
        val cleanList = products.map { p ->
            if (p.kategori.isBlank()) {
                p.copy(kategori = com.example.util.CategoryClassifier.classifyCategory(p.urunAdi))
            } else {
                p
            }
        }
        productDao.insertAll(cleanList)
    }

    suspend fun deleteProduct(product: Product) {
        productDao.deleteProduct(product)
    }

    suspend fun resetAllData() {
        productDao.deleteAllProducts()
        adetselDao?.deleteAllAdetselKayitlar()
        stockLogDao?.deleteAllLogs()
        inventoryAuditDao?.deleteAllAudits()
    }

    suspend fun createUnifiedBackupJson(context: Context): String {
        return DataBackupManager.createUnifiedBackupJson(
            context = context,
            productDao = productDao,
            adetselDao = adetselDao,
            stockLogDao = stockLogDao,
            inventoryAuditDao = inventoryAuditDao
        )
    }

    suspend fun saveLocalBackup(context: Context, tag: String = "manual"): File? {
        return DataBackupManager.saveAutoBackupToStorage(
            context = context,
            productDao = productDao,
            adetselDao = adetselDao,
            stockLogDao = stockLogDao,
            inventoryAuditDao = inventoryAuditDao,
            tag = tag
        )
    }

    fun getLocalBackups(context: Context): List<BackupMetadata> {
        return DataBackupManager.listLocalBackups(context)
    }

    suspend fun restoreFromJson(context: Context, jsonString: String, merge: Boolean = true): BackupRestoreResult {
        return DataBackupManager.restoreFromJson(
            context = context,
            jsonString = jsonString,
            productDao = productDao,
            adetselDao = adetselDao,
            stockLogDao = stockLogDao,
            inventoryAuditDao = inventoryAuditDao,
            mergeWithExisting = merge
        )
    }
}
