package com.example.smartnewsapp.di

import android.content.Context
import androidx.room.Room
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.smartnewsapp.data.local.ChatDao
import com.example.smartnewsapp.data.local.NewsDao
import com.example.smartnewsapp.data.local.ProfileDao
import com.example.smartnewsapp.data.local.SmartNewsDatabase
import com.example.smartnewsapp.data.remote.HermesApi
import com.example.smartnewsapp.data.remote.OpenRouterApi
import com.example.smartnewsapp.domain.gateway.ChatGateway
import com.example.smartnewsapp.data.remote.gateway.DynamicChatGateway
import com.example.smartnewsapp.data.remote.gateway.DynamicNewsGateway
import com.example.smartnewsapp.data.remote.gateway.HermesGateway
import com.example.smartnewsapp.data.remote.gateway.MockGateway
import com.example.smartnewsapp.domain.gateway.NewsGateway
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SmartNewsDatabase {
        return Room.databaseBuilder(
            context,
            SmartNewsDatabase::class.java,
            "smart_news.db"
        )
            .addMigrations(SmartNewsDatabase.MIGRATION_4_5, SmartNewsDatabase.MIGRATION_5_6)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideNewsDao(db: SmartNewsDatabase): NewsDao = db.newsDao()

    @Provides
    fun provideProfileDao(db: SmartNewsDatabase): ProfileDao = db.profileDao()

    @Provides
    fun provideChatDao(db: SmartNewsDatabase): ChatDao = db.chatDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .build()
    }

    @Provides
    @Singleton
    fun provideHermesApi(okHttpClient: OkHttpClient): HermesApi {
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl("https://hermes.example.com/") // This should ideally be configurable
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(HermesApi::class.java)
    }

    @Provides
    @Singleton
    fun provideOpenRouterApi(okHttpClient: OkHttpClient): OpenRouterApi {
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl("https://openrouter.ai/api/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return retrofit.create(OpenRouterApi::class.java)
    }

    @Provides
    @Singleton
    fun provideNewsGateway(dynamicNewsGateway: DynamicNewsGateway): NewsGateway {
        return dynamicNewsGateway
    }

    @Provides
    @Singleton
    fun provideChatGateway(dynamicChatGateway: DynamicChatGateway): ChatGateway {
        return dynamicChatGateway
    }
}
