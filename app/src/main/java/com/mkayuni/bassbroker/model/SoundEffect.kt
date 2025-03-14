package com.mkayuni.bassbroker.model

data class SoundEffect(
    val id: Int,
    val name: String,
    val resourceId: Int,
    val type: SoundType
)

enum class SoundType {
    PRICE_UP,
    PRICE_DOWN,
    PRICE_STABLE,
    CUSTOM,

    // New prediction sound types
    PREDICT_HIGH_UP,
    PREDICT_HIGH_DOWN,
    PREDICT_MEDIUM_UP,
    PREDICT_MEDIUM_DOWN,
    PREDICT_LOW
}