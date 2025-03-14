// Create as Class
package com.mkayuni.bassbroker.util

import android.content.Context
import android.media.MediaPlayer
import com.mkayuni.bassbroker.R

class PatternPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun playBullishSound() {
        playSound(R.raw.pattern_bullish)
    }

    fun playBearishSound() {
        playSound(R.raw.pattern_bearish)
    }

    fun playBreakoutSound() {
        playSound(R.raw.pattern_breakout)
    }

    fun playBreakdownSound() {
        playSound(R.raw.pattern_breakdown)
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