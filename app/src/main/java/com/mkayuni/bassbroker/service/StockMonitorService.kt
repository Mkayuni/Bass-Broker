package com.mkayuni.bassbroker.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.mkayuni.bassbroker.R
import com.mkayuni.bassbroker.api.StockRepository
import com.mkayuni.bassbroker.model.Stock
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import android.content.pm.ServiceInfo

class StockMonitorService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var mediaPlayer: MediaPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)

        // Schedule periodic stock checks
        setupWorkManager()

        return START_STICKY
    }

    private fun setupWorkManager() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val stockWorkRequest = PeriodicWorkRequestBuilder<StockCheckWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "stockCheck",
            ExistingPeriodicWorkPolicy.REPLACE,
            stockWorkRequest
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Stock Monitor Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Bass Broker")
            .setContentText("Monitoring your stocks")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    companion object {
        private const val CHANNEL_ID = "StockMonitorChannel"
        private const val NOTIFICATION_ID = 1
    }
}

// Worker class for periodic stock checks
class StockCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // TODO: Get saved alerts and check stocks
        // TODO: Play appropriate sounds when thresholds are hit
        return Result.success()
    }
}