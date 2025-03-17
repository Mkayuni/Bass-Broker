package com.mkayuni.bassbroker.ui.stocks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Enhanced stock chart with clear price history and prediction visualization
 */
@Composable
fun EnhancedStockChart(
    priceHistory: List<Double>,
    predictions: List<Double>? = null,
    confidence: Float = 0f,
    isNeuralNetwork: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Handle empty data case
    if (priceHistory.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.LightGray.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text("No price data available")
        }
        return
    }

    // Calculate data ranges with padding
    val allData = if (predictions.isNullOrEmpty()) priceHistory else priceHistory + predictions
    val minValue = (allData.minOrNull() ?: 0.0)
    val maxValue = (allData.maxOrNull() ?: 0.0)

    // Add padding to the range to prevent drawing at edges
    val valueRange = maxValue - minValue
    val paddedMin = minValue - (valueRange * 0.05)
    val paddedMax = maxValue + (valueRange * 0.05)

    // Colors
    val historyLineColor = Color(0xFF1E88E5)  // Blue
    val positiveGreen = Color(0xFF4CAF50)
    val negativeRed = Color(0xFFE53935)
    val gridLineColor = Color(0x22000000)

    // Determine prediction direction for appropriate coloring
    val predictionDirection = if (predictions != null && predictions.isNotEmpty()) {
        val lastHistory = priceHistory.first()
        val lastPrediction = predictions.last()
        if (lastPrediction > lastHistory) "up" else if (lastPrediction < lastHistory) "down" else "neutral"
    } else {
        "neutral"
    }

    // Set prediction color based on direction
    val predictionLineColor = when (predictionDirection) {
        "up" -> positiveGreen
        "down" -> negativeRed
        else -> Color.Gray
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Chart header with legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${priceHistory.size} Day Price History",
                style = MaterialTheme.typography.titleSmall
            )

            // Legend
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // History
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(historyLineColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "History",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                }

                // Prediction
                if (predictions != null && predictions.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(predictionLineColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${if (isNeuralNetwork) "Neural Net" else "Statistical"} (${(confidence * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main chart area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .shadow(1.dp, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // The actual chart canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            // Draw subtle grid lines (horizontal)
                            val gridCount = 3
                            for (i in 0..gridCount) {
                                val y = size.height * i / gridCount
                                drawLine(
                                    color = gridLineColor,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                                )
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val valueRange = paddedMax - paddedMin

                    // Calculate chart areas
                    val historyWidth = if (predictions != null) width * 0.7f else width
                    val historyStep = historyWidth / (priceHistory.size - 1).coerceAtLeast(1)

                    // Function to calculate Y position from price
                    val getYPosition: (Double) -> Float = { price ->
                        height - ((price - paddedMin) / valueRange * height).toFloat()
                    }

                    // Draw history line
                    val historyPath = Path()
                    val historyFillPath = Path()

                    // Start paths
                    val firstX = historyWidth
                    val firstY = getYPosition(priceHistory.first())
                    historyPath.moveTo(firstX, firstY)
                    historyFillPath.moveTo(firstX, height)
                    historyFillPath.lineTo(firstX, firstY)

                    // Draw history points in REVERSE order (newest to oldest)
                    for (i in 1 until priceHistory.size) {
                        val x = historyWidth - (i * historyStep)
                        val y = getYPosition(priceHistory[i])
                        historyPath.lineTo(x, y)
                        historyFillPath.lineTo(x, y)
                    }

                    // Complete fill path
                    val lastHistoryX = 0f  // Leftmost point is oldest history point
                    historyFillPath.lineTo(lastHistoryX, height)
                    historyFillPath.close()

                    // Draw history fill
                    drawPath(
                        path = historyFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                historyLineColor.copy(alpha = 0.2f),
                                historyLineColor.copy(alpha = 0f)
                            )
                        )
                    )

                    // Draw history line
                    drawPath(
                        path = historyPath,
                        color = historyLineColor,
                        style = Stroke(
                            width = 2.5f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Draw prediction if available
                    if (!predictions.isNullOrEmpty()) {
                        val predictionPath = Path()
                        val predictionFillPath = Path()

                        // Start from current price point
                        predictionPath.moveTo(firstX, firstY)
                        predictionFillPath.moveTo(firstX, height)
                        predictionFillPath.lineTo(firstX, firstY)

                        // Calculate prediction parameters
                        val predictionWidth = width - historyWidth
                        val predictionStep = predictionWidth / predictions.size

                        // Draw prediction line
                        for (i in predictions.indices) {
                            val x = firstX + ((i + 1) * predictionStep)
                            val y = getYPosition(predictions[i])
                            predictionPath.lineTo(x, y)
                            predictionFillPath.lineTo(x, y)
                        }

                        // Complete prediction fill path
                        val lastPredictionX = firstX + (predictions.size * predictionStep)
                        val lastPredictionY = getYPosition(predictions.last())
                        predictionFillPath.lineTo(lastPredictionX, height)
                        predictionFillPath.close()

                        // Draw prediction fill
                        drawPath(
                            path = predictionFillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    predictionLineColor.copy(alpha = 0.2f),
                                    predictionLineColor.copy(alpha = 0f)
                                )
                            )
                        )

                        // Draw confidence interval if confidence > 0
                        if (confidence > 0) {
                            val confPath = Path()
                            val intervalFactor = 0.02 + (1.0 - confidence) * 0.08

                            // Start from current price
                            confPath.moveTo(firstX, firstY)

                            // Upper bound
                            for (i in predictions.indices) {
                                val x = firstX + ((i + 1) * predictionStep)
                                val prediction = predictions[i]
                                val interval = prediction * intervalFactor
                                val upperY = getYPosition(prediction + interval)
                                confPath.lineTo(x, upperY)
                            }

                            // Connect to lower bound (reversed)
                            for (i in predictions.indices.reversed()) {
                                val x = firstX + ((i + 1) * predictionStep)
                                val prediction = predictions[i]
                                val interval = prediction * intervalFactor
                                val lowerY = getYPosition(prediction - interval)
                                confPath.lineTo(x, lowerY)
                            }

                            confPath.close()

                            // Draw confidence interval
                            drawPath(
                                path = confPath,
                                color = predictionLineColor.copy(alpha = 0.15f)
                            )
                        }

                        // Draw prediction line
                        drawPath(
                            path = predictionPath,
                            color = predictionLineColor,
                            style = Stroke(
                                width = 2.5f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 3f))
                            )
                        )

                        // Draw important points
                        // Current price point
                        drawCircle(
                            color = Color.White,
                            radius = 5.dp.toPx(),
                            center = Offset(firstX, firstY)
                        )
                        drawCircle(
                            color = historyLineColor,
                            radius = 3.dp.toPx(),
                            center = Offset(firstX, firstY)
                        )

                        // Final prediction point
                        drawCircle(
                            color = Color.White,
                            radius = 5.dp.toPx(),
                            center = Offset(lastPredictionX, lastPredictionY)
                        )
                        drawCircle(
                            color = predictionLineColor,
                            radius = 3.dp.toPx(),
                            center = Offset(lastPredictionX, lastPredictionY)
                        )

                        // PREDICTION PRICE LABEL - Position inside chart bounds
                        // Calculate position to ensure it stays within the chart boundaries
                        val labelWidth = 50.dp.toPx()
                        val labelHeight = 18.dp.toPx()

                        // Calculate X position to keep label fully inside chart
                        val predLabelX = lastPredictionX.coerceIn(
                            labelWidth / 2 + 5.dp.toPx(),
                            width - labelWidth / 2 - 5.dp.toPx()
                        )

                        // Always place label at top with padding
                        val predLabelY = 10.dp.toPx()

                        drawRoundRect(
                            color = predictionLineColor,
                            topLeft = Offset(predLabelX - labelWidth / 2, predLabelY),
                            size = Size(labelWidth, labelHeight),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )

                        // Draw price text
                        drawContext.canvas.nativeCanvas.drawText(
                            "${String.format("%.2f", predictions.last())}",
                            predLabelX,
                            predLabelY + 12.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = 11.sp.toPx()
                                isFakeBoldText = true
                            }
                        )

                        // Add dotted line connecting label to point
                        drawLine(
                            color = predictionLineColor,
                            start = Offset(lastPredictionX, 28.dp.toPx()),
                            end = Offset(lastPredictionX, lastPredictionY - 5.dp.toPx()),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f))
                        )

                        // CURRENT PRICE LABEL - Position inside chart bounds
                        // Calculate position to ensure it stays within the chart boundaries
                        val curLabelX = firstX.coerceIn(
                            labelWidth / 2 + 5.dp.toPx(),
                            width - labelWidth / 2 - 5.dp.toPx()
                        )

                        drawRoundRect(
                            color = historyLineColor,
                            topLeft = Offset(curLabelX - labelWidth / 2, predLabelY),
                            size = Size(labelWidth, labelHeight),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )

                        // Draw price text
                        drawContext.canvas.nativeCanvas.drawText(
                            "${String.format("%.2f", priceHistory.first())}",
                            curLabelX,
                            predLabelY + 12.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = 11.sp.toPx()
                                isFakeBoldText = true
                            }
                        )

                        // Add dotted line connecting label to point
                        drawLine(
                            color = historyLineColor,
                            start = Offset(firstX, 28.dp.toPx()),
                            end = Offset(firstX, firstY - 5.dp.toPx()),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f))
                        )
                    } else {
                        // Just show current price if no prediction
                        val labelWidth = 50.dp.toPx()
                        val labelHeight = 18.dp.toPx()
                        val predLabelY = 10.dp.toPx()

                        // Calculate X position to keep label fully inside chart
                        val curLabelX = firstX.coerceIn(
                            labelWidth / 2 + 5.dp.toPx(),
                            width - labelWidth / 2 - 5.dp.toPx()
                        )

                        drawRoundRect(
                            color = historyLineColor,
                            topLeft = Offset(curLabelX - labelWidth / 2, predLabelY),
                            size = Size(labelWidth, labelHeight),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )

                        // Draw current price text
                        drawContext.canvas.nativeCanvas.drawText(
                            "${String.format("%.2f", priceHistory.first())}",
                            curLabelX,
                            predLabelY + 12.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = 11.sp.toPx()
                                isFakeBoldText = true
                            }
                        )

                        // Add dotted line connecting label to point
                        drawLine(
                            color = historyLineColor,
                            start = Offset(curLabelX, predLabelY + labelHeight),
                            end = Offset(firstX, firstY - 5.dp.toPx()),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f))
                        )
                    }
                }
            }
        }
    }
}

/**
 * Backward compatibility function that calls EnhancedStockChart
 */
@Composable
fun SimpleStockChart(
    priceHistory: List<Double>,
    predictions: List<Double>? = null,
    confidence: Float = 0f,
    isNeuralNetwork: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Call the enhanced version with the same parameters
    EnhancedStockChart(
        priceHistory = priceHistory,
        predictions = predictions,
        confidence = confidence,
        isNeuralNetwork = isNeuralNetwork,
        modifier = modifier
    )
}