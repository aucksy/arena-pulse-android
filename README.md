# ArenaPulse: Live AI Leaderboard

ArenaPulse is a native Android leaderboard for frontier AI models. It streams the public
OpenRouter model catalog, merges it with a deterministic benchmark registry, and renders
rankings, head-to-head comparisons and a Pareto cost/value matrix in a high-contrast
"Cyber-Obsidian" Compose UI.

No API key is required. No Room, Hilt, KSP or KAPT is used anywhere.

## Features

- **Live Rankings** — pulsing live-sync status with a relative "last sync" label, top-three
  podium, category filters (All, Coding, Reasoning, Agentic, Open-Weight, Ultra-Cheap),
  six sort modes including Newest, animated score bars and `Modifier.animateItem()` row
  transitions. Pull down to re-sync.
- **Search and provider filters** — multi-term search across model name, provider and slug,
  plus one-tap provider narrowing and a watchlist-only view. Essential once the live catalog
  returns several hundred models.
- **Watchlist** — star any model to pin it. Persisted in SharedPreferences and surfaced as a
  dedicated Signals section.
- **Price-move tracking** — each sync is compared against the previous cache entry, so cards
  show a `▼ 12%` / `▲ 8%` badge when a provider changes pricing, and Signals lists the
  steepest drops.
- **New-arrival badges** — models first seen within 60 days are tagged, with a New Arrivals
  list in Signals.
- **Model Inspector** — animated five-axis radar chart, rank context, share sheet, copyable
  model ID, capability badges and a daily token-volume cost simulator with Light / Standard /
  Heavy presets.
- **Head-to-Head Versus** — searchable model pickers, six tug-of-war metrics, an overall
  verdict and a shareable comparison summary.
- **Pareto Value Matrix** — log-scale cost vs. intelligence scatter plot with a highlighted
  frontier. Plots the top 40 by default with a "Show all" toggle for large catalogs.
- **Market Signals** — best value, fastest, cheapest, coding/reasoning/agentic leaders, price
  moves, new arrivals, open-weight watchlist and your watchlist.
- **About and data settings** — version, snapshot freshness, data provenance, privacy notes,
  clear cache and resync, clear watchlist.
- **Offline-first** — the merged catalog is cached as JSON in
  `filesDir/leaderboard_cache.json`; if the network fails the app serves the cache or a
  built-in seed of 24 frontier models.

## Architecture

```
app/src/main/java/com/arenapulse/android/
  AppContainer.kt              manual dependency injection
  MainActivity.kt              edge-to-edge Compose host
  data/
    ModelDto.kt                OpenRouter DTOs + $/token -> $/1M conversion
    FrontierScoresRegistry.kt  curated benchmark table + family heuristics + 24 seeds
    OpenRouterApiClient.kt     OkHttp client for /api/v1/models
    LeaderboardRepository.kt   live -> cache -> seed fallback, JSON persistence, price history
    UserPreferences.kt         SharedPreferences-backed watchlist
  domain/
    AiModel.kt                 serializable leaderboard entity + value/price-delta math
    LeaderboardUseCases.kt     search, filtering, sorting, cost simulation, Pareto frontier
  ui/
    LeaderboardViewModel.kt    StateFlow UI state, watchlist and cache orchestration
    ShareHelper.kt             ACTION_SEND helper
    theme/Theme.kt             Cyber-Obsidian palette and typography
    components/                radar chart and scatter plot canvases
    screens/                   Rankings, Versus, Matrix, Signals, Inspector, pickers, About
```

Per-tab UI state (search text, filters, scroll position) survives bottom-navigation switches
via `rememberSaveableStateHolder`.

## Live data pipeline

1. `GET https://openrouter.ai/api/v1/models` (unauthenticated).
2. Parse `id`, `name`, `created`, `context_length`, `pricing.prompt`,
   `pricing.completion`, `architecture.modality` and `supported_parameters`.
3. Convert per-token prices to USD per 1M tokens.
4. Merge with `FrontierScoresRegistry`: curated slugs first, substring matches for
   dated snapshots, then a deterministic family heuristic for new models.
5. Diff against the previous cache entry to attach previous prices.
6. Persist the merged list to `leaderboard_cache.json`.

Blended price uses a 3:1 input/output weighting:
`input * 0.75 + output * 0.25`. Value score is `overall / blended price` with a
$0.05 floor. The app only auto-refreshes when the snapshot is older than ten minutes.

## Build and test

All compilation happens in GitHub Actions; no local Android SDK is required.

- Workflow: `.github/workflows/android-ci.yml`
- Unit tests: `gradle testDebugUnitTest --stacktrace`
- APK: `gradle assembleDebug --stacktrace`
- Artifact: `ArenaPulse-Debug-APK`

## Releases

Download the latest APK from
[GitHub Releases](https://github.com/aucksy/arena-pulse-android/releases/latest).

The release workflow (`.github/workflows/release.yml`) runs the unit tests, builds the
debug APK and publishes it as a GitHub Release asset. It can be triggered by pushing a
`v*` tag or manually from the Actions tab with a tag name such as `v1.1.0`.

```bash
git tag v1.1.0
git push origin v1.1.0
```

## Stack

Kotlin 2.0.21, AGP 8.7.3, Gradle 8.10.2, Jetpack Compose (BOM 2024.11.00),
Material 3, kotlinx.serialization 1.7.3, OkHttp 4.12.0, coroutines 1.9.0,
compileSdk/targetSdk 35, minSdk 26, JVM 17.
