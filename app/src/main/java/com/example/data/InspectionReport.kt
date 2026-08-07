package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inspection_reports")
data class InspectionReport(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tarih: Long = System.currentTimeMillis(),
    val reyonAdi: String,
    val tarananUrunSayisi: Int,
    val suresiGecenSayisi: Int,
    val kritikUrunSayisi: Int,
    val fireTutari: Double = 0.0
)
