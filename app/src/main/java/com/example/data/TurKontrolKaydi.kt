package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tur_kontrol_kayitlari")
data class TurKontrolKaydi(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val turId: Int = 0,
    val productId: Int,
    val urunAdiSnapshot: String,
    val barkodSnapshot: String = "",
    val durum: String, // "SATILDI", "FIRE", "NOTR"
    val islemAdedi: Int = 0,
    val kontrolTarihi: Long = System.currentTimeMillis()
)
