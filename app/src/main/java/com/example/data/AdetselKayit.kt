package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "adetsel_kayitlar")
data class AdetselKayit(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val productId: Int = 0,
    val urunAdi: String = "",
    val urunKodu: String = "",
    val barkod: String = "",
    val kategori: String = "",
    val eklenmeTarihi: Long = System.currentTimeMillis(),
    val yapildiMi: Boolean = false,
    val sayimSonucu: String = "", // "TAM", "EKSIK", "FAZLA"
    val beklenenAdet: Int = 0,
    val sayilanAdet: Int = 0,
    val farkAdet: Int = 0, // sayilanAdet - beklenenAdet
    val notlar: String = "",
    val islemTarihi: Long = 0L
) {
    fun getFormattedEklenmeTarihi(): String {
        return try {
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR"))
            sdf.format(Date(eklenmeTarihi))
        } catch (e: Exception) {
            "-"
        }
    }

    fun getFormattedIslemTarihi(): String {
        if (islemTarihi <= 0L) return "-"
        return try {
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR"))
            sdf.format(Date(islemTarihi))
        } catch (e: Exception) {
            "-"
        }
    }

    fun getDisplayCode(): String {
        return when {
            urunKodu.isNotBlank() -> urunKodu
            barkod.isNotBlank() -> barkod
            else -> "-"
        }
    }
}
