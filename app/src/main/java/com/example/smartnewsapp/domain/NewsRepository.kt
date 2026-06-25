package com.example.smartnewsapp.domain

import com.example.smartnewsapp.data.local.Article
import com.example.smartnewsapp.data.local.NewsDao
import com.example.smartnewsapp.domain.gateway.NewsGateway
import com.example.smartnewsapp.data.remote.scraper.ImageScraper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepository @Inject constructor(
    private val newsDao: NewsDao,
    private val newsGateway: NewsGateway,
    private val imageScraper: ImageScraper
) {
    fun getArticles(): Flow<List<Article>> = newsDao.getAllArticles()

    fun getArticle(id: String): Flow<Article> = newsDao.getArticleById(id)

    suspend fun updateArticleFeedback(id: String, feedback: Int) {
        newsDao.updateArticleFeedback(id, feedback)
    }

    suspend fun syncNews() {
        try {
            val remoteArticles = newsGateway.fetchLatestNews()
            newsDao.insertArticles(remoteArticles)
            
            // Background image scraping
            CoroutineScope(Dispatchers.IO).launch {
                val dbArticles = newsDao.getArticlesByIds(remoteArticles.map { it.id })
                fetchMissingImages(dbArticles)
            }
        } catch (e: Exception) {
            // Handle error, maybe log or rethrow
            e.printStackTrace()
        }
    }

    private suspend fun fetchMissingImages(articles: List<Article>) {
        articles.forEach { article ->
            val img = article.media.image
            if (img.url == null || img.type == "none" || img.type == "fallback_category") {
                val scrapedUrl = imageScraper.scrapeOgImage(article.source.url)
                if (scrapedUrl != null) {
                    val updatedMedia = article.media.copy(
                        image = img.copy(url = scrapedUrl, type = "source", isRepresentational = false)
                    )
                    val updatedArticle = article.copy(media = updatedMedia)
                    newsDao.insertArticles(listOf(updatedArticle))
                }
            }
        }
    }
}
