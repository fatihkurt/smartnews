package com.example.smartnewsapp.domain

import com.example.smartnewsapp.data.local.Article
import com.example.smartnewsapp.data.local.NewsDao
import com.example.smartnewsapp.domain.gateway.NewsGateway
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepository @Inject constructor(
    private val newsDao: NewsDao,
    private val newsGateway: NewsGateway
) {
    fun getArticles(): Flow<List<Article>> = newsDao.getAllArticles()

    fun getArticle(id: String): Flow<Article> = newsDao.getArticleById(id)

    suspend fun syncNews() {
        try {
            val remoteArticles = newsGateway.fetchLatestNews()
            newsDao.insertArticles(remoteArticles)
        } catch (e: Exception) {
            // Handle error, maybe log or rethrow
            e.printStackTrace()
        }
    }
}
