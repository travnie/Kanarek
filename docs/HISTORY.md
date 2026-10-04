# Kanarek history

Kanarek began as the `fidy` / `feedy` / `feedget` project, later lived inside `trvny/feeds`, and was extracted to `travnie/Kanarek` with Git history preserved. The commit history is the canonical detailed record; this file keeps only durable milestones.

## Major milestones

### News reader

- native RSS/Atom parsing with an optional Worker aggregation path,
- OPML import/export, feed discovery and HTML-to-Atom scraping,
- conditional ETag/304 caching and last-known-good behavior,
- thumbnails/favicons, local search, source filters and optional headline ranking,
- clean-reader extraction, saved/offline article text and new-story notifications,
- per-widget feed selection, navigation and refresh interval.

### Radio and IPTV

- Media3 background playback with system media controls,
- M3U/M3U8 import/export, per-stream `User-Agent` / `Referer` support and stream metadata,
- station favorites, Radio Browser discovery and iptv-org logo fallback,
- radio/TV grouping, tabs and ICY now-playing metadata,
- player widget and Google Cast in the `play` flavor; `foss` remains GMS-free.

Cast receivers fetch media independently, so local per-stream request headers do not transfer to Cast.

### UI and storage

- one-window `HomeActivity` with reader/player pages,
- source search and grouped station browsing,
- dedicated Memory & Data controls for cache and reader state,
- launcher-safe `RemoteViews` widgets with Robolectric inflation coverage.

### Backend and portability

- optional Cloudflare Worker for feed aggregation, discovery/scraping, clean-reader extraction, station/logo lookup and optional synchronized state,
- portable parsing/domain logic moved into the Kotlin Multiplatform `shared` module,
- production Worker deployment owned by Cloudflare Workers Builds.

## Naming and infrastructure

The product eventually standardized on:

- app/package: **Kanarek** / `com.kanarek`,
- Worker: `kanarek`,
- D1 state database: `kanarek-state`,
- repository: `travnie/Kanarek`.

Older `fidy`, `feedy`, `feedget` and monorepo paths are historical names only.

## Feedseek overlap

Kanarek's on-demand `/scrape` path and Feedseek's generated feeds solve related page-to-feed problems differently. A future integration may let `/discover` consult a Feedseek-maintained source map before probing generic feed paths; this is planned, not current behavior.

## Current horizon

- authenticated feeds without weakening credential boundaries,
- playlist drag-and-drop/reordering,
- real Android Auto validation; MediaSession support exists, but in-car behavior is not yet verified.

## Current documentation

- [Architecture](ARCHITECTURE.md)
- [Worker and API](WORKER.md)
- [Development and releases](DEVELOPMENT.md)
- [Integrations](INTEGRATIONS.md)
- [Known concerns](CONCERNS.md)

Build/toolchain versions live in repository configuration, not in this history file.
