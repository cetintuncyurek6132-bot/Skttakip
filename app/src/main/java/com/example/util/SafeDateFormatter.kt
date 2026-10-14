package com.example.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Proje genelinde thread-safe tarih formatlama.
 * SimpleDateFormat yerine java.time.format.DateTimeFormatter kullanır.
 * 
 * Tüm uygulamada SADECE bu object üzerinden tarih formatlanmalıdır.
 */
object SafeDateFormatter {

    private val TR_LOCALE = Locale.forLanguageTag("tr-TR")
    private val ZONE = ZoneId.systemDefault()

    // Yaygın pattern'ler için cache'li formatter'lar
    private val SHORT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", TR_LOCALE)
    private val SHORT_DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", TR_LOCALE)
    private val NUMERIC_DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", TR_LOCALE)
    private val LONG_DATE = DateTimeFormatter.ofPattern("dd MMMM yyyy", TR_LOCALE)
    private val NUMERIC_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy", TR_LOCALE)
    private val MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", TR_LOCALE)
    private val FILENAME_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd", TR_LOCALE)
    private val FILENAME_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss", TR_LOCALE)
    private val TIME_SHORT = DateTimeFormatter.ofPattern("HH:mm:ss", TR_LOCALE)

    /**
     * Millis timestamp'i verilen formatter ile formatlar.
     * Thread-safe, null-safe.
     */
    private fun format(millis: Long, formatter: DateTimeFormatter): String {
        if (millis <= 0L) return "-"
        return try {
            Instant.ofEpochMilli(millis).atZone(ZONE).format(formatter)
        } catch (_: Exception) {
            "-"
        }
    }

    /** "31/12/2026" */
    fun shortDate(millis: Long): String = format(millis, SHORT_DATE)

    /** "31/12/2026 14:30" */
    fun shortDateTime(millis: Long): String = format(millis, SHORT_DATE_TIME)

    /** "31.12.2026 14:30" */
    fun numericDateTime(millis: Long): String = format(millis, NUMERIC_DATE_TIME)

    /** "31 Aralık 2026" */
    fun longDate(millis: Long): String = format(millis, LONG_DATE)

    /** "31.12.2026" (Excel uyumlu) */
    fun numericDate(millis: Long): String = format(millis, NUMERIC_DATE)

    /** "Aralık 2026" (takvim başlığı) */
    fun monthYear(millis: Long): String = format(millis, MONTH_YEAR)

    /** "2026-12-31" (dosya adı) */
    fun filenameDate(millis: Long): String = format(millis, FILENAME_DATE)

    /** "2026-12-31_14-30-55" (dosya adı) */
    fun filenameDateTime(millis: Long): String = format(millis, FILENAME_DATE_TIME)

    /** "14:30:55" (saat) */
    fun timeShort(millis: Long): String = format(millis, TIME_SHORT)

    /**
     * "2026-12-31" formatındaki string'i millis'e çevirir.
     * Genelde dosya adları için kullanılır.
     */
    fun parseFilenameDate(dateStr: String): Long {
        if (dateStr.isBlank()) return 0L
        return try {
            LocalDate.parse(dateStr, FILENAME_DATE)
                .atStartOfDay(ZONE)
                .toInstant()
                .toEpochMilli()
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * "31.12.2026" formatındaki string'i millis'e çevirir.
     */
    fun parseNumericDate(dateStr: String): Long {
        if (dateStr.isBlank()) return 0L
        return try {
            LocalDate.parse(dateStr, NUMERIC_DATE)
                .atStartOfDay(ZONE)
                .toInstant()
                .toEpochMilli()
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Şu anki zamanı verilen formatter ile formatlar.
     */
    fun nowFilenameDate(): String = filenameDate(System.currentTimeMillis())
    fun nowFilenameDateTime(): String = filenameDateTime(System.currentTimeMillis())
    fun nowNumericDate(): String = numericDate(System.currentTimeMillis())
    fun nowShortDate(): String = shortDate(System.currentTimeMillis())
}
