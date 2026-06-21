package com.example.smartnewsapp.data.remote.gateway

import com.example.smartnewsapp.data.local.Article
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
                summary = "Developers are finding creative ways to run large language models locally on Android devices.",
                content = "Recent advancements in quantization techniques have allowed mobile developers to run parameter-heavy models natively on Android devices. This enables offline inference, ensuring strict data privacy and significantly reducing server costs.\n\nWhile battery life remains a concern, the new NPUs are alleviating much of the computation strain.",
                sourceUrl = "https://mock.news/article/1",
                imageUrl = "https://picsum.photos/seed/llm/800/400",
                author = "Mock Author A",
                publishedAt = System.currentTimeMillis() - 3600000,
                relevanceScore = 0.95f
            ),
            Article(
                id = "mock_2",
                title = "Jetpack Compose Navigation Updated",
                summary = "The new androidx.navigation3 library simplifies passing arguments.",
                content = "Navigation in Jetpack Compose has always been a point of contention for developers used to the older XML/Fragment system. The latest navigation3 library introduces a robust, type-safe way to define your navigation graphs.\n\nBy leveraging generic types, you can now seamlessly pass objects directly as Navigation Keys without complex serialization hacks.",
                sourceUrl = "https://mock.news/article/2",
                imageUrl = "https://picsum.photos/seed/compose/800/400",
                author = "Mock Author B",
                publishedAt = System.currentTimeMillis() - 7200000,
                relevanceScore = 0.82f
            ),
            Article(
                id = "mock_3",
                title = "Global Chip Shortage Finally Easing",
                summary = "Major semiconductor manufacturers report stabilizing supply chains.",
                content = "After years of supply constraints following the pandemic, major players in the semiconductor industry are finally seeing light at the end of the tunnel. Production capacities have been significantly expanded in multiple regions.\n\nExperts predict that consumer electronics prices may start to normalize by the end of the year, though automotive chips remain somewhat constrained.",
                sourceUrl = "https://mock.news/article/3",
                imageUrl = "https://picsum.photos/seed/chips/800/400",
                author = "Mock Author C",
                publishedAt = System.currentTimeMillis() - 14400000,
                relevanceScore = 0.76f
            ),
            Article(
                id = "mock_4",
                title = "The Rise of Rust in Android Platform",
                summary = "Google expands its use of memory-safe languages in the Android OS.",
                content = "Google's commitment to memory safety continues as the proportion of Rust code in the Android Open Source Project (AOSP) grows steadily. This transition has led to a measurable decrease in memory-related security vulnerabilities.\n\nWhile C and C++ still dominate legacy components, all new system-level services are being actively evaluated for Rust implementation.",
                sourceUrl = "https://mock.news/article/4",
                imageUrl = "https://picsum.photos/seed/rust/800/400",
                author = "Mock Author D",
                publishedAt = System.currentTimeMillis() - 28800000,
                relevanceScore = 0.91f
            ),
            Article(
                id = "mock_5",
                title = "Quantum Computing Milestone Reached",
                summary = "Researchers demonstrate error correction at scale.",
                content = "A critical hurdle in quantum computing has been overcome: maintaining qubit stability. A team of international researchers successfully implemented a scalable error-correction algorithm that dramatically increases coherence time.\n\nThis breakthrough paves the way for practical quantum applications in cryptography and materials science within the next decade.",
                sourceUrl = "https://mock.news/article/5",
                imageUrl = "https://picsum.photos/seed/quantum/800/400",
                author = "Mock Author E",
                publishedAt = System.currentTimeMillis() - 86400000,
                relevanceScore = 0.65f
            ),
            Article(
                id = "mock_6",
                title = "New Material Could Revolutionize Batteries",
                summary = "Solid-state batteries move closer to commercial reality.",
                content = "A novel ceramic polymer composite has shown immense promise as a solid electrolyte. Batteries utilizing this material exhibit higher energy density and completely eliminate the flammability risks associated with liquid electrolytes.\n\nAutomakers are already racing to partner with the laboratory, hoping to integrate this technology into next-generation EVs.",
                sourceUrl = "https://mock.news/article/6",
                imageUrl = "https://picsum.photos/seed/battery/800/400",
                author = "Mock Author F",
                publishedAt = System.currentTimeMillis() - 172800000,
                relevanceScore = 0.70f
            ),
            Article(
                id = "mock_7",
                title = "KMP adoption skyrockets in 2026",
                summary = "Kotlin Multiplatform becomes the defacto standard for mobile logic sharing.",
                content = "With the stabilization of KMP and Compose Multiplatform, teams are rapidly migrating away from competing frameworks. The ability to write native UI while sharing pure Kotlin business logic has proven to be the most maintainable approach for large teams.\n\nMajor companies have reported a 40% reduction in code duplication across iOS and Android teams.",
                sourceUrl = "https://mock.news/article/7",
                imageUrl = "https://picsum.photos/seed/kmp/800/400",
                author = "Mock Author G",
                publishedAt = System.currentTimeMillis() - 259200000,
                relevanceScore = 0.88f
            ),
            Article(
                id = "mock_8",
                title = "Space Tourism Hits Record Numbers",
                summary = "Suborbital flights are becoming increasingly common.",
                content = "The commercial spaceflight industry has seen a massive surge in bookings this year. With reusable rocket technology driving down costs, brief suborbital excursions are now accessible to thousands rather than just a handful of billionaires.\n\nHowever, environmental groups are raising concerns about the atmospheric impact of these frequent launches.",
                sourceUrl = "https://mock.news/article/8",
                imageUrl = "https://picsum.photos/seed/space/800/400",
                author = "Mock Author H",
                publishedAt = System.currentTimeMillis() - 345600000,
                relevanceScore = 0.55f
            ),
            Article(
                id = "mock_9",
                title = "AI Code Assistants Reach Human Parity",
                summary = "New benchmarks show agentic coders outperforming average engineers.",
                content = "The latest generation of agentic coding assistants can now autonomously resolve complex architectural bugs across entire repositories. Benchmarks released today demonstrate an unprecedented success rate in real-world GitHub issues.\n\nThis shift is causing a re-evaluation of the junior developer role, emphasizing system design and code review over rote typing.",
                sourceUrl = "https://mock.news/article/9",
                imageUrl = "https://picsum.photos/seed/ai/800/400",
                author = "Mock Author I",
                publishedAt = System.currentTimeMillis() - 432000000,
                relevanceScore = 0.98f
            ),
            Article(
                id = "mock_10",
                title = "The Return of RSS",
                summary = "Users fleeing algorithmic timelines rediscover chronological feeds.",
                content = "In a surprising trend, 'old school' RSS readers are experiencing a renaissance. Frustrated by opaque algorithms and bloated social media platforms, tech-savvy users are building their own curated information feeds.\n\nThis movement focuses on intentional consumption, ensuring that important news isn't buried by engagement-driven metrics.",
                sourceUrl = "https://mock.news/article/10",
                imageUrl = "https://picsum.photos/seed/rss/800/400",
                author = "Mock Author J",
                publishedAt = System.currentTimeMillis() - 518400000,
                relevanceScore = 0.85f
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
