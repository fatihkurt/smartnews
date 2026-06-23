package com.example.smartnewsapp.data.remote.scraper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageScraper @Inject constructor(
    okHttpClient: OkHttpClient
) {
    // Clone the client and remove interceptors to prevent logging massive HTML bodies
    private val scraperClient = okHttpClient.newBuilder()
        .apply { interceptors().clear() }
        .build()

    suspend fun scrapeOgImage(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = scraperClient.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return@withContext null
            }

            val html = response.body?.string() ?: return@withContext null
            
            // Regex to match <meta property="og:image" content="..."> or <meta name="twitter:image" content="...">
            val ogRegex = """<meta\s+(?:property|name)=["'](?:og|twitter):image["']\s+content=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
            val ogRegexAlt = """<meta\s+content=["']([^"']+)["']\s+(?:property|name)=["'](?:og|twitter):image["']""".toRegex(RegexOption.IGNORE_CASE)
            
            val match = ogRegex.find(html) ?: ogRegexAlt.find(html)
            return@withContext match?.groupValues?.get(1)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
