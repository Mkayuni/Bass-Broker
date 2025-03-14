package com.mkayuni.bassbroker.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.mkayuni.bassbroker.R
import com.mkayuni.bassbroker.model.Stock
import com.mkayuni.bassbroker.model.SoundType
import com.mkayuni.bassbroker.util.SoundPlayer
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class StockMonitorService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var mediaPlayer: MediaPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)  // Compatible with API 24+

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

    private val soundPlayer = SoundPlayer(applicationContext)
    private val repository = StockRepository()

    override suspend fun doWork(): Result {
        try {
            // Get saved stocks and alert thresholds
            val prefs = applicationContext.getSharedPreferences("stock_alerts", Context.MODE_PRIVATE)
            val stocks = prefs.all.mapNotNull { entry ->
                try {
                    val values = entry.value.toString().split(",")
                    if (values.size >= 3) {
                        val symbol = entry.key
                        val highAlert = values[0].toDoubleOrNull()
                        val lowAlert = values[1].toDoubleOrNull()
                        val highSoundType = values[2].toIntOrNull()?.let { SoundType.values().getOrNull(it) }
                        val lowSoundType = values[3].toIntOrNull()?.let { SoundType.values().getOrNull(it) }

                        Stock(
                            symbol = symbol,
                            name = symbol, // Placeholder, will be updated from API
                            currentPrice = 0.0, // Will be updated from API
                            previousClose = 0.0, // Will be updated from API
                            alertThresholdHigh = highAlert,
                            alertThresholdLow = lowAlert
                        ) to Pair(highSoundType, lowSoundType)
                    } else null
                } catch (e: Exception) {
                    null
                }
            }

            // Check each stock
            stocks.forEach { (stock, sounds) ->
                repository.getStockPrice(stock.symbol).onSuccess { updatedStock ->
                    // Check high alert - safe way
                    val highThreshold = stock.alertThresholdHigh
                    if (highThreshold != null && updatedStock.currentPrice >= highThreshold) {
                        // Play high alert sound
                        sounds.first?.let { soundPlayer.playSound(it) }
                    }

                    // Check low alert - safe way
                    val lowThreshold = stock.alertThresholdLow
                    if (lowThreshold != null && updatedStock.currentPrice <= lowThreshold) {
                        // Play low alert sound
                        sounds.second?.let { soundPlayer.playSound(it) }
                    }
                }
            }

            return Result.success()
        } catch (e: Exception) {
            // Log the error and return failure
            return Result.failure()
        }
    }
}