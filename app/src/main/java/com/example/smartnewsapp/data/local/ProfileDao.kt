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

    @Query("SELECT * FROM interest_profile ORDER BY ABS(score) DESC LIMIT :limit")
    suspend fun getTopInterestProfiles(limit: Int): List<InterestProfile>

    @Query("SELECT * FROM interest_profile WHERE keyword = :keyword LIMIT 1")
    suspend fun getProfileByKeyword(keyword: String): InterestProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: InterestProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: InterestProfile)
}
