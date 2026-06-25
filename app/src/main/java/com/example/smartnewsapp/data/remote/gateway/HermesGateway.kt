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
import java.util.Locale
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
        val parsed = Uri.parse(normalizeAndroidLocalhost(sourceUrl))
        val path = parsed.path.orEmpty()
        val targetPath = if (path.endsWith("/api/news")) {
            path.removeSuffix("/api/news") + "/api/news/recommended"
        } else {
            path
        }
        val builder = parsed.buildUpon()
            .encodedPath(targetPath)
            .clearQuery()

        normalizeLanguage(language)?.let { languageCode ->
            builder.appendQueryParameter("language", languageCode)
        }

        return builder.build().toString()
    }

    private fun normalizeAndroidLocalhost(sourceUrl: String): String {
        val parsed = Uri.parse(sourceUrl)
        val host = parsed.host ?: return sourceUrl
        if (host != "localhost" && host != "127.0.0.1") return sourceUrl

        val authority = buildString {
            append("10.0.2.2")
            if (parsed.port != -1) {
                append(":")
                append(parsed.port)
            }
        }

        return parsed.buildUpon()
            .encodedAuthority(authority)
            .build()
            .toString()
    }

    private fun normalizeLanguage(language: String): String? {
        return when (language.trim().lowercase(Locale.ROOT)) {
            "english", "en", "eng" -> "en"
            "turkish", "tr", "turkce" -> "tr"
            else -> language.trim().takeIf { it.isNotBlank() }
        }
    }
}
