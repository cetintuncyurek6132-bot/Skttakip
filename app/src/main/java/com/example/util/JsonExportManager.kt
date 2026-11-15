package com.example.util

import android.content.Context
import com.example.data.AdetselDao
import com.example.data.BackupRestoreResult
import com.example.data.DataBackupManager
import com.example.data.InventoryAuditDao
import com.example.data.ProductDao
import com.example.data.StockLogDao

object JsonExportManager {

    suspend fun exportFullBackup(
        context: Context,
        productDao: ProductDao,
        adetselDao: AdetselDao? = null,
        stockLogDao: StockLogDao? = null,
        inventoryAuditDao: InventoryAuditDao? = null
    ): String {
        return DataBackupManager.createUnifiedBackupJson(
            context = context,
            productDao = productDao,
            adetselDao = adetselDao,
            stockLogDao = stockLogDao,
            inventoryAuditDao = inventoryAuditDao
        )
    }

    suspend fun restoreFullBackup(
        context: Context,
        jsonString: String,
        productDao: ProductDao,
        adetselDao: AdetselDao? = null,
        stockLogDao: StockLogDao? = null,
        inventoryAuditDao: InventoryAuditDao? = null,
        mergeWithExisting: Boolean = true
    ): BackupRestoreResult {
        return DataBackupManager.restoreFromJson(
            context = context,
            jsonString = jsonString,
            productDao = productDao,
            adetselDao = adetselDao,
            stockLogDao = stockLogDao,
            inventoryAuditDao = inventoryAuditDao,
            mergeWithExisting = mergeWithExisting
        )
    }
}
