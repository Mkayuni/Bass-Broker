package com.mkayuni.bassbroker.ui.stocks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mkayuni.bassbroker.model.Stock
import android.annotation.SuppressLint
import android.graphics.Color
import android.view.ViewGroup
import android.widget.LinearLayout
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

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
                // Chart
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) {
                    StockPriceChart(priceHistory)
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
                        androidx.compose.ui.graphics.Color.Green
                    else
                        androidx.compose.ui.graphics.Color.Red

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

@SuppressLint("ViewConstructor")
@Composable
fun StockPriceChart(priceHistory: List<Double>) {
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Configure chart
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)

                // Configure X axis
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)

                // Configure left Y axis
                axisLeft.setDrawGridLines(true)

                // Disable right Y axis
                axisRight.isEnabled = false

                // Set data
                val entries = priceHistory.mapIndexed { index, price ->
                    Entry(index.toFloat(), price.toFloat())
                }

                val dataSet = LineDataSet(entries, "Price").apply {
                    color = Color.BLUE
                    setDrawCircles(false)
                    lineWidth = 2f
                    setDrawValues(false)
                    fillColor = Color.parseColor("#80ADD8E6") // Light blue with alpha
                    setDrawFilled(true)
                }

                data = LineData(dataSet)

                // Refresh chart
                invalidate()
            }
        },
        update = { chart ->
            // Code to update chart if needed
        }
    )
}