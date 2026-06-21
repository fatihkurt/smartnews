package com.example.smartnewsapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartnewsapp.domain.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val newsRepository: NewsRepository
) : ViewModel() {

    val articles = newsRepository.getArticles()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Automatically sync on startup
        sync()
    }

    fun sync() {
        viewModelScope.launch {
            newsRepository.syncNews()
        }
    }
}
