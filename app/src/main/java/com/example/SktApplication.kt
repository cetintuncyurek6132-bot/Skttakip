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
