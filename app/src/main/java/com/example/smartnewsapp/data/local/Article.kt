package com.example.smartnewsapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "articles")
data class Article(
    @PrimaryKey
    val id: String,
    val title: String,
    val summary: String,
    val content: String,
    val sourceUrl: String,
    val imageUrl: String? = null,
    val author: String,
    val publishedAt: Long,
    val relevanceScore: Float,
    val isRead: Boolean = false,
    val isSaved: Boolean = false
)
