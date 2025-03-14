package com.mkayuni.bassbroker.ui.stocks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mkayuni.bassbroker.model.Stock
import com.mkayuni.bassbroker.model.SoundType
import com.mkayuni.bassbroker.util.SoundPlayer
import com.mkayuni.bassbroker.viewmodel.StockViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockListScreen(viewModel: StockViewModel) {
    val stocks by viewModel.stocks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showAddDialog by viewModel.showAddStockDialog.collectAsState()
    val selectedStock by viewModel.selectedStock.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val showAlertConfigDialog by viewModel.showAlertConfigDialog.collectAsState()
    val selectedAlertConfig by viewModel.selectedAlertConfig.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bass Broker") },
                actions = {
                    IconButton(onClick = { viewModel.refreshStocks() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
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

            // Error message
            if (errorMessage != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(errorMessage ?: "")
                }
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
        val stock = selectedStock!!
        val prices = viewModel.priceHistory.collectAsState().value[stock.symbol] ?: emptyList()

        StockDetailDialog(
            stock = stock,
            priceHistory = prices,  // Add this line
            onDismiss = { viewModel.hideStockDetailDialog() },
            onRemove = {
                viewModel.removeStock(stock.symbol)
                viewModel.hideStockDetailDialog()
            },
            onSetHighAlert = { viewModel.showHighAlertConfig(it) },
            onSetLowAlert = { viewModel.showLowAlertConfig(it) }
        )
    }

    // Alert Configuration Dialog
    if (showAlertConfigDialog && selectedAlertConfig != null) {
        val config = selectedAlertConfig!!
        AlertConfigDialog(
            stock = config.first,
            isHighAlert = config.second,
            currentThreshold = config.third,
            onDismiss = { viewModel.hideAlertConfigDialog() },
            onSave = { threshold, soundType ->
                viewModel.saveAlertConfig(threshold, soundType)
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
    priceHistory: List<Double>,
    onDismiss: () -> Unit,
    onRemove: () -> Unit,
    onSetHighAlert: (Stock) -> Unit,
    onSetLowAlert: (Stock) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${stock.symbol} - ${stock.name}") },
        text = {
            Column(modifier = Modifier.padding(8.dp)) {
                // Add chart at the top
                if (priceHistory.isNotEmpty()) {
                    Text("Price History", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    StockPriceChart(priceHistory = priceHistory)
                    Spacer(modifier = Modifier.height(16.dp))
                }
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

                // High alert row with button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("High Alert: ${stock.alertThresholdHigh?.let { "$${String.format("%.2f", it)}" } ?: "Not set"}")

                    Button(
                        onClick = { onSetHighAlert(stock) },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Set")
                    }
                }

                // Low alert row with button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Low Alert: ${stock.alertThresholdLow?.let { "$${String.format("%.2f", it)}" } ?: "Not set"}")

                    Button(
                        onClick = { onSetLowAlert(stock) },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Set")
                    }
                }

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

@Composable
fun AlertConfigDialog(
    stock: Stock,
    isHighAlert: Boolean,
    currentThreshold: Double?,
    onDismiss: () -> Unit,
    onSave: (Double, SoundType) -> Unit
) {
    var threshold by remember { mutableStateOf(currentThreshold?.toString() ?: "") }
    var selectedSoundType by remember { mutableStateOf<SoundType>(SoundType.PRICE_UP) }
    val context = LocalContext.current
    val soundPlayer = remember { SoundPlayer(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set ${if (isHighAlert) "High" else "Low"} Alert for ${stock.symbol}") },
        text = {
            Column(modifier = Modifier.padding(8.dp)) {
                // Threshold input
                OutlinedTextField(
                    value = threshold,
                    onValueChange = {
                        // Only allow numeric input with decimal point
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            threshold = it
                        }
                    },
                    label = { Text("Price Threshold") },
                    prefix = { Text("$") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                // Sound selection
                Text(
                    "Select Sound Effect",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Radio buttons for sound types
                Column {
                    SoundTypeOption(
                        name = "🚀 Upward Bass",
                        soundType = SoundType.PRICE_UP,
                        isSelected = selectedSoundType == SoundType.PRICE_UP,
                        onClick = { selectedSoundType = SoundType.PRICE_UP },
                        onPlaySound = { soundPlayer.playSound(SoundType.PRICE_UP) }
                    )

                    SoundTypeOption(
                        name = "📉 Downward Bass",
                        soundType = SoundType.PRICE_DOWN,
                        isSelected = selectedSoundType == SoundType.PRICE_DOWN,
                        onClick = { selectedSoundType = SoundType.PRICE_DOWN },
                        onPlaySound = { soundPlayer.playSound(SoundType.PRICE_DOWN) }
                    )

                    SoundTypeOption(
                        name = "🎵 Stable Bass",
                        soundType = SoundType.PRICE_STABLE,
                        isSelected = selectedSoundType == SoundType.PRICE_STABLE,
                        onClick = { selectedSoundType = SoundType.PRICE_STABLE },
                        onPlaySound = { soundPlayer.playSound(SoundType.PRICE_STABLE) }
                    )

                    SoundTypeOption(
                        name = "🎸 Custom Bass",
                        soundType = SoundType.CUSTOM,
                        isSelected = selectedSoundType == SoundType.CUSTOM,
                        onClick = { selectedSoundType = SoundType.CUSTOM },
                        onPlaySound = { soundPlayer.playSound(SoundType.CUSTOM) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    threshold.toDoubleOrNull()?.let { thresholdValue ->
                        onSave(thresholdValue, selectedSoundType)
                    }
                },
                enabled = threshold.isNotEmpty() && threshold.toDoubleOrNull() != null
            ) {
                Text("Save")
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
fun SoundTypeOption(
    name: String,
    soundType: SoundType,
    isSelected: Boolean,
    onClick: () -> Unit,
    onPlaySound: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )

        Text(
            text = name,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick)
                .padding(start = 8.dp)
        )

        IconButton(onClick = onPlaySound) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Play Sound"
            )
        }
    }
}