// Create as Class
package com.mkayuni.bassbroker.util

import com.mkayuni.bassbroker.model.Stock

class PatternDetector {
    companion object {
        // Detect if a stock is in a bullish trend
        fun isBullishTrend(historicalPrices: List<Double>): Boolean {
            if (historicalPrices.size < 5) return false

            // Simple moving average check
            val shortTermAvg = historicalPrices.takeLast(5).average()
            val longTermAvg = historicalPrices.takeLast(20).average()

            return shortTermAvg > longTermAvg
        }

        // Detect if a stock is in a bearish trend
        fun isBearishTrend(historicalPrices: List<Double>): Boolean {
            if (historicalPrices.size < 5) return false

            // Simple moving average check
            val shortTermAvg = historicalPrices.takeLast(5).average()
            val longTermAvg = historicalPrices.takeLast(20).average()

            return shortTermAvg < longTermAvg
        }

        // Detect potential breakout
        fun isBreakoutPattern(stock: Stock, historicalPrices: List<Double>): Boolean {
            if (historicalPrices.size < 20) return false

            // Check if current price is significantly higher than recent resistance
            val recentHigh = historicalPrices.takeLast(20).maxOrNull() ?: return false

            return stock.currentPrice > recentHigh * 1.02 // 2% above resistance
        }

        // Detect potential breakdown
        fun isBreakdownPattern(stock: Stock, historicalPrices: List<Double>): Boolean {
            if (historicalPrices.size < 20) return false

            // Check if current price is significantly lower than recent support
            val recentLow = historicalPrices.takeLast(20).minOrNull() ?: return false

            return stock.currentPrice < recentLow * 0.98 // 2% below support
        }
    }
}