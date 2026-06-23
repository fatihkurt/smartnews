# Local Recommendation Engine Implementation Plan

> **For Gemini:** Implement this plan in `/home/hermes/repos/smartnews`. Keep user personalization on-device. Do not send profile categories, implicit interest scores, like/dislike events, article history, user ids, device ids, or chat content to Hermes API.

**Goal:** Complete SmartApp recommendations by using the local interest graph to re-rank the full Hermes news feed on-device, plus add simple like/dislike feedback that updates local Room scores.

**Architecture:** Hermes API provides a generic full feed (`/api/news`, later `/api/news/recommended`) without personal data. SmartApp stores personal signals in Room/DataStore and applies a local second-pass ranking in `NewsRepository`/`MainViewModel`. The interest graph is used after fetching/caching articles, not as a personalized server request.

**Tech Stack:** Android Kotlin, Jetpack Compose, Room, DataStore Preferences, Flow/combine, Hilt, Retrofit, kotlinx.serialization.

---

## Direct answer: where is the graph used?

Use the graph locally after news is fetched, not while making a personalized API request.

Flow:

1. `NewsRepository.syncNews()` fetches the full generic feed from Hermes.
2. `NewsDao.insertArticles(remoteArticles)` stores/cache articles locally.
3. Main feed observes a `Flow<List<Article>>` from Room.
4. Repository/ViewModel combines:
   - cached articles
   - `InterestGraphManager.getInterestProfile()`
   - `SettingsRepository.settings.selectedCategories`
5. `LocalRecommendationScorer.sortArticles(...)` computes local scores and returns personalized order.
6. `MainScreen` renders this locally sorted list.

So the graph should not reduce what the API returns. It should only change local order.

Recommended data flow:

```text
Hermes API full generic feed
        ↓
NewsRepository.syncNews()
        ↓
Room articles table
        ↓
combine(articles, interest_profile, selectedCategories)
        ↓
LocalRecommendationScorer.sortArticles()
        ↓
MainScreen personalized feed
```

---

## Privacy / generic API rules

- Do not send `categories` to Hermes API.
- Do not send `interest_profile` scores to Hermes API.
- Do not send `like`, `dislike`, read time, saves, hides, user id, device id, or chat history to Hermes API.
- Do not add `limit` by default; fetch the full feed so recommendations do not reduce result count.
- Like/dislike affects local order only.
- Dislike/negative interests down-rank; they do not delete or permanently hide articles.

---

## Existing files to inspect/use

Current implicit graph:

- `app/src/main/java/com/example/smartnewsapp/domain/InterestGraphManager.kt`
- `app/src/main/java/com/example/smartnewsapp/data/local/InterestProfile.kt`
- `app/src/main/java/com/example/smartnewsapp/data/local/ProfileDao.kt`

Latest profile/category work is on `origin/main`:

- `app/src/main/java/com/example/smartnewsapp/ui/profile/ProfileScreen.kt`
- `app/src/main/java/com/example/smartnewsapp/ui/profile/ProfileViewModel.kt`
- `app/src/main/java/com/example/smartnewsapp/domain/SettingsRepository.kt`

Feed path:

- `app/src/main/java/com/example/smartnewsapp/ui/MainViewModel.kt`
- `app/src/main/java/com/example/smartnewsapp/domain/NewsRepository.kt`
- `app/src/main/java/com/example/smartnewsapp/domain/gateway/NewsGateway.kt`
- `app/src/main/java/com/example/smartnewsapp/data/remote/gateway/HermesGateway.kt`
- `app/src/main/java/com/example/smartnewsapp/data/remote/HermesApi.kt`
- `app/src/main/java/com/example/smartnewsapp/ui/main/MainScreen.kt`
- `app/src/main/java/com/example/smartnewsapp/ui/chat/ChatScreen.kt`
- `app/src/main/java/com/example/smartnewsapp/ui/chat/ChatViewModel.kt`

Before coding, reconcile this branch with `origin/main` because latest Gemini category/profile work is not currently on `feat/vectororch-news-endpoint`.

---

## SQLite / Room database requirements from current device dump

The current local SQLite dump shared by Fatih contains these tables:

```sql
CREATE TABLE android_metadata (locale TEXT);
CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT);
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

- `articles.source`, `articles.content`, `articles.media`, `articles.metadata`, and `articles.assistantPrompts` are JSON/text columns in the actual DB dump. Latest `origin/main` models use structured Kotlin types plus `Converters.kt`; Gemini must preserve this converter-backed storage shape.
- `interest_profile.keyword` is not unique in the dump. Current DAO does `getProfileByKeyword(keyword)` and then `insertProfile(profile)` with `OnConflictStrategy.REPLACE`; because the primary key is auto-generated `id`, a new insert with `id = 0` will not conflict on `keyword`. Gemini must prevent duplicate keyword rows before relying on scores.
- The implementation should either:
  1. add a unique index on `interest_profile.keyword` and migrate/dedupe existing rows, or
  2. preserve `existingProfile.id` on every update and add a cleanup migration for any duplicate rows already present.
- Prefer option 1 for correctness: make `keyword` unique at the Room entity level and add a migration that deduplicates by normalized keyword.
- Do not add server-side DB/storage for recommendation data. These SQLite changes are Android Room/local-only.

Recommended Room entity change:

```kotlin
@Entity(
    tableName = "interest_profile",
    indices = [Index(value = ["keyword"], unique = true)]
)
data class InterestProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val keyword: String,
    val score: Float,
    val lastUpdated: Long
)
```

Recommended DAO change:

```kotlin
@Query("SELECT * FROM interest_profile WHERE keyword = :keyword LIMIT 1")
suspend fun getProfileByKeyword(keyword: String): InterestProfile?

@Query("SELECT * FROM interest_profile ORDER BY ABS(score) DESC LIMIT :limit")
suspend fun getTopInterestProfiles(limit: Int): List<InterestProfile>

@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun upsertProfile(profile: InterestProfile)
```

When updating an existing keyword, always copy from the existing row so the existing `id` is preserved:

```kotlin
val updated = existing.copy(
    score = newScore,
    lastUpdated = nowMillis
)
profileDao.upsertProfile(updated)
```

For new keywords, insert normally.

Migration/dedup requirement:

- Bump Room database version from the implementation branch's current version to the next version after merging `origin/main`.
- Note: current local branch has `SmartNewsDatabase.version = 2`; latest `origin/main` has `version = 4` with `Converters.kt`. After reconciling, add the next migration from the merged version, likely `MIGRATION_4_5`.
- Before adding a unique index, dedupe existing `interest_profile` rows by normalized keyword. Keep one row per keyword, preserving a useful score. Preferred dedupe policy:
  - group by lowercase trimmed keyword;
  - sum scores within the group;
  - clamp to `[-10, 10]`;
  - keep `MAX(lastUpdated)`;
  - delete duplicate rows;
  - create unique index `index_interest_profile_keyword`.
- If implementing exact SQL dedupe is too risky in a Room migration, do a simpler safe policy: keep the most recently updated row per keyword and delete older duplicates, then create the unique index.

Migration verification:

- Install/run over an existing DB containing duplicate `interest_profile.keyword` rows.
- Confirm Room opens without destructive migration.
- Confirm `SELECT keyword, COUNT(*) FROM interest_profile GROUP BY keyword HAVING COUNT(*) > 1` returns no rows.
- Confirm Profile screen still shows scores.
- Confirm like/dislike updates an existing row instead of creating duplicates.

---

## Feedback button decision

Add two explicit feedback buttons:

- 👍 Like / More like this
- 👎 Less like this

Do not add a neutral button.

Neutral is the default state: the user does nothing. A neutral button adds UI noise and creates ambiguity. If the user changes their mind after like/dislike, show a snackbar with `Undo`.

Suggested UI placement:

- First MVP: article detail/chat screen header or toolbar area.
- Avoid crowding the main feed cards initially.
- Later optional: overflow menu with `Follow story`, `Hide article`, `Less from this source`.

---

## Scoring semantics

### Implicit read-time signal

Keep weaker than explicit feedback:

- Open article only: optional `+0.1`
- Read >30 seconds: `+0.5`
- Read >90 seconds or reaches bottom: optional `+0.75`

### Explicit like

Like means “show me more like this”. Apply positive deltas locally:

- `article.category`: `+1.5`
- each `article.metadata.tags`: `+1.0`
- each `article.metadata.entities`: `+0.5`
- `article.metadata.followTopicId`: `+2.0`
- source domain/name: `+0.25` optional

### Explicit dislike

Dislike means “show me less like this”. Apply negative deltas locally:

- `article.category`: `-0.75`
- each `article.metadata.tags`: `-1.0`
- each `article.metadata.entities`: `-0.5`
- `article.metadata.followTopicId`: `-2.0`
- source domain/name: `-0.25` optional

Do not remove the article from the database/feed only because of dislike. Down-rank it.

### Stronger future actions

Implement later only if needed:

- Save/bookmark: `+3.0`
- Follow story: strong boost to `followTopicId`
- Hide article: local hidden state for that specific article
- Not interested in topic: stronger penalty for a tag/follow topic
- Less from source: stronger source-domain penalty

---

## Score hygiene

### Clamp scores

Clamp every `InterestProfile.score` after updates:

- min: `-10.0`
- max: `+10.0`

This prevents one topic from dominating forever.

### Decay old interests

Before applying a new delta, decay the old score based on `lastUpdated`:

```kotlin
val days = ((nowMillis - lastUpdated) / DAY_MS).coerceAtLeast(0)
val decayedScore = oldScore * 0.98f.pow(days)
val newScore = (decayedScore + delta).coerceIn(-10f, 10f)
```

Use a helper so all interaction types apply the same decay/clamp rules.

---

## Local ranking formula

Create a local article score like this:

```text
localScore =
  genericServerOrderBase
  + selectedCategoryBoost
  + tagInterestScore
  + entityInterestScore
  + followTopicInterestScore
  + sourceInterestScore
  - alreadyReadPenalty
  + savedBoost
```

Suggested weights:

- selected category match: `+3.0`
- tag interest: `interest.score * 0.8`
- entity interest: `interest.score * 0.4`
- follow topic interest: `interest.score * 1.2`
- source interest: `interest.score * 0.2`
- already read: `-1.5`
- saved: `+1.0`

Keep server/generic order as a tie-breaker so important/fresh news still appears.

---

## Task 1: Reconcile branch state

**Objective:** Implement on top of latest SmartApp profile/category code.

**Steps:**

1. Run `git status -sb`.
2. If there are local changes, stop and ask before modifying the branch.
3. Bring latest `origin/main` into the implementation branch using the preferred workflow:
   - merge `origin/main`, or
   - rebase on `origin/main`, or
   - create a new branch from `origin/main` and re-apply VectorOrch endpoint changes.
4. Verify `SettingsRepository.kt` contains `selectedCategories` and `ProfileScreen.kt` renders category chips.

**Verification:**

- `git log --oneline --decorate --graph --max-count=12 --all` shows the implementation branch contains `930a17f` or a descendant.

---

## Task 2: Add `LocalRecommendationScorer`

**Objective:** Centralize all on-device ranking logic.

**Files:**

- Create: `app/src/main/java/com/example/smartnewsapp/domain/LocalRecommendationScorer.kt`
- Test if test setup is healthy: `app/src/test/java/com/example/smartnewsapp/domain/LocalRecommendationScorerTest.kt`

**Implementation details:**

Create an injectable/scoped class or plain class with methods similar to:

```kotlin
class LocalRecommendationScorer {
    fun score(
        article: Article,
        selectedCategories: Set<String>,
        interests: List<InterestProfile>,
        serverRank: Int = 0
    ): Float

    fun sortArticles(
        articles: List<Article>,
        selectedCategories: Set<String>,
        interests: List<InterestProfile>
    ): List<Article>
}
```

Feature extraction:

- category: `article.category`
- tags: `article.metadata.tags`
- entities: `article.metadata.entities ?: emptyList()`
- follow topic: `article.metadata.followTopicId`
- source: `article.source.domain ?: article.source.name`

Rules:

- Normalize tokens locally: trim, lowercase, replace spaces/underscores with hyphens.
- Positive interest scores boost matching articles.
- Negative interest scores lower matching articles.
- Never filter out an article.
- Preserve deterministic ordering with stable tie-breakers.

**Verification:**

- Matching selected category moves an article upward.
- Matching positive tag/entity moves an article upward.
- Matching negative tag/entity moves an article downward but article remains present.
- Sort is deterministic when scores tie.

---

## Task 3: Fix Room schema for reliable interest graph updates

**Objective:** Make `interest_profile.keyword` unique and migration-safe before adding more feedback signals.

**Files:**

- Modify: `app/src/main/java/com/example/smartnewsapp/data/local/InterestProfile.kt`
- Modify: `app/src/main/java/com/example/smartnewsapp/data/local/ProfileDao.kt`
- Modify: `app/src/main/java/com/example/smartnewsapp/data/local/SmartNewsDatabase.kt`
- Add/modify migration location used by the app if migrations live outside `SmartNewsDatabase.kt`.

**Implementation details:**

- Add `indices = [Index(value = ["keyword"], unique = true)]` to `InterestProfile`.
- Add/keep `getProfileByKeyword(keyword)` with `LIMIT 1`.
- Add `getTopInterestProfiles(limit)` sorted by `ABS(score) DESC`.
- Rename `insertProfile` to `upsertProfile` if desired, but keep compatibility if existing code calls `insertProfile`.
- Always preserve `existingProfile.id` when updating an existing keyword.
- Add a Room migration from the reconciled DB version to the next version.
- Current branch has DB version 2; latest `origin/main` has DB version 4 with converters and JSON/text article columns. After branch reconciliation, likely add `MIGRATION_4_5`, not `2_3`.
- Migration must dedupe existing duplicate keywords before creating the unique index.
- Unique index name should be stable: `index_interest_profile_keyword`.

**Migration SQL guidance:**

Use one of these policies:

1. Preferred if SQL is straightforward: normalize with `LOWER(TRIM(keyword))`, sum scores, clamp to `[-10, 10]`, keep `MAX(lastUpdated)`.
2. Safer fallback: keep the row with the latest `lastUpdated` per normalized keyword, delete older duplicates, then create the unique index.

Do not use destructive migration; users already have useful local interest scores.

**Verification:**

- Room opens on an existing DB shaped like Fatih's dump.
- `interest_profile` has no duplicate keyword rows after migration.
- Profile screen still displays existing scores.
- Repeated like/dislike for the same keyword updates one row instead of inserting duplicates.
- Existing `articles` JSON/text converter behavior remains intact after merging `origin/main`.

---

## Task 4: Improve `InterestGraphManager` feature updates

**Objective:** Let screens pass an `Article`, not hand-built keyword lists.

**Files:**

- Modify: `app/src/main/java/com/example/smartnewsapp/domain/InterestGraphManager.kt`
- Modify: `app/src/main/java/com/example/smartnewsapp/data/local/ProfileDao.kt`

**Implementation details:**

Add methods:

```kotlin
suspend fun handleArticleLiked(article: Article)
suspend fun handleArticleDisliked(article: Article)
suspend fun handleArticleReadTime(article: Article, readTimeSeconds: Long)
suspend fun getTopInterestProfiles(limit: Int = 20): List<InterestProfile>
```

Keep existing methods for backward compatibility if already used.

Add DAO method:

```kotlin
@Query("SELECT * FROM interest_profile ORDER BY ABS(score) DESC LIMIT :limit")
suspend fun getTopInterestProfiles(limit: Int): List<InterestProfile>
```

Internally extract features with weights from the “Scoring semantics” section.

Add helper:

```kotlin
private suspend fun recordWeightedFeatures(features: List<WeightedFeature>)
```

Apply decay and clamp before saving each profile.

Important: this task depends on Task 3's unique-keyword migration. Do not build scoring on top of duplicate keyword rows.

**Verification:**

- Liking an article updates category/tags/entities/follow-topic scores.
- Disliking an article lowers scores.
- Reading >30 seconds gives smaller positive updates.
- Scores stay within `[-10, 10]`.
- Old scores decay when updated.

---

## Task 5: Add like/dislike UI with undo

**Objective:** Collect explicit feedback without adding a confusing neutral state.

**Files:**

- Modify: `app/src/main/java/com/example/smartnewsapp/ui/chat/ChatScreen.kt`
- Modify: `app/src/main/java/com/example/smartnewsapp/ui/chat/ChatViewModel.kt`
- Possibly modify: `app/src/main/java/com/example/smartnewsapp/ui/main/MainScreen.kt` later only if feed-card feedback is desired.

**Implementation details:**

- Add two actions on article detail/chat screen:
  - 👍 More like this
  - 👎 Less like this
- In `ChatViewModel`, expose:
  - `likeCurrentArticle()`
  - `dislikeCurrentArticle()`
- Call `InterestGraphManager.handleArticleLiked(article)` / `handleArticleDisliked(article)`.
- Show snackbar: `Feedback saved` with `Undo`.
- For undo, either:
  - apply inverse deltas, or
  - store last operation and restore previous scores if easy.

Do not add a neutral button. No feedback is neutral.

**Verification:**

- Tap like; Profile screen scores increase.
- Tap dislike; Profile screen scores decrease.
- Undo reverses the immediate change.
- Recomposition does not double-record feedback.

---

## Task 6: Record read-time interaction once per article session

**Objective:** Preserve implicit learning while keeping it weaker than explicit feedback.

**Files:**

- Modify: `app/src/main/java/com/example/smartnewsapp/ui/chat/ChatScreen.kt`
- Modify: `app/src/main/java/com/example/smartnewsapp/ui/chat/ChatViewModel.kt`

**Implementation details:**

- Start a timer when article detail/chat screen opens.
- On leaving the screen or after threshold, record once.
- If read time >30 seconds, call `handleArticleReadTime(article, readTimeSeconds)`.
- Guard with a per-screen-session flag so recomposition cannot record multiple times.

**Verification:**

- Open/close quickly: no score change.
- Stay >30 seconds: small score increase.
- Rotate/recompose: no duplicate increments.

---

## Task 7: Combine articles + graph + settings for feed ordering

**Objective:** Use the graph in the main feed rendering path.

**Files:**

- Modify: `app/src/main/java/com/example/smartnewsapp/domain/NewsRepository.kt`
- Modify: `app/src/main/java/com/example/smartnewsapp/ui/MainViewModel.kt`
- Possibly modify DI: `app/src/main/java/com/example/smartnewsapp/di/AppModule.kt`

**Implementation details:**

Expose something like:

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

Then make `MainViewModel.articles` use `getRecommendedArticles()` instead of raw `getArticles()`.

Avoid a Room migration for ranking in MVP. In-memory sorting is simpler and keeps personalization responsive.

**Verification:**

- Change selected categories in Profile; returning to Main feed changes order.
- Like an article; related articles move upward.
- Dislike an article; related articles move downward.
- Total number of articles remains unchanged.

---

## Task 8: Fetch generic server recommended endpoint, but no personal params

**Objective:** Use Hermes generic ranking when available while keeping personalization local.

**Files:**

- Modify: `app/src/main/java/com/example/smartnewsapp/data/remote/gateway/HermesGateway.kt`
- Possibly modify: `app/src/main/java/com/example/smartnewsapp/data/remote/HermesApi.kt`

**Implementation details:**

- If configured news URL is `/api/news`, keep working as-is until API endpoint exists.
- Once API endpoint exists, support `/api/news/recommended`.
- Only append non-personal params:
  - `language`, optional
  - `include_reasons`, optional debug/dev
- Do not append:
  - `categories`
  - `interests`
  - `user_id`
  - `device_id`
  - `limit` by default
  - event/history data

**Verification:**

- Network inspector/log confirms no personal params are sent.
- Full feed still downloads.
- Local ranking still works with `/api/news` and `/api/news/recommended`.

---

## Task 9: Optional recommendation reason display

**Objective:** Explain ranking without blocking MVP.

**Files:**

- Modify: `app/src/main/java/com/example/smartnewsapp/ui/main/MainScreen.kt`
- Modify `Article.kt` only if storing server reason fields is necessary.

**Implementation details:**

- Prefer local reasons from `LocalRecommendationScorer` later:
  - `Because you liked AI`
  - `Because you selected Technology`
  - `Less prominent because you disliked crypto`
- If server returns generic `recommendation_reasons`, show at most one short reason.
- Keep reason display optional.

**Verification:**

- Cards render normally without reason fields.
- Reason text does not crowd the card.

---

## Manual verification checklist

1. Select categories in Profile: `Technology`, `Artificial Intelligence`, `World News`.
2. Pull-to-refresh feed.
3. Confirm request does not contain `categories`, `interests`, user/device identifiers, or event/history params.
4. Like an AI/technology article.
5. Confirm Profile score rows update.
6. Confirm related articles move upward in the feed.
7. Dislike an unwanted topic.
8. Confirm related articles move downward but remain available.
9. Read an article for >30 seconds.
10. Confirm small score increase.
11. Turn network off.
12. Confirm cached articles still display and local ranking still applies.

Expected result:

- Hermes API remains generic/stateless.
- SmartApp keeps personal data local.
- The interest graph is used in local feed ordering after fetch/cache.
- Like/dislike/read-time update the graph.
- Recommendations reorder articles without reducing result count.
