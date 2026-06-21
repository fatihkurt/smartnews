package com.example.smartnewsapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM interest_profile ORDER BY score DESC")
    fun getInterestProfile(): Flow<List<InterestProfile>>

    @Query("SELECT * FROM interest_profile WHERE keyword = :keyword")
    suspend fun getProfileByKeyword(keyword: String): InterestProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: InterestProfile)
}
