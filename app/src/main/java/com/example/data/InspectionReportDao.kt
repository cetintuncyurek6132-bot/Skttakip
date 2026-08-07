package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InspectionReportDao {
    @Query("SELECT * FROM inspection_reports ORDER BY tarih DESC")
    fun getAllReports(): Flow<List<InspectionReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: InspectionReport): Long

    @Query("DELETE FROM inspection_reports WHERE id = :id")
    suspend fun deleteReportById(id: Int)

    @Query("DELETE FROM inspection_reports")
    suspend fun deleteAllReports()
}
