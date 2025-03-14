// Update as Class (not create a new file)
package com.mkayuni.bassbroker.util

import android.content.Context
import android.media.MediaPlayer
import com.mkayuni.bassbroker.R
import com.mkayuni.bassbroker.model.SoundType
import com.mkayuni.bassbroker.service.MarketStatus
import com.mkayuni.bassbroker.service.NewsSentiment

class SoundPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    // Existing functionality for stock sounds
    fun playSound(soundType: SoundType) {
        // Release any existing player
        releaseMediaPlayer()

        // Get resource ID based on sound type
        val resourceId = when (soundType) {
            SoundType.PRICE_UP -> R.raw.bass_up
            SoundType.PRICE_DOWN -> R.raw.bass_down
            SoundType.PRICE_STABLE -> R.raw.bass_stable
            SoundType.CUSTOM -> R.raw.bass_up // Default to up sound for custom
        }

        // Create and play the sound
        mediaPlayer = MediaPlayer.create(context, resourceId)
        mediaPlayer?.setOnCompletionListener { releaseMediaPlayer() }
        mediaPlayer?.start()
    }

    fun playCustomSound(resourceId: Int) {
        releaseMediaPlayer()
        mediaPlayer = MediaPlayer.create(context, resourceId)
        mediaPlayer?.setOnCompletionListener { releaseMediaPlayer() }
        mediaPlayer?.start()
    }

    // News-related sounds
    fun playNewsSentimentSound(sentiment: NewsSentiment) {
        val resourceId = when (sentiment) {
            NewsSentiment.VERY_POSITIVE -> R.raw.news_very_positive
            NewsSentiment.POSITIVE -> R.raw.news_positive
            NewsSentiment.NEUTRAL -> R.raw.news_neutral
            NewsSentiment.NEGATIVE -> R.raw.news_negative
            NewsSentiment.VERY_NEGATIVE -> R.raw.news_very_negative
        }

        playCustomSound(resourceId)
    }

    // Market status sounds
    fun playMarketOpenSound() {
        playCustomSound(R.raw.market_open)
    }

    fun playMarketCloseSound() {
        playCustomSound(R.raw.market_close)
    }

    fun playMarketStatusSound(status: MarketStatus) {
        val resourceId = when (status) {
            MarketStatus.OPEN -> R.raw.market_active
            MarketStatus.CLOSED -> R.raw.market_closed
            MarketStatus.PRE_MARKET -> R.raw.market_pre
            MarketStatus.AFTER_HOURS -> R.raw.market_after
            MarketStatus.CLOSED_WEEKEND -> R.raw.market_weekend
        }

        playCustomSound(resourceId)
    }

    // Pattern recognition sounds
    fun playBullishSound() {
        playCustomSound(R.raw.pattern_bullish)
    }

    fun playBearishSound() {
        playCustomSound(R.raw.pattern_bearish)
    }

    fun playBreakoutSound() {
        playCustomSound(R.raw.pattern_breakout)
    }

    fun playBreakdownSound() {
        playCustomSound(R.raw.pattern_breakdown)
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}