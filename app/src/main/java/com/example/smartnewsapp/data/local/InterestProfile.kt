package com.example.smartnewsapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "interest_profile",
    indices = [Index(value = ["keyword"], unique = true)]
)
data class InterestProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val keyword: String,
    val score: Float, // Positive for liked, negative for disliked
    val lastUpdated: Long
)
