package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_audits")
data class InventoryAuditEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val category: String, // "GIDA", "DOLAP", "GIDA DIŞI"
    val auditDateMillis: Long,
    val revenue: Double = 0.0,
    val fireCost: Double = 0.0,
    val targetRatio: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
