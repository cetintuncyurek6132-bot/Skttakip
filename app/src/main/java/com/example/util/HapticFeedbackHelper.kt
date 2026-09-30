package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Barkod tarama, ürün ekleme, SKT güncelleme ve kullanıcı işlemlerinde
 * ses ve dokunsal geri bildirim (haptic & audio feedback) sağlayan yardımcı sınıf.
 */
object HapticFeedbackHelper {

    @Volatile
    private var cachedVibrator: Vibrator? = null
    @Volatile
    private var vibratorChecked = false
    private var lastHapticTime = 0L

    @Volatile
    private var cachedToneGenerator: ToneGenerator? = null

    private fun getToneGenerator(): ToneGenerator? {
        if (cachedToneGenerator == null) {
            synchronized(this) {
                if (cachedToneGenerator == null) {
                    cachedToneGenerator = try {
                        ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                    } catch (_: Exception) {
                        try {
                            ToneGenerator(AudioManager.STREAM_SYSTEM, 100)
                        } catch (_: Exception) {
                            try {
                                ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                            } catch (_: Exception) {
                                null
                            }
                        }
                    }
                }
            }
        }
        return cachedToneGenerator
    }

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
     * Başarılı işlem onay sesi çalar (Bip)
     */
    fun playSuccessTone() {
        try {
            getToneGenerator()?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (_: Exception) {}
    }

    /**
     * Hata / uyarı sesi çalar
     */
    fun playWarningTone() {
        try {
            getToneGenerator()?.startTone(ToneGenerator.TONE_PROP_NACK, 160)
        } catch (_: Exception) {}
    }

    /**
     * Çift vuruşlu işlem sesi
     */
    fun playDoubleTone() {
        try {
            getToneGenerator()?.startTone(ToneGenerator.TONE_PROP_BEEP2, 140)
        } catch (_: Exception) {}
    }

    /**
     * İşlem başarılı olduğunda (barkod okuma, ürün ekleme, SKT kaydetme)
     * ses ve onay titreşimi tetikler.
     */
    fun triggerSuccessHaptic(context: Context, playSound: Boolean = true) {
        if (playSound) {
            playSuccessTone()
        }

        val now = System.currentTimeMillis()
        if (now - lastHapticTime < 150L) return
        lastHapticTime = now

        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Hafif ve keskin tek dokunuş (45ms)
                vibrator.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(45)
            }
        } catch (_: Exception) {
            // Cihaz titreşim motoru veya izin hatalarını sessizce yakala
        }
    }

    /**
     * Çift vuruşlu hafif onay titreşimi (Örn: Hızlı QR Düzeltme veya parti ekleme)
     */
    fun triggerDoublePulseHaptic(context: Context, playSound: Boolean = true) {
        if (playSound) {
            playDoubleTone()
        }

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
     * Hata veya uyarı durumlarında belirgin titreşim ve ses
     */
    fun triggerWarningHaptic(context: Context, playSound: Boolean = true) {
        if (playSound) {
            playWarningTone()
        }

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
