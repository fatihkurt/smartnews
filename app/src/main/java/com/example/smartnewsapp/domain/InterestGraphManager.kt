package com.example.smartnewsapp.domain

import com.example.smartnewsapp.data.local.InterestProfile
import com.example.smartnewsapp.data.local.ProfileDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InterestGraphManager @Inject constructor(
    private val profileDao: ProfileDao
) {
    fun getInterestProfile(): Flow<List<InterestProfile>> = profileDao.getInterestProfile()

    suspend fun recordInteraction(keyword: String, positiveFeedback: Boolean, weight: Float = 1.0f) {
        val existingProfile = profileDao.getProfileByKeyword(keyword)
        val scoreAdjustment = if (positiveFeedback) weight else -weight
        
        val updatedProfile = if (existingProfile != null) {
            existingProfile.copy(
                score = existingProfile.score + scoreAdjustment,
                lastUpdated = System.currentTimeMillis()
            )
        } else {
            InterestProfile(
                keyword = keyword,
                score = scoreAdjustment,
                lastUpdated = System.currentTimeMillis()
            )
        }

        profileDao.insertProfile(updatedProfile)
    }

    /**
     * Call this when a user explicitly upvotes an article
     */
    suspend fun handleArticleUpvote(articleKeywords: List<String>) {
        articleKeywords.forEach { keyword ->
            recordInteraction(keyword, positiveFeedback = true, weight = 2.0f)
        }
    }

    /**
     * Call this when a user explicitly downvotes or dismisses an article
     */
    suspend fun handleArticleDownvote(articleKeywords: List<String>) {
        articleKeywords.forEach { keyword ->
            recordInteraction(keyword, positiveFeedback = false, weight = 2.0f)
        }
    }

    /**
     * Call this based on implicit reading time
     */
    suspend fun handleArticleReadTime(articleKeywords: List<String>, readTimeSeconds: Long) {
        if (readTimeSeconds > 30) {
            // Read for more than 30 seconds -> mild positive interest
            articleKeywords.forEach { keyword ->
                recordInteraction(keyword, positiveFeedback = true, weight = 0.5f)
            }
        }
    }
}
