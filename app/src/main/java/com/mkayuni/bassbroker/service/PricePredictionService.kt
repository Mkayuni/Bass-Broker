package com.mkayuni.bassbroker.service

import android.content.Context
import android.util.Log
import kotlin.math.abs

class PricePredictionService(private val context: Context) {

    private val tensorFlowService = TensorFlowPredictionService(context)
    private val TAG = "PricePrediction"

    /**
     * Predicts stock prices using either TensorFlow or statistical methods
     */
    fun predictPrices(
        symbol: String,
        historicalPrices: List<Double>,
        useTensorFlow: Boolean = true
    ): PredictionResult {
        Log.d(TAG, "Prediction requested for $symbol - Using TensorFlow: $useTensorFlow")

        // Try TensorFlow prediction first if requested
        if (useTensorFlow) {
            try {
                Log.d(TAG, "Attempting TensorFlow prediction...")
                val tfResult = tensorFlowService.predictPrices(symbol, historicalPrices)

                if (tfResult != null) {
                    Log.d(TAG, "TensorFlow prediction SUCCESS for $symbol")
                    Log.d(TAG, "Neural network predicted prices: ${tfResult.predictedPrices}")
                    Log.d(TAG, "Neural network confidence: ${tfResult.confidence}")

                    // For comparison, also calculate statistical prediction but don't return it
                    val statResult = predictPricesStatistical(historicalPrices)
                    Log.d(TAG, "Statistical predicted prices (not used): ${statResult.predictedPrices}")
                    Log.d(TAG, "Statistical confidence (not used): ${statResult.confidence}")

                    return PredictionResult(
                        predictedPrices = tfResult.predictedPrices,
                        confidence = tfResult.confidence,
                        modelType = "neural"
                    )
                } else {
                    Log.w(TAG, "TensorFlow prediction returned null for $symbol")
                }
            } catch (e: Exception) {
                Log.e(TAG, "TensorFlow prediction failed, falling back to statistical", e)
            }
        }

        // Fall back to statistical prediction
        Log.d(TAG, "Using statistical prediction for $symbol")
        val result = predictPricesStatistical(historicalPrices)
        Log.d(TAG, "Statistical predicted prices: ${result.predictedPrices}")
        Log.d(TAG, "Statistical confidence: ${result.confidence}")

        return result.copy(modelType = "statistical") // Add model type for tracking
    }

    /**
     * Statistical prediction method
     */
    private fun predictPricesStatistical(historicalPrices: List<Double>): PredictionResult {
        if (historicalPrices.size < 5) {
            Log.d(TAG, "Not enough price history for statistical prediction")
            return PredictionResult(emptyList(), 0.3f, "statistical")
        }

        // Get recent trend (more weight to recent prices)
        val recentPrices = historicalPrices.takeLast(10)
        val trend = calculateWeightedTrend(recentPrices)
        Log.d(TAG, "Statistical weighted trend: $trend")

        // Get volatility
        val volatility = calculateVolatility(recentPrices)
        Log.d(TAG, "Statistical volatility: $volatility")

        // Calculate momentum
        val momentum = calculateMomentum(recentPrices)
        Log.d(TAG, "Statistical momentum: $momentum")

        // Calculate predictions
        val predictions = mutableListOf<Double>()
        var lastPrice = historicalPrices.first() // Use the most recent price (at index 0)

        for (i in 1..5) {
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
        Log.d(TAG, "Statistical consistency: $consistency, final confidence: $confidence")

        return PredictionResult(predictions, confidence, "statistical")
    }

    // Statistical calculation methods
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

    private fun calculateVolatility(prices: List<Double>): Double {
        if (prices.size < 2) return 0.0

        val changes = (1 until prices.size).map { i ->
            abs((prices[i] - prices[i-1]) / prices[i-1])
        }

        return changes.average()
    }

    private fun calculateMomentum(prices: List<Double>): Double {
        if (prices.size < 5) return 0.0

        // Calculate short-term vs long-term trend
        val shortTerm = (prices.last() - prices[prices.size - 3]) / prices[prices.size - 3]
        val longTerm = (prices.last() - prices.first()) / prices.first()

        return (shortTerm * 0.7) + (longTerm * 0.3)
    }

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

    private fun calculateConfidence(volatility: Double, consistency: Double): Float {
        val volatilityFactor = (1.0 - (volatility * 10.0)).coerceIn(0.2, 0.9)
        val confidenceValue = (volatilityFactor * 0.5) + (consistency * 0.5)
        return confidenceValue.toFloat().coerceIn(0.1f, 0.9f)
    }

    /**
     * Result of price prediction, including predicted prices and confidence level
     */
    data class PredictionResult(
        val predictedPrices: List<Double>,
        val confidence: Float, // 0.0-1.0 scale
        val modelType: String = "unknown" // Add model type for tracking
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