package com.example.smartnewsapp.domain.gateway

import com.example.smartnewsapp.data.remote.ChatResponse
import com.example.smartnewsapp.data.remote.Message

interface ChatGateway {
    suspend fun chat(messages: List<Message>): ChatResponse
}
