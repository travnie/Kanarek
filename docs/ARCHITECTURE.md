# Architecture

Kanarek has three Android destinations (**News**, **Radio & TV**, **Tools**), one portable Kotlin Multiplatform module and an optional Cloudflare Worker. The Android shell and its services remain the product boundary. Tool web apps remain owned by [twojstar/benches](https://github.com/travnie/twojstar/tree/main/benches).

## Ownership

| Area | Source of truth | Responsibilities |
| --- | --- | --- |
| `app/` | Android application | Compose navigation, DataStore, WorkManager, widgets, Media3 service, file picking and Bench WebViews |
| `shared/src/commonMain/` | Portable domain | Feed parsing, OPML/M3U codecs, state transformations, reader/player models |
| `shared/src/androidMain/` and `iosMain/` | Platform adapters | Platform-specific implementations for shared contracts |
| `worker/` | Optional edge | Feed aggregation, discovery, scraping, clean article extraction, radio directory, logo lookup and optional state sync |
| `travnie/twojstar/benches` | Upstream Bench sources | Codebench, Docbench and Streambench browser UIs and independent Workers |

`HomeActivity` hosts a three-page Compose pager. `ReaderScreen` and `PlayerScreen` keep normal app state, while `ToolsScreen` owns isolated Bench WebViews and incoming file choices. Disabling swipe on the Tools page avoids stealing gestures from embedded tools. An Android Activity is not the playback engine.

## News and widgets

`NewsRepository` uses the configured Worker for merged feeds and ETag caching when available; if the backend URL is blank or a request fails, feeds can still be fetched and parsed on-device using shared `FeedParser`. Individual source failures are isolated. Last-known-good widget content must survive a failed refresh.

Reader state, saved/offline articles, notifications and cache cleanup are Android-owned. News and player launcher widgets use `RemoteViews` and WorkManager rather than Compose; Robolectric tests apply real widget layouts.

## Playback

`PlayerService` is a `MediaSessionService` and owns the ExoPlayer, media session and playback lifetime. The Activity binds for local control; widgets send explicit service actions. Playlist-provided `User-Agent` / `Referer` are used by local playback; the Cast receiver independently fetches remote streams, so those headers do not follow Cast. Cast code belongs only in `play`; `foss` remains GMS-free.

## Tools and file boundaries

The three Benches are embedded as **pinned generated assets** staged by `.github/scripts/stage-benches.sh` from a single twojstar revision. The script verifies per-Bench hashes; generated HTML must never be hand-edited here.

`OfflineBenches` serves each asset through an isolated HTTPS-like `WebViewAssetLoader` origin and restricts requests. Only the existing allowlisted Streambench API is reachable from its offline mode. Hosted mode is an explicit user choice, not an offline fallback guarantee.

Android's share/view intents carry scoped URI access. `IncomingBenchFiles` performs bounded, read-only imports after the user selects a Bench. `BenchExportReceiver` handles bounded, chunked exports with the system save picker. Only trusted bundled main-frame origins receive native bridging; no unrestricted third-party JS interface, silent upload, or blanket filesystem permission. The original file is not edited in place.

The Quick Settings scan tile requires a one-use token. Scan results are parsed locally; opening a link, inserting a contact or using another system action requires an explicit user tap.

## Operational boundaries

- External feeds, streams, playlists, QR data and HTML are untrusted; bound I/O and validate URLs, redirects, MIME and payload sizes.
- `worker/wrangler.jsonc` owns default-feed configuration. `.github/scripts/sync-default-feeds.mjs` renders its Android copy; CI checks for drift.
- `gradle/libs.versions.toml` and module build files own SDK/dependency versions; `worker/package.json` and Wrangler own Worker versions/configuration.
- Worker `Cache API` is fast edge caching; optional `SCRAPE_KV` adds discovery/scrape persistence and optional `STATE_DB` enables paired state.
- Worker helper tests run in Node, not a complete Workers runtime. Verify binding/HTMLRewriter/Cache API behavior separately when changed.
- KMP's iOS target currently validates portability, **not** a shipping iOS app.
- Release signing depends on external secrets; launcher behavior varies by host. Check real devices for QR/camera, export, widgets and Cast.

## Further reading

- [Tools and Benches](TOOLS_INTEGRATION.md)
- [Development and validation](DEVELOPMENT.md)
- [Worker API](WORKER.md)
- [F-Droid](FDROID.md)
- [History](HISTORY.md)
