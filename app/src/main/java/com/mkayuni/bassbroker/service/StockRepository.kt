package com.mkayuni.bassbroker.service

import com.mkayuni.bassbroker.api.StockApiService
import com.mkayuni.bassbroker.model.Stock
import com.mkayuni.bassbroker.viewmodel.StockViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import android.util.Log
import com.mkayuni.bassbroker.model.MarketIndices

class StockRepository(private val viewModel: StockViewModel? = null) {
    private val apiService: StockApiService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://query1.finance.yahoo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(StockApiService::class.java)
    }

    /**
     * Fetches current stock price and previous close
     */
    suspend fun getStockPrice(symbol: String): Result<Stock> = withContext(Dispatchers.IO) {
        try {
            Log.d("StockRepository", "Getting prices for $symbol")

            val response = apiService.getStockPrice(symbol)

            if (response.chart.error != null) {
                return@withContext Result.failure(
                    IOException("API Error: ${response.chart.error.description}")
                )
            }

            val result = response.chart.result?.firstOrNull()
                ?: return@withContext Result.failure(IOException("No data found for $symbol"))

            val companyName = getCompanyName(symbol) ?: symbol
            val currentPrice = result.meta.regularMarketPrice

            // Get accurate previous close from historical data
            val quotes = result.indicators.quote.firstOrNull()
            val closePrices = quotes?.close?.filterNotNull()

            // Update price history in ViewModel if available
            if (!closePrices.isNullOrEmpty()) {
                viewModel?.updatePriceHistory(symbol, closePrices)
            } else {
                Log.w("StockRepository", "No price history available for $symbol")
            }

            // Use historical data for previous close if available
            val previousClosePrice = if (!closePrices.isNullOrEmpty() && closePrices.size > 1) {
                closePrices[closePrices.size - 2]
            } else if (result.meta.previousClose > 0.001) {
                result.meta.previousClose
            } else {
                result.meta.regularMarketPrice * 0.99
            }

            val stock = Stock(
                symbol = result.meta.symbol,
                name = companyName,
                currentPrice = currentPrice,
                previousClose = previousClosePrice
            )

            Result.success(stock)
        } catch (e: Exception) {
            Log.e("StockRepository", "Error getting stock price for $symbol", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches current market indices data (S&P 500, NASDAQ, VIX)
     */
    suspend fun getMarketIndices(): Result<MarketIndices> = withContext(Dispatchers.IO) {
        try {
            Log.d("StockRepository", "Getting market indices")

            // Fetch S&P 500 data
            val sp500Result = getStockPrice("^GSPC").getOrNull()

            // Fetch NASDAQ data
            val nasdaqResult = getStockPrice("^IXIC").getOrNull()

            // Fetch VIX data
            val vixResult = getStockPrice("^VIX").getOrNull()

            // Create market indices object
            val indices = MarketIndices(
                sp500Price = sp500Result?.currentPrice ?: 0.0,
                sp500Change = sp500Result?.let {
                    ((it.currentPrice - it.previousClose) / it.previousClose) * 100
                } ?: 0.0,
                nasdaqPrice = nasdaqResult?.currentPrice ?: 0.0,
                nasdaqChange = nasdaqResult?.let {
                    ((it.currentPrice - it.previousClose) / it.previousClose) * 100
                } ?: 0.0,
                vixPrice = vixResult?.currentPrice ?: 0.0,
                vixChange = vixResult?.let {
                    ((it.currentPrice - it.previousClose) / it.previousClose) * 100
                } ?: 0.0
            )

            Result.success(indices)
        } catch (e: Exception) {
            Log.e("StockRepository", "Error getting market indices", e)
            Result.failure(e)
        }
    }


    /**
     * Fetches at least 60 days of historical data for TensorFlow predictions
     */
    suspend fun getHistoricalDataForPrediction(symbol: String): Result<List<Double>> = withContext(Dispatchers.IO) {
        try {
            Log.d("StockRepository", "Fetching historical data for $symbol")

            // Fetch historical data with a 4-month range to ensure at least 60 days
            val response = apiService.getHistoricalData(
                symbol = symbol,
                interval = "1d", // Daily interval
                range = "4mo"    // 4 months of data
            )

            if (response.chart.error != null) {
                return@withContext Result.failure(
                    IOException("API Error: ${response.chart.error.description}")
                )
            }

            val result = response.chart.result?.firstOrNull()
                ?: return@withContext Result.failure(IOException("No data found for $symbol"))

            val quotes = result.indicators.quote.firstOrNull()
            val closePrices = quotes?.close?.filterNotNull()

            // Validate that we have at least 60 days of data
            if (closePrices == null || closePrices.size < 60) {
                return@withContext Result.failure(
                    IOException("Insufficient historical data: ${closePrices?.size} days found, but 60 are required")
                )
            }

            // Return exactly 60 days of data
            Log.d("StockRepository", "Successfully fetched ${closePrices.size} days of data for $symbol")
            return@withContext Result.success(closePrices.takeLast(60))
        } catch (e: Exception) {
            Log.e("StockRepository", "Error fetching historical data for $symbol", e)
            Result.failure(e)
        }
    }

    /**
     * Returns the company name for a given stock symbol
     */
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
            "SOUN" -> "SoundHound AI, Inc."
            "APLD" -> "Applied Digital Corporation"
            "SERV" -> "Serve Robotics, Inc."
            "SWTX" -> "SpringWorks Therapeutics, Inc."
            "SOFI" -> "SoFi Technologies, Inc."
            "AMPS" -> "Altus Power, Inc."
            "PAYS" -> "Paysign, Inc."
            else -> null
        }
    }
}