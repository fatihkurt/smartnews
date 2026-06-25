package com.example.smartnewsapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartnewsapp.domain.InterestGraphManager
import com.example.smartnewsapp.domain.LocalRecommendationScorer
import com.example.smartnewsapp.domain.NewsRepository
import com.example.smartnewsapp.domain.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val interestGraphManager: InterestGraphManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val articles = combine(
        newsRepository.getArticles(),
        interestGraphManager.getInterestProfile(),
        settingsRepository.settings
    ) { articles, interestProfile, settings ->
        LocalRecommendationScorer.sortArticles(
            articles = articles,
            interestProfile = interestProfile,
            selectedCategories = settings.selectedCategories
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        // Automatically sync on startup
        sync()
    }

    fun sync() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                newsRepository.syncNews()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun handleArticleUpvote(articleKeywords: List<String>) {
        viewModelScope.launch {
            interestGraphManager.handleArticleUpvote(articleKeywords)
        }
    }

    fun handleArticleDownvote(articleKeywords: List<String>) {
        viewModelScope.launch {
            interestGraphManager.handleArticleDownvote(articleKeywords)
        }
    }
}
