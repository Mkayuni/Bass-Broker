package com.mkayuni.bassbroker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mkayuni.bassbroker.api.StockRepository
import com.mkayuni.bassbroker.model.Stock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StockViewModel : ViewModel() {

    private val repository = StockRepository()

    // State flows for UI updates
    private val _stocks = MutableStateFlow<List<Stock>>(emptyList())
    val stocks: StateFlow<List<Stock>> = _stocks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _showAddStockDialog = MutableStateFlow(false)
    val showAddStockDialog: StateFlow<Boolean> = _showAddStockDialog.asStateFlow()

    private val _selectedStock = MutableStateFlow<Stock?>(null)
    val selectedStock: StateFlow<Stock?> = _selectedStock.asStateFlow()

    // Load some sample stocks on initialization
    init {
        loadInitialStocks()
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
                    }
                }

                _stocks.value = stocksList
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
                    }
                }

                _stocks.value = updatedStocks
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to refresh stocks: ${e.localizedMessage}"
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

    fun clearError() {
        _errorMessage.value = null
    }
}