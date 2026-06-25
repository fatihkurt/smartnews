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

enum class NewsProvider {
    MOCK, HERMES
}

data class UserSettings(
    val activeProvider: ChatProvider,
    val activeNewsProvider: NewsProvider,
    val openRouterApiKey: String,
    val openRouterModel: String,
    val hermesApiKey: String,
    val hermesChatUrl: String,
    val hermesModel: String,
    val newsSourceUrl: String,
    val selectedLanguage: String,
    val selectedCategories: Set<String>
)

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val ACTIVE_PROVIDER = stringPreferencesKey("active_provider")
        val ACTIVE_NEWS_PROVIDER = stringPreferencesKey("active_news_provider")
        val OPENROUTER_API_KEY = stringPreferencesKey("openrouter_api_key")
        val OPENROUTER_MODEL = stringPreferencesKey("openrouter_model")
        val HERMES_API_KEY = stringPreferencesKey("hermes_api_key")
        val HERMES_CHAT_URL = stringPreferencesKey("hermes_chat_url")
        val HERMES_MODEL = stringPreferencesKey("hermes_model")
        val NEWS_SOURCE_URL = stringPreferencesKey("news_source_url")
        val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
        val SELECTED_CATEGORIES = androidx.datastore.preferences.core.stringSetPreferencesKey("selected_categories")
    }

    val settings: Flow<UserSettings> = dataStore.data.map { preferences ->
        val providerStr = preferences[ACTIVE_PROVIDER] ?: ChatProvider.MOCK.name
        val provider = try { ChatProvider.valueOf(providerStr) } catch (e: Exception) { ChatProvider.MOCK }
        
        val newsProviderStr = preferences[ACTIVE_NEWS_PROVIDER] ?: NewsProvider.HERMES.name
        val newsProvider = try { NewsProvider.valueOf(newsProviderStr) } catch (e: Exception) { NewsProvider.HERMES }
        
        UserSettings(
            activeProvider = provider,
            activeNewsProvider = newsProvider,
            openRouterApiKey = preferences[OPENROUTER_API_KEY] ?: "",
            openRouterModel = preferences[OPENROUTER_MODEL] ?: "openai/gpt-4o",
            hermesApiKey = preferences[HERMES_API_KEY] ?: "",
            hermesChatUrl = preferences[HERMES_CHAT_URL] ?: "https://hermes.vectororch.com/v1/chat/completions",
            hermesModel = preferences[HERMES_MODEL] ?: "hermes-agent",
            newsSourceUrl = preferences[NEWS_SOURCE_URL] ?: "https://hermes.vectororch.com/api/news",
            selectedLanguage = preferences[SELECTED_LANGUAGE] ?: "English",
            selectedCategories = preferences[SELECTED_CATEGORIES] ?: emptySet()
        )
    }

    suspend fun updateProvider(provider: ChatProvider) {
        dataStore.edit { preferences ->
            preferences[ACTIVE_PROVIDER] = provider.name
        }
    }

    suspend fun updateNewsProvider(provider: NewsProvider) {
        dataStore.edit { preferences ->
            preferences[ACTIVE_NEWS_PROVIDER] = provider.name
        }
    }

    suspend fun updateOpenRouterCredentials(apiKey: String, model: String) {
        dataStore.edit { preferences ->
            preferences[OPENROUTER_API_KEY] = apiKey
            preferences[OPENROUTER_MODEL] = model
        }
    }

    suspend fun updateHermesApiKey(apiKey: String) {
        dataStore.edit { preferences ->
            preferences[HERMES_API_KEY] = apiKey
        }
    }

    suspend fun updateHermesChatUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[HERMES_CHAT_URL] = url
        }
    }

    suspend fun updateHermesModel(model: String) {
        dataStore.edit { preferences ->
            preferences[HERMES_MODEL] = model
        }
    }

    suspend fun updateNewsSourceUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[NEWS_SOURCE_URL] = url
        }
    }

    suspend fun updateSelectedLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[SELECTED_LANGUAGE] = language
        }
    }

    suspend fun updateSelectedCategories(categories: Set<String>) {
        dataStore.edit { preferences ->
            preferences[SELECTED_CATEGORIES] = categories
        }
    }
}
