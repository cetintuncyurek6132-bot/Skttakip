package com.example.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import java.util.Calendar

/**
 * Reaktif gün başlangıcı (todayMidnight).
 * 
 * Gece yarısı geçtiğinde otomatik olarak yeni değer yayar.
 * Tüm ekranlar bunu `val todayMidnight by rememberTodayMidnight()` 
 * şeklinde kullanmalıdır.
 * 
 * @param refreshIntervalMillis Kontrol sıklığı (default 60 sn).
 *        60 saniyede bir kontrol etmek pil dostu ve yeterlidir.
 */
@Composable
fun rememberTodayMidnight(
    refreshIntervalMillis: Long = 60_000L
): State<Long> {
    return produceState(
        initialValue = getTodayMidnight()
    ) {
        while (true) {
            delay(refreshIntervalMillis)
            val current = getTodayMidnight()
            if (current != value) {
                value = current
            }
        }
    }
}

/**
 * Bugünün gece yarısı timestamp'ini hesaplar (statik).
 */
fun getTodayMidnight(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
