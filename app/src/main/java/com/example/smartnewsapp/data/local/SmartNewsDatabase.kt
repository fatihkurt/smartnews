package com.example.smartnewsapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Article::class, InterestProfile::class, ChatMessage::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SmartNewsDatabase : RoomDatabase() {
    abstract fun newsDao(): NewsDao
    abstract fun profileDao(): ProfileDao
    abstract fun chatDao(): ChatDao
}
