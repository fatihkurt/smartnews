package com.example.smartnewsapp.data.remote.gateway

import com.example.smartnewsapp.data.local.Article
import com.example.smartnewsapp.data.remote.ChatRequest
import com.example.smartnewsapp.data.remote.ChatResponse
import com.example.smartnewsapp.data.remote.HermesApi
import com.example.smartnewsapp.data.remote.Message
import com.example.smartnewsapp.domain.SettingsRepository
import com.example.smartnewsapp.domain.gateway.ChatGateway
import com.example.smartnewsapp.domain.gateway.NewsGateway
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HermesGateway @Inject constructor(
    private val hermesApi: HermesApi,
    private val settingsRepository: SettingsRepository
) : NewsGateway, ChatGateway {

    override suspend fun fetchLatestNews(): List<Article> {
        val url = settingsRepository.settings.first().newsSourceUrl
        return hermesApi.fetchLatestNews(url)
    }

    override suspend fun chat(messages: List<Message>): ChatResponse {
        return hermesApi.chat(ChatRequest(messages = messages))
    }
}
