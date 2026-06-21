package com.example.smartnewsapp.data.remote.gateway

import com.example.smartnewsapp.data.local.Article
import com.example.smartnewsapp.domain.NewsProvider
import com.example.smartnewsapp.domain.SettingsRepository
import com.example.smartnewsapp.domain.gateway.NewsGateway
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicNewsGateway @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val mockGateway: MockGateway,
    private val hermesGateway: HermesGateway
) : NewsGateway {

    override suspend fun fetchLatestNews(): List<Article> {
        val provider = settingsRepository.settings.first().activeNewsProvider
        
        return when (provider) {
            NewsProvider.MOCK -> mockGateway.fetchLatestNews()
            NewsProvider.HERMES -> hermesGateway.fetchLatestNews()
        }
    }
}
