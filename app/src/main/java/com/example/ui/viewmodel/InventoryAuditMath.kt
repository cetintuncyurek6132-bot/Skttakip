package com.example.ui.viewmodel

import com.example.data.InventoryAuditEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class MonthEndProjectionResult(
    val elapsedCalendarDays: Long,
    val daysInMonth: Int,
    val dailyAverageRevenue: Double,
    val dailyAverageFireCost: Double,
    val projectedMonthEndRevenue: Double,
    val projectedMonthEndFireCost: Double,
    val projectedMonthEndRatio: Double
)

fun millisToLocalDate(
    millis: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): LocalDate {
    val safeMillis = if (millis > 0L) millis else System.currentTimeMillis()
    return Instant.ofEpochMilli(safeMillis).atZone(zoneId).toLocalDate()
}

/**
 * İki kayıt arasındaki gerçek takvim günü farkını hesaplar.
 * Aynı gün veya ters sıra durumunda bölme hatasını önlemek için en az 1 gün döner.
 */
fun calculateCalendarDaysBetween(
    previousMillis: Long,
    currentMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): Long {
    if (previousMillis <= 0L || currentMillis <= 0L) return 1L
    val prevDate = Instant.ofEpochMilli(previousMillis).atZone(zoneId).toLocalDate()
    val currDate = Instant.ofEpochMilli(currentMillis).atZone(zoneId).toLocalDate()
    return ChronoUnit.DAYS.between(prevDate, currDate).coerceAtLeast(1L)
}

/**
 * Parçalı Envanter haftalık veri girişlerinde `auditDateMillis` sayım başlangıç tarihine sabitlenmiş olabilir;
 * diğer akışlarda `auditDateMillis` doğrudan her kaydın kendi tarihidir.
 * Tarihler birbirinden farklı günlere sahipse `auditDateMillis` kullanılır, aksi halde `updatedAt`/`createdAt` esas alınır.
 */
fun shouldUseAuditDateDirectly(
    audits: List<InventoryAuditEntity>,
    zoneId: ZoneId = ZoneId.systemDefault()
): Boolean {
    if (audits.isEmpty()) return true
    val distinctAuditDays = audits
        .map { millisToLocalDate(it.auditDateMillis, zoneId) }
        .distinct()
        .size
    val distinctUpdateDays = audits
        .map {
            val updateTs = when {
                it.updatedAt > 0L -> it.updatedAt
                it.createdAt > 0L -> it.createdAt
                else -> it.auditDateMillis
            }
            millisToLocalDate(updateTs, zoneId)
        }
        .distinct()
        .size
    return distinctAuditDays > 1 && distinctAuditDays >= distinctUpdateDays
}

fun resolveAuditRecordTimestamp(
    audit: InventoryAuditEntity,
    useAuditDateDirectly: Boolean
): Long {
    return if (useAuditDateDirectly) {
        when {
            audit.auditDateMillis > 0L -> audit.auditDateMillis
            audit.updatedAt > 0L -> audit.updatedAt
            else -> audit.createdAt
        }
    } else {
        when {
            audit.updatedAt > 0L -> audit.updatedAt
            audit.createdAt > 0L -> audit.createdAt
            else -> audit.auditDateMillis
        }
    }
}

fun chronologicalAuditComparator(useAuditDateDirectly: Boolean): Comparator<InventoryAuditEntity> {
    return compareBy<InventoryAuditEntity> { resolveAuditRecordTimestamp(it, useAuditDateDirectly) }
        .thenBy { it.auditDateMillis }
        .thenBy { it.updatedAt }
        .thenBy { it.createdAt }
        .thenBy { it.id }
}

/**
 * Önceki kayıt ile mevcut kayıt arasındaki gerçek takvim günü farkına bölerek
 * günlük ortalama satış farkını hesaplar:
 * gunlukOrtalamaSatis = (currentRecord.revenue - previousRecord.revenue) / gecenGun
 */
fun calculateDailyAverageSales(
    previousRecord: InventoryAuditEntity,
    currentRecord: InventoryAuditEntity,
    useAuditDateDirectly: Boolean = true,
    zoneId: ZoneId = ZoneId.systemDefault()
): Pair<Double, Long> {
    val prevMillis = resolveAuditRecordTimestamp(previousRecord, useAuditDateDirectly)
    val currMillis = resolveAuditRecordTimestamp(currentRecord, useAuditDateDirectly)
    val gecenGun = calculateCalendarDaysBetween(prevMillis, currMillis, zoneId)
    val farkCiro = currentRecord.revenue.coerceAtLeast(0.0) - previousRecord.revenue.coerceAtLeast(0.0)
    val gunlukOrtalamaSatis = farkCiro / gecenGun.toDouble()
    return gunlukOrtalamaSatis to gecenGun
}

/**
 * Ay sonu projeksiyonunu ilk girilen veriden en son girilen veriye kadar olan
 * gerçek takvim günü farkı üzerinden hesaplar (araya boşluk girse bile).
 * Örn: İlk veri 16'sında, son veri 24'ünde ise geçen gün = 8.
 */
fun calculateMonthEndProjection(
    records: List<InventoryAuditEntity>,
    latestAudit: InventoryAuditEntity,
    zoneId: ZoneId = ZoneId.systemDefault()
): MonthEndProjectionResult {
    val pool = if (records.isNotEmpty()) records else listOf(latestAudit)
    val useAuditDate = shouldUseAuditDateDirectly(pool, zoneId)
    val chronological = pool.sortedWith(chronologicalAuditComparator(useAuditDate))

    val firstRecord = chronological.first()
    val lastRecord = chronological.last()

    val firstMillis = resolveAuditRecordTimestamp(firstRecord, useAuditDate)
    val lastMillis = resolveAuditRecordTimestamp(lastRecord, useAuditDate)

    val firstDate = millisToLocalDate(firstMillis, zoneId)
    val latestDate = millisToLocalDate(lastMillis, zoneId)

    val gecenGun = ChronoUnit.DAYS.between(firstDate, latestDate).coerceAtLeast(1L)
    val ayinGunu = latestDate.lengthOfMonth().coerceAtLeast(1)

    val ciro = latestAudit.revenue.coerceAtLeast(0.0)
    val fire = latestAudit.fireCost.coerceAtLeast(0.0)

    val gunlukOrtCiro = ciro / gecenGun.toDouble()
    val gunlukOrtFire = fire / gecenGun.toDouble()
    val tahminiAySonuCiro = gunlukOrtCiro * ayinGunu.toDouble()
    val tahminiAySonuFire = gunlukOrtFire * ayinGunu.toDouble()
    val tahminiAySonuOrani = if (tahminiAySonuCiro > 0.0) {
        (tahminiAySonuFire / tahminiAySonuCiro) * 100.0
    } else if (tahminiAySonuFire > 0.0) {
        100.0
    } else {
        0.0
    }

    return MonthEndProjectionResult(
        elapsedCalendarDays = gecenGun,
        daysInMonth = ayinGunu,
        dailyAverageRevenue = gunlukOrtCiro,
        dailyAverageFireCost = gunlukOrtFire,
        projectedMonthEndRevenue = tahminiAySonuCiro,
        projectedMonthEndFireCost = tahminiAySonuFire,
        projectedMonthEndRatio = tahminiAySonuOrani
    )
}
