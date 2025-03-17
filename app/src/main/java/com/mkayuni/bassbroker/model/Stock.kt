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
        get() = if (previousClose > 0) {
            (priceChange / previousClose) * 100
        } else {
            0.0 // Default to 0% if previous close is zero
        }
}

data class MarketIndices(
    val sp500Price: Double,
    val sp500Change: Double,
    val nasdaqPrice: Double,
    val nasdaqChange: Double,
    val vixPrice: Double,
    val vixChange: Double
)