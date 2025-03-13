package com.mkayuni.bassbroker.api

import com.mkayuni.bassbroker.model.Stock
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class StockRepository {
    private val apiService: StockApiService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://query1.finance.yahoo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(StockApiService::class.java)
    }

    suspend fun getStockPrice(symbol: String): Stock {
        try {
            val response = apiService.getStockPrice(symbol)
            val result = response.chart.result[0]

            return Stock(
                symbol = result.meta.symbol,
                name = result.meta.symbol, // API doesn't provide name, using symbol
                currentPrice = result.meta.regularMarketPrice,
                previousClose = result.meta.previousClose
            )
        } catch (e: Exception) {
            // In a real app, you'd handle this better
            throw e
        }
    }
}