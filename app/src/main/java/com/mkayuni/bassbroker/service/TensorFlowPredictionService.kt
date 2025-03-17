package com.mkayuni.bassbroker.service

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.Calendar

class TensorFlowPredictionService(private val context: Context) {

    companion object {
        private const val TAG = "TFPredictionService"
    }

    // Cache for loaded models
    private val interpreters = mutableMapOf<String, Interpreter>()
    private val metadata = mutableMapOf<String, ModelMetadata>()

    /**
     * Predicts future stock prices using a pre-trained TensorFlow Lite model
     */
    fun predictPrices(symbol: String, historicalPrices: List<Double>): PredictionResult? {
        try {
            // Load model if not already loaded
            if (symbol !in interpreters) {
                loadModel(symbol)
            }

            val interpreter = interpreters[symbol] ?: return null
            val modelMetadata = metadata[symbol] ?: return null

            // Ensure we have enough historical data
            if (historicalPrices.size < modelMetadata.sequenceLength) {
                Log.w(TAG, "Not enough historical data for prediction, need ${modelMetadata.sequenceLength} days")
                return null
            }

            // Prepare input tensor (shape: [1, sequence_length, num_features])
            val recentPrices = historicalPrices.takeLast(modelMetadata.sequenceLength)
            val inputBuffer = prepareInputTensor(recentPrices, modelMetadata)

            // Prepare output tensor (shape: [1, prediction_days])
            val outputBuffer = ByteBuffer.allocateDirect(4 * 1 * modelMetadata.predictionDays)
                .order(ByteOrder.nativeOrder())

            // Run inference
            interpreter.run(inputBuffer, outputBuffer)

            // Process predictions
            val predictions = mutableListOf<Double>()
            outputBuffer.rewind()

            // We only want the first 5 predictions, even though the model produces 35
            val predictionsToUse = minOf(5, modelMetadata.predictionDays)

            for (i in 0 until predictionsToUse) {
                val normalizedPrediction = outputBuffer.getFloat()
                val actualPrice = denormalizePrice(normalizedPrediction.toDouble(), modelMetadata)

                // Sanity check the predictions to avoid extreme values
                val currentPrice = historicalPrices.last()
                val maxReasonablePrice = currentPrice * 1.5 // Limit to 50% increase
                val minReasonablePrice = currentPrice * 0.5 // Limit to 50% decrease

                val sanitizedPrice = actualPrice.coerceIn(minReasonablePrice, maxReasonablePrice)
                predictions.add(sanitizedPrice)

                // Log differences between raw and sanitized predictions
                if (sanitizedPrice != actualPrice) {
                    Log.w(TAG, "Sanitized unreasonable prediction: $actualPrice -> $sanitizedPrice")
                }
            }

            // Calculate a simple confidence score (0.0-1.0)
            val confidence = 0.7f

            return PredictionResult(predictions, confidence)

        } catch (e: Exception) {
            Log.e(TAG, "Error during TensorFlow prediction", e)
            return null
        }
    }

    /**
     * Prepares the input tensor with all 12 features to match the training data
     */
    private fun prepareInputTensor(
        historicalPrices: List<Double>,
        metadata: ModelMetadata
    ): ByteBuffer {
        val sequenceLength = metadata.sequenceLength
        val numFeatures = 12 // Match the 12 features from Python training script
        val inputBuffer = ByteBuffer.allocateDirect(4 * 1 * sequenceLength * numFeatures)
            .order(ByteOrder.nativeOrder())

        // Calculate some additional data needed for features
        val twoYearHighLow = calculateTwoYearHighLow(historicalPrices)

        // Current date for calendar-based features
        val calendar = Calendar.getInstance()

        for (i in 0 until sequenceLength) {
            // Feature 0: Stock Closing price (normalized between 0-1 using metadata)
            val normalizedPrice = normalizePrice(historicalPrices[i], metadata)
            inputBuffer.putFloat(normalizedPrice.toFloat())

            // Feature 1: RSI
            val rsi = computeRSI(
                historicalPrices.subList(
                    maxOf(0, i - 13),
                    minOf(i + 1, historicalPrices.size)
                )
            )
            // Normalize RSI (0-100 scale to 0-1)
            inputBuffer.putFloat((rsi / 100.0).toFloat())

            // Feature 2: MACD
            val macd = computeMACD(
                historicalPrices.subList(
                    maxOf(0, i - 25),
                    minOf(i + 1, historicalPrices.size)
                )
            )
            // Normalize MACD (approximate normalization based on typical values)
            val normalizedMacd = (macd + 5.0) / 10.0 // Simple normalization, adjust as needed
            inputBuffer.putFloat(normalizedMacd.coerceIn(0.0, 1.0).toFloat())

            // Feature 3: Signal Line (simplified approximation)
            val signalLine = approximateSignalLine(macd)
            val normalizedSignal = (signalLine + 5.0) / 10.0 // Simple normalization
            inputBuffer.putFloat(normalizedSignal.coerceIn(0.0, 1.0).toFloat())

            // Feature 4: S&P 500 Close (normalized)
            // Since we don't have real-time S&P data, use a reasonable default
            val normalizedSP = 0.5f // Middle value
            inputBuffer.putFloat(normalizedSP)

            // Feature 5: VIX Close (normalized)
            // Since we don't have real-time VIX data, use a reasonable default
            val normalizedVIX = 0.2f // Default VIX value
            inputBuffer.putFloat(normalizedVIX)

            // Feature 6: 2-Year High
            val twoYearHigh = twoYearHighLow.first
            val normalizedTwoYearHigh = normalizePrice(twoYearHigh, metadata)
            inputBuffer.putFloat(normalizedTwoYearHigh.toFloat())

            // Feature 7: 2-Year Low
            val twoYearLow = twoYearHighLow.second
            val normalizedTwoYearLow = normalizePrice(twoYearLow, metadata)
            inputBuffer.putFloat(normalizedTwoYearLow.toFloat())

            // Feature 8: Drawdown from 2-Year High
            val drawdown = if (twoYearHigh > 0) {
                (historicalPrices[i] - twoYearHigh) / twoYearHigh
            } else {
                0.0
            }
            // Normalize drawdown (typically -1 to 0 range to 0-1)
            val normalizedDrawdown = (drawdown + 1.0) / 2.0
            inputBuffer.putFloat(normalizedDrawdown.coerceIn(0.0, 1.0).toFloat())

            // Feature 9: Month (1-12 normalized to 0-1)
            val month = (calendar.get(Calendar.MONTH) + 1) / 12.0
            inputBuffer.putFloat(month.toFloat())

            // Feature 10: Day of Week (0-6 normalized to 0-1)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0-indexed
            val normalizedDayOfWeek = dayOfWeek / 6.0
            inputBuffer.putFloat(normalizedDayOfWeek.toFloat())

            // Feature 11: Is Holiday Period (0 or 1, already normalized)
            val holidayMonth = calendar.get(Calendar.MONTH) + 1 // 1-indexed
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val isHolidayPeriod = if ((holidayMonth == 12 && day >= 20) || (holidayMonth == 1 && day <= 10)) {
                1.0f
            } else {
                0.0f
            }
            inputBuffer.putFloat(isHolidayPeriod)

            // Increment calendar day for next sequence item
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return inputBuffer
    }

    /**
     * Helper method to calculate 2-year high and low from historical prices
     * Returns a Pair where first is the high and second is the low
     */
    private fun calculateTwoYearHighLow(prices: List<Double>): Pair<Double, Double> {
        // Assuming approximately 252 trading days per year
        val twoYearWindow = minOf(prices.size, 252 * 2)

        if (prices.isEmpty()) {
            return Pair(0.0, 0.0)
        }

        val recentPrices = prices.takeLast(twoYearWindow)
        val high = recentPrices.maxOrNull() ?: prices.last()
        val low = recentPrices.minOrNull() ?: prices.last()

        return Pair(high, low)
    }

    /**
     * Helper method to approximate a signal line from MACD
     */
    private fun approximateSignalLine(macd: Double): Double {
        // In a proper implementation, the signal line is a 9-day EMA of MACD
        // For simplicity, we'll just use a slight smoothing of the MACD value
        return macd * 0.9
    }

    /**
     * Loads the TensorFlow Lite model and its metadata
     */
    private fun loadModel(symbol: String) {
        try {
            // Load metadata first
            val metadataJson = context.assets.open("models/${symbol}_metadata.json")
                .bufferedReader().use { it.readText() }

            val metadataObj = JSONObject(metadataJson)
            val modelMetadata = ModelMetadata(
                minPrice = metadataObj.getDouble("min_price"),
                maxPrice = metadataObj.getDouble("max_price"),
                sequenceLength = metadataObj.getInt("sequence_length"),
                predictionDays = metadataObj.getInt("prediction_days")
            )

            metadata[symbol] = modelMetadata

            // Log metadata for debugging
            Log.d(TAG, "Model metadata for $symbol: min=$${modelMetadata.minPrice}, max=$${modelMetadata.maxPrice}, " +
                    "sequence=${modelMetadata.sequenceLength}, predictions=${modelMetadata.predictionDays}")

            // Load the model file
            val assetManager = context.assets
            val modelFile = assetManager.openFd("models/${symbol}_model.tflite")
            val fileInputStream = FileInputStream(modelFile.fileDescriptor)
            val fileChannel = fileInputStream.channel
            val startOffset = modelFile.startOffset
            val declaredLength = modelFile.declaredLength

            val mappedByteBuffer = fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                startOffset,
                declaredLength
            )

            // Create the interpreter
            val interpreter = Interpreter(mappedByteBuffer)
            interpreters[symbol] = interpreter

            // Log model input shape to help debug future issues
            val inputTensor = interpreter.getInputTensor(0)
            val inputShape = inputTensor.shape()
            Log.d(TAG, "Model input shape for $symbol: ${inputShape.contentToString()}")

            Log.d(TAG, "Successfully loaded model for $symbol")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model for $symbol", e)
        }
    }

    /**
     * Normalizes a price value based on the model's metadata
     */
    private fun normalizePrice(price: Double, metadata: ModelMetadata): Double {
        return (price - metadata.minPrice) / (metadata.maxPrice - metadata.minPrice)
    }

    /**
     * Denormalizes a price value based on the model's metadata
     */
    private fun denormalizePrice(normalizedPrice: Double, metadata: ModelMetadata): Double {
        return normalizedPrice * (metadata.maxPrice - metadata.minPrice) + metadata.minPrice
    }

    /**
     * Computes RSI (Relative Strength Index) for a given dataset
     */
    private fun computeRSI(data: List<Double>): Double {
        if (data.size < 14) {
            return 50.0 // Default value if not enough data
        }

        var gains = 0.0
        var losses = 0.0

        // Calculate average gains and losses
        for (i in 1 until data.size) {
            val change = data[i] - data[i-1]
            if (change > 0) {
                gains += change
            } else {
                losses -= change // Make losses positive
            }
        }

        // Average over the period
        val avgGain = gains / (data.size - 1)
        val avgLoss = losses / (data.size - 1)

        // Calculate RSI
        return if (avgLoss == 0.0) {
            100.0
        } else {
            val rs = avgGain / avgLoss
            100.0 - (100.0 / (1.0 + rs))
        }
    }

    /**
     * Computes MACD (Moving Average Convergence Divergence) for a given dataset
     */
    private fun computeMACD(data: List<Double>): Double {
        if (data.size < 26) {
            // Not enough data to calculate MACD
            return 0.0
        }

        // Step 1: Calculate the 12-period EMA
        val ema12 = calculateEMA(data, 12)

        // Step 2: Calculate the 26-period EMA
        val ema26 = calculateEMA(data, 26)

        // Step 3: Calculate the MACD line (EMA12 - EMA26)
        val macdLine = ema12.last() - ema26.last()

        return macdLine
    }

    /**
     * Calculates the Exponential Moving Average (EMA) for a given dataset and period
     */
    private fun calculateEMA(data: List<Double>, period: Int): List<Double> {
        if (data.size < period) {
            throw IllegalArgumentException("Not enough data points to calculate EMA for period $period")
        }

        val ema = mutableListOf<Double>()
        val smoothingFactor = 2.0 / (period + 1)

        // Calculate the Simple Moving Average (SMA) for the first EMA value
        val sma = data.take(period).average()
        ema.add(sma)

        // Calculate subsequent EMA values
        for (i in period until data.size) {
            val currentPrice = data[i]
            val previousEma = ema.last()
            val currentEma = (currentPrice - previousEma) * smoothingFactor + previousEma
            ema.add(currentEma)
        }

        return ema
    }

    /**
     * Metadata for the TensorFlow Lite model
     */
    data class ModelMetadata(
        val minPrice: Double,
        val maxPrice: Double,
        val sequenceLength: Int,
        val predictionDays: Int
    )

    /**
     * Result of the prediction
     */
    data class PredictionResult(
        val predictedPrices: List<Double>,
        val confidence: Float
    )
}