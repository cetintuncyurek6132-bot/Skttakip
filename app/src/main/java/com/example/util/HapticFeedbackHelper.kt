package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Barkod tarama, ürün ekleme, SKT güncelleme ve kullanıcı işlemlerinde
 * hafif, tatmin edici dokunsal geri bildirim (haptic feedback) sağlayan yardımcı sınıf.
 */
object HapticFeedbackHelper {

    @Volatile
    private var cachedVibrator: Vibrator? = null
    @Volatile
    private var vibratorChecked = false
    private var lastHapticTime = 0L

    private fun getVibrator(context: Context): Vibrator? {
        if (!vibratorChecked) {
            try {
                val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.applicationContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                cachedVibrator = if (v?.hasVibrator() == true) v else null
            } catch (_: Exception) {
                cachedVibrator = null
            }
            vibratorChecked = true
        }
        return cachedVibrator
    }

    /**
     * İşlem başarılı olduğunda (barkod okuma, ürün ekleme, SKT kaydetme)
     * hafif ve net bir onay titreşimi tetikler.
     */
    fun triggerSuccessHaptic(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastHapticTime < 150L) return
        lastHapticTime = now

        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Hafif ve keskin tek dokunuş (40ms)
                vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(40)
            }
        } catch (_: Exception) {
            // Cihaz titreşim motoru veya izin hatalarını sessizce yakala
        }
    }

    /**
     * Çift vuruşlu hafif onay titreşimi (Örn: Hızlı QR Düzeltme veya parti ekleme)
     */
    fun triggerDoublePulseHaptic(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastHapticTime < 200L) return
        lastHapticTime = now

        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 30, 40, 35)
                val amplitudes = intArrayOf(0, 160, 0, 200)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 30, 40, 35), -1)
            }
        } catch (_: Exception) {
            // Sessiz yakalama
        }
    }

    /**
     * Hata veya uyarı durumlarında belirgin titreşim
     */
    fun triggerWarningHaptic(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastHapticTime < 250L) return
        lastHapticTime = now

        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 60, 50, 100)
                val amplitudes = intArrayOf(0, 220, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 60, 50, 100), -1)
            }
        } catch (_: Exception) {
            // Sessiz yakalama
        }
    }
}
