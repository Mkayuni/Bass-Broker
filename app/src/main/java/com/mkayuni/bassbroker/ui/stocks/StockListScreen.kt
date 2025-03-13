package com.mkayuni.bassbroker.ui.stocks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mkayuni.bassbroker.model.Stock
import com.mkayuni.bassbroker.viewmodel.StockViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockListScreen(viewModel: StockViewModel) {
    val stocks by viewModel.stocks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showAddDialog by viewModel.showAddStockDialog.collectAsState()
    val selectedStock by viewModel.selectedStock.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bass Broker") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddStockDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Add Stock")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (stocks.isEmpty() && !isLoading) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Add stocks to monitor using the + button",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                // Stock list
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(stocks) { stock ->
                        StockItem(
                            stock = stock,
                            onClick = { viewModel.showStockDetailDialog(stock) }
                        )
                        HorizontalDivider()
                    }
                }
            }

            // Loading indicator
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    // Add Stock Dialog
    if (showAddDialog) {
        AddStockDialog(
            onDismiss = { viewModel.hideAddStockDialog() },
            onAddStock = { symbol ->
                viewModel.addStock(symbol)
                viewModel.hideAddStockDialog()
            }
        )
    }

    // Stock Detail Dialog
    if (selectedStock != null) {
        StockDetailDialog(
            stock = selectedStock!!,
            onDismiss = { viewModel.hideStockDetailDialog() },
            onRemove = {
                viewModel.removeStock(selectedStock!!.symbol)
                viewModel.hideStockDetailDialog()
            }
        )
    }
}

@Composable
fun StockItem(
    stock: Stock,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stock Symbol and Name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stock.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stock.name,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Price and Change
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${String.format("%.2f", stock.currentPrice)}",
                    style = MaterialTheme.typography.titleMedium
                )

                val changeColor = when {
                    stock.priceChange > 0 -> Color.Green
                    stock.priceChange < 0 -> Color.Red
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Text(
                    text = "${String.format("%+.2f", stock.priceChange)} (${String.format("%.2f", stock.percentChange)}%)",
                    color = changeColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun AddStockDialog(
    onDismiss: () -> Unit,
    onAddStock: (String) -> Unit
) {
    var symbol by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Stock") },
        text = {
            OutlinedTextField(
                value = symbol,
                onValueChange = { symbol = it.uppercase() },
                label = { Text("Stock Symbol") },
                singleLine = true,
                placeholder = { Text("Ex: AAPL, MSFT, GOOGL") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = { onAddStock(symbol) },
                enabled = symbol.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun StockDetailDialog(
    stock: Stock,
    onDismiss: () -> Unit,
    onRemove: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${stock.symbol} - ${stock.name}") },
        text = {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Current Price:")
                    Text("$${String.format("%.2f", stock.currentPrice)}")
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Previous Close:")
                    Text("$${String.format("%.2f", stock.previousClose)}")
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Change:")
                    val changeColor = when {
                        stock.priceChange > 0 -> Color.Green
                        stock.priceChange < 0 -> Color.Red
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Text(
                        "${String.format("%+.2f", stock.priceChange)} (${String.format("%.2f", stock.percentChange)}%)",
                        color = changeColor
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    "Configure Alerts",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Text(
                    "High Alert: ${stock.alertThresholdHigh?.let { "$${String.format("%.2f", it)}" } ?: "Not set"}",
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                Text(
                    "Low Alert: ${stock.alertThresholdLow?.let { "$${String.format("%.2f", it)}" } ?: "Not set"}",
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                // Add remove button
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRemove,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Remove Stock")
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