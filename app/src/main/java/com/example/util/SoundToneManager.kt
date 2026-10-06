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

    // Profesyonel Perakende El Terminali (Zebra / Honeywell) Lazer Tonları
    val TONES = listOf(
        SoundToneOption(1, "Zebra Lazer Bip (Barkod)", ToneGenerator.TONE_PROP_BEEP, 60),
        SoundToneOption(2, "Honeywell Onay (Etiket/QR)", ToneGenerator.TONE_PROP_ACK, 80)
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
                        ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                    } catch (_: Exception) {
                        try {
                            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
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

    fun playBarcodeBeep() {
        try {
            getToneGenerator()?.startTone(ToneGenerator.TONE_PROP_BEEP, 60)
        } catch (e: Exception) {
            Log.e(TAG, "Barcode beep error: ${e.message}")
        }
    }

    fun playLabelFixBeep() {
        try {
            getToneGenerator()?.startTone(ToneGenerator.TONE_PROP_ACK, 80)
        } catch (e: Exception) {
            Log.e(TAG, "Label fix beep error: ${e.message}")
        }
    }

    fun playTonePreview(context: Context? = null, toneId: Int) {
        playTone(toneId)
    }

    fun playTone(toneId: Int) {
        try {
            if (toneId == 2) {
                playLabelFixBeep()
            } else {
                playBarcodeBeep()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play tone $toneId: ${e.message}")
        }
    }
}
