package com.example.smartnewsapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsDao {
    @Query("SELECT * FROM articles ORDER BY publishedAt DESC")
    fun getAllArticles(): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE id = :id")
    fun getArticleById(id: String): Flow<Article>

    @Query("SELECT * FROM articles WHERE id IN (:ids)")
    suspend fun getArticlesByIds(ids: List<String>): List<Article>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticlesReplace(articles: List<Article>)

    @Transaction
    suspend fun insertArticles(articles: List<Article>) {
        val existingArticles = getArticlesByIds(articles.map { it.id })
        val existingMap = existingArticles.associateBy { it.id }
        
        val articlesToUpsert = articles.map { article ->
            val existing = existingMap[article.id]
            if (existing != null) {
                val preserveMedia = existing.media.image.url != null && 
                                    existing.media.image.type != "none" && 
                                    existing.media.image.type != "fallback_category"
                                    
                article.copy(
                    isRead = existing.isRead,
                    isSaved = existing.isSaved,
                    feedback = existing.feedback,
                    media = if (preserveMedia) existing.media else article.media
                )
            } else {
                article
            }
        }
        
        insertArticlesReplace(articlesToUpsert)
    }

    @Query("UPDATE articles SET isRead = :isRead WHERE id = :id")
    suspend fun updateArticleReadStatus(id: String, isRead: Boolean)

    @Query("UPDATE articles SET feedback = :feedback WHERE id = :id")
    suspend fun updateArticleFeedback(id: String, feedback: Int)
}
