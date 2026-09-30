package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StockLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: StockLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<StockLog>)

    @Query("SELECT * FROM stock_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<StockLog>>

    @Query("SELECT * FROM stock_logs ORDER BY timestamp DESC")
    suspend fun getAllLogsList(): List<StockLog>

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM stock_logs WHERE actionType = 'SATIS'")
    fun getTotalSoldQuantityFlow(): Flow<Int>

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM stock_logs WHERE actionType = 'SATIS'")
    suspend fun getTotalSoldQuantity(): Int

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM stock_logs WHERE actionType = 'FIRE'")
    fun getTotalFireQuantityFlow(): Flow<Int>

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM stock_logs WHERE actionType = 'FIRE'")
    suspend fun getTotalFireQuantity(): Int

    @Query("SELECT COUNT(*) FROM stock_logs WHERE (barcode = :code OR barcode = :barcode) AND actionType = 'SKT_GIRIS'")
    suspend fun getSktEntryCountForBarcode(code: String, barcode: String = code): Int

    @Query("SELECT COUNT(*) FROM stock_logs WHERE actionType = 'SKT_GIRIS'")
    fun getTotalSktEntryCountFlow(): Flow<Int>

    @Query("SELECT * FROM stock_logs WHERE actionType IN ('SATIS', 'FIRE') ORDER BY timestamp DESC")
    fun getSalesAndFireLogsFlow(): Flow<List<StockLog>>

    @Query("SELECT * FROM stock_logs WHERE barcode = :code ORDER BY timestamp DESC")
    fun getLogsForBarcodeFlow(code: String): Flow<List<StockLog>>

    @Query("DELETE FROM stock_logs")
    suspend fun deleteAllLogs()
}
