package com.mkayuni.bassbroker.api

import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApiService {
    @GET("v2/everything")
    suspend fun getNewsForCompany(
        @Query("q") companySymbol: String,
        @Query("sortBy") sortBy: String = "publishedAt",
        @Query("apiKey") apiKey: String = "YOUR_API_KEY_HERE"
    ): NewsResponse
}