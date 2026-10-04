# Concerns

This is a maintenance-risk map, not a bug list. Items here are worth remembering when changing nearby code.

## 1. Default feed generation is a cross-module contract

**Risk:** low.

`worker/wrangler.jsonc` is the maintained source of truth for default feeds. `.github/scripts/sync-default-feeds.mjs` renders that list into `NewsRepository.DEFAULT_FEEDS`, preserving the Android on-device fallback without requiring runtime access to the Worker or GitHub.

**Current mitigation:** existing Worker CI runs the generator in `--check` mode for Worker changes and also when the Android default-feed block or sync script changes. A stale generated list therefore fails CI instead of silently drifting.

**Watch for:** changing the shape/location of `DEFAULT_FEEDS` without updating the sync script, or introducing a second independently maintained default list elsewhere.

## 2. Worker tests do not exercise the Workers runtime

**Risk:** medium.

`worker/vitest.config.ts` runs tests in plain Node. This is appropriate for the current pure helper tests, but it cannot validate Cloudflare-specific behavior such as real bindings, Cache API semantics, `HTMLRewriter` runtime differences or D1/KV integration wiring.

**Current mitigation:** runtime-facing code is kept thin and many transformations are exported as pure helpers; Worker CI typechecks the package and Cloudflare Workers Builds owns deployment.

**Watch for:** changes whose correctness depends on Workers runtime behavior rather than pure parsing. Those are candidates for Cloudflare's Vitest/Workers test environment instead of pretending a Node unit test covers them.

## 3. Several files are large orchestration hotspots

**Risk:** medium.

The current tree contains a few intentionally broad files, notably `worker/src/index.ts`, `ReaderComponents.kt`, `PlayerComponents.kt` and `PlayerService.kt`.

Large size is not itself a defect, but these files combine many routes/components/state transitions and therefore have a larger regression radius.

**Guideline:** extract only along an existing concern boundary when a change benefits from it. Do not create a parallel architecture merely to make files shorter.

## 4. External feeds, directories and streams are inherently unstable

**Risk:** medium operationally, low architecturally.

Kanarek depends on third-party publishers, generated feeds, Radio Browser, iptv-org metadata and individual radio/IPTV stream endpoints. Hosts can disappear, redirect, rate-limit, change HTML or require new headers without a Kanarek release.

**Current mitigation:** bounded reads, per-source failure isolation, on-device fallback, persisted stations, last-known-good widget data, optional directory/logo lookups and per-stream request headers for local playback. Cast uses the receiver's own network requests, so streams that require Kanarek's local `User-Agent`/`Referer` overrides can still fail when cast.

**Watch for:** fixes that special-case one provider inside a shared model. Keep provider quirks at the integration edge where possible.

## 5. iOS is currently a portability target, not a shipping client

**Risk:** low while this boundary stays explicit.

The KMP module builds/tests Android and iOS targets, but this repository contains no standalone iOS application project. Treat the current iOS target as a portability guard for shared logic until the repository gains an actual iOS client.

Prefer portable `commonMain` code when it is naturally platform-independent, but do not force Android-specific lifecycle/storage code through abstractions solely for hypothetical reuse. If a shipping iOS client is added, revisit this boundary as a product/architecture change.

## 6. Release signing credentials are external state

**Risk:** operational.

GitHub release signing depends on repository secrets in `travnie/kanarek`. The rolling release is currently producing signed APKs, so the migrated workflow is operational, but secret values still cannot be inspected from repository contents.

**Current mitigation:** the rolling release exercises the signed path, while Gradle release signing remains optional so unsigned/F-Droid downstream builds are possible.

**Watch for:** credential rotation or repository migration breaking the signed release path even when unsigned/local builds still pass.

## 7. Launcher widget behavior depends partly on host launchers

**Risk:** low to medium.

App Widgets run inside another process/UI host and support only a constrained `RemoteViews` vocabulary. Launcher implementations also differ in auto-advance and resize behavior.

**Current mitigation:** launcher-safe layouts, explicit size classes, fallback auto-advance behavior, bounded image cache and Robolectric tests that apply real `RemoteViews`.

**Watch for:** Compose-only components or unsupported view/resource assumptions leaking into widget layouts.

## 8. Backend optionality is an architectural invariant

**Risk:** high if accidentally broken.

The Worker adds substantial functionality, which makes it tempting to rely on normalized Worker responses everywhere. Basic RSS/Atom reading must still work with no backend and should fall back on-device when a backend request fails under the current contract.

**Current mitigation:** `NewsRepository` explicitly contains both paths and shared `FeedParser` remains Android-usable.

Treat a change that makes the Worker mandatory for ordinary feeds as an architectural/product change, not a routine refactor.

## Recently corrected drift

The architecture documentation previously listed portable parser/model files as if they still lived under `app/src/main`. They now live in `shared/commonMain`. The codebase-knowledge refresh corrects that map; future module moves should update these docs in the same change.
