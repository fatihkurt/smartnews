package com.example.smartnewsapp.domain

import com.example.smartnewsapp.data.local.Article
import com.example.smartnewsapp.data.local.Content
import com.example.smartnewsapp.data.local.ImageInfo
import com.example.smartnewsapp.data.local.InterestProfile
import com.example.smartnewsapp.data.local.Media
import com.example.smartnewsapp.data.local.Metadata
import com.example.smartnewsapp.data.local.Source
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalRecommendationScorerTest {

    @Test
    fun selectedWorldNewsMatchesWorldCategory() {
        val world = article(id = "world", category = "world")
        val business = article(id = "business", category = "business")

        val sorted = LocalRecommendationScorer.sortArticles(
            articles = listOf(business, world),
            interestProfile = emptyList(),
            selectedCategories = setOf("World News")
        )

        assertEquals(listOf("world", "business"), sorted.map { it.id })
    }

    @Test
    fun selectedArtificialIntelligenceMatchesAiTags() {
        val ai = article(id = "ai", category = "technology", tags = listOf("AI"))
        val finance = article(id = "finance", category = "finance")

        val sorted = LocalRecommendationScorer.sortArticles(
            articles = listOf(finance, ai),
            interestProfile = emptyList(),
            selectedCategories = setOf("Artificial Intelligence")
        )

        assertEquals(listOf("ai", "finance"), sorted.map { it.id })
    }

    @Test
    fun interestKeywordsAreNormalizedBeforeMatching() {
        val ai = article(id = "ai", category = "technology", tags = listOf("ai"))
        val sports = article(id = "sports", category = "sports")

        val sorted = LocalRecommendationScorer.sortArticles(
            articles = listOf(sports, ai),
            interestProfile = listOf(
                InterestProfile(keyword = " Artificial Intelligence ", score = 4f, lastUpdated = 1L)
            ),
            selectedCategories = emptySet()
        )

        assertEquals(listOf("ai", "sports"), sorted.map { it.id })
    }

    @Test
    fun aliasesUseCanonicalInterestKeywords() {
        assertEquals("artificial intelligence", LocalRecommendationScorer.canonicalKeyword("AI"))
        assertEquals("world news", LocalRecommendationScorer.canonicalKeyword("world"))
    }

    @Test
    fun neutralProfilePreservesIncomingFeedOrder() {
        val first = article(id = "first", category = "business", importance = 1)
        val second = article(id = "second", category = "science", importance = 5)

        val sorted = LocalRecommendationScorer.sortArticles(
            articles = listOf(first, second),
            interestProfile = emptyList(),
            selectedCategories = emptySet()
        )

        assertEquals(listOf("first", "second"), sorted.map { it.id })
    }

    private fun article(
        id: String,
        category: String,
        tags: List<String> = listOf(category),
        importance: Int = 3
    ): Article {
        return Article(
            id = id,
            title = "$category headline",
            category = category,
            source = Source(name = "Source $id", url = "https://example.com/$id"),
            publishedAt = "2026-06-25T00:00:00Z",
            language = "en",
            content = Content(
                summary = "$category summary",
                expanded = "$category expanded",
                whyItMatters = "$category matters"
            ),
            media = Media(image = ImageInfo(url = null, type = "none")),
            metadata = Metadata(
                importance = importance,
                tags = tags,
                entities = emptyList(),
                followTopicId = null,
                isBreaking = false,
                confidence = 1f
            )
        )
    }
}
