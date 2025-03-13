package com.mkayuni.bassbroker.api

import com.mkayuni.bassbroker.model.Stock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class StockRepository {
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

            val stock = Stock(
                symbol = result.meta.symbol,
                name = companyName,
                currentPrice = result.meta.regularMarketPrice,
                previousClose = result.meta.previousClose
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
            else -> null
        }
    }
}