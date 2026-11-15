package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar
import java.util.Locale

fun computeAuditPeriod(timestampMillis: Long = System.currentTimeMillis()): String {
    val cal = Calendar.getInstance(Locale.forLanguageTag("tr-TR")).apply {
        firstDayOfWeek = Calendar.MONDAY
        minimalDaysInFirstWeek = 4
        timeInMillis = if (timestampMillis > 0L) timestampMillis else System.currentTimeMillis()
    }
    val year = cal.get(Calendar.YEAR)
    val week = cal.get(Calendar.WEEK_OF_YEAR)
    return String.format(Locale.US, "%04d-W%02d", year, week)
}

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
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val auditPeriod: String = computeAuditPeriod(updatedAt)
)
