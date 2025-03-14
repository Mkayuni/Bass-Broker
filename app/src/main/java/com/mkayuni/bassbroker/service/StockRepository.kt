package com.mkayuni.bassbroker.service

import com.mkayuni.bassbroker.api.StockApiService
import com.mkayuni.bassbroker.model.Stock
import com.mkayuni.bassbroker.viewmodel.StockViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class StockRepository(private val viewModel: StockViewModel? = null) {
    private val apiService: StockApiService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://query1.finance.yahoo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(StockApiService::class.java)
    }

    suspend fun getStockPrice(symbol: String): kotlin.Result<Stock> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getStockPrice(symbol)

            if (response.chart.error != null) {
                return@withContext kotlin.Result.failure(
                    IOException("API Error: ${response.chart.error.description}")
                )
            }

            val result = response.chart.result?.firstOrNull()
                ?: return@withContext kotlin.Result.failure(IOException("No data found for $symbol"))

            val companyName = getCompanyName(symbol) ?: symbol
            val currentPrice = result.meta.regularMarketPrice

            // Get accurate previous close from historical data
            val quotes = result.indicators.quote.firstOrNull()
            val closePrices = quotes?.close?.filterNotNull()

            // Update price history in ViewModel if available
            if (!closePrices.isNullOrEmpty()) {
                viewModel?.updatePriceHistory(symbol, closePrices)
            }

            // Use historical data for previous close if available
            val previousClosePrice = if (!closePrices.isNullOrEmpty() && closePrices.size > 1) {
                // Use the second-to-last close price as previous close
                closePrices[closePrices.size - 2]
            } else if (result.meta.previousClose > 0.001) {
                // Fall back to meta.previousClose if it seems valid
                result.meta.previousClose
            } else {
                // Last resort fallback if all else fails
                result.meta.regularMarketPrice * 0.99
            }

            val stock = Stock(
                symbol = result.meta.symbol,
                name = companyName,
                currentPrice = currentPrice,
                previousClose = previousClosePrice
            )

            kotlin.Result.success(stock)
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }

    // This would typically come from a separate API call, but we're simplifying
    private fun getCompanyName(symbol: String): String? {
        return when (symbol) {
            "AAPL" -> "Apple Inc."
            "MSFT" -> "Microsoft Corp."
            "GOOGL" -> "Alphabet Inc."
            "AMZN" -> "Amazon.com Inc."
            "META" -> "Meta Platforms Inc."
            "TSLA" -> "Tesla Inc."
            "NVDA" -> "NVIDIA Corp."
            "JPM" -> "JPMorgan Chase & Co."
            "NFLX" -> "Netflix Inc."
            "DIS" -> "Walt Disney Co."
            "SMCI" -> "Super Micro Computer, Inc."
            else -> null
        }
    }
}