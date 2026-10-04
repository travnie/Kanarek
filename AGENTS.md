# AGENTS.md

Kanarek: Android news reader/player with a Kotlin Multiplatform core and optional Cloudflare Worker.

## Layout and boundaries

- `app/`: Android UI, lifecycle, persistence, WorkManager, MediaSession playback and launcher widgets.
- `shared/`: source of truth for portable models, parsers, codecs and state transformations. Do not reintroduce portable domain logic into Android-only code.
- `worker/`: optional edge features. Ordinary RSS/Atom reading must remain functional with no backend configured or when the Worker is unavailable.
- Keep one maintained source of truth per concern; do not duplicate config/version values into prose.

## Platform invariants

- Keep `main` free of mandatory GMS dependencies. Google-specific functionality belongs behind the existing `play` flavor; `foss` must remain buildable without it.
- Launcher widgets use `RemoteViews`; keep layouts launcher-safe and preserve widget inflation coverage.
- Playback is service-owned rather than Activity-owned. UI/navigation changes must not make background media depend on one screen staying alive.
- External feeds, pages, playlists and streams are untrusted. Keep network work bounded and isolate individual source failures.
- Never commit credentials, tokens or private deployment metadata. Non-secret Wrangler account/binding/resource IDs may stay in `worker/wrangler.jsonc` when required for reproducible deployment.

## Delivery

- Cloudflare Workers Builds owns production Worker deployment; GitHub Actions validates the Worker and must not grow a second production deploy path.
- Build/dependency/SDK versions live in Gradle/Wrangler/package configuration. Agent docs should describe invariants, not copy version numbers.
- Generated/build output is not maintained source.
- `megalinter-reports/updated_sources` contains suggestions; apply only intended fixes.

## Work

- Check `main`, open PRs and recent changes before overlapping work.
- Keep one logical change per PR.
- Validate the affected surface: shared logic with shared tests, Android behavior with Android checks, Worker behavior with Worker checks.
