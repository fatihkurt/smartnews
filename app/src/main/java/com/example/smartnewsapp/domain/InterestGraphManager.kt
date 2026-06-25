package com.example.smartnewsapp.domain

import com.example.smartnewsapp.data.local.Article
import com.example.smartnewsapp.data.local.InterestProfile
import com.example.smartnewsapp.data.local.ProfileDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
class InterestGraphManager @Inject constructor(
    private val profileDao: ProfileDao
) {
    fun getInterestProfile(): Flow<List<InterestProfile>> = profileDao.getInterestProfile()

    suspend fun recordInteraction(keyword: String, delta: Float) {
        val existingProfile = profileDao.getProfileByKeyword(keyword)
        val nowMillis = System.currentTimeMillis()
        
        val updatedProfile = if (existingProfile != null) {
            val days = ((nowMillis - existingProfile.lastUpdated) / 86400000L).coerceAtLeast(0)
            val decayedScore = existingProfile.score * 0.98f.pow(days.toFloat())
            val newScore = (decayedScore + delta).coerceIn(-10f, 10f)
            existingProfile.copy(
                score = newScore,
                lastUpdated = nowMillis
            )
        } else {
            InterestProfile(
                keyword = keyword,
                score = delta.coerceIn(-10f, 10f),
                lastUpdated = nowMillis
            )
        }

        profileDao.upsertProfile(updatedProfile)
    }

    suspend fun handleArticleLiked(article: Article) {
        recordInteraction(article.category, 1.5f)
        article.metadata.tags?.forEach { recordInteraction(it, 1.0f) }
        article.metadata.entities?.forEach { recordInteraction(it, 0.5f) }
        article.metadata.followTopicId?.let { recordInteraction(it, 2.0f) }
        recordInteraction(article.source.name, 0.25f)
    }

    suspend fun handleArticleDisliked(article: Article) {
        recordInteraction(article.category, -0.75f)
        article.metadata.tags?.forEach { recordInteraction(it, -1.0f) }
        article.metadata.entities?.forEach { recordInteraction(it, -0.5f) }
        article.metadata.followTopicId?.let { recordInteraction(it, -2.0f) }
        recordInteraction(article.source.name, -0.25f)
    }

    suspend fun handleArticleReadTime(article: Article, readTimeSeconds: Long) {
        if (readTimeSeconds > 30) {
            recordInteraction(article.category, 0.5f)
            article.metadata.tags?.forEach { recordInteraction(it, 0.5f) }
        }
    }
}
