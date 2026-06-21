package com.example.smartnewsapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "interest_profile")
data class InterestProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val keyword: String,
    val score: Float, // Positive for liked, negative for disliked
    val lastUpdated: Long
)
