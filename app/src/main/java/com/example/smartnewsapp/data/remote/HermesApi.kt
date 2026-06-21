package com.example.smartnewsapp.data.remote

import com.example.smartnewsapp.data.local.Article
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

@Serializable
data class ChatRequest(
    val model: String = "hermes",
    val messages: List<Message>
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

@Serializable
data class ChatResponse(
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val message: Message
)

interface HermesApi {
    // Assuming the Hermes server exposes an endpoint to fetch gathered news
    @GET("api/news")
    suspend fun fetchLatestNews(): List<Article>

    // OpenAI-compatible chat completions endpoint
    @POST("v1/chat/completions")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
}
