package com.mkayuni.bassbroker.model

data class Stock(
    val symbol: String,
    val name: String,
    val currentPrice: Double,
    val previousClose: Double,
    var alertThresholdHigh: Double? = null,
    var alertThresholdLow: Double? = null
) {
    val priceChange: Double
        get() = currentPrice - previousClose

    val percentChange: Double
        get() = (priceChange / previousClose) * 100
}