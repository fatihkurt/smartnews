package com.example.smartnewsapp.domain

import com.example.smartnewsapp.data.local.Article
import com.example.smartnewsapp.data.local.InterestProfile
import java.util.Locale

object LocalRecommendationScorer {

    private const val SELECTED_CATEGORY_BOOST = 10f

    private val whitespaceRegex = Regex("\\s+")
    private val nonWordRegex = Regex("[^\\p{L}\\p{Nd}]+")

    private val aliases = mapOf(
        "artificial intelligence" to setOf("ai", "machine learning", "ml"),
        "world news" to setOf("world", "international", "global"),
        "local news" to setOf("local"),
        "climate change" to setOf("climate", "environment"),
        "cryptocurrency" to setOf("crypto", "bitcoin", "ethereum"),
        "e sports" to setOf("esports"),
        "art and culture" to setOf("art", "arts", "culture"),
        "movies and tv" to setOf("movies", "tv", "television", "film"),
        "food and dining" to setOf("food", "dining"),
        "books and literature" to setOf("books", "literature"),
        "law and crime" to setOf("law", "crime", "legal"),
        "space exploration" to setOf("space")
    )

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

        val interestScores = interestProfile
            .mapNotNull { profile ->
                canonicalKeyword(profile.keyword).takeIf { it.isNotBlank() }?.let { keyword ->
                    keyword to profile.score
                }
            }
            .groupingBy { it.first }
            .fold(0f) { total, entry -> total + entry.second }

        data class ScoredArticle(val article: Article, val score: Float, val index: Int)

        return articles.mapIndexed { index, article ->
            ScoredArticle(
                article = article,
                score = scoreArticle(
                    article = article,
                    selectedCategories = selectedCategories,
                    interestScores = interestScores
                ),
                index = index
            )
        }
        .sortedWith(
            compareByDescending<ScoredArticle> { it.score }
                .thenBy { it.index }
        )
        .map { it.article }
    }

    internal fun scoreArticle(
        article: Article,
        selectedCategories: Set<String>,
        interestScores: Map<String, Float>
    ): Float {
        val articleTerms = extractArticleTerms(article)
        val articleText = normalizedArticleText(article)
        val articleTokens = articleText.split(" ").filterTo(mutableSetOf()) { it.isNotBlank() }

        var score = 0f

        selectedCategories.forEach { category ->
            if (matchesAny(expandTerm(category), articleTerms, articleText, articleTokens)) {
                score += SELECTED_CATEGORY_BOOST
            }
        }

        interestScores.forEach { (keyword, interestScore) ->
            if (matchesAny(expandTerm(keyword), articleTerms, articleText, articleTokens)) {
                score += interestScore
            }
        }

        return score
    }

    fun normalizeKeyword(value: String): String {
        return value
            .trim()
            .lowercase(Locale.ROOT)
            .replace("&", " and ")
            .replace(nonWordRegex, " ")
            .replace(whitespaceRegex, " ")
            .trim()
    }

    fun canonicalKeyword(value: String): String {
        val normalized = normalizeKeyword(value)
        if (normalized.isBlank()) return ""

        return aliases.entries
            .firstOrNull { (canonical, aliasSet) ->
                normalized == canonical || aliasSet.contains(normalized)
            }
            ?.key
            ?: normalized
    }

    private fun extractArticleTerms(article: Article): Set<String> {
        val rawTerms = buildList {
            add(article.category)
            add(article.source.name)
            article.source.domain?.let { add(it) }
            addAll(article.metadata.tags)
            article.metadata.entities?.let { addAll(it) }
            article.metadata.followTopicId?.let { add(it) }
            article.content.terms?.forEach { add(it.term) }
        }

        return rawTerms
            .flatMap { expandTerm(it) }
            .filterTo(mutableSetOf()) { it.isNotBlank() }
    }

    private fun normalizedArticleText(article: Article): String {
        val text = buildList {
            add(article.title)
            add(article.content.summary)
            add(article.content.expanded)
            add(article.content.whyItMatters)
            article.content.background?.let { add(it) }
            article.content.keyPoints?.let { addAll(it) }
            article.content.terms?.forEach {
                add(it.term)
                add(it.explanation)
            }
        }.joinToString(" ")

        return normalizeKeyword(text)
    }

    private fun expandTerm(value: String): Set<String> {
        val normalized = normalizeKeyword(value)
        if (normalized.isBlank()) return emptySet()

        val directAliases = aliases[normalized].orEmpty()
        val reverseAliases = aliases
            .filterValues { normalized in it }
            .keys

        return buildSet {
            add(normalized)
            addAll(directAliases)
            addAll(reverseAliases)
        }
    }

    private fun matchesAny(
        candidates: Set<String>,
        articleTerms: Set<String>,
        articleText: String,
        articleTokens: Set<String>
    ): Boolean {
        return candidates.any { candidate ->
            candidate in articleTerms || phraseMatches(candidate, articleText, articleTokens)
        }
    }

    private fun phraseMatches(
        candidate: String,
        articleText: String,
        articleTokens: Set<String>
    ): Boolean {
        if (candidate.isBlank()) return false
        return if (candidate.contains(" ")) {
            " $articleText ".contains(" $candidate ")
        } else {
            candidate in articleTokens
        }
    }
}
