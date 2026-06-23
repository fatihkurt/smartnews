package com.example.smartnewsapp.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartnewsapp.data.local.ChatDao
import com.example.smartnewsapp.data.local.ChatMessage
import com.example.smartnewsapp.data.remote.Message
import com.example.smartnewsapp.domain.gateway.ChatGateway
import com.example.smartnewsapp.domain.NewsRepository
import com.example.smartnewsapp.domain.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatDao: ChatDao,
    private val chatGateway: ChatGateway,
    private val newsRepository: NewsRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val articleIdFlow = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val article = articleIdFlow.filterNotNull().flatMapLatest { id ->
        newsRepository.getArticle(id)
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages = articleIdFlow.filterNotNull().flatMapLatest { id ->
        chatDao.getMessagesForArticle(id)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun initArticle(id: String) {
        articleIdFlow.value = id
    }

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping

    fun sendMessage(text: String) {
        val currentId = articleIdFlow.value ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            // 1. Save user message locally
            val userMsg = ChatMessage(
                articleId = currentId,
                isUser = true,
                message = text,
                timestamp = System.currentTimeMillis()
            )
            chatDao.insertMessage(userMsg)

            _isTyping.value = true

            try {
                // 2. Prepare context for Hermes
                val currentArticle = article.value
                val systemPrompt = "You are an AI assistant helping the user understand the following article:\n\nTitle: ${currentArticle?.title}\nContent: ${currentArticle?.content}"
                
                val apiMessages = mutableListOf(Message(role = "system", content = systemPrompt))
                
                // Add previous chat history
                messages.value.takeLast(10).forEach { msg ->
                    apiMessages.add(Message(
                        role = if (msg.isUser) "user" else "assistant",
                        content = msg.message
                    ))
                }
                
                // Add the new user message
                apiMessages.add(Message(role = "user", content = text))

                // 3. Get active provider for metadata
                val activeProvider = settingsRepository.settings.first().activeProvider.name
                
                // 4. Send request to Gateway
                val response = chatGateway.chat(apiMessages)
                
                // 5. Save Hermes response locally
                val hermesMsgText = response.choices.firstOrNull()?.message?.content ?: "I'm sorry, I couldn't process that."
                
                val hermesMsg = ChatMessage(
                    articleId = currentId,
                    isUser = false,
                    message = hermesMsgText,
                    provider = activeProvider,
                    timestamp = System.currentTimeMillis()
                )
                chatDao.insertMessage(hermesMsg)

            } catch (e: Exception) {
                e.printStackTrace()
                // Save error message
                chatDao.insertMessage(ChatMessage(
                    articleId = currentId,
                    isUser = false,
                    message = "Error connecting to Chat Provider: ${e.message}",
                    provider = "SYSTEM",
                    timestamp = System.currentTimeMillis()
                ))
            } finally {
                _isTyping.value = false
            }
        }
    }
}
