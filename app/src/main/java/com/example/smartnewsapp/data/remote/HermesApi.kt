package com.example.smartnewsapp.data.remote

import com.example.smartnewsapp.data.local.Article
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

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
    // Configurable endpoint to fetch gathered news
    @GET
    suspend fun fetchLatestNews(@Url url: String): List<Article>

    // OpenAI-compatible chat completions endpoint
    @POST
    suspend fun chat(
        @Url url: String,
        @Header("Authorization") authorization: String?,
        @Body request: ChatRequest
    ): ChatResponse
}
