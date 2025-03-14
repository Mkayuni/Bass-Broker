// Create as Class
package com.mkayuni.bassbroker.util

import android.content.Context
import android.media.MediaPlayer
import com.mkayuni.bassbroker.R
import com.mkayuni.bassbroker.service.MarketStatus

class MarketBellPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun playMarketOpenSound() {
        playSound(R.raw.market_open)
    }

    fun playMarketCloseSound() {
        playSound(R.raw.market_close)
    }

    fun playMarketStatusSound(status: MarketStatus) {
        val resourceId = when (status) {
            MarketStatus.OPEN -> R.raw.market_active
            MarketStatus.CLOSED -> R.raw.market_closed
            MarketStatus.PRE_MARKET -> R.raw.market_pre
            MarketStatus.AFTER_HOURS -> R.raw.market_after
            MarketStatus.CLOSED_WEEKEND -> R.raw.market_weekend
        }

        playSound(resourceId)
    }

    private fun playSound(resourceId: Int) {
        releaseMediaPlayer()

        mediaPlayer = MediaPlayer.create(context, resourceId)
        mediaPlayer?.setOnCompletionListener { releaseMediaPlayer() }
        mediaPlayer?.start()
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}