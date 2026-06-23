package com.example.smartnewsapp.data.remote.gateway

import com.example.smartnewsapp.data.local.*
import com.example.smartnewsapp.data.remote.ChatResponse
import com.example.smartnewsapp.data.remote.Choice
import com.example.smartnewsapp.data.remote.Message
import com.example.smartnewsapp.domain.gateway.ChatGateway
import com.example.smartnewsapp.domain.gateway.NewsGateway
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockGateway @Inject constructor() : NewsGateway, ChatGateway {

    override suspend fun fetchLatestNews(): List<Article> {
        delay(1500) // Simulate network delay
        return listOf(
            Article(
                id = "mock_1",
                title = "Local LLMs Revolutionize Mobile Apps",
                category = "technology",
                source = Source(name = "Mock Tech News", url = "https://mock.news/article/1", domain = "mock.news"),
                publishedAt = "2026-06-23T08:00:00Z",
                updatedAt = "2026-06-23T08:30:00Z",
                language = "en",
                content = Content(
                    summary = "Developers are finding creative ways to run large language models locally on Android devices.",
                    expanded = "Recent advancements in quantization techniques have allowed mobile developers to run parameter-heavy models natively on Android devices. This enables offline inference, ensuring strict data privacy and significantly reducing server costs. While battery life remains a concern, the new NPUs are alleviating much of the computation strain.",
                    whyItMatters = "Running AI locally means your data never leaves your phone, ensuring absolute privacy while avoiding expensive cloud computing costs.",
                    background = "Historically, LLMs required massive cloud server farms to run efficiently.",
                    keyPoints = listOf("Local execution ensures privacy", "Reduces server costs", "NPUs mitigate battery drain"),
                    terms = listOf(Term("Quantization", "A technique to reduce the precision of neural networks to make them smaller and faster."))
                ),
                media = Media(image = ImageInfo(url = "https://picsum.photos/seed/llm/800/400", type = "source", alt = "Mobile phone with AI chip", isRepresentational = true)),
                metadata = Metadata(importance = 4, tags = listOf("AI", "Android", "Mobile"), isBreaking = false, confidence = 0.95f),
                assistantPrompts = AssistantPrompts(listOf("How does quantization work?", "Which Android devices support local LLMs?"))
            ),
            Article(
                id = "mock_2",
                title = "Jetpack Compose Navigation Updated",
                category = "technology",
                source = Source(name = "Android Developers", url = "https://mock.news/article/2", domain = "mock.news"),
                publishedAt = "2026-06-22T14:00:00Z",
                language = "en",
                content = Content(
                    summary = "The new androidx.navigation3 library simplifies passing arguments.",
                    expanded = "Navigation in Jetpack Compose has always been a point of contention for developers used to the older XML/Fragment system. The latest navigation3 library introduces a robust, type-safe way to define your navigation graphs. By leveraging generic types, you can now seamlessly pass objects directly as Navigation Keys without complex serialization hacks.",
                    whyItMatters = "It severely cuts down the boilerplate code required to build complex multi-screen Android apps, making development faster and less error-prone.",
                    keyPoints = listOf("Introduces type-safe navigation", "No more complex serialization hacks")
                ),
                media = Media(image = ImageInfo(url = "https://picsum.photos/seed/compose/800/400", type = "source")),
                metadata = Metadata(importance = 3, tags = listOf("Android", "Compose", "Kotlin"), isBreaking = false, confidence = 0.9f)
            ),
            Article(
                id = "mock_3",
                title = "Global Chip Shortage Finally Easing",
                category = "economy",
                source = Source(name = "Global Finance", url = "https://mock.news/article/3"),
                publishedAt = "2026-06-21T09:00:00Z",
                language = "en",
                content = Content(
                    summary = "Major semiconductor manufacturers report stabilizing supply chains.",
                    expanded = "After years of supply constraints following the pandemic, major players in the semiconductor industry are finally seeing light at the end of the tunnel. Production capacities have been significantly expanded in multiple regions. Experts predict that consumer electronics prices may start to normalize by the end of the year, though automotive chips remain somewhat constrained.",
                    whyItMatters = "You might finally see price drops on electronics like laptops, smartphones, and game consoles as the supply catches up with demand.",
                    keyPoints = listOf("Semiconductor production expanded", "Consumer electronics prices may normalize")
                ),
                media = Media(image = ImageInfo(url = "https://picsum.photos/seed/chips/800/400", type = "source")),
                metadata = Metadata(importance = 5, tags = listOf("Economy", "Technology", "Supply Chain"), isBreaking = true, confidence = 0.88f)
            ),
            Article(
                id = "mock_4",
                title = "The Rise of Rust in Android Platform",
                category = "technology",
                source = Source(name = "Tech Insider", url = "https://developer.android.com/"),
                publishedAt = "2026-06-20T10:00:00Z",
                language = "en",
                content = Content(
                    summary = "Google expands its use of memory-safe languages in the Android OS.",
                    expanded = "Google's commitment to memory safety continues as the proportion of Rust code in the Android Open Source Project (AOSP) grows steadily. This transition has led to a measurable decrease in memory-related security vulnerabilities. While C and C++ still dominate legacy components, all new system-level services are being actively evaluated for Rust implementation.",
                    whyItMatters = "Your Android phone will become significantly harder to hack and more stable as core system components are rewritten in a safer language."
                ),
                media = Media(image = ImageInfo(url = null, type = "fallback_category")),
                metadata = Metadata(importance = 4, tags = listOf("Security", "Android", "Rust"), isBreaking = false, confidence = 0.92f),
                assistantPrompts = AssistantPrompts(listOf("Why is Rust safer than C++?", "How much of Android is written in Rust now?"))
            )
        )
    }

    override suspend fun chat(messages: List<Message>): ChatResponse {
        delay(2000) // Simulate network delay
        return ChatResponse(
            choices = listOf(
                Choice(
                    message = Message(
                        role = "assistant",
                        content = "This is a mock response from the LLM. In a real scenario, I would analyze your prompt along with the article context and provide a relevant, insightful answer!"
                    )
                )
            )
        )
    }
}
