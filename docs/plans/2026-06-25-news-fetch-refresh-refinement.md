# News Fetch and Refresh Refinement

Date: 2026-06-25

## Goal

Make SmartNews refresh behavior understandable and reliable when using the Hermes
news API. Pull-to-refresh should either fetch genuinely fresh live news or make
it clear that the app reloaded the latest cached Hermes recommendation snapshot.

## Current Behavior

- Main feed startup and pull-to-refresh both call `MainViewModel.sync()`.
- `sync()` calls `NewsRepository.syncNews()`.
- `NewsRepository.syncNews()` calls `NewsGateway.fetchLatestNews()` and upserts
  returned articles into Room.
- The default Hermes news URL is `https://hermes.vectororch.com/api/news`.
- `HermesGateway.buildGenericNewsUrl()` rewrites URLs ending in `/api/news` to
  `/api/news/recommended` and appends the selected language.

This means the app currently fetches the generic recommended Hermes feed:

```text
https://hermes.vectororch.com/api/news/recommended?language=<code>
```

That endpoint is deterministic over the latest server-side cron snapshot. It is
healthy when repeated refreshes return the same article IDs until the Hermes
server cron output changes.

## Problems To Fix

1. Pull-to-refresh does not request fresh server-side news.
   - Files:
     - `app/src/main/java/com/example/smartnewsapp/ui/main/MainScreen.kt`
     - `app/src/main/java/com/example/smartnewsapp/ui/MainViewModel.kt`
     - `app/src/main/java/com/example/smartnewsapp/data/remote/gateway/HermesGateway.kt`
   - The refresh gesture only repeats the same GET request.
   - It does not call `POST /api/news/refresh`.
   - It does not call `GET /api/news/live`.

2. Fetch failures are silent in the UI.
   - File: `app/src/main/java/com/example/smartnewsapp/domain/NewsRepository.kt`
   - `syncNews()` catches all exceptions and only prints the stack trace.
   - The UI then keeps displaying cached Room articles, which looks like a
     successful refresh with unchanged news.

3. Old rows remain in the main feed after later syncs.
   - Files:
     - `app/src/main/java/com/example/smartnewsapp/domain/NewsRepository.kt`
     - `app/src/main/java/com/example/smartnewsapp/data/local/NewsDao.kt`
   - Sync upserts returned articles but does not remove articles missing from the
     latest response.
   - `getAllArticles()` reads every stored row.

4. Empty state asks the user to pull down, but pull-to-refresh is not mounted.
   - File: `app/src/main/java/com/example/smartnewsapp/ui/main/MainScreen.kt`
   - `PullToRefreshBox` is only rendered when there are articles or refresh is
     already active.

5. Configured query parameters are dropped.
   - File: `app/src/main/java/com/example/smartnewsapp/data/remote/gateway/HermesGateway.kt`
   - `buildGenericNewsUrl()` calls `clearQuery()`.
   - A configured URL such as `/api/news/live?feed_url=...` loses its existing
     query params.

## Proposed Product Semantics

Use two explicit refresh modes:

- Normal load:
  - Fetch `GET /api/news/recommended`.
  - Keep this as the generic, low-latency, cache-friendly feed.

- Manual pull-to-refresh:
  - Prefer `GET /api/news/live` for immediate fresh RSS headlines.
  - If the product wants the durable Hermes cron snapshot instead, call
    `POST /api/news/refresh`, wait briefly or poll, then fetch
    `GET /api/news/recommended`.

Recommended MVP: use `/api/news/live` for pull-to-refresh and keep startup on
`/api/news/recommended`. This gives visible refresh behavior without relying on
the slower cron refresh lifecycle.

## Implementation Tasks

### Task 1: Add Refresh Intent To The News Gateway

Change the gateway API so callers can distinguish normal load from manual
refresh:

```kotlin
interface NewsGateway {
    suspend fun fetchLatestNews(mode: NewsFetchMode = NewsFetchMode.Normal): List<Article>
}

enum class NewsFetchMode {
    Normal,
    ManualRefresh
}
```

Hermes behavior:

- `Normal` uses `/api/news/recommended`.
- `ManualRefresh` uses `/api/news/live`, unless settings explicitly point to a
  non-Hermes endpoint.

### Task 2: Preserve Existing Query Parameters

Update `HermesGateway.buildGenericNewsUrl()` so it does not unconditionally drop
query params.

Rules:

- Preserve configured query params.
- Override only `language`.
- Rewrite `/api/news` to `/api/news/recommended` only for normal mode.
- Do not rewrite `/api/news/live`.

### Task 3: Surface Refresh Errors

Change `NewsRepository.syncNews()` to return a result instead of swallowing
exceptions:

```kotlin
sealed interface NewsSyncResult {
    data class Success(val count: Int) : NewsSyncResult
    data class Failure(val throwable: Throwable) : NewsSyncResult
}
```

Then expose an error state from `MainViewModel` so `MainScreen` can show a
snackbar or inline message when refresh fails.

### Task 4: Decide Stale Article Policy

Pick one policy and implement it intentionally:

- Mirror latest feed:
  - Delete local rows whose IDs are not in the latest remote response.
  - Best when the main screen should show only current feed items.

- Keep archive:
  - Add a `lastSeenInFeedAt` or `feedGeneration` column.
  - Query only current generation for the main feed.
  - Keep older articles available for saved/read history later.

Recommended MVP: add a `feedGeneration` or `lastSeenInFeedAt` column so saved
articles and read history are not accidentally deleted.

### Task 5: Mount Pull-To-Refresh In Empty State

Wrap the empty state inside `PullToRefreshBox` too. The empty-state text should
match actual behavior:

- "Pull to fetch latest news" if manual refresh uses `/api/news/live`.
- "Pull to reload feed" if it only reloads `/api/news/recommended`.

### Task 6: Add Focused Tests

Add tests for:

- `/api/news` default is rewritten to `/api/news/recommended` in normal mode.
- Manual refresh uses `/api/news/live`.
- Existing query params survive URL building.
- `language` is normalized and overrides any configured language query.
- Repository returns failure on gateway exception.
- Main feed excludes stale rows according to the chosen stale article policy.

## Manual Validation

1. Launch app with Hermes news provider selected.
2. Confirm startup calls:

```text
GET https://hermes.vectororch.com/api/news/recommended?language=tr
```

3. Pull to refresh.
4. Confirm manual refresh calls:

```text
GET https://hermes.vectororch.com/api/news/live?language=tr
```

5. Disable network and pull to refresh.
6. Confirm the UI reports refresh failure instead of silently looking unchanged.
7. Confirm old articles do not appear in the main feed unless the chosen archive
   policy intentionally includes them.

## Notes

- Do not send selected categories, interest graph data, read history, device IDs,
  user IDs, or chat content to Hermes.
- Keep personal ranking local by combining Room articles with local settings and
  interest profile data.
- The Hermes recommended endpoint is working when it returns the same IDs for the
  same server cron snapshot.
