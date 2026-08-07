package com.example.data

import com.example.sync.CloudSyncManager
import kotlinx.coroutines.flow.Flow

class ProductRepository(
    private val productDao: ProductDao,
    private val reportDao: InspectionReportDao,
    private val turDao: TurDao? = null
) {
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allReports: Flow<List<InspectionReport>> = reportDao.getAllReports()
    val allTurRaporlari: Flow<List<TurRaporu>> = turDao?.getAllTurRaporlari() ?: kotlinx.coroutines.flow.emptyFlow()

    suspend fun getProductByBarcode(barkod: String): Product? {
        return productDao.getProductByBarcode(barkod)
    }

    suspend fun getProductListDirect(): List<Product> {
        return productDao.getAllProductsList()
    }

    suspend fun updateProductStock(productId: Int, newStok: Int) {
        productDao.updateStockAndControlDate(productId, newStok, System.currentTimeMillis())
        val updatedProd = productDao.getProductByBarcode("") // Get by id if needed
        allProducts // Trigger flow
        // Fetch product to sync
        scopeSyncProduct(productId)
    }

    private suspend fun scopeSyncProduct(productId: Int) {
        val list = productDao.getAllProductsList()
        val prod = list.find { it.id == productId }
        if (prod != null) {
            CloudSyncManager.syncProductToCloud(prod)
        }
    }

    suspend fun saveTourReport(rapor: TurRaporu, kayitlar: List<TurKontrolKaydi>): Long {
        val res = turDao?.saveCompletedTour(rapor, kayitlar) ?: 0L
        CloudSyncManager.syncTourReportToCloud(rapor, kayitlar)
        return res
    }

    suspend fun deleteTurRaporu(id: Int) {
        turDao?.deleteTurRaporu(id)
        turDao?.deleteKontrolKayitlariByTurId(id)
    }

    suspend fun clearAllTurRaporlari() {
        turDao?.deleteAllTurRaporlari()
        turDao?.deleteAllKontrolKayitlari()
    }

    fun getKayitlarByTurId(turId: Int): Flow<List<TurKontrolKaydi>> {
        return turDao?.getKayitlarByTurId(turId) ?: kotlinx.coroutines.flow.emptyFlow()
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
}
