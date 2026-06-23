package com.example.smartnewsapp.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromSource(source: Source): String {
        return json.encodeToString(source)
    }

    @TypeConverter
    fun toSource(data: String): Source {
        return json.decodeFromString(data)
    }

    @TypeConverter
    fun fromContent(content: Content): String {
        return json.encodeToString(content)
    }

    @TypeConverter
    fun toContent(data: String): Content {
        return json.decodeFromString(data)
    }

    @TypeConverter
    fun fromMedia(media: Media): String {
        return json.encodeToString(media)
    }

    @TypeConverter
    fun toMedia(data: String): Media {
        return json.decodeFromString(data)
    }

    @TypeConverter
    fun fromMetadata(metadata: Metadata): String {
        return json.encodeToString(metadata)
    }

    @TypeConverter
    fun toMetadata(data: String): Metadata {
        return json.decodeFromString(data)
    }

    @TypeConverter
    fun fromAssistantPrompts(assistantPrompts: AssistantPrompts?): String? {
        if (assistantPrompts == null) return null
        return json.encodeToString(assistantPrompts)
    }

    @TypeConverter
    fun toAssistantPrompts(data: String?): AssistantPrompts? {
        if (data == null) return null
        return json.decodeFromString(data)
    }
}
