package com.example.smartnewsapp.domain

import com.example.smartnewsapp.data.local.Article
import com.example.smartnewsapp.data.local.InterestProfile

object LocalRecommendationScorer {

    /**
     * Re-ranks a generic list of articles locally based on implicit user interest graph
     * and explicitly selected categories.
     */
    fun sortArticles(
        articles: List<Article>,
        interestProfile: List<InterestProfile>,
        selectedCategories: Set<String>
    ): List<Article> {
        if (articles.isEmpty()) return emptyList()

        // Create a fast lookup map for keyword scores
        val scoreMap = interestProfile.associate { it.keyword.lowercase() to it.score }

        // Local class to hold the computed score for sorting
        data class ScoredArticle(val article: Article, val score: Float)

        return articles.map { article ->
            var score = 0f

            // 1. Explicit Category Match (+10 per matched category)
            // If the article's category matches an explicitly selected category by the user
            if (selectedCategories.any { it.equals(article.category, ignoreCase = true) }) {
                score += 10f
            }

            // 2. Implicit Interest Profile Matches
            // We match the article's category, source name, and metadata tags against the interest graph
            val articleKeywords = mutableListOf<String>()
            articleKeywords.add(article.category.lowercase())
            articleKeywords.add(article.source.name.lowercase())
            article.metadata.tags?.forEach { articleKeywords.add(it.lowercase()) }

            articleKeywords.forEach { kw ->
                val kwScore = scoreMap[kw]
                if (kwScore != null) {
                    score += kwScore
                }
            }

            // 3. Base freshness / importance (if available in metadata)
            // Assume metadata.importance is 1-5
            score += article.metadata.importance.toFloat()

            // Optional: boost newer articles slightly
            
            ScoredArticle(article, score)
        }
        .sortedByDescending { it.score }
        .map { it.article }
    }
}
