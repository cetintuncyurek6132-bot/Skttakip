package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

data class SoundToneOption(
    val id: Int,
    val title: String,
    val toneType: Int,
    val durationMs: Int
)

object SoundToneManager {
    private const val TAG = "SoundToneManager"

    const val KEY_SCANNER_SOUND_BARCODE = "scanner_sound_barcode"
    const val KEY_SCANNER_SOUND_LABEL_FIX = "scanner_sound_label_fix"

    val TONES = listOf(
        SoundToneOption(1, "Klasik Market Bip", ToneGenerator.TONE_PROP_BEEP, 70),
        SoundToneOption(2, "Çift Onay Bipi", ToneGenerator.TONE_PROP_ACK, 90),
        SoundToneOption(3, "Yumuşak Melodik", ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 100),
        SoundToneOption(4, "Yüksek / Net Terminal", ToneGenerator.TONE_DTMF_D, 80),
        SoundToneOption(5, "Başarı Çanı / Pozitif", ToneGenerator.TONE_PROP_PROMPT, 110)
    )

    fun getToneOptionById(id: Int): SoundToneOption {
        return TONES.find { it.id == id } ?: TONES[0]
    }

    fun getOptionById(id: Int): SoundToneOption = getToneOptionById(id)

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
