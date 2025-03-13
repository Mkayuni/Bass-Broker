package com.mkayuni.bassbroker.api

import com.mkayuni.bassbroker.model.Stock
import retrofit2.http.GET
import retrofit2.http.Query

interface StockApiService {
    @GET("v8/finance/chart/")
    suspend fun getStockPrice(@Query("symbol") symbol: String): StockResponse
}

// Response models
data class StockResponse(
    val chart: Chart
)

data class Chart(
    val result: List<Result>
)

data class Result(
    val meta: Meta,
    val indicators: Indicators
)

data class Meta(
    val regularMarketPrice: Double,
    val previousClose: Double,
    val symbol: String
)

data class Indicators(
    val quote: List<Quote>
)

data class Quote(
    val close: List<Double?>
)