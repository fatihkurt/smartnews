package com.example.smartnewsapp.data.remote.gateway

import android.net.Uri
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
        val settings = settingsRepository.settings.first()
        return hermesApi.fetchLatestNews(
            buildGenericNewsUrl(
                sourceUrl = settings.newsSourceUrl,
                language = settings.selectedLanguage
            )
        )
    }

    override suspend fun chat(messages: List<Message>): ChatResponse {
        val settings = settingsRepository.settings.first()
        val apiKey = settings.hermesApiKey
        val url = settings.hermesChatUrl
        val model = settings.hermesModel
        val authHeader = if (apiKey.isNotBlank()) "Bearer $apiKey" else null
        return hermesApi.chat(url, authHeader, ChatRequest(model = model, messages = messages))
    }

    private fun buildGenericNewsUrl(sourceUrl: String, language: String): String {
        val parsed = Uri.parse(sourceUrl)
        val path = parsed.path.orEmpty()
        val targetPath = if (path.endsWith("/api/news")) {
            path.removeSuffix("/api/news") + "/api/news/recommended"
        } else {
            path
        }
        val builder = parsed.buildUpon()
            .encodedPath(targetPath)
            .clearQuery()

        if (language.isNotBlank()) {
            builder.appendQueryParameter("language", language)
        }

        return builder.build().toString()
    }
}
