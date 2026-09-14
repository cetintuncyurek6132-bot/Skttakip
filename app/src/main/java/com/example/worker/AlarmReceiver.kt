package com.example.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            MorningCheckWorker.scheduleDailyMorningCheck(context)
            return
        }

        // Alarm triggered: perform check & show notification in background
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                MorningCheckWorker.performDirectCheckAndNotify(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
