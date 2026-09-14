package com.example.worker

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.ExpiryStatus
import com.example.data.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MorningCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                performDirectCheckAndNotify(context)
                Result.success()
            } catch (e: Exception) {
                Log.e("MorningCheckWorker", "Work failed", e)
                Result.retry()
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "skt_alerts_channel"
        const val WORK_NAME = "skt_morning_periodic_check"
        private const val NOTIFICATION_ID = 1001
        private const val ALARM_REQUEST_CODE = 2001

        /**
         * Checks products from database and triggers a rich notification.
         */
        suspend fun performDirectCheckAndNotify(context: Context) {
            val db = AppDatabase.getDatabase(context)
            val products = try {
                db.productDao().getAllProductsList()
            } catch (e: Exception) {
                emptyList()
            }

            val validProducts = products.filter { it.sktTarihi > 0L && it.stokAdedi > 0 }
            val overdue2DaysUnremoved = validProducts.filter { it.getRemainingDays() <= -2 }
            val expired = validProducts.filter { it.getExpiryStatus() == ExpiryStatus.EXPIRED }
            val critical = validProducts.filter { it.getExpiryStatus() == ExpiryStatus.CRITICAL }
            val totalRiskCount = expired.size + critical.size

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            createNotificationChannel(notificationManager)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "urunler")
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val (title, shortText, bigText) = when {
                overdue2DaysUnremoved.isNotEmpty() -> {
                    val overdueSummary = overdue2DaysUnremoved.take(4).joinToString("\n") { 
                        "• ${it.urunAdi} (${-it.getRemainingDays()} gün geçti - ${it.stokAdedi} Adet Rafta!)" 
                    }
                    val totalOverdueStock = overdue2DaysUnremoved.sumOf { it.stokAdedi }
                    Triple(
                        "🛑 DİKKAT: SKT'si 2+ Gün Geçmiş $totalOverdueStock Adet Ürün Rafta!",
                        "${overdue2DaysUnremoved.size} ürünün SKT'si 2 günden fazla geçti ve raftan kaldırılmadı!",
                        "🛑 RAFTAN KALDIRILMAYAN GÜNÜ GEÇMİŞ ÜRÜNLER (${overdue2DaysUnremoved.size} Çeşit / $totalOverdueStock Adet):\n$overdueSummary\n\nMüşteri güvenliği için lütfen bu ürünleri DERHAL raftan kaldırıp imha/iade işlemini tamamlayın."
                    )
                }
                expired.isNotEmpty() && critical.isNotEmpty() -> {
                    val expSummary = expired.take(3).joinToString("\n") { "• ${it.urunAdi} (Süresi Geçti)" }
                    val critSummary = critical.take(3).joinToString("\n") { "• ${it.urunAdi} (${it.getRemainingDays()} gün kaldı)" }
                    Triple(
                        "🚨 $totalRiskCount Üründe SKT Riski Var!",
                        "${expired.size} ürünün süresi doldu, ${critical.size} ürün kritik eşikte.",
                        "🚨 SÜRESİ GEÇENLER (${expired.size}):\n$expSummary\n\n⚠️ KRİTİK ÜRÜNLER (${critical.size}):\n$critSummary\n\nReyonları kontrol etmek için dokunun."
                    )
                }
                expired.isNotEmpty() -> {
                    val expSummary = expired.take(4).joinToString("\n") { "• ${it.urunAdi} (${it.stokAdedi} adet)" }
                    Triple(
                        "🚨 ${expired.size} Ürünün Son Kullanma Tarihi Geçti!",
                        "Reyonda süresi dolmuş ürünler tespit edildi. İade/imha işlemi yapın.",
                        "🚨 SÜRESİ GEÇEN ÜRÜNLER:\n$expSummary\n\nÜrünleri raftan kaldırmak için dokunun."
                    )
                }
                critical.isNotEmpty() -> {
                    val critSummary = critical.take(4).joinToString("\n") { "• ${it.urunAdi} (${it.getRemainingDays()} gün kaldı)" }
                    Triple(
                        "⚠️ ${critical.size} Ürünün SKT Tarihi Yaklaşıyor!",
                        "Kritik eşikteki ürünler için indirim veya öne çekme yapabilirsiniz.",
                        "⚠️ KRİTİK YAKLAŞANLAR:\n$critSummary\n\nDetayları incelemek için dokunun."
                    )
                }
                else -> {
                    Triple(
                        "✅ Günlük SKT Durumu Güncel",
                        "Reyonlar güvende. Kritik süresi dolan ürün bulunmuyor.",
                        "✅ Tüm Ürünler Güvende:\nKritik veya süresi geçen ürün bulunmuyor. Ürün listesini incelemek için dokunun."
                    )
                }
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(shortText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        }

        private fun createNotificationChannel(notificationManager: NotificationManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "SKT ve Reyon Uyarıları",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Günü geçen, yaklaşan ürünler ve günlük kontrol hatırlatmaları"
                    enableLights(true)
                    enableVibration(true)
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }

        /**
         * Schedules WorkManager periodic check for günü gelen/yaklaşan SKT notifications.
         */
        fun scheduleDailyMorningCheck(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build()

                val now = Calendar.getInstance()
                val target = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 8)
                    set(Calendar.MINUTE, 30)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (target.before(now) || target.equals(now)) {
                    target.add(Calendar.DAY_OF_MONTH, 1)
                }
                val initialDelayMillis = target.timeInMillis - now.timeInMillis

                val periodicWorkRequest = PeriodicWorkRequestBuilder<MorningCheckWorker>(
                    24, TimeUnit.HOURS,
                    15, TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicWorkRequest
                )
            } catch (e: Exception) {
                Log.w("MorningCheckWorker", "WorkManager schedule failed: ${e.message}")
            }
        }

        fun scheduleExactAlarm(context: Context) {
            // Deprecated: WorkManager handles periodic background checks cleanly without alarm manager overhead
        }

        /**
         * Triggers an immediate notification to test receiving alerts on phone.
         */
        suspend fun triggerTestNotificationDirectly(context: Context) {
            performDirectCheckAndNotify(context)
        }
    }
}
