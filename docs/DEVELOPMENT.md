# Development

## Requirements and build

Use JDK 17, Android SDK, the **exact** Gradle version in `gradle/wrapper/gradle-wrapper.properties`, and Node.js/npm for the Worker. Dependency pins live in `gradle/libs.versions.toml`, `worker/package.json` and their lock/config files, not here.

The repository does **not** commit `gradlew` or the wrapper JAR. On a new clone, install the declared Gradle version and generate the wrapper:

```sh
GRADLE_VERSION=$(sed -n 's#^distributionUrl=.*/gradle-\([0-9][A-Za-z0-9.-]*\)-\(bin\|all\)\.zip$#\1#p' gradle/wrapper/gradle-wrapper.properties)
gradle --version
gradle wrapper --gradle-version "$GRADLE_VERSION" --no-daemon
./gradlew assemblePlayDebug assembleFossDebug
```

`play` includes Google Cast; `foss` excludes GMS and is the F-Droid-compatible flavor. `app/build.gradle.kts` owns version names, SDK levels and package IDs.

### Bundled Benches

CI release and preview APKs include pinned portable Bench assets. For equivalent local builds, install **Node 24**, npm, git, **Bash 4+** and **`sha256sum` on `PATH`**, then run this **before** assembling (stock macOS Bash 3.2 and the default checksum utilities are insufficient):

```sh
bash .github/scripts/stage-benches.sh
./gradlew assemblePlayDebug
```

The script fetches a specific commit from `travnie/twojstar`, builds all three portable pages and checks hashes. Generated `app/src/main/assets/benches/` files are ignored. Builds made without staging can open the hosted Benches but must not be described as offline-equipped.

## Tests

```sh
./gradlew testPlayDebugUnitTest :shared:testAndroidHostTest lintPlayDebug
./gradlew assemblePlayDebug assembleFossDebug
# macOS with Xcode:
./gradlew :shared:iosSimulatorArm64Test
# Worker:
cd worker && npm ci && npm run typecheck && npm test
```

- `shared/commonTest`: parsers, OPML/M3U codecs, state and portable UI models.
- `app/src/test`: Android orchestration, backups, scanner/security cases, player state and Robolectric widget `RemoteViews`.
- `worker/test`: parser, ETag, discovery, station and state helpers. Vitest uses Node, not a simulated Cloudflare runtime.
- There is no general Android instrumentation suite. Validate native file picker, camera, Bench exports, launcher widgets and Cast on devices before claiming those flows fully verified.

## CI and release

- `android-ci.yml`: staged Benches, both APK flavors, tests/lint and preview/debug artifacts.
- `kmp-ios-ci.yml`: iOS simulator tests for the shared library, not an iOS application.
- `worker-ci.yml`: typecheck, Vitest and default-feed sync check; **does not deploy**.
- `release.yml`: rolling signed Play/FOSS APKs for relevant `main` changes, with pinned Bench asset verification.
- Production Worker deployment belongs solely to **Cloudflare Workers Builds** on `worker/` changes.
- GitHub Dependabot, CodeQL and MegaLinter provide additional checks. Triage dependency alerts against actual runtime reachability; build-tooling-only advisories are not automatically application vulnerabilities.

Use `.github/scripts/sync-default-feeds.mjs` and its CI check for changes to bundled feed defaults. Do not modify generated results independently.

Release signing requires external `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` secrets. For F-Droid version tracking, update `versionCode` / `versionName` in `app/build.gradle.kts` and use a matching `v<versionName>` tag. Pushing a tag does **not** trigger an APK build: `release.yml` publishes the moving `kanarek-latest` artifacts for relevant `main` changes or manual dispatch. Preview APKs use a separate `com.kanarek.preview` application ID and debug signature and install beside release APKs.

## Boundaries

Keep portable logic under `shared/`; keep Android lifecycle/state under `app/`. Never make the Worker mandatory for standard feeds or leak Google-specific APIs into `foss`. Keep one app station store and one playback service. See [Architecture](ARCHITECTURE.md), [Tools](TOOLS_INTEGRATION.md), [Worker](WORKER.md) and [F-Droid](FDROID.md).
