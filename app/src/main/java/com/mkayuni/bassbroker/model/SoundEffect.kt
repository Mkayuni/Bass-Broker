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
    CUSTOM
}