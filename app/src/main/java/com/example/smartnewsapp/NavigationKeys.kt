package com.example.smartnewsapp

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey
@Serializable data class Chat(val articleId: String) : NavKey
@Serializable data object Profile : NavKey
@Serializable data object Settings : NavKey
