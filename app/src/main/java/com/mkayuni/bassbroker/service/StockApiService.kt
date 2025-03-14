package com.mkayuni.bassbroker.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface StockApiService {
    @GET("v8/finance/chart/{symbol}")
    suspend fun getStockPrice(
        @Path("symbol") symbol: String,
        @Query("interval") interval: String = "1d",
        @Query("range") range: String = "5d"
    ): StockResponse

    @GET("v8/finance/chart/{symbol}")
    suspend fun getHistoricalData(
        @Path("symbol") symbol: String,
        @Query("interval") interval: String = "1d",
        @Query("range") range: String = "5d"
    ): StockResponse
}

// Response models
data class StockResponse(
    val chart: Chart
)

data class Chart(
    val result: List<Result>? = null,
    val error: YahooError? = null
)

data class YahooError(
    val code: String,
    val description: String
)

data class Result(
    val meta: Meta,
    val timestamp: List<Long>? = null,
    val indicators: Indicators
)

data class Meta(
    val currency: String,
    val symbol: String,
    val exchangeName: String? = null,
    val instrumentType: String? = null,
    val regularMarketPrice: Double,
    val previousClose: Double,
    val gmtoffset: Int? = null,
    val regularMarketTime: Long? = null,
    val timezone: String? = null,
    val exchangeTimezoneName: String? = null
)

data class Indicators(
    val quote: List<Quote>
)

data class Quote(
    val high: List<Double?>? = null,
    val volume: List<Long?>? = null,
    val close: List<Double?>? = null,
    val low: List<Double?>? = null,
    val open: List<Double?>? = null
)