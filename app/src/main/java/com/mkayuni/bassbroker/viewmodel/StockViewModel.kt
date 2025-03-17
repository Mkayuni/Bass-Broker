package com.mkayuni.bassbroker.viewmodel

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mkayuni.bassbroker.service.MarketHoursService
import com.mkayuni.bassbroker.service.MarketStatus
import com.mkayuni.bassbroker.service.NewsRepository
import com.mkayuni.bassbroker.service.StockRepository
import com.mkayuni.bassbroker.util.PatternDetector
import com.mkayuni.bassbroker.model.Stock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import com.mkayuni.bassbroker.util.SoundPlayer
import com.mkayuni.bassbroker.model.SoundType
import android.content.Context
import android.util.Log
import com.mkayuni.bassbroker.service.PricePredictionService
import com.mkayuni.bassbroker.model.MarketIndices

class StockViewModel(application: Application) : AndroidViewModel(application) {

    private val stockRepository = StockRepository()
    private val newsRepository = NewsRepository()
    private val marketHoursService = MarketHoursService()
    private val soundPlayer = SoundPlayer(application)
    private val repository = StockRepository(this)

    // State flows for UI updates
    private val _stocks = MutableStateFlow<List<Stock>>(emptyList())
    val stocks: StateFlow<List<Stock>> = _stocks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _marketStatus = MutableStateFlow(MarketStatus.CLOSED)
    val marketStatus: StateFlow<MarketStatus> = _marketStatus.asStateFlow()

    private val _showAddStockDialog = MutableStateFlow(false)
    val showAddStockDialog: StateFlow<Boolean> = _showAddStockDialog.asStateFlow()

    private val _selectedStock = MutableStateFlow<Stock?>(null)
    val selectedStock: StateFlow<Stock?> = _selectedStock.asStateFlow()

    // Historical price data for pattern detection
    private val historicalPrices = mutableMapOf<String, MutableList<Double>>()

    // Handler for periodic updates
    private val handler = Handler(Looper.getMainLooper())
    private val marketCheckRunnable = object : Runnable {
        override fun run() {
            checkMarketStatus()
            handler.postDelayed(this, TimeUnit.MINUTES.toMillis(1))
        }
    }

    // Store sound selections for alerts
    private val _alertSounds = MutableStateFlow<Map<String, Map<Boolean, SoundType>>>(emptyMap())
    val alertSounds: StateFlow<Map<String, Map<Boolean, SoundType>>> = _alertSounds.asStateFlow()

    // Add to StockViewModel
    private val _priceHistory = MutableStateFlow<Map<String, List<Double>>>(emptyMap())
    val priceHistory: StateFlow<Map<String, List<Double>>> = _priceHistory.asStateFlow()

    private val _showAlertConfigDialog = MutableStateFlow(false)
    val showAlertConfigDialog: StateFlow<Boolean> = _showAlertConfigDialog.asStateFlow()

    private val _selectedAlertConfig = MutableStateFlow<Triple<Stock, Boolean, Double?>?>(null)
    val selectedAlertConfig: StateFlow<Triple<Stock, Boolean, Double?>?> = _selectedAlertConfig.asStateFlow()

    private val predictionService = PricePredictionService(getApplication())

    private val _predictions = MutableStateFlow<Map<String, PricePredictionService.PredictionResult>>(emptyMap())
    val predictions: StateFlow<Map<String, PricePredictionService.PredictionResult>> = _predictions.asStateFlow()

    private val _useTensorFlow = MutableStateFlow(true)
    val useTensorFlow: StateFlow<Boolean> = _useTensorFlow.asStateFlow()

    private val _marketIndices = MutableStateFlow<MarketIndices?>(null)
    val marketIndices: StateFlow<MarketIndices?> = _marketIndices.asStateFlow()


    fun updatePriceHistory(symbol: String, prices: List<Double>) {
        Log.d("StockViewModel", "Updating price history for $symbol with ${prices.size} data points")
        _priceHistory.value = _priceHistory.value.toMutableMap().apply {
            put(symbol, prices)
        }

        // Debug log the price history after update
        Log.d("StockViewModel", "Price history now contains ${_priceHistory.value.size} stocks")
        _priceHistory.value.forEach { (sym, priceList) ->
            Log.d("StockViewModel", "  $sym: ${priceList.size} prices, first few: ${priceList.take(3)}")
        }
    }

    private val dataUpdateRunnable = object : Runnable {
        override fun run() {
            if (marketHoursService.isMarketOpen()) {
                refreshStocks()
                checkForPatterns()
                checkForNews()
            }

            // Determine next update interval based on market status
            val delayMinutes = when (_marketStatus.value) {
                MarketStatus.OPEN -> 5L  // Update every 5 minutes during market hours
                MarketStatus.PRE_MARKET, MarketStatus.AFTER_HOURS -> 15L  // Less frequent updates outside core hours
                else -> 30L  // Minimal updates when market is closed
            }

            handler.postDelayed(this, TimeUnit.MINUTES.toMillis(delayMinutes))
        }
    }

    init {
        loadInitialStocks()
        checkMarketStatus()
        fetchMarketIndices()

        // Start periodic checks
        handler.post(marketCheckRunnable)
        handler.post(dataUpdateRunnable)
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacks(marketCheckRunnable)
        handler.removeCallbacks(dataUpdateRunnable)
    }

    private fun checkMarketStatus() {
        val newStatus = marketHoursService.getMarketStatus()

        // If status changed, play appropriate sound
        if (_marketStatus.value != newStatus) {
            when (newStatus) {
                MarketStatus.OPEN -> soundPlayer.playMarketOpenSound()
                MarketStatus.CLOSED -> soundPlayer.playMarketCloseSound()
                else -> soundPlayer.playMarketStatusSound(newStatus)
            }

            _marketStatus.value = newStatus
        }
    }

    fun predictStockPrices(symbol: String, useTF: Boolean = true) {
        viewModelScope.launch {
            _useTensorFlow.value = useTF

            val history = _priceHistory.value[symbol] ?: return@launch
            if (history.isEmpty()) return@launch

            val prediction = predictionService.predictPrices(symbol, history, useTF)

            _predictions.value = _predictions.value.toMutableMap().apply {
                put(symbol, prediction)
            }

            // Play sound based on prediction
            playPredictionSound(prediction)
        }
    }

    private fun playPredictionSound(prediction: PricePredictionService.PredictionResult) {
        if (prediction.predictedPrices.isEmpty()) return

        val soundType = when {
            prediction.confidence > 0.7f && prediction.direction == PricePredictionService.PredictionDirection.STRONGLY_UP ||
                    prediction.confidence > 0.7f && prediction.direction == PricePredictionService.PredictionDirection.UP ->
                SoundType.PREDICT_HIGH_UP

            prediction.confidence > 0.7f && prediction.direction == PricePredictionService.PredictionDirection.STRONGLY_DOWN ||
                    prediction.confidence > 0.7f && prediction.direction == PricePredictionService.PredictionDirection.DOWN ->
                SoundType.PREDICT_HIGH_DOWN

            prediction.confidence > 0.4f && prediction.direction == PricePredictionService.PredictionDirection.UP ->
                SoundType.PREDICT_MEDIUM_UP

            prediction.confidence > 0.4f && prediction.direction == PricePredictionService.PredictionDirection.DOWN ->
                SoundType.PREDICT_MEDIUM_DOWN

            else ->
                SoundType.PREDICT_LOW
        }

        soundPlayer.playSound(soundType)
    }

    private fun saveAlertPreferences() {
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("stock_alerts", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        // Clear existing preferences
        editor.clear()

        // Save each stock's alerts
        for (stock in _stocks.value) {
            val symbol = stock.symbol
            val highAlert = stock.alertThresholdHigh
            val lowAlert = stock.alertThresholdLow
            val highSoundType = _alertSounds.value[symbol]?.get(true)?.ordinal ?: 0
            val lowSoundType = _alertSounds.value[symbol]?.get(false)?.ordinal ?: 0

            if (highAlert != null || lowAlert != null) {
                editor.putString(symbol, "${highAlert ?: 0},${lowAlert ?: 0},$highSoundType,$lowSoundType")
            }
        }

        editor.apply()
    }

    private fun loadInitialStocks() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val initialSymbols = listOf("AAPL", "MSFT", "GOOGL")
                val stocksList = mutableListOf<Stock>()

                for (symbol in initialSymbols) {
                    repository.getStockPrice(symbol).onSuccess { stock ->
                        stocksList.add(stock)
                        // Initialize historical prices
                        historicalPrices[symbol] = mutableListOf(stock.currentPrice)
                    }
                }

                _stocks.value = stocksList
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load initial stocks: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshStocks() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentSymbols = _stocks.value.map { it.symbol }
                val updatedStocks = mutableListOf<Stock>()

                for (symbol in currentSymbols) {
                    repository.getStockPrice(symbol).onSuccess { stock ->
                        updatedStocks.add(stock)

                        // Update historical prices
                        historicalPrices[symbol]?.add(stock.currentPrice)

                        // Keep only last 100 prices
                        if (historicalPrices[symbol]?.size ?: 0 > 100) {
                            historicalPrices[symbol] = historicalPrices[symbol]?.takeLast(100)?.toMutableList() ?: mutableListOf()
                        }
                    }
                }

                _stocks.value = updatedStocks
                _errorMessage.value = null

                // Fetch market indices data
                fetchMarketIndices()

            } catch (e: Exception) {
                _errorMessage.value = "Failed to refresh stocks: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun checkForPatterns() {
        for ((symbol, prices) in historicalPrices) {
            if (prices.size < 20) continue

            val stock = _stocks.value.find { it.symbol == symbol } ?: continue

            when {
                PatternDetector.isBreakoutPattern(stock, prices) -> {
                    soundPlayer.playBreakoutSound()
                }
                PatternDetector.isBreakdownPattern(stock, prices) -> {
                    soundPlayer.playBreakdownSound()
                }
                PatternDetector.isBullishTrend(prices) -> {
                    soundPlayer.playBullishSound()
                }
                PatternDetector.isBearishTrend(prices) -> {
                    soundPlayer.playBearishSound()
                }
            }
        }
    }

    private fun checkForNews() {
        viewModelScope.launch {
            for (stock in _stocks.value) {
                newsRepository.getNewsForStock(stock.symbol).onSuccess { response ->
                    // Only process recent news (last 6 hours)
                    val recentNews = response.articles.filter {
                        try {
                            val formatter = org.threeten.bp.format.DateTimeFormatter.ISO_DATE_TIME
                            val newsTime = org.threeten.bp.ZonedDateTime.parse(it.publishedAt, formatter)
                            val hoursAgo = org.threeten.bp.ZonedDateTime.now().minusHours(6)
                            newsTime.isAfter(hoursAgo)
                        } catch (e: Exception) {
                            false
                        }
                    }

                    if (recentNews.isNotEmpty()) {
                        val sentiment = newsRepository.calculateNewsSentiment(recentNews)
                        soundPlayer.playNewsSentimentSound(sentiment)
                    }
                }
            }
        }
    }

    fun fetchMarketIndices() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getMarketIndices().onSuccess { indices ->
                    _marketIndices.value = indices
                    _errorMessage.value = null
                }.onFailure { error ->
                    _errorMessage.value = "Failed to fetch market indices: ${error.localizedMessage}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error fetching market indices: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addStock(symbol: String) {
        // Check if stock already exists
        if (_stocks.value.any { it.symbol.equals(symbol, ignoreCase = true) }) {
            _errorMessage.value = "Stock $symbol is already in your list"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getStockPrice(symbol).onSuccess { newStock ->
                    val currentStocks = _stocks.value.toMutableList()
                    currentStocks.add(newStock)
                    _stocks.value = currentStocks

                    // Initialize historical prices
                    historicalPrices[symbol] = mutableListOf(newStock.currentPrice)

                    _errorMessage.value = null
                }.onFailure { error ->
                    _errorMessage.value = "Failed to add $symbol: ${error.localizedMessage}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error adding stock: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun removeStock(symbol: String) {
        val currentStocks = _stocks.value.toMutableList()
        currentStocks.removeAll { it.symbol.equals(symbol, ignoreCase = true) }
        _stocks.value = currentStocks

        // Clean up historical data
        historicalPrices.remove(symbol)
    }

    fun showAddStockDialog() {
        _showAddStockDialog.value = true
    }

    fun hideAddStockDialog() {
        _showAddStockDialog.value = false
    }

    fun showStockDetailDialog(stock: Stock) {
        _selectedStock.value = stock
    }

    fun hideStockDetailDialog() {
        _selectedStock.value = null
    }

    fun showHighAlertConfig(stock: Stock) {
        _selectedAlertConfig.value = Triple(stock, true, stock.alertThresholdHigh)
        _showAlertConfigDialog.value = true
    }

    fun showLowAlertConfig(stock: Stock) {
        _selectedAlertConfig.value = Triple(stock, false, stock.alertThresholdLow)
        _showAlertConfigDialog.value = false
    }

    fun hideAlertConfigDialog() {
        _showAlertConfigDialog.value = false
        _selectedAlertConfig.value = null
    }

    fun saveAlertConfig(threshold: Double, soundType: SoundType) {
        val config = _selectedAlertConfig.value ?: return
        val stock = config.first
        val isHighAlert = config.second

        // Update the stock's alert threshold
        val updatedStocks = _stocks.value.toMutableList()
        val stockIndex = updatedStocks.indexOfFirst { it.symbol == stock.symbol }

        if (stockIndex >= 0) {
            val updatedStock = updatedStocks[stockIndex].copy()
            if (isHighAlert) {
                updatedStock.alertThresholdHigh = threshold
            } else {
                updatedStock.alertThresholdLow = threshold
            }
            updatedStocks[stockIndex] = updatedStock
            _stocks.value = updatedStocks
        }

        // Save the sound selection
        val currentSoundMap = _alertSounds.value.toMutableMap()
        val stockSoundMap = currentSoundMap[stock.symbol]?.toMutableMap() ?: mutableMapOf()
        stockSoundMap[isHighAlert] = soundType
        currentSoundMap[stock.symbol] = stockSoundMap
        _alertSounds.value = currentSoundMap

        // Hide the dialog
        hideAlertConfigDialog()
    }

    fun clearError() {
        _errorMessage.value = null
    }
}