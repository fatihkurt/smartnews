package com.example.smartnewsapp.domain

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ChatProvider {
    MOCK, HERMES, OPENROUTER
}

data class UserSettings(
    val activeProvider: ChatProvider,
    val openRouterApiKey: String,
    val openRouterModel: String,
    val newsSourceUrl: String
)

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val ACTIVE_PROVIDER = stringPreferencesKey("active_provider")
        val OPENROUTER_API_KEY = stringPreferencesKey("openrouter_api_key")
        val OPENROUTER_MODEL = stringPreferencesKey("openrouter_model")
        val NEWS_SOURCE_URL = stringPreferencesKey("news_source_url")
    }

    val settings: Flow<UserSettings> = dataStore.data.map { preferences ->
        val providerStr = preferences[ACTIVE_PROVIDER] ?: ChatProvider.MOCK.name
        val provider = try { ChatProvider.valueOf(providerStr) } catch (e: Exception) { ChatProvider.MOCK }
        
        UserSettings(
            activeProvider = provider,
            openRouterApiKey = preferences[OPENROUTER_API_KEY] ?: "",
            openRouterModel = preferences[OPENROUTER_MODEL] ?: "openai/gpt-4o",
            newsSourceUrl = preferences[NEWS_SOURCE_URL] ?: "https://hermes.example.com/api/news"
        )
    }

    suspend fun updateProvider(provider: ChatProvider) {
        dataStore.edit { preferences ->
            preferences[ACTIVE_PROVIDER] = provider.name
        }
    }

    suspend fun updateOpenRouterCredentials(apiKey: String, model: String) {
        dataStore.edit { preferences ->
            preferences[OPENROUTER_API_KEY] = apiKey
            preferences[OPENROUTER_MODEL] = model
        }
    }

    suspend fun updateNewsSourceUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[NEWS_SOURCE_URL] = url
        }
    }
}
