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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mkayuni.bassbroker.model.Stock
import com.mkayuni.bassbroker.model.SoundType
import com.mkayuni.bassbroker.service.MarketStatus
import com.mkayuni.bassbroker.service.PricePredictionService
import com.mkayuni.bassbroker.util.SoundPlayer
import com.mkayuni.bassbroker.viewmodel.StockViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import com.mkayuni.bassbroker.model.MarketIndices
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.font.FontStyle


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
    val marketStatus by viewModel.marketStatus.collectAsState()
    val marketIndices by viewModel.marketIndices.collectAsState()
    val predictions by viewModel.predictions.collectAsState()
    val priceHistories by viewModel.priceHistory.collectAsState()

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            // Use enhanced top bar
            ProfessionalTopBar(onRefreshClick = { viewModel.refreshStocks() })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddStockDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Stock",
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        ) {
            // Wrap everything in a scrollable column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 80.dp)
            ) {
                // Market Status Banner - now part of scrollable content
                EnhancedMarketStatusBanner(marketStatus = marketStatus)

                // Market Overview Card - now part of scrollable content
                EnhancedMarketOverviewCard(marketIndices = marketIndices)

                // Loading indicator during refresh
                if (isLoading && stocks.isNotEmpty()) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                // Empty state or stocks list
                if (stocks.isEmpty() && !isLoading) {
                    // Enhanced empty state
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp), // Give some minimum height
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddChart,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "Add stocks to monitor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            "Tap the + button to get started",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Replace LazyColumn with regular Column for stocks
                    // Each stock is added directly to main scrollable column
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        stocks.forEach { stock ->
                            ProfessionalStockItem(
                                stock = stock,
                                onClick = { viewModel.showStockDetailDialog(stock) },
                                prediction = predictions[stock.symbol],
                                priceHistory = priceHistories[stock.symbol] ?: emptyList()
                            )
                        }
                        // Add some space at the bottom for better UX
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // Loading indicator overlay
            if (isLoading && stocks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Error message at the bottom
            if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp) // Position above FAB
                ) {
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        action = {
                            TextButton(
                                onClick = { viewModel.clearError() },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Dismiss")
                            }
                        }
                    ) {
                        Text(errorMessage ?: "")
                    }
                }
            }
        }
    }

    // Keep existing dialogs
    if (showAddDialog) {
        AddStockDialog(
            onDismiss = { viewModel.hideAddStockDialog() },
            onAddStock = { symbol ->
                viewModel.addStock(symbol)
                viewModel.hideAddStockDialog()
            }
        )
    }

    if (selectedStock != null) {
        val stock = selectedStock!!
        val prices = priceHistories[stock.symbol] ?: emptyList()
        val prediction = predictions[stock.symbol]
        var showChartDialog by remember(stock.symbol) { mutableStateOf(false) }

        StockDetailDialog(
            stock = stock,
            priceHistory = prices,
            prediction = prediction,
            onDismiss = { viewModel.hideStockDetailDialog() },
            onRemove = {
                viewModel.removeStock(stock.symbol)
                viewModel.hideStockDetailDialog()
            },
            onSetHighAlert = { viewModel.showHighAlertConfig(it) },
            onSetLowAlert = { viewModel.showLowAlertConfig(it) },
            onViewChart = { showChartDialog = true },
            onPredict = { s, useTensorFlow -> viewModel.predictStockPrices(s.symbol, useTensorFlow) }
        )

        if (showChartDialog) {
            StockChartDialog(
                stock = stock,
                priceHistory = prices,
                onDismiss = { showChartDialog = false }
            )
        }
    }

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

// Enhanced Market Status Banner
@Composable
fun EnhancedMarketStatusBanner(marketStatus: MarketStatus) {
    val (backgroundColor, startGradient, endGradient) = when (marketStatus) {
        MarketStatus.OPEN -> Triple(
            Color(0xFF1B5E20), // Dark Green
            Color(0xFF2E7D32), // Medium Green
            Color(0xFF388E3C)  // Light Green
        )
        MarketStatus.PRE_MARKET -> Triple(
            Color(0xFF0D47A1), // Dark Blue
            Color(0xFF1565C0), // Medium Blue
            Color(0xFF1976D2)  // Light Blue
        )
        MarketStatus.AFTER_HOURS -> Triple(
            Color(0xFF4A148C), // Dark Purple
            Color(0xFF6A1B9A), // Medium Purple
            Color(0xFF7B1FA2)  // Light Purple
        )
        MarketStatus.CLOSED, MarketStatus.CLOSED_WEEKEND -> Triple(
            Color(0xFF424242), // Dark Gray
            Color(0xFF616161), // Medium Gray
            Color(0xFF757575)  // Light Gray
        )
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
        MarketStatus.CLOSED -> "Next trading day: 9:30 AM ET"
        MarketStatus.CLOSED_WEEKEND -> "Next trading day: 9:30 AM ET"
    }

    val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"))
    val currentIcon = when (marketStatus) {
        MarketStatus.OPEN -> Icons.Default.OpenInNew
        MarketStatus.PRE_MARKET -> Icons.Default.Timelapse
        MarketStatus.AFTER_HOURS -> Icons.Default.Nightlight
        MarketStatus.CLOSED, MarketStatus.CLOSED_WEEKEND -> Icons.Default.Lock
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(startGradient, endGradient)
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(backgroundColor.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = currentIcon,
                    contentDescription = "Market Status",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = statusText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = contextText,
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = currentTime,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "ET",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}


// Enhanced Market Overview Card with professional styling and row layout
@Composable
fun EnhancedMarketOverviewCard(marketIndices: MarketIndices? = null) {
    var selectedTabIndex by remember { mutableStateOf(0) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Market Overview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "Live Data",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom tab selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp)
            ) {
                TabButton(
                    text = "Indices",
                    isSelected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    text = "Bonds",
                    isSelected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content based on selected tab
            AnimatedVisibility(
                visible = selectedTabIndex == 0,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                // Major Indices in a row layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // S&P 500
                    CompactIndexItem(
                        name = "S&P 500",
                        shortName = "SPX",
                        value = marketIndices?.sp500Price?.let { String.format("%,.2f", it) } ?: "Loading...",
                        change = marketIndices?.sp500Change?.let { "${String.format("%+.2f", it)}%" } ?: "0.00%",
                        isPositive = marketIndices?.sp500Change?.let { it >= 0 } ?: true,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // NASDAQ
                    CompactIndexItem(
                        name = "NASDAQ Composite",
                        shortName = "COMP",
                        value = marketIndices?.nasdaqPrice?.let { String.format("%,.2f", it) } ?: "Loading...",
                        change = marketIndices?.nasdaqChange?.let { "${String.format("%+.2f", it)}%" } ?: "0.00%",
                        isPositive = marketIndices?.nasdaqChange?.let { it >= 0 } ?: true,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // VIX
                    CompactIndexItem(
                        name = "CBOE Volatility Index",
                        shortName = "VIX",
                        value = marketIndices?.vixPrice?.let { String.format("%.2f", it) } ?: "Loading...",
                        change = marketIndices?.vixChange?.let { "${String.format("%+.2f", it)}%" } ?: "0.00%",
                        isPositive = marketIndices?.vixChange?.let { it >= 0 } ?: false,
                        modifier = Modifier.weight(1f),
                        showVixNote = false
                    )
                }
            }

            AnimatedVisibility(
                visible = selectedTabIndex == 1,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                // Bond Yields in a row layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 10Y Treasury
                    CompactIndexItem(
                        name = "10-Year Treasury",
                        shortName = "10Y",
                        value = "4.12%",
                        change = "+0.02",
                        isPositive = true,
                        modifier = Modifier.weight(1f),
                        isYield = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // 5Y Treasury
                    CompactIndexItem(
                        name = "5-Year Treasury",
                        shortName = "5Y",
                        value = "3.88%",
                        change = "-0.05",
                        isPositive = false,
                        modifier = Modifier.weight(1f),
                        isYield = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // 2Y Treasury
                    CompactIndexItem(
                        name = "2-Year Treasury",
                        shortName = "2Y",
                        value = "3.62%",
                        change = "-0.03",
                        isPositive = false,
                        modifier = Modifier.weight(1f),
                        isYield = true
                    )
                }
            }

            // Optional: Add a footer with last update time
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Last updated: ${formatCurrentTime()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// Enhanced tab button
@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Helper function to format current time
private fun formatCurrentTime(): String {
    val formatter = DateTimeFormatter.ofPattern("h:mm a")
    return LocalTime.now().format(formatter)
}

// Compact index item for row layout
@Composable
fun CompactIndexItem(
    name: String,
    shortName: String,
    value: String,
    change: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier,
    isYield: Boolean = false,
    showVixNote: Boolean = false
) {
    val accentColor = if (isPositive) Color(0xFF00C853) else Color(0xFFE53935)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Symbol and name
            Text(
                text = shortName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            // Full name with smaller font
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Value
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (isYield) {
                Text(
                    text = "Yield",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Change pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(accentColor.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = if (isPositive) "Up" else "Down",
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = change,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
        }
    }
}

// Sample chart data generator for illustration
private fun generateSampleChartData(isPositive: Boolean): List<Float> {
    val baseValue = 50f
    val random = kotlin.random.Random(System.currentTimeMillis())
    return List(24) { index ->
        val randomFactor = random.nextFloat() * 10f - 5f
        val trend = if (isPositive) index * 0.5f else -index * 0.5f
        baseValue + randomFactor + trend
    }
}

// Professional index item with mini chart
@Composable
fun ProfessionalIndexItem(
    name: String,
    shortName: String,
    value: String,
    change: String,
    isPositive: Boolean,
    chartData: List<Float>,
    isYield: Boolean = false,
    showVixNote: Boolean = false
) {
    val accentColor = if (isPositive) Color(0xFF00C853) else Color(0xFFE53935)

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Index info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Index badge
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = shortName.first().toString(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Index name
                    Column {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = shortName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )

                            if (showVixNote) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "(Fear Index)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Value and change
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Change pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(accentColor.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = if (isPositive) "Up" else "Down",
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = change,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }

                    if (isYield) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Yield",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            // Right side: Mini chart
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(4.dp)
            ) {
                // Draw the mini chart
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (chartData.size >= 2) {
                        val path = Path()
                        val width = size.width
                        val height = size.height
                        val xStep = width / (chartData.size - 1)

                        // Find min/max for normalization
                        val min = chartData.minOrNull() ?: 0f
                        val max = chartData.maxOrNull() ?: 100f
                        val range = (max - min).coerceAtLeast(0.01f)

                        // Create the path
                        chartData.forEachIndexed { index, value ->
                            val x = index * xStep
                            val normalizedY = 1f - ((value - min) / range)
                            val y = height * normalizedY

                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }

                        // Draw the line
                        drawPath(
                            path = path,
                            color = accentColor,
                            style = Stroke(
                                width = 2.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // Optional: Draw area under the line
                        val fillPath = Path()
                        fillPath.addPath(path)
                        fillPath.lineTo(width, height)
                        fillPath.lineTo(0f, height)
                        fillPath.close()

                        drawPath(
                            path = fillPath,
                            color = accentColor.copy(alpha = 0.1f),
                            style = Fill
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedMarketIndexItem(
    name: String,
    value: String,
    change: String,
    isPositive: Boolean,
    iconRes: ImageVector
) {
    val accentColor = if (isPositive) Color(0xFF00C853) else Color(0xFFE53935)

    Card(
        modifier = Modifier
            .width(110.dp)
            .height(90.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = iconRes,
                    contentDescription = name,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(accentColor.copy(alpha = 0.1f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = change,
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Professional Top Bar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalTopBar(onRefreshClick: () -> Unit) {
    // Configure whether to use personal branding
    val usePersonalBranding = true // Set to false to use "Bass Broker" only

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo as finance icon
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = "Bass Broker Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                if (usePersonalBranding) {
                    // Two-part title with personal branding
                    Text(
                        text = "Kayuni's ",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Bass Broker",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    // Single title without personal branding
                    Text(
                        text = "Bass Broker",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        actions = {
            // Action buttons with tooltips
            IconButton(
                onClick = { /* Open settings */ },
                modifier = Modifier.tooltipAnchor("Settings")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }

            IconButton(
                onClick = onRefreshClick,
                modifier = Modifier.tooltipAnchor("Refresh Data")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.shadow(elevation = 3.dp)
    )
}

// Simple tooltip anchor extension (for devices that support tooltips)
fun Modifier.tooltipAnchor(text: String): Modifier {
    return this.then(
        // For platforms supporting tooltips, you would use:
        // Modifier.semantics { tooltip = text }
        // Since this functionality might be limited, we'll use a placeholder for now
        Modifier
    )
}

// Enhanced Stock Item
@Composable
fun ProfessionalStockItem(
    stock: Stock,
    onClick: () -> Unit,
    prediction: PricePredictionService.PredictionResult?,
    priceHistory: List<Double>
) {
    // Define more professional colors
    val positiveColor = Color(0xFF00C853) // Vibrant green
    val negativeColor = Color(0xFFE53935) // Vibrant red
    val neutralBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    val changePercent = if (stock.previousClose > 0) {
        ((stock.currentPrice - stock.previousClose) / stock.previousClose) * 100
    } else 0.0

    val changeColor = if (changePercent >= 0) positiveColor else negativeColor

    var expanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            // Main stock row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stock Logo (showing first letter)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stock.symbol.first().toString(),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

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

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(changeColor.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (changePercent >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = "Price trend",
                                tint = changeColor,
                                modifier = Modifier.size(14.dp)
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(
                                text = "${changePercent.formatPercent()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = changeColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Mini chart with improved visual style
                    if (priceHistory.isNotEmpty()) {
                        ProfessionalMiniChart(
                            priceHistory = priceHistory,
                            color = changeColor
                        )

                        Spacer(modifier = Modifier.height(12.dp))
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
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )

                            ProfessionalPredictionPill(prediction = it)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
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
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )

                            Row {
                                if (stock.alertThresholdHigh != null) {
                                    ProfessionalAlertPill(
                                        value = stock.alertThresholdHigh ?: 0.0,
                                        isHighAlert = true
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                if (stock.alertThresholdLow != null) {
                                    ProfessionalAlertPill(
                                        value = stock.alertThresholdLow ?: 0.0,
                                        isHighAlert = false
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Key stats with improved visuals
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = neutralBackground
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Day Range",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = "${(stock.currentPrice * 0.99).formatPrice()} - ${(stock.currentPrice * 1.01).formatPrice()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = neutralBackground
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Prev. Close",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = "${stock.previousClose.formatPrice()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfessionalMiniChart(
    priceHistory: List<Double>,
    color: Color
) {
    val points = priceHistory.takeLast(30) // Use the last 30 data points

    if (points.size < 2) return

    val min = points.minOrNull() ?: 0.0
    val max = points.maxOrNull() ?: 0.0
    val range = (max - min).coerceAtLeast(0.01) // Avoid division by zero

    // Improved chart visualization
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            // Draw a line for the chart
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val step = width / (points.size - 1)

                val path = Path().apply {
                    for (i in points.indices) {
                        val x = i * step
                        val normalizedY = ((points[i] - min) / range).toFloat()
                        val y = height - (normalizedY * height)

                        if (i == 0) {
                            moveTo(x, y)
                        } else {
                            lineTo(x, y)
                        }
                    }
                }

                // Draw the line
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Optional: Draw area under the line
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    color = color.copy(alpha = 0.1f),
                    style = Fill
                )
            }

            // Price range overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${min.formatPrice()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Text(
                    text = "${max.formatPrice()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun ProfessionalPredictionPill(prediction: PricePredictionService.PredictionResult) {
    val (backgroundColor, textColor, iconVector) = when (prediction.direction) {
        PricePredictionService.PredictionDirection.STRONGLY_UP -> Triple(
            Color(0xFF00C853), Color.White, Icons.Default.TrendingUp
        )
        PricePredictionService.PredictionDirection.UP -> Triple(
            Color(0xFF66BB6A), Color.White, Icons.Default.TrendingUp
        )
        PricePredictionService.PredictionDirection.NEUTRAL -> Triple(
            Color(0xFFBDBDBD), Color.Black, Icons.Default.HorizontalRule
        )
        PricePredictionService.PredictionDirection.DOWN -> Triple(
            Color(0xFFEF5350), Color.White, Icons.Default.TrendingDown
        )
        PricePredictionService.PredictionDirection.STRONGLY_DOWN -> Triple(
            Color(0xFFD32F2F), Color.White, Icons.Default.TrendingDown
        )
    }

    val directionText = when (prediction.direction) {
        PricePredictionService.PredictionDirection.STRONGLY_UP -> "Strong Buy"
        PricePredictionService.PredictionDirection.UP -> "Buy"
        PricePredictionService.PredictionDirection.NEUTRAL -> "Hold"
        PricePredictionService.PredictionDirection.DOWN -> "Sell"
        PricePredictionService.PredictionDirection.STRONGLY_DOWN -> "Strong Sell"
    }

    val confidenceText = "${(prediction.confidence * 100).toInt()}%"

    Card(
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = directionText,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

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
}

@Composable
fun ProfessionalAlertPill(
    value: Double,
    isHighAlert: Boolean
) {
    val color = if (isHighAlert) Color(0xFF43A047) else Color(0xFFE53935)
    val icon = if (isHighAlert) Icons.Default.TrendingUp else Icons.Default.TrendingDown

    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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
                text = "${value.formatPrice()}",
                color = color,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Keep existing dialogs
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
    prediction: PricePredictionService.PredictionResult? = null,
    onDismiss: () -> Unit,
    onRemove: () -> Unit,
    onSetHighAlert: (Stock) -> Unit,
    onSetLowAlert: (Stock) -> Unit,
    onViewChart: (Stock) -> Unit,
    onPredict: (Stock, Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${stock.symbol} - ${stock.name}") },
        text = {
            var useNeuralNetwork by remember { mutableStateOf(true) }
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Chart
                if (priceHistory.isNotEmpty()) {
                    Text("Price History", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        EnhancedStockChart(
                            priceHistory = priceHistory,
                            predictions = prediction?.predictedPrices,
                            confidence = prediction?.confidence ?: 0f,
                            isNeuralNetwork = useNeuralNetwork
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Stock Details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Current Price:")
                    Text("${String.format("%.2f", stock.currentPrice)}")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Previous Close:")
                    Text("${String.format("%.2f", stock.previousClose)}")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("Configure Alerts", style = MaterialTheme.typography.titleMedium)

                // High Alert
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("High Alert: ${stock.alertThresholdHigh?.let { "${String.format("%.2f", it)}" } ?: "Not set"}")
                    Button(onClick = { onSetHighAlert(stock) }) { Text("Set") }
                }

                // Low Alert
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Low Alert: ${stock.alertThresholdLow?.let { "${String.format("%.2f", it)}" } ?: "Not set"}")
                    Button(onClick = { onSetLowAlert(stock) }) { Text("Set") }
                }

                // Chart and Prediction Buttons
                Button(onClick = { onViewChart(stock) }, modifier = Modifier.fillMaxWidth()) {
                    Text("View Chart")
                }

                // Neural Network Prediction
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Use Neural Network")
                    Switch(
                        checked = useNeuralNetwork,
                        onCheckedChange = { useNeuralNetwork = it }
                    )
                }

                Button(
                    onClick = { onPredict(stock, useNeuralNetwork) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Predict Price Trend")
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Remove Button
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
            Button(onClick = onDismiss) { Text("Close") }
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
    var selectedSoundType by remember { mutableStateOf(SoundType.PRICE_UP) }
    val context = LocalContext.current
    val soundPlayer = remember { SoundPlayer(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set ${if (isHighAlert) "High" else "Low"} Alert for ${stock.symbol}") },
        text = {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = threshold,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) threshold = it },
                    label = { Text("Price Threshold") },
                    prefix = { Text("$") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                Text("Select Sound Effect", style = MaterialTheme.typography.titleMedium)

                Column {
                    SoundTypeOption("🚀 Upward Bass", SoundType.PRICE_UP, selectedSoundType == SoundType.PRICE_UP,
                        onClick = { selectedSoundType = SoundType.PRICE_UP }, onPlaySound = { soundPlayer.playSound(SoundType.PRICE_UP) })

                    SoundTypeOption("📉 Downward Bass", SoundType.PRICE_DOWN, selectedSoundType == SoundType.PRICE_DOWN,
                        onClick = { selectedSoundType = SoundType.PRICE_DOWN }, onPlaySound = { soundPlayer.playSound(SoundType.PRICE_DOWN) })

                    SoundTypeOption("🎵 Stable Bass", SoundType.PRICE_STABLE, selectedSoundType == SoundType.PRICE_STABLE,
                        onClick = { selectedSoundType = SoundType.PRICE_STABLE }, onPlaySound = { soundPlayer.playSound(SoundType.PRICE_STABLE) })

                    SoundTypeOption("🎸 Custom Bass", SoundType.CUSTOM, selectedSoundType == SoundType.CUSTOM,
                        onClick = { selectedSoundType = SoundType.CUSTOM }, onPlaySound = { soundPlayer.playSound(SoundType.CUSTOM) })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    threshold.toDoubleOrNull()?.let { thresholdValue -> onSave(thresholdValue, selectedSoundType) }
                },
                enabled = threshold.isNotEmpty() && threshold.toDoubleOrNull() != null
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
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

