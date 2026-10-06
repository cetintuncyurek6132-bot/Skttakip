package com.example.worker

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.util.AppUpdateChecker

class DownloadCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            Log.d("DownloadCompleteReceiver", "İndirme tamamlandı sinyali alındı. ID: $downloadId")
            if (downloadId != -1L) {
                AppUpdateChecker.handleDownloadComplete(context, downloadId)
            }
        }
    }
}
