package com.example

import com.example.data.Product
import com.example.data.StockLog
import com.example.ui.screens.analytics.FinancialKpiMetrics
import com.example.util.CategoryClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToLong

class ExampleUnitTest {

    @Test
    fun testFinancialMetricsCalculation() {
        val trLocale = Locale.forLanguageTag("tr-TR")
        val products = listOf(
            Product(id = 1, barkod = "8690001", urunKodu = "K1", urunAdi = "Süt", kategori = "Dolap", sktTarihi = System.currentTimeMillis() + 86400000L, stokAdedi = 10, fiyat = 35.0),
            Product(id = 2, barkod = "8690002", urunKodu = "K2", urunAdi = "Peynir", kategori = "Dolap", sktTarihi = System.currentTimeMillis() - 86400000L, stokAdedi = 4, fiyat = 85.0), // Expired
            Product(id = 3, barkod = "8690003", urunKodu = "K3", urunAdi = "Bisküvi", kategori = "Gıda", sktTarihi = System.currentTimeMillis() + 864000000L, stokAdedi = 20, fiyat = null) // No price
        )

        val stockLogs = listOf(
            StockLog(id = 1, barcode = "8690001", productName = "Süt", actionType = "SATIS", quantity = 8, sktDate = null, daysRemaining = 2),
            StockLog(id = 2, barcode = "8690002", productName = "Peynir", actionType = "FIRE", quantity = 2, sktDate = null, daysRemaining = -1),
            StockLog(id = 3, barcode = "8690003", productName = "Bisküvi", actionType = "SATIS", quantity = 5, sktDate = null, daysRemaining = 10)
        )

        val priceMap = products.filter { it.barkod.isNotBlank() && (it.fiyat ?: 0.0) > 0.0 }.associate { it.barkod to it.fiyat!! }

        // Kurtarılan Ciro: Süt (8 * 35 = 280) + Bisküvi (5 * 0 = 0) = 280
        val savedRevenue = stockLogs.filter { it.actionType == "SATIS" }.sumOf {
            it.quantity * (priceMap[it.barcode] ?: 0.0)
        }
        assertEquals(280.0, savedRevenue, 0.01)

        // Fire Maliyeti: Peynir Log (2 * 85 = 170) + Peynir Kalan Expired Stok (4 * 85 = 340) = 510
        val pastFireCost = stockLogs.filter { it.actionType == "FIRE" }.sumOf {
            it.quantity * (priceMap[it.barcode] ?: 0.0)
        }
        val expiredCost = products.filter { it.sktTarihi < System.currentTimeMillis() }.sumOf {
            it.stokAdedi * (it.fiyat ?: 0.0)
        }
        val totalFireCost = pastFireCost + expiredCost
        assertEquals(510.0, totalFireCost, 0.01)

        // Kurtarma Oranı: Toplam Satılan (8 + 5 = 13) / (13 + 2 = 15) -> 86.6% -> 87%
        val totalSold = stockLogs.filter { it.actionType == "SATIS" }.sumOf { it.quantity }
        val totalFire = stockLogs.filter { it.actionType == "FIRE" }.sumOf { it.quantity }
        val recoveryRate = ((totalSold.toDouble() / (totalSold + totalFire).toDouble()) * 100).toInt()
        assertEquals(86, recoveryRate) // 13 / 15 * 100 = 86.66 -> int 86

        val currencyFormatter = NumberFormat.getIntegerInstance(trLocale)
        val formattedSaved = "₺" + currencyFormatter.format(savedRevenue.roundToLong())
        assertEquals("₺280", formattedSaved)

        val metrics = FinancialKpiMetrics(
            recoveryRate = recoveryRate,
            savedRevenue = savedRevenue,
            fireRiskCost = totalFireCost,
            formattedSavedRevenue = formattedSaved,
            formattedFireRiskCost = "₺" + currencyFormatter.format(totalFireCost.roundToLong())
        )
        assertTrue(metrics.recoveryRate >= 75) // Emerald threshold
    }

    @Test
    fun testReyonClassification() {
        assertEquals("Dolap", CategoryClassifier.classifyCategory("Tavuk Göğsü 500g"))
        assertEquals("Dolap", CategoryClassifier.classifyCategory("Süzme Peynir"))
        assertEquals("Dolap", CategoryClassifier.classifyCategory("Dana Kıyma"))
        assertEquals("Dolap", CategoryClassifier.classifyCategory("Ayran 1L"))

        assertEquals("Gıda", CategoryClassifier.classifyCategory("Kuru Fasulye"))
        assertEquals("Gıda", CategoryClassifier.classifyCategory("Çikolatalı Gofret"))
        assertEquals("Gıda", CategoryClassifier.classifyCategory("Makarna 500g"))
    }

    @Test
    fun testObservationDaysPaydaCalculation() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val pastDate = today.minusDays(10)
        val passedDays = maxOf(1L, ChronoUnit.DAYS.between(pastDate, today))
        assertEquals(10L, passedDays)

        val salesCount = 20
        val dailyVelocity = salesCount.toDouble() / passedDays.toDouble()
        assertEquals(2.0, dailyVelocity, 0.001)
    }

    @Test
    fun testYellowTagImportance() {
        val prod = Product(id = 10, barkod = "8690999", urunKodu = "K99", urunAdi = "Yoğurt", kategori = "Dolap", sktTarihi = System.currentTimeMillis() + 86400000L, stokAdedi = 15, isImportant = false)
        val yellowTagged = prod.copy(isImportant = true)
        assertTrue(yellowTagged.isImportant)
    }
}
