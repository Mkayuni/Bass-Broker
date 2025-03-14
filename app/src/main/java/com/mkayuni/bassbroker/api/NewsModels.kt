// Create as File
package com.mkayuni.bassbroker.api

data class NewsResponse(
    val status: String,
    val totalResults: Int,
    val articles: List<Article>
)

data class Article(
    val source: Source,
    val author: String?,
    val title: String,
    val description: String?,
    val url: String,
    val publishedAt: String,
    val content: String?
)

data class Source(
    val id: String?,
    val name: String
)