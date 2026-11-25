package com.example

import com.example.data.InventoryAuditEntity
import com.example.ui.viewmodel.calculateCalendarDaysBetween
import com.example.ui.viewmodel.calculateDailyAverageSales
import com.example.ui.viewmodel.calculateMonthEndProjection
import com.example.ui.viewmodel.shouldUseAuditDateDirectly
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class InventoryAuditMathTest {

    private val testZone = ZoneId.of("Europe/Istanbul")

    private fun millisFor(year: Int, month: Int, day: Int, hour: Int = 10): Long {
        return LocalDate.of(year, month, day)
            .atTime(hour, 0)
            .atZone(testZone)
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun dailyAverageSales_dividesRevenueDiffByActualCalendarDaysGap() {
        val prev = InventoryAuditEntity(
            id = 1,
            category = "GIDA",
            auditDateMillis = millisFor(2026, 11, 21),
            revenue = 10_000.0,
            fireCost = 100.0,
            createdAt = millisFor(2026, 11, 21),
            updatedAt = millisFor(2026, 11, 21)
        )
        val curr = InventoryAuditEntity(
            id = 2,
            category = "GIDA",
            auditDateMillis = millisFor(2026, 11, 24),
            revenue = 13_000.0,
            fireCost = 130.0,
            createdAt = millisFor(2026, 11, 24),
            updatedAt = millisFor(2026, 11, 24)
        )

        val (dailyAvg, days) = calculateDailyAverageSales(
            previousRecord = prev,
            currentRecord = curr,
            useAuditDateDirectly = true,
            zoneId = testZone
        )

        assertEquals(3L, days)
        assertEquals(1_000.0, dailyAvg, 0.001)
    }

    @Test
    fun sameDayOrReversedDates_clampElapsedDaysToAtLeastOne() {
        val day24Morning = millisFor(2026, 11, 24, hour = 9)
        val day24Evening = millisFor(2026, 11, 24, hour = 21)
        val day21 = millisFor(2026, 11, 21)

        assertEquals(1L, calculateCalendarDaysBetween(day24Morning, day24Evening, testZone))
        assertEquals(1L, calculateCalendarDaysBetween(day24Morning, day21, testZone))
    }

    @Test
    fun monthEndProjection_usesTotalCalendarDaysFromFirstToLatestRecord() {
        val first = InventoryAuditEntity(
            id = 1,
            category = "GIDA",
            auditDateMillis = millisFor(2026, 11, 16),
            revenue = 8_000.0,
            fireCost = 80.0,
            createdAt = millisFor(2026, 11, 16),
            updatedAt = millisFor(2026, 11, 16)
        )
        val middle = InventoryAuditEntity(
            id = 2,
            category = "GIDA",
            auditDateMillis = millisFor(2026, 11, 21),
            revenue = 13_000.0,
            fireCost = 130.0,
            createdAt = millisFor(2026, 11, 21),
            updatedAt = millisFor(2026, 11, 21)
        )
        val latest = InventoryAuditEntity(
            id = 3,
            category = "GIDA",
            auditDateMillis = millisFor(2026, 11, 24),
            revenue = 16_000.0,
            fireCost = 160.0,
            createdAt = millisFor(2026, 11, 24),
            updatedAt = millisFor(2026, 11, 24)
        )

        val projection = calculateMonthEndProjection(
            records = listOf(latest, first, middle),
            latestAudit = latest,
            zoneId = testZone
        )

        assertEquals(8L, projection.elapsedCalendarDays)
        assertEquals(30, projection.daysInMonth)
        assertEquals(2_000.0, projection.dailyAverageRevenue, 0.001)
        assertEquals(60_000.0, projection.projectedMonthEndRevenue, 0.001)
        assertEquals(600.0, projection.projectedMonthEndFireCost, 0.001)
    }

    @Test
    fun lockedWeeklyAuditDateMillis_fallsBackToUpdatedAtForCalendarGapAndProjection() {
        val lockedStart = millisFor(2026, 11, 16)
        val r1 = InventoryAuditEntity(
            id = 1,
            category = "DOLAP",
            auditDateMillis = lockedStart,
            revenue = 5_000.0,
            fireCost = 50.0,
            createdAt = millisFor(2026, 11, 16),
            updatedAt = millisFor(2026, 11, 16)
        )
        val r2 = InventoryAuditEntity(
            id = 2,
            category = "DOLAP",
            auditDateMillis = lockedStart,
            revenue = 10_000.0,
            fireCost = 100.0,
            createdAt = millisFor(2026, 11, 21),
            updatedAt = millisFor(2026, 11, 21)
        )
        val r3 = InventoryAuditEntity(
            id = 3,
            category = "DOLAP",
            auditDateMillis = lockedStart,
            revenue = 16_000.0,
            fireCost = 160.0,
            createdAt = millisFor(2026, 11, 24),
            updatedAt = millisFor(2026, 11, 24)
        )

        val records = listOf(r1, r2, r3)
        val useAuditDate = shouldUseAuditDateDirectly(records, testZone)
        assertFalse(useAuditDate)

        val (dailyAvg, days) = calculateDailyAverageSales(
            previousRecord = r2,
            currentRecord = r3,
            useAuditDateDirectly = useAuditDate,
            zoneId = testZone
        )
        assertEquals(3L, days)
        assertEquals(2_000.0, dailyAvg, 0.001)

        val projection = calculateMonthEndProjection(records, r3, testZone)
        assertEquals(8L, projection.elapsedCalendarDays)
        assertEquals(60_000.0, projection.projectedMonthEndRevenue, 0.001)
        assertTrue(shouldUseAuditDateDirectly(listOf(r1.copy(auditDateMillis = millisFor(2026, 11, 16)), r3.copy(auditDateMillis = millisFor(2026, 11, 24))), testZone))
    }

    @Test
    fun combinedInventorySummary_sumsLatestPeriodicAndContinuous_andChecksGeneralTargetOnly() {
        val audits = listOf(
            // GIDA: eski kayıt ve en yeni kayıt
            InventoryAuditEntity(
                id = 1,
                category = "GIDA",
                auditDateMillis = millisFor(2026, 11, 10),
                revenue = 5_000.0,
                fireCost = 500.0,
                targetRatio = 1.0,
                updatedAt = millisFor(2026, 11, 10)
            ),
            InventoryAuditEntity(
                id = 2,
                category = "GIDA",
                auditDateMillis = millisFor(2026, 11, 24),
                revenue = 40_000.0,
                fireCost = 400.0,
                targetRatio = 1.0,
                updatedAt = millisFor(2026, 11, 24)
            ),
            InventoryAuditEntity(
                id = 3,
                category = "DOLAP",
                auditDateMillis = millisFor(2026, 11, 24),
                revenue = 30_000.0,
                fireCost = 300.0,
                targetRatio = 1.0,
                updatedAt = millisFor(2026, 11, 24)
            ),
            InventoryAuditEntity(
                id = 4,
                category = "GIDA DIŞI",
                auditDateMillis = millisFor(2026, 11, 24),
                revenue = 10_000.0,
                fireCost = 100.0,
                targetRatio = 1.0,
                updatedAt = millisFor(2026, 11, 24)
            ),
            // Sürekli kategorilerden biri kendi hedefini aşsa bile (örn. EKMEK %5)
            InventoryAuditEntity(
                id = 5,
                category = "ET-TAVUK",
                auditDateMillis = millisFor(2026, 11, 24),
                revenue = 10_000.0,
                fireCost = 200.0,
                targetRatio = 3.0,
                updatedAt = millisFor(2026, 11, 24)
            ),
            InventoryAuditEntity(
                id = 6,
                category = "EKMEK",
                auditDateMillis = millisFor(2026, 11, 24),
                revenue = 5_000.0,
                fireCost = 250.0,
                targetRatio = 1.0,
                updatedAt = millisFor(2026, 11, 24)
            ),
            InventoryAuditEntity(
                id = 7,
                category = "MEYVE-SEBZE",
                auditDateMillis = millisFor(2026, 11, 24),
                revenue = 3_000.0,
                fireCost = 150.0,
                targetRatio = 6.0,
                updatedAt = millisFor(2026, 11, 24)
            ),
            InventoryAuditEntity(
                id = 8,
                category = "DONUK MEYVE-SEBZE",
                auditDateMillis = millisFor(2026, 11, 24),
                revenue = 2_000.0,
                fireCost = 100.0,
                targetRatio = 2.0,
                updatedAt = millisFor(2026, 11, 24)
            )
        )
        // Toplam Ciro = 40k + 30k + 10k + 10k + 5k + 3k + 2k = 100_000
        // Toplam Fire = 400 + 300 + 100 + 200 + 250 + 150 + 100 = 1_500 -> Oran = %1.50
        val underTarget = com.example.ui.viewmodel.computeCombinedInventorySummary(
            auditsList = audits,
            combinedTargetRatio = 2.0
        )
        assertEquals(100_000.0, underTarget.totalRevenue, 0.001)
        assertEquals(1_500.0, underTarget.totalFireCost, 0.001)
        assertEquals(1.5, underTarget.combinedRatio ?: -1.0, 0.001)
        assertFalse(underTarget.isOverTarget)

        val equalTarget = com.example.ui.viewmodel.computeCombinedInventorySummary(
            auditsList = audits,
            combinedTargetRatio = 1.5
        )
        assertFalse(equalTarget.isOverTarget)

        val overTarget = com.example.ui.viewmodel.computeCombinedInventorySummary(
            auditsList = audits,
            combinedTargetRatio = 1.4
        )
        assertTrue(overTarget.isOverTarget)
    }
}
