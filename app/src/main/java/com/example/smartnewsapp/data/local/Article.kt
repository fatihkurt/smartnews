package com.example.smartnewsapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "articles")
data class Article(
    @PrimaryKey
    val id: String,
    val title: String,
    val category: String,
    val source: Source,
    @SerialName("published_at") val publishedAt: String,
    @SerialName("updated_at") val updatedAt: String? = null,
    val language: String,
    val content: Content,
    val media: Media,
    val metadata: Metadata,
    @SerialName("assistant_prompts") val assistantPrompts: AssistantPrompts? = null,
    
    // Local fields
    val isRead: Boolean = false,
    val isSaved: Boolean = false
)

@Serializable
data class Source(
    val name: String,
    val url: String,
    val domain: String? = null
)

@Serializable
data class Content(
    val summary: String,
    val expanded: String,
    @SerialName("why_it_matters") val whyItMatters: String,
    val background: String? = null,
    @SerialName("key_points") val keyPoints: List<String>? = null,
    val terms: List<Term>? = null
)

@Serializable
data class Term(
    val term: String,
    val explanation: String
)

@Serializable
data class Media(
    val image: ImageInfo
)

@Serializable
data class ImageInfo(
    val url: String?,
    val type: String,
    val alt: String? = null,
    val credit: String? = null,
    @SerialName("is_representational") val isRepresentational: Boolean? = null
)

@Serializable
data class Metadata(
    val importance: Int,
    val tags: List<String>,
    val entities: List<String>? = null,
    @SerialName("follow_topic_id") val followTopicId: String? = null,
    @SerialName("is_breaking") val isBreaking: Boolean,
    val confidence: Float
)

@Serializable
data class AssistantPrompts(
    @SerialName("suggested_questions") val suggestedQuestions: List<String>? = null
)
