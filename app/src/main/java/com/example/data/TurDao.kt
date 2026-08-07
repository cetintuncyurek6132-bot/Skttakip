package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TurDao {
    @Query("SELECT * FROM tur_raporlari ORDER BY turTarihi DESC")
    fun getAllTurRaporlari(): Flow<List<TurRaporu>>

    @Query("SELECT * FROM tur_kontrol_kayitlari WHERE turId = :turId ORDER BY kontrolTarihi ASC")
    fun getKayitlarByTurId(turId: Int): Flow<List<TurKontrolKaydi>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTurRaporu(rapor: TurRaporu): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKontrolKayitlari(kayitlar: List<TurKontrolKaydi>)

    @Query("DELETE FROM tur_raporlari WHERE id = :id")
    suspend fun deleteTurRaporu(id: Int)

    @Query("DELETE FROM tur_kontrol_kayitlari WHERE turId = :turId")
    suspend fun deleteKontrolKayitlariByTurId(turId: Int)

    @Query("DELETE FROM tur_raporlari")
    suspend fun deleteAllTurRaporlari()

    @Query("DELETE FROM tur_kontrol_kayitlari")
    suspend fun deleteAllKontrolKayitlari()

    @Transaction
    suspend fun saveCompletedTour(
        rapor: TurRaporu,
        kayitlar: List<TurKontrolKaydi>
    ): Long {
        val turId = insertTurRaporu(rapor)
        val kayitlarWithTurId = kayitlar.map { it.copy(turId = turId.toInt()) }
        insertKontrolKayitlari(kayitlarWithTurId)
        return turId
    }
}
