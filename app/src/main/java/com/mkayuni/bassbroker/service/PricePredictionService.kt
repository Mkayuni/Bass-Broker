package com.mkayuni.bassbroker.service

import kotlin.math.abs

class PricePredictionService {

    /**
     * Predicts future prices based on historical data using simple statistical models
     * @param historicalPrices List of historical prices, with most recent price last
     * @param daysToPredict Number of days to predict into the future
     * @return PredictionResult containing predicted prices and confidence level
     */
    fun predictPrices(historicalPrices: List<Double>, daysToPredict: Int = 5): PredictionResult {
        if (historicalPrices.size < 5) {
            return PredictionResult(emptyList(), 0.3f)
        }

        // Get recent trend (more weight to recent prices)
        val recentPrices = historicalPrices.takeLast(10)
        val trend = calculateWeightedTrend(recentPrices)

        // Get volatility
        val volatility = calculateVolatility(recentPrices)

        // Calculate momentum
        val momentum = calculateMomentum(recentPrices)

        // Calculate predictions
        val predictions = mutableListOf<Double>()
        var lastPrice = historicalPrices.last()

        for (i in 1..daysToPredict) {
            // Simple prediction model combining trend with momentum and decay
            val dayFactor = 1.0 / (1 + 0.1 * i) // Decay factor - confidence decreases over time
            val predictedChange = (trend * dayFactor) + (momentum * (1 - dayFactor))
            val predictedPrice = lastPrice * (1 + predictedChange)
            predictions.add(predictedPrice)
            lastPrice = predictedPrice // Use predicted price for next day's prediction
        }

        // Calculate confidence (0.0-1.0) based on volatility and consistency
        val consistency = calculateConsistency(recentPrices)
        val confidence = calculateConfidence(volatility, consistency)

        return PredictionResult(predictions, confidence)
    }

    /**
     * Calculate weighted trend where recent days have higher weight
     */
    private fun calculateWeightedTrend(prices: List<Double>): Double {
        if (prices.size < 2) return 0.0

        var weightedSum = 0.0
        var weightSum = 0.0

        for (i in 1 until prices.size) {
            val weight = i.toDouble() // More recent prices get higher weight
            val change = (prices[i] - prices[i-1]) / prices[i-1]
            weightedSum += change * weight
            weightSum += weight
        }

        return if (weightSum > 0) weightedSum / weightSum else 0.0
    }

    /**
     * Calculate price volatility
     */
    private fun calculateVolatility(prices: List<Double>): Double {
        if (prices.size < 2) return 0.0

        val changes = (1 until prices.size).map { i ->
            abs((prices[i] - prices[i-1]) / prices[i-1])
        }

        return changes.average()
    }

    /**
     * Calculate price momentum
     */
    private fun calculateMomentum(prices: List<Double>): Double {
        if (prices.size < 5) return 0.0

        // Calculate short-term vs long-term trend
        val shortTerm = (prices.last() - prices[prices.size - 3]) / prices[prices.size - 3]
        val longTerm = (prices.last() - prices.first()) / prices.first()

        return (shortTerm * 0.7) + (longTerm * 0.3)
    }

    /**
     * Measure consistency of price movements
     */
    private fun calculateConsistency(prices: List<Double>): Double {
        if (prices.size < 3) return 0.5

        var sameDirectionCount = 0
        var totalCount = 0

        for (i in 2 until prices.size) {
            val change1 = prices[i] - prices[i-1]
            val change2 = prices[i-1] - prices[i-2]

            if ((change1 > 0 && change2 > 0) || (change1 < 0 && change2 < 0)) {
                sameDirectionCount++
            }
            totalCount++
        }

        return if (totalCount > 0) sameDirectionCount.toDouble() / totalCount else 0.5
    }

    /**
     * Calculate overall prediction confidence
     */
    private fun calculateConfidence(volatility: Double, consistency: Double): Float {
        // High volatility = low confidence, high consistency = high confidence
        val volatilityFactor = (1.0 - (volatility * 10.0)).coerceIn(0.2, 0.9)
        val confidenceValue = (volatilityFactor * 0.5) + (consistency * 0.5)
        return confidenceValue.toFloat().coerceIn(0.1f, 0.9f)
    }

    /**
     * Result of price prediction, including predicted prices and confidence level
     */
    data class PredictionResult(
        val predictedPrices: List<Double>,
        val confidence: Float // 0.0-1.0 scale
    ) {
        val direction: PredictionDirection
            get() {
                if (predictedPrices.isEmpty()) return PredictionDirection.NEUTRAL

                val firstPrice = predictedPrices.first()
                val lastPrice = predictedPrices.last()
                val percentChange = (lastPrice - firstPrice) / firstPrice * 100

                return when {
                    percentChange > 3.0 -> PredictionDirection.STRONGLY_UP
                    percentChange > 1.0 -> PredictionDirection.UP
                    percentChange < -3.0 -> PredictionDirection.STRONGLY_DOWN
                    percentChange < -1.0 -> PredictionDirection.DOWN
                    else -> PredictionDirection.NEUTRAL
                }
            }
    }
    /**
     * Direction of price prediction
     */
    enum class PredictionDirection {
        STRONGLY_UP, UP, NEUTRAL, DOWN, STRONGLY_DOWN
    }
}