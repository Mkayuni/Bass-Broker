package com.mkayuni.bassbroker.model

data class StockAlert(
    val stockSymbol: String,
    val thresholdValue: Double,
    val isHighAlert: Boolean,
    val soundEffectId: Int
)