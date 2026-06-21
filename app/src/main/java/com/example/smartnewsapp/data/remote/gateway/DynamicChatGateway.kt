package com.example.smartnewsapp.data.remote.gateway

import com.example.smartnewsapp.data.remote.ChatResponse
import com.example.smartnewsapp.data.remote.Message
import com.example.smartnewsapp.domain.ChatProvider
import com.example.smartnewsapp.domain.SettingsRepository
import com.example.smartnewsapp.domain.gateway.ChatGateway
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicChatGateway @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val mockGateway: MockGateway,
    private val hermesGateway: HermesGateway,
    private val openRouterGateway: OpenRouterGateway
) : ChatGateway {
    
    override suspend fun chat(messages: List<Message>): ChatResponse {
        val activeProvider = settingsRepository.settings.first().activeProvider
        
        return when (activeProvider) {
            ChatProvider.MOCK -> mockGateway.chat(messages)
            ChatProvider.HERMES -> hermesGateway.chat(messages)
            ChatProvider.OPENROUTER -> openRouterGateway.chat(messages)
        }
    }
}
