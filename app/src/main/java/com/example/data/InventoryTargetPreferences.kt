package com.example.data

import android.content.Context
import androidx.core.content.edit

object InventoryTargetPreferences {
    private const val PREFS_NAME = "inventory_target_prefs"
    private const val KEY_STORE_COMBINED_TARGET = "store_combined_target_ratio"
    private const val KEY_CONTINUOUS_TOTAL_TARGET = "continuous_total_target_ratio"

    const val DEFAULT_STORE_COMBINED_TARGET = 2.0
    const val DEFAULT_CONTINUOUS_TOTAL_TARGET = 5.0

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getStoreCombinedTargetRatio(context: Context): Double {
        val stored = getPrefs(context).getFloat(
            KEY_STORE_COMBINED_TARGET,
            DEFAULT_STORE_COMBINED_TARGET.toFloat()
        ).toDouble()
        return if (stored > 0.0) stored else DEFAULT_STORE_COMBINED_TARGET
    }

    fun setStoreCombinedTargetRatio(context: Context, ratio: Double) {
        val safeRatio = if (ratio > 0.0) ratio else DEFAULT_STORE_COMBINED_TARGET
        getPrefs(context).edit {
            putFloat(KEY_STORE_COMBINED_TARGET, safeRatio.toFloat())
        }
    }

    fun getContinuousTotalTargetRatio(context: Context): Double {
        val stored = getPrefs(context).getFloat(
            KEY_CONTINUOUS_TOTAL_TARGET,
            DEFAULT_CONTINUOUS_TOTAL_TARGET.toFloat()
        ).toDouble()
        return if (stored > 0.0) stored else DEFAULT_CONTINUOUS_TOTAL_TARGET
    }

    fun setContinuousTotalTargetRatio(context: Context, ratio: Double) {
        val safeRatio = if (ratio > 0.0) ratio else DEFAULT_CONTINUOUS_TOTAL_TARGET
        getPrefs(context).edit {
            putFloat(KEY_CONTINUOUS_TOTAL_TARGET, safeRatio.toFloat())
        }
    }
}
