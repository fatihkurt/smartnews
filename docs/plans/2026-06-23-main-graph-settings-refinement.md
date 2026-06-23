# Main Branch Graph Settings Refinement Plan

> **For Gemini:** Implement this plan on top of SmartNews `main` after pulling latest `origin/main`. This is a refinement plan based on inspecting `origin/main` commit `930a17f`. Keep personalization local; do not send interest graph/profile data to Hermes API.

**Goal:** Make the current profile categories and implicit interest graph actually affect the main feed, while preserving the user's existing local SQLite data and avoiding duplicate `interest_profile` rows.

**Architecture:** SmartApp should fetch/cache the full generic Hermes news feed, then combine cached articles with local `interest_profile` and `SettingsRepository.selectedCategories` to compute a personalized in-memory order. Profile/category/feedback data stays in Room/DataStore only. The API remains generic/stateless.

**Tech Stack:** Kotlin, Jetpack Compose, Room, DataStore Preferences, Flow `combine`, Hilt, Retrofit, kotlinx.serialization.

---

## Inspection summary from latest `main`

Pulled latest main:

- repo: `/home/hermes/repos/smartnews`
- branch inspected: `main`
- latest commit: `930a17f feat: Add profile language & category selection, fix chat scroll and link source URL`

Current state:

- Profile category settings exist:
  - `app/src/main/java/com/example/smartnewsapp/domain/SettingsRepository.kt`
  - `selectedLanguage: String`
  - `selectedCategories: Set<String>`
  - `updateSelectedLanguage(...)`
  - `updateSelectedCategories(...)`
- Profile UI shows category chips:
  - `app/src/main/java/com/example/smartnewsapp/ui/profile/ProfileScreen.kt`
- Implicit graph storage exists:
  - `app/src/main/java/com/example/smartnewsapp/data/local/InterestProfile.kt`
  - `app/src/main/java/com/example/smartnewsapp/data/local/ProfileDao.kt`
  - `app/src/main/java/com/example/smartnewsapp/domain/InterestGraphManager.kt`
- Main feed does not use either selected categories or graph scores:
  - `MainViewModel.articles = newsRepository.getArticles()`
  - `NewsRepository.getArticles() = newsDao.getAllArticles()`
  - `NewsDao.getAllArticles()` sorts only by `publishedAt DESC`
- Article read/like/dislike events are not connected to `InterestGraphManager` from real UI paths.
- There is no `LocalRecommendationScorer` yet.
- There is no like/dislike feedback UI yet.
- Room DB currently uses destructive migration:
  - `AppModule.kt:53` has `.fallbackToDestructiveMigration().build()`
- `interest_profile.keyword` is not unique, so duplicate keyword rows are possible.
- Tests/build could not be run in this environment because Java is unavailable:
  - `bash ./gradlew testDebugUnitTest --no-daemon`
  - result: `JAVA_HOME is not set and no 'java' command could be found in your PATH.`

Conclusion: graph/settings do not currently work as a recommendation system. They are collected/displayed, but not used to order the feed and not safely updated.

---

## Current SQLite schema constraints from user-provided dump

The current local database dump has:

```sql
CREATE TABLE `articles` (
  `id` TEXT NOT NULL,
  `title` TEXT NOT NULL,
  `category` TEXT NOT NULL,
  `source` TEXT NOT NULL,
  `publishedAt` TEXT NOT NULL,
  `updatedAt` TEXT,
  `language` TEXT NOT NULL,
  `content` TEXT NOT NULL,
  `media` TEXT NOT NULL,
  `metadata` TEXT NOT NULL,
  `assistantPrompts` TEXT,
  `isRead` INTEGER NOT NULL,
  `isSaved` INTEGER NOT NULL,
  PRIMARY KEY(`id`)
);
CREATE TABLE `interest_profile` (
  `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  `keyword` TEXT NOT NULL,
  `score` REAL NOT NULL,
  `lastUpdated` INTEGER NOT NULL
);
CREATE TABLE `chat_messages` (
  `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  `articleId` TEXT NOT NULL,
  `isUser` INTEGER NOT NULL,
  `message` TEXT NOT NULL,
  `provider` TEXT NOT NULL,
  `timestamp` INTEGER NOT NULL
);
```

Important implications:

1. `articles.source`, `content`, `media`, `metadata`, and `assistantPrompts` are JSON/text columns. Preserve the `Converters.kt` structured-model storage added on `main`.
2. `interest_profile.keyword` has no unique index. Current `@Insert(onConflict = REPLACE)` only conflicts on primary key `id`, not keyword.
3. If a new `InterestProfile(keyword = existingKeyword, id = 0, ...)` is inserted, Room can create duplicate keyword rows.
4. Before local ranking depends on graph scores, fix keyword uniqueness and dedupe existing data.
5. Do not use destructive migration because users already have valuable local interests and cached articles.

---

## Privacy/API boundary requirements

Do not send these to Hermes API:

- `selectedCategories`
- `interest_profile` scores
- like/dislike events
- read time
- saves/hides
- user id
- device id
- chat history/content for recommendation

The graph should be used locally after fetching/caching news.

Correct flow:

```text
Hermes API full generic feed
        ↓
NewsRepository.syncNews()
        ↓
Room articles table
        ↓
combine(newsDao.getAllArticles(), interestGraphManager.getInterestProfile(), settingsRepository.settings)
        ↓
LocalRecommendationScorer.sortArticles(...)
        ↓
MainScreen personalized order
```

Recommendations should reorder, not reduce, article count. Negative interests down-rank only; they do not delete/hide unless a separate explicit hide feature is added later.

---

## Task 1: Remove destructive migration and add explicit Room migration path

**Objective:** Preserve existing local data while changing `interest_profile` to be safe for recommendation scoring.

**Files:**

- Modify: `app/src/main/java/com/example/smartnewsapp/di/AppModule.kt`
- Modify: `app/src/main/java/com/example/smartnewsapp/data/local/SmartNewsDatabase.kt`
- Possibly create: `app/src/main/java/com/example/smartnewsapp/data/local/Migrations.kt`

**Steps:**

1. Remove `.fallbackToDestructiveMigration()` from `Room.databaseBuilder(...)`.
2. Add explicit migrations via `.addMigrations(...)`.
3. Keep `@TypeConverters(Converters::class)` intact.
4. Bump DB version from `4` to `5` when adding unique keyword index.

**Suggested migration file:**

```kotlin
package com.example.smartnewsapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // See Task 2 for dedupe/index SQL.
    }
}
```

**Verification:**

- App opens an existing DB without destructive reset.
- Existing articles/interests remain visible.
- `Room.databaseBuilder(...).addMigrations(MIGRATION_4_5).build()` is used.

---

## Task 2: Make `interest_profile.keyword` unique and dedupe existing rows

**Objective:** Prevent duplicate keyword rows so graph scores are reliable.

**Files:**

- Modify: `app/src/main/java/com/example/smartnewsapp/data/local/InterestProfile.kt`
- Modify: `app/src/main/java/com/example/smartnewsapp/data/local/ProfileDao.kt`
- Modify/create migration from Task 1.

**Entity change:**

```kotlin
import androidx.room.Index

@Entity(
    tableName = "interest_profile",
    indices = [Index(value = ["keyword"], unique = true)]
)
data class InterestProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val keyword: String,
    val score: Float,
    val lastUpdated: Long
)
```

**DAO changes:**

```kotlin
@Query("SELECT * FROM interest_profile ORDER BY score DESC")
fun getInterestProfile(): Flow<List<InterestProfile>>

@Query("SELECT * FROM interest_profile WHERE keyword = :keyword LIMIT 1")
suspend fun getProfileByKeyword(keyword: String): InterestProfile?

@Query("SELECT * FROM interest_profile ORDER BY ABS(score) DESC LIMIT :limit")
suspend fun getTopInterestProfiles(limit: Int): List<InterestProfile>

@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun upsertProfile(profile: InterestProfile)
```

Keep `insertProfile(...)` as a wrapper if existing code still calls it, or update call sites.

**Migration requirements:**

Before creating the unique index, dedupe existing rows. Two acceptable policies:

Preferred policy:

- normalize keyword with `LOWER(TRIM(keyword))`
- group duplicates
- sum scores
- clamp final score to `[-10, 10]`
- keep `MAX(lastUpdated)`
- delete duplicate rows
- create `CREATE UNIQUE INDEX IF NOT EXISTS index_interest_profile_keyword ON interest_profile(keyword)`

Safer fallback policy:

- keep the most recently updated row per normalized keyword
- delete older duplicates
- create unique index

**Verification SQL:**

```sql
SELECT keyword, COUNT(*)
FROM interest_profile
GROUP BY keyword
HAVING COUNT(*) > 1;
```

Expected: no rows.

---

## Task 3: Add `LocalRecommendationScorer`

**Objective:** Centralize local ranking logic that uses selected categories and interest graph scores.

**Files:**

- Create: `app/src/main/java/com/example/smartnewsapp/domain/LocalRecommendationScorer.kt`
- Test if test setup is available: `app/src/test/java/com/example/smartnewsapp/domain/LocalRecommendationScorerTest.kt`

**Suggested API:**

```kotlin
class LocalRecommendationScorer @Inject constructor() {
    fun sortArticles(
        articles: List<Article>,
        selectedCategories: Set<String>,
        interests: List<InterestProfile>
    ): List<Article> = articles
        .mapIndexed { index, article -> article to score(article, selectedCategories, interests, index) }
        .sortedWith(compareByDescending<Pair<Article, Float>> { it.second }.thenBy { articles.indexOf(it.first) })
        .map { it.first }

    fun score(
        article: Article,
        selectedCategories: Set<String>,
        interests: List<InterestProfile>,
        originalIndex: Int = 0
    ): Float {
        // Implement weights below.
    }
}
```

**Feature extraction:**

- category: `article.category`
- tags: `article.metadata.tags`
- entities: `article.metadata.entities ?: emptyList()`
- follow topic: `article.metadata.followTopicId`
- source: `article.source.domain ?: article.source.name`

**Weights:**

- selected category match: `+3.0`
- tag interest: `interest.score * 0.8`
- entity interest: `interest.score * 0.4`
- follow topic interest: `interest.score * 1.2`
- source interest: `interest.score * 0.2`
- already read: `-1.5`
- saved: `+1.0`

Rules:

- Normalize tokens: trim, lowercase, replace spaces/underscores with hyphens.
- Negative scores lower ranking but never remove articles.
- Preserve deterministic tie-break using existing feed order.

---

## Task 4: Use graph/settings in `NewsRepository` feed flow

**Objective:** Make the main feed actually reflect profile categories and graph scores.

**Files:**

- Modify: `app/src/main/java/com/example/smartnewsapp/domain/NewsRepository.kt`
- Modify: `app/src/main/java/com/example/smartnewsapp/ui/MainViewModel.kt`
- Modify DI if needed: `app/src/main/java/com/example/smartnewsapp/di/AppModule.kt`

**Implementation:**

Inject:

- `InterestGraphManager`
- `SettingsRepository`
- `LocalRecommendationScorer`

Add:

```kotlin
fun getRecommendedArticles(): Flow<List<Article>> = combine(
    newsDao.getAllArticles(),
    interestGraphManager.getInterestProfile(),
    settingsRepository.settings
) { articles, interests, settings ->
    localRecommendationScorer.sortArticles(
        articles = articles,
        selectedCategories = settings.selectedCategories,
        interests = interests
    )
}
```

Then change:

```kotlin
val articles = newsRepository.getArticles()
```

to:

```kotlin
val articles = newsRepository.getRecommendedArticles()
```

**Verification:**

- Selecting categories on Profile changes Main feed ordering.
- Updating interest scores changes Main feed ordering.
- Article count remains the same.

---

## Task 5: Improve `InterestGraphManager` updates

**Objective:** Make feedback/read-time update structured article features, not only manually provided keyword lists.

**Files:**

- Modify: `app/src/main/java/com/example/smartnewsapp/domain/InterestGraphManager.kt`

**Add methods:**

```kotlin
suspend fun handleArticleLiked(article: Article)
suspend fun handleArticleDisliked(article: Article)
suspend fun handleArticleReadTime(article: Article, readTimeSeconds: Long)
```

Keep old methods if needed for compatibility.

**Feature deltas:**

Like:

- category: `+1.5`
- tags: `+1.0`
- entities: `+0.5`
- followTopicId: `+2.0`
- source: `+0.25` optional

Dislike:

- category: `-0.75`
- tags: `-1.0`
- entities: `-0.5`
- followTopicId: `-2.0`
- source: `-0.25` optional

Read time:

- `>30s`: small `+0.5` to category/tags or existing old method equivalent.

**Score hygiene:**

- clamp scores to `[-10, 10]`
- apply decay before update:

```kotlin
val days = ((nowMillis - lastUpdated) / DAY_MS).coerceAtLeast(0)
val decayed = oldScore * 0.98f.pow(days)
val newScore = (decayed + delta).coerceIn(-10f, 10f)
```

Always preserve existing `id` when updating.

---

## Task 6: Add like/dislike feedback UI, no neutral button

**Objective:** Add explicit user feedback that updates local graph.

**Files:**

- Modify: `app/src/main/java/com/example/smartnewsapp/ui/chat/ChatViewModel.kt`
- Modify: `app/src/main/java/com/example/smartnewsapp/ui/chat/ChatScreen.kt`

**Implementation:**

- Add two actions in article detail header:
  - `👍 More like this`
  - `👎 Less like this`
- Do not add a neutral button. No action is neutral.
- ViewModel methods:

```kotlin
fun likeCurrentArticle()
fun dislikeCurrentArticle()
```

- Call `InterestGraphManager.handleArticleLiked(article)` / `handleArticleDisliked(article)`.
- Show snackbar with `Undo` if practical.

**Verification:**

- Like increases relevant score rows on Profile screen.
- Dislike decreases relevant rows.
- Main feed reorders after navigating back.
- No network call includes feedback data.

---

## Task 7: Record read-time once per article session

**Objective:** Make implicit graph collection real but weaker than explicit feedback.

**Files:**

- Modify: `app/src/main/java/com/example/smartnewsapp/ui/chat/ChatScreen.kt`
- Modify: `app/src/main/java/com/example/smartnewsapp/ui/chat/ChatViewModel.kt`

**Implementation:**

- Start timer when `ChatScreen(articleId)` opens.
- On dispose or after threshold, record once.
- If read time >30 seconds, call `handleArticleReadTime(article, seconds)`.
- Guard against duplicate recording from recomposition.

**Verification:**

- Quick open/close does not change scores.
- Staying >30 seconds applies small score increase once.

---

## Task 8: Keep Hermes API generic when fetching news

**Objective:** Avoid leaking personal recommendation signals to server logs or backend.

**Files:**

- Inspect/modify: `app/src/main/java/com/example/smartnewsapp/data/remote/gateway/HermesGateway.kt`
- Inspect/modify: `app/src/main/java/com/example/smartnewsapp/data/remote/HermesApi.kt`

**Rules:**

- Do not append `categories` query param.
- Do not append `interests` query param.
- Do not append user/device ids.
- Do not append read/like/dislike events.
- Do not append `limit` by default.
- Fetch full generic feed from `/api/news` or `/api/news/recommended` when server endpoint is ready.

**Verification:**

- Network logs show no personal recommendation params.
- Full feed still downloads.

---

## Task 9: Verification commands

Run locally where Java/Android SDK are available:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

If wrapper is not executable, fix once:

```bash
chmod +x gradlew
```

Manual checks:

1. Open Profile; select categories.
2. Pull-to-refresh Main feed.
3. Verify feed count stays same but order changes by selected categories.
4. Open article; tap Like.
5. Return to Profile; verify score rows changed.
6. Return to Main; verify related articles move up.
7. Open another article; tap Dislike.
8. Verify related articles move down but are still present.
9. Confirm network request URL/body does not include categories/interests/user ids/events.
10. Upgrade over an existing DB; verify no destructive migration and no duplicate keywords.

---

## Acceptance criteria

- `selectedCategories` affects local feed order.
- `interest_profile` affects local feed order.
- Like/dislike/read-time update `interest_profile` locally.
- `interest_profile.keyword` cannot duplicate after migration.
- No destructive migration is used.
- No personal recommendation data is sent to Hermes API.
- Recommendations reorder articles without reducing count.
