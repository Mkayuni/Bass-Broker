// Create as Class
package com.mkayuni.bassbroker.service

import com.mkayuni.bassbroker.api.Article
import com.mkayuni.bassbroker.api.NewsApiService
import com.mkayuni.bassbroker.api.NewsResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NewsRepository {
    private val newsApiService: NewsApiService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://newsapi.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        newsApiService = retrofit.create(NewsApiService::class.java)
    }

    suspend fun getNewsForStock(symbol: String): kotlin.Result<NewsResponse> {
        return try {
            val response = newsApiService.getNewsForCompany(symbol)
            kotlin.Result.success(response)
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }

    fun calculateNewsSentiment(articles: List<Article>): NewsSentiment {
        // Simple algorithm - count keywords in titles
        var positiveCount = 0
        var negativeCount = 0

        val positiveWords = listOf("rise", "up", "gain", "positive", "growth", "profit", "beat", "surge")
        val negativeWords = listOf("fall", "down", "drop", "negative", "loss", "miss", "plunge", "cut")

        articles.forEach { article ->
            val title = article.title.lowercase()
            positiveWords.forEach { if (title.contains(it)) positiveCount++ }
            negativeWords.forEach { if (title.contains(it)) negativeCount++ }
        }

        return when {
            positiveCount - negativeCount > 5 -> NewsSentiment.VERY_POSITIVE
            positiveCount - negativeCount > 2 -> NewsSentiment.POSITIVE
            negativeCount - positiveCount > 5 -> NewsSentiment.VERY_NEGATIVE
            negativeCount - positiveCount > 2 -> NewsSentiment.NEGATIVE
            else -> NewsSentiment.NEUTRAL
        }
    }
}

enum class NewsSentiment {
    VERY_POSITIVE,
    POSITIVE,
    NEUTRAL,
    NEGATIVE,
    VERY_NEGATIVE
}