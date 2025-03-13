package com.mkayuni.bassbroker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mkayuni.bassbroker.api.StockRepository
import com.mkayuni.bassbroker.model.Stock
import com.mkayuni.bassbroker.model.StockAlert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class StockViewModel : ViewModel() {

    private val repository = StockRepository()

    // State flows for UI updates
    private val _stocks = MutableStateFlow<List<Stock>>(emptyList())
    val stocks: StateFlow<List<Stock>> = _stocks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showAddStockDialog = MutableStateFlow(false)
    val showAddStockDialog: StateFlow<Boolean> = _showAddStockDialog.asStateFlow()

    private val _selectedStock = MutableStateFlow<Stock?>(null)
    val selectedStock: StateFlow<Stock?> = _selectedStock.asStateFlow()

    // Sample data for now - replace with actual API calls later
    init {
        // Load some sample stocks
        val sampleStocks = listOf(
            Stock("AAPL", "Apple Inc.", 190.68, 187.25),
            Stock("MSFT", "Microsoft Corp.", 378.92, 375.54),
            Stock("GOOGL", "Alphabet Inc.", 142.15, 140.23)
        )
        _stocks.value = sampleStocks
    }

    fun refreshStocks() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // For each stock in our list, refresh its data
                val updatedStocks = _stocks.value.map { stock ->
                    repository.getStockPrice(stock.symbol)
                }
                _stocks.value = updatedStocks
            } catch (e: Exception) {
                // In a real app, you'd handle errors properly
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addStock(symbol: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // In a production app, this would fetch from API
                // For now, create a dummy stock with the symbol
                val newStock = Stock(
                    symbol = symbol,
                    name = "$symbol Corp.", // Placeholder name
                    currentPrice = 100.0 + Random.nextDouble() * 50,
                    previousClose = 100.0 + Random.nextDouble() * 50
                )

                // Add to the existing list
                val currentStocks = _stocks.value.toMutableList()
                currentStocks.add(newStock)
                _stocks.value = currentStocks
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun removeStock(symbol: String) {
        val currentStocks = _stocks.value.toMutableList()
        currentStocks.removeAll { it.symbol == symbol }
        _stocks.value = currentStocks
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
}