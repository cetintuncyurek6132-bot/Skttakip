package com.example

import android.app.Application
import android.content.Intent
import android.os.Looper
import android.os.Process
import android.util.Log

/**
 * Safe-Startup Application with Global Uncaught Exception Interceptor.
 * Prevents abrupt app crashes on non-fatal background/helper errors and ensures clean recovery.
 */
class SktApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupGlobalCrashInterceptor()
        setupToneReleaseOnLowMemory()
    }

    override fun onTerminate() {
        super.onTerminate()
        try {
            com.example.util.SoundToneManager.release()
            com.example.util.HapticFeedbackHelper.release()
        } catch (_: Exception) {
        }
    }

    private fun setupToneReleaseOnLowMemory() {
        // Uygulama düşük bellekte kapatılırsa ToneGenerator'ları serbest bırak
        // Bu API 14+ için geçerlidir
        registerComponentCallbacks(object : android.content.ComponentCallbacks2 {
            override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {}
            override fun onLowMemory() {
                try {
                    com.example.util.SoundToneManager.release()
                    com.example.util.HapticFeedbackHelper.release()
                } catch (_: Exception) {}
            }
            override fun onTrimMemory(level: Int) {
                if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
                    try {
                        com.example.util.SoundToneManager.release()
                        com.example.util.HapticFeedbackHelper.release()
                    } catch (_: Exception) {}
                }
            }
        })
    }

    private fun setupGlobalCrashInterceptor() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "FATAL INTERCEPTOR caught uncaught exception on thread '${thread.name}': ${throwable.message}", throwable)

            val isMainThread = Looper.myLooper() == Looper.getMainLooper()

            if (!isMainThread) {
                // Background thread error: log and swallow to avoid app crash
                Log.w(TAG, "Suppressed background thread crash to keep UI active. Thread: ${thread.name}")
                return@setDefaultUncaughtExceptionHandler
            }

            // Main UI thread error: attempt safe restart into MainActivity without looping
            try {
                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
                Process.killProcess(Process.myPid())
                System.exit(10)
            } catch (recoveryError: Throwable) {
                Log.e(TAG, "Emergency restart failed: ${recoveryError.message}", recoveryError)
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    companion object {
        private const val TAG = "SafeStartup"
    }
}
