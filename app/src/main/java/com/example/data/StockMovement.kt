package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "stock_movements")
data class StockMovement(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val barkod: String,
    val urunAdi: String,
    val adet: Int,
    val islemTuru: String, // "SATIS" or "FIRE"
    val tarih: Long = System.currentTimeMillis()
)

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements ORDER BY tarih DESC")
    fun getAllStockMovements(): Flow<List<StockMovement>>

    @Query("SELECT * FROM stock_movements ORDER BY tarih DESC")
    suspend fun getAllStockMovementsList(): List<StockMovement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: StockMovement): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movements: List<StockMovement>)

    @Query("DELETE FROM stock_movements")
    suspend fun deleteAllMovements()
}
