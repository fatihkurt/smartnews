package com.example.smartnewsapp.data.remote.gateway

import com.example.smartnewsapp.data.remote.ChatRequest
import com.example.smartnewsapp.data.remote.ChatResponse
import com.example.smartnewsapp.data.remote.Message
import com.example.smartnewsapp.data.remote.OpenRouterApi
import com.example.smartnewsapp.domain.SettingsRepository
import com.example.smartnewsapp.domain.gateway.ChatGateway
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenRouterGateway @Inject constructor(
    private val api: OpenRouterApi,
    private val settingsRepository: SettingsRepository
) : ChatGateway {
    
    override suspend fun chat(messages: List<Message>): ChatResponse {
        val settings = settingsRepository.settings.first()
        val authHeader = "Bearer ${settings.openRouterApiKey}"
        val request = ChatRequest(
            model = settings.openRouterModel,
            messages = messages
        )
        return api.chat(authorization = authHeader, request = request)
    }
}
