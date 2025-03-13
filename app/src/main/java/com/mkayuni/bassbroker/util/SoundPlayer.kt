package com.mkayuni.bassbroker.util

import android.content.Context
import android.media.MediaPlayer
import com.mkayuni.bassbroker.R
import com.mkayuni.bassbroker.model.SoundType

class SoundPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

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

    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}