package com.example.smartnewsapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Article::class, InterestProfile::class, ChatMessage::class],
    version = 2,
    exportSchema = false
)
abstract class SmartNewsDatabase : RoomDatabase() {
    abstract fun newsDao(): NewsDao
    abstract fun profileDao(): ProfileDao
    abstract fun chatDao(): ChatDao
}
