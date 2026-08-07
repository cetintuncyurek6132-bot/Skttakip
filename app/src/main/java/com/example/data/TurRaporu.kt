package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tur_raporlari")
data class TurRaporu(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val turTarihi: Long = System.currentTimeMillis(),
    val hedefReyon: String,
    val toplamUrunSayisi: Int,
    val satilanUrunSayisi: Int,
    val toplamSatilanAdet: Int,
    val fireUrunSayisi: Int,
    val toplamFireAdet: Int,
    val notrUrunSayisi: Int,
    val tamamlandiMi: Boolean = true
)
