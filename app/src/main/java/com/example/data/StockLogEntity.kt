package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_logs")
data class StockLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val barcode: String,
    val productName: String,
    val actionType: String, // "SKT_GIRIS", "SATIS", "FIRE"
    val quantity: Int,      // Kaç adet düşüldü veya girildi
    val sktDate: Long?,     // Ürünün o anki SKT tarihi
    val daysRemaining: Int?,// İşlem anında SKT'ye kaç gün kalmıştı?
    val timestamp: Long = System.currentTimeMillis() // İşlem anı
)
