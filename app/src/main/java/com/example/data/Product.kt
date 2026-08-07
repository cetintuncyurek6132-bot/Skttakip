package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Calendar
import java.util.Locale

fun String.normalizeForSearch(): String {
    if (this.isBlank()) return ""

    var normalized = this.lowercase(Locale("tr", "TR"))
        .replace('ı', 'i')
        .replace('i', 'i')
        .replace('ö', 'o')
        .replace('ü', 'u')
        .replace('ç', 'c')
        .replace('ş', 's')
        .replace('ğ', 'g')

    normalized = normalized
        .replace("1 litre", "1l 1lt 1litre 1000ml")
        .replace("1 lt", "1l 1lt 1000ml")
        .replace("1.5 litre", "1.5l 1.5lt 1.5litre 1500ml")
        .replace("2.5 litre", "2.5l 2.5lt 2.5litre 2500ml")
        .replace("2 litre", "2l 2lt 2litre 2000ml")
        .replace("500 ml", "500ml 0.5l")
        .replace("330 ml", "330ml 0.33l")
        .replace("250 ml", "250ml 0.25l")
        .replace("200 ml", "200ml 0.2l")
        .replace("1 kg", "1kg 1000g 1000gr")
        .replace("500 gr", "500gr 500g")
        .replace("250 gr", "250gr 250g")

    return normalized
}

fun Product.matchesSearchQuery(rawQuery: String): Boolean {
    val q = rawQuery.trim()
    if (q.isEmpty()) return true

    val targetText = "${this.urunAdi} ${this.barkod} ${this.urunKodu} ${this.kategori}".normalizeForSearch()

    val expandedTarget = buildString {
        append(targetText)
        if (targetText.contains("1.5l") || targetText.contains("1.5 lt")) append(" 1.5 1,5 1500ml 1.5litre 1.5lt")
        if (targetText.contains("2.5l") || targetText.contains("2.5 lt")) append(" 2.5 2,5 2500ml 2.5litre 2.5lt")
        if (targetText.contains("1l") || targetText.contains("1 lt")) append(" 1 1litre 1lt 1000ml")
        if (targetText.contains("500ml") || targetText.contains("500 gr")) append(" 500 0.5l 500g 500gr")
        if (targetText.contains("330ml")) append(" 330 0.33l")
        if (targetText.contains("250ml") || targetText.contains("250 gr")) append(" 250 0.25l 250g 250gr")
        if (targetText.contains("coca")) append(" kola cola coka")
        if (targetText.contains("kola")) append(" cola coca")
    }

    val queryTokens = q.normalizeForSearch()
        .replace(',', '.')
        .split("\\s+".toRegex())
        .filter { it.isNotBlank() }

    if (queryTokens.isEmpty()) return true

    return queryTokens.all { token ->
        expandedTarget.contains(token) ||
        (token.contains('.') && expandedTarget.contains(token.replace('.', ','))) ||
        (token.contains(',') && expandedTarget.contains(token.replace(',', '.')))
    }
}

@Entity(
    tableName = "products",
    indices = [Index(value = ["barkod"], unique = false)]
)
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val barkod: String,
    val urunKodu: String,
    val urunAdi: String,
    val kategori: String,
    val sktTarihi: Long, // Timestamp in milliseconds
    val stokAdedi: Int,
    val eklenmeTarihi: Long = System.currentTimeMillis(),
    val isImportant: Boolean = false,
    val sonKontrolTarihi: Long = 0L,
    val fiyat: Double? = null
) {
    fun getFormattedPrice(): String? {
        val f = fiyat ?: return null
        if (f <= 0.0) return null
        return "₺${String.format(Locale("tr", "TR"), "%.2f", f)}"
    }
    fun getRemainingDays(): Long {
        if (sktTarihi <= 0L) return 9999L

        val todayMidnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val sktMidnight = Calendar.getInstance().apply {
            timeInMillis = sktTarihi
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val diffMillis = sktMidnight - todayMidnight
        return diffMillis / (1000L * 60 * 60 * 24)
    }

    fun getExpiryStatus(): ExpiryStatus {
        if (sktTarihi <= 0L) return ExpiryStatus.NORMAL
        val days = getRemainingDays()
        return when {
            days <= 0 -> ExpiryStatus.EXPIRED
            days <= 7 -> ExpiryStatus.CRITICAL
            days <= 30 -> ExpiryStatus.SOON
            else -> ExpiryStatus.NORMAL
        }
    }
}

enum class ExpiryStatus(val label: String) {
    EXPIRED("Süresi Geçen"),
    CRITICAL("Kritik"),
    SOON("Yakın"),
    NORMAL("Normal")
}
