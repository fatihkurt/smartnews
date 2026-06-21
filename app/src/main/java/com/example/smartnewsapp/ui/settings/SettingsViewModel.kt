package com.example.smartnewsapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartnewsapp.domain.AppDefaults
import com.example.smartnewsapp.domain.ChatProvider
import com.example.smartnewsapp.domain.SettingsRepository
import com.example.smartnewsapp.domain.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserSettings(
            ChatProvider.MOCK,
            "",
            AppDefaults.OPENROUTER_MODEL,
            AppDefaults.NEWS_SOURCE_URL
        )
    )

    fun updateProvider(provider: ChatProvider) {
        viewModelScope.launch {
            settingsRepository.updateProvider(provider)
        }
    }

    fun updateOpenRouterCredentials(apiKey: String, model: String) {
        viewModelScope.launch {
            settingsRepository.updateOpenRouterCredentials(apiKey, model)
        }
    }

    fun updateNewsSourceUrl(url: String) {
        viewModelScope.launch {
            settingsRepository.updateNewsSourceUrl(url)
        }
    }
}
