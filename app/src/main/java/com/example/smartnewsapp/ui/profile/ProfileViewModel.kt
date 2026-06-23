package com.example.smartnewsapp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartnewsapp.data.local.InterestProfile
import com.example.smartnewsapp.domain.InterestGraphManager
import com.example.smartnewsapp.domain.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    interestGraphManager: InterestGraphManager
) : ViewModel() {

    val interestProfile = interestGraphManager.getInterestProfile()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val settings = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateLanguage(language: String) {
        viewModelScope.launch {
            settingsRepository.updateSelectedLanguage(language)
        }
    }

    fun updateCategories(categories: Set<String>) {
        viewModelScope.launch {
            settingsRepository.updateSelectedCategories(categories)
        }
    }
}
