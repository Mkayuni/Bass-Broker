package com.mkayuni.bassbroker.ui.stocks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mkayuni.bassbroker.model.Stock
import com.mkayuni.bassbroker.service.MarketHoursService
import com.mkayuni.bassbroker.service.MarketStatus
import com.mkayuni.bassbroker.service.PricePredictionService
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun StockList(
    stocks: List<Stock>,
    onStockClick: (Stock) -> Unit,
    marketStatus: MarketStatus = MarketStatus.CLOSED,
    isLoading: Boolean = false,
    predictions: Map<String, PricePredictionService.PredictionResult> = emptyMap(),
    priceHistories: Map<String, List<Double>> = emptyMap()
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Market Status Banner
        MarketStatusBanner(marketStatus = marketStatus)

        // Market Overview Section
        MarketOverviewCard()

        // Loading indicator during refresh
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Stock List
        LazyColumn {
            items(stocks) { stock ->
                EnhancedStockItem(
                    stock = stock,
                    onClick = { onStockClick(stock) },
                    prediction = predictions[stock.symbol],
                    priceHistory = priceHistories[stock.symbol] ?: emptyList()
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun MarketStatusBanner(marketStatus: MarketStatus) {
    val backgroundColor = when (marketStatus) {
        MarketStatus.OPEN -> Color(0xFF43A047) // Green
        MarketStatus.PRE_MARKET -> Color(0xFF1E88E5) // Blue
        MarketStatus.AFTER_HOURS -> Color(0xFF7E57C2) // Purple
        MarketStatus.CLOSED, MarketStatus.CLOSED_WEEKEND -> Color(0xFF757575) // Gray
    }

    val statusText = when (marketStatus) {
        MarketStatus.OPEN -> "Market Open"
        MarketStatus.PRE_MARKET -> "Pre-Market"
        MarketStatus.AFTER_HOURS -> "After Hours"
        MarketStatus.CLOSED -> "Market Closed"
        MarketStatus.CLOSED_WEEKEND -> "Weekend - Market Closed"
    }

    val contextText = when (marketStatus) {
        MarketStatus.OPEN -> "Regular trading hours: 9:30 AM - 4:00 PM ET"
        MarketStatus.PRE_MARKET -> "Early trading session: 4:00 AM - 9:30 AM ET"
        MarketStatus.AFTER_HOURS -> "Late trading session: 4:00 PM - 8:00 PM ET"
        MarketStatus.CLOSED, MarketStatus.CLOSED_WEEKEND -> "Next trading day: 9:30 AM ET"
    }

    val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.AccessTime,
            contentDescription = "Market Hours",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = statusText,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "$contextText • $currentTime ET",
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun MarketOverviewCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Market Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MarketIndexItem(
                    name = "S&P 500",
                    value = "4,891.25",
                    change = "+0.38%",
                    isPositive = true
                )

                MarketIndexItem(
                    name = "VIX",
                    value = "18.23",
                    change = "-2.15%",
                    isPositive = false
                )

                MarketIndexItem(
                    name = "10Y Yield",
                    value = "4.12%",
                    change = "+0.02",
                    isPositive = true
                )
            }
        }
    }
}

@Composable
fun MarketIndexItem(
    name: String,
    value: String,
    change: String,
    isPositive: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = change,
            style = MaterialTheme.typography.bodySmall,
            color = if (isPositive) Color(0xFF43A047) else Color(0xFFE53935)
        )
    }
}

@Composable
fun EnhancedStockItem(
    stock: Stock,
    onClick: () -> Unit,
    prediction: PricePredictionService.PredictionResult?,
    priceHistory: List<Double>
) {
    val changePercent = if (stock.previousClose > 0) {
        ((stock.currentPrice - stock.previousClose) / stock.previousClose) * 100
    } else 0.0

    val changeColor = if (changePercent >= 0) Color(0xFF43A047) else Color(0xFFE53935)

    var expanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        // Main stock row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Symbol and company name
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stock.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = stock.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Right side: Price and change
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "$${stock.currentPrice.formatPrice()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (changePercent >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = "Price trend",
                        tint = changeColor,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "${changePercent.formatPercent()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = changeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Expand/collapse icon
            IconButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.then(Modifier.size(32.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.graphicsLayer {
                        rotationZ = rotationState
                    }
                )
            }
        }

        // Expandable content
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Mini chart
                if (priceHistory.isNotEmpty()) {
                    MiniChart(
                        priceHistory = priceHistory,
                        color = changeColor
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Prediction pill if available
                prediction?.let {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI Prediction:",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        PredictionPill(prediction = it)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Alert indicators
                if (stock.alertThresholdHigh != null || stock.alertThresholdLow != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Alerts:",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row {
                            if (stock.alertThresholdHigh != null) {
                                AlertPill(
                                    value = stock.alertThresholdHigh ?: 0.0,
                                    isHighAlert = true
                                )

                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            if (stock.alertThresholdLow != null) {
                                AlertPill(
                                    value = stock.alertThresholdLow ?: 0.0,
                                    isHighAlert = false
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Key stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatisticItem(
                        label = "Day Range",
                        value = "$${(stock.currentPrice * 0.99).formatPrice()} - $${(stock.currentPrice * 1.01).formatPrice()}"
                    )

                    StatisticItem(
                        label = "Prev. Close",
                        value = "$${stock.previousClose.formatPrice()}"
                    )
                }
            }
        }
    }
}

@Composable
fun StatisticItem(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun MiniChart(
    priceHistory: List<Double>,
    color: Color
) {
    val points = priceHistory.takeLast(30) // Use the last 30 data points

    if (points.size < 2) return

    val min = points.minOrNull() ?: 0.0
    val max = points.maxOrNull() ?: 0.0
    val range = (max - min).coerceAtLeast(0.01) // Avoid division by zero

    // Simple chart visualization
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Draw a simple bar chart
            for (price in points) {
                val heightPercentage = ((price - min) / range).toFloat()

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(heightPercentage.coerceIn(0.05f, 1f))
                        .padding(horizontal = 1.dp)
                        .background(color.copy(alpha = 0.7f))
                )
            }
        }

        // Price range overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$${min.formatPrice()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Text(
                text = "$${max.formatPrice()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun PredictionPill(prediction: PricePredictionService.PredictionResult) {
    val (backgroundColor, textColor) = when (prediction.direction) {
        PricePredictionService.PredictionDirection.STRONGLY_UP -> Color(0xFF00C853) to Color.White
        PricePredictionService.PredictionDirection.UP -> Color(0xFF66BB6A) to Color.White
        PricePredictionService.PredictionDirection.NEUTRAL -> Color(0xFFBDBDBD) to Color.Black
        PricePredictionService.PredictionDirection.DOWN -> Color(0xFFEF5350) to Color.White
        PricePredictionService.PredictionDirection.STRONGLY_DOWN -> Color(0xFFD32F2F) to Color.White
    }

    val directionText = when (prediction.direction) {
        PricePredictionService.PredictionDirection.STRONGLY_UP -> "Strong Buy"
        PricePredictionService.PredictionDirection.UP -> "Buy"
        PricePredictionService.PredictionDirection.NEUTRAL -> "Hold"
        PricePredictionService.PredictionDirection.DOWN -> "Sell"
        PricePredictionService.PredictionDirection.STRONGLY_DOWN -> "Strong Sell"
    }

    val confidenceText = "${(prediction.confidence * 100).toInt()}%"

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = directionText,
            color = textColor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = confidenceText,
            color = textColor,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun AlertPill(
    value: Double,
    isHighAlert: Boolean
) {
    val color = if (isHighAlert) Color(0xFF43A047) else Color(0xFFE53935)
    val icon = if (isHighAlert) Icons.Default.TrendingUp else Icons.Default.TrendingDown

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = if (isHighAlert) "High Alert" else "Low Alert",
            tint = color,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = "$${value.formatPrice()}",
            color = color,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

// Helper functions for formatting
fun Double.formatPrice(): String = String.format("%.2f", this)
fun Double.formatPercent(): String = String.format("%.2f", abs(this))