package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AdetselDao {

    @Query("SELECT * FROM adetsel_kayitlar ORDER BY eklenmeTarihi DESC")
    fun getAllAdetselKayitlari(): Flow<List<AdetselKayit>>

    @Query("SELECT * FROM adetsel_kayitlar ORDER BY eklenmeTarihi DESC")
    suspend fun getAllAdetselKayitlariDirect(): List<AdetselKayit>

    @Query("SELECT * FROM adetsel_kayitlar WHERE yapildiMi = 0 ORDER BY eklenmeTarihi DESC")
    fun getYapilacakKayitlar(): Flow<List<AdetselKayit>>

    @Query("SELECT * FROM adetsel_kayitlar WHERE yapildiMi = 0")
    suspend fun getPendingKayitlarList(): List<AdetselKayit>

    @Query("SELECT * FROM adetsel_kayitlar WHERE yapildiMi = 1 ORDER BY islemTarihi DESC")
    fun getYapildiKayitlar(): Flow<List<AdetselKayit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdetselKayit(kayit: AdetselKayit): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdetselKayitlarBatch(kayitlar: List<AdetselKayit>)

    @Update
    suspend fun updateAdetselKayit(kayit: AdetselKayit)

    @Delete
    suspend fun deleteAdetselKayit(kayit: AdetselKayit)

    @Query("DELETE FROM adetsel_kayitlar WHERE id = :id")
    suspend fun deleteAdetselKayitById(id: Int)

    @Query("DELETE FROM adetsel_kayitlar WHERE yapildiMi = 1")
    suspend fun clearCompletedAdetselKayitlar()

    @Query("DELETE FROM adetsel_kayitlar")
    suspend fun deleteAllAdetselKayitlar()
}
