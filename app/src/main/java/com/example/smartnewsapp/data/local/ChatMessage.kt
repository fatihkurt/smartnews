package com.example.smartnewsapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val articleId: String,
    val isUser: Boolean,
    val message: String,
    val provider: String = "",
    val timestamp: Long
)
