package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

data class SoundToneOption(
    val id: Int,
    val title: String,
    val description: String,
    val toneType: Int,
    val durationMs: Int
)

object SoundToneManager {
    private const val TAG = "SoundToneManager"
    const val PREFS_NAME = "app_settings_prefs"

    const val KEY_SCANNER_SOUND_BARCODE = "scanner_sound_barcode"
    const val KEY_SCANNER_SOUND_LABEL_FIX = "scanner_sound_label_fix"

    // 1. Kategori: Barkod Tarama Sesleri
    val BARCODE_TONES = listOf(
        SoundToneOption(1, "Klasik Lazer Bip", "Hızlı ve net lazer okuma tonu", ToneGenerator.TONE_PROP_BEEP, 60),
        SoundToneOption(2, "Market Terminal Bip", "Perakende kasa terminali tonu", ToneGenerator.TONE_PROP_ACK, 75),
        SoundToneOption(3, "Hızlı Çift Bip", "Seri okutma için çift vuruş", ToneGenerator.TONE_PROP_BEEP2, 55)
    )

    // 2. Kategori: Raf Etiketi / QR Doğrulama Sesleri
    val QR_FIX_TONES = listOf(
        SoundToneOption(11, "Onay Bipi", "Temiz ve tok doğrulama sinyali", ToneGenerator.TONE_PROP_ACK, 80),
        SoundToneOption(12, "Çift Onay", "Ardışık iki vuruşlu doğrulama", ToneGenerator.TONE_PROP_BEEP2, 90),
        SoundToneOption(13, "Başarılı Zil", "Melodik yüksek frekanslı zil", ToneGenerator.TONE_PROP_PROMPT, 120)
    )

    val ALL_TONES = BARCODE_TONES + QR_FIX_TONES

    fun getToneOptionById(id: Int): SoundToneOption {
        return ALL_TONES.find { it.id == id } ?: BARCODE_TONES[0]
    }

    fun getOptionById(id: Int): SoundToneOption = getToneOptionById(id)

    fun getBarcodeToneId(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_SCANNER_SOUND_BARCODE, 1)
    }

    fun setBarcodeToneId(context: Context, id: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_SCANNER_SOUND_BARCODE, id).apply()
    }

    fun getBarcodeToneTitle(context: Context): String {
        val id = getBarcodeToneId(context)
        return getToneOptionById(id).title
    }

    fun getQrFixToneId(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_SCANNER_SOUND_LABEL_FIX, 11)
    }

    fun setQrFixToneId(context: Context, id: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_SCANNER_SOUND_LABEL_FIX, id).apply()
    }

    fun getQrFixToneTitle(context: Context): String {
        val id = getQrFixToneId(context)
        return getToneOptionById(id).title
    }

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
                            ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                        } catch (_: Exception) {
                            try {
                                ToneGenerator(AudioManager.STREAM_SYSTEM, 100)
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

    fun playBarcodeBeep(context: Context? = null) {
        try {
            val toneOption = if (context != null) {
                getToneOptionById(getBarcodeToneId(context))
            } else {
                BARCODE_TONES[0]
            }
            getToneGenerator()?.startTone(toneOption.toneType, toneOption.durationMs)
        } catch (e: Exception) {
            Log.e(TAG, "Barcode beep error: ${e.message}")
        }
    }

    fun playLabelFixBeep(context: Context? = null) {
        try {
            val toneOption = if (context != null) {
                getToneOptionById(getQrFixToneId(context))
            } else {
                QR_FIX_TONES[0]
            }
            getToneGenerator()?.startTone(toneOption.toneType, toneOption.durationMs)
        } catch (e: Exception) {
            Log.e(TAG, "Label fix beep error: ${e.message}")
        }
    }

    fun playTonePreview(context: Context? = null, toneId: Int) {
        playTone(toneId)
    }

    fun playTone(toneId: Int) {
        try {
            val option = getToneOptionById(toneId)
            getToneGenerator()?.startTone(option.toneType, option.durationMs)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play tone $toneId: ${e.message}")
        }
    }
}
