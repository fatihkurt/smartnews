package com.example.smartnewsapp.domain.gateway

import com.example.smartnewsapp.data.local.Article

interface NewsGateway {
    suspend fun fetchLatestNews(): List<Article>
}
