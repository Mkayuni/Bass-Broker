package com.mkayuni.bassbroker.ui.stocks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A simple price chart implemented in pure Compose without any external libraries.
 * This can be used as a temporary replacement until the MPAndroidChart issues are resolved.
 */
@Composable
fun SimpleStockChart(
    priceHistory: List<Double>,
    predictions: List<Double>? = null,
    confidence: Float = 0f,
    modifier: Modifier = Modifier
) {
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

    // Combine historical data with predictions for min/max calculation if predictions exist
    val allData = if (!predictions.isNullOrEmpty()) priceHistory + predictions else priceHistory
    val min = allData.minOrNull() ?: 0.0
    val max = allData.maxOrNull() ?: 0.0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = if (predictions != null) "${priceHistory.size} days of price history with prediction"
            else "${priceHistory.size} days of price history",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.LightGray.copy(alpha = 0.1f))
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val width = size.width
                val height = size.height
                val range = (max - min).toFloat()

                // Calculate step based on available space for history
                val historyWidth = if (predictions != null) width * 0.7f else width
                val step = historyWidth / (priceHistory.size - 1)

                // Draw the previous close line
                if (priceHistory.size > 1) {
                    val prevCloseY = height - (((priceHistory[1] - min) / (max - min)) * height).toFloat()
                    drawLine(
                        color = Color(0xFFFFA500), // Orange
                        start = Offset(0f, prevCloseY),
                        end = Offset(width, prevCloseY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(10f, 10f)
                        )
                    )
                }

                // Draw the price line
                val path = Path()
                val fillPath = Path()

                // Move to the first point
                val firstX = 0f
                val firstY = height - (((priceHistory[0] - min) / (max - min)) * height).toFloat()
                path.moveTo(firstX, firstY)
                fillPath.moveTo(firstX, height)
                fillPath.lineTo(firstX, firstY)

                // Draw the line through all points
                for (i in 1 until priceHistory.size) {
                    val x = i * step
                    val y = height - (((priceHistory[i] - min) / (max - min)) * height).toFloat()
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }

                // Complete the fill path
                fillPath.lineTo(priceHistory.size * step, height)
                fillPath.close()

                // Draw the fill
                drawPath(
                    path = fillPath,
                    color = Color.Blue.copy(alpha = 0.2f)
                )

                // Draw the line
                drawPath(
                    path = path,
                    color = Color.Blue,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw circles at important points
                val circleRadius = 4.dp.toPx()

                // First point
                drawCircle(
                    color = Color.Blue,
                    radius = circleRadius,
                    center = Offset(firstX, firstY)
                )

                // Last point of history
                val lastHistoryX = (priceHistory.size - 1) * step
                val lastHistoryY = height - (((priceHistory.last() - min) / (max - min)) * height).toFloat()
                drawCircle(
                    color = Color.Blue,
                    radius = circleRadius,
                    center = Offset(lastHistoryX, lastHistoryY)
                )

                // Draw predictions if available
                if (!predictions.isNullOrEmpty()) {
                    // Prediction line
                    val predictionPath = Path()
                    predictionPath.moveTo(lastHistoryX, lastHistoryY)

                    // Step size for prediction - use remaining space
                    val predictionWidth = width - lastHistoryX
                    val predictionStep = predictionWidth / predictions.size

                    // Draw prediction line
                    for (i in predictions.indices) {
                        val x = lastHistoryX + ((i + 1) * predictionStep)
                        val y = height - (((predictions[i] - min) / (max - min)) * height).toFloat()
                        predictionPath.lineTo(x, y)
                    }

                    // Draw prediction line (dashed)
                    drawPath(
                        path = predictionPath,
                        color = Color.Red,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f)),
                            cap = StrokeCap.Round
                        )
                    )

                    // Draw confidence interval if confidence is provided
                    if (confidence > 0) {
                        val confidencePath = Path()

                        // Upper bound
                        confidencePath.moveTo(lastHistoryX, lastHistoryY)
                        for (i in predictions.indices) {
                            val x = lastHistoryX + ((i + 1) * predictionStep)
                            val prediction = predictions[i]
                            // Wider interval for lower confidence
                            val interval = prediction * (0.02 + (1.0 - confidence) * 0.08)
                            val upperY = height - (((prediction + interval - min) / (max - min)) * height).toFloat()
                            confidencePath.lineTo(x, upperY)
                        }

                        // Connect to lower bound
                        for (i in predictions.indices.reversed()) {
                            val x = lastHistoryX + ((i + 1) * predictionStep)
                            val prediction = predictions[i]
                            val interval = prediction * (0.02 + (1.0 - confidence) * 0.08)
                            val lowerY = height - (((prediction - interval - min) / (max - min)) * height).toFloat()
                            confidencePath.lineTo(x, lowerY)
                        }

                        confidencePath.close()

                        // Draw confidence interval
                        drawPath(
                            path = confidencePath,
                            color = Color.Red.copy(alpha = 0.15f)
                        )
                    }

                    // Draw circle at last prediction point
                    val lastPredictionX = lastHistoryX + (predictions.size * predictionStep)
                    val lastPredictionY = height - (((predictions.last() - min) / (max - min)) * height).toFloat()
                    drawCircle(
                        color = Color.Red,
                        radius = circleRadius,
                        center = Offset(lastPredictionX, lastPredictionY)
                    )
                }
            }

            // Add price range labels
            Text(
                text = "$${String.format("%.2f", max)}",
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 4.dp, top = 4.dp)
            )

            Text(
                text = "$${String.format("%.2f", min)}",
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 4.dp, bottom = 4.dp)
            )

            // Add prediction confidence indicator if available
            if (confidence > 0) {
                Text(
                    text = "Confidence: ${(confidence * 100).toInt()}%",
                    fontSize = 10.sp,
                    color = Color.Red,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 4.dp, top = 4.dp)
                )
            }

            // Add day markers
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${priceHistory.size}d",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(start = 4.dp)
                )

                if (priceHistory.size > 3) {
                    Text(
                        text = "Mid",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }

                Text(
                    text = "Now",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                if (predictions != null) {
                    Text(
                        text = "Prediction",
                        fontSize = 10.sp,
                        color = Color.Red,
                        textAlign = TextAlign.End,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }

        // Price info at the bottom
        Spacer(modifier = Modifier.height(8.dp))

        if (priceHistory.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Current: $${String.format("%.2f", priceHistory[0])}",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "Previous: $${String.format("%.2f", priceHistory[1])}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            val change = priceHistory[0] - priceHistory[1]
            val percentChange = if (priceHistory[1] > 0) (change / priceHistory[1] * 100) else 0.0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                val changeColor = if (change >= 0) Color.Green else Color.Red

                Text(
                    text = "Change: ${String.format("%+.2f", change)} (${String.format("%.2f", percentChange)}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = changeColor
                )
            }

            // Add prediction info if available
            if (!predictions.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                val predictedChange = predictions.last() - priceHistory[0]
                val predictedPercentChange = (predictedChange / priceHistory[0] * 100)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val predictionColor = if (predictedChange >= 0) Color.Green else Color.Red

                    Text(
                        text = "Prediction: ${String.format("%+.2f", predictedChange)} (${String.format("%.2f", predictedPercentChange)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = predictionColor
                    )
                }
            }
        }
    }
}