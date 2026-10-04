# Kanarek development

## Requirements

- Android Studio or command-line Android SDK
- JDK 17
- the Gradle version declared in `gradle/wrapper/gradle-wrapper.properties`
- Node.js and npm for the Worker

Dependency versions live in `gradle/libs.versions.toml`.

The Gradle wrapper JAR and scripts are not committed. On a fresh clone, follow the bootstrap in `.github/workflows/android-ci.yml`: read the required version from `gradle/wrapper/gradle-wrapper.properties`, install/use that exact Gradle version, then run its `wrapper` task. Do not rely on an older system Gradle to configure the project.

## Build and install

Run Android commands from the repository root:

```bash
./gradlew assembleDebug
./gradlew installPlayDebug
```

`assembleDebug` builds both product flavors:

- `play`: includes the proprietary Google Cast sender SDK,
- `foss`: GMS-free and suitable for F-Droid builds.

Flavor-specific commands include:

```bash
./gradlew assemblePlayDebug
./gradlew assembleFossDebug
./gradlew assembleFossRelease
```

After installation, long-press the Android home screen, open **Widgets**, and add either the Kanarek news widget or the radio/TV player widget.

## Tests

App logic and widget inflation:

```bash
./gradlew testPlayDebugUnitTest
```

Worker typecheck and tests:

```bash
cd worker
npm ci
npm run typecheck
npm test
```

The JVM suite covers feed parsing, entity decoding, image precedence, date normalization, OPML round-trips, headline ranking, M3U/M3U8 parsing, per-stream headers, favicon fallback, and widget RemoteViews inflation.

The Worker suite covers feed parsing, conditional ETags, output formats, Radio Browser mapping, iptv-org logo selection, discovery/scraping, and state-related helpers.

## Continuous integration

Repository workflows live in `.github/workflows/`.

- `android-ci.yml`: builds play and foss debug APKs, runs Android lint and JVM tests.
- `worker-ci.yml`: TypeScript typecheck and Vitest tests for Worker changes.
- `release.yml` ("Rolling release"): on every push to `main` touching `app/`, `shared/` or the
  Gradle files, builds signed play/foss APKs and republishes them under the moving
  `kanarek-latest` tag. No workflow here triggers on a pushed tag — publishing a `v<versionName>`
  tag builds nothing.
- Production Worker deployment is owned by Cloudflare Workers Builds and runs from `worker/` on `main` changes.
- GitHub CodeQL default setup provides repository code scanning.
- Dependabot handles dependency updates according to the repository configuration.

Android build output should be verified through CI when the local environment cannot provide the required SDK and exact Gradle setup.

## Android notes

- The player requests notification permission on Android 13+ when needed for notification and lock-screen controls. Playback itself can continue without the visible notification permission.
- Slideshow auto-advance depends partly on launcher support for `autoAdvanceViewId`; the flipper also starts itself as a fallback.
- Widget layouts must contain only RemoteViews-supported classes.
- Widget PendingIntents remain immutable. The news widget uses an activity trampoline for article links.
- Widget images use raw network fetching and the shared `WidgetImageCache`, not Coil.
- A failed news refresh keeps the last successful item set.
- Default feed changes must stay synchronized between the app and the Worker.
- Pure data codecs should remain free of Android imports so they can run in JVM tests.

## Release flavors

The intended release artifacts are:

- `kanarek-<version>.apk` for the play flavor,
- `kanarek-<version>-foss.apk` for the GMS-free flavor.

`app/build.gradle.kts` is the version source of truth. Standalone release tags use `v<versionName>`, matching the migrated `v1.0.0` and `v1.0.1` tags and the F-Droid metadata. Before a new release, bump `versionName` and increase `versionCode`, merge that change to `main`, then tag the release commit with the matching `v<versionName>` tag.

Release signing uses repository secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`. The rolling release is currently producing signed APKs, so the migrated signing path is operational; the secret values remain external GitHub state and cannot be inspected from repository contents.

Further reading:

- [Architecture](ARCHITECTURE.md)
- [Worker and API](WORKER.md)
- [Project history](HISTORY.md)
