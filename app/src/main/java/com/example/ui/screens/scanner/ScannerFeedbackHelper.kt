package com.example.ui.screens.scanner

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.graphics.Color
import com.example.data.Product
import com.example.data.findMatchingProducts
import com.example.ui.theme.CriticalOrange
import com.example.ui.theme.ExpiredRed
import com.example.ui.theme.NormalGreen
import com.example.ui.theme.SoonYellow
import com.example.ui.theme.TurquoisePrimary

enum class ScanResultRisk(
    val title: String,
    val color: Color,
    val isCritical: Boolean
) {
    SAFE("✅ GÜVENLİ", NormalGreen, false),
    WARNING("⚠️ YAKLAŞIYOR", SoonYellow, false),
    CRITICAL("🚨 KRİTİK SKT", CriticalOrange, true),
    EXPIRED("⛔ SÜRESİ GEÇTİ", ExpiredRed, true),
    NO_SKT("ℹ️ SKT KAYDI YOK", TurquoisePrimary, false),
    NOT_FOUND("❓ YENİ ÜRÜN", Color(0xFF6366F1), false)
}

object ScannerFeedbackHelper {

    private var lastFeedbackTime = 0L
    @Volatile
    private var cachedVibrator: Vibrator? = null
    @Volatile
    private var vibratorChecked = false

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

    fun evaluateProductRisk(
        barcode: String,
        products: List<Product>,
        todayMidnight: Long = System.currentTimeMillis()
    ): Pair<ScanResultRisk, Long?> {
        val matches = products.findMatchingProducts(barcode)
        if (matches.isEmpty()) {
            return Pair(ScanResultRisk.NOT_FOUND, null)
        }

        val prodsWithSkt = matches.filter { it.sktTarihi > 0L }
        if (prodsWithSkt.isEmpty()) {
            return Pair(ScanResultRisk.NO_SKT, null)
        }

        val nearest = prodsWithSkt.minByOrNull { it.sktTarihi } ?: prodsWithSkt.first()
        val remainingDays = nearest.getRemainingDays(todayMidnight)

        val risk = when {
            remainingDays < 0 -> ScanResultRisk.EXPIRED
            remainingDays <= 3 -> ScanResultRisk.CRITICAL
            remainingDays <= 15 -> ScanResultRisk.WARNING
            else -> ScanResultRisk.SAFE
        }
        return Pair(risk, remainingDays)
    }

    fun playFeedback(
        context: Context,
        toneGenerator: ToneGenerator?,
        risk: ScanResultRisk,
        soundEnabled: Boolean = true,
        vibrationEnabled: Boolean = true
    ) {
        val now = System.currentTimeMillis()
        if (now - lastFeedbackTime < 450L) {
            return // Prevent audio & vibrator IPC floods that trigger SELinux audit rate limit
        }
        lastFeedbackTime = now

        if (soundEnabled && toneGenerator != null) {
            try {
                when (risk) {
                    ScanResultRisk.SAFE -> toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                    ScanResultRisk.WARNING -> toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 130)
                    ScanResultRisk.CRITICAL, ScanResultRisk.EXPIRED -> toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
                    ScanResultRisk.NOT_FOUND -> toneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, 140)
                    ScanResultRisk.NO_SKT -> toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                }
            } catch (e: Exception) {
                // Ignore audio playback error
            }
        }

        if (vibrationEnabled) {
            triggerHaptic(context, risk)
        }
    }

    private fun triggerHaptic(context: Context, risk: ScanResultRisk) {
        try {
            val vibrator = getVibrator(context) ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (risk) {
                    ScanResultRisk.SAFE, ScanResultRisk.NO_SKT -> {
                        // Single sharp crisp click
                        VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE)
                    }
                    ScanResultRisk.WARNING -> {
                        // Two pulses: 35ms on, 50ms off, 45ms on
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 35, 50, 45),
                            intArrayOf(0, 170, 0, 200),
                            -1
                        )
                    }
                    ScanResultRisk.CRITICAL, ScanResultRisk.EXPIRED -> {
                        // Two strong urgent buzzes: 80ms on, 60ms off, 140ms on (max intensity)
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 80, 60, 140),
                            intArrayOf(0, 255, 0, 255),
                            -1
                        )
                    }
                    ScanResultRisk.NOT_FOUND -> {
                        // 3 quick gentle taps
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 25, 40, 25, 40, 25),
                            intArrayOf(0, 140, 0, 140, 0, 140),
                            -1
                        )
                    }
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                when (risk) {
                    ScanResultRisk.SAFE, ScanResultRisk.NO_SKT -> vibrator.vibrate(45)
                    ScanResultRisk.WARNING -> vibrator.vibrate(longArrayOf(0, 35, 50, 45), -1)
                    ScanResultRisk.CRITICAL, ScanResultRisk.EXPIRED -> vibrator.vibrate(longArrayOf(0, 80, 60, 140), -1)
                    ScanResultRisk.NOT_FOUND -> vibrator.vibrate(longArrayOf(0, 25, 40, 25, 40, 25), -1)
                }
            }
        } catch (e: Exception) {
            // Ignore haptic error
        }
    }
}
