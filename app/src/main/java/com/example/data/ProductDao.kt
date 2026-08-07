package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY sktTarihi ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY sktTarihi ASC")
    suspend fun getAllProductsList(): List<Product>

    @Query("SELECT * FROM products WHERE barkod = :barkod LIMIT 1")
    suspend fun getProductByBarcode(barkod: String): Product?

    @Query("SELECT * FROM products WHERE barkod = :barkod ORDER BY sktTarihi ASC")
    suspend fun getProductsByBarcode(barkod: String): List<Product>

    @Query("SELECT * FROM products WHERE kategori = :kategori ORDER BY sktTarihi ASC")
    fun getProductsByCategory(kategori: String): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product): Int

    @Query("UPDATE products SET stokAdedi = :newStok, sonKontrolTarihi = :now WHERE id = :productId")
    suspend fun updateStockAndControlDate(productId: Int, newStok: Int, now: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteProduct(product: Product): Int

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()
}
