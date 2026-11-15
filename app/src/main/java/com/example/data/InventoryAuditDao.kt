package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryAuditDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(audit: InventoryAuditEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAudits(audits: List<InventoryAuditEntity>)

    @Update
    suspend fun updateAudit(audit: InventoryAuditEntity)

    @Delete
    suspend fun deleteAudit(audit: InventoryAuditEntity)

    @Query("SELECT * FROM inventory_audits ORDER BY auditDateMillis DESC, createdAt DESC")
    fun getAllAuditsFlow(): Flow<List<InventoryAuditEntity>>

    @Query("SELECT * FROM inventory_audits ORDER BY auditDateMillis DESC, createdAt DESC")
    suspend fun getAllAuditsList(): List<InventoryAuditEntity>

    @Query("SELECT * FROM inventory_audits WHERE category = :category ORDER BY auditDateMillis DESC, createdAt DESC LIMIT 1")
    suspend fun getLatestAuditForCategory(category: String): InventoryAuditEntity?

    @Query("DELETE FROM inventory_audits WHERE id = :id")
    suspend fun deleteAuditById(id: Int)

    @Query("DELETE FROM inventory_audits")
    suspend fun deleteAllAudits()
}
