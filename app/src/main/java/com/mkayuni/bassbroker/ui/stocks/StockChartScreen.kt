package com.mkayuni.bassbroker.ui.stocks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mkayuni.bassbroker.model.Stock

@Composable
fun StockChartDialog(
    stock: Stock,
    priceHistory: List<Double>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${stock.symbol} Price Chart") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Add explanation text
                Text(
                    text = "Displaying ${priceHistory.size} days of historical data",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Chart
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) {
                    EnhancedStockChart(priceHistory = priceHistory)
                }

                // Price info
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Current: $${String.format("%.2f", stock.currentPrice)}")
                    Text("Previous: $${String.format("%.2f", stock.previousClose)}")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val changeColor = if (stock.priceChange >= 0)
                        Color.Green
                    else
                        Color.Red

                    Text(
                        text = "Change: ${String.format("%+.2f", stock.priceChange)} (${String.format("%.2f", stock.percentChange)}%)",
                        color = changeColor
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}